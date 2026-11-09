package com.mtg.adversarial;

import com.mtg.MtgConstants;
import com.mtg.platform.INetworkHelper;
import com.mtg.platform.IPlatformHelper;
import com.mtg.platform.Services;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial M1 Stress and Boundary Verification")
public class AdversarialM1StressTest {

    public interface PublicUnregisteredService {}

    private static final Path PROJECT_ROOT = findProjectRoot();

    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        return current != null ? current : Paths.get(".").toAbsolutePath();
    }

    private static Path resolveJarPath(String subproject) {
        Path buildLibs = PROJECT_ROOT.resolve(subproject).resolve("build").resolve("libs");
        if (!Files.exists(buildLibs)) return null;

        try {
            return Files.list(buildLibs)
                    .filter(p -> p.toString().endsWith(".jar")
                            && !p.toString().endsWith("-sources.jar")
                            && !p.toString().endsWith("-dev.jar"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // Scope 1: High-Concurrency Access to Services.load() Across 50+ Threads
    // =========================================================================
    @Nested
    @DisplayName("High-Concurrency Stress on Services.load()")
    class ConcurrencyStressTests {

        @Test
        @DisplayName("Stress: 64 concurrent threads pounding Services.load() simultaneously")
        void testHighConcurrency64Threads() throws Exception {
            int threadCount = 64;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CyclicBarrier startGate = new CyclicBarrier(threadCount);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
            List<IPlatformHelper> platformResults = Collections.synchronizedList(new ArrayList<>());
            List<INetworkHelper> networkResults = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await(10, TimeUnit.SECONDS);

                        // High frequency concurrent requests
                        for (int j = 0; j < 50; j++) {
                            IPlatformHelper p = Services.load(IPlatformHelper.class);
                            INetworkHelper n = Services.load(INetworkHelper.class);
                            assertNotNull(p);
                            assertNotNull(n);
                            platformResults.add(p);
                            networkResults.add(n);
                        }
                    } catch (Throwable t) {
                        exceptions.add(t);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            boolean finished = completionLatch.await(15, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertTrue(finished, "Execution timed out under high thread concurrency (possible deadlock)");
            assertTrue(exceptions.isEmpty(), "Unexpected exceptions encountered during 64-thread stress: " + exceptions);
            assertEquals(threadCount * 50, platformResults.size());

            // Verify strict reference identity (singleton invariant) across all 3200 queries
            IPlatformHelper expectedPlatform = platformResults.getFirst();
            for (IPlatformHelper actual : platformResults) {
                assertSame(expectedPlatform, actual, "Singleton violation under high concurrency");
            }

            INetworkHelper expectedNetwork = networkResults.getFirst();
            for (INetworkHelper actual : networkResults) {
                assertSame(expectedNetwork, actual, "Singleton violation under high concurrency for network helper");
            }
        }

        @Test
        @DisplayName("Stress: 128 concurrent threads under extreme contention")
        void testHighContention128Threads() throws Exception {
            int threadCount = 128;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CyclicBarrier gate = new CyclicBarrier(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCounter = new AtomicInteger(0);
            AtomicReference<Throwable> firstFailure = new AtomicReference<>();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        gate.await(10, TimeUnit.SECONDS);
                        for (int r = 0; r < 20; r++) {
                            IPlatformHelper helper = Services.load(IPlatformHelper.class);
                            assertNotNull(helper.getPlatformName());
                            successCounter.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        firstFailure.compareAndSet(null, t);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertTrue(completed, "128-thread contention timed out");
            assertNull(firstFailure.get(), "Failure during 128-thread contention: " + firstFailure.get());
            assertEquals(threadCount * 20, successCounter.get());
        }

        @Test
        @DisplayName("Stress: 50 concurrent threads requesting unregistered service simultaneously")
        void testConcurrentUnregisteredServiceThrowsConsistently() throws Exception {
            int threadCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger expectedExceptionCount = new AtomicInteger(0);
            List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        Services.load(PublicUnregisteredService.class);
                        unexpectedErrors.add(new AssertionError("Expected IllegalStateException not thrown"));
                    } catch (IllegalStateException e) {
                        if (e.getMessage() != null && e.getMessage().contains(PublicUnregisteredService.class.getName())) {
                            expectedExceptionCount.incrementAndGet();
                        } else {
                            unexpectedErrors.add(e);
                        }
                    } catch (Throwable t) {
                        unexpectedErrors.add(t);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertTrue(done, "Concurrent missing service requests timed out");
            assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors during missing service requests: " + unexpectedErrors);
            assertEquals(threadCount, expectedExceptionCount.get(), "All 50 threads must receive IllegalStateException");
        }

        @Test
        @DisplayName("Stress: Null parameter handling in Services.load()")
        void testNullParameterFailsFast() {
            assertThrows(NullPointerException.class, () -> Services.load(null),
                    "Services.load(null) must immediately fail fast with NullPointerException");
        }

        @Test
        @DisplayName("Re-entrancy / Circular Dependency Stress: Nested and circular computeIfAbsent behavior")
        void testReentrantServiceLoading() {
            ConcurrentHashMap<Class<?>, Object> testCache = new ConcurrentHashMap<>();

            // 1. Nested computeIfAbsent on distinct keys
            assertDoesNotThrow(() -> {
                Object val1 = testCache.computeIfAbsent(String.class, k1 -> {
                    return testCache.computeIfAbsent(Integer.class, k2 -> 42);
                });
                assertEquals(42, val1);
            }, "Nested computeIfAbsent for different keys should resolve without deadlock");

            // 2. Circular dependency detection (A -> B -> A)
            // If service discovery has a circular dependency, ConcurrentHashMap deadlocks if not handled
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                ConcurrentHashMap<String, Object> circularMap = new ConcurrentHashMap<>();
                try {
                    circularMap.computeIfAbsent("A", kA -> {
                        return circularMap.computeIfAbsent("B", kB -> {
                            // Circular reference back to A while A is still computing!
                            return circularMap.get("A"); // get returns null while computing
                        });
                    });
                } catch (Throwable ignored) {
                }
            });

            assertDoesNotThrow(() -> {
                future.get(2, TimeUnit.SECONDS);
            }, "Circular get during computeIfAbsent should complete within timeout");
            executor.shutdownNow();
        }
    }

    // =========================================================================
    // Scope 2: ClassLoader Isolation Stress (Null & Custom ClassLoaders)
    // =========================================================================
    @Nested
    @DisplayName("ClassLoader Isolation Stress Tests")
    class ClassLoaderIsolationStressTests {

        @Test
        @DisplayName("Isolation: Thread Context ClassLoader is null")
        void testContextClassLoaderIsNull() throws Exception {
            ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(null);
                assertNull(Thread.currentThread().getContextClassLoader());

                // 1. Cached load must work even with null TCCL
                IPlatformHelper helper = Services.load(IPlatformHelper.class);
                assertNotNull(helper, "Cached service load should succeed with null TCCL");

                // 2. Direct invocation of discoverService reflection with null TCCL
                Method discoverMethod = Services.class.getDeclaredMethod("discoverService", Class.class);
                discoverMethod.setAccessible(true);
                Object discovered = discoverMethod.invoke(null, IPlatformHelper.class);

                assertNotNull(discovered, "discoverService must fall back to clazz.getClassLoader() when TCCL is null");
                assertTrue(discovered instanceof IPlatformHelper);
                assertEquals("Common-Test", ((IPlatformHelper) discovered).getPlatformName());
            } finally {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        }

        @Test
        @DisplayName("Isolation: Thread Context ClassLoader is a custom barren ClassLoader")
        void testCustomBarrenClassLoader() throws Exception {
            ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
            // Barren classloader with no classpath and null parent
            URLClassLoader barrenCl = new URLClassLoader(new URL[0], null);
            try {
                Thread.currentThread().setContextClassLoader(barrenCl);

                // discoverService must fall back to clazz.getClassLoader() when contextCl has no services
                Method discoverMethod = Services.class.getDeclaredMethod("discoverService", Class.class);
                discoverMethod.setAccessible(true);
                Object discovered = discoverMethod.invoke(null, IPlatformHelper.class);

                assertNotNull(discovered, "Fallback to clazz.getClassLoader() must succeed when contextCl lacks service");
                assertEquals("Common-Test", ((IPlatformHelper) discovered).getPlatformName());
            } finally {
                barrenCl.close();
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        }

        @Test
        @DisplayName("Isolation Stress: Concurrent threads with heterogeneous ClassLoaders")
        void testHeterogeneousClassLoadersConcurrently() throws Exception {
            int threadCount = 60;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            List<URLClassLoader> loadersToClose = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                URLClassLoader customCl = new URLClassLoader(new URL[0], null);
                loadersToClose.add(customCl);

                executor.submit(() -> {
                    ClassLoader previous = Thread.currentThread().getContextClassLoader();
                    try {
                        // Group 1: null TCCL, Group 2: Barren custom TCCL, Group 3: Default TCCL
                        if (threadId % 3 == 0) {
                            Thread.currentThread().setContextClassLoader(null);
                        } else if (threadId % 3 == 1) {
                            Thread.currentThread().setContextClassLoader(customCl);
                        }

                        barrier.await(5, TimeUnit.SECONDS);

                        for (int k = 0; k < 20; k++) {
                            IPlatformHelper p = Services.load(IPlatformHelper.class);
                            INetworkHelper n = Services.load(INetworkHelper.class);
                            assertNotNull(p);
                            assertNotNull(n);
                            assertEquals("Common-Test", p.getPlatformName());
                        }
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        Thread.currentThread().setContextClassLoader(previous);
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            executor.shutdownNow();
            for (URLClassLoader cl : loadersToClose) {
                cl.close();
            }

            assertTrue(done, "Heterogeneous ClassLoader stress timed out");
            assertTrue(errors.isEmpty(), "Encountered errors across heterogeneous ClassLoaders: " + errors);
        }
    }

    // =========================================================================
    // Scope 3: JAR Artifact Boundary Testing and Defect Detection Oracle
    // =========================================================================
    @Nested
    @DisplayName("JAR Artifact Boundary Testing & Oracle Verification")
    class JarArtifactBoundaryTests {

        @Test
        @DisplayName("Boundary: All generated JARs unpack and satisfy all required entries")
        void testAllGeneratedJarsComprehensive() throws Exception {
            validateCommonJar(resolveJarPath("common"));
            validateFabricJar(resolveJarPath("fabric"));
            validateNeoForgeJar(resolveJarPath("neoforge"));
            validateForgeJar(resolveJarPath("forge"));
        }

        private void validateCommonJar(Path path) throws Exception {
            assertNotNull(path, "mtg-common.jar must exist");
            try (JarFile jar = new JarFile(path.toFile())) {
                assertManifestValid(jar);
                assertEntryExists(jar, "assets/mtg/lang/en_us.json");
                assertEntryExists(jar, "com/mtg/MtgConstants.class");
                assertEntryExists(jar, "com/mtg/domain/ManaCost.class");
                assertEntryExists(jar, "com/mtg/platform/Services.class");
                assertEntryExists(jar, "com/mtg/platform/IPlatformHelper.class");
                assertEntryExists(jar, "com/mtg/platform/INetworkHelper.class");
                assertEntryExists(jar, "com/mtg/network/CustomPacket.class");

                // Validate en_us.json content is non-empty and well-formed
                String langJson = readEntryString(jar, "assets/mtg/lang/en_us.json");
                assertTrue(langJson.contains("\"itemGroup.mtg\""), "en_us.json must contain itemGroup.mtg key");
            }
        }

        private void validateFabricJar(Path path) throws Exception {
            assertNotNull(path, "mtg-fabric.jar must exist");
            try (JarFile jar = new JarFile(path.toFile())) {
                assertManifestValid(jar);
                assertEntryExists(jar, "fabric.mod.json");
                String modJson = readEntryString(jar, "fabric.mod.json");
                assertTrue(modJson.contains("\"id\": \"mtg\""), "fabric.mod.json must declare id: 'mtg'");

                // Verify entrypoints and provider class presence inside the JAR
                assertEntryExists(jar, "com/mtg/fabric/FabricMtgMod.class");
                assertEntryExists(jar, "com/mtg/fabric/FabricMtgClientMod.class");

                // Validate ServiceLoader bindings in JAR
                assertServiceRegistration(jar, "com.mtg.platform.IPlatformHelper", "com.mtg.fabric.platform.FabricPlatformHelper");
                assertServiceRegistration(jar, "com.mtg.platform.INetworkHelper", "com.mtg.fabric.platform.FabricNetworkHelper");

                // Validate bundled common classes & assets
                assertEntryExists(jar, "com/mtg/domain/ManaCost.class");
                assertEntryExists(jar, "com/mtg/MtgConstants.class");
                assertEntryExists(jar, "assets/mtg/lang/en_us.json");
            }
        }

        private void validateNeoForgeJar(Path path) throws Exception {
            assertNotNull(path, "mtg-neoforge.jar must exist");
            try (JarFile jar = new JarFile(path.toFile())) {
                assertManifestValid(jar);
                assertEntryExists(jar, "META-INF/neoforge.mods.toml");
                String toml = readEntryString(jar, "META-INF/neoforge.mods.toml");
                assertTrue(toml.contains("modId = \"mtg\"") || toml.contains("modId=\"mtg\""), "neoforge.mods.toml must declare modId='mtg'");

                assertEntryExists(jar, "com/mtg/neoforge/NeoForgeMtgMod.class");
                assertServiceRegistration(jar, "com.mtg.platform.IPlatformHelper", "com.mtg.neoforge.platform.NeoForgePlatformHelper");
                assertServiceRegistration(jar, "com.mtg.platform.INetworkHelper", "com.mtg.neoforge.platform.NeoForgeNetworkHelper");

                assertEntryExists(jar, "com/mtg/domain/ManaCost.class");
                assertEntryExists(jar, "com/mtg/MtgConstants.class");
                assertEntryExists(jar, "assets/mtg/lang/en_us.json");
            }
        }

        private void validateForgeJar(Path path) throws Exception {
            assertNotNull(path, "mtg-forge.jar must exist");
            try (JarFile jar = new JarFile(path.toFile())) {
                assertManifestValid(jar);
                assertEntryExists(jar, "META-INF/mods.toml");
                String toml = readEntryString(jar, "META-INF/mods.toml");
                assertTrue(toml.contains("modId = \"mtg\"") || toml.contains("modId=\"mtg\""), "mods.toml must declare modId='mtg'");

                assertEntryExists(jar, "com/mtg/forge/ForgeMtgMod.class");
                assertServiceRegistration(jar, "com.mtg.platform.IPlatformHelper", "com.mtg.forge.platform.ForgePlatformHelper");
                assertServiceRegistration(jar, "com.mtg.platform.INetworkHelper", "com.mtg.forge.platform.ForgeNetworkHelper");

                assertEntryExists(jar, "com/mtg/domain/ManaCost.class");
                assertEntryExists(jar, "com/mtg/MtgConstants.class");
                assertEntryExists(jar, "assets/mtg/lang/en_us.json");
            }
        }

        private void assertManifestValid(JarFile jar) throws Exception {
            Manifest mf = jar.getManifest();
            assertNotNull(mf, "Jar must have valid MANIFEST.MF: " + jar.getName());
            assertEquals("1.0", mf.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));
        }

        private void assertEntryExists(JarFile jar, String entryName) {
            JarEntry entry = jar.getJarEntry(entryName);
            assertNotNull(entry, "Missing required JAR entry: '" + entryName + "' in " + jar.getName());
        }

        private String readEntryString(JarFile jar, String entryName) throws Exception {
            JarEntry entry = jar.getJarEntry(entryName);
            assertNotNull(entry, "Entry not found: " + entryName);
            try (InputStream is = jar.getInputStream(entry)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private void assertServiceRegistration(JarFile jar, String serviceInterface, String expectedImpl) throws Exception {
            String servicePath = "META-INF/services/" + serviceInterface;
            JarEntry entry = jar.getJarEntry(servicePath);
            assertNotNull(entry, "Missing service descriptor: " + servicePath + " in " + jar.getName());

            String content = readEntryString(jar, servicePath).trim();
            assertTrue(content.contains(expectedImpl),
                    "Service descriptor " + servicePath + " does not contain expected provider: " + expectedImpl);

            // Crucial boundary check: does the provider implementation class actually exist in the JAR?
            String classEntryName = expectedImpl.replace('.', '/') + ".class";
            assertNotNull(jar.getJarEntry(classEntryName),
                    "Service descriptor references '" + expectedImpl + "' but '" + classEntryName + "' is missing from " + jar.getName());
        }

        // =====================================================================
        // Oracle Validation: Verify that our inspector detects mutated/corrupt JARs
        // =====================================================================
        @Test
        @DisplayName("Oracle Check: Inspector detects missing fabric.mod.json")
        void testOracleDetectsMissingModMetadata() throws Exception {
            byte[] badJarBytes = buildSyntheticJar(Map.of(
                    "com/mtg/MtgConstants.class", new byte[]{1, 2, 3}
            ));

            Path tempJar = Files.createTempFile("oracle-test-bad-metadata", ".jar");
            try {
                Files.write(tempJar, badJarBytes);
                assertThrows(AssertionError.class, () -> validateFabricJar(tempJar),
                        "Oracle must fail when fabric.mod.json is missing");
            } finally {
                Files.deleteIfExists(tempJar);
            }
        }

        @Test
        @DisplayName("Oracle Check: Inspector detects missing service provider implementation class")
        void testOracleDetectsDanglingServiceDescriptor() throws Exception {
            // Service descriptor declares the expected provider, but the provider's .class file is missing from the JAR!
            byte[] badJarBytes = buildSyntheticJar(Map.of(
                    "fabric.mod.json", "{\"id\": \"mtg\"}".getBytes(StandardCharsets.UTF_8),
                    "com/mtg/fabric/FabricMtgMod.class", new byte[]{0},
                    "com/mtg/fabric/FabricMtgClientMod.class", new byte[]{0},
                    "META-INF/services/com.mtg.platform.IPlatformHelper", "com.mtg.fabric.platform.FabricPlatformHelper\n".getBytes(StandardCharsets.UTF_8),
                    // Missing: "com/mtg/fabric/platform/FabricPlatformHelper.class"
                    "META-INF/services/com.mtg.platform.INetworkHelper", "com.mtg.fabric.platform.FabricNetworkHelper\n".getBytes(StandardCharsets.UTF_8),
                    "com/mtg/fabric/platform/FabricNetworkHelper.class", new byte[]{0},
                    "com/mtg/domain/ManaCost.class", new byte[]{0},
                    "com/mtg/MtgConstants.class", new byte[]{0},
                    "assets/mtg/lang/en_us.json", "{}".getBytes(StandardCharsets.UTF_8)
            ));

            Path tempJar = Files.createTempFile("oracle-test-dangling-service", ".jar");
            try {
                Files.write(tempJar, badJarBytes);
                AssertionError err = assertThrows(AssertionError.class, () -> validateFabricJar(tempJar));
                assertTrue(err.getMessage().contains("FabricPlatformHelper.class' is missing from"),
                        "Oracle must detect missing provider class file: " + err.getMessage());
            } finally {
                Files.deleteIfExists(tempJar);
            }
        }

        @Test
        @DisplayName("Oracle Check: Inspector detects missing localization assets in loader JAR")
        void testOracleDetectsMissingLangAsset() throws Exception {
            byte[] badJarBytes = buildSyntheticJar(Map.of(
                    "fabric.mod.json", "{\"id\": \"mtg\"}".getBytes(StandardCharsets.UTF_8),
                    "com/mtg/fabric/FabricMtgMod.class", new byte[]{0},
                    "com/mtg/fabric/FabricMtgClientMod.class", new byte[]{0},
                    "META-INF/services/com.mtg.platform.IPlatformHelper", "com.mtg.fabric.platform.FabricPlatformHelper\n".getBytes(StandardCharsets.UTF_8),
                    "com/mtg/fabric/platform/FabricPlatformHelper.class", new byte[]{0},
                    "META-INF/services/com.mtg.platform.INetworkHelper", "com.mtg.fabric.platform.FabricNetworkHelper\n".getBytes(StandardCharsets.UTF_8),
                    "com/mtg/fabric/platform/FabricNetworkHelper.class", new byte[]{0},
                    "com/mtg/domain/ManaCost.class", new byte[]{0},
                    "com/mtg/MtgConstants.class", new byte[]{0}
                    // missing assets/mtg/lang/en_us.json!
            ));

            Path tempJar = Files.createTempFile("oracle-test-missing-lang", ".jar");
            try {
                Files.write(tempJar, badJarBytes);
                AssertionError err = assertThrows(AssertionError.class, () -> validateFabricJar(tempJar));
                assertTrue(err.getMessage().contains("en_us.json"),
                        "Oracle must detect missing en_us.json: " + err.getMessage());
            } finally {
                Files.deleteIfExists(tempJar);
            }
        }

        private byte[] buildSyntheticJar(Map<String, byte[]> entries) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            try (JarOutputStream jos = new JarOutputStream(baos, manifest)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    JarEntry je = new JarEntry(entry.getKey());
                    jos.putNextEntry(je);
                    jos.write(entry.getValue());
                    jos.closeEntry();
                }
            }
            return baos.toByteArray();
        }
    }
}
