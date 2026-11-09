package com.mtg.deck;

import com.mtg.config.ProgressionConfig;

/**
 * Pure mathematical calculator for XP level cost of dedicated card hand capacity expansions (4 -> 7 slots).
 * Formula: step = currentCapacity - initialHandCapacity
 *          cost = round(handUpgradeBaseXpCost * handUpgradeXpMultiplier^step)
 */
public final class DeckCapacityUpgradeCalculator {

    private DeckCapacityUpgradeCalculator() {}

    public static int calculateUpgradeCost(int currentCapacity) {
        return calculateUpgradeCost(currentCapacity, ProgressionConfig.defaults());
    }

    public static int calculateUpgradeCost(int currentCapacity, ProgressionConfig config) {
        if (config == null) {
            config = ProgressionConfig.defaults();
        }
        if (currentCapacity < config.initialHandCapacity()) {
            return 0;
        }
        if (currentCapacity >= config.maxHandCapacity()) {
            return -1; // Hard cap reached (7 slots)
        }
        int step = currentCapacity - config.initialHandCapacity();
        double cost = config.handUpgradeBaseXpCost() * Math.pow(config.handUpgradeXpMultiplier(), step);
        return (int) Math.round(cost);
    }

    public static boolean canUpgrade(int currentCapacity, int availableXpLevels) {
        return canUpgrade(currentCapacity, availableXpLevels, ProgressionConfig.defaults());
    }

    public static boolean canUpgrade(int currentCapacity, int availableXpLevels, ProgressionConfig config) {
        int cost = calculateUpgradeCost(currentCapacity, config);
        return cost > 0 && availableXpLevels >= cost;
    }
}
