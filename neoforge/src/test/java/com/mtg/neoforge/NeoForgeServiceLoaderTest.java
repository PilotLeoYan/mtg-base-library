package com.mtg.neoforge;

import com.mtg.MtgConstants;
import com.mtg.neoforge.platform.NeoForgeNetworkHelper;
import com.mtg.neoforge.platform.NeoForgePlatformHelper;
import com.mtg.platform.INetworkHelper;
import com.mtg.platform.IPlatformHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NeoForge Platform ServiceLoader Binding Verification")
public class NeoForgeServiceLoaderTest {

    @Test
    @DisplayName("NeoForgePlatformHelper is bound and returns 'NeoForge'")
    void testNeoForgePlatformHelper() {
        IPlatformHelper helper = ServiceLoader.load(IPlatformHelper.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("NeoForgePlatformHelper not registered"));

        assertInstanceOf(NeoForgePlatformHelper.class, helper);
        assertEquals("NeoForge", helper.getPlatformName());
        assertTrue(helper.isDedicatedServer());
        assertFalse(helper.isClient());
        assertEquals("mtg", MtgConstants.MOD_ID);
    }

    @Test
    @DisplayName("NeoForgeNetworkHelper is bound in NeoForge target")
    void testNeoForgeNetworkHelper() {
        INetworkHelper helper = ServiceLoader.load(INetworkHelper.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("NeoForgeNetworkHelper not registered"));

        assertInstanceOf(NeoForgeNetworkHelper.class, helper);
    }
}
