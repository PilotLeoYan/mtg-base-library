package com.mtg.world.boss;

import com.mtg.config.WorldGenConfig;

/**
 * Calculates dynamically scaled stats (HP, attack damage) for World Boss encounters
 * based on multi-player combatant group size and configuration multipliers.
 */
public final class BossStatScaler {
    private BossStatScaler() {}

    /**
     * Calculates scaled maximum health accounting for group size and configuration multipliers.
     * Each additional combatant beyond the first increases health by +35%.
     *
     * @param baseHp Base health of the boss
     * @param activeCombatants Count of players and summons participating in combat (minimum 1)
     * @param config World generation configuration
     * @return Scaled maximum health
     */
    public static float calculateScaledMaxHealth(float baseHp, int activeCombatants, WorldGenConfig config) {
        double configMultiplier = (config != null) ? config.bossHealthMultiplier() : 1.0;
        int extraCombatants = Math.max(0, activeCombatants - 1);
        double groupMultiplier = 1.0 + (0.35 * extraCombatants);
        return (float) (baseHp * configMultiplier * groupMultiplier);
    }

    /**
     * Calculates scaled attack damage based on configuration multiplier.
     *
     * @param baseDamage Base attack damage
     * @param config World generation configuration
     * @return Scaled attack damage
     */
    public static float calculateScaledAttackDamage(float baseDamage, WorldGenConfig config) {
        double configMultiplier = (config != null) ? config.bossDamageMultiplier() : 1.0;
        return (float) (baseDamage * configMultiplier);
    }

    /**
     * Alias for calculateScaledMaxHealth.
     */
    public static float scaleMaxHealth(float baseHp, int activeCombatants, WorldGenConfig config) {
        return calculateScaledMaxHealth(baseHp, activeCombatants, config);
    }

    /**
     * Alias for calculateScaledAttackDamage.
     */
    public static float scaleAttackDamage(float baseDamage, WorldGenConfig config) {
        return calculateScaledAttackDamage(baseDamage, config);
    }
}
