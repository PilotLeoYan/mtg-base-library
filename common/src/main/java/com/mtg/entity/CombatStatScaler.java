package com.mtg.entity;

/**
 * Combat stat scaling formulas translating MTG Power and Toughness
 * into Minecraft damage and health attributes.
 */
public final class CombatStatScaler {
    public static final float HP_PER_TOUGHNESS = 4.0f;     // 1 Toughness = 4.0 HP (2 Minecraft hearts)
    public static final float DAMAGE_PER_POWER = 2.0f;     // 1 Power = 2.0 damage (1 Minecraft heart)
    public static final float BOSS_DEATHTOUCH_MULTIPLIER = 1.5f; // +50% critical against boss mobs

    private CombatStatScaler() {}

    public static float calculateMaxHealth(int toughness) {
        return Math.max(1.0f, toughness * HP_PER_TOUGHNESS);
    }

    public static float calculateAttackDamage(int power) {
        return Math.max(0.0f, power * DAMAGE_PER_POWER);
    }

    public static float calculateDeathtouchDamage(float baseDamage, float targetMaxHp, boolean isBoss) {
        if (isBoss) {
            return baseDamage * BOSS_DEATHTOUCH_MULTIPLIER;
        }
        return Math.max(baseDamage, targetMaxHp);
    }
}
