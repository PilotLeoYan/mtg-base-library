package com.mtg.api.mechanics.one;

import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.*;
import com.mtg.api.trigger.CombatDamageEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.*;
import java.util.Objects;
import java.util.function.Consumer;

public class PhyrexiaExpansion implements IExpansionSet {
    public static final String SET_CODE = "one";
    public static final KeywordDefinition TOXIC = KeywordDefinition.parameterized("mtg:toxic", "Toxic", "Deals combat damage in the form of poison counters.", KeywordCategory.COMBAT);
    public static final KeywordDefinition CORRUPTED = KeywordDefinition.of("mtg:corrupted", "Corrupted", "Active if an opponent has 3 or more poison counters.", KeywordCategory.MODIFIER);
    public static final KeywordDefinition PROLIFERATE = KeywordDefinition.of("mtg:proliferate", "Proliferate", "Choose targets with counters and add one more of each.", KeywordCategory.ACTION);

    private final PoisonCounterManager poisonManager;

    public PhyrexiaExpansion(PoisonCounterManager poisonManager) {
        this.poisonManager = Objects.requireNonNull(poisonManager, "PoisonCounterManager cannot be null");
    }

    public PhyrexiaExpansion() {
        this(new PoisonCounterManager());
    }

    public PoisonCounterManager getPoisonManager() {
        return poisonManager;
    }

    @Override
    public String getSetCode() { return SET_CODE; }

    @Override
    public String getSetName() { return "Phyrexia: All Will Be One"; }

    @Override
    public void registerKeywords(KeywordRegistry registry) {
        registry.register(TOXIC);
        registry.register(CORRUPTED);
        registry.register(PROLIFERATE);
    }

    @Override
    public void registerTriggers(TriggerBus bus) {
        bus.registerListener(CombatDamageEvent.class, new ToxicCombatDamageListener(poisonManager, bus));
    }

    @Override
    public void registerCards(Consumer<CardDefinition> registry) {
        // Venerated Rotpriest
        registry.accept(CardDefinition.builder("mtg:one/venerated_rotpriest", SET_CODE, "Venerated Rotpriest")
            .manaCost(new ManaCost(0, 0, 0, 0, 0, 1, 0, PhyrexianManaCost.NONE)) // {G}
            .addType(CardType.CREATURE)
            .addSubtype("Phyrexian")
            .addSubtype("Druid")
            .rarity(CardRarity.RARE)
            .combatStats(1, 2)
            .addKeyword(KeywordInstance.of(TOXIC, 1))
            .build());
    }
}
