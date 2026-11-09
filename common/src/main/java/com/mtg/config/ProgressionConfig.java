package com.mtg.config;

import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public record ProgressionConfig(
    int initialHandCapacity,
    int maxHandCapacity,
    int handUpgradeBaseXpCost,
    double handUpgradeXpMultiplier,
    int thresholdCommonToUncommon,
    int thresholdUncommonToRare,
    int thresholdRareToMythic,
    int thresholdMythicVault,
    double miningCardDropChance,
    double farmingCardDropChance,
    double mobCardDropChance,
    double bossCardDropChance,
    int dungeonChestCardCount,
    int starterPackCardCount
) {
    private static final Logger LOGGER = Logger.getLogger(ProgressionConfig.class.getName());

    public static ProgressionConfig defaults() {
        return new ProgressionConfig(
            4, 7, 10, 1.5,
            4, 4, 4, 4,
            0.02, 0.01, 0.05, 1.00,
            2, 5
        );
    }

    public ProgressionConfig sanitize() {
        return new ProgressionConfig(
            Math.clamp(initialHandCapacity, 1, 7),
            Math.clamp(maxHandCapacity, 4, 7),
            Math.clamp(handUpgradeBaseXpCost, 5, 50),
            Math.clamp(handUpgradeXpMultiplier, 1.0, 3.0),
            Math.clamp(thresholdCommonToUncommon, 2, 20),
            Math.clamp(thresholdUncommonToRare, 2, 20),
            Math.clamp(thresholdRareToMythic, 2, 20),
            Math.clamp(thresholdMythicVault, 2, 20),
            Math.clamp(miningCardDropChance, 0.0, 1.0),
            Math.clamp(farmingCardDropChance, 0.0, 1.0),
            Math.clamp(mobCardDropChance, 0.0, 1.0),
            Math.clamp(bossCardDropChance, 0.0, 1.0),
            Math.clamp(dungeonChestCardCount, 0, 10),
            Math.clamp(starterPackCardCount, 1, 10)
        );
    }

    public TomlDocument toTomlDocument() {
        TomlDocument doc = new TomlDocument();

        doc.addSectionComment("hand_capacity", "# Dedicated hand slot capacities and XP upgrade costs");
        doc.set("hand_capacity", "initial_hand_capacity", initialHandCapacity);
        doc.set("hand_capacity", "max_hand_capacity", maxHandCapacity);
        doc.set("hand_capacity", "hand_upgrade_base_xp_cost", handUpgradeBaseXpCost);
        doc.set("hand_capacity", "hand_upgrade_xp_multiplier", handUpgradeXpMultiplier);

        doc.addSectionComment("duplicate_protection", "# Duplicate card thresholds for higher-rarity wildcard transmutation");
        doc.set("duplicate_protection", "threshold_common_to_uncommon", thresholdCommonToUncommon);
        doc.set("duplicate_protection", "threshold_uncommon_to_rare", thresholdUncommonToRare);
        doc.set("duplicate_protection", "threshold_rare_to_mythic", thresholdRareToMythic);
        doc.set("duplicate_protection", "threshold_mythic_vault", thresholdMythicVault);

        doc.addSectionComment("drop_rates", "# Card discovery and drop chances from mining, farming, mobs, and loot");
        doc.set("drop_rates", "mining_card_drop_chance", miningCardDropChance);
        doc.set("drop_rates", "farming_card_drop_chance", farmingCardDropChance);
        doc.set("drop_rates", "mob_card_drop_chance", mobCardDropChance);
        doc.set("drop_rates", "boss_card_drop_chance", bossCardDropChance);
        doc.set("drop_rates", "dungeon_chest_card_count", dungeonChestCardCount);
        doc.set("drop_rates", "starter_pack_card_count", starterPackCardCount);

        return doc;
    }

    public static ProgressionConfig fromTomlDocument(TomlDocument doc) {
        ProgressionConfig def = defaults();

        int initHand = doc.getInt("hand_capacity", "initial_hand_capacity", def.initialHandCapacity);
        int maxHand = doc.getInt("hand_capacity", "max_hand_capacity", def.maxHandCapacity);
        int baseXp = doc.getInt("hand_capacity", "hand_upgrade_base_xp_cost", def.handUpgradeBaseXpCost);
        double xpMult = doc.getDouble("hand_capacity", "hand_upgrade_xp_multiplier", def.handUpgradeXpMultiplier);

        int cToU = doc.getInt("duplicate_protection", "threshold_common_to_uncommon", def.thresholdCommonToUncommon);
        int uToR = doc.getInt("duplicate_protection", "threshold_uncommon_to_rare", def.thresholdUncommonToRare);
        int rToM = doc.getInt("duplicate_protection", "threshold_rare_to_mythic", def.thresholdRareToMythic);
        int mVault = doc.getInt("duplicate_protection", "threshold_mythic_vault", def.thresholdMythicVault);

        double miningDrop = doc.getDouble("drop_rates", "mining_card_drop_chance", def.miningCardDropChance);
        double farmingDrop = doc.getDouble("drop_rates", "farming_card_drop_chance", def.farmingCardDropChance);
        double mobDrop = doc.getDouble("drop_rates", "mob_card_drop_chance", def.mobCardDropChance);
        double bossDrop = doc.getDouble("drop_rates", "boss_card_drop_chance", def.bossCardDropChance);
        int dungeonCount = doc.getInt("drop_rates", "dungeon_chest_card_count", def.dungeonChestCardCount);
        int starterCount = doc.getInt("drop_rates", "starter_pack_card_count", def.starterPackCardCount);

        return new ProgressionConfig(
            initHand, maxHand, baseXp, xpMult,
            cToU, uToR, rToM, mVault,
            miningDrop, farmingDrop, mobDrop, bossDrop, dungeonCount, starterCount
        ).sanitize();
    }

    public static ProgressionConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            ProgressionConfig cfg = defaults();
            cfg.save(path);
            return cfg;
        }
        try {
            TomlDocument doc = TomlParser.parse(path);
            return fromTomlDocument(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load progression.toml at " + path + "; falling back to defaults", e);
            return defaults();
        }
    }

    public void save(Path path) {
        try {
            TomlWriter.write(toTomlDocument(), path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write progression config to " + path, e);
        }
    }
}
