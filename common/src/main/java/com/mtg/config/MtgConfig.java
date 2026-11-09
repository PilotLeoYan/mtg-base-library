package com.mtg.config;

import com.mtg.platform.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MtgConfig {
    private static final Logger LOGGER = Logger.getLogger(MtgConfig.class.getName());

    private static volatile ManaConfig mana = ManaConfig.defaults();
    private static volatile CardsConfig cards = CardsConfig.defaults();
    private static volatile ProgressionConfig progression = ProgressionConfig.defaults();
    private static volatile WorldGenConfig worldGen = WorldGenConfig.defaults();

    private MtgConfig() {}

    public static ManaConfig getMana() { return mana; }
    public static CardsConfig getCards() { return cards; }
    public static ProgressionConfig getProgression() { return progression; }
    public static WorldGenConfig getWorldGen() { return worldGen; }

    public static void setMana(ManaConfig config) {
        mana = config != null ? config.sanitize() : ManaConfig.defaults();
    }

    public static void setCards(CardsConfig config) {
        cards = config != null ? config.sanitize() : CardsConfig.defaults();
    }

    public static void setProgression(ProgressionConfig config) {
        progression = config != null ? config.sanitize() : ProgressionConfig.defaults();
    }

    public static void setWorldGen(WorldGenConfig config) {
        worldGen = config != null ? config.sanitize() : WorldGenConfig.defaults();
    }

    public static synchronized void init() {
        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("mtg");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create MTG config directory at: " + configDir, e);
        }
        loadAll(configDir);
    }

    public static synchronized void loadAll(Path configDir) {
        mana = ManaConfig.loadOrCreate(configDir.resolve("mana.toml"));
        cards = CardsConfig.loadOrCreate(configDir.resolve("cards.toml"));
        progression = ProgressionConfig.loadOrCreate(configDir.resolve("progression.toml"));
        worldGen = WorldGenConfig.loadOrCreate(configDir.resolve("world_gen.toml"));
    }

    public static synchronized void saveAll(Path configDir) {
        mana.save(configDir.resolve("mana.toml"));
        cards.save(configDir.resolve("cards.toml"));
        progression.save(configDir.resolve("progression.toml"));
        worldGen.save(configDir.resolve("world_gen.toml"));
    }

    public static synchronized void resetToDefaults() {
        mana = ManaConfig.defaults();
        cards = CardsConfig.defaults();
        progression = ProgressionConfig.defaults();
        worldGen = WorldGenConfig.defaults();
    }
}
