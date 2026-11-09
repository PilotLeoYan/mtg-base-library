package com.mtg.progression;

import com.mtg.config.MtgConfig;
import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central progression manager service coordinating player card unlocks,
 * duplicate transmutations, and wildcard redemptions across all gameplay activities.
 */
public class ProgressionManager {
    private static volatile ProgressionManager instance;

    private final Map<UUID, PlayerProgression> playerProgressions = new ConcurrentHashMap<>();
    private final Map<String, CardDefinition> cardRegistry = new ConcurrentHashMap<>();
    private ProgressionConfig config;

    public static ProgressionManager getInstance() {
        if (instance == null) {
            synchronized (ProgressionManager.class) {
                if (instance == null) {
                    instance = new ProgressionManager();
                }
            }
        }
        return instance;
    }

    public static void setInstance(ProgressionManager custom) {
        instance = custom;
    }

    public ProgressionManager() {
        this(MtgConfig.getProgression());
    }

    public ProgressionManager(ProgressionConfig config) {
        this.config = config != null ? config : ProgressionConfig.defaults();
    }

    public void setConfig(ProgressionConfig config) {
        this.config = config != null ? config : ProgressionConfig.defaults();
    }

    public ProgressionConfig getConfig() {
        return config != null ? config : MtgConfig.getProgression();
    }

    public void registerCard(CardDefinition card) {
        Objects.requireNonNull(card, "CardDefinition cannot be null");
        cardRegistry.put(card.id(), card);
    }

    public void registerCards(Collection<CardDefinition> cards) {
        if (cards != null) {
            cards.forEach(this::registerCard);
        }
    }

    public Optional<CardDefinition> getCard(String cardId) {
        if (cardId == null) return Optional.empty();
        return Optional.ofNullable(cardRegistry.get(cardId));
    }

    public Collection<CardDefinition> getAllCards() {
        return Collections.unmodifiableCollection(cardRegistry.values());
    }

    public PlayerProgression getProgression(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return playerProgressions.computeIfAbsent(playerId, PlayerProgression::new);
    }

    public boolean isCardUnlocked(UUID playerId, String cardId) {
        return getProgression(playerId).hasCard(cardId);
    }

    public int getWildcardCount(UUID playerId, CardRarity rarity) {
        return getProgression(playerId).getWildcardCount(rarity);
    }

    public int getDuplicateCount(UUID playerId, CardRarity rarity) {
        return getProgression(playerId).getDuplicateCount(rarity);
    }

    public CardAwardResult awardCard(UUID playerId, String cardId, CardUnlockSource source) {
        CardDefinition card = cardRegistry.get(cardId);
        if (card == null) {
            return CardAwardResult.invalid(cardId);
        }

        PlayerProgression progression = getProgression(playerId);
        ProgressionConfig currentConfig = getConfig();

        synchronized (progression) {
            if (!progression.hasCard(cardId)) {
                progression.unlockCard(cardId);
                return CardAwardResult.newUnlock(cardId, card.rarity());
            } else {
                return WildcardTransmutationEngine.processDuplicate(
                        progression, currentConfig, cardId, card.rarity(), 1
                );
            }
        }
    }

    public WildcardRedemptionResult redeemWildcard(UUID playerId, CardRarity rarity, String targetCardId) {
        if (rarity == null) {
            return WildcardRedemptionResult.failure(WildcardRedemptionResult.Status.RARITY_MISMATCH,
                    targetCardId, CardRarity.COMMON, 0, "Rarity cannot be null");
        }

        CardDefinition card = cardRegistry.get(targetCardId);
        if (card == null) {
            return WildcardRedemptionResult.failure(
                    WildcardRedemptionResult.Status.INVALID_CARD,
                    targetCardId,
                    rarity,
                    getWildcardCount(playerId, rarity),
                    "Unknown card ID: " + targetCardId
            );
        }

        if (card.rarity() != rarity) {
            return WildcardRedemptionResult.failure(
                    WildcardRedemptionResult.Status.RARITY_MISMATCH,
                    targetCardId,
                    rarity,
                    getWildcardCount(playerId, rarity),
                    "Card rarity " + card.rarity() + " does not match wildcard rarity " + rarity
            );
        }

        PlayerProgression progression = getProgression(playerId);
        synchronized (progression) {
            if (progression.hasCard(targetCardId)) {
                return WildcardRedemptionResult.failure(
                        WildcardRedemptionResult.Status.ALREADY_UNLOCKED,
                        targetCardId,
                        rarity,
                        progression.getWildcardCount(rarity),
                        "Card is already unlocked: " + targetCardId
                );
            }

            if (!progression.consumeWildcard(rarity, 1)) {
                return WildcardRedemptionResult.failure(
                        WildcardRedemptionResult.Status.INSUFFICIENT_WILDCARDS,
                        targetCardId,
                        rarity,
                        progression.getWildcardCount(rarity),
                        "Insufficient " + rarity + " wildcards"
                );
            }

            progression.unlockCard(targetCardId);
            return WildcardRedemptionResult.success(targetCardId, rarity, progression.getWildcardCount(rarity));
        }
    }

    public void clear() {
        playerProgressions.clear();
        cardRegistry.clear();
    }
}
