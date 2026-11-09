package com.mtg.test.e2e.tier1;

import com.mtg.api.ExpansionRegistry;
import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.api.trigger.TriggerEvent;
import com.mtg.api.trigger.TriggerPriority;
import com.mtg.cards.catalog.BasicLandsExpansion;
import com.mtg.domain.CardDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 05: Modular Expansion APIs E2E Test")
public class Feature05_ModularExpansionApisTest {

    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("SKILL.md"))) {
            current = current.getParent();
        }
        return current != null ? current : Paths.get(".").toAbsolutePath();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        ExpansionRegistry.clear();
    }

    @Test
    @DisplayName("1. ExpansionRegistry manages registration and retrieval of modular sets and verifies archive presence")
    void testExpansionRegistryLifecycle() {
        BasicLandsExpansion base = new BasicLandsExpansion();
        IExpansionSet synthetic = E2ETestExpansionHelper.createSyntheticExpansion("syn", "Synthetic Expansion");

        ExpansionRegistry.registerExpansion(base);
        ExpansionRegistry.registerExpansion(synthetic);

        assertEquals(2, ExpansionRegistry.getAllExpansions().size());
        assertTrue(ExpansionRegistry.getExpansion("base").isPresent());
        assertTrue(ExpansionRegistry.getExpansion("syn").isPresent());
        assertEquals("Magic: The Gathering Base Library (Basic Lands)", ExpansionRegistry.getExpansion("base").get().getSetName());
        assertEquals("Synthetic Expansion", ExpansionRegistry.getExpansion("syn").get().getSetName());

        // Verify archived expansions exist on disk
        Path archivedDir = findProjectRoot().resolve("archived-expansions");
        assertTrue(Files.isDirectory(archivedDir), "archived-expansions/ must exist on disk");
        assertTrue(Files.isDirectory(archivedDir.resolve("core-set")), "archived-expansions/core-set must exist");
        assertTrue(Files.isDirectory(archivedDir.resolve("phyrexia-one")), "archived-expansions/phyrexia-one must exist");
        assertTrue(Files.isDirectory(archivedDir.resolve("zendikar-rising")), "archived-expansions/zendikar-rising must exist");
    }

    @Test
    @DisplayName("2. Basic Lands expansion exports set cards and registers lifecycle hooks")
    void testBasicLandsContractRegistration() {
        BasicLandsExpansion base = new BasicLandsExpansion();
        List<CardDefinition> cards = new ArrayList<>();
        base.registerCards(cards::add);

        assertFalse(cards.isEmpty(), "Basic lands expansion must register cards");
        assertEquals(6, cards.size(), "Must register all 6 canonical basic lands");
        assertTrue(cards.stream().anyMatch(c -> c.id().contains("plains")), "Must register Plains");
        assertTrue(cards.stream().anyMatch(c -> c.id().contains("wastes")), "Must register Wastes");

        TriggerBus bus = new TriggerBus();
        base.registerTriggers(bus);
        assertNotNull(bus, "Trigger bus registration must execute cleanly");
    }

    @Test
    @DisplayName("3. Synthetic modular expansion exports set cards and registers custom trigger hooks")
    void testSyntheticModularExpansionRegistration() {
        AtomicBoolean customTriggerRegistered = new AtomicBoolean(false);
        class CustomExpansionEvent extends TriggerEvent {
            public CustomExpansionEvent(Object source) { super(source); }
        }

        IExpansionSet customSet = new IExpansionSet() {
            @Override
            public String getSetCode() { return "custom_m4"; }
            @Override
            public String getSetName() { return "Custom M4 Expansion"; }
            @Override
            public void registerCards(java.util.function.Consumer<CardDefinition> registry) {
                E2ETestExpansionHelper.getSyntheticCards().forEach(registry);
            }
            @Override
            public void registerKeywords(KeywordRegistry registry) {}
            @Override
            public void registerTriggers(TriggerBus bus) {
                bus.registerListener(CustomExpansionEvent.class, e -> customTriggerRegistered.set(true));
            }
        };

        List<CardDefinition> cards = new ArrayList<>();
        customSet.registerCards(cards::add);
        assertFalse(cards.isEmpty(), "Custom expansion must register synthetic cards");

        TriggerBus bus = new TriggerBus();
        customSet.registerTriggers(bus);
        bus.post(new CustomExpansionEvent(this));
        assertTrue(customTriggerRegistered.get(), "Custom expansion trigger must fire on bus");
    }

    @Test
    @DisplayName("4. TriggerBus delivers dispatched events to registered listeners deterministically")
    void testTriggerBusDispatchPipeline() {
        TriggerBus bus = new TriggerBus();
        AtomicBoolean received = new AtomicBoolean(false);

        class SampleTriggerEvent extends TriggerEvent {
            public SampleTriggerEvent(Object source) {
                super(source);
            }
        }

        bus.registerListener(SampleTriggerEvent.class, TriggerPriority.NORMAL, e -> received.set(true));
        bus.post(new SampleTriggerEvent(this));

        assertTrue(received.get(), "Registered listener must receive posted event");
    }

    @Test
    @DisplayName("5. KeywordRegistry dynamic registration tracks custom keywords and categories")
    void testKeywordRegistryRegistration() {
        KeywordRegistry registry = new KeywordRegistry();
        KeywordDefinition customKeyword = KeywordDefinition.of("test:corrupted_burst", "Corrupted Burst", "Reminder", KeywordCategory.ACTION);

        registry.register(customKeyword);
        Optional<KeywordDefinition> retrieved = registry.get("test:corrupted_burst");

        assertTrue(retrieved.isPresent(), "Custom keyword must be registered");
        assertEquals(KeywordCategory.ACTION, retrieved.get().category());
    }
}
