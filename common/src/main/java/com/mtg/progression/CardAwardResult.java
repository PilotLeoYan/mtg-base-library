package com.mtg.progression;

import com.mtg.domain.CardRarity;

import java.util.Optional;

/**
 * Immutable outcome record produced when attempting to award a card to a player.
 */
public record CardAwardResult(
    Type type,
    String cardId,
    CardRarity rarity,
    int currentDuplicates,
    int threshold,
    Optional<CardRarity> wildcardAwarded,
    int newWildcardBalance
) {
    public enum Type {
        NEW_UNLOCK,
        DUPLICATE_ACCUMULATED,
        DUPLICATE_CONVERTED,
        INVALID_CARD
    }

    public static CardAwardResult newUnlock(String cardId, CardRarity rarity) {
        return new CardAwardResult(Type.NEW_UNLOCK, cardId, rarity, 0, 0, Optional.empty(), 0);
    }

    public static CardAwardResult duplicateAccumulated(String cardId, CardRarity rarity, int current, int threshold, int wildcardBalance) {
        return new CardAwardResult(Type.DUPLICATE_ACCUMULATED, cardId, rarity, current, threshold, Optional.empty(), wildcardBalance);
    }

    public static CardAwardResult duplicateConverted(String cardId, CardRarity rarity, int remainder, int threshold, CardRarity wildcardRarity, int wildcardBalance) {
        return new CardAwardResult(Type.DUPLICATE_CONVERTED, cardId, rarity, remainder, threshold, Optional.of(wildcardRarity), wildcardBalance);
    }

    public static CardAwardResult invalid(String cardId) {
        return new CardAwardResult(Type.INVALID_CARD, cardId, CardRarity.COMMON, 0, 0, Optional.empty(), 0);
    }

    public boolean isNewUnlock() {
        return type == Type.NEW_UNLOCK;
    }

    public boolean isDuplicate() {
        return type == Type.DUPLICATE_ACCUMULATED || type == Type.DUPLICATE_CONVERTED;
    }

    public boolean didTransmute() {
        return type == Type.DUPLICATE_CONVERTED;
    }
}
