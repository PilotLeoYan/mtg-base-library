package com.mtg.platform;

import com.mtg.MtgConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServiceLoader Discovery and Resolution Tests")
public class ServiceLoaderResolutionTest {

    @Test
    @DisplayName("Verify mod constants are defined correctly for Minecraft 26.3")
    void testModConstants() {
        assertEquals("mtg", MtgConstants.MOD_ID);
        assertEquals("MTG Base Library", MtgConstants.MOD_NAME);
        assertEquals("1.0.0", MtgConstants.MOD_VERSION);
        assertEquals("26.3", MtgConstants.MINECRAFT_VERSION);
    }

    @Test
    @DisplayName("Should successfully discover IPlatformHelper via ServiceLoader")
    void testPlatformHelperDiscovery() {
        Optional<IPlatformHelper> helper = ServiceLoader.load(IPlatformHelper.class).findFirst();
        assertTrue(helper.isPresent(), "Expected an IPlatformHelper service to be registered and discoverable");
        assertNotNull(helper.get().getPlatformName(), "Platform name must not be null");
        assertFalse(helper.get().getPlatformName().isBlank(), "Platform name must not be blank");
        assertNotNull(helper.get().getConfigDirectory(), "Config directory must not be null");
    }

    @Test
    @DisplayName("Should successfully discover INetworkHelper via ServiceLoader")
    void testNetworkHelperDiscovery() {
        Optional<INetworkHelper> helper = ServiceLoader.load(INetworkHelper.class).findFirst();
        assertTrue(helper.isPresent(), "Expected an INetworkHelper service to be registered and discoverable");
    }

    @Test
    @DisplayName("Services singleton holder returns non-null bound instances")
    void testServicesSingletonBinding() {
        assertNotNull(Services.PLATFORM, "Services.PLATFORM must be initialized");
        assertNotNull(Services.NETWORK, "Services.NETWORK must be initialized");
        assertSame(Services.PLATFORM, Services.load(IPlatformHelper.class), "Subsequent loads should return consistent instance");
    }

    @Test
    @DisplayName("Services.load throws descriptive exception when service implementation is missing")
    void testMissingServiceThrowsDescriptiveException() {
        interface UnregisteredService {}

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            Services.load(UnregisteredService.class);
        });
        assertTrue(ex.getMessage().contains(UnregisteredService.class.getName()),
                "Exception message must specify the missing service FQCN");
    }

    @Test
    @DisplayName("ServiceLoader discovery is thread-safe across concurrent accesses")
    void testConcurrentServiceAccess() throws InterruptedException {
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    IPlatformHelper helper = Services.PLATFORM;
                    assertNotNull(helper.getPlatformName());
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertNull(failure.get(), "Concurrent access to Services.PLATFORM failed: " + failure.get());
    }
}
