package com.mtg.cards.catalog;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.domain.KeywordInstance;
import com.mtg.cards.casting.CardCastingService;
import com.mtg.cards.effects.*;
import com.mtg.config.MtgConfig;
import com.mtg.domain.*;
import com.mtg.entity.ISummonedCreature;
import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import com.mtg.entity.TappableManaAbility;
import com.mtg.entity.catalog.BaseCreatures;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;

import java.util.*;
import java.util.function.Consumer;

/**
 * Concrete catalog of representative Magic: The Gathering cards across all 5 types
 * and colors, along with their respective ICardEffectHandler implementations.
 */
public final class BaseCardCatalog {
    private BaseCardCatalog() {}

    public static final String SET_CODE = "base";

    // LANDS
    public static final CardDefinition PLAINS = CardDefinition.builder("mtg:base/plains", SET_CODE, "Plains")
        .manaCost(ManaCost.ZERO)
        .addType(CardType.LAND)
        .addSubtype("Plains")
        .rarity(CardRarity.COMMON)
        .baseCooldownTicks(1200)
        .windupDelayTicks(0)
        .build();

    public static final CardDefinition ISLAND = CardDefinition.builder("mtg:base/island", SET_CODE, "Island")
        .manaCost(ManaCost.ZERO)
        .addType(CardType.LAND)
        .addSubtype("Island")
        .rarity(CardRarity.COMMON)
        .baseCooldownTicks(1200)
        .windupDelayTicks(0)
        .build();

    public static final CardDefinition SWAMP = CardDefinition.builder("mtg:base/swamp", SET_CODE, "Swamp")
        .manaCost(ManaCost.ZERO)
        .addType(CardType.LAND)
        .addSubtype("Swamp")
        .rarity(CardRarity.COMMON)
        .baseCooldownTicks(1200)
        .windupDelayTicks(0)
        .build();

    public static final CardDefinition MOUNTAIN = CardDefinition.builder("mtg:base/mountain", SET_CODE, "Mountain")
        .manaCost(ManaCost.ZERO)
        .addType(CardType.LAND)
        .addSubtype("Mountain")
        .rarity(CardRarity.COMMON)
        .baseCooldownTicks(1200)
        .windupDelayTicks(0)
        .build();

    public static final CardDefinition FOREST = CardDefinition.builder("mtg:base/forest", SET_CODE, "Forest")
        .manaCost(ManaCost.ZERO)
        .addType(CardType.LAND)
        .addSubtype("Forest")
        .rarity(CardRarity.COMMON)
        .baseCooldownTicks(1200)
        .windupDelayTicks(0)
        .build();

    public static final CardDefinition WASTES = CardDefinition.builder("mtg:base/wastes", SET_CODE, "Wastes")
        .manaCost(ManaCost.ZERO)
        .addType(CardType.LAND)
        .addSubtype("Wastes")
        .rarity(CardRarity.COMMON)
        .baseCooldownTicks(1200)
        .windupDelayTicks(0)
        .build();

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

    // CREATURES (EXPOSED FROM BASE CREATURES)
    public static final CardDefinition SHIVAN_DRAGON = BaseCreatures.SHIVAN_DRAGON;
    public static final CardDefinition DEMON_OF_DEATHS_GATE = BaseCreatures.DEMON_OF_DEATHS_GATE;

    public static List<CardDefinition> getAllCards() {
        List<CardDefinition> all = new ArrayList<>();
        // Lands
        all.add(PLAINS);
        all.add(ISLAND);
        all.add(SWAMP);
        all.add(MOUNTAIN);
        all.add(FOREST);
        all.add(WASTES);
        // Instants
        all.add(GIANT_GROWTH);
        all.add(LIGHTNING_BOLT);
        all.add(COUNTERSPELL);
        all.add(UNSUMMON);
        all.add(DARK_RITUAL);
        all.add(HEALING_SALVE);
        // Sorceries
        all.add(WRATH_OF_GOD);
        all.add(DIVINATION);
        all.add(MIND_ROT);
        all.add(PYROCLASM);
        // Enchantments
        all.add(GLORIOUS_ANTHEM);
        all.add(PHYREXIAN_ARENA);
        all.add(RANCOR);
        all.add(LEVITATION);
        // Creatures
        all.addAll(BaseCreatures.getAll());
        return Collections.unmodifiableList(all);
    }

    public static void registerAllCards(Consumer<CardDefinition> registry) {
        getAllCards().forEach(registry);
    }

    public static void registerAllEffects() {
        // --- LANDS ---
        CardEffectRegistry.register(PLAINS.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.WHITE, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            pool.setMaxMana(pool.getMaxMana() + 1);
            return CardEffectResult.success("mtg.cast.plains.success");
        });

        CardEffectRegistry.register(ISLAND.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.BLUE, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            pool.setMaxMana(pool.getMaxMana() + 1);
            return CardEffectResult.success("mtg.cast.island.success");
        });

        CardEffectRegistry.register(SWAMP.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.BLACK, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            pool.setMaxMana(pool.getMaxMana() + 1);
            return CardEffectResult.success("mtg.cast.swamp.success");
        });

        CardEffectRegistry.register(MOUNTAIN.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.RED, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            pool.setMaxMana(pool.getMaxMana() + 1);
            return CardEffectResult.success("mtg.cast.mountain.success");
        });

        CardEffectRegistry.register(FOREST.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.GREEN, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            pool.setMaxMana(pool.getMaxMana() + 1);
            return CardEffectResult.success("mtg.cast.forest.success");
        });

        CardEffectRegistry.register(WASTES.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.COLORLESS, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            pool.setMaxMana(pool.getMaxMana() + 1);
            return CardEffectResult.success("mtg.cast.wastes.success");
        });

        // --- INSTANTS ---
        CardEffectRegistry.register(GIANT_GROWTH.id(), ctx -> {
            List<SummonedCreature> summons = SummonManager.getInstance().getPlayerSummons(ctx.casterId());
            if (!summons.isEmpty()) {
                summons.getFirst().applyStatModifier(3, 3);
            }
            return CardEffectResult.success("mtg.cast.giant_growth.success", Map.of("boost", 3));
        });

        CardEffectRegistry.register(LIGHTNING_BOLT.id(), ctx -> {
            if (ctx.targetEntityId() >= 0) {
                // If there's a target summon, deal damage
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
            // Cancels channeling cast
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

        // --- SORCERIES ---
        CardEffectRegistry.register(WRATH_OF_GOD.id(), ctx -> {
            // Destroy all summoned creatures in the vicinity
            for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                c.dismiss();
            }
            return CardEffectResult.success("mtg.cast.wrath_of_god.success");
        });

        CardEffectRegistry.register(DIVINATION.id(), ctx -> {
            // Draw cards / bonus wildcard progress
            ProgressionManager.getInstance().getProgression(ctx.casterId()).addWildcards(CardRarity.UNCOMMON, 1);
            return CardEffectResult.success("mtg.cast.divination.success");
        });

        CardEffectRegistry.register(MIND_ROT.id(), ctx -> {
            return CardEffectResult.success("mtg.cast.mind_rot.success");
        });

        CardEffectRegistry.register(PYROCLASM.id(), ctx -> {
            // Deal 4.0 damage (2 damage in MTG) to all creatures
            for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                c.takeDamage(4.0f);
            }
            return CardEffectResult.success("mtg.cast.pyroclasm.success", Map.of("damage", 4.0));
        });

        // --- ENCHANTMENTS ---
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
                    public void onTick(ActiveEnchantment enchantment) {
                        // Continuous anthem effect: all player's summons have +1/+1
                    }

                    @Override
                    public void onExpire(ActiveEnchantment enchantment) {
                        for (SummonedCreature c : SummonManager.getInstance().getPlayerSummons(ctx.casterId())) {
                            c.applyStatModifier(-1, -1);
                        }
                    }
                }
            );
            // Apply +1/+1 to existing summons
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
                        // Upkeep: owner loses 1 life, gains 1 Black mana
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

        // --- CREATURES ---
        for (CardDefinition creatureCard : BaseCreatures.getAll()) {
            CardEffectRegistry.register(creatureCard.id(), ctx -> {
                SummonedCreature creature = new SummonedCreature(
                    UUID.randomUUID(),
                    ctx.casterId(),
                    creatureCard,
                    ctx.targetX(),
                    ctx.targetY(),
                    ctx.targetZ()
                );
                // Check if Glorious Anthem is active for caster
                if (EnchantmentManager.getInstance().hasEnchantment(ctx.casterId(), GLORIOUS_ANTHEM.id())) {
                    creature.applyStatModifier(1, 1);
                }
                SummonManager.getInstance().registerSummon(creature);
                return CardEffectResult.success("mtg.cast.creature.success");
            });
        }
    }
}
