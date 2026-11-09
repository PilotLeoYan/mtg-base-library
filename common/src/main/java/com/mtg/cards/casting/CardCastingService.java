package com.mtg.cards.casting;

import com.mtg.api.trigger.CastSpellEvent;
import com.mtg.api.trigger.EnterBattlefieldEvent;
import com.mtg.api.trigger.LandPlayedEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.cards.effects.CardEffectRegistry;
import com.mtg.cards.effects.CastContext;
import com.mtg.cards.effects.ICardEffectHandler;
import com.mtg.config.CardsConfig;
import com.mtg.config.MtgConfig;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckManager;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.CardDefinition;
import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import com.mtg.mana.ManaManager;
import com.mtg.progression.ProgressionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core runtime casting engine coordinating spellcast initiation,
 * windup channeling delays, damage interruptions, cooldowns, and effect resolution.
 */
public class CardCastingService {
    private static final TriggerBus DEFAULT_BUS = new TriggerBus();
    private static volatile CardCastingService instance;

    private CardsConfig config;
    private final CooldownTracker cooldownTracker;
    private final Map<UUID, ActiveCast> activeCasts = new ConcurrentHashMap<>();
    private TriggerBus triggerBus = DEFAULT_BUS;

    public static CardCastingService getInstance() {
        if (instance == null) {
            synchronized (CardCastingService.class) {
                if (instance == null) {
                    instance = new CardCastingService();
                }
            }
        }
        return instance;
    }

    public static void setInstance(CardCastingService custom) {
        instance = custom;
    }

    public CardCastingService() {
        this(MtgConfig.getCards());
    }

    public CardCastingService(CardsConfig config) {
        this.config = config != null ? config.sanitize() : CardsConfig.defaults();
        this.cooldownTracker = new CooldownTracker(this.config);
        // Ensure base effects and cards are initialized
        BasicLandsCatalog.registerAllEffects();
    }

    public void setConfig(CardsConfig config) {
        this.config = config != null ? config.sanitize() : CardsConfig.defaults();
    }

    public CardsConfig getConfig() {
        return config != null ? config : MtgConfig.getCards();
    }

    public CooldownTracker getCooldownTracker() {
        return cooldownTracker;
    }

    public void setTriggerBus(TriggerBus triggerBus) {
        this.triggerBus = triggerBus != null ? triggerBus : DEFAULT_BUS;
    }

    public TriggerBus getTriggerBus() {
        return triggerBus != null ? triggerBus : DEFAULT_BUS;
    }

    /**
     * Primary entry point to initiate casting a card from a deck slot.
     */
    public CastResult initiateCast(UUID playerId, int slotIndex, CastTarget target) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        CastTarget resolvedTarget = target != null ? target : CastTarget.self();

        PlaneswalkerDeck deck = DeckManager.getInstance().getDeck(playerId);
        synchronized (deck) {
            // 1. Boundary check for slot
            if (slotIndex < 0 || slotIndex >= PlaneswalkerDeck.MAX_CAPACITY) {
                return CastResult.failed(null, slotIndex, "Slot index out of range: " + slotIndex);
            }
            if (slotIndex >= deck.getCapacity()) {
                return CastResult.lockedSlot(slotIndex);
            }

            DeckCardSlot slot = deck.getSlot(slotIndex);
            DeckSlotState state = slot.getState(deck.getCapacity());

            if (state == DeckSlotState.EMPTY) {
                return CastResult.emptySlot(slotIndex);
            }
            if (state == DeckSlotState.COOLDOWN) {
                return CastResult.onCooldown(slot.getCardId(), slotIndex);
            }
            if (state == DeckSlotState.WINDUP) {
                return CastResult.channeling(slot.getCardId(), slotIndex);
            }
            if (state != DeckSlotState.READY) {
                return CastResult.failed(slot.getCardId(), slotIndex, "Slot not ready: " + state);
            }

            // Check if player is already channeling another spell
            if (activeCasts.containsKey(playerId)) {
                return CastResult.channeling(slot.getCardId(), slotIndex);
            }

            // 2. Resolve CardDefinition
            String cardId = slot.getCardId();
            Optional<CardDefinition> cardOpt = ProgressionManager.getInstance().getCard(cardId);
            if (cardOpt.isEmpty()) {
                // Fallback search in BasicLandsCatalog
                for (CardDefinition c : BasicLandsCatalog.getAllCards()) {
                    if (c.id().equals(cardId)) {
                        ProgressionManager.getInstance().registerCard(c);
                        cardOpt = Optional.of(c);
                        break;
                    }
                }
            }

            if (cardOpt.isEmpty()) {
                return CastResult.cardNotFound(cardId, slotIndex);
            }

            CardDefinition card = cardOpt.get();

            // 3. Mana Affordability Gate
            if (!card.isLand()) {
                boolean canPay = ManaManager.getInstance().canCast(playerId, card.manaCost());
                if (!canPay) {
                    return CastResult.insufficientMana(card.id(), slotIndex);
                }
                // Atomically deduct mana
                boolean paid = ManaManager.getInstance().payCost(playerId, card.manaCost());
                if (!paid) {
                    return CastResult.insufficientMana(card.id(), slotIndex);
                }
            }

            // 4. Calculate Windup Delay
            int windupTicks = (card.isLand() || card.isInstant()) ? 0
                : (slot.getMaxWindupTicks() > 0 ? slot.getMaxWindupTicks() : cooldownTracker.calculateWindupTicks(card));

            if (windupTicks <= 0) {
                // Instant cast resolution
                resolveCast(playerId, slotIndex, card, resolvedTarget);
                cooldownTracker.applyCooldown(deck, slotIndex, card, playerId);
                return CastResult.successInstant(card.id(), slotIndex);
            } else {
                // Channeling cast initiation
                slot.startWindup(windupTicks);
                ActiveCast cast = new ActiveCast(UUID.randomUUID(), playerId, slotIndex, card, resolvedTarget, windupTicks);
                activeCasts.put(playerId, cast);
                return CastResult.channelingStarted(cast);
            }
        }
    }

    /**
     * Resolves the effect of a card upon completion of casting.
     */
    public void resolveCast(UUID playerId, int slotIndex, CardDefinition card, CastTarget target) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(card, "CardDefinition cannot be null");
        CastTarget t = target != null ? target : CastTarget.self();

        int x = (int) Math.round(t.targetX());
        int y = (int) Math.round(t.targetY());
        int z = (int) Math.round(t.targetZ());

        // 1. Fire domain trigger events
        if (card.isLand()) {
            getTriggerBus().post(new LandPlayedEvent(this, playerId, card, x, y, z));
        } else {
            getTriggerBus().post(new CastSpellEvent(this, playerId, card, false, 0));
        }

        if (card.types().contains(com.mtg.domain.CardType.CREATURE)
            || card.types().contains(com.mtg.domain.CardType.ENCHANTMENT)
            || card.types().contains(com.mtg.domain.CardType.LAND)) {
            getTriggerBus().post(new EnterBattlefieldEvent(this, playerId, card, x, y, z));
        }

        // 2. Execute concrete effect handler from CardEffectRegistry
        CastContext context = new CastContext(playerId, card, slotIndex, t.targetEntityId(), t.targetX(), t.targetY(), t.targetZ(), false, 0, System.currentTimeMillis(), true);
        Optional<ICardEffectHandler> handlerOpt = CardEffectRegistry.get(card.id());
        if (handlerOpt.isPresent()) {
            handlerOpt.get().apply(context);
        } else if (card.isCreature()) {
            // Default creature summon if no custom effect handler registered
            SummonedCreature creature = new SummonedCreature(UUID.randomUUID(), playerId, card, t.targetX(), t.targetY(), t.targetZ());
            SummonManager.getInstance().registerSummon(creature);
        }
    }

    /**
     * Ticks in-flight channeling casts, resolving finished ones.
     */
    public void tick() {
        Iterator<Map.Entry<UUID, ActiveCast>> it = activeCasts.entrySet().iterator();
        while (it.hasNext()) {
            ActiveCast cast = it.next().getValue();
            cast.tick();
            if (cast.isFinished()) {
                it.remove();
                resolveCast(cast.getPlayerId(), cast.getSlotIndex(), cast.getCard(), cast.getTarget());
                PlaneswalkerDeck deck = DeckManager.getInstance().getDeck(cast.getPlayerId());
                synchronized (deck) {
                    cooldownTracker.applyCooldown(deck, cast.getSlotIndex(), cast.getCard(), cast.getPlayerId());
                }
            }
        }
    }

    /**
     * Handles incoming damage to a player, interrupting channeling if threshold exceeded.
     */
    public boolean handlePlayerDamage(UUID playerId, double damageAmount) {
        if (!config.damageInterruptsChanneling() || damageAmount < config.damageInterruptThreshold()) {
            return false;
        }

        ActiveCast cast = activeCasts.get(playerId);
        if (cast == null) {
            return false;
        }

        synchronized (cast) {
            if (cast.isFinished() || cast.isCancelled() || cast.isInterrupted() || cast.getRemainingWindupTicks() <= 0 || !cast.tryInterrupt()) {
                return false;
            }
            activeCasts.remove(playerId, cast);
        }

        PlaneswalkerDeck deck = DeckManager.getInstance().getDeck(playerId);
        synchronized (deck) {
            DeckCardSlot slot = deck.getSlot(cast.getSlotIndex());
            slot.resetDelays();
            // 20 ticks (1s) penalty cooldown on interrupt
            deck.startCooldown(cast.getSlotIndex(), 20);
        }

        getTriggerBus().post(new SpellCastInterruptedEvent(this, playerId, cast.getCard(), damageAmount, "DAMAGE_INTERRUPT"));
        return true;
    }

    public Optional<ActiveCast> getActiveCast(UUID playerId) {
        if (playerId == null) return Optional.empty();
        return Optional.ofNullable(activeCasts.get(playerId));
    }

    public boolean cancelCast(UUID playerId) {
        if (playerId == null) return false;
        ActiveCast cast = activeCasts.remove(playerId);
        if (cast != null) {
            cast.cancel();
            PlaneswalkerDeck deck = DeckManager.getInstance().getDeck(playerId);
            synchronized (deck) {
                deck.getSlot(cast.getSlotIndex()).resetDelays();
            }
            return true;
        }
        return false;
    }

    public void clear() {
        for (ActiveCast cast : activeCasts.values()) {
            cast.cancel();
        }
        activeCasts.clear();
    }
}
