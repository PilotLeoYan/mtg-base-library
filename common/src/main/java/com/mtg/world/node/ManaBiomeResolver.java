package com.mtg.world.node;

import java.util.Locale;

/**
 * Resolves Minecraft biome identifiers and category strings to their corresponding ManaNodeType.
 */
public final class ManaBiomeResolver {
    private ManaBiomeResolver() {}

    /**
     * Resolves a Minecraft biome ID (e.g. "minecraft:plains", "minecraft:deep_ocean") to a ManaNodeType.
     *
     * @param biomeId The resource location or name of the biome
     * @return The corresponding ManaNodeType, defaulting to PLAINS_NODE if unrecognized
     */
    public static ManaNodeType resolve(String biomeId) {
        if (biomeId == null || biomeId.isBlank()) {
            return ManaNodeType.PLAINS_NODE;
        }

        String lower = biomeId.toLowerCase(Locale.ROOT);

        // Colorless / Eldritch / Void
        if (lower.contains("the_end") || lower.contains("end_highlands") || lower.contains("end_barrens")
                || lower.contains("deep_dark") || lower.contains("mushroom")) {
            return ManaNodeType.WASTES_NODE;
        }

        // Blue / Oceanic / River
        if (lower.contains("ocean") || lower.contains("river") || lower.contains("beach")) {
            return ManaNodeType.ISLAND_NODE;
        }

        // Black / Swamp / Corrupted
        if (lower.contains("swamp") || lower.contains("mangrove") || lower.contains("soul_sand")) {
            return ManaNodeType.SWAMP_NODE;
        }

        // Red / Mountain / Desert / Volcanic
        if (lower.contains("mountain") || lower.contains("badlands") || lower.contains("desert")
                || lower.contains("peaks") || lower.contains("crimson") || lower.contains("basalt")
                || lower.contains("nether_wastes")) {
            return ManaNodeType.MOUNTAIN_NODE;
        }

        // Green / Forest / Jungle
        if (lower.contains("forest") || lower.contains("jungle") || lower.contains("taiga")
                || lower.contains("lush_caves") || lower.contains("grove")) {
            return ManaNodeType.FOREST_NODE;
        }

        // White / Plains / Savannah / Meadow
        if (lower.contains("plains") || lower.contains("meadow") || lower.contains("savanna")) {
            return ManaNodeType.PLAINS_NODE;
        }

        return ManaNodeType.PLAINS_NODE;
    }
}
