package com.mtg.test.card;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;
import com.mtg.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — CardDefinition JSON & Immutability")
public class CardDefinitionAdversarialTest {

    @Nested
    @DisplayName("1. Fuzzing & Malformed JSON Token Stress")
    class MalformedJsonStressTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            "   ",
            "\n\t\r",
            "{",
            "}",
            "[",
            "]",
            "null",
            "true",
            "false",
            "12345",
            "\"just a string\"",
            "[1, 2, 3]",
            "{\"id\": }",
            "{\"id\": \"test\",",
            "{\"id\": \"test\", \"setCode\": \"base\", \"name\": \"N\", \"types\": [\"LAND\"], }", // trailing comma
            "{\"id\": \"test\" \"setCode\": \"base\"}", // missing comma
            "{\"id\": \"test\\", // unclosed escape
            "{\"name\": \"unclosed string",
            "{\"types\": [\"LAND\"", // unclosed array
            "{\"manaCost\": {\"generic\": 1", // unclosed object
            "{\"id\": \"x\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"combatStats\": {", // unclosed nested
            "{\"id\": \"x\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"keywords\": [{\"id\":",
            "// comment only",
            "/* block comment */",
            "{\"id\": \"x\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": -}",
            "{\"id\": \"x\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": 12.34.56}",
            "{\"id\": \"x\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": abc}",
            "{\"id\": \"x\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": 9999999999999999999999999999999999999999999999999999999}"
        })
        @DisplayName("Malformed or truncated JSON tokens must throw CardParseException without unhandled JVM crash")
        void testMalformedJsonThrowsCardParseException(String malformedJson) {
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(malformedJson));
        }

        @Test
        @DisplayName("Null JSON string throws CardParseException")
        void testNullJsonThrows() {
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(null));
        }

        @Test
        @DisplayName("Non-object root element throws CardParseException with descriptive message")
        void testNonObjectRootThrows() {
            CardParseException ex = assertThrows(CardParseException.class, () -> CardDefinition.fromJson("[\"item1\", \"item2\"]"));
            assertTrue(ex.getMessage().contains("Root JSON element must be an object"));
        }
    }

    @Nested
    @DisplayName("2. Missing Keys & Structural Validation")
    class MissingKeysStressTests {

        @Test
        @DisplayName("Missing 'id' throws CardParseException")
        void testMissingId() {
            String json = """
                {
                    "setCode": "base",
                    "name": "Island",
                    "types": ["LAND"]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Missing 'setCode' throws CardParseException")
        void testMissingSetCode() {
            String json = """
                {
                    "id": "mtg:base/island",
                    "name": "Island",
                    "types": ["LAND"]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Missing 'name' throws CardParseException")
        void testMissingName() {
            String json = """
                {
                    "id": "mtg:base/island",
                    "setCode": "base",
                    "types": ["LAND"]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Missing 'types' array throws CardParseException")
        void testMissingTypes() {
            String json = """
                {
                    "id": "mtg:base/island",
                    "setCode": "base",
                    "name": "Island"
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Empty 'types' array throws CardParseException")
        void testEmptyTypes() {
            String json = """
                {
                    "id": "mtg:base/island",
                    "setCode": "base",
                    "name": "Island",
                    "types": []
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Wrong attribute types (e.g. numeric ID) throws CardParseException")
        void testWrongAttributeType() {
            String json = """
                {
                    "id": 12345,
                    "setCode": "base",
                    "name": "Island",
                    "types": ["LAND"]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Missing optional fields properly populate defaults without NPE")
        void testMissingOptionalFieldsDefaultGracefully() {
            String minimalJson = """
                {
                    "id": "mtg:base/minimal_land",
                    "setCode": "base",
                    "name": "Minimal Land",
                    "types": ["LAND"]
                }
                """;
            CardDefinition card = CardDefinition.fromJson(minimalJson);
            assertNotNull(card);
            assertEquals("mtg:base/minimal_land", card.id());
            assertEquals("base", card.setCode());
            assertEquals("Minimal Land", card.name());
            assertEquals(ManaCost.ZERO, card.manaCost());
            assertEquals(Set.of(CardType.LAND), card.types());
            assertTrue(card.subtypes().isEmpty());
            assertEquals(CardRarity.COMMON, card.rarity());
            assertTrue(card.combatStats().isEmpty());
            assertTrue(card.keywords().isEmpty());
            assertEquals(100, card.baseCooldownTicks());
            assertEquals(0, card.windupDelayTicks());
        }

        @Test
        @DisplayName("Empty sub-objects in manaCost, combatStats, and subtypes default gracefully")
        void testEmptySubObjectsDefaultGracefully() {
            String json = """
                {
                    "id": "mtg:base/blank_stats",
                    "setCode": "base",
                    "name": "Blank Stats",
                    "types": ["CREATURE"],
                    "manaCost": {
                        "phyrexian": {}
                    },
                    "combatStats": {},
                    "subtypes": [],
                    "keywords": []
                }
                """;
            CardDefinition card = CardDefinition.fromJson(json);
            assertNotNull(card);
            assertEquals(0, card.manaCost().totalManaValue());
            assertEquals(PhyrexianManaCost.NONE, card.manaCost().phyrexian());
            assertTrue(card.combatStats().isPresent());
            assertEquals(0, card.combatStats().get().power());
            assertEquals(0, card.combatStats().get().toughness());
            assertTrue(card.subtypes().isEmpty());
            assertTrue(card.keywords().isEmpty());
        }
    }

    @Nested
    @DisplayName("3. Negative Costs & Numeric Bounds Rejection")
    class NegativeBoundsStressTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"generic\": -1}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"white\": -1}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"blue\": -2}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"black\": -3}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"red\": -4}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"green\": -5}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"colorless\": -1}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"phyrexian\": {\"white\": -1}}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"phyrexian\": {\"blue\": -1}}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"phyrexian\": {\"black\": -1}}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"phyrexian\": {\"red\": -1}}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"phyrexian\": {\"green\": -1}}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"manaCost\": {\"phyrexian\": {\"colorless\": -1}}}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"baseCooldownTicks\": -1}",
            "{\"id\": \"c\", \"setCode\": \"s\", \"name\": \"N\", \"types\": [\"CREATURE\"], \"windupDelayTicks\": -1}"
        })
        @DisplayName("Negative mana costs, cooldowns, or windups in JSON throw CardParseException")
        void testNegativeValuesInJsonThrow(String jsonWithNegative) {
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(jsonWithNegative));
        }

        @Test
        @DisplayName("Direct constructor and builder reject negative mana costs and delays")
        void testDirectConstructionRejectsNegatives() {
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(-1, 0, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, -1, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, 0, -1, 0, 0, 0, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, 0, 0, -1, 0, 0, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, 0, 0, 0, -1, 0, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, 0, 0, 0, 0, -1, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, 0, 0, 0, 0, 0, -1, PhyrexianManaCost.NONE));

            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(-1, 0, 0, 0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, -1, 0, 0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, -1, 0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, -1, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, 0, -1, 0));
            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, 0, 0, -1));

            assertThrows(IllegalArgumentException.class, () ->
                CardDefinition.builder("id", "base", "name").baseCooldownTicks(-1).build()
            );
            assertThrows(IllegalArgumentException.class, () ->
                CardDefinition.builder("id", "base", "name").windupDelayTicks(-1).build()
            );
        }
    }

    @Nested
    @DisplayName("4. Malformed Enums & Unknown Tokens")
    class MalformedEnumStressTests {

        @Test
        @DisplayName("Unknown CardType string throws CardParseException")
        void testUnknownCardType() {
            String json = """
                {
                    "id": "mtg:base/card",
                    "setCode": "base",
                    "name": "Invalid Type Card",
                    "types": ["SPAGHETTI_MONSTER"]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Unknown CardRarity string throws CardParseException")
        void testUnknownRarity() {
            String json = """
                {
                    "id": "mtg:base/card",
                    "setCode": "base",
                    "name": "Invalid Rarity Card",
                    "types": ["LAND"],
                    "rarity": "ULTRA_LEGENDARY"
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Unknown KeywordCategory string throws CardParseException")
        void testUnknownKeywordCategory() {
            String json = """
                {
                    "id": "mtg:base/card",
                    "setCode": "base",
                    "name": "Invalid Keyword Cat",
                    "types": ["CREATURE"],
                    "keywords": [
                        {"id": "fly", "name": "Fly", "category": "IMPOSSIBLE_CAT"}
                    ]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }

        @Test
        @DisplayName("Keyword with null id throws CardParseException")
        void testKeywordWithNullId() {
            String json = """
                {
                    "id": "mtg:base/card",
                    "setCode": "base",
                    "name": "Null Keyword ID",
                    "types": ["CREATURE"],
                    "keywords": [
                        {"name": "No ID"}
                    ]
                }
                """;
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(json));
        }
    }

    @Nested
    @DisplayName("5. Extensibility & Forward Compatibility")
    class ForwardCompatibilityTests {

        @Test
        @DisplayName("Extra unrecognized fields in root and nested objects are ignored gracefully")
        void testUnrecognizedFieldsIgnored() {
            String json = """
                {
                    "id": "mtg:base/forward_test",
                    "setCode": "base",
                    "name": "Forward Compatible Card",
                    "types": ["INSTANT"],
                    "rarity": "UNCOMMON",
                    "manaCost": {
                        "red": 2,
                        "extraEnergyCost": 5
                    },
                    "flavorText": "A blast from the future.",
                    "artist": "John Avon",
                    "collectorNumber": 123,
                    "rulings": ["Can target planeswalkers."],
                    "nestedFuture": {
                        "enabled": true,
                        "tags": ["burn", "spell"]
                    }
                }
                """;
            CardDefinition card = CardDefinition.fromJson(json);
            assertNotNull(card);
            assertEquals("mtg:base/forward_test", card.id());
            assertEquals("Forward Compatible Card", card.name());
            assertTrue(card.isInstant());
            assertEquals(2, card.manaCost().red());
        }
    }

    @Nested
    @DisplayName("6. Immutability Attacks")
    class ImmutabilityStressTests {

        @Test
        @DisplayName("Direct mutation of CardDefinition collections throws UnsupportedOperationException")
        void testGetterImmutability() {
            CardDefinition card = CardDefinition.builder("mtg:test/angel", "test", "Angel")
                .addType(CardType.CREATURE)
                .addSubtype("Angel")
                .addKeyword(KeywordInstance.of(CoreKeywords.FLYING))
                .build();

            assertThrows(UnsupportedOperationException.class, () -> card.types().add(CardType.ENCHANTMENT));
            assertThrows(UnsupportedOperationException.class, () -> card.types().remove(CardType.CREATURE));
            assertThrows(UnsupportedOperationException.class, () -> card.types().clear());

            assertThrows(UnsupportedOperationException.class, () -> card.subtypes().add("Warrior"));
            assertThrows(UnsupportedOperationException.class, () -> card.subtypes().clear());

            assertThrows(UnsupportedOperationException.class, () -> card.keywords().add(KeywordInstance.of(CoreKeywords.HASTE)));
            assertThrows(UnsupportedOperationException.class, () -> card.keywords().remove(0));
            assertThrows(UnsupportedOperationException.class, () -> card.keywords().clear());
        }

        @Test
        @DisplayName("Mutating source collections after Builder submission does not mutate CardDefinition")
        void testBuilderSourceCollectionMutationIsolation() {
            Set<CardType> mutableTypes = new HashSet<>();
            mutableTypes.add(CardType.CREATURE);

            Set<String> mutableSubtypes = new HashSet<>();
            mutableSubtypes.add("Goblin");

            List<KeywordInstance> mutableKeywords = new ArrayList<>();
            mutableKeywords.add(KeywordInstance.of(CoreKeywords.HASTE));

            CardDefinition card = CardDefinition.builder("mtg:test/goblin", "test", "Goblin")
                .types(mutableTypes)
                .subtypes(mutableSubtypes)
                .keywords(mutableKeywords)
                .build();

            // Mutate source collections
            mutableTypes.add(CardType.ARTIFACT);
            mutableSubtypes.add("Shaman");
            mutableKeywords.add(KeywordInstance.of(CoreKeywords.DEATHTOUCH));

            // CardDefinition must NOT reflect source mutations
            assertEquals(Set.of(CardType.CREATURE), card.types(), "Card types must be isolated from source collection mutation");
            assertEquals(Set.of("Goblin"), card.subtypes(), "Card subtypes must be isolated from source collection mutation");
            assertEquals(1, card.keywords().size(), "Card keywords must be isolated from source collection mutation");
            assertEquals("mtg:haste", card.keywords().get(0).getKeywordId());
        }

        @Test
        @DisplayName("Mutating Builder after build() does not mutate previously built CardDefinition")
        void testBuilderMutationAfterBuildIsolation() {
            CardDefinition.Builder builder = CardDefinition.builder("mtg:test/sol_ring", "test", "Sol Ring")
                .addType(CardType.ARTIFACT)
                .manaCost(ManaCost.ofGeneric(1));

            CardDefinition card1 = builder.build();

            // Continue mutating builder
            builder.addType(CardType.CREATURE)
                .addSubtype("Golem")
                .baseCooldownTicks(500);

            CardDefinition card2 = builder.build();

            assertEquals(Set.of(CardType.ARTIFACT), card1.types());
            assertTrue(card1.subtypes().isEmpty());
            assertEquals(100, card1.baseCooldownTicks());

            assertEquals(Set.of(CardType.ARTIFACT, CardType.CREATURE), card2.types());
            assertEquals(Set.of("Golem"), card2.subtypes());
            assertEquals(500, card2.baseCooldownTicks());
        }

        @Test
        @DisplayName("Direct constructor defensive copying isolates mutable collections")
        void testConstructorDefensiveCopying() {
            Set<CardType> types = new HashSet<>(Set.of(CardType.INSTANT));
            Set<String> subtypes = new HashSet<>(Set.of("Arcane"));
            List<KeywordInstance> keywords = new ArrayList<>(List.of(KeywordInstance.of(CoreKeywords.DEATHTOUCH)));

            CardDefinition card = new CardDefinition(
                "mtg:test/arcane", "test", "Arcane Spell",
                ManaCost.ZERO, types, subtypes, CardRarity.COMMON,
                Optional.empty(), keywords, 60, 0
            );

            types.clear();
            subtypes.clear();
            keywords.clear();

            assertFalse(card.types().isEmpty());
            assertFalse(card.subtypes().isEmpty());
            assertFalse(card.keywords().isEmpty());
        }

        @Test
        @DisplayName("Null elements in collection parameters are strictly rejected with NullPointerException")
        void testNullElementsRejectedInCollections() {
            assertThrows(NullPointerException.class, () ->
                CardDefinition.builder("id", "base", "name")
                    .addType(null)
            );
            assertThrows(NullPointerException.class, () ->
                CardDefinition.builder("id", "base", "name")
                    .addSubtype(null)
            );
            assertThrows(NullPointerException.class, () ->
                CardDefinition.builder("id", "base", "name")
                    .addKeyword(null)
            );

            // Null in collections via constructor
            Set<CardType> typesWithNull = new HashSet<>();
            typesWithNull.add(CardType.CREATURE);
            typesWithNull.add(null);

            assertThrows(NullPointerException.class, () ->
                new CardDefinition("id", "base", "name", ManaCost.ZERO, typesWithNull, Set.of(), CardRarity.COMMON, Optional.empty(), List.of(), 10, 0)
            );
        }
    }

    @Nested
    @DisplayName("7. Serialization Round-Trip Stress with Escapes & Edge Cards")
    class SerializationRoundTripStressTests {

        @Test
        @DisplayName("Card with quotes, backslashes, and newlines in text round-trips identically")
        void testCardWithSpecialCharactersRoundTrip() {
            CardDefinition trickyCard = CardDefinition.builder("mtg:test/tricky_\"card\"_\\path", "test", "Urza's \"Bane\" \\ Special")
                .manaCost(ManaCost.ofBlue(2))
                .addType(CardType.INSTANT)
                .addSubtype("Trap \\ Arcane")
                .rarity(CardRarity.RARE)
                .addKeyword(new KeywordInstance(
                    new KeywordDefinition("mtg:special", "Special \"Quote\"", "Deals 2 damage to \"target\" player.", KeywordCategory.TRIGGERED, false),
                    0
                ))
                .baseCooldownTicks(120)
                .windupDelayTicks(10)
                .build();

            String json = trickyCard.toJson();
            assertNotNull(json);

            CardDefinition parsed = CardDefinition.fromJson(json);
            assertNotNull(parsed);
            assertEquals(trickyCard.id(), parsed.id());
            assertEquals(trickyCard.name(), parsed.name());
            assertEquals(trickyCard.subtypes(), parsed.subtypes());
            assertEquals(trickyCard.keywords().get(0).definition().name(), parsed.keywords().get(0).definition().name());
            assertEquals(trickyCard.keywords().get(0).definition().reminderText(), parsed.keywords().get(0).definition().reminderText());
        }

        @Test
        @DisplayName("Card with all mana colors and all 6 phyrexian mana components round-trips identically")
        void testRainbowPhyrexianCardRoundTrip() {
            PhyrexianManaCost phyrexian = new PhyrexianManaCost(1, 2, 3, 4, 5, 6);
            ManaCost fullCost = new ManaCost(7, 1, 2, 3, 4, 5, 8, phyrexian);

            CardDefinition omnath = CardDefinition.builder("mtg:test/all_colors", "test", "All Colors Incarnation")
                .manaCost(fullCost)
                .addType(CardType.CREATURE)
                .rarity(CardRarity.MYTHIC)
                .combatStats(10, 10)
                .build();

            String json = omnath.toJson();
            CardDefinition parsed = CardDefinition.fromJson(json);

            assertEquals(fullCost.generic(), parsed.manaCost().generic());
            assertEquals(fullCost.white(), parsed.manaCost().white());
            assertEquals(fullCost.blue(), parsed.manaCost().blue());
            assertEquals(fullCost.black(), parsed.manaCost().black());
            assertEquals(fullCost.red(), parsed.manaCost().red());
            assertEquals(fullCost.green(), parsed.manaCost().green());
            assertEquals(fullCost.colorless(), parsed.manaCost().colorless());
            assertEquals(phyrexian.whitePhyrexian(), parsed.manaCost().phyrexian().whitePhyrexian());
            assertEquals(phyrexian.bluePhyrexian(), parsed.manaCost().phyrexian().bluePhyrexian());
            assertEquals(phyrexian.blackPhyrexian(), parsed.manaCost().phyrexian().blackPhyrexian());
            assertEquals(phyrexian.redPhyrexian(), parsed.manaCost().phyrexian().redPhyrexian());
            assertEquals(phyrexian.greenPhyrexian(), parsed.manaCost().phyrexian().greenPhyrexian());
            assertEquals(phyrexian.colorlessPhyrexian(), parsed.manaCost().phyrexian().colorlessPhyrexian());
            assertEquals(fullCost.totalManaValue(), parsed.manaCost().totalManaValue());
        }

        @Test
        @DisplayName("Card with negative combat stats (e.g. Char-Rumbler / Spinal Parasite) round-trips identically")
        void testNegativeCombatStatsRoundTrip() {
            CardDefinition spinalParasite = CardDefinition.builder("mtg:test/spinal_parasite", "test", "Spinal Parasite")
                .manaCost(ManaCost.ofGeneric(5))
                .addType(CardType.ARTIFACT)
                .addType(CardType.CREATURE)
                .addSubtype("Insect")
                .rarity(CardRarity.UNCOMMON)
                .combatStats(-1, -1)
                .build();

            String json = spinalParasite.toJson();
            CardDefinition parsed = CardDefinition.fromJson(json);

            assertTrue(parsed.combatStats().isPresent());
            assertEquals(-1, parsed.combatStats().get().power());
            assertEquals(-1, parsed.combatStats().get().toughness());
        }

        @Test
        @DisplayName("Land card without combatStats serializes to null combatStats and parses as empty")
        void testNullCombatStatsRoundTrip() {
            CardDefinition land = CardDefinition.builder("mtg:base/plains", "base", "Plains")
                .manaCost(ManaCost.ZERO)
                .addType(CardType.LAND)
                .addSubtype("Plains")
                .build();

            String json = land.toJson();
            assertTrue(json.contains("\"combatStats\": null"));

            CardDefinition parsed = CardDefinition.fromJson(json);
            assertTrue(parsed.combatStats().isEmpty());
        }
    }
}
