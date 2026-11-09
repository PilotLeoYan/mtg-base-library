package com.mtg.entity.catalog;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.domain.KeywordInstance;
import com.mtg.api.mechanics.one.PhyrexiaExpansion;
import com.mtg.domain.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Representative creature card definitions for Milestone 4 creature summon invocations.
 */
public final class BaseCreatures {
    private BaseCreatures() {}

    public static final String SET_CODE_BASE = "base";
    public static final String SET_CODE_ONE = "one";

    // Llanowar Elves: {G}, 1/1, Elf Druid, {T}: Add {G}
    public static final CardDefinition LLANOWAR_ELVES = CardDefinition.builder("mtg:base/llanowar_elves", SET_CODE_BASE, "Llanowar Elves")
        .manaCost(ManaCost.ofGreen(1))
        .addType(CardType.CREATURE)
        .addSubtype("Elf")
        .addSubtype("Druid")
        .rarity(CardRarity.COMMON)
        .combatStats(1, 1)
        .baseCooldownTicks(120)
        .windupDelayTicks(10)
        .build();

    // Serra Angel: {3}{W}{W}, 4/4, Angel, Flying, Vigilance
    public static final CardDefinition SERRA_ANGEL = CardDefinition.builder("mtg:base/serra_angel", SET_CODE_BASE, "Serra Angel")
        .manaCost(new ManaCost(3, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE))
        .addType(CardType.CREATURE)
        .addSubtype("Angel")
        .rarity(CardRarity.UNCOMMON)
        .combatStats(4, 4)
        .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
        .addKeyword(KeywordInstance.of(CoreKeywords.VIGILANCE))
        .baseCooldownTicks(300)
        .windupDelayTicks(30)
        .build();

    // Vampire Nighthawk: {1}{B}{B}, 2/3, Vampire Shaman, Flying, Deathtouch, Lifelink
    public static final CardDefinition VAMPIRE_NIGHTHAWK = CardDefinition.builder("mtg:base/vampire_nighthawk", SET_CODE_BASE, "Vampire Nighthawk")
        .manaCost(new ManaCost(1, 0, 0, 2, 0, 0, 0, PhyrexianManaCost.NONE))
        .addType(CardType.CREATURE)
        .addSubtype("Vampire")
        .addSubtype("Shaman")
        .rarity(CardRarity.UNCOMMON)
        .combatStats(2, 3)
        .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
        .addKeyword(KeywordInstance.of(CoreKeywords.DEATHTOUCH))
        .addKeyword(KeywordInstance.of(CoreKeywords.LIFELINK))
        .baseCooldownTicks(240)
        .windupDelayTicks(20)
        .build();

    // Goblin Guide: {R}, 2/2, Goblin Scout, Haste
    public static final CardDefinition GOBLIN_GUIDE = CardDefinition.builder("mtg:base/goblin_guide", SET_CODE_BASE, "Goblin Guide")
        .manaCost(ManaCost.ofRed(1))
        .addType(CardType.CREATURE)
        .addSubtype("Goblin")
        .addSubtype("Scout")
        .rarity(CardRarity.RARE)
        .combatStats(2, 2)
        .addKeyword(KeywordInstance.of(CoreKeywords.HASTE))
        .baseCooldownTicks(100)
        .windupDelayTicks(10)
        .build();

    // Phyrexian Rager: {2}{B}, 2/2, Phyrexian Horror
    public static final CardDefinition PHYREXIAN_RAGER = CardDefinition.builder("mtg:base/phyrexian_rager", SET_CODE_BASE, "Phyrexian Rager")
        .manaCost(new ManaCost(2, 0, 0, 1, 0, 0, 0, PhyrexianManaCost.NONE))
        .addType(CardType.CREATURE)
        .addSubtype("Phyrexian")
        .addSubtype("Horror")
        .rarity(CardRarity.COMMON)
        .combatStats(2, 2)
        .baseCooldownTicks(200)
        .windupDelayTicks(20)
        .build();

    // Air Elemental: {3}{U}{U}, 4/4, Elemental, Flying
    public static final CardDefinition AIR_ELEMENTAL = CardDefinition.builder("mtg:base/air_elemental", SET_CODE_BASE, "Air Elemental")
        .manaCost(new ManaCost(3, 0, 2, 0, 0, 0, 0, PhyrexianManaCost.NONE))
        .addType(CardType.CREATURE)
        .addSubtype("Elemental")
        .rarity(CardRarity.UNCOMMON)
        .combatStats(4, 4)
        .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
        .baseCooldownTicks(280)
        .windupDelayTicks(30)
        .build();

    // Venerated Rotpriest: {G}, 1/2, Phyrexian Druid, Toxic 1
    public static final CardDefinition VENERATED_ROTPRIEST = CardDefinition.builder("mtg:one/venerated_rotpriest", SET_CODE_ONE, "Venerated Rotpriest")
        .manaCost(ManaCost.ofGreen(1))
        .addType(CardType.CREATURE)
        .addSubtype("Phyrexian")
        .addSubtype("Druid")
        .rarity(CardRarity.RARE)
        .combatStats(1, 2)
        .addKeyword(KeywordInstance.of(PhyrexiaExpansion.TOXIC, 1))
        .baseCooldownTicks(120)
        .windupDelayTicks(10)
        .build();

    // Shivan Dragon: {4}{R}{R}, 5/5, Dragon, Flying, Rarity: RARE
    public static final CardDefinition SHIVAN_DRAGON = CardDefinition.builder("mtg:base/shivan_dragon", SET_CODE_BASE, "Shivan Dragon")
        .manaCost(new ManaCost(4, 0, 0, 0, 2, 0, 0, PhyrexianManaCost.NONE))
        .addType(CardType.CREATURE)
        .addSubtype("Dragon")
        .rarity(CardRarity.RARE)
        .combatStats(5, 5)
        .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
        .baseCooldownTicks(360)
        .windupDelayTicks(30)
        .build();

    // Demon of Death's Gate: {6}{B}{B}{B}, 9/9, Demon, Flying, Trample, Rarity: MYTHIC
    public static final CardDefinition DEMON_OF_DEATHS_GATE = CardDefinition.builder("mtg:base/demon_of_deaths_gate", SET_CODE_BASE, "Demon of Death's Gate")
        .manaCost(new ManaCost(6, 0, 0, 3, 0, 0, 0, PhyrexianManaCost.NONE))
        .addType(CardType.CREATURE)
        .addSubtype("Demon")
        .rarity(CardRarity.MYTHIC)
        .combatStats(9, 9)
        .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
        .addKeyword(KeywordInstance.of(CoreKeywords.TRAMPLE))
        .baseCooldownTicks(480)
        .windupDelayTicks(40)
        .build();

    public static List<CardDefinition> getAll() {
        return List.of(
            LLANOWAR_ELVES,
            SERRA_ANGEL,
            VAMPIRE_NIGHTHAWK,
            GOBLIN_GUIDE,
            PHYREXIAN_RAGER,
            AIR_ELEMENTAL,
            VENERATED_ROTPRIEST,
            SHIVAN_DRAGON,
            DEMON_OF_DEATHS_GATE
        );
    }

    public static void registerAll(Consumer<CardDefinition> registry) {
        getAll().forEach(registry);
    }
}
