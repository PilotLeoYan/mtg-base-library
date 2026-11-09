package com.mtg.test.config;

import com.mtg.config.*;
import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MtgConfig System, TOML Serialization, and Clamping Suite")
public class MtgConfigTest {

    @AfterEach
    void tearDown() {
        MtgConfig.resetToDefaults();
    }

    @Test
    @DisplayName("Default configuration values match documented specifications")
    void testDefaultConfigValues() {
        ManaConfig mana = ManaConfig.defaults();
        assertEquals(100, mana.biomeRegenIntervalTicks());
        assertEquals(5, mana.initialMaxMana());
        assertEquals(20, mana.hardCapMaxMana());
        assertEquals(1.25, mana.manaUpgradeXpMultiplier());

        CardsConfig cards = CardsConfig.defaults();
        assertEquals(1.0, cards.cardCooldownMultiplier());
        assertEquals(60.0, cards.landCooldownSeconds());
        assertTrue(cards.enableSorceryWindupDelays());
        assertEquals(3, cards.maxActiveSummonsPerPlayer());

        ProgressionConfig progression = ProgressionConfig.defaults();
        assertEquals(4, progression.initialHandCapacity());
        assertEquals(7, progression.maxHandCapacity());
        assertEquals(4, progression.thresholdCommonToUncommon());
        assertEquals(4, progression.thresholdUncommonToRare());
        assertEquals(4, progression.thresholdRareToMythic());

        WorldGenConfig worldGen = WorldGenConfig.defaults();
        assertTrue(worldGen.enableManaNodes());
        assertTrue(worldGen.enablePoisonLethality());
        assertEquals(10, worldGen.poisonLethalThreshold());
        assertEquals(3, worldGen.corruptedThreshold());
    }

    @Test
    @DisplayName("TOML serialization and deserialization round-trip preserves defaults identically")
    void testTomlSerializationRoundTrip(@TempDir Path tempDir) {
        MtgConfig.resetToDefaults();
        MtgConfig.saveAll(tempDir);

        assertTrue(Files.exists(tempDir.resolve("mana.toml")));
        assertTrue(Files.exists(tempDir.resolve("cards.toml")));
        assertTrue(Files.exists(tempDir.resolve("progression.toml")));
        assertTrue(Files.exists(tempDir.resolve("world_gen.toml")));

        MtgConfig.loadAll(tempDir);

        assertEquals(100, MtgConfig.getMana().biomeRegenIntervalTicks());
        assertEquals(5, MtgConfig.getMana().initialMaxMana());
        assertEquals(1.0, MtgConfig.getCards().cardCooldownMultiplier());
        assertEquals(4, MtgConfig.getProgression().thresholdCommonToUncommon());
        assertEquals(10, MtgConfig.getWorldGen().poisonLethalThreshold());
    }

    @Test
    @DisplayName("Partial TOML overrides update only specified keys and preserve defaults for the rest")
    void testTomlPartialOverride(@TempDir Path tempDir) throws IOException {
        String partialManaToml = """
            [regeneration]
            biome_regen_interval_ticks = 50
            """;
        Files.writeString(tempDir.resolve("mana.toml"), partialManaToml);

        ManaConfig loaded = ManaConfig.loadOrCreate(tempDir.resolve("mana.toml"));
        assertEquals(50, loaded.biomeRegenIntervalTicks(), "Overridden property must be loaded");
        assertEquals(5, loaded.initialMaxMana(), "Unspecified property must retain default value");
        assertEquals(20, loaded.hardCapMaxMana(), "Unspecified property must retain default value");
    }

    @Test
    @DisplayName("Numeric bounds clamping constrains out-of-range values to valid intervals")
    void testNumericBoundsClamping(@TempDir Path tempDir) throws IOException {
        String outOfBoundsCardsToml = """
            [cooldowns]
            card_cooldown_multiplier = -10.0
            
            [summons]
            max_active_summons_per_player = 999
            """;
        Files.writeString(tempDir.resolve("cards.toml"), outOfBoundsCardsToml);

        CardsConfig loaded = CardsConfig.loadOrCreate(tempDir.resolve("cards.toml"));
        assertEquals(0.05, loaded.cardCooldownMultiplier(), "Negative cooldown multiplier must clamp to minimum (0.05)");
        assertEquals(10, loaded.maxActiveSummonsPerPlayer(), "Excessive summons must clamp to maximum (10)");
    }

    @Test
    @DisplayName("Corrupted TOML syntax falls back safely to default config without crashing")
    void testCorruptedTomlRecovery(@TempDir Path tempDir) throws IOException {
        String corruptedToml = """
            [cards
            this is not valid toml = = = !!!
            """;
        Files.writeString(tempDir.resolve("cards.toml"), corruptedToml);

        CardsConfig loaded = CardsConfig.loadOrCreate(tempDir.resolve("cards.toml"));
        assertNotNull(loaded);
        assertEquals(1.0, loaded.cardCooldownMultiplier(), "Corrupted config must safely return defaults");
        assertEquals(3, loaded.maxActiveSummonsPerPlayer());
    }

    @Test
    @DisplayName("Biome affinity mapping override correctly retrieves updated color")
    void testBiomeAffinityMapping(@TempDir Path tempDir) throws IOException {
        String affinityToml = """
            [biome_affinities]
            "minecraft:plains" = "GREEN"
            """;
        Files.writeString(tempDir.resolve("mana.toml"), affinityToml);

        ManaConfig loaded = ManaConfig.loadOrCreate(tempDir.resolve("mana.toml"));
        assertEquals("GREEN", loaded.getBiomeAffinity("minecraft:plains"));
        assertEquals("BLUE", loaded.getBiomeAffinity("minecraft:ocean")); // Retains default ocean affinity
    }
}
