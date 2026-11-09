package com.mtg.api.keyword;

public final class CoreKeywords {
    private CoreKeywords() {}

    public static final KeywordDefinition FLYING = KeywordDefinition.of(
        "mtg:flying", "Flying",
        "This creature can only be blocked or hit by creatures with flying or reach.",
        KeywordCategory.STATIC
    );

    public static final KeywordDefinition DEATHTOUCH = KeywordDefinition.of(
        "mtg:deathtouch", "Deathtouch",
        "Any amount of damage this deals to a creature is lethal.",
        KeywordCategory.COMBAT
    );

    public static final KeywordDefinition LIFELINK = KeywordDefinition.of(
        "mtg:lifelink", "Lifelink",
        "Damage dealt by this source heals its caster for the same amount.",
        KeywordCategory.TRIGGERED
    );

    public static final KeywordDefinition FIRST_STRIKE = KeywordDefinition.of(
        "mtg:first_strike", "First Strike",
        "Deals combat damage before entities without first strike.",
        KeywordCategory.COMBAT
    );

    public static final KeywordDefinition TRAMPLE = KeywordDefinition.of(
        "mtg:trample", "Trample",
        "Excess combat damage penetrates to nearby enemy entities.",
        KeywordCategory.COMBAT
    );

    public static final KeywordDefinition HASTE = KeywordDefinition.of(
        "mtg:haste", "Haste",
        "This creature can attack immediately upon entering the battlefield.",
        KeywordCategory.STATIC
    );

    public static final KeywordDefinition VIGILANCE = KeywordDefinition.of(
        "mtg:vigilance", "Vigilance",
        "Attacking does not cause this creature to enter defense cooldown.",
        KeywordCategory.STATIC
    );

    public static final KeywordDefinition HEXPROOF = KeywordDefinition.of(
        "mtg:hexproof", "Hexproof",
        "Cannot be targeted by spells or abilities controlled by opponents.",
        KeywordCategory.STATIC
    );

    public static final KeywordDefinition REACH = KeywordDefinition.of(
        "mtg:reach", "Reach",
        "This creature can block and hit creatures with flying.",
        KeywordCategory.STATIC
    );

    public static final KeywordDefinition DEFENDER = KeywordDefinition.of(
        "mtg:defender", "Defender",
        "This creature cannot attack.",
        KeywordCategory.STATIC
    );
}
