package com.mtg.cards.catalog;

import com.mtg.cards.effects.CardEffectRegistry;
import com.mtg.cards.effects.CardEffectResult;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Concrete catalog defining the 6 canonical Basic Lands for the MTG Base Library.
 */
public final class BasicLandsCatalog {
    private BasicLandsCatalog() {}

    public static final String SET_CODE = "base";

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

    private static final List<CardDefinition> ALL_CARDS = List.of(
            PLAINS,
            ISLAND,
            SWAMP,
            MOUNTAIN,
            FOREST,
            WASTES
    );

    public static List<CardDefinition> getAllCards() {
        return ALL_CARDS;
    }

    public static void registerAllCards(Consumer<CardDefinition> registry) {
        getAllCards().forEach(registry);
    }

    public static void registerAllEffects() {
        CardEffectRegistry.register(PLAINS.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.WHITE, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            if (pool != null) {
                pool.setMaxMana(pool.getMaxMana() + 1);
            }
            return CardEffectResult.success("mtg.cast.plains.success");
        });

        CardEffectRegistry.register(ISLAND.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.BLUE, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            if (pool != null) {
                pool.setMaxMana(pool.getMaxMana() + 1);
            }
            return CardEffectResult.success("mtg.cast.island.success");
        });

        CardEffectRegistry.register(SWAMP.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.BLACK, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            if (pool != null) {
                pool.setMaxMana(pool.getMaxMana() + 1);
            }
            return CardEffectResult.success("mtg.cast.swamp.success");
        });

        CardEffectRegistry.register(MOUNTAIN.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.RED, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            if (pool != null) {
                pool.setMaxMana(pool.getMaxMana() + 1);
            }
            return CardEffectResult.success("mtg.cast.mountain.success");
        });

        CardEffectRegistry.register(FOREST.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.GREEN, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            if (pool != null) {
                pool.setMaxMana(pool.getMaxMana() + 1);
            }
            return CardEffectResult.success("mtg.cast.forest.success");
        });

        CardEffectRegistry.register(WASTES.id(), ctx -> {
            ManaManager.getInstance().addMana(ctx.casterId(), ManaColor.COLORLESS, 1);
            ManaPool pool = ManaManager.getInstance().getPlayerMana(ctx.casterId());
            if (pool != null) {
                pool.setMaxMana(pool.getMaxMana() + 1);
            }
            return CardEffectResult.success("mtg.cast.wastes.success");
        });
    }
}
