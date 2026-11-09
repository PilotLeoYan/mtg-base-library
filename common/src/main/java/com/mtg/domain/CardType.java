package com.mtg.domain;

public enum CardType {
    LAND,
    CREATURE,
    INSTANT,
    SORCERY,
    ENCHANTMENT,
    ARTIFACT,
    PLANESWALKER;

    public boolean isPermanent() {
        return this == LAND || this == CREATURE || this == ENCHANTMENT || this == ARTIFACT || this == PLANESWALKER;
    }

    public boolean isSpell() {
        return this != LAND;
    }

    public boolean hasCastDelay() {
        return this == SORCERY;
    }
}
