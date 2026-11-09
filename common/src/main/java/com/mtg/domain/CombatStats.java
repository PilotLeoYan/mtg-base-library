package com.mtg.domain;

public record CombatStats(int power, int toughness) {
    public CombatStats add(int powerDelta, int toughnessDelta) {
        return new CombatStats(this.power + powerDelta, this.toughness + toughnessDelta);
    }

    public boolean isLethal(int damageTaken) {
        return damageTaken >= this.toughness;
    }
}
