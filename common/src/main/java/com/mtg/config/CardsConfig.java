package com.mtg.config;

import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public record CardsConfig(
    double cardCooldownMultiplier,
    double landCooldownSeconds,
    double instantBaseCooldownSeconds,
    double duplicateCooldownReduction,
    boolean enableSorceryWindupDelays,
    double sorceryDelayPerManaSeconds,
    boolean damageInterruptsChanneling,
    double damageInterruptThreshold,
    int maxActiveSummonsPerPlayer,
    boolean enableSummonAutoGuard,
    int summonLeashDistanceBlocks,
    int enchantmentBaseDurationTicks
) {
    private static final Logger LOGGER = Logger.getLogger(CardsConfig.class.getName());

    public static CardsConfig defaults() {
        return new CardsConfig(
            1.0, 60.0, 3.0, 0.05,
            true, 0.5, true, 4.0,
            3, true, 48,
            1200
        );
    }

    public CardsConfig sanitize() {
        return new CardsConfig(
            Math.clamp(cardCooldownMultiplier, 0.05, 20.0),
            Math.clamp(landCooldownSeconds, 5.0, 300.0),
            Math.clamp(instantBaseCooldownSeconds, 0.5, 60.0),
            Math.clamp(duplicateCooldownReduction, 0.0, 0.50),
            enableSorceryWindupDelays,
            Math.clamp(sorceryDelayPerManaSeconds, 0.1, 3.0),
            damageInterruptsChanneling,
            Math.clamp(damageInterruptThreshold, 0.5, 20.0),
            Math.clamp(maxActiveSummonsPerPlayer, 1, 10),
            enableSummonAutoGuard,
            Math.clamp(summonLeashDistanceBlocks, 16, 128),
            Math.clamp(enchantmentBaseDurationTicks, 100, 72000)
        );
    }

    public TomlDocument toTomlDocument() {
        TomlDocument doc = new TomlDocument();

        doc.addSectionComment("cooldowns", "# Cooldown duration settings");
        doc.set("cooldowns", "card_cooldown_multiplier", cardCooldownMultiplier);
        doc.set("cooldowns", "land_cooldown_seconds", landCooldownSeconds);
        doc.set("cooldowns", "instant_base_cooldown_seconds", instantBaseCooldownSeconds);
        doc.set("cooldowns", "duplicate_cooldown_reduction", duplicateCooldownReduction);

        doc.addSectionComment("delays", "# Sorcery and channeling delays");
        doc.set("delays", "enable_sorcery_windup_delays", enableSorceryWindupDelays);
        doc.set("delays", "sorcery_delay_per_mana_seconds", sorceryDelayPerManaSeconds);
        doc.set("delays", "damage_interrupts_channeling", damageInterruptsChanneling);
        doc.set("delays", "damage_interrupt_threshold", damageInterruptThreshold);

        doc.addSectionComment("summons", "# Summon limits and AI");
        doc.set("summons", "max_active_summons_per_player", maxActiveSummonsPerPlayer);
        doc.set("summons", "enable_summon_auto_guard", enableSummonAutoGuard);
        doc.set("summons", "summon_leash_distance_blocks", summonLeashDistanceBlocks);

        doc.addSectionComment("enchantments", "# Enchantment aura durations");
        doc.set("enchantments", "enchantment_base_duration_ticks", enchantmentBaseDurationTicks);

        return doc;
    }

    public static CardsConfig fromTomlDocument(TomlDocument doc) {
        CardsConfig def = defaults();

        double cdMult = doc.getDouble("cooldowns", "card_cooldown_multiplier", def.cardCooldownMultiplier);
        double landCd = doc.getDouble("cooldowns", "land_cooldown_seconds", def.landCooldownSeconds);
        double instantCd = doc.getDouble("cooldowns", "instant_base_cooldown_seconds", def.instantBaseCooldownSeconds);
        double dupRed = doc.getDouble("cooldowns", "duplicate_cooldown_reduction", def.duplicateCooldownReduction);

        boolean enableDelays = doc.getBoolean("delays", "enable_sorcery_windup_delays", def.enableSorceryWindupDelays);
        double delayPerMana = doc.getDouble("delays", "sorcery_delay_per_mana_seconds", def.sorceryDelayPerManaSeconds);
        boolean dmgInterrupts = doc.getBoolean("delays", "damage_interrupts_channeling", def.damageInterruptsChanneling);
        double dmgThreshold = doc.getDouble("delays", "damage_interrupt_threshold", def.damageInterruptThreshold);

        int maxSummons = doc.getInt("summons", "max_active_summons_per_player", def.maxActiveSummonsPerPlayer);
        boolean autoGuard = doc.getBoolean("summons", "enable_summon_auto_guard", def.enableSummonAutoGuard);
        int leashDist = doc.getInt("summons", "summon_leash_distance_blocks", def.summonLeashDistanceBlocks);

        int enchDuration = doc.getInt("enchantments", "enchantment_base_duration_ticks", def.enchantmentBaseDurationTicks);

        return new CardsConfig(
            cdMult, landCd, instantCd, dupRed,
            enableDelays, delayPerMana, dmgInterrupts, dmgThreshold,
            maxSummons, autoGuard, leashDist, enchDuration
        ).sanitize();
    }

    public static CardsConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            CardsConfig cfg = defaults();
            cfg.save(path);
            return cfg;
        }
        try {
            TomlDocument doc = TomlParser.parse(path);
            return fromTomlDocument(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load cards.toml at " + path + "; falling back to defaults", e);
            return defaults();
        }
    }

    public void save(Path path) {
        try {
            TomlWriter.write(toTomlDocument(), path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write cards config to " + path, e);
        }
    }
}
