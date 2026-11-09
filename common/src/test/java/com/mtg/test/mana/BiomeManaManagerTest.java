package com.mtg.test.mana;

import com.mtg.config.ManaConfig;
import com.mtg.domain.ManaColor;
import com.mtg.mana.BiomeManaManager;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Subsystem — BiomeManaManager Affinity & Generation Suite")
public class BiomeManaManagerTest {

    @Test
    @DisplayName("Vanilla biome strings map to correct MTG color wheel affinities")
    void testVanillaBiomeColorMapping() {
        BiomeManaManager manager = new BiomeManaManager(ManaConfig.defaults());

        assertEquals(ManaColor.WHITE, manager.getBiomeAffinityColor("minecraft:plains"));
        assertEquals(ManaColor.WHITE, manager.getBiomeAffinityColor("minecraft:sunflower_plains"));
        assertEquals(ManaColor.WHITE, manager.getBiomeAffinityColor("minecraft:meadow"));

        assertEquals(ManaColor.BLUE, manager.getBiomeAffinityColor("minecraft:ocean"));
        assertEquals(ManaColor.BLUE, manager.getBiomeAffinityColor("minecraft:deep_ocean"));
        assertEquals(ManaColor.BLUE, manager.getBiomeAffinityColor("minecraft:river"));

        assertEquals(ManaColor.BLACK, manager.getBiomeAffinityColor("minecraft:swamp"));
        assertEquals(ManaColor.BLACK, manager.getBiomeAffinityColor("minecraft:mangrove_swamp"));
        assertEquals(ManaColor.BLACK, manager.getBiomeAffinityColor("minecraft:deep_dark"));

        assertEquals(ManaColor.RED, manager.getBiomeAffinityColor("minecraft:desert"));
        assertEquals(ManaColor.RED, manager.getBiomeAffinityColor("minecraft:badlands"));
        assertEquals(ManaColor.RED, manager.getBiomeAffinityColor("minecraft:nether_wastes"));

        assertEquals(ManaColor.GREEN, manager.getBiomeAffinityColor("minecraft:forest"));
        assertEquals(ManaColor.GREEN, manager.getBiomeAffinityColor("minecraft:taiga"));
        assertEquals(ManaColor.GREEN, manager.getBiomeAffinityColor("minecraft:jungle"));

        assertEquals(ManaColor.COLORLESS, manager.getBiomeAffinityColor("minecraft:the_end"));
        assertEquals(ManaColor.COLORLESS, manager.getBiomeAffinityColor("custom:unregistered_biome"));
        assertEquals(ManaColor.COLORLESS, manager.getBiomeAffinityColor(null));
    }

    @Test
    @DisplayName("Periodic tick interval: regeneration fires every 100 ticks")
    void testRegenTickIntervalEnforcement() {
        BiomeManaManager manager = new BiomeManaManager(ManaConfig.defaults());

        assertFalse(manager.shouldRegen(0));
        assertFalse(manager.shouldRegen(50));
        assertFalse(manager.shouldRegen(99));
        assertTrue(manager.shouldRegen(100));
        assertFalse(manager.shouldRegen(101));
        assertTrue(manager.shouldRegen(200));
    }

    @Test
    @DisplayName("Land mana bonus stacks onto base regeneration amount")
    void testLandManaBonusIncreasesRegen() {
        BiomeManaManager manager = new BiomeManaManager(ManaConfig.defaults());
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);

        int added = manager.tickRegen(pool, "minecraft:forest", 2); // 1 base + 2 land = 3
        assertEquals(3, added);
        assertEquals(3, pool.get(ManaColor.GREEN));
        assertEquals(3, pool.getTotalMana());
    }

    @Test
    @DisplayName("Regeneration pulse is clamped at max pool capacity")
    void testRegenCappedAtMaxMana() {
        BiomeManaManager manager = new BiomeManaManager(ManaConfig.defaults());
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.BLUE, 19);

        int added = manager.tickRegen(pool, "minecraft:ocean", 3); // Attempts to add 4 (1 + 3)
        assertEquals(1, added, "Should only add 1 to reach capacity 20");
        assertEquals(20, pool.get(ManaColor.BLUE));
        assertEquals(20, pool.getTotalMana());
    }

    @Test
    @DisplayName("Decay tick reduces unspent mana starting with Colorless then highest color")
    void testDecayReducesUnspentMana() {
        BiomeManaManager manager = new BiomeManaManager(ManaConfig.defaults());
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.COLORLESS, 2);
        pool.addMana(ManaColor.RED, 5);

        int decayed = manager.tickDecay(pool); // decayAmount default is 1
        assertEquals(1, decayed);
        assertEquals(1, pool.get(ManaColor.COLORLESS), "Colorless should decay first");
        assertEquals(5, pool.get(ManaColor.RED));

        manager.tickDecay(pool); // second decay consumes remaining Colorless
        assertEquals(0, pool.get(ManaColor.COLORLESS));
        assertEquals(5, pool.get(ManaColor.RED));

        manager.tickDecay(pool); // third decay consumes Red
        assertEquals(4, pool.get(ManaColor.RED));
    }
}
