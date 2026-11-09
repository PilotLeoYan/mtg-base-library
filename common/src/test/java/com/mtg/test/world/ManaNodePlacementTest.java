package com.mtg.test.world;

import com.mtg.config.WorldGenConfig;
import com.mtg.world.node.ManaBiomeResolver;
import com.mtg.world.node.ManaNodePlacement;
import com.mtg.world.node.ManaNodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Elemental Mana Node Procedural Placement Suite")
public class ManaNodePlacementTest {

    @Test
    @DisplayName("Deterministic placement: identical seed and coordinates yield identical placement result")
    void testDeterministicPlacement() {
        long worldSeed = 123456789L;
        int chunkX = 42;
        int chunkZ = -17;
        String biome = "minecraft:plains";
        WorldGenConfig config = new WorldGenConfig(true, 1.0, 3, true, 32, 8, true, 0.05, true, 0.01, 1.0, 1.0, true, 10, 3);

        Optional<ManaNodePlacement.PlacementResult> res1 = ManaNodePlacement.evaluateChunk(worldSeed, chunkX, chunkZ, biome, config);
        Optional<ManaNodePlacement.PlacementResult> res2 = ManaNodePlacement.evaluateChunk(worldSeed, chunkX, chunkZ, biome, config);

        assertTrue(res1.isPresent(), "Node must spawn when chance is 1.0");
        assertTrue(res2.isPresent(), "Node must spawn when chance is 1.0");
        assertEquals(res1.get().blockX(), res2.get().blockX());
        assertEquals(res1.get().blockZ(), res2.get().blockZ());
        assertEquals(res1.get().localX(), res2.get().localX());
        assertEquals(res1.get().localZ(), res2.get().localZ());
        assertEquals(res1.get().nodeType(), res2.get().nodeType());
    }

    @Test
    @DisplayName("Seed sensitivity: different world seeds generate different placement coordinates")
    void testSeedSensitivity() {
        int chunkX = 10;
        int chunkZ = 20;
        String biome = "minecraft:mountains";
        WorldGenConfig config = new WorldGenConfig(true, 1.0, 3, true, 32, 8, true, 0.05, true, 0.01, 1.0, 1.0, true, 10, 3);

        Optional<ManaNodePlacement.PlacementResult> res1 = ManaNodePlacement.evaluateChunk(100L, chunkX, chunkZ, biome, config);
        Optional<ManaNodePlacement.PlacementResult> res2 = ManaNodePlacement.evaluateChunk(999999L, chunkX, chunkZ, biome, config);

        assertTrue(res1.isPresent());
        assertTrue(res2.isPresent());
        // Different seeds should produce different local coordinates
        assertNotEquals(res1.get().blockX() + ":" + res1.get().blockZ(),
                res2.get().blockX() + ":" + res2.get().blockZ());
    }

    @Test
    @DisplayName("Local block coordinate constraints: localX and localZ must be strictly between 0 and 15")
    void testLocalCoordinateBounds() {
        long seed = 888888L;
        WorldGenConfig config = new WorldGenConfig(true, 1.0, 3, true, 32, 8, true, 0.05, true, 0.01, 1.0, 1.0, true, 10, 3);

        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                Optional<ManaNodePlacement.PlacementResult> res = ManaNodePlacement.evaluateChunk(seed, cx, cz, "minecraft:forest", config);
                assertTrue(res.isPresent());
                int lx = res.get().localX();
                int lz = res.get().localZ();
                assertTrue(lx >= 0 && lx < 16, "Local X must be in [0, 15]");
                assertTrue(lz >= 0 && lz < 16, "Local Z must be in [0, 15]");
                assertEquals((cx << 4) + lx, res.get().blockX());
                assertEquals((cz << 4) + lz, res.get().blockZ());
            }
        }
    }

    @Test
    @DisplayName("Biome resolution assigns correct ManaNodeType to nodes")
    void testBiomeResolution() {
        assertEquals(ManaNodeType.PLAINS_NODE, ManaBiomeResolver.resolve("minecraft:plains"));
        assertEquals(ManaNodeType.PLAINS_NODE, ManaBiomeResolver.resolve("minecraft:meadow"));
        assertEquals(ManaNodeType.ISLAND_NODE, ManaBiomeResolver.resolve("minecraft:ocean"));
        assertEquals(ManaNodeType.ISLAND_NODE, ManaBiomeResolver.resolve("minecraft:deep_ocean"));
        assertEquals(ManaNodeType.SWAMP_NODE, ManaBiomeResolver.resolve("minecraft:swamp"));
        assertEquals(ManaNodeType.SWAMP_NODE, ManaBiomeResolver.resolve("minecraft:mangrove_swamp"));
        assertEquals(ManaNodeType.MOUNTAIN_NODE, ManaBiomeResolver.resolve("minecraft:desert"));
        assertEquals(ManaNodeType.MOUNTAIN_NODE, ManaBiomeResolver.resolve("minecraft:badlands"));
        assertEquals(ManaNodeType.FOREST_NODE, ManaBiomeResolver.resolve("minecraft:forest"));
        assertEquals(ManaNodeType.FOREST_NODE, ManaBiomeResolver.resolve("minecraft:jungle"));
        assertEquals(ManaNodeType.WASTES_NODE, ManaBiomeResolver.resolve("minecraft:the_end"));
        assertEquals(ManaNodeType.WASTES_NODE, ManaBiomeResolver.resolve("minecraft:deep_dark"));
    }

    @Test
    @DisplayName("Config disable flag prevents node generation completely")
    void testDisabledConfig() {
        WorldGenConfig disabledConfig = new WorldGenConfig(false, 1.0, 3, true, 32, 8, true, 0.05, true, 0.01, 1.0, 1.0, true, 10, 3);
        Optional<ManaNodePlacement.PlacementResult> res = ManaNodePlacement.evaluateChunk(12345L, 0, 0, "minecraft:plains", disabledConfig);
        assertTrue(res.isEmpty(), "Disabled mana nodes should never spawn");
    }
}
