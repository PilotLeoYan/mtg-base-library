package com.mtg.world.altar;

import java.util.Objects;

/**
 * Immutable definition and layout specification of a Planeswalker Altar structure.
 */
public record AltarDefinition(
    String structureId,
    String blockTranslationKey,
    int width,
    int height,
    int length
) {
    public static final String DEFAULT_STRUCTURE_ID = "mtg:planeswalker_altar";
    public static final String DEFAULT_BLOCK_KEY = "block.mtg.mana_altar";

    public AltarDefinition {
        Objects.requireNonNull(structureId, "StructureId cannot be null");
        Objects.requireNonNull(blockTranslationKey, "BlockTranslationKey cannot be null");
    }

    public static AltarDefinition standard() {
        return new AltarDefinition(DEFAULT_STRUCTURE_ID, DEFAULT_BLOCK_KEY, 7, 5, 7);
    }
}
