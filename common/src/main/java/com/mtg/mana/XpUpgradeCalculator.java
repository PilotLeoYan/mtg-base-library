package com.mtg.mana;

import com.mtg.config.ManaConfig;

/**
 * Pure mathematical calculator for XP level cost scaling of permanent max mana capacity upgrades.
 * Formula: tier = (currentMaxMana - initialMaxMana) / upgradeStep
 *          cost = max(1, round(baseXpCost * multiplier^tier))
 * Returns -1 if currentMaxMana >= hardCapMaxMana.
 */
public final class XpUpgradeCalculator {
    public static final int DEFAULT_UPGRADE_STEP = 5;
    public static final int DEFAULT_INITIAL_MAX_MANA = 20;
    public static final int DEFAULT_HARD_CAP_MAX_MANA = 100;
    public static final int DEFAULT_BASE_XP_COST = 5;
    public static final double DEFAULT_XP_MULTIPLIER = 1.25;

    private XpUpgradeCalculator() {}

    public static int getNextUpgradeCost(int currentMaxMana) {
        return getNextUpgradeCost(
                currentMaxMana,
                DEFAULT_INITIAL_MAX_MANA,
                DEFAULT_HARD_CAP_MAX_MANA,
                DEFAULT_BASE_XP_COST,
                DEFAULT_XP_MULTIPLIER,
                DEFAULT_UPGRADE_STEP
        );
    }

    public static int getNextUpgradeCost(int currentMaxMana, ManaConfig config) {
        if (config == null) {
            return getNextUpgradeCost(currentMaxMana);
        }
        return getNextUpgradeCost(
                currentMaxMana,
                config.initialMaxMana(),
                config.hardCapMaxMana(),
                config.baseXpLevelCost(),
                config.manaUpgradeXpMultiplier(),
                DEFAULT_UPGRADE_STEP
        );
    }

    public static int getNextUpgradeCost(int currentMaxMana, int initialMaxMana, int hardCapMaxMana,
                                         int baseXpCost, double multiplier, int upgradeStep) {
        if (currentMaxMana >= hardCapMaxMana) {
            return -1;
        }
        int step = Math.max(1, upgradeStep);
        int tier = Math.max(0, (currentMaxMana - initialMaxMana) / step);
        double cost = baseXpCost * Math.pow(multiplier, tier);
        return Math.max(1, (int) Math.round(cost));
    }

    public static boolean canUpgrade(int currentMaxMana, int hardCapMaxMana, int playerXpLevels, int cost) {
        if (currentMaxMana >= hardCapMaxMana) return false;
        return cost > 0 && playerXpLevels >= cost;
    }

    public static int calculateNewMaxMana(int currentMaxMana, int hardCapMaxMana, int upgradeStep) {
        return Math.min(hardCapMaxMana, currentMaxMana + Math.max(1, upgradeStep));
    }
}
