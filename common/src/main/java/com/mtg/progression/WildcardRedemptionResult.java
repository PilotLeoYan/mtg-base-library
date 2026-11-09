package com.mtg.progression;

import com.mtg.domain.CardRarity;

/**
 * Immutable outcome record produced when attempting to redeem a wildcard for a card unlock.
 */
public record WildcardRedemptionResult(
    Status status,
    String cardId,
    CardRarity rarity,
    int remainingWildcards,
    String message
) {
    public enum Status {
        SUCCESS,
        INVALID_CARD,
        RARITY_MISMATCH,
        ALREADY_UNLOCKED,
        INSUFFICIENT_WILDCARDS
    }

    public static WildcardRedemptionResult success(String cardId, CardRarity rarity, int remaining) {
        return new WildcardRedemptionResult(Status.SUCCESS, cardId, rarity, remaining, "Successfully redeemed wildcard for card " + cardId);
    }

    public static WildcardRedemptionResult failure(Status status, String cardId, CardRarity rarity, int remaining, String message) {
        return new WildcardRedemptionResult(status, cardId, rarity, remaining, message);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
