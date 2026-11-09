package com.mtg.platform;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface IPlatformHelper {
    /**
     * Name of the target mod loader (e.g., "Fabric", "NeoForge", "Forge").
     */
    String getPlatformName();

    /**
     * Returns true if running in a physical client environment.
     */
    boolean isClient();

    /**
     * Returns true if running on a dedicated server (headless).
     */
    boolean isDedicatedServer();

    /**
     * Returns true if running in a development / test environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Directory path where configuration files are located.
     */
    Path getConfigDirectory();

    /**
     * Directory path of the game installation root.
     */
    default Path getGameDirectory() {
        return Paths.get(".");
    }

    /**
     * Checks if another mod is loaded by mod ID.
     */
    default boolean isModLoaded(String modId) {
        return false;
    }
}
