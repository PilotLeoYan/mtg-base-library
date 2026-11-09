package com.mtg.api.mechanics.znr;

import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.*;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.*;
import java.util.function.Consumer;

public class ZendikarRisingExpansion implements IExpansionSet {
    public static final String SET_CODE = "znr";
    public static final KeywordDefinition LANDFALL = KeywordDefinition.of("mtg:landfall", "Landfall", "Triggers whenever a land enters the battlefield under your control.", KeywordCategory.TRIGGERED);
    public static final KeywordDefinition KICKER = KeywordDefinition.of("mtg:kicker", "Kicker", "You may pay an additional cost as you cast this spell.", KeywordCategory.MODIFIER);

    @Override
    public String getSetCode() { return SET_CODE; }

    @Override
    public String getSetName() { return "Zendikar Rising"; }

    @Override
    public void registerKeywords(KeywordRegistry registry) {
        registry.register(LANDFALL);
        registry.register(KICKER);
    }

    @Override
    public void registerTriggers(TriggerBus bus) {
        // ZNR set-wide triggers can be subscribed here if required
    }

    @Override
    public void registerCards(Consumer<CardDefinition> registry) {
        // Lotus Cobra
        registry.accept(CardDefinition.builder("mtg:znr/lotus_cobra", SET_CODE, "Lotus Cobra")
            .manaCost(new ManaCost(1, 0, 0, 0, 0, 1, 0, PhyrexianManaCost.NONE)) // {1}{G}
            .addType(CardType.CREATURE)
            .addSubtype("Snake")
            .rarity(CardRarity.RARE)
            .combatStats(2, 1)
            .addKeyword(KeywordInstance.of(LANDFALL))
            .build());

        // Roost of Drakes
        registry.accept(CardDefinition.builder("mtg:znr/roost_of_drakes", SET_CODE, "Roost of Drakes")
            .manaCost(new ManaCost(0, 0, 1, 0, 0, 0, 0, PhyrexianManaCost.NONE)) // {U}
            .addType(CardType.ENCHANTMENT)
            .rarity(CardRarity.UNCOMMON)
            .addKeyword(KeywordInstance.of(KICKER))
            .build());
    }
}
