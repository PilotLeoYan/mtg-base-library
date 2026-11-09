package com.mtg.test.e2e.tier1;

import com.mtg.config.ManaConfig;
import com.mtg.domain.ManaColor;
import com.mtg.mana.BiomeManaManager;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 07: Biome-Affinity Periodic Mana Regeneration E2E Test")
public class Feature07_BiomeManaRegenTest {

    private BiomeManaManager manager;
    private ManaPool pool;

    @BeforeEach
    void setUp() {
        manager = new BiomeManaManager(ManaConfig.defaults());
        pool = new ManaPool(UUID.randomUUID(), 20);
        pool.clear();
    }

    @Test
    @DisplayName("1. Plains biome regenerates White mana into player pool")
    void testPlainsBiomeWhiteManaRegeneration() {
        assertEquals(ManaColor.WHITE, manager.getBiomeAffinityColor("minecraft:plains"));
        int regenerated = manager.tickRegen(pool, "minecraft:plains", 0);

        assertTrue(regenerated > 0, "Regen should award mana");
        assertEquals(regenerated, pool.get(ManaColor.WHITE));
        assertEquals(0, pool.get(ManaColor.BLUE));
    }

    @Test
    @DisplayName("2. Forest biome regenerates Green mana into player pool")
    void testForestBiomeGreenManaRegeneration() {
        assertEquals(ManaColor.GREEN, manager.getBiomeAffinityColor("minecraft:forest"));
        int regenerated = manager.tickRegen(pool, "minecraft:forest", 0);

        assertTrue(regenerated > 0);
        assertEquals(regenerated, pool.get(ManaColor.GREEN));
    }

    @Test
    @DisplayName("3. Ocean biome regenerates Blue mana into player pool")
    void testOceanBiomeBlueManaRegeneration() {
        assertEquals(ManaColor.BLUE, manager.getBiomeAffinityColor("minecraft:ocean"));
        int regenerated = manager.tickRegen(pool, "minecraft:ocean", 0);

        assertTrue(regenerated > 0);
        assertEquals(regenerated, pool.get(ManaColor.BLUE));
    }

    @Test
    @DisplayName("4. Swamp biome regenerates Black mana into player pool")
    void testSwampBiomeBlackManaRegeneration() {
        assertEquals(ManaColor.BLACK, manager.getBiomeAffinityColor("minecraft:swamp"));
        int regenerated = manager.tickRegen(pool, "minecraft:swamp", 0);

        assertTrue(regenerated > 0);
        assertEquals(regenerated, pool.get(ManaColor.BLACK));
    }

    @Test
    @DisplayName("5. Badlands biome regenerates Red mana with Land bonus")
    void testBadlandsBiomeRedManaRegenerationWithLandBonus() {
        assertEquals(ManaColor.RED, manager.getBiomeAffinityColor("minecraft:badlands"));
        int regenerated = manager.tickRegen(pool, "minecraft:badlands", 2);

        // Base regen (1) + Land bonus (2) = 3 Red mana
        assertEquals(3, regenerated);
        assertEquals(3, pool.get(ManaColor.RED));
    }
}
