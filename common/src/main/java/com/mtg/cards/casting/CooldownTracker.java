package com.mtg.cards.casting;

import com.mtg.config.CardsConfig;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.CardDefinition;
import com.mtg.progression.ProgressionManager;

import java.util.Objects;
import java.util.UUID;

/**
 * Service calculating cast delays, windup times, and post-cast cooldowns
 * according to MTG card types and CardsConfig settings.
 */
public final class CooldownTracker {
    private final CardsConfig config;

    public CooldownTracker(CardsConfig config) {
        this.config = config != null ? config.sanitize() : CardsConfig.defaults();
    }

    public CardsConfig getConfig() {
        return config;
    }

    /**
     * Calculates the windup/channeling delay for a card before its effect executes.
     */
    public int calculateWindupTicks(CardDefinition card) {
        Objects.requireNonNull(card, "CardDefinition cannot be null");

        if (card.isLand() || card.isInstant()) {
            return 0;
        }

        if (card.isSorcery()) {
            if (!config.enableSorceryWindupDelays()) {
                return 0;
            }
            int totalMana = card.manaCost().totalManaValue();
            // 0.5s per mana point = 10 ticks per mana point at 20 TPS
            int delay = (int) Math.round(totalMana * config.sorceryDelayPerManaSeconds() * 20.0);
            return Math.max(card.windupDelayTicks(), Math.max(0, delay));
        }

        if (card.isCreature() || card.isEnchantment()) {
            if (card.windupDelayTicks() > 0) {
                return card.windupDelayTicks();
            }
            int mana = card.manaCost().totalManaValue();
            return Math.max(10, mana * 5);
        }

        return Math.max(0, card.windupDelayTicks());
    }

    /**
     * Calculates the post-cast cooldown in ticks for a card slot.
     */
    public int calculateCooldownTicks(CardDefinition card, UUID playerId) {
        Objects.requireNonNull(card, "CardDefinition cannot be null");
        double mult = config.cardCooldownMultiplier();

        if (card.isLand()) {
            return Math.max(1, (int) Math.round(config.landCooldownSeconds() * 20.0 * mult));
        }

        if (card.isInstant()) {
            int base = card.baseCooldownTicks() > 0 ? card.baseCooldownTicks() : (int) Math.round(config.instantBaseCooldownSeconds() * 20.0);
            int dups = playerId != null ? ProgressionManager.getInstance().getDuplicateCount(playerId, card.rarity()) : 0;
            double reduction = Math.min(0.50, Math.max(0.0, dups * config.duplicateCooldownReduction()));
            return Math.max(1, (int) Math.round(base * (1.0 - reduction) * mult));
        }

        int base = card.baseCooldownTicks() > 0 ? card.baseCooldownTicks() : 100;
        return Math.max(1, (int) Math.round(base * mult));
    }

    /**
     * Applies the computed cooldown to the specified deck slot.
     */
    public void applyCooldown(PlaneswalkerDeck deck, int slotIndex, CardDefinition card, UUID playerId) {
        Objects.requireNonNull(deck, "PlaneswalkerDeck cannot be null");
        int ticks = calculateCooldownTicks(card, playerId);
        deck.startCooldown(slotIndex, ticks);
    }
}
