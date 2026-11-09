package com.mtg.test.e2e.tier1;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.casting.CardCastingService;
import com.mtg.cards.casting.CastResult;
import com.mtg.cards.casting.CastTarget;
import com.mtg.cards.casting.SpellCastInterruptedEvent;
import com.mtg.config.CardsConfig;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckManager;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 18: Card Cooldown & Cast Delay Enforcement E2E Test")
public class Feature18_CardCooldownAndDelayTest {

    private UUID playerId;
    private CardCastingService castingService;
    private TriggerBus testBus;
    private PlaneswalkerDeck deck;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        testBus = new TriggerBus();
        castingService = new CardCastingService(CardsConfig.defaults());
        castingService.setTriggerBus(testBus);
        CardCastingService.setInstance(castingService);

        E2ETestExpansionHelper.registerAllCards(ProgressionManager.getInstance()::registerCard);
        E2ETestExpansionHelper.registerAllEffects();

        deck = DeckManager.getInstance().getDeck(playerId);
        for (int i = 0; i < PlaneswalkerDeck.MAX_CAPACITY; i++) {
            deck.unequipCard(i);
            deck.getSlot(i).resetDelays();
        }

        manaPool = ManaManager.getInstance().getOrCreatePlayerMana(playerId);
        manaPool.clear();
        manaPool.setMaxMana(20);
    }

    @AfterEach
    void tearDown() {
        castingService.clear();
        DeckManager.getInstance().clear();
        ManaManager.getInstance().reset();
    }

    @Test
    @DisplayName("1. Slot ticking decrements remaining cooldown until reaching 0")
    void testCooldownTickDown() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        DeckCardSlot slot = deck.getSlot(0);

        slot.startCooldown(10);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));

        for (int i = 0; i < 5; i++) {
            slot.tick();
        }
        assertEquals(5, slot.getCooldownRemainingTicks(), "Cooldown should decrement by 5");
    }

    @Test
    @DisplayName("2. Slot returns to READY state when cooldown ticks reach 0")
    void testSlotReturnsToReadyOnCooldownExpiry() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        DeckCardSlot slot = deck.getSlot(0);
        slot.startCooldown(2);

        slot.tick(); // 1
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));

        slot.tick(); // 0
        assertEquals(DeckSlotState.READY, slot.getState(deck.getCapacity()), "Slot must transition back to READY");
    }

    @Test
    @DisplayName("3. Attempting to cast while slot is on cooldown is rejected with SLOT_ON_COOLDOWN")
    void testCastRejectedWhileOnCooldown() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        manaPool.addMana(ManaColor.RED, 5);

        // Put slot on cooldown
        deck.getSlot(0).startCooldown(50);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertFalse(result.isSuccess(), "Cast must fail while on cooldown");
        assertEquals(CastResult.Status.SLOT_ON_COOLDOWN, result.status());
    }

    @Test
    @DisplayName("4. Damage interruption aborts active cast and posts SpellCastInterruptedEvent")
    void testInterruptSpellDuringCast() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_PYROCLASM.id(), 100, 20);
        manaPool.addMana(ManaColor.RED, 2);

        AtomicBoolean eventFired = new AtomicBoolean(false);
        testBus.registerListener(SpellCastInterruptedEvent.class, e -> eventFired.set(true));

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertEquals(CastResult.Status.CHANNELING_STARTED, result.status());

        // Interrupt cast with heavy damage (threshold is 3.0f in default config)
        boolean interrupted = castingService.handlePlayerDamage(playerId, 5.0);
        assertTrue(interrupted, "Damage above threshold should interrupt channeling");
        assertTrue(eventFired.get(), "SpellCastInterruptedEvent must be dispatched");
        assertNotEquals(DeckSlotState.WINDUP, deck.getSlot(0).getState(deck.getCapacity()), "Slot should no longer be in WINDUP state");
    }

    @Test
    @DisplayName("5. Cooldowns on different slots are completely independent")
    void testIndependentSlotCooldowns() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        deck.equipCard(1, E2ETestExpansionHelper.TEST_GIANT_GROWTH.id(), 60, 0);

        deck.getSlot(0).startCooldown(100);
        assertEquals(DeckSlotState.COOLDOWN, deck.getSlot(0).getState(deck.getCapacity()));
        assertEquals(DeckSlotState.READY, deck.getSlot(1).getState(deck.getCapacity()), "Slot 1 must remain READY");
    }
}
