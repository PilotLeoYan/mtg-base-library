package com.mtg.test.config;

import com.mtg.config.*;
import com.mtg.network.S2CSyncConfigPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S2CSyncConfigPacket Serialization, Network Sync & Clamping Suite")
public class ConfigSyncNetworkTest {

    @AfterEach
    void tearDown() {
        MtgConfig.resetToDefaults();
    }

    @Test
    @DisplayName("Packet channel ID matches mtg:sync_config")
    void testChannelId() {
        S2CSyncConfigPacket packet = S2CSyncConfigPacket.fromCurrent();
        assertEquals("mtg:sync_config", packet.getChannelId());
    }

    @Test
    @DisplayName("Round-trip serialization preserves all configs including biomeMultipliers and spawn rates")
    void testRoundTripSerialization() {
        Map<String, Double> customMultipliers = new LinkedHashMap<>(ManaConfig.defaultMultipliers());
        customMultipliers.put("minecraft:desert", 1.75);
        customMultipliers.put("minecraft:custom_biome", 2.50);

        ManaConfig mana = new ManaConfig(
                50, 2, 2, 150, 2,
                10, 30, 8, 1.5,
                3.0, true,
                ManaConfig.defaultAffinities(),
                customMultipliers
        );

        WorldGenConfig worldGen = new WorldGenConfig(
                true, 0.04, 5,
                true, 48, 12,
                true, 0.08,
                true, 0.02,
                1.5, 1.25,
                true, 10, 3
        );

        CardsConfig cards = CardsConfig.defaults();
        ProgressionConfig prog = ProgressionConfig.defaults();

        S2CSyncConfigPacket packet = new S2CSyncConfigPacket(mana, cards, prog, worldGen);

        byte[] encoded = packet.encode();
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        S2CSyncConfigPacket decoded = S2CSyncConfigPacket.decode(encoded);
        assertNotNull(decoded);

        // Mana verification
        assertEquals(50, decoded.manaConfig().biomeRegenIntervalTicks());
        assertEquals(10, decoded.manaConfig().initialMaxMana());
        assertEquals(1.75, decoded.manaConfig().getBiomeMultiplier("minecraft:desert"), 0.001);
        assertEquals(2.50, decoded.manaConfig().getBiomeMultiplier("minecraft:custom_biome"), 0.001);

        // WorldGen verification
        assertEquals(0.04, decoded.worldGenConfig().manaNodeChunkChance(), 0.001);
        assertEquals(0.08, decoded.worldGenConfig().customMobSpawnRate(), 0.001);
        assertEquals(0.02, decoded.worldGenConfig().bossSpawnRate(), 0.001);
        assertEquals(1.5, decoded.worldGenConfig().bossHealthMultiplier(), 0.001);
        assertEquals(1.25, decoded.worldGenConfig().bossDamageMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Client handle applies received authoritative server configs to MtgConfig singletons")
    void testClientPacketHandle() {
        ManaConfig serverMana = new ManaConfig(
                40, 3, 2, 100, 1,
                12, 40, 6, 1.3,
                2.0, false,
                ManaConfig.defaultAffinities()
        );

        S2CSyncConfigPacket packet = new S2CSyncConfigPacket(serverMana, CardsConfig.defaults(), ProgressionConfig.defaults(), WorldGenConfig.defaults());

        // Apply packet on client
        packet.handle(null);

        assertEquals(40, MtgConfig.getMana().biomeRegenIntervalTicks(), "MtgConfig.getMana() must be updated by packet handler");
        assertEquals(12, MtgConfig.getMana().initialMaxMana());
    }

    @Test
    @DisplayName("Extreme out-of-bounds values are sanitized upon packet construction and decoding")
    void testBoundsSanitization() {
        Map<String, Double> extremeMultipliers = Map.of("minecraft:desert", 999.0, "minecraft:ocean", -5.0);

        ManaConfig extremeMana = new ManaConfig(
                1, 999, 999, 1, 999,
                999, 999, 999, 99.0,
                999.0, false,
                ManaConfig.defaultAffinities(),
                extremeMultipliers
        );

        WorldGenConfig extremeWorld = new WorldGenConfig(
                true, 50.0, 999,
                true, 999, 999,
                true, 50.0,
                true, 50.0,
                999.0, 999.0,
                true, 999, 999
        );

        S2CSyncConfigPacket packet = new S2CSyncConfigPacket(extremeMana, CardsConfig.defaults(), ProgressionConfig.defaults(), extremeWorld);
        byte[] bytes = packet.encode();
        S2CSyncConfigPacket decoded = S2CSyncConfigPacket.decode(bytes);

        // Clamped values check
        assertEquals(20, decoded.manaConfig().biomeRegenIntervalTicks(), "Regen ticks clamped to min 20");
        assertEquals(20, decoded.manaConfig().baseRegenAmount(), "Base regen clamped to max 20");
        assertEquals(50, decoded.manaConfig().initialMaxMana(), "Initial max mana clamped to max 50");
        assertEquals(10.0, decoded.manaConfig().getBiomeMultiplier("minecraft:desert"), 0.01, "Multiplier clamped to 10.0");
        assertEquals(0.1, decoded.manaConfig().getBiomeMultiplier("minecraft:ocean"), 0.01, "Multiplier clamped to min 0.1");

        assertEquals(0.10, decoded.worldGenConfig().manaNodeChunkChance(), 0.001, "Node chance clamped to 0.10");
        assertEquals(1.0, decoded.worldGenConfig().customMobSpawnRate(), 0.001, "Mob spawn rate clamped to 1.0");
        assertEquals(0.5, decoded.worldGenConfig().bossSpawnRate(), 0.001, "Boss spawn rate clamped to 0.5");
        assertEquals(10.0, decoded.worldGenConfig().bossHealthMultiplier(), 0.001, "Boss HP mult clamped to 10.0");
    }
}
