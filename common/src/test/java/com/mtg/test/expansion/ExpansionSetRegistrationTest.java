package com.mtg.test.expansion;

import com.mtg.api.ExpansionRegistry;
import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.cards.catalog.BasicLandsExpansion;
import com.mtg.cards.effects.CardEffectRegistry;
import com.mtg.cards.effects.CardEffectResult;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import com.mtg.guidebook.JournalBookModel;
import com.mtg.world.boss.BossManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Expansion Set Registration and Contract Compliance Suite")
public class ExpansionSetRegistrationTest {

    @BeforeEach
    void setup() {
        ExpansionRegistry.clear();
    }

    // =========================================================================
    // Synthetic Independent Mock Expansions
    // =========================================================================

    public static class MockAlphaExpansion implements IExpansionSet {
        private final List<String> executionLog;

        public MockAlphaExpansion(List<String> executionLog) {
            this.executionLog = executionLog;
        }

        public MockAlphaExpansion() {
            this(new ArrayList<>());
        }

        @Override
        public String getSetCode() {
            return "alpha";
        }

        @Override
        public String getSetName() {
            return "Mock Alpha Expansion";
        }

        @Override
        public int getPriority() {
            return 50;
        }

        @Override
        public void registerKeywords(KeywordRegistry registry) {
            executionLog.add("alpha:keywords");
            registry.register(KeywordDefinition.of("mtg:alpha_strike", "Alpha Strike", "First strike modifier", KeywordCategory.COMBAT));
        }

        @Override
        public void registerTriggers(TriggerBus bus) {
            executionLog.add("alpha:triggers");
        }

        @Override
        public void registerCards(Consumer<CardDefinition> registry) {
            executionLog.add("alpha:cards");
            CardDefinition knight = CardDefinition.builder("mtg:alpha/knight", "alpha", "Alpha Knight")
                    .manaCost(ManaCost.ofWhite(2))
                    .addType(CardType.CREATURE)
                    .rarity(CardRarity.COMMON)
                    .combatStats(2, 2)
                    .build();
            registry.accept(knight);
        }

        @Override
        public void registerEffects() {
            executionLog.add("alpha:effects");
            CardEffectRegistry.register("mtg:alpha/knight", ctx -> CardEffectResult.success("mtg.cast.alpha_knight.success"));
        }
    }

    public static class MockBetaExpansion implements IExpansionSet {
        private final List<String> executionLog;

        public MockBetaExpansion(List<String> executionLog) {
            this.executionLog = executionLog;
        }

        public MockBetaExpansion() {
            this(new ArrayList<>());
        }

        @Override
        public String getSetCode() {
            return "beta";
        }

        @Override
        public String getSetName() {
            return "Mock Beta Expansion";
        }

        @Override
        public int getPriority() {
            return 80;
        }

        @Override
        public void registerKeywords(KeywordRegistry registry) {
            executionLog.add("beta:keywords");
            registry.register(KeywordDefinition.of("mtg:beta_ward", "Beta Ward", "Ward shield modifier", KeywordCategory.MODIFIER));
        }

        @Override
        public void registerTriggers(TriggerBus bus) {
            executionLog.add("beta:triggers");
        }

        @Override
        public void registerCards(Consumer<CardDefinition> registry) {
            executionLog.add("beta:cards");
            CardDefinition mage = CardDefinition.builder("mtg:beta/mage", "beta", "Beta Mage")
                    .manaCost(ManaCost.ofBlue(2))
                    .addType(CardType.CREATURE)
                    .rarity(CardRarity.UNCOMMON)
                    .combatStats(1, 3)
                    .build();
            registry.accept(mage);
        }

        @Override
        public void registerEffects() {
            executionLog.add("beta:effects");
            CardEffectRegistry.register("mtg:beta/mage", ctx -> CardEffectResult.success("mtg.cast.beta_mage.success"));
        }
    }

    // =========================================================================
    // Test Cases
    // =========================================================================

    @Test
    @DisplayName("1. Expansion set contract compliance: valid set codes, names, and defaults")
    void testBasicLandsExpansionContract() {
        BasicLandsExpansion base = new BasicLandsExpansion();
        assertEquals("base", base.getSetCode());
        assertEquals("Magic: The Gathering Base Library (Basic Lands)", base.getSetName());
        assertTrue(base.isEnabled());
        assertEquals(100, base.getPriority());

        MockAlphaExpansion alpha = new MockAlphaExpansion();
        assertEquals("alpha", alpha.getSetCode());
        assertEquals("Mock Alpha Expansion", alpha.getSetName());
        assertTrue(alpha.isEnabled());
        assertEquals(50, alpha.getPriority());

        MockBetaExpansion beta = new MockBetaExpansion();
        assertEquals("beta", beta.getSetCode());
        assertEquals("Mock Beta Expansion", beta.getSetName());
        assertTrue(beta.isEnabled());
        assertEquals(80, beta.getPriority());
    }

    @Test
    @DisplayName("2. BasicLandsExpansion registers the 6 Basic Lands and their effects")
    void testBasicLandsExpansionRegistration() {
        BasicLandsExpansion base = new BasicLandsExpansion();
        ExpansionRegistry.registerExpansion(base);

        Optional<IExpansionSet> retrieved = ExpansionRegistry.getExpansion("base");
        assertTrue(retrieved.isPresent());
        assertEquals("Magic: The Gathering Base Library (Basic Lands)", retrieved.get().getSetName());

        // Case-insensitive retrieval
        Optional<IExpansionSet> retrievedUpper = ExpansionRegistry.getExpansion("BASE");
        assertTrue(retrievedUpper.isPresent());
        assertSame(retrieved.get(), retrievedUpper.get());

        KeywordRegistry kwRegistry = new KeywordRegistry();
        TriggerBus bus = new TriggerBus();
        Map<String, CardDefinition> registeredCards = new HashMap<>();

        ExpansionRegistry.initializeAll(card -> registeredCards.put(card.id(), card), kwRegistry, bus);

        assertEquals(6, registeredCards.size());
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.PLAINS.id()));
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.ISLAND.id()));
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.SWAMP.id()));
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.MOUNTAIN.id()));
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.FOREST.id()));
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.WASTES.id()));

        // Verify land effect handlers registered
        assertTrue(CardEffectRegistry.get(BasicLandsCatalog.PLAINS.id()).isPresent());
        assertTrue(CardEffectRegistry.get(BasicLandsCatalog.FOREST.id()).isPresent());
    }

    @Test
    @DisplayName("3. Multi-expansion compatibility and priority ordering (Base 100 > Beta 80 > Alpha 50)")
    void testMultiExpansionCompatibilityAndPriorityOrdering() {
        List<String> executionOrder = new ArrayList<>();

        IExpansionSet loggingBase = new BasicLandsExpansion() {
            @Override
            public void registerCards(Consumer<CardDefinition> registry) {
                executionOrder.add("base");
                super.registerCards(registry);
            }
        };

        MockAlphaExpansion alpha = new MockAlphaExpansion() {
            @Override
            public void registerCards(Consumer<CardDefinition> registry) {
                executionOrder.add("alpha");
                super.registerCards(registry);
            }
        };

        MockBetaExpansion beta = new MockBetaExpansion() {
            @Override
            public void registerCards(Consumer<CardDefinition> registry) {
                executionOrder.add("beta");
                super.registerCards(registry);
            }
        };

        // Register in reverse order of priority to test sorting
        ExpansionRegistry.registerExpansion(alpha); // Priority 50
        ExpansionRegistry.registerExpansion(loggingBase); // Priority 100
        ExpansionRegistry.registerExpansion(beta);  // Priority 80

        assertEquals(3, ExpansionRegistry.getAllExpansions().size());

        KeywordRegistry kwRegistry = new KeywordRegistry();
        TriggerBus bus = new TriggerBus();
        Map<String, CardDefinition> registeredCards = new HashMap<>();

        ExpansionRegistry.initializeAll(card -> registeredCards.put(card.id(), card), kwRegistry, bus);

        // Verification of priority order: 100 (base) -> 80 (beta) -> 50 (alpha)
        assertEquals(List.of("base", "beta", "alpha"), executionOrder,
                "Expansions must initialize in strictly descending order of priority");

        // Verification of multi-expansion card coexistence
        assertTrue(registeredCards.containsKey(BasicLandsCatalog.PLAINS.id()), "Base lands must be present");
        assertTrue(registeredCards.containsKey("mtg:alpha/knight"), "Alpha card must be present");
        assertTrue(registeredCards.containsKey("mtg:beta/mage"), "Beta card must be present");

        // Verification of multi-expansion keyword coexistence
        assertTrue(kwRegistry.has("mtg:alpha_strike"), "Alpha keyword must be registered");
        assertTrue(kwRegistry.has("mtg:beta_ward"), "Beta keyword must be registered");

        // Verification of effects
        assertTrue(CardEffectRegistry.get("mtg:alpha/knight").isPresent());
        assertTrue(CardEffectRegistry.get("mtg:beta/mage").isPresent());
    }

    @Test
    @DisplayName("4. ServiceLoader discovery executes safely and returns discovered expansion list")
    void testServiceLoaderDiscovery() {
        List<IExpansionSet> discovered = ExpansionRegistry.discoverAndRegisterViaServiceLoader();
        assertNotNull(discovered, "ServiceLoader discovered list must not be null");

        // Ensure all discovered expansions are present in ExpansionRegistry
        for (IExpansionSet exp : discovered) {
            assertTrue(ExpansionRegistry.getExpansion(exp.getSetCode()).isPresent());
        }
    }

    @Test
    void equalPriorityAddonsRunAllHooksInSetCodeOrder() {
        List<String> hooks = new ArrayList<>();
        BossManager bosses = new BossManager();
        JournalBookModel guidebook = new JournalBookModel();
        ExpansionRegistry.registerExpansion(new MockBetaExpansion() {
            @Override public int getPriority() { return 50; }
            @Override public void registerBosses(BossManager manager) {
                assertSame(bosses, manager);
                hooks.add("beta:bosses");
            }
            @Override public void registerGuidebookContent(JournalBookModel journal) {
                assertSame(guidebook, journal);
                hooks.add("beta:guidebook");
            }
        });
        ExpansionRegistry.registerExpansion(new MockAlphaExpansion() {
            @Override public void registerBosses(BossManager manager) {
                assertSame(bosses, manager);
                hooks.add("alpha:bosses");
            }
            @Override public void registerGuidebookContent(JournalBookModel journal) {
                assertSame(guidebook, journal);
                hooks.add("alpha:guidebook");
            }
        });

        List<CardDefinition> cards = new ArrayList<>();
        ExpansionRegistry.initializeAll(cards::add, new KeywordRegistry(), new TriggerBus(), bosses, guidebook);

        assertEquals(List.of("mtg:alpha/knight", "mtg:beta/mage"), cards.stream().map(CardDefinition::id).toList());
        assertEquals(List.of("alpha:bosses", "alpha:guidebook", "beta:bosses", "beta:guidebook"), hooks);
    }

    @Test
    void rejectsACardIdAlreadyOwnedByAnotherSet() {
        ExpansionRegistry.registerExpansion(new BasicLandsExpansion());
        ExpansionRegistry.registerExpansion(new MockAlphaExpansion() {
            @Override public void registerCards(Consumer<CardDefinition> registry) {
                registry.accept(BasicLandsCatalog.PLAINS);
            }
        });

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ExpansionRegistry.initializeAll(card -> {}, new KeywordRegistry(), new TriggerBus()));

        assertTrue(error.getMessage().contains("mtg:base/plains"));
        assertTrue(error.getMessage().contains("base and alpha"));
    }

    @Test
    @DisplayName("5. Duplicate expansion set registration with identical set code throws IllegalStateException")
    void testDuplicateSetRegistrationCollision() {
        BasicLandsExpansion base1 = new BasicLandsExpansion();
        ExpansionRegistry.registerExpansion(base1);

        // Attempting to register another set with same code "base"
        IExpansionSet base2 = new IExpansionSet() {
            @Override public String getSetCode() { return "base"; }
            @Override public String getSetName() { return "Duplicate Base"; }
            @Override public void registerCards(Consumer<CardDefinition> registry) {}
            @Override public void registerKeywords(KeywordRegistry registry) {}
            @Override public void registerTriggers(TriggerBus bus) {}
        };

        assertThrows(IllegalStateException.class, () -> ExpansionRegistry.registerExpansion(base2));

        // Attempting case-insensitive duplicate ("BASE")
        IExpansionSet baseUpper = new IExpansionSet() {
            @Override public String getSetCode() { return "BASE"; }
            @Override public String getSetName() { return "Duplicate Upper Base"; }
            @Override public void registerCards(Consumer<CardDefinition> registry) {}
            @Override public void registerKeywords(KeywordRegistry registry) {}
            @Override public void registerTriggers(TriggerBus bus) {}
        };

        assertThrows(IllegalStateException.class, () -> ExpansionRegistry.registerExpansion(baseUpper));
    }

    @Test
    @DisplayName("6. Querying unregistered set code returns empty Optional without NPE")
    void testUnregisteredSetQueryReturnsEmpty() {
        Optional<IExpansionSet> result = ExpansionRegistry.getExpansion("MH3");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        Optional<IExpansionSet> nullResult = ExpansionRegistry.getExpansion(null);
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    @DisplayName("7. Disabled expansion set is skipped during initializeAll")
    void testDisabledExpansionSkippedDuringInitialization() {
        MockAlphaExpansion disabledAlpha = new MockAlphaExpansion() {
            @Override
            public boolean isEnabled() {
                return false;
            }
        };

        ExpansionRegistry.registerExpansion(disabledAlpha);

        KeywordRegistry kwRegistry = new KeywordRegistry();
        TriggerBus bus = new TriggerBus();
        Map<String, CardDefinition> registeredCards = new HashMap<>();

        ExpansionRegistry.initializeAll(card -> registeredCards.put(card.id(), card), kwRegistry, bus);

        assertFalse(kwRegistry.has("mtg:alpha_strike"), "Disabled expansion keywords should not be registered");
        assertFalse(registeredCards.containsKey("mtg:alpha/knight"), "Disabled expansion cards should not be registered");
    }
}
