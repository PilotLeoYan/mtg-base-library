package com.mtg.world.altar;

/**
 * Types of rituals executable at a Planeswalker Altar.
 */
public enum AltarRitualType {
    /**
     * Replenishes current player mana for the offered gem's color up to max cap.
     */
    MANA_REPLENISHMENT,

    /**
     * Offering 1 of each of 5 colored gems (WUBRG) overcharges the mana pool
     * beyond max cap (+5) and awards 1 Rare/Mythic wildcard.
     */
    CONFLUX_OVERCHARGE,

    /**
     * Offering a Mana Gem alongside Planeswalker focus (XP levels) summons a Mythic World Boss.
     */
    BOSS_SUMMON
}
