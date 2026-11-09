package com.mtg.fabric;

import com.mtg.MtgConstants;
import com.mtg.fabric.platform.FabricNetworkHelper;
import com.mtg.fabric.platform.FabricPlatformHelper;
import com.mtg.platform.INetworkHelper;
import com.mtg.platform.IPlatformHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fabric Platform ServiceLoader Binding Verification")
public class FabricServiceLoaderTest {

    @Test
    @DisplayName("FabricPlatformHelper is bound and returns 'Fabric'")
    void testFabricPlatformHelper() {
        IPlatformHelper helper = ServiceLoader.load(IPlatformHelper.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FabricPlatformHelper not registered"));

        assertInstanceOf(FabricPlatformHelper.class, helper);
        assertEquals("Fabric", helper.getPlatformName());
        assertTrue(helper.isDedicatedServer());
        assertFalse(helper.isClient());
        assertEquals("mtg", MtgConstants.MOD_ID);
    }

    @Test
    @DisplayName("FabricNetworkHelper is bound in Fabric target")
    void testFabricNetworkHelper() {
        INetworkHelper helper = ServiceLoader.load(INetworkHelper.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FabricNetworkHelper not registered"));

        assertInstanceOf(FabricNetworkHelper.class, helper);
    }
}
