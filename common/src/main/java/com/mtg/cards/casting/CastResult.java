package com.mtg.cards.casting;

import java.util.Objects;
import java.util.Optional;

/**
 * Result outcome of a card casting attempt.
 */
public record CastResult(
    Status status,
    String cardId,
    int slotIndex,
    Optional<ActiveCast> activeCast,
    String message
) {
    public enum Status {
        SUCCESS_INSTANT,
        CHANNELING_STARTED,
        INSUFFICIENT_MANA,
        SLOT_ON_COOLDOWN,
        SLOT_CHANNELING,
        SLOT_EMPTY,
        SLOT_LOCKED,
        MAX_SUMMONS_EXCEEDED,
        INVALID_TARGET,
        CARD_NOT_FOUND,
        INTERRUPTED,
        FAILED
    }

    public CastResult {
        Objects.requireNonNull(status, "Status cannot be null");
        activeCast = activeCast == null ? Optional.empty() : activeCast;
        message = message == null ? "" : message;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS_INSTANT || status == Status.CHANNELING_STARTED;
    }

    public static CastResult successInstant(String cardId, int slotIndex) {
        return new CastResult(Status.SUCCESS_INSTANT, cardId, slotIndex, Optional.empty(), "Cast resolved immediately.");
    }

    public static CastResult channelingStarted(ActiveCast cast) {
        Objects.requireNonNull(cast, "ActiveCast cannot be null");
        return new CastResult(Status.CHANNELING_STARTED, cast.getCard().id(), cast.getSlotIndex(), Optional.of(cast), "Channeling started.");
    }

    public static CastResult insufficientMana(String cardId, int slotIndex) {
        return new CastResult(Status.INSUFFICIENT_MANA, cardId, slotIndex, Optional.empty(), "Insufficient mana to cast card.");
    }

    public static CastResult onCooldown(String cardId, int slotIndex) {
        return new CastResult(Status.SLOT_ON_COOLDOWN, cardId, slotIndex, Optional.empty(), "Card slot is on cooldown.");
    }

    public static CastResult channeling(String cardId, int slotIndex) {
        return new CastResult(Status.SLOT_CHANNELING, cardId, slotIndex, Optional.empty(), "Card slot is currently channeling.");
    }

    public static CastResult emptySlot(int slotIndex) {
        return new CastResult(Status.SLOT_EMPTY, null, slotIndex, Optional.empty(), "Slot is empty.");
    }

    public static CastResult lockedSlot(int slotIndex) {
        return new CastResult(Status.SLOT_LOCKED, null, slotIndex, Optional.empty(), "Slot is locked.");
    }

    public static CastResult maxSummonsExceeded(String cardId, int slotIndex) {
        return new CastResult(Status.MAX_SUMMONS_EXCEEDED, cardId, slotIndex, Optional.empty(), "Active summon limit reached.");
    }

    public static CastResult cardNotFound(String cardId, int slotIndex) {
        return new CastResult(Status.CARD_NOT_FOUND, cardId, slotIndex, Optional.empty(), "Card definition not found.");
    }

    public static CastResult interrupted(String cardId, int slotIndex, String reason) {
        return new CastResult(Status.INTERRUPTED, cardId, slotIndex, Optional.empty(), reason != null ? reason : "Casting interrupted.");
    }

    public static CastResult failed(String cardId, int slotIndex, String reason) {
        return new CastResult(Status.FAILED, cardId, slotIndex, Optional.empty(), reason != null ? reason : "Cast failed.");
    }
}
