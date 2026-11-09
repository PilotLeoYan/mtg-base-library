package com.mtg.cards.casting;

import com.mtg.domain.CardDefinition;

import java.util.Objects;
import java.util.UUID;

/**
 * Tracks an in-flight channeling spell cast by a player.
 */
public final class ActiveCast {

    public enum CastState {
        CHANNELING,
        RESOLVED,
        INTERRUPTED,
        CANCELLED
    }

    private final UUID castId;
    private final UUID playerId;
    private final int slotIndex;
    private final CardDefinition card;
    private final CastTarget target;
    private final int totalWindupTicks;
    private int remainingWindupTicks;
    private CastState state;

    public ActiveCast(UUID castId, UUID playerId, int slotIndex, CardDefinition card, CastTarget target, int totalWindupTicks) {
        this.castId = Objects.requireNonNull(castId, "castId cannot be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.slotIndex = slotIndex;
        this.card = Objects.requireNonNull(card, "card cannot be null");
        this.target = target != null ? target : CastTarget.self();
        this.totalWindupTicks = Math.max(1, totalWindupTicks);
        this.remainingWindupTicks = this.totalWindupTicks;
        this.state = CastState.CHANNELING;
    }

    public synchronized void tick() {
        if (state == CastState.CHANNELING && remainingWindupTicks > 0) {
            remainingWindupTicks--;
            if (remainingWindupTicks <= 0) {
                state = CastState.RESOLVED;
            }
        }
    }

    public synchronized float getProgressFraction() {
        return Math.clamp(1.0f - ((float) remainingWindupTicks / (float) totalWindupTicks), 0.0f, 1.0f);
    }

    public synchronized CastingProgress getProgress() {
        return CastingProgress.of(remainingWindupTicks, totalWindupTicks, isFinished(), isInterrupted());
    }

    public synchronized boolean isFinished() {
        return remainingWindupTicks <= 0 && state == CastState.RESOLVED;
    }

    public synchronized boolean isChanneling() {
        return state == CastState.CHANNELING && remainingWindupTicks > 0;
    }

    public synchronized boolean isInterrupted() {
        return state == CastState.INTERRUPTED;
    }

    public synchronized boolean isCancelled() {
        return state == CastState.CANCELLED;
    }

    public synchronized boolean tryInterrupt() {
        if (state == CastState.CHANNELING && remainingWindupTicks > 0) {
            this.state = CastState.INTERRUPTED;
            return true;
        }
        return false;
    }

    public synchronized void interrupt() {
        if (state == CastState.CHANNELING && remainingWindupTicks > 0) {
            this.state = CastState.INTERRUPTED;
        }
    }

    public synchronized boolean tryCancel() {
        if (state == CastState.CHANNELING && remainingWindupTicks > 0) {
            this.state = CastState.CANCELLED;
            return true;
        }
        return false;
    }

    public synchronized void cancel() {
        if (state == CastState.CHANNELING && remainingWindupTicks > 0) {
            this.state = CastState.CANCELLED;
        }
    }

    public UUID getCastId() { return castId; }
    public UUID getPlayerId() { return playerId; }
    public int getSlotIndex() { return slotIndex; }
    public CardDefinition getCard() { return card; }
    public CastTarget getTarget() { return target; }
    public int getTotalWindupTicks() { return totalWindupTicks; }
    public synchronized int getRemainingWindupTicks() { return remainingWindupTicks; }
    public synchronized int getRemainingTicks() { return remainingWindupTicks; }
    public synchronized CastState getState() { return state; }
}
