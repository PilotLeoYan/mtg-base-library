package com.mtg.forge;

import com.mtg.MtgConstants;
import com.mtg.forge.platform.ForgeNetworkHelper;
import com.mtg.forge.platform.ForgePlatformHelper;
import com.mtg.platform.INetworkHelper;
import com.mtg.platform.IPlatformHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Forge Platform ServiceLoader Binding Verification")
public class ForgeServiceLoaderTest {

    @Test
    @DisplayName("ForgePlatformHelper is bound and returns 'Forge'")
    void testForgePlatformHelper() {
        IPlatformHelper helper = ServiceLoader.load(IPlatformHelper.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ForgePlatformHelper not registered"));

        assertInstanceOf(ForgePlatformHelper.class, helper);
        assertEquals("Forge", helper.getPlatformName());
        assertTrue(helper.isDedicatedServer());
        assertFalse(helper.isClient());
        assertEquals("mtg", MtgConstants.MOD_ID);
    }

    @Test
    @DisplayName("ForgeNetworkHelper is bound in Forge target")
    void testForgeNetworkHelper() {
        INetworkHelper helper = ServiceLoader.load(INetworkHelper.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ForgeNetworkHelper not registered"));

        assertInstanceOf(ForgeNetworkHelper.class, helper);
    }
}
