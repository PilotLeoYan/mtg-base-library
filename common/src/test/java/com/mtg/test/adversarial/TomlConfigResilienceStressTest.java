package com.mtg.test.adversarial;

import com.mtg.config.*;
import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParseException;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — TOML Parser Resilience, Clamping & Fault Recovery")
public class TomlConfigResilienceStressTest {

    // =========================================================================
    // 1. TOML Syntax Error Detection
    // =========================================================================

    @Test
    @DisplayName("Syntax Errors: Parser reliably rejects missing '=', empty keys, and malformed headers")
    void testTomlParserSyntaxErrorRejection() {
        // Missing '=' operator
        assertThrows(TomlParseException.class, () -> TomlParser.parse("missing_equals_operator value"));

        // Empty key before '='
        assertThrows(TomlParseException.class, () -> TomlParser.parse(" = 123"));

        // Missing value after '='
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = "));

        // Empty section header
        assertThrows(TomlParseException.class, () -> TomlParser.parse("[]"));

        // Unclosed section header
        assertThrows(TomlParseException.class, () -> TomlParser.parse("[unclosed_section"));

        // Non-number unquoted garbage value
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = invalid_bare_value_without_quotes"));

        // Garbage characters line
        assertThrows(TomlParseException.class, () -> TomlParser.parse("@@@###$$$%%%"));
    }

    @Test
    @DisplayName("Syntax Errors: Single quote character as value throws TomlParseException")
    void testSingleQuoteWithoutClosingThrowsTomlParseException() {
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = \""));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = '"));
    }

    // =========================================================================
    // 2. Type Mismatch Fallbacks & Safe Conversions
    // =========================================================================

    @Test
    @DisplayName("Type Mismatch: TomlDocument getter fallbacks return defaults on incompatible types")
    void testTomlDocumentTypeMismatchFallbacks() throws TomlParseException {
        String toml = """
            [mismatches]
            str_val = "not_a_number"
            num_val = 42
            float_val = 3.75
            bool_val = true
            bool_str = "false"
            """;
        TomlDocument doc = TomlParser.parse(toml);

        // getInt with string that cannot parse as int -> returns default
        assertEquals(99, doc.getInt("mismatches", "str_val", 99));

        // getInt with boolean value -> returns default
        assertEquals(10, doc.getInt("mismatches", "bool_val", 10));

        // getInt with float number -> truncates safely to int (3)
        assertEquals(3, doc.getInt("mismatches", "float_val", 0));

        // getDouble with integer -> converts to double (42.0)
        assertEquals(42.0, doc.getDouble("mismatches", "num_val", 0.0));

        // getDouble with invalid string -> returns default
        assertEquals(1.5, doc.getDouble("mismatches", "str_val", 1.5));

        // getBoolean with number -> returns default
        assertFalse(doc.getBoolean("mismatches", "num_val", false));

        // getBoolean with string "false" -> parses boolean false
        assertFalse(doc.getBoolean("mismatches", "bool_str", true));

        // getString with number -> converts to string "42"
        assertEquals("42", doc.getString("mismatches", "num_val", "default"));

        // Missing keys in non-existent section -> return default
        assertEquals(55, doc.getInt("non_existent", "missing_key", 55));
        assertEquals(9.9, doc.getDouble("non_existent", "missing_key", 9.9));
        assertTrue(doc.getBoolean("non_existent", "missing_key", true));
        assertEquals("fallback", doc.getString("non_existent", "missing_key", "fallback"));
    }

    // =========================================================================
    // 3. Out-of-Bounds Numbers & Extreme Values Clamping
    // =========================================================================

    @Test
    @DisplayName("Numeric Bounds Clamping: Extreme negative and positive numbers are constrained across all 4 configs")
    void testComprehensiveNumericBoundsClamping(@TempDir Path tempDir) throws IOException {
        // 1. ManaConfig extreme values
        String extremeMana = """
            [regeneration]
            biome_regen_interval_ticks = -99999
            base_regen_amount = 9999
            land_mana_bonus = -50
            decay_interval_ticks = 1000000
            decay_amount = 500
            
            [capacity]
            initial_max_mana = -10
            hard_cap_max_mana = 50000
            base_xp_level_cost = -100
            mana_upgrade_xp_multiplier = 99.0
            
            [phyrexian]
            phyrexian_life_cost_damage = -20.0
            """;
        Path manaPath = tempDir.resolve("mana.toml");
        Files.writeString(manaPath, extremeMana);
        ManaConfig mana = ManaConfig.loadOrCreate(manaPath);

        assertEquals(20, mana.biomeRegenIntervalTicks(), "biome_regen_interval_ticks clamped to min 20");
        assertEquals(20, mana.baseRegenAmount(), "base_regen_amount clamped to max 20");
        assertEquals(0, mana.landManaBonus(), "land_mana_bonus clamped to min 0");
        assertEquals(72000, mana.decayIntervalTicks(), "decay_interval_ticks clamped to max 72000");
        assertEquals(10, mana.decayAmount(), "decay_amount clamped to max 10");
        assertEquals(1, mana.initialMaxMana(), "initial_max_mana clamped to min 1");
        assertEquals(100, mana.hardCapMaxMana(), "hard_cap_max_mana clamped to max 100");
        assertEquals(1, mana.baseXpLevelCost(), "base_xp_level_cost clamped to min 1");
        assertEquals(5.0, mana.manaUpgradeXpMultiplier(), "mana_upgrade_xp_multiplier clamped to max 5.0");
        assertEquals(1.0, mana.phyrexianLifeCostDamage(), "phyrexian_life_cost_damage clamped to min 1.0");

        // 2. CardsConfig extreme values
        String extremeCards = """
            [cooldowns]
            card_cooldown_multiplier = 1000.0
            land_cooldown_seconds = -1.0
            instant_base_cooldown_seconds = 500.0
            duplicate_cooldown_reduction = 0.99
            
            [delays]
            sorcery_delay_per_mana_seconds = -5.0
            damage_interrupt_threshold = 100.0
            
            [summons]
            max_active_summons_per_player = 0
            summon_leash_distance_blocks = 10000
            
            [enchantments]
            enchantment_base_duration_ticks = 50
            """;
        Path cardsPath = tempDir.resolve("cards.toml");
        Files.writeString(cardsPath, extremeCards);
        CardsConfig cards = CardsConfig.loadOrCreate(cardsPath);

        assertEquals(20.0, cards.cardCooldownMultiplier(), "card_cooldown_multiplier clamped to max 20.0");
        assertEquals(5.0, cards.landCooldownSeconds(), "land_cooldown_seconds clamped to min 5.0");
        assertEquals(60.0, cards.instantBaseCooldownSeconds(), "instant_base_cooldown_seconds clamped to max 60.0");
        assertEquals(0.50, cards.duplicateCooldownReduction(), "duplicate_cooldown_reduction clamped to max 0.50");
        assertEquals(0.1, cards.sorceryDelayPerManaSeconds(), "sorcery_delay_per_mana_seconds clamped to min 0.1");
        assertEquals(20.0, cards.damageInterruptThreshold(), "damage_interrupt_threshold clamped to max 20.0");
        assertEquals(1, cards.maxActiveSummonsPerPlayer(), "max_active_summons_per_player clamped to min 1");
        assertEquals(128, cards.summonLeashDistanceBlocks(), "summon_leash_distance_blocks clamped to max 128");
        assertEquals(100, cards.enchantmentBaseDurationTicks(), "enchantment_base_duration_ticks clamped to min 100");

        // 3. ProgressionConfig extreme values
        String extremeProgression = """
            [hand_capacity]
            initial_hand_capacity = 0
            max_hand_capacity = 25
            hand_upgrade_base_xp_cost = 1
            hand_upgrade_xp_multiplier = 10.0
            
            [duplicate_protection]
            threshold_common_to_uncommon = 50
            threshold_uncommon_to_rare = 0
            threshold_rare_to_mythic = 99
            threshold_mythic_vault = -1
            
            [drop_rates]
            mining_card_drop_chance = -0.5
            farming_card_drop_chance = 5.0
            mob_card_drop_chance = -1.0
            boss_card_drop_chance = 10.0
            dungeon_chest_card_count = 50
            starter_pack_card_count = 0
            """;
        Path progPath = tempDir.resolve("progression.toml");
        Files.writeString(progPath, extremeProgression);
        ProgressionConfig prog = ProgressionConfig.loadOrCreate(progPath);

        assertEquals(1, prog.initialHandCapacity(), "initial_hand_capacity clamped to min 1");
        assertEquals(7, prog.maxHandCapacity(), "max_hand_capacity clamped to max 7");
        assertEquals(5, prog.handUpgradeBaseXpCost(), "hand_upgrade_base_xp_cost clamped to min 5");
        assertEquals(3.0, prog.handUpgradeXpMultiplier(), "hand_upgrade_xp_multiplier clamped to max 3.0");
        assertEquals(20, prog.thresholdCommonToUncommon(), "threshold clamped to max 20");
        assertEquals(2, prog.thresholdUncommonToRare(), "threshold clamped to min 2");
        assertEquals(20, prog.thresholdRareToMythic(), "threshold clamped to max 20");
        assertEquals(2, prog.thresholdMythicVault(), "threshold clamped to min 2");
        assertEquals(0.0, prog.miningCardDropChance(), "mining drop chance clamped to min 0.0");
        assertEquals(1.0, prog.farmingCardDropChance(), "farming drop chance clamped to max 1.0");
        assertEquals(0.0, prog.mobCardDropChance(), "mob drop chance clamped to min 0.0");
        assertEquals(1.0, prog.bossCardDropChance(), "boss drop chance clamped to max 1.0");
        assertEquals(10, prog.dungeonChestCardCount(), "dungeon chest count clamped to max 10");
        assertEquals(1, prog.starterPackCardCount(), "starter pack count clamped to min 1");

        // 4. WorldGenConfig extreme values
        String extremeWorldGen = """
            [mana_nodes]
            mana_node_chunk_chance = 0.99
            mana_node_pulse_amount = 50
            
            [altars]
            altar_spacing_chunks = 500
            altar_separation_chunks = 0
            
            [mobs_and_bosses]
            boss_health_multiplier = 0.01
            boss_damage_multiplier = 100.0
            
            [mechanics]
            poison_lethal_threshold = 500
            corrupted_threshold = 0
            """;
        Path worldGenPath = tempDir.resolve("world_gen.toml");
        Files.writeString(worldGenPath, extremeWorldGen);
        WorldGenConfig worldGen = WorldGenConfig.loadOrCreate(worldGenPath);

        assertEquals(0.10, worldGen.manaNodeChunkChance(), "mana_node_chunk_chance clamped to max 0.10");
        assertEquals(10, worldGen.manaNodePulseAmount(), "mana_node_pulse_amount clamped to max 10");
        assertEquals(128, worldGen.altarSpacingChunks(), "altar_spacing_chunks clamped to max 128");
        assertEquals(4, worldGen.altarSeparationChunks(), "altar_separation_chunks clamped to min 4");
        assertEquals(0.2, worldGen.bossHealthMultiplier(), "boss_health_multiplier clamped to min 0.2");
        assertEquals(10.0, worldGen.bossDamageMultiplier(), "boss_damage_multiplier clamped to max 10.0");
        assertEquals(100, worldGen.poisonLethalThreshold(), "poison_lethal_threshold clamped to max 100");
        assertEquals(1, worldGen.corruptedThreshold(), "corrupted_threshold clamped to min 1");
    }

    // =========================================================================
    // 4. Unicode & Special Character Handling
    // =========================================================================

    @Test
    @DisplayName("Unicode Characters: UTF-8 comments, emojis, accents, and CJK text parse and serialize cleanly")
    void testUnicodeAndSpecialCharacters(@TempDir Path tempDir) throws IOException, TomlParseException {
        String unicodeToml = """
            # ⚔️ Phyrexian invasion parameters: 完成化 (Compleation)
            # 🌲 Forest / ☀️ Plains / 💀 Swamp / 🌋 Mountain / 🌊 Island
            
            [biome_affinities]
            "minecraft:forêt_ancienne" = "GREEN"
            "minecraft:desert_☀️" = "RED"
            "minecraft:deep_dark_💀" = "BLACK"
            
            [i18n_test]
            greeting = "Bienvenue dans l'univers de Magic: The Gathering! 🐉"
            japanese = "プレインズウォーカー・ジャーナル"
            escaped_text = "Line 1\\nLine 2\\tTabbed \\"Quoted\\""
            """;

        Path filePath = tempDir.resolve("unicode_test.toml");
        Files.writeString(filePath, unicodeToml, StandardCharsets.UTF_8);

        TomlDocument doc = TomlParser.parse(filePath);

        assertEquals("GREEN", doc.getString("biome_affinities", "minecraft:forêt_ancienne", ""));
        assertEquals("RED", doc.getString("biome_affinities", "minecraft:desert_☀️", ""));
        assertEquals("BLACK", doc.getString("biome_affinities", "minecraft:deep_dark_💀", ""));
        assertEquals("Bienvenue dans l'univers de Magic: The Gathering! 🐉", doc.getString("i18n_test", "greeting", ""));
        assertEquals("プレインズウォーカー・ジャーナル", doc.getString("i18n_test", "japanese", ""));
        assertEquals("Line 1\nLine 2\tTabbed \"Quoted\"", doc.getString("i18n_test", "escaped_text", ""));

        // Round-trip verification
        String written = TomlWriter.write(doc);
        TomlDocument doc2 = TomlParser.parse(written);
        assertEquals(doc.getString("i18n_test", "greeting", ""), doc2.getString("i18n_test", "greeting", ""));
        assertEquals(doc.getString("i18n_test", "japanese", ""), doc2.getString("i18n_test", "japanese", ""));
    }

    // =========================================================================
    // 5. Corrupted File Auto-Recovery with Fallback Defaults
    // =========================================================================

    @Test
    @DisplayName("Corrupted File Recovery: Non-crashing fallback to defaults on corrupt, truncated, or binary files")
    void testCorruptedFileAutoRecovery(@TempDir Path tempDir) throws IOException {
        Path corruptFile = tempDir.resolve("corrupt.toml");

        // Case A: Syntax error (missing closing section)
        Files.writeString(corruptFile, "[unclosed_section\nbroken = = =");
        ManaConfig manaA = ManaConfig.loadOrCreate(corruptFile);
        assertNotNull(manaA);
        assertEquals(100, manaA.biomeRegenIntervalTicks(), "Must recover default ManaConfig");

        // Case B: Incomplete quote / single quote
        Files.writeString(corruptFile, "key = \"");
        CardsConfig cardsB = CardsConfig.loadOrCreate(corruptFile);
        assertNotNull(cardsB);
        assertEquals(1.0, cardsB.cardCooldownMultiplier(), "Must recover default CardsConfig");

        // Case C: Non-UTF-8 binary garbage bytes
        byte[] binaryGarbage = new byte[] { (byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x12, (byte)0x80, (byte)0x81 };
        Files.write(corruptFile, binaryGarbage);
        ProgressionConfig progC = ProgressionConfig.loadOrCreate(corruptFile);
        assertNotNull(progC);
        assertEquals(4, progC.initialHandCapacity(), "Must recover default ProgressionConfig");

        // Case D: Truncated empty file
        Files.writeString(corruptFile, "");
        WorldGenConfig worldGenD = WorldGenConfig.loadOrCreate(corruptFile);
        assertNotNull(worldGenD);
        assertTrue(worldGenD.enableManaNodes(), "Empty file must return valid defaults");

        // Case E: File with only whitespace and comments
        Files.writeString(corruptFile, "# Just a comment\n   \n\t\n# Another comment\n");
        ManaConfig manaE = ManaConfig.loadOrCreate(corruptFile);
        assertNotNull(manaE);
        assertEquals(5, manaE.initialMaxMana());

        // Case F: Non-existent file auto-generation
        Path nonExistent = tempDir.resolve("auto_created.toml");
        assertFalse(Files.exists(nonExistent));
        ManaConfig manaF = ManaConfig.loadOrCreate(nonExistent);
        assertNotNull(manaF);
        assertTrue(Files.exists(nonExistent), "loadOrCreate must auto-create missing config file");
    }

    // =========================================================================
    // 6. Scientific Notation & Edge Case Numbers
    // =========================================================================

    @Test
    @DisplayName("Number Formats: Scientific notation and negative numbers parse properly")
    void testScientificNotationAndNumbers() throws TomlParseException {
        String toml = """
            [numbers]
            sci_pos = 1.5e2
            sci_neg = 2.0e-1
            sci_cap = 1E3
            negative_int = -42
            positive_int = +100
            zero = 0
            """;
        TomlDocument doc = TomlParser.parse(toml);

        assertEquals(150.0, doc.getDouble("numbers", "sci_pos", 0.0), 0.001);
        assertEquals(0.2, doc.getDouble("numbers", "sci_neg", 0.0), 0.001);
        assertEquals(1000.0, doc.getDouble("numbers", "sci_cap", 0.0), 0.001);
        assertEquals(-42, doc.getInt("numbers", "negative_int", 0));
        assertEquals(100, doc.getInt("numbers", "positive_int", 0));
        assertEquals(0, doc.getInt("numbers", "zero", -1));
    }

    // =========================================================================
    // 7. Backslash Parity & Section Comment Stress Tests
    // =========================================================================

    @Test
    @DisplayName("TOML Resilience: Backslash parity scanner handles odd/even backslashes and quotes")
    void testBackslashParityAndSectionCommentsStress() throws TomlParseException {
        // Case 1: Escaped quote inside string (\")
        String toml1 = "key = \"quoted \\\" quote\"";
        TomlDocument doc1 = TomlParser.parse(toml1);
        assertEquals("quoted \" quote", doc1.getString("", "key", ""));

        // Case 2: Escaped backslash before closing quote (\\") followed by comment
        String toml2 = "key = \"quoted \\\\\" # comment with # inside";
        TomlDocument doc2 = TomlParser.parse(toml2);
        assertEquals("quoted \\", doc2.getString("", "key", ""));

        // Case 3: Odd backslashes before quote (\\\" -> escaped backslash + escaped quote)
        String toml3 = "key = \"quoted \\\\\\\" quote\"";
        TomlDocument doc3 = TomlParser.parse(toml3);
        assertEquals("quoted \\\" quote", doc3.getString("", "key", ""));

        // Case 4: 4 backslashes (\\\\ -> 2 escaped backslashes)
        String toml4 = "key = \"quoted \\\\\\\\\"";
        TomlDocument doc4 = TomlParser.parse(toml4);
        assertEquals("quoted \\\\", doc4.getString("", "key", ""));

        // Case 5: Literal single quotes preserve backslashes verbatim
        String toml5 = "key = 'literal \\n \\\\ \"'";
        TomlDocument doc5 = TomlParser.parse(toml5);
        assertEquals("literal \\n \\\\ \"", doc5.getString("", "key", ""));

        // Case 6: Quoted keys (both single and double quoted)
        String toml6 = """
            "double.quoted.key\\"" = 100
            'single.quoted.key' = 200
            """;
        TomlDocument doc6 = TomlParser.parse(toml6);
        assertEquals(100, doc6.getInt("", "double.quoted.key\"", 0));
        assertEquals(200, doc6.getInt("", "single.quoted.key", 0));

        // Case 7: Section comments and inline comments in section headers
        String toml7 = """
            # Pending comment 1
            # Pending comment 2
            [section.with.dots] # inline header comment with # and [brackets]
            val = 42
            """;
        TomlDocument doc7 = TomlParser.parse(toml7);
        assertEquals(42, doc7.getInt("section.with.dots", "val", 0));
        var comments = doc7.getSectionComments("section.with.dots");
        assertEquals(2, comments.size());
        assertEquals("# Pending comment 1", comments.get(0));
        assertEquals("# Pending comment 2", comments.get(1));

        // Case 8: Syntax rejections for malformed quoted keys / values
        assertThrows(TomlParseException.class, () -> TomlParser.parse("\"key\"trailing = 1"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("'key'trailing = 1"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = \"val\"trailing"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = 'val'trailing"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = \"unclosed string"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("key = 'unclosed literal"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("[unclosed_section # comment"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("[header] extra_non_comment"));
        assertThrows(TomlParseException.class, () -> TomlParser.parse("[   ]"));
    }
}

