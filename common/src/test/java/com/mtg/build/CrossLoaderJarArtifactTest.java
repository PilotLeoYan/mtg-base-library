package com.mtg.build;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cross-Loader Build JAR Artifact Verification")
public class CrossLoaderJarArtifactTest {

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
                    .filter(p -> p.toString().endsWith(".jar") && !p.toString().endsWith("-sources.jar") && !p.toString().endsWith("-dev.jar"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    static boolean hasBuiltJars() {
        return resolveJarPath("common") != null
                && resolveJarPath("fabric") != null
                && resolveJarPath("neoforge") != null
                && resolveJarPath("forge") != null;
    }

    @Test
    @DisplayName("Verify Common JAR artifact integrity and contents")
    void testCommonJarArtifact() throws Exception {
        Path jarPath = resolveJarPath("common");
        assertNotNull(jarPath, "mtg-common.jar must exist after build (ensure jar task ran)");
        assertTrue(Files.size(jarPath) > 500, "mtg-common.jar size must be > 500 bytes");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            assertNotNull(jar.getManifest(), "mtg-common.jar must have a valid MANIFEST.MF");
            assertEquals("1.0", jar.getManifest().getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));

            assertNotNull(jar.getEntry("com/mtg/MtgConstants.class"), "mtg-common.jar must contain MtgConstants.class");
            assertNotNull(jar.getEntry("com/mtg/domain/ManaCost.class"), "mtg-common.jar must contain ManaCost.class");
            assertNotNull(jar.getEntry("com/mtg/platform/IPlatformHelper.class"), "mtg-common.jar must contain IPlatformHelper.class");
        }
    }

    @Test
    @DisplayName("Verify Fabric JAR artifact integrity and fabric.mod.json")
    void testFabricJarArtifact() throws Exception {
        Path jarPath = resolveJarPath("fabric");
        assertNotNull(jarPath, "mtg-fabric.jar must exist after build");
        assertTrue(Files.size(jarPath) > 1000, "mtg-fabric.jar size must be > 1 KB");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry modJsonEntry = jar.getJarEntry("fabric.mod.json");
            assertNotNull(modJsonEntry, "mtg-fabric.jar must contain fabric.mod.json");

            try (InputStream is = jar.getInputStream(modJsonEntry)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("\"id\": \"mtg\""), "fabric.mod.json must declare id: 'mtg'");
                assertTrue(content.contains("FabricMtgMod"), "fabric.mod.json must declare FabricMtgMod entrypoint");
            }

            JarEntry serviceEntry = jar.getJarEntry("META-INF/services/com.mtg.platform.IPlatformHelper");
            assertNotNull(serviceEntry, "mtg-fabric.jar must contain IPlatformHelper service declaration");

            assertNotNull(jar.getEntry("com/mtg/fabric/FabricMtgMod.class"), "mtg-fabric.jar must contain FabricMtgMod.class");
            assertNotNull(jar.getEntry("com/mtg/domain/ManaCost.class"), "mtg-fabric.jar must bundle common domain classes");
            assertNotNull(jar.getEntry("com/mtg/MtgConstants.class"), "mtg-fabric.jar must bundle MtgConstants.class");
        }
    }

    @Test
    @DisplayName("Verify NeoForge JAR artifact integrity and neoforge.mods.toml")
    void testNeoForgeJarArtifact() throws Exception {
        Path jarPath = resolveJarPath("neoforge");
        assertNotNull(jarPath, "mtg-neoforge.jar must exist after build");
        assertTrue(Files.size(jarPath) > 1000, "mtg-neoforge.jar size must be > 1 KB");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry tomlEntry = jar.getJarEntry("META-INF/neoforge.mods.toml");
            assertNotNull(tomlEntry, "mtg-neoforge.jar must contain META-INF/neoforge.mods.toml");

            try (InputStream is = jar.getInputStream(tomlEntry)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("modId = \"mtg\"") || content.contains("modId=\"mtg\""),
                        "neoforge.mods.toml must declare modId = 'mtg'");
            }

            JarEntry serviceEntry = jar.getJarEntry("META-INF/services/com.mtg.platform.IPlatformHelper");
            assertNotNull(serviceEntry, "mtg-neoforge.jar must contain IPlatformHelper service declaration");

            assertNotNull(jar.getEntry("com/mtg/neoforge/NeoForgeMtgMod.class"), "mtg-neoforge.jar must contain NeoForgeMtgMod.class");
            assertNotNull(jar.getEntry("com/mtg/domain/ManaCost.class"), "mtg-neoforge.jar must bundle common domain classes");
            assertNotNull(jar.getEntry("com/mtg/MtgConstants.class"), "mtg-neoforge.jar must bundle MtgConstants.class");
        }
    }

    @Test
    @DisplayName("Verify Forge JAR artifact integrity and mods.toml")
    void testForgeJarArtifact() throws Exception {
        Path jarPath = resolveJarPath("forge");
        assertNotNull(jarPath, "mtg-forge.jar must exist after build");
        assertTrue(Files.size(jarPath) > 1000, "mtg-forge.jar size must be > 1 KB");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry tomlEntry = jar.getJarEntry("META-INF/mods.toml");
            assertNotNull(tomlEntry, "mtg-forge.jar must contain META-INF/mods.toml");

            try (InputStream is = jar.getInputStream(tomlEntry)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("modId = \"mtg\"") || content.contains("modId=\"mtg\""),
                        "mods.toml must declare modId = 'mtg'");
            }

            JarEntry serviceEntry = jar.getJarEntry("META-INF/services/com.mtg.platform.IPlatformHelper");
            assertNotNull(serviceEntry, "mtg-forge.jar must contain IPlatformHelper service declaration");

            assertNotNull(jar.getEntry("com/mtg/forge/ForgeMtgMod.class"), "mtg-forge.jar must contain ForgeMtgMod.class");
            assertNotNull(jar.getEntry("com/mtg/domain/ManaCost.class"), "mtg-forge.jar must bundle common domain classes");
            assertNotNull(jar.getEntry("com/mtg/MtgConstants.class"), "mtg-forge.jar must bundle MtgConstants.class");
        }
    }
}
