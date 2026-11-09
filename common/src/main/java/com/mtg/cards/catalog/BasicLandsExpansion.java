package com.mtg.cards.catalog;

import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardDefinition;

import java.util.function.Consumer;

/**
 * Base Library Expansion registering the 6 Basic Lands and their effect handlers.
 */
public class BasicLandsExpansion implements IExpansionSet {
    public static final String SET_CODE = "base";

    @Override
    public String getSetCode() {
        return SET_CODE;
    }

    @Override
    public String getSetName() {
        return "Magic: The Gathering Base Library (Basic Lands)";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void registerKeywords(KeywordRegistry registry) {
        // Core keywords are auto-registered by KeywordRegistry
    }

    @Override
    public void registerTriggers(TriggerBus bus) {
        // Base triggers registered here if any
    }

    @Override
    public void registerCards(Consumer<CardDefinition> registry) {
        BasicLandsCatalog.registerAllCards(registry);
        BasicLandsCatalog.registerAllEffects();
    }

    @Override
    public void registerEffects() {
        BasicLandsCatalog.registerAllEffects();
    }
}
