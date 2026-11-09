package com.mtg.test.e2e.tier1;

import com.mtg.api.ExpansionRegistry;
import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.cards.catalog.BasicLandsExpansion;
import com.mtg.cards.effects.ActiveEnchantment;
import com.mtg.cards.effects.IEnchantmentTickListener;
import com.mtg.deck.DeckCardSlot;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import com.mtg.entity.ITappablePermanent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 04: Project Skill (SKILL.md) & SOLID Architecture E2E Test")
public class Feature04_ProjectSkillAndSolidTest {

    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("SKILL.md"))) {
            current = current.getParent();
        }
        return current != null ? current : Paths.get(".").toAbsolutePath();
    }

    @Test
    @DisplayName("1. SKILL.md file exists at project root with frontmatter metadata and SOLID sections")
    void testSkillFilePresenceAndMetadata() throws Exception {
        Path skillPath = findProjectRoot().resolve("SKILL.md");
        assertTrue(Files.exists(skillPath), "SKILL.md must exist at project root");

        String content = Files.readString(skillPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("name: mtg-expansion-framework"), "SKILL.md must contain frontmatter name");
        assertTrue(content.contains("version:"), "SKILL.md must contain frontmatter version");
        assertTrue(content.contains("SOLID Principles"), "SKILL.md must contain SOLID principles section");
        assertTrue(content.contains("Single Responsibility Principle"), "SKILL.md must document SRP");
        assertTrue(content.contains("Open/Closed Principle"), "SKILL.md must document OCP");
    }

    @Test
    @DisplayName("2. Single Responsibility Principle (SRP): Immutable card metadata separated from mutable slot state")
    void testSingleResponsibilitySeparation() {
        CardDefinition definition = E2ETestExpansionHelper.TEST_LIGHTNING_BOLT;
        assertNotNull(definition);
        assertEquals("mtg:test/lightning_bolt", definition.id());
        assertEquals(60, definition.baseCooldownTicks());

        // Slot state is isolated, mutable, and tracks runtime tick progression independently
        DeckCardSlot slot = new DeckCardSlot(0);
        slot.equip("mtg:test/lightning_bolt", 60, 0);
        slot.startCooldown(60);
        assertEquals(60, slot.getCooldownRemainingTicks());

        slot.tick();
        assertEquals(59, slot.getCooldownRemainingTicks(), "Slot tick mutation must not affect CardDefinition");
        assertEquals(60, definition.baseCooldownTicks(), "CardDefinition must remain completely immutable");
    }

    @Test
    @DisplayName("3. Open/Closed Principle (OCP): New expansion sets extend functionality without modifying core classes")
    void testOpenClosedPrincipleExpansion() {
        ExpansionRegistry.clear();

        // 3a. Register pure base BasicLandsExpansion
        BasicLandsExpansion baseExp = new BasicLandsExpansion();
        ExpansionRegistry.registerExpansion(baseExp);
        assertTrue(ExpansionRegistry.getExpansion(BasicLandsExpansion.SET_CODE).isPresent());

        // 3b. Register synthetic custom expansion
        String testSetCode = "custom_test_set";
        IExpansionSet customExpansion = new IExpansionSet() {
            @Override
            public String getSetCode() { return testSetCode; }
            @Override
            public String getSetName() { return "Custom Test Expansion"; }
            @Override
            public void registerCards(Consumer<CardDefinition> registry) {
                registry.accept(CardDefinition.builder("custom:sample_card", testSetCode, "Sample")
                        .manaCost(ManaCost.ZERO)
                        .addType(CardType.INSTANT)
                        .rarity(CardRarity.COMMON)
                        .build());
            }
            @Override
            public void registerKeywords(KeywordRegistry registry) {}
            @Override
            public void registerTriggers(TriggerBus bus) {}
        };

        ExpansionRegistry.registerExpansion(customExpansion);
        assertTrue(ExpansionRegistry.getExpansion(testSetCode).isPresent(), "ExpansionRegistry should host new expansion");
        assertEquals("Custom Test Expansion", ExpansionRegistry.getExpansion(testSetCode).get().getSetName());

        // 3c. Verify archived-expansions directory structure exists with all 3 archived sets
        Path root = findProjectRoot();
        Path archivedDir = root.resolve("archived-expansions");
        assertTrue(Files.isDirectory(archivedDir), "archived-expansions/ must exist on disk");
        assertTrue(Files.isDirectory(archivedDir.resolve("core-set")), "archived-expansions/core-set must exist");
        assertTrue(Files.isDirectory(archivedDir.resolve("phyrexia-one")), "archived-expansions/phyrexia-one must exist");
        assertTrue(Files.isDirectory(archivedDir.resolve("zendikar-rising")), "archived-expansions/zendikar-rising must exist");

        ExpansionRegistry.clear();
    }

    @Test
    @DisplayName("4. Liskov Substitution Principle (LSP): All card types conform to standard CardDefinition contracts")
    void testLiskovSubstitutionCardTypes() {
        Set<CardDefinition> cards = Set.of(
                BasicLandsCatalog.PLAINS,                          // LAND
                E2ETestExpansionHelper.TEST_LIGHTNING_BOLT,       // INSTANT
                E2ETestExpansionHelper.TEST_PYROCLASM,            // SORCERY
                E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM,      // ENCHANTMENT
                E2ETestExpansionHelper.TEST_SERRA_ANGEL           // CREATURE
        );

        for (CardDefinition card : cards) {
            assertNotNull(card.id(), "All card types must provide valid ID");
            assertNotNull(card.manaCost(), "All card types must provide valid ManaCost");
            assertFalse(card.types().isEmpty(), "All card types must declare at least one CardType");
            assertTrue(card.baseCooldownTicks() >= 0, "All card types must have non-negative cooldown");
        }
    }

    @Test
    @DisplayName("5. Interface Segregation Principle (ISP): Granular mechanics interfaces are segregated")
    void testInterfaceSegregationPrinciple() {
        class MockEnchantmentListener implements IEnchantmentTickListener {
            private boolean expired = false;
            @Override
            public void onExpire(ActiveEnchantment enchantment) {
                this.expired = true;
            }
            public boolean isExpired() { return expired; }
        }

        class MockTappable implements ITappablePermanent {
            private boolean active = false;
            @Override public boolean canActivate(UUID pId) { return !active; }
            @Override public boolean activate(UUID pId) { active = true; return true; }
        }

        MockEnchantmentListener listenerObj = new MockEnchantmentListener();
        MockTappable tapObj = new MockTappable();

        assertTrue(listenerObj instanceof IEnchantmentTickListener);
        assertFalse(listenerObj instanceof ITappablePermanent, "Enchantment listener does not implement ITappablePermanent");
        assertTrue(tapObj instanceof ITappablePermanent);
        assertFalse(tapObj instanceof IEnchantmentTickListener, "Tappable does not implement IEnchantmentTickListener");
    }
}
