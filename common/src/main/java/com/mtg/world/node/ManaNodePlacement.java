package com.mtg.world.node;

import com.mtg.config.WorldGenConfig;

import java.util.Optional;
import java.util.Random;

/**
 * Deterministic chunk-level placement calculator for Elemental Mana Nodes.
 * Uses 64-bit coordinate hashing to evaluate node presence and coordinates.
 */
public final class ManaNodePlacement {
    private ManaNodePlacement() {}

    public record PlacementResult(
        ManaNodeType nodeType,
        int blockX,
        int blockZ,
        int localX,
        int localZ
    ) {}

    /**
     * Evaluates whether a mana node spawns in the given chunk and computes its placement details.
     *
     * @param worldSeed World level seed
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param biomeId Biome identifier string for this chunk
     * @param config World generation configuration
     * @return Optional containing PlacementResult if a node spawns, or empty if not
     */
    public static Optional<PlacementResult> evaluateChunk(
            long worldSeed,
            int chunkX,
            int chunkZ,
            String biomeId,
            WorldGenConfig config
    ) {
        if (config != null && !config.enableManaNodes()) {
            return Optional.empty();
        }

        double chance = config != null ? config.manaNodeChunkChance() : 0.02;

        long seed = worldSeed ^ (chunkX * 341873128712L + chunkZ * 132897987541L);
        Random r = new Random(seed);

        if (r.nextDouble() >= chance) {
            return Optional.empty();
        }

        ManaNodeType type = ManaBiomeResolver.resolve(biomeId);
        int localX = r.nextInt(16);
        int localZ = r.nextInt(16);
        int blockX = (chunkX << 4) + localX;
        int blockZ = (chunkZ << 4) + localZ;

        return Optional.of(new PlacementResult(type, blockX, blockZ, localX, localZ));
    }

    /**
     * Helper to check if a chunk contains a mana node without resolving coordinates.
     */
    public static boolean doesChunkContainNode(long worldSeed, int chunkX, int chunkZ, double chunkChance) {
        long seed = worldSeed ^ (chunkX * 341873128712L + chunkZ * 132897987541L);
        Random r = new Random(seed);
        return r.nextDouble() < chunkChance;
    }
}
