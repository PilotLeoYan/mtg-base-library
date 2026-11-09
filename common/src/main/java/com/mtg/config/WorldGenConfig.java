package com.mtg.config;

import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public record WorldGenConfig(
    boolean enableManaNodes,
    double manaNodeChunkChance,
    int manaNodePulseAmount,
    boolean enableAltars,
    int altarSpacingChunks,
    int altarSeparationChunks,
    boolean enableCustomMobs,
    double customMobSpawnRate,
    boolean enableWorldBosses,
    double bossSpawnRate,
    double bossHealthMultiplier,
    double bossDamageMultiplier,
    boolean enablePoisonLethality,
    int poisonLethalThreshold,
    int corruptedThreshold
) {
    private static final Logger LOGGER = Logger.getLogger(WorldGenConfig.class.getName());

    public WorldGenConfig(
        boolean enableManaNodes,
        double manaNodeChunkChance,
        int manaNodePulseAmount,
        boolean enableAltars,
        int altarSpacingChunks,
        int altarSeparationChunks,
        boolean enableCustomMobs,
        boolean enableWorldBosses,
        double bossHealthMultiplier,
        double bossDamageMultiplier,
        boolean enablePoisonLethality,
        int poisonLethalThreshold,
        int corruptedThreshold
    ) {
        this(
            enableManaNodes, manaNodeChunkChance, manaNodePulseAmount,
            enableAltars, altarSpacingChunks, altarSeparationChunks,
            enableCustomMobs, 0.05,
            enableWorldBosses, 0.01,
            bossHealthMultiplier, bossDamageMultiplier,
            enablePoisonLethality, poisonLethalThreshold, corruptedThreshold
        );
    }

    public static WorldGenConfig defaults() {
        return new WorldGenConfig(
            true, 0.02, 3,
            true, 32, 8,
            true, 0.05,
            true, 0.01,
            1.0, 1.0,
            true, 10, 3
        );
    }

    public WorldGenConfig sanitize() {
        return new WorldGenConfig(
            enableManaNodes,
            Math.clamp(manaNodeChunkChance, 0.001, 0.10),
            Math.clamp(manaNodePulseAmount, 1, 10),
            enableAltars,
            Math.clamp(altarSpacingChunks, 16, 128),
            Math.clamp(altarSeparationChunks, 4, 64),
            enableCustomMobs,
            Math.clamp(customMobSpawnRate, 0.001, 1.0),
            enableWorldBosses,
            Math.clamp(bossSpawnRate, 0.0001, 0.5),
            Math.clamp(bossHealthMultiplier, 0.2, 10.0),
            Math.clamp(bossDamageMultiplier, 0.2, 10.0),
            enablePoisonLethality,
            Math.clamp(poisonLethalThreshold, 1, 100),
            Math.clamp(corruptedThreshold, 1, 10)
        );
    }

    public TomlDocument toTomlDocument() {
        TomlDocument doc = new TomlDocument();

        doc.addSectionComment("mana_nodes", "# Elemental Mana Node procedural generation");
        doc.set("mana_nodes", "enable_mana_nodes", enableManaNodes);
        doc.set("mana_nodes", "mana_node_chunk_chance", manaNodeChunkChance);
        doc.set("mana_nodes", "mana_node_pulse_amount", manaNodePulseAmount);

        doc.addSectionComment("altars", "# Planeswalker Altar procedural structures");
        doc.set("altars", "enable_altars", enableAltars);
        doc.set("altars", "altar_spacing_chunks", altarSpacingChunks);
        doc.set("altars", "altar_separation_chunks", altarSeparationChunks);

        doc.addSectionComment("mobs_and_bosses", "# Custom MTG mobs and boss encounters");
        doc.set("mobs_and_bosses", "enable_custom_mobs", enableCustomMobs);
        doc.set("mobs_and_bosses", "custom_mob_spawn_rate", customMobSpawnRate);
        doc.set("mobs_and_bosses", "enable_world_bosses", enableWorldBosses);
        doc.set("mobs_and_bosses", "boss_spawn_rate", bossSpawnRate);
        doc.set("mobs_and_bosses", "boss_health_multiplier", bossHealthMultiplier);
        doc.set("mobs_and_bosses", "boss_damage_multiplier", bossDamageMultiplier);

        doc.addSectionComment("mechanics", "# State-Based Actions and threshold mechanics");
        doc.set("mechanics", "enable_poison_lethality", enablePoisonLethality);
        doc.set("mechanics", "poison_lethal_threshold", poisonLethalThreshold);
        doc.set("mechanics", "corrupted_threshold", corruptedThreshold);

        return doc;
    }

    public static WorldGenConfig fromTomlDocument(TomlDocument doc) {
        WorldGenConfig def = defaults();

        boolean enableNodes = doc.getBoolean("mana_nodes", "enable_mana_nodes", def.enableManaNodes);
        double nodeChance = doc.getDouble("mana_nodes", "mana_node_chunk_chance", def.manaNodeChunkChance);
        int nodePulse = doc.getInt("mana_nodes", "mana_node_pulse_amount", def.manaNodePulseAmount);

        boolean enableAltars = doc.getBoolean("altars", "enable_altars", def.enableAltars);
        int spacing = doc.getInt("altars", "altar_spacing_chunks", def.altarSpacingChunks);
        int separation = doc.getInt("altars", "altar_separation_chunks", def.altarSeparationChunks);

        boolean enableMobs = doc.getBoolean("mobs_and_bosses", "enable_custom_mobs", def.enableCustomMobs);
        double mobRate = doc.getDouble("mobs_and_bosses", "custom_mob_spawn_rate", def.customMobSpawnRate);
        boolean enableBosses = doc.getBoolean("mobs_and_bosses", "enable_world_bosses", def.enableWorldBosses);
        double bossRate = doc.getDouble("mobs_and_bosses", "boss_spawn_rate", def.bossSpawnRate);
        double bossHp = doc.getDouble("mobs_and_bosses", "boss_health_multiplier", def.bossHealthMultiplier);
        double bossDmg = doc.getDouble("mobs_and_bosses", "boss_damage_multiplier", def.bossDamageMultiplier);

        boolean enablePoison = doc.getBoolean("mechanics", "enable_poison_lethality", def.enablePoisonLethality);
        int poisonThreshold = doc.getInt("mechanics", "poison_lethal_threshold", def.poisonLethalThreshold);
        int corruptedThresh = doc.getInt("mechanics", "corrupted_threshold", def.corruptedThreshold);

        return new WorldGenConfig(
            enableNodes, nodeChance, nodePulse,
            enableAltars, spacing, separation,
            enableMobs, mobRate,
            enableBosses, bossRate, bossHp, bossDmg,
            enablePoison, poisonThreshold, corruptedThresh
        ).sanitize();
    }

    public static WorldGenConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            WorldGenConfig cfg = defaults();
            cfg.save(path);
            return cfg;
        }
        try {
            TomlDocument doc = TomlParser.parse(path);
            return fromTomlDocument(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load world_gen.toml at " + path + "; falling back to defaults", e);
            return defaults();
        }
    }

    public void save(Path path) {
        try {
            TomlWriter.write(toTomlDocument(), path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write world_gen config to " + path, e);
        }
    }
}
