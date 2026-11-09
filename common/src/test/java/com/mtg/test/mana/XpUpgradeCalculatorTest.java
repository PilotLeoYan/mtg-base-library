package com.mtg.test.mana;

import com.mtg.mana.XpUpgradeCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Subsystem — XpUpgradeCalculator Exponential Scaling Suite")
public class XpUpgradeCalculatorTest {

    @Test
    @DisplayName("Tier 0 upgrade (20 -> 25 max mana) costs exactly 5 XP levels")
    void testTier0Cost() {
        int cost = XpUpgradeCalculator.getNextUpgradeCost(20);
        assertEquals(5, cost);
    }

    @Test
    @DisplayName("Tier 1 upgrade (25 -> 30 max mana) scales to 6 XP levels")
    void testTier1Cost() {
        int cost = XpUpgradeCalculator.getNextUpgradeCost(25);
        assertEquals(6, cost); // round(5 * 1.25^1) = round(6.25) = 6
    }

    @Test
    @DisplayName("Exponential curve increases cost strictly across ascending tiers")
    void testExponentialScalingCurve() {
        int costTier0 = XpUpgradeCalculator.getNextUpgradeCost(20); // 5
        int costTier1 = XpUpgradeCalculator.getNextUpgradeCost(25); // 6
        int costTier2 = XpUpgradeCalculator.getNextUpgradeCost(30); // 8 (5 * 1.5625 = 7.81)
        int costTier3 = XpUpgradeCalculator.getNextUpgradeCost(35); // 10 (5 * 1.953 = 9.77)
        int costTier4 = XpUpgradeCalculator.getNextUpgradeCost(40); // 12 (5 * 2.441 = 12.21)

        assertEquals(5, costTier0);
        assertEquals(6, costTier1);
        assertEquals(8, costTier2);
        assertEquals(10, costTier3);
        assertEquals(12, costTier4);

        assertTrue(costTier1 > costTier0);
        assertTrue(costTier2 > costTier1);
        assertTrue(costTier3 > costTier2);
        assertTrue(costTier4 > costTier3);
    }

    @Test
    @DisplayName("Hard cap capacity (100) returns -1 cost disabling further upgrades")
    void testHardCapCostReturnsNegativeOne() {
        assertEquals(-1, XpUpgradeCalculator.getNextUpgradeCost(100));
        assertEquals(-1, XpUpgradeCalculator.getNextUpgradeCost(105));
    }

    @Test
    @DisplayName("canUpgrade accurately validates available XP levels against required cost")
    void testCanUpgradeValidation() {
        int costTier0 = XpUpgradeCalculator.getNextUpgradeCost(20); // 5
        assertFalse(XpUpgradeCalculator.canUpgrade(20, 100, 4, costTier0));
        assertTrue(XpUpgradeCalculator.canUpgrade(20, 100, 5, costTier0));
        assertTrue(XpUpgradeCalculator.canUpgrade(20, 100, 10, costTier0));

        // Rejection when at hard cap
        assertFalse(XpUpgradeCalculator.canUpgrade(100, 100, 50, -1));
    }

    @Test
    @DisplayName("calculateNewMaxMana clamps properly to hard cap")
    void testCalculateNewMaxMana() {
        assertEquals(25, XpUpgradeCalculator.calculateNewMaxMana(20, 100, 5));
        assertEquals(100, XpUpgradeCalculator.calculateNewMaxMana(95, 100, 5));
        assertEquals(100, XpUpgradeCalculator.calculateNewMaxMana(98, 100, 5));
    }
}
