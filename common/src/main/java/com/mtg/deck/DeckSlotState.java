package com.mtg.deck;

/**
 * Lifecycle state of an individual slot in the dedicated Planeswalker Deck container.
 */
public enum DeckSlotState {
    /**
     * Unlocked slot with no card equipped.
     */
    EMPTY,

    /**
     * Card is equipped, cooldown is zero, windup is zero, and ready to be cast.
     */
    READY,

    /**
     * Card has been cast and is recovering; radial cooldown wipe mask active.
     */
    COOLDOWN,

    /**
     * Card is actively channeling (e.g. Sorcery delay or cast windup).
     */
    WINDUP,

    /**
     * Slot index exceeds player's currently unlocked deck capacity (requires XP upgrade).
     */
    LOCKED
}
