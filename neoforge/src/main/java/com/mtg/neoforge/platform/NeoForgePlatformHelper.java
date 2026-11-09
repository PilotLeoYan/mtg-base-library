package com.mtg.neoforge.platform;

import com.mtg.platform.IPlatformHelper;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return true;
    }

    @Override
    public Path getConfigDirectory() {
        return Paths.get("config");
    }
}
