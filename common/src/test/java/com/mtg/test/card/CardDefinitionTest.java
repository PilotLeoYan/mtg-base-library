package com.mtg.test.card;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CardDefinition Validation, Lifecycle, and Serialization Suite")
public class CardDefinitionTest {

    @Test
    @DisplayName("Create valid Creature CardDefinition (Serra Angel) with CombatStats and Keywords")
    void testCreateValidCreatureDefinition() {
        ManaCost cost = new ManaCost(3, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE); // {3}{W}{W}
        CardDefinition serraAngel = CardDefinition.builder("mtg:base/serra_angel", "base", "Serra Angel")
            .manaCost(cost)
            .addType(CardType.CREATURE)
            .addSubtype("Angel")
            .rarity(CardRarity.UNCOMMON)
            .combatStats(4, 4)
            .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
            .addKeyword(KeywordInstance.of(CoreKeywords.VIGILANCE))
            .baseCooldownTicks(100)
            .windupDelayTicks(0)
            .build();

        assertNotNull(serraAngel);
        assertEquals("mtg:base/serra_angel", serraAngel.id());
        assertEquals("base", serraAngel.setCode());
        assertEquals("Serra Angel", serraAngel.name());
        assertEquals(5, serraAngel.manaCost().totalManaValue());
        assertTrue(serraAngel.isCreature());
        assertFalse(serraAngel.isLand());
        assertFalse(serraAngel.isInstant());
        assertEquals(CardRarity.UNCOMMON, serraAngel.rarity());

        assertTrue(serraAngel.combatStats().isPresent());
        assertEquals(4, serraAngel.combatStats().get().power());
        assertEquals(4, serraAngel.combatStats().get().toughness());

        assertTrue(serraAngel.hasKeyword("mtg:flying"));
        assertTrue(serraAngel.hasKeyword("mtg:vigilance"));
        assertFalse(serraAngel.hasKeyword("mtg:deathtouch"));
        assertEquals(100, serraAngel.baseCooldownTicks());
        assertEquals(0, serraAngel.windupDelayTicks());
    }

    @Test
    @DisplayName("Create valid Instant CardDefinition (Lightning Bolt) without cast delay")
    void testCreateValidInstantDefinition() {
        CardDefinition bolt = CardDefinition.builder("mtg:base/lightning_bolt", "base", "Lightning Bolt")
            .manaCost(ManaCost.ofRed(1))
            .addType(CardType.INSTANT)
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(60)
            .windupDelayTicks(0)
            .build();

        assertTrue(bolt.isInstant());
        assertFalse(bolt.isCreature());
        assertFalse(bolt.isLand());
        assertEquals(1, bolt.manaCost().red());
        assertEquals(1, bolt.manaCost().totalManaValue());
        assertEquals(60, bolt.baseCooldownTicks());
        assertEquals(0, bolt.windupDelayTicks());
        assertTrue(bolt.combatStats().isEmpty());
    }

    @Test
    @DisplayName("Create valid Sorcery CardDefinition (Wrath of God) with windup delay")
    void testCreateValidSorceryDefinition() {
        CardDefinition wrath = CardDefinition.builder("mtg:base/wrath_of_god", "base", "Wrath of God")
            .manaCost(new ManaCost(2, 2, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE))
            .addType(CardType.SORCERY)
            .rarity(CardRarity.RARE)
            .baseCooldownTicks(200)
            .windupDelayTicks(40)
            .build();

        assertTrue(wrath.isSorcery());
        assertFalse(wrath.isInstant());
        assertEquals(4, wrath.manaCost().totalManaValue());
        assertEquals(40, wrath.windupDelayTicks());
        assertEquals(CardRarity.RARE, wrath.rarity());
    }

    @Test
    @DisplayName("Create valid Land CardDefinition (Forest) verifying permanent and spell rules")
    void testCreateValidLandDefinition() {
        CardDefinition forest = CardDefinition.builder("mtg:base/forest", "base", "Forest")
            .manaCost(ManaCost.ZERO)
            .addType(CardType.LAND)
            .addSubtype("Forest")
            .rarity(CardRarity.COMMON)
            .baseCooldownTicks(1200)
            .build();

        assertTrue(forest.isLand());
        assertFalse(forest.isCreature());
        assertTrue(forest.types().iterator().next().isPermanent());
        assertFalse(forest.types().iterator().next().isSpell());
        assertEquals(0, forest.manaCost().totalManaValue());
    }

    @Test
    @DisplayName("Negative mana cost values throw IllegalArgumentException")
    void testNegativeManaCostRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ManaCost(-1, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, -1, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE));
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, -1, 0, 0, 0));
    }

    @Test
    @DisplayName("Missing ID, null types, empty types, or null rarity are rejected")
    void testMissingIdOrTypesRejected() {
        assertThrows(NullPointerException.class, () ->
            new CardDefinition(null, "base", "Card", ManaCost.ZERO, Set.of(CardType.INSTANT), Set.of(), CardRarity.COMMON, null, null, 10, 0)
        );
        assertThrows(NullPointerException.class, () ->
            new CardDefinition("id", null, "Card", ManaCost.ZERO, Set.of(CardType.INSTANT), Set.of(), CardRarity.COMMON, null, null, 10, 0)
        );
        assertThrows(NullPointerException.class, () ->
            new CardDefinition("id", "base", null, ManaCost.ZERO, Set.of(CardType.INSTANT), Set.of(), CardRarity.COMMON, null, null, 10, 0)
        );
        assertThrows(NullPointerException.class, () ->
            new CardDefinition("id", "base", "Card", ManaCost.ZERO, Set.of(CardType.INSTANT), Set.of(), null, null, null, 10, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new CardDefinition("id", "base", "Card", ManaCost.ZERO, Set.of(), Set.of(), CardRarity.COMMON, null, null, 10, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new CardDefinition("id", "base", "Card", ManaCost.ZERO, Set.of(CardType.INSTANT), Set.of(), CardRarity.COMMON, null, null, -1, 0)
        );
    }

    @Test
    @DisplayName("CardDefinition collections are immutable")
    void testCardDefinitionImmutability() {
        CardDefinition bolt = CardDefinition.builder("mtg:base/lightning_bolt", "base", "Lightning Bolt")
            .manaCost(ManaCost.ofRed(1))
            .addType(CardType.INSTANT)
            .addSubtype("Arc")
            .rarity(CardRarity.COMMON)
            .build();

        assertThrows(UnsupportedOperationException.class, () -> bolt.types().add(CardType.SORCERY));
        assertThrows(UnsupportedOperationException.class, () -> bolt.subtypes().add("Fire"));
        assertThrows(UnsupportedOperationException.class, () -> bolt.keywords().clear());
    }

    @Test
    @DisplayName("CardDefinition JSON serialization round-trip succeeds across all fields")
    void testCardDefinitionJsonSerializationRoundTrip() {
        KeywordDefinition testKeyword = KeywordDefinition.of("mtg:landfall", "Landfall", "Triggers when land enters", KeywordCategory.TRIGGERED);
        CardDefinition cobra = CardDefinition.builder("mtg:test/lotus_cobra", "test", "Lotus Cobra")
            .manaCost(new ManaCost(1, 0, 0, 0, 0, 1, 0, PhyrexianManaCost.NONE))
            .addType(CardType.CREATURE)
            .addSubtype("Snake")
            .rarity(CardRarity.RARE)
            .combatStats(2, 1)
            .addKeyword(KeywordInstance.of(testKeyword))
            .baseCooldownTicks(100)
            .windupDelayTicks(0)
            .build();

        String json = cobra.toJson();
        assertNotNull(json);
        assertTrue(json.contains("mtg:test/lotus_cobra"));
        assertTrue(json.contains("Lotus Cobra"));

        CardDefinition deserialized = CardDefinition.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(cobra.id(), deserialized.id());
        assertEquals(cobra.setCode(), deserialized.setCode());
        assertEquals(cobra.name(), deserialized.name());
        assertEquals(cobra.manaCost().generic(), deserialized.manaCost().generic());
        assertEquals(cobra.manaCost().green(), deserialized.manaCost().green());
        assertEquals(cobra.types(), deserialized.types());
        assertEquals(cobra.subtypes(), deserialized.subtypes());
        assertEquals(cobra.rarity(), deserialized.rarity());
        assertEquals(cobra.combatStats(), deserialized.combatStats());
        assertEquals(cobra.baseCooldownTicks(), deserialized.baseCooldownTicks());
        assertEquals(cobra.windupDelayTicks(), deserialized.windupDelayTicks());
        assertEquals(1, deserialized.keywords().size());
        assertEquals("mtg:landfall", deserialized.keywords().get(0).getKeywordId());
    }

    @Test
    @DisplayName("Malformed JSON throws CardParseException without crashing JVM")
    void testMalformedJsonHandling() {
        assertThrows(CardParseException.class, () -> CardDefinition.fromJson(null));
        assertThrows(CardParseException.class, () -> CardDefinition.fromJson(""));
        assertThrows(CardParseException.class, () -> CardDefinition.fromJson("{ invalid json"));
        assertThrows(CardParseException.class, () -> CardDefinition.fromJson("{\"id\": 123}"));
        assertThrows(CardParseException.class, () -> CardDefinition.fromJson("{\"id\": \"test\"}")); // missing setCode, name
    }
}
