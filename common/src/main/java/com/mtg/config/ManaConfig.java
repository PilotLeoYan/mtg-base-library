package com.mtg.config;

import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public record ManaConfig(
    int biomeRegenIntervalTicks,
    int baseRegenAmount,
    int landManaBonus,
    int decayIntervalTicks,
    int decayAmount,
    int initialMaxMana,
    int hardCapMaxMana,
    int baseXpLevelCost,
    double manaUpgradeXpMultiplier,
    double phyrexianLifeCostDamage,
    boolean allowLethalPhyrexianPayment,
    Map<String, String> biomeAffinities,
    Map<String, Double> biomeMultipliers
) {
    private static final Logger LOGGER = Logger.getLogger(ManaConfig.class.getName());

    public ManaConfig {
        biomeAffinities = biomeAffinities != null ? Map.copyOf(biomeAffinities) : defaultAffinities();
        biomeMultipliers = biomeMultipliers != null ? Map.copyOf(biomeMultipliers) : defaultMultipliers();
    }

    public ManaConfig(
        int biomeRegenIntervalTicks,
        int baseRegenAmount,
        int landManaBonus,
        int decayIntervalTicks,
        int decayAmount,
        int initialMaxMana,
        int hardCapMaxMana,
        int baseXpLevelCost,
        double manaUpgradeXpMultiplier,
        double phyrexianLifeCostDamage,
        boolean allowLethalPhyrexianPayment,
        Map<String, String> biomeAffinities
    ) {
        this(
            biomeRegenIntervalTicks, baseRegenAmount, landManaBonus, decayIntervalTicks, decayAmount,
            initialMaxMana, hardCapMaxMana, baseXpLevelCost, manaUpgradeXpMultiplier,
            phyrexianLifeCostDamage, allowLethalPhyrexianPayment, biomeAffinities, defaultMultipliers()
        );
    }

    public static Map<String, String> defaultAffinities() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("minecraft:plains", "WHITE");
        map.put("minecraft:sunflower_plains", "WHITE");
        map.put("minecraft:meadow", "WHITE");
        map.put("minecraft:ocean", "BLUE");
        map.put("minecraft:deep_ocean", "BLUE");
        map.put("minecraft:river", "BLUE");
        map.put("minecraft:swamp", "BLACK");
        map.put("minecraft:mangrove_swamp", "BLACK");
        map.put("minecraft:deep_dark", "BLACK");
        map.put("minecraft:desert", "RED");
        map.put("minecraft:badlands", "RED");
        map.put("minecraft:nether_wastes", "RED");
        map.put("minecraft:forest", "GREEN");
        map.put("minecraft:taiga", "GREEN");
        map.put("minecraft:jungle", "GREEN");
        map.put("minecraft:the_end", "COLORLESS");
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Double> defaultMultipliers() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("minecraft:desert", 1.25);
        map.put("minecraft:badlands", 1.25);
        map.put("minecraft:deep_ocean", 1.20);
        map.put("minecraft:nether_wastes", 1.50);
        return Collections.unmodifiableMap(map);
    }

    public static ManaConfig defaults() {
        return new ManaConfig(
            100, 1, 1, 200, 1,
            5, 20, 5, 1.25,
            2.0, false,
            defaultAffinities(),
            defaultMultipliers()
        );
    }

    public ManaConfig sanitize() {
        Map<String, Double> sanitizedMults = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : biomeMultipliers.entrySet()) {
            double val = entry.getValue() != null ? entry.getValue() : 1.0;
            sanitizedMults.put(entry.getKey(), Math.clamp(val, 0.1, 10.0));
        }

        return new ManaConfig(
            Math.clamp(biomeRegenIntervalTicks, 20, 72000),
            Math.clamp(baseRegenAmount, 1, 20),
            Math.clamp(landManaBonus, 0, 10),
            Math.clamp(decayIntervalTicks, 20, 72000),
            Math.clamp(decayAmount, 0, 10),
            Math.clamp(initialMaxMana, 1, 50),
            Math.clamp(hardCapMaxMana, 5, 100),
            Math.clamp(baseXpLevelCost, 1, 100),
            Math.clamp(manaUpgradeXpMultiplier, 1.0, 5.0),
            Math.clamp(phyrexianLifeCostDamage, 1.0, 20.0),
            allowLethalPhyrexianPayment,
            biomeAffinities,
            sanitizedMults
        );
    }

    public String getBiomeAffinity(String biomeId) {
        if (biomeId == null) return "COLORLESS";
        return biomeAffinities.getOrDefault(biomeId, "COLORLESS");
    }

    public double getBiomeMultiplier(String biomeId) {
        if (biomeId == null) return 1.0;
        return biomeMultipliers.getOrDefault(biomeId, 1.0);
    }

    public TomlDocument toTomlDocument() {
        TomlDocument doc = new TomlDocument();

        doc.addSectionComment("regeneration", "# Interval in game ticks between natural biome mana regeneration pulses");
        doc.set("regeneration", "biome_regen_interval_ticks", biomeRegenIntervalTicks);
        doc.set("regeneration", "base_regen_amount", baseRegenAmount);
        doc.set("regeneration", "land_mana_bonus", landManaBonus);
        doc.set("regeneration", "decay_interval_ticks", decayIntervalTicks);
        doc.set("regeneration", "decay_amount", decayAmount);

        doc.addSectionComment("capacity", "# Starting maximum mana cap and XP upgrade settings");
        doc.set("capacity", "initial_max_mana", initialMaxMana);
        doc.set("capacity", "hard_cap_max_mana", hardCapMaxMana);
        doc.set("capacity", "base_xp_level_cost", baseXpLevelCost);
        doc.set("capacity", "mana_upgrade_xp_multiplier", manaUpgradeXpMultiplier);

        doc.addSectionComment("phyrexian", "# Phyrexian mana life cost settings");
        doc.set("phyrexian", "phyrexian_life_cost_damage", phyrexianLifeCostDamage);
        doc.set("phyrexian", "allow_lethal_phyrexian_payment", allowLethalPhyrexianPayment);

        doc.addSectionComment("biome_affinities", "# Custom biome color affinities");
        for (Map.Entry<String, String> entry : biomeAffinities.entrySet()) {
            doc.set("biome_affinities", entry.getKey(), entry.getValue());
        }

        doc.addSectionComment("biome_multipliers", "# Custom biome mana regeneration multipliers");
        for (Map.Entry<String, Double> entry : biomeMultipliers.entrySet()) {
            doc.set("biome_multipliers", entry.getKey(), entry.getValue());
        }

        return doc;
    }

    public static ManaConfig fromTomlDocument(TomlDocument doc) {
        ManaConfig def = defaults();

        int regenTicks = doc.getInt("regeneration", "biome_regen_interval_ticks", def.biomeRegenIntervalTicks);
        int baseRegen = doc.getInt("regeneration", "base_regen_amount", def.baseRegenAmount);
        int landBonus = doc.getInt("regeneration", "land_mana_bonus", def.landManaBonus);
        int decayTicks = doc.getInt("regeneration", "decay_interval_ticks", def.decayIntervalTicks);
        int decayAmt = doc.getInt("regeneration", "decay_amount", def.decayAmount);

        int initMax = doc.getInt("capacity", "initial_max_mana", def.initialMaxMana);
        int hardCap = doc.getInt("capacity", "hard_cap_max_mana", def.hardCapMaxMana);
        int baseXp = doc.getInt("capacity", "base_xp_level_cost", def.baseXpLevelCost);
        double xpMult = doc.getDouble("capacity", "mana_upgrade_xp_multiplier", def.manaUpgradeXpMultiplier);

        double phyDamage = doc.getDouble("phyrexian", "phyrexian_life_cost_damage", def.phyrexianLifeCostDamage);
        boolean allowLethal = doc.getBoolean("phyrexian", "allow_lethal_phyrexian_payment", def.allowLethalPhyrexianPayment);

        Map<String, String> affinities = new LinkedHashMap<>(defaultAffinities());
        if (doc.hasSection("biome_affinities")) {
            Map<String, Object> sec = doc.getSection("biome_affinities");
            for (Map.Entry<String, Object> entry : sec.entrySet()) {
                affinities.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Map<String, Double> multipliers = new LinkedHashMap<>(defaultMultipliers());
        if (doc.hasSection("biome_multipliers")) {
            Map<String, Object> sec = doc.getSection("biome_multipliers");
            for (Map.Entry<String, Object> entry : sec.entrySet()) {
                try {
                    multipliers.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
                } catch (NumberFormatException ignored) {}
            }
        }

        return new ManaConfig(
            regenTicks, baseRegen, landBonus, decayTicks, decayAmt,
            initMax, hardCap, baseXp, xpMult,
            phyDamage, allowLethal, affinities, multipliers
        ).sanitize();
    }

    public static ManaConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            ManaConfig cfg = defaults();
            cfg.save(path);
            return cfg;
        }
        try {
            TomlDocument doc = TomlParser.parse(path);
            return fromTomlDocument(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load mana.toml at " + path + "; falling back to defaults", e);
            return defaults();
        }
    }

    public void save(Path path) {
        try {
            TomlWriter.write(toTomlDocument(), path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write mana config to " + path, e);
        }
    }
}
