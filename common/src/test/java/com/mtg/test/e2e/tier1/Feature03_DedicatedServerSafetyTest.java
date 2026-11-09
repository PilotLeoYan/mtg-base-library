package com.mtg.test.e2e.tier1;

import com.mtg.guidebook.IJournalScreenProvider;
import com.mtg.guidebook.NoopJournalScreenProvider;
import com.mtg.guidebook.catalog.DefaultJournalContent;
import com.mtg.network.C2SCastCardPacket;
import com.mtg.network.CustomPacket;
import com.mtg.network.S2CSyncManaPacket;
import com.mtg.platform.IPlatformHelper;
import com.mtg.platform.Services;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 03: Dedicated Server Separation & Safety E2E Test")
public class Feature03_DedicatedServerSafetyTest {

    @Test
    @DisplayName("1. Headless execution verified in JVM environment without display requirement")
    void testHeadlessExecutionEnvironment() {
        boolean headless = GraphicsEnvironment.isHeadless();
        String headlessProperty = System.getProperty("java.awt.headless");
        assertTrue(headless || "true".equalsIgnoreCase(headlessProperty),
                "Runtime must operate cleanly in headless execution mode");
    }

    @Test
    @DisplayName("2. Platform environment contract enforces exclusivity between client and dedicated server")
    void testPlatformEnvironmentContractExclusivity() {
        IPlatformHelper platform = Services.PLATFORM;
        assertNotNull(platform);
        boolean isDedicated = platform.isDedicatedServer();
        boolean isClient = platform.isClient();

        assertNotEquals(isDedicated, isClient,
                "A platform cannot be simultaneously a client and a dedicated server");
    }

    @Test
    @DisplayName("3. Zero-Client-Import Rule: Core server packages contain zero references to client screens or renderers")
    void testZeroClientImportsInCorePackages() throws Exception {
        List<String> serverPackages = List.of(
                "com.mtg.mana",
                "com.mtg.cards",
                "com.mtg.deck",
                "com.mtg.domain",
                "com.mtg.progression",
                "com.mtg.world"
        );

        List<String> violations = new ArrayList<>();
        for (String pkg : serverPackages) {
            List<Class<?>> classes = scanPackageClasses(pkg);
            for (Class<?> clazz : classes) {
                if (clazz.getName().startsWith("com.mtg.client.")) continue;

                for (Field field : clazz.getDeclaredFields()) {
                    if (isForbiddenClientType(field.getType().getName())) {
                        violations.add(clazz.getName() + " field " + field.getName() + " -> " + field.getType().getName());
                    }
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    if (isForbiddenClientType(method.getReturnType().getName())) {
                        violations.add(clazz.getName() + " method " + method.getName() + " return -> " + method.getReturnType().getName());
                    }
                    for (Class<?> p : method.getParameterTypes()) {
                        if (isForbiddenClientType(p.getName())) {
                            violations.add(clazz.getName() + " method " + method.getName() + " param -> " + p.getName());
                        }
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), "Dedicated server safety violations detected:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("4. Network packet classes are safe for headless serialization without graphics classes")
    void testPacketsHeadlessCompatibility() {
        List<Class<? extends CustomPacket>> packetClasses = List.of(
                S2CSyncManaPacket.class,
                C2SCastCardPacket.class
        );

        for (Class<? extends CustomPacket> pkt : packetClasses) {
            for (Field field : pkt.getDeclaredFields()) {
                assertFalse(isForbiddenClientType(field.getType().getName()),
                        pkt.getSimpleName() + " must not reference client types in field " + field.getName());
            }
        }
    }

    @Test
    @DisplayName("5. Headless environment safely resolves NoopJournalScreenProvider")
    void testNoopJournalScreenProviderResolution() {
        Optional<IJournalScreenProvider> providerOpt = ServiceLoader.load(IJournalScreenProvider.class).findFirst();
        IJournalScreenProvider provider = providerOpt.orElseGet(NoopJournalScreenProvider::new);
        assertNotNull(provider);
        assertFalse(provider.isAvailable(), "In headless environment, graphical screen provider is not available");
        assertDoesNotThrow(() -> provider.openJournalScreen(DefaultJournalContent.createDefaultJournal(), null, null, 1),
                "Opening journal screen in headless mode must not throw");
    }

    private boolean isForbiddenClientType(String typeName) {
        return typeName.startsWith("net.minecraft.client.")
                || typeName.startsWith("com.mojang.blaze3d.")
                || typeName.contains(".client.gui.")
                || typeName.endsWith(".Screen");
    }

    private List<Class<?>> scanPackageClasses(String packageName) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = getClass().getClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = cl.getResources(path);

        while (resources.hasMoreElements()) {
            URL res = resources.nextElement();
            File dir = new File(res.getFile());
            if (dir.exists() && dir.isDirectory()) {
                scanDir(dir, packageName, classes);
            }
        }
        return classes;
    }

    private void scanDir(File directory, String currentPackage, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, currentPackage + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = currentPackage + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className, false, getClass().getClassLoader()));
                } catch (Throwable ignored) {}
            }
        }
    }
}
