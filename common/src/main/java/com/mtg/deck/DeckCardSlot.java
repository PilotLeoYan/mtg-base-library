package com.mtg.deck;

import java.util.Objects;

/**
 * Encapsulates the runtime state of a single card slot in the dedicated hand.
 * Tracks equipped card identity, cooldown recovery, and windup channeling ticks.
 */
public final class DeckCardSlot {
    private final int slotIndex;
    private String cardId;
    private int cooldownRemainingTicks;
    private int maxCooldownTicks;
    private int windupRemainingTicks;
    private int maxWindupTicks;

    public DeckCardSlot(int slotIndex) {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("Slot index cannot be negative");
        }
        this.slotIndex = slotIndex;
        this.cardId = null;
        this.cooldownRemainingTicks = 0;
        this.maxCooldownTicks = 0;
        this.windupRemainingTicks = 0;
        this.maxWindupTicks = 0;
    }

    public DeckCardSlot(int slotIndex, String cardId, int cooldownRemainingTicks, int maxCooldownTicks,
                        int windupRemainingTicks, int maxWindupTicks) {
        this.slotIndex = slotIndex;
        this.cardId = cardId;
        this.cooldownRemainingTicks = Math.max(0, cooldownRemainingTicks);
        this.maxCooldownTicks = Math.max(0, maxCooldownTicks);
        this.windupRemainingTicks = Math.max(0, windupRemainingTicks);
        this.maxWindupTicks = Math.max(0, maxWindupTicks);
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public synchronized String getCardId() {
        return cardId;
    }

    public synchronized boolean hasCard() {
        return cardId != null && !cardId.isBlank();
    }

    public synchronized void equip(String cardId, int baseCooldownTicks, int windupTicks) {
        this.cardId = Objects.requireNonNull(cardId, "Card ID cannot be null");
        this.maxCooldownTicks = Math.max(0, baseCooldownTicks);
        this.cooldownRemainingTicks = 0;
        this.maxWindupTicks = Math.max(0, windupTicks);
        this.windupRemainingTicks = 0;
    }

    public synchronized void clear() {
        this.cardId = null;
        this.cooldownRemainingTicks = 0;
        this.maxCooldownTicks = 0;
        this.windupRemainingTicks = 0;
        this.maxWindupTicks = 0;
    }

    public synchronized int getCooldownRemainingTicks() {
        return cooldownRemainingTicks;
    }

    public synchronized int getMaxCooldownTicks() {
        return maxCooldownTicks;
    }

    public synchronized float getCooldownProgress() {
        if (maxCooldownTicks <= 0 || cooldownRemainingTicks <= 0) return 0.0f;
        return Math.clamp((float) cooldownRemainingTicks / (float) maxCooldownTicks, 0.0f, 1.0f);
    }

    public synchronized void startCooldown(int ticks) {
        int t = Math.max(0, ticks);
        this.cooldownRemainingTicks = t;
        if (this.maxCooldownTicks < t) {
            this.maxCooldownTicks = t;
        }
    }

    public synchronized int getWindupRemainingTicks() {
        return windupRemainingTicks;
    }

    public synchronized int getMaxWindupTicks() {
        return maxWindupTicks;
    }

    public synchronized float getWindupProgress() {
        if (maxWindupTicks <= 0 || windupRemainingTicks <= 0) return 0.0f;
        return Math.clamp((float) windupRemainingTicks / (float) maxWindupTicks, 0.0f, 1.0f);
    }

    public synchronized void startWindup(int ticks) {
        int t = Math.max(0, ticks);
        this.windupRemainingTicks = t;
        if (this.maxWindupTicks < t) {
            this.maxWindupTicks = t;
        }
    }

    public synchronized void tick() {
        if (windupRemainingTicks > 0) {
            windupRemainingTicks--;
        } else if (cooldownRemainingTicks > 0) {
            cooldownRemainingTicks--;
        }
    }

    public synchronized DeckSlotState getState(int currentDeckCapacity) {
        if (slotIndex >= currentDeckCapacity) {
            return DeckSlotState.LOCKED;
        }
        if (!hasCard()) {
            return DeckSlotState.EMPTY;
        }
        if (windupRemainingTicks > 0) {
            return DeckSlotState.WINDUP;
        }
        if (cooldownRemainingTicks > 0) {
            return DeckSlotState.COOLDOWN;
        }
        return DeckSlotState.READY;
    }

    public synchronized boolean canCast(int currentDeckCapacity) {
        return getState(currentDeckCapacity) == DeckSlotState.READY;
    }

    public synchronized void resetDelays() {
        this.windupRemainingTicks = 0;
        this.cooldownRemainingTicks = 0;
    }
}
