package com.mtg.test.e2e.tier1;

import com.mtg.config.*;
import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 22: Configuration System (TOML / JSON) E2E Test")
public class Feature22_ConfigurationSystemTest {

    @AfterEach
    void tearDown() {
        MtgConfig.resetToDefaults();
    }

    @Test
    @DisplayName("1. ManaConfig default parameters conform to project specification")
    void testManaConfigDefaults() {
        ManaConfig config = ManaConfig.defaults();
        assertEquals(100, config.biomeRegenIntervalTicks());
        assertEquals(5, config.initialMaxMana());
        assertEquals(20, config.hardCapMaxMana());
        assertEquals(1.25, config.manaUpgradeXpMultiplier());
    }

    @Test
    @DisplayName("2. CardsConfig default parameters conform to project specification")
    void testCardsConfigDefaults() {
        CardsConfig config = CardsConfig.defaults();
        assertEquals(1.0, config.cardCooldownMultiplier());
        assertEquals(60.0, config.landCooldownSeconds());
        assertTrue(config.enableSorceryWindupDelays());
        assertEquals(3, config.maxActiveSummonsPerPlayer());
    }

    @Test
    @DisplayName("3. ProgressionConfig default parameters conform to project specification")
    void testProgressionConfigDefaults() {
        ProgressionConfig config = ProgressionConfig.defaults();
        assertEquals(4, config.initialHandCapacity());
        assertEquals(7, config.maxHandCapacity());
        assertEquals(4, config.thresholdCommonToUncommon());
        assertEquals(4, config.thresholdUncommonToRare());
        assertEquals(4, config.thresholdRareToMythic());
    }

    @Test
    @DisplayName("4. WorldGenConfig default parameters conform to project specification")
    void testWorldGenConfigDefaults() {
        WorldGenConfig config = WorldGenConfig.defaults();
        assertTrue(config.enableManaNodes());
        assertTrue(config.enablePoisonLethality());
        assertEquals(10, config.poisonLethalThreshold());
    }

    @Test
    @DisplayName("5. TomlParser and TomlWriter serialize and parse document structures cleanly")
    void testTomlRoundTripSerialization() {
        String toml = """
                [mana]
                biome_regen_interval_ticks = 100
                initial_max_mana = 5
                enable_decay = true
                """;

        TomlDocument doc = TomlParser.parse(toml);
        assertNotNull(doc);
        assertEquals(100, doc.getInt("mana", "biome_regen_interval_ticks", 0));
        assertEquals(5, doc.getInt("mana", "initial_max_mana", 0));
        assertTrue(doc.getBoolean("mana", "enable_decay", false));

        String written = TomlWriter.write(doc);
        TomlDocument reParsed = TomlParser.parse(written);
        assertEquals(100, reParsed.getInt("mana", "biome_regen_interval_ticks", 0));
    }
}
