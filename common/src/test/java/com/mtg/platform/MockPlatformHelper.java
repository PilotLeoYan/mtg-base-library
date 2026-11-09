package com.mtg.platform;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MockPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Common-Test";
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
        return Paths.get("build/test-config");
    }
}
