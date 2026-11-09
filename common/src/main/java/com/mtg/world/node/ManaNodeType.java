package com.mtg.world.node;

import com.mtg.domain.ManaColor;

/**
 * The six elemental mana node types corresponding to the WUBRGC mana colors.
 */
public enum ManaNodeType {
    PLAINS_NODE(ManaColor.WHITE, "block.mtg.elemental_node_plains", "item.mtg.mana_gem_white", "Plains Elemental Mana Node"),
    ISLAND_NODE(ManaColor.BLUE, "block.mtg.elemental_node_island", "item.mtg.mana_gem_blue", "Island Elemental Mana Node"),
    SWAMP_NODE(ManaColor.BLACK, "block.mtg.elemental_node_swamp", "item.mtg.mana_gem_black", "Swamp Elemental Mana Node"),
    MOUNTAIN_NODE(ManaColor.RED, "block.mtg.elemental_node_mountain", "item.mtg.mana_gem_red", "Mountain Elemental Mana Node"),
    FOREST_NODE(ManaColor.GREEN, "block.mtg.elemental_node_forest", "item.mtg.mana_gem_green", "Forest Elemental Mana Node"),
    WASTES_NODE(ManaColor.COLORLESS, "block.mtg.elemental_node_wastes", "item.mtg.mana_gem_colorless", "Wastes Elemental Mana Node");

    private final ManaColor manaColor;
    private final String blockTranslationKey;
    private final String gemTranslationKey;
    private final String displayName;

    ManaNodeType(ManaColor manaColor, String blockTranslationKey, String gemTranslationKey, String displayName) {
        this.manaColor = manaColor;
        this.blockTranslationKey = blockTranslationKey;
        this.gemTranslationKey = gemTranslationKey;
        this.displayName = displayName;
    }

    public ManaColor getManaColor() {
        return manaColor;
    }

    public String getBlockTranslationKey() {
        return blockTranslationKey;
    }

    public String getGemTranslationKey() {
        return gemTranslationKey;
    }

    public String getDisplayName() {
        return displayName;
    }
}
