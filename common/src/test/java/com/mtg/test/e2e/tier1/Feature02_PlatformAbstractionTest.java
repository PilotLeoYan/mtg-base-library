package com.mtg.test.e2e.tier1;

import com.mtg.network.CustomPacket;
import com.mtg.platform.INetworkHelper;
import com.mtg.platform.IPlatformHelper;
import com.mtg.platform.Services;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 02: Platform Abstraction ServiceLoader E2E Test")
public class Feature02_PlatformAbstractionTest {

    @Test
    @DisplayName("1. Discover IPlatformHelper via standard java.util.ServiceLoader")
    void testDiscoverPlatformHelperViaServiceLoader() {
        Optional<IPlatformHelper> helper = ServiceLoader.load(IPlatformHelper.class).findFirst();
        assertTrue(helper.isPresent(), "IPlatformHelper must be discoverable via ServiceLoader");
        assertNotNull(helper.get().getPlatformName(), "Platform name must not be null");
        assertFalse(helper.get().getPlatformName().isBlank(), "Platform name must not be blank");
    }

    @Test
    @DisplayName("2. Discover INetworkHelper via standard java.util.ServiceLoader")
    void testDiscoverNetworkHelperViaServiceLoader() {
        Optional<INetworkHelper> helper = ServiceLoader.load(INetworkHelper.class).findFirst();
        assertTrue(helper.isPresent(), "INetworkHelper must be discoverable via ServiceLoader");
    }

    @Test
    @DisplayName("3. Platform environment query returns valid development configuration directory")
    void testPlatformEnvironmentConfiguration() {
        IPlatformHelper platform = Services.PLATFORM;
        assertNotNull(platform, "Services.PLATFORM must be non-null");
        assertTrue(platform.isDevelopmentEnvironment(), "Test suite runs in development environment");

        Path configDir = platform.getConfigDirectory();
        assertNotNull(configDir, "Config directory path must not be null");
        assertFalse(configDir.toString().isBlank(), "Config directory path must not be blank");
    }

    @Test
    @DisplayName("4. Network helper dispatches packets through mock network pipeline safely")
    void testNetworkHelperDispatchExecution() {
        INetworkHelper network = Services.NETWORK;
        assertNotNull(network, "Services.NETWORK must be non-null");

        CustomPacket mockPacket = new CustomPacket() {
            @Override
            public String getChannelId() {
                return "test:channel";
            }

            @Override
            public byte[] encode() {
                return new byte[]{0x01, 0x02};
            }
        };

        assertDoesNotThrow(() -> network.sendToServer(mockPacket), "sendToServer must execute safely");
        assertDoesNotThrow(() -> network.sendToPlayer(new Object(), mockPacket), "sendToPlayer must execute safely");
        assertDoesNotThrow(() -> network.sendToAllTracking(new Object(), mockPacket), "sendToAllTracking must execute safely");
    }

    @Test
    @DisplayName("5. Services singleton cache returns consistent instances across queries")
    void testServicesCacheConsistency() {
        assertSame(Services.PLATFORM, Services.load(IPlatformHelper.class), "Cached Services.PLATFORM should match dynamic load");
        assertSame(Services.NETWORK, Services.load(INetworkHelper.class), "Cached Services.NETWORK should match dynamic load");
    }
}
