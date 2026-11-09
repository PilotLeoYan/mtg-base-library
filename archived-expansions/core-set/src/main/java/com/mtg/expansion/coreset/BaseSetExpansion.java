package com.mtg.cards.catalog;

import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardDefinition;

import java.util.function.Consumer;

/**
 * Base set expansion implementation registering the 30 representative core cards
 * and their respective effect handlers.
 */
public class BaseSetExpansion implements IExpansionSet {
    public static final String SET_CODE = "base";

    @Override
    public String getSetCode() {
        return SET_CODE;
    }

    @Override
    public String getSetName() {
        return "Magic: The Gathering Base Set";
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
        // Base triggers registered here
    }

    @Override
    public void registerCards(Consumer<CardDefinition> registry) {
        BaseCardCatalog.registerAllCards(registry);
        BaseCardCatalog.registerAllEffects();
    }
}
