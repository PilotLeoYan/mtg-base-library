package com.mtg.world.altar;

import com.mtg.config.WorldGenConfig;

import java.util.Random;

/**
 * Procedural grid-based placement evaluator for Planeswalker Altars.
 * Enforces configurable spacing and separation intervals between altar structures.
 */
public final class AltarPlacement {
    private AltarPlacement() {}

    /**
     * Determines whether the specified chunk contains a Planeswalker Altar.
     *
     * @param worldSeed World level seed
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param config World generation configuration
     * @return true if an altar spawns in this chunk, false otherwise
     */
    public static boolean isAltarChunk(long worldSeed, int chunkX, int chunkZ, WorldGenConfig config) {
        if (config != null && !config.enableAltars()) {
            return false;
        }

        int spacing = config != null ? config.altarSpacingChunks() : 32;
        int separation = config != null ? config.altarSeparationChunks() : 8;

        return isAltarChunk(worldSeed, chunkX, chunkZ, spacing, separation);
    }

    /**
     * Raw grid computation with explicit spacing and separation.
     */
    public static boolean isAltarChunk(long worldSeed, int chunkX, int chunkZ, int spacing, int separation) {
        if (spacing <= separation) {
            spacing = separation + 1;
        }

        int cellX = Math.floorDiv(chunkX, spacing);
        int cellZ = Math.floorDiv(chunkZ, spacing);

        long cellSeed = worldSeed ^ (cellX * 987654321L + cellZ * 123456789L);
        Random r = new Random(cellSeed);

        int maxOffset = spacing - separation;
        int targetOffsetX = r.nextInt(Math.max(1, maxOffset));
        int targetOffsetZ = r.nextInt(Math.max(1, maxOffset));

        int targetChunkX = cellX * spacing + targetOffsetX;
        int targetChunkZ = cellZ * spacing + targetOffsetZ;

        return chunkX == targetChunkX && chunkZ == targetChunkZ;
    }
}
