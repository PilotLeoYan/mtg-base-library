package com.mtg.test.e2e.tier1;

import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.cards.effects.CardEffectRegistry;
import com.mtg.cards.effects.CardEffectResult;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.KeywordInstance;
import com.mtg.domain.ManaCost;
import com.mtg.domain.PhyrexianManaCost;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Test helper providing synthetic MTG cards and effects for Tier 1 E2E tests,
 * while utilizing the pure Base Library (BasicLandsCatalog, etc.).
 */
public final class E2ETestExpansionHelper {
    private E2ETestExpansionHelper() {}

    public static final String SET_CODE = "test";

    // --- Synthetic Instants ---
    public static final CardDefinition TEST_LIGHTNING_BOLT = CardDefinition.builder("mtg:test/lightning_bolt", SET_CODE, "Lightning Bolt")
            .manaCost(ManaCost.ofRed(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_GIANT_GROWTH = CardDefinition.builder("mtg:test/giant_growth", SET_CODE, "Giant Growth")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_COUNTERSPELL = CardDefinition.builder("mtg:test/counterspell", SET_CODE, "Counterspell")
            .manaCost(ManaCost.ofBlue(2))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    // --- Synthetic Sorceries ---
    public static final CardDefinition TEST_PYROCLASM = CardDefinition.builder("mtg:test/pyroclasm", SET_CODE, "Pyroclasm")
            .manaCost(new ManaCost(1, 0, 0, 0, 1, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.SORCERY)
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(100)
            .windupDelayTicks(20)
            .build();

    // --- Synthetic Enchantments ---
    public static final CardDefinition TEST_GLORIOUS_ANTHEM = CardDefinition.builder("mtg:test/glorious_anthem", SET_CODE, "Glorious Anthem")
            .manaCost(new ManaCost(1, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.RARE)
            .baseCooldownTicks(100)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_RANCOR = CardDefinition.builder("mtg:test/rancor", SET_CODE, "Rancor")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_LEVITATION = CardDefinition.builder("mtg:test/levitation", SET_CODE, "Levitation")
            .manaCost(new ManaCost(2, 0, 2, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(100)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_PHYREXIAN_ARENA = CardDefinition.builder("mtg:test/phyrexian_arena", SET_CODE, "Phyrexian Arena")
            .manaCost(new ManaCost(1, 0, 0, 2, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.RARE)
            .baseCooldownTicks(100)
            .windupDelayTicks(0)
            .build();

    // --- Synthetic Creatures ---
    public static final CardDefinition TEST_SERRA_ANGEL = CardDefinition.builder("mtg:test/serra_angel", SET_CODE, "Serra Angel")
            .manaCost(new ManaCost(3, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .rarity(CardRarity.UNCOMMON)
            .combatStats(4, 4)
            .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
            .addKeyword(KeywordInstance.of(CoreKeywords.VIGILANCE))
            .baseCooldownTicks(100)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_LLANOWAR_ELVES = CardDefinition.builder("mtg:test/llanowar_elves", SET_CODE, "Llanowar Elves")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.CREATURE)
            .rarity(CardRarity.COMMON)
            .combatStats(1, 1)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_SHIVAN_DRAGON = CardDefinition.builder("mtg:test/shivan_dragon", SET_CODE, "Shivan Dragon")
            .manaCost(new ManaCost(4, 0, 0, 0, 2, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .rarity(CardRarity.RARE)
            .combatStats(5, 5)
            .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
            .baseCooldownTicks(120)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition TEST_MYTHIC_AVATAR = CardDefinition.builder("mtg:test/mythic_avatar", SET_CODE, "Mythic Avatar")
            .manaCost(new ManaCost(4, 0, 0, 0, 2, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .rarity(CardRarity.MYTHIC)
            .combatStats(7, 7)
            .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
            .baseCooldownTicks(120)
            .windupDelayTicks(0)
            .build();

    private static final List<CardDefinition> SYNTHETIC_CARDS = List.of(
            TEST_LIGHTNING_BOLT,
            TEST_GIANT_GROWTH,
            TEST_COUNTERSPELL,
            TEST_PYROCLASM,
            TEST_GLORIOUS_ANTHEM,
            TEST_RANCOR,
            TEST_LEVITATION,
            TEST_PHYREXIAN_ARENA,
            TEST_SERRA_ANGEL,
            TEST_LLANOWAR_ELVES,
            TEST_SHIVAN_DRAGON,
            TEST_MYTHIC_AVATAR
    );

    public static List<CardDefinition> getSyntheticCards() {
        return SYNTHETIC_CARDS;
    }

    public static List<CardDefinition> getAllTestCards() {
        List<CardDefinition> all = new ArrayList<>(BasicLandsCatalog.getAllCards());
        all.addAll(SYNTHETIC_CARDS);
        return List.copyOf(all);
    }

    public static void registerAllCards(Consumer<CardDefinition> registry) {
        BasicLandsCatalog.registerAllCards(registry);
        SYNTHETIC_CARDS.forEach(registry);
    }

    public static void registerAllEffects() {
        BasicLandsCatalog.registerAllEffects();

        CardEffectRegistry.register(TEST_LIGHTNING_BOLT.id(), ctx ->
                CardEffectResult.success("mtg.cast.lightning_bolt.success"));
        CardEffectRegistry.register(TEST_GIANT_GROWTH.id(), ctx ->
                CardEffectResult.success("mtg.cast.giant_growth.success"));
        CardEffectRegistry.register(TEST_COUNTERSPELL.id(), ctx ->
                CardEffectResult.success("mtg.cast.counterspell.success"));
        CardEffectRegistry.register(TEST_PYROCLASM.id(), ctx ->
                CardEffectResult.success("mtg.cast.pyroclasm.success"));
        CardEffectRegistry.register(TEST_GLORIOUS_ANTHEM.id(), ctx ->
                CardEffectResult.success("mtg.cast.glorious_anthem.success"));
        CardEffectRegistry.register(TEST_RANCOR.id(), ctx ->
                CardEffectResult.success("mtg.cast.rancor.success"));
        CardEffectRegistry.register(TEST_LEVITATION.id(), ctx ->
                CardEffectResult.success("mtg.cast.levitation.success"));
        CardEffectRegistry.register(TEST_PHYREXIAN_ARENA.id(), ctx ->
                CardEffectResult.success("mtg.cast.phyrexian_arena.success"));
        CardEffectRegistry.register(TEST_SERRA_ANGEL.id(), ctx ->
                CardEffectResult.success("mtg.cast.serra_angel.success"));
        CardEffectRegistry.register(TEST_LLANOWAR_ELVES.id(), ctx ->
                CardEffectResult.success("mtg.cast.llanowar_elves.success"));
        CardEffectRegistry.register(TEST_SHIVAN_DRAGON.id(), ctx ->
                CardEffectResult.success("mtg.cast.shivan_dragon.success"));
        CardEffectRegistry.register(TEST_MYTHIC_AVATAR.id(), ctx ->
                CardEffectResult.success("mtg.cast.mythic_avatar.success"));
    }

    public static IExpansionSet createSyntheticExpansion(String setCode, String setName) {
        return new IExpansionSet() {
            @Override
            public String getSetCode() {
                return setCode;
            }

            @Override
            public String getSetName() {
                return setName;
            }

            @Override
            public void registerKeywords(KeywordRegistry registry) {
            }

            @Override
            public void registerTriggers(TriggerBus bus) {
            }

            @Override
            public void registerCards(Consumer<CardDefinition> registry) {
                SYNTHETIC_CARDS.forEach(registry);
            }
        };
    }
}
