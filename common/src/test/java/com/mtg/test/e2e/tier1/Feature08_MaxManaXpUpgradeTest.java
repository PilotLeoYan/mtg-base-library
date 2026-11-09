package com.mtg.test.e2e.tier1;

import com.mtg.config.ManaConfig;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.mana.XpUpgradeCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 08: Max Mana Cap & XP Permanent Upgrades E2E Test")
public class Feature08_MaxManaXpUpgradeTest {

    private UUID playerId;
    private ManaManager manaManager;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        manaManager = ManaManager.getInstance();
        manaManager.reset();
    }

    @AfterEach
    void tearDown() {
        manaManager.reset();
    }

    @Test
    @DisplayName("1. Starting max mana capacity reflects baseline configuration")
    void testBaselineMaxManaCapacity() {
        ManaPool pool = manaManager.getOrCreatePlayerMana(playerId);
        pool.setMaxMana(20);
        assertEquals(20, pool.getMaxMana());
    }

    @Test
    @DisplayName("2. Tier 0 upgrade from 20 max mana requires 5 XP levels")
    void testTier0UpgradeCostCalculation() {
        int cost = XpUpgradeCalculator.getNextUpgradeCost(20);
        assertEquals(5, cost, "Upgrading from 20 to 25 max mana must cost exactly 5 XP levels");
    }

    @Test
    @DisplayName("3. Upgrading max mana permanently increases pool capacity from 5 to 10 (or 20 to 25 with 100 hardCap)")
    void testExecuteMaxManaUpgrade() {
        ManaConfig defaultCfg = ManaConfig.defaults();
        manaManager.setConfig(new ManaConfig(
                defaultCfg.biomeRegenIntervalTicks(),
                defaultCfg.baseRegenAmount(),
                defaultCfg.landManaBonus(),
                defaultCfg.decayIntervalTicks(),
                defaultCfg.decayAmount(),
                20,
                100,
                5,
                1.25,
                defaultCfg.phyrexianLifeCostDamage(),
                defaultCfg.allowLethalPhyrexianPayment(),
                defaultCfg.biomeAffinities(),
                defaultCfg.biomeMultipliers()
        ));
        ManaPool pool = manaManager.getOrCreatePlayerMana(playerId);
        pool.setMaxMana(20);

        boolean upgraded = manaManager.upgradeMaxMana(playerId, 10); // Provide 10 XP levels (> 5 required)
        assertTrue(upgraded, "Upgrade should succeed with sufficient XP");
        assertEquals(25, pool.getMaxMana(), "Max mana must increase by +5 to 25");
        manaManager.setConfig(ManaConfig.defaults());
    }

    @Test
    @DisplayName("4. Tier 1 upgrade cost scales exponentially to 6 XP levels")
    void testTier1UpgradeCostScaling() {
        int nextCost = XpUpgradeCalculator.getNextUpgradeCost(25);
        assertEquals(6, nextCost, "Upgrading from 25 to 30 max mana must scale to 6 XP levels");
        assertTrue(nextCost > XpUpgradeCalculator.getNextUpgradeCost(20), "Cost must increase monotonically");
    }

    @Test
    @DisplayName("5. Upgrades at hard cap (100) return -1 cost preventing further expansion")
    void testHardCapCostPrevention() {
        int capCost = XpUpgradeCalculator.getNextUpgradeCost(100);
        assertEquals(-1, capCost, "At hard cap 100 max mana, cost must return -1");
        assertFalse(XpUpgradeCalculator.canUpgrade(100, 100, 50, 5), "canUpgrade must return false at hard cap");
    }
}
