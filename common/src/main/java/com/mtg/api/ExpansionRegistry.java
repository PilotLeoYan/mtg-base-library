package com.mtg.api;

import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardDefinition;
import com.mtg.guidebook.JournalBookModel;
import com.mtg.world.boss.BossManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ExpansionRegistry {
    private static final Map<String, IExpansionSet> EXPANSIONS = new ConcurrentHashMap<>();

    private ExpansionRegistry() {}

    public static void registerExpansion(IExpansionSet expansion) {
        Objects.requireNonNull(expansion, "Expansion cannot be null");
        String key = expansion.getSetCode().toLowerCase(Locale.ROOT);
        IExpansionSet existing = EXPANSIONS.putIfAbsent(key, expansion);
        if (existing != null && existing != expansion) {
            throw new IllegalStateException("Duplicate expansion set registration for code: " + expansion.getSetCode());
        }
    }

    public static Optional<IExpansionSet> getExpansion(String setCode) {
        if (setCode == null) return Optional.empty();
        return Optional.ofNullable(EXPANSIONS.get(setCode.toLowerCase(Locale.ROOT)));
    }

    public static Collection<IExpansionSet> getAllExpansions() {
        return Collections.unmodifiableCollection(EXPANSIONS.values());
    }

    public static void initializeAll(Consumer<CardDefinition> cardRegistry, KeywordRegistry keywordRegistry, TriggerBus triggerBus) {
        initializeAll(cardRegistry, keywordRegistry, triggerBus, null, null);
    }

    public static void initializeAll(Consumer<CardDefinition> cardRegistry, KeywordRegistry keywordRegistry,
                                     TriggerBus triggerBus, BossManager bosses, JournalBookModel guidebook) {
        List<IExpansionSet> enabled = EXPANSIONS.entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .sorted(Comparator.<Map.Entry<String, IExpansionSet>>comparingInt(entry -> entry.getValue().getPriority())
                        .reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .toList();

        Map<String, String> cardOwners = new HashMap<>();
        for (IExpansionSet expansion : enabled) {
            expansion.registerKeywords(keywordRegistry);
            expansion.registerTriggers(triggerBus);
            expansion.registerCards(card -> {
                String owner = cardOwners.putIfAbsent(card.id(), expansion.getSetCode());
                if (owner != null) {
                    throw new IllegalStateException("Duplicate card ID " + card.id() + " from expansions "
                            + owner + " and " + expansion.getSetCode());
                }
                cardRegistry.accept(card);
            });
            expansion.registerEffects();
            if (bosses != null) {
                expansion.registerBosses(bosses);
            }
            if (guidebook != null) {
                expansion.registerGuidebookContent(guidebook);
            }
        }
    }

    public static List<IExpansionSet> discoverAndRegisterViaServiceLoader() {
        Module module = ExpansionRegistry.class.getModule();
        module.addUses(IExpansionSet.class);
        ServiceLoader<IExpansionSet> loader = module.getLayer() == null
                ? ServiceLoader.load(IExpansionSet.class)
                : ServiceLoader.load(module.getLayer(), IExpansionSet.class);
        List<IExpansionSet> discovered = loader.stream().map(ServiceLoader.Provider::get).toList();
        discovered.forEach(ExpansionRegistry::registerExpansion);
        return discovered;
    }

    public static void clear() {
        EXPANSIONS.clear();
    }
}
