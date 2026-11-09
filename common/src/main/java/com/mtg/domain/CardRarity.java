package com.mtg.domain;

public enum CardRarity {
    COMMON("common", 0xFFFFFF, 1),
    UNCOMMON("uncommon", 0x708090, 2),
    RARE("rare", 0xDAA520, 3),
    MYTHIC("mythic", 0xFF4500, 4);

    private final String id;
    private final int hexColor;
    private final int tier;

    CardRarity(String id, int hexColor, int tier) {
        this.id = id;
        this.hexColor = hexColor;
        this.tier = tier;
    }

    public String getId() { return id; }
    public int getHexColor() { return hexColor; }
    public int getTier() { return tier; }
}
