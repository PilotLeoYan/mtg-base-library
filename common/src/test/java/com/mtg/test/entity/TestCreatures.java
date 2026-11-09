package com.mtg.test.entity;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.domain.*;

final class TestCreatures {
    private TestCreatures() {}

    static final KeywordDefinition TOXIC = KeywordDefinition.parameterized(
            "mtg:toxic", "Toxic", "Toxic", KeywordCategory.COMBAT);

    static final CardDefinition LLANOWAR_ELVES = CardDefinition.builder("mtg:base/llanowar_elves", "base", "Llanowar Elves")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.CREATURE)
            .addSubtype("Elf")
            .addSubtype("Druid")
            .rarity(CardRarity.COMMON)
            .combatStats(1, 1)
            .baseCooldownTicks(120)
            .windupDelayTicks(10)
            .build();

    static final CardDefinition SERRA_ANGEL = CardDefinition.builder("mtg:base/serra_angel", "base", "Serra Angel")
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

    static final CardDefinition VAMPIRE_NIGHTHAWK = CardDefinition.builder("mtg:base/vampire_nighthawk", "base", "Vampire Nighthawk")
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

    static final CardDefinition GOBLIN_GUIDE = CardDefinition.builder("mtg:base/goblin_guide", "base", "Goblin Guide")
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

    static final CardDefinition PHYREXIAN_RAGER = CardDefinition.builder("mtg:base/phyrexian_rager", "base", "Phyrexian Rager")
            .manaCost(new ManaCost(2, 0, 0, 1, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .addSubtype("Phyrexian")
            .addSubtype("Horror")
            .rarity(CardRarity.COMMON)
            .combatStats(2, 2)
            .baseCooldownTicks(200)
            .windupDelayTicks(20)
            .build();

    static final CardDefinition VENERATED_ROTPRIEST = CardDefinition.builder("mtg:one/venerated_rotpriest", "one", "Venerated Rotpriest")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.CREATURE)
            .addSubtype("Phyrexian")
            .addSubtype("Druid")
            .rarity(CardRarity.RARE)
            .combatStats(1, 2)
            .addKeyword(KeywordInstance.of(TOXIC, 1))
            .baseCooldownTicks(120)
            .windupDelayTicks(10)
            .build();
}
