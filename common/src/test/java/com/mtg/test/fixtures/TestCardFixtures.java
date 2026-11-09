package com.mtg.test.fixtures;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.cards.effects.*;
import com.mtg.config.MtgConfig;
import com.mtg.domain.*;
import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import com.mtg.mana.ManaManager;
import com.mtg.progression.ProgressionManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Shared test fixtures providing synthetic test cards, keywords, and effect handlers
 * for unit and integration testing without depending on external expansions.
 */
public final class TestCardFixtures {
    private TestCardFixtures() {}

    public static final String SET_CODE = "base";

    // 6 BASIC LANDS (Re-exposed from BasicLandsCatalog)
    public static final CardDefinition PLAINS = BasicLandsCatalog.PLAINS;
    public static final CardDefinition ISLAND = BasicLandsCatalog.ISLAND;
    public static final CardDefinition SWAMP = BasicLandsCatalog.SWAMP;
    public static final CardDefinition MOUNTAIN = BasicLandsCatalog.MOUNTAIN;
    public static final CardDefinition FOREST = BasicLandsCatalog.FOREST;
    public static final CardDefinition WASTES = BasicLandsCatalog.WASTES;

    // KEYWORDS
    public static final KeywordDefinition TOXIC = KeywordDefinition.parameterized(
            "mtg:toxic",
            "Toxic",
            "Deals combat damage in the form of poison counters.",
            KeywordCategory.COMBAT
    );

    // INSTANTS
    public static final CardDefinition GIANT_GROWTH = CardDefinition.builder("mtg:base/giant_growth", SET_CODE, "Giant Growth")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition LIGHTNING_BOLT = CardDefinition.builder("mtg:base/lightning_bolt", SET_CODE, "Lightning Bolt")
            .manaCost(ManaCost.ofRed(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition COUNTERSPELL = CardDefinition.builder("mtg:base/counterspell", SET_CODE, "Counterspell")
            .manaCost(ManaCost.ofBlue(2))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(80)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition UNSUMMON = CardDefinition.builder("mtg:base/unsummon", SET_CODE, "Unsummon")
            .manaCost(ManaCost.ofBlue(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition DARK_RITUAL = CardDefinition.builder("mtg:base/dark_ritual", SET_CODE, "Dark Ritual")
            .manaCost(ManaCost.ofBlack(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(80)
            .windupDelayTicks(0)
            .build();

    public static final CardDefinition HEALING_SALVE = CardDefinition.builder("mtg:base/healing_salve", SET_CODE, "Healing Salve")
            .manaCost(ManaCost.ofWhite(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

    // SORCERIES
    public static final CardDefinition WRATH_OF_GOD = CardDefinition.builder("mtg:base/wrath_of_god", SET_CODE, "Wrath of God")
            .manaCost(new ManaCost(2, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.SORCERY)
            .rarity(CardRarity.RARE)
            .baseCooldownTicks(200)
            .windupDelayTicks(40)
            .build();

    public static final CardDefinition DIVINATION = CardDefinition.builder("mtg:base/divination", SET_CODE, "Divination")
            .manaCost(new ManaCost(2, 0, 1, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.SORCERY)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(100)
            .windupDelayTicks(30)
            .build();

    public static final CardDefinition MIND_ROT = CardDefinition.builder("mtg:base/mind_rot", SET_CODE, "Mind Rot")
            .manaCost(new ManaCost(2, 0, 0, 1, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.SORCERY)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(100)
            .windupDelayTicks(30)
            .build();

    public static final CardDefinition PYROCLASM = CardDefinition.builder("mtg:base/pyroclasm", SET_CODE, "Pyroclasm")
            .manaCost(new ManaCost(1, 0, 0, 0, 1, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.SORCERY)
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(80)
            .windupDelayTicks(20)
            .build();

    // ENCHANTMENTS
    public static final CardDefinition GLORIOUS_ANTHEM = CardDefinition.builder("mtg:base/glorious_anthem", SET_CODE, "Glorious Anthem")
            .manaCost(new ManaCost(1, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.RARE)
            .baseCooldownTicks(200)
            .windupDelayTicks(20)
            .build();

    public static final CardDefinition PHYREXIAN_ARENA = CardDefinition.builder("mtg:base/phyrexian_arena", SET_CODE, "Phyrexian Arena")
            .manaCost(new ManaCost(1, 0, 0, 2, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.RARE)
            .baseCooldownTicks(200)
            .windupDelayTicks(20)
            .build();

    public static final CardDefinition RANCOR = CardDefinition.builder("mtg:base/rancor", SET_CODE, "Rancor")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.ENCHANTMENT)
            .addSubtype("Aura")
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(80)
            .windupDelayTicks(10)
            .build();

    public static final CardDefinition LEVITATION = CardDefinition.builder("mtg:base/levitation", SET_CODE, "Levitation")
            .manaCost(new ManaCost(2, 0, 2, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.UNCOMMON)
            .baseCooldownTicks(200)
            .windupDelayTicks(20)
            .build();

    // CREATURES
    public static final CardDefinition LLANOWAR_ELVES = CardDefinition.builder("mtg:base/llanowar_elves", SET_CODE, "Llanowar Elves")
            .manaCost(ManaCost.ofGreen(1))
            .addType(CardType.CREATURE)
            .addSubtype("Elf")
            .addSubtype("Druid")
            .rarity(CardRarity.COMMON)
            .combatStats(1, 1)
            .baseCooldownTicks(120)
            .windupDelayTicks(10)
            .build();

    public static final CardDefinition SERRA_ANGEL = CardDefinition.builder("mtg:base/serra_angel", SET_CODE, "Serra Angel")
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

    public static final CardDefinition VAMPIRE_NIGHTHAWK = CardDefinition.builder("mtg:base/vampire_nighthawk", SET_CODE, "Vampire Nighthawk")
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

    public static final CardDefinition GOBLIN_GUIDE = CardDefinition.builder("mtg:base/goblin_guide", SET_CODE, "Goblin Guide")
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

    public static final CardDefinition PHYREXIAN_RAGER = CardDefinition.builder("mtg:base/phyrexian_rager", SET_CODE, "Phyrexian Rager")
            .manaCost(new ManaCost(2, 0, 0, 1, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .addSubtype("Phyrexian")
            .addSubtype("Horror")
            .rarity(CardRarity.COMMON)
            .combatStats(2, 2)
            .baseCooldownTicks(200)
            .windupDelayTicks(20)
            .build();

    public static final CardDefinition AIR_ELEMENTAL = CardDefinition.builder("mtg:base/air_elemental", SET_CODE, "Air Elemental")
            .manaCost(new ManaCost(3, 0, 2, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .addSubtype("Elemental")
            .rarity(CardRarity.UNCOMMON)
            .combatStats(4, 4)
            .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
            .baseCooldownTicks(280)
            .windupDelayTicks(30)
            .build();

    public static final CardDefinition VENERATED_ROTPRIEST = CardDefinition.builder("mtg:base/venerated_rotpriest", SET_CODE, "Venerated Rotpriest")
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

    public static final CardDefinition SHIVAN_DRAGON = CardDefinition.builder("mtg:base/shivan_dragon", SET_CODE, "Shivan Dragon")
            .manaCost(new ManaCost(4, 0, 0, 0, 2, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .addSubtype("Dragon")
            .rarity(CardRarity.RARE)
            .combatStats(5, 5)
            .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
            .baseCooldownTicks(360)
            .windupDelayTicks(30)
            .build();

    public static final CardDefinition DEMON_OF_DEATHS_GATE = CardDefinition.builder("mtg:base/demon_of_deaths_gate", SET_CODE, "Demon of Death's Gate")
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

    private static final List<CardDefinition> ALL_CREATURES = List.of(
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

    private static final List<CardDefinition> ALL_CARDS = List.of(
            PLAINS,
            ISLAND,
            SWAMP,
            MOUNTAIN,
            FOREST,
            WASTES,
            GIANT_GROWTH,
            LIGHTNING_BOLT,
            COUNTERSPELL,
            UNSUMMON,
            DARK_RITUAL,
            HEALING_SALVE,
            WRATH_OF_GOD,
            DIVINATION,
            MIND_ROT,
            PYROCLASM,
            GLORIOUS_ANTHEM,
            PHYREXIAN_ARENA,
            RANCOR,
            LEVITATION,
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

    public static List<CardDefinition> getAllCards() {
        return ALL_CARDS;
    }

    public static List<CardDefinition> getAllCreatures() {
        return ALL_CREATURES;
    }

    public static void registerAllCards(Consumer<CardDefinition> registry) {
        getAllCards().forEach(registry);
    }

    public static void registerAllEffects() {
        // Register the 6 Basic Land effects via BasicLandsCatalog
        BasicLandsCatalog.registerAllEffects();

        // Instants
        CardEffectRegistry.register(GIANT_GROWTH.id(), ctx -> {
            List<SummonedCreature> summons = SummonManager.getInstance().getPlayerSummons(ctx.casterId());
            if (!summons.isEmpty()) {
                summons.getFirst().applyStatModifier(3, 3);
            }
            return CardEffectResult.success("mtg.cast.giant_growth.success", Map.of("boost", 3));
        });

        CardEffectRegistry.register(LIGHTNING_BOLT.id(), ctx -> {
            if (ctx.targetEntityId() >= 0) {
                for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                    if (c.getEntityId().hashCode() == ctx.targetEntityId()) {
                        c.takeDamage(6.0f);
                        break;
                    }
                }
            }
            return CardEffectResult.success("mtg.cast.lightning_bolt.success", Map.of("damage", 6.0));
        });

        CardEffectRegistry.register(COUNTERSPELL.id(), ctx -> {
            return CardEffectResult.success("mtg.cast.counterspell.success");
        });

        CardEffectRegistry.register(UNSUMMON.id(), ctx -> {
            List<SummonedCreature> summons = SummonManager.getInstance().getPlayerSummons(ctx.casterId());
            if (!summons.isEmpty()) {
                summons.getLast().dismiss();
            }
            return CardEffectResult.success("mtg.cast.unsummon.success");
        });

        CardEffectRegistry.register(DARK_RITUAL.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.BLACK, 3);
            return CardEffectResult.success("mtg.cast.dark_ritual.success", Map.of("manaAdded", 3));
        });

        CardEffectRegistry.register(HEALING_SALVE.id(), ctx -> {
            List<SummonedCreature> summons = SummonManager.getInstance().getPlayerSummons(ctx.casterId());
            if (!summons.isEmpty()) {
                summons.getFirst().heal(6.0f);
            }
            return CardEffectResult.success("mtg.cast.healing_salve.success", Map.of("healing", 6.0));
        });

        // Sorceries
        CardEffectRegistry.register(WRATH_OF_GOD.id(), ctx -> {
            for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                c.dismiss();
            }
            return CardEffectResult.success("mtg.cast.wrath_of_god.success");
        });

        CardEffectRegistry.register(DIVINATION.id(), ctx -> {
            ProgressionManager.getInstance().getProgression(ctx.casterId()).addWildcards(CardRarity.UNCOMMON, 1);
            return CardEffectResult.success("mtg.cast.divination.success");
        });

        CardEffectRegistry.register(MIND_ROT.id(), ctx -> {
            return CardEffectResult.success("mtg.cast.mind_rot.success");
        });

        CardEffectRegistry.register(PYROCLASM.id(), ctx -> {
            for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                c.takeDamage(4.0f);
            }
            return CardEffectResult.success("mtg.cast.pyroclasm.success", Map.of("damage", 4.0));
        });

        // Enchantments
        CardEffectRegistry.register(GLORIOUS_ANTHEM.id(), ctx -> {
            int duration = MtgConfig.getCards().enchantmentBaseDurationTicks();
            ActiveEnchantment ench = new ActiveEnchantment(
                    ctx.casterId(),
                    GLORIOUS_ANTHEM,
                    ActiveEnchantment.EnchantmentTargetType.CASTER,
                    -1,
                    duration,
                    new IEnchantmentTickListener() {
                        @Override
                        public void onTick(ActiveEnchantment enchantment) {}

                        @Override
                        public void onExpire(ActiveEnchantment enchantment) {
                            for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                                c.applyStatModifier(-1, -1);
                            }
                        }
                    }
            );
            for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                c.applyStatModifier(1, 1);
            }
            EnchantmentManager.getInstance().addEnchantment(ench);
            return CardEffectResult.success("mtg.cast.glorious_anthem.success");
        });

        CardEffectRegistry.register(PHYREXIAN_ARENA.id(), ctx -> {
            int duration = MtgConfig.getCards().enchantmentBaseDurationTicks();
            ActiveEnchantment ench = new ActiveEnchantment(
                    ctx.casterId(),
                    PHYREXIAN_ARENA,
                    ActiveEnchantment.EnchantmentTargetType.CASTER,
                    -1,
                    duration,
                    new IEnchantmentTickListener() {
                        @Override
                        public void onUpkeep(ActiveEnchantment enchantment) {
                            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.BLACK, 1);
                        }
                    }
            );
            EnchantmentManager.getInstance().addEnchantment(ench);
            return CardEffectResult.success("mtg.cast.phyrexian_arena.success");
        });

        CardEffectRegistry.register(RANCOR.id(), ctx -> {
            int duration = MtgConfig.getCards().enchantmentBaseDurationTicks();
            List<SummonedCreature> summons = SummonManager.getInstance().getPlayerSummons(ctx.casterId());
            if (!summons.isEmpty()) {
                SummonedCreature targetCreature = summons.getFirst();
                targetCreature.applyStatModifier(2, 0);
                ActiveEnchantment ench = new ActiveEnchantment(
                        ctx.casterId(),
                        RANCOR,
                        ActiveEnchantment.EnchantmentTargetType.TARGET_ENTITY,
                        targetCreature.getEntityId().hashCode(),
                        duration,
                        new IEnchantmentTickListener() {
                            @Override
                            public void onExpire(ActiveEnchantment enchantment) {
                                targetCreature.applyStatModifier(-2, 0);
                            }
                        }
                );
                EnchantmentManager.getInstance().addEnchantment(ench);
            }
            return CardEffectResult.success("mtg.cast.rancor.success");
        });

        CardEffectRegistry.register(LEVITATION.id(), ctx -> {
            int duration = MtgConfig.getCards().enchantmentBaseDurationTicks();
            ActiveEnchantment ench = new ActiveEnchantment(
                    ctx.casterId(),
                    LEVITATION,
                    ActiveEnchantment.EnchantmentTargetType.CASTER,
                    -1,
                    duration,
                    null
            );
            EnchantmentManager.getInstance().addEnchantment(ench);
            return CardEffectResult.success("mtg.cast.levitation.success");
        });

        // Creatures
        for (CardDefinition creatureCard : getAllCreatures()) {
            CardEffectRegistry.register(creatureCard.id(), ctx -> {
                SummonedCreature creature = new SummonedCreature(
                        UUID.randomUUID(),
                        ctx.casterId(),
                        creatureCard,
                        ctx.targetX(),
                        ctx.targetY(),
                        ctx.targetZ()
                );
                if (EnchantmentManager.getInstance().hasEnchantment(ctx.casterId(), GLORIOUS_ANTHEM.id())) {
                    creature.applyStatModifier(1, 1);
                }
                SummonManager.getInstance().registerSummon(creature);
                return CardEffectResult.success("mtg.cast.creature.success");
            });
        }
    }
}
