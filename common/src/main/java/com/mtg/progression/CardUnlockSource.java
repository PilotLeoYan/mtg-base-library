package com.mtg.progression;

/**
 * Enumerates gameplay activities through which cards may be discovered or unlocked.
 */
public enum CardUnlockSource {
    MINING("mining"),
    FARMING("farming"),
    MOB_DROP("mob_drop"),
    BOSS_DROP("boss_drop"),
    DUNGEON_CHEST("dungeon_chest"),
    STARTER_PACK("starter_pack"),
    WILDCARD_REDEMPTION("wildcard_redemption"),
    COMMAND("command");

    private final String id;

    CardUnlockSource(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
