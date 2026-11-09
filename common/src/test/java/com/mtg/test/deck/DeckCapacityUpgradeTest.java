package com.mtg.test.deck;

import com.mtg.config.ProgressionConfig;
import com.mtg.deck.DeckCapacityUpgradeCalculator;
import com.mtg.deck.DeckManager;
import com.mtg.deck.PlaneswalkerDeck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dedicated Card Hand — DeckCapacityUpgrade XP Scaling Suite")
public class DeckCapacityUpgradeTest {

    private ProgressionConfig config;

    @BeforeEach
    void setup() {
        config = ProgressionConfig.defaults();
        DeckManager.getInstance().clear();
    }

    @Test
    @DisplayName("XP upgrade costs follow exponential progression: 4->5 (10 lvl), 5->6 (15 lvl), 6->7 (23 lvl)")
    void testUpgradeCostFormula() {
        // Step 0: 10 * 1.5^0 = 10
        assertEquals(10, DeckCapacityUpgradeCalculator.calculateUpgradeCost(4, config));

        // Step 1: 10 * 1.5^1 = 15
        assertEquals(15, DeckCapacityUpgradeCalculator.calculateUpgradeCost(5, config));

        // Step 2: 10 * 1.5^2 = 22.5 -> round = 23
        assertEquals(23, DeckCapacityUpgradeCalculator.calculateUpgradeCost(6, config));
    }

    @Test
    @DisplayName("Hard cap at 7 slots returns -1 cost and canUpgrade returns false")
    void testMaxCapacityReached() {
        assertEquals(-1, DeckCapacityUpgradeCalculator.calculateUpgradeCost(7, config));
        assertFalse(DeckCapacityUpgradeCalculator.canUpgrade(7, 100, config));
    }

    @Test
    @DisplayName("Upgrade is rejected when player possesses fewer XP levels than cost")
    void testInsufficientXpRejection() {
        assertFalse(DeckCapacityUpgradeCalculator.canUpgrade(4, 9, config));
        assertTrue(DeckCapacityUpgradeCalculator.canUpgrade(4, 10, config));

        assertFalse(DeckCapacityUpgradeCalculator.canUpgrade(5, 14, config));
        assertTrue(DeckCapacityUpgradeCalculator.canUpgrade(5, 15, config));
    }

    @Test
    @DisplayName("DeckManager.upgradeHandCapacity integrates XP deduction check and capacity upgrade")
    void testDeckManagerUpgradeIntegration() {
        UUID playerId = UUID.randomUUID();
        DeckManager manager = DeckManager.getInstance();
        PlaneswalkerDeck deck = manager.getDeck(playerId);

        assertEquals(4, deck.getCapacity());

        // Insufficient XP (9 < 10)
        assertFalse(manager.upgradeHandCapacity(playerId, 9));
        assertEquals(4, deck.getCapacity());

        // Sufficient XP (10 >= 10)
        assertTrue(manager.upgradeHandCapacity(playerId, 10));
        assertEquals(5, deck.getCapacity());

        // Second upgrade: 5 -> 6 requires 15
        assertFalse(manager.upgradeHandCapacity(playerId, 14));
        assertTrue(manager.upgradeHandCapacity(playerId, 15));
        assertEquals(6, deck.getCapacity());

        // Third upgrade: 6 -> 7 requires 23
        assertTrue(manager.upgradeHandCapacity(playerId, 25));
        assertEquals(7, deck.getCapacity());

        // Attempting upgrade past 7 fails
        assertFalse(manager.upgradeHandCapacity(playerId, 100));
        assertEquals(7, deck.getCapacity());
    }
}
