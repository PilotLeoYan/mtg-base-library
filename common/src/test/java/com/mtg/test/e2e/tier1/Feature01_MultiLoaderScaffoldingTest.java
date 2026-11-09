package com.mtg.test.e2e.tier1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 01: Multi-Loader Scaffolding & CLI Build E2E Test")
public class Feature01_MultiLoaderScaffoldingTest {

    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        return current != null ? current : Paths.get(".").toAbsolutePath();
    }

    private static Path resolveJarPath(String subproject) {
        Path buildLibs = findProjectRoot().resolve(subproject).resolve("build").resolve("libs");
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

    @Test
    @DisplayName("1. Settings.gradle configures all 4 subprojects (common, fabric, neoforge, forge)")
    void testSubprojectLayoutInSettingsGradle() throws Exception {
        Path settingsFile = findProjectRoot().resolve("settings.gradle");
        assertTrue(Files.exists(settingsFile), "settings.gradle must exist at project root");

        String content = Files.readString(settingsFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("'common'"), "settings.gradle must include 'common'");
        assertTrue(content.contains("'fabric'"), "settings.gradle must include 'fabric'");
        assertTrue(content.contains("'neoforge'"), "settings.gradle must include 'neoforge'");
        assertTrue(content.contains("'forge'"), "settings.gradle must include 'forge'");
    }

    @Test
    @DisplayName("2. Common JAR artifact contains compiled domain and platform classes")
    void testCommonJarArtifactIntegrity() throws Exception {
        Path jarPath = resolveJarPath("common");
        assertNotNull(jarPath, "mtg-common.jar must exist");
        assertTrue(Files.size(jarPath) > 500, "mtg-common.jar size must be greater than 500 bytes");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            assertNotNull(jar.getManifest(), "Common JAR must contain a valid MANIFEST.MF");
            assertEquals("1.0", jar.getManifest().getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));
            assertNotNull(jar.getEntry("com/mtg/MtgConstants.class"), "Must bundle MtgConstants");
            assertNotNull(jar.getEntry("com/mtg/domain/CardDefinition.class"), "Must bundle CardDefinition");
            assertNotNull(jar.getEntry("com/mtg/mana/ManaManager.class"), "Must bundle ManaManager");
        }
    }

    @Test
    @DisplayName("3. Fabric JAR contains fabric.mod.json metadata and entrypoint class")
    void testFabricModDescriptorMetadata() throws Exception {
        Path jarPath = resolveJarPath("fabric");
        assertNotNull(jarPath, "mtg-fabric.jar must exist");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var modJson = jar.getJarEntry("fabric.mod.json");
            assertNotNull(modJson, "Fabric JAR must bundle fabric.mod.json");

            try (InputStream is = jar.getInputStream(modJson)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("\"id\": \"mtg\""), "fabric.mod.json must declare modid 'mtg'");
                assertTrue(content.contains("FabricMtgMod"), "fabric.mod.json must declare FabricMtgMod");
            }
            assertNotNull(jar.getEntry("com/mtg/fabric/FabricMtgMod.class"), "Fabric JAR must contain FabricMtgMod.class");
        }
    }

    @Test
    @DisplayName("4. NeoForge JAR contains META-INF/neoforge.mods.toml descriptor")
    void testNeoForgeModDescriptorMetadata() throws Exception {
        Path jarPath = resolveJarPath("neoforge");
        assertNotNull(jarPath, "mtg-neoforge.jar must exist");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var modsToml = jar.getJarEntry("META-INF/neoforge.mods.toml");
            assertNotNull(modsToml, "NeoForge JAR must contain META-INF/neoforge.mods.toml");

            try (InputStream is = jar.getInputStream(modsToml)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("modId=\"mtg\"") || content.contains("modId = \"mtg\""), "neoforge.mods.toml must declare modId 'mtg'");
            }
            assertNotNull(jar.getEntry("com/mtg/neoforge/NeoForgeMtgMod.class"), "NeoForge JAR must contain NeoForgeMtgMod.class");
        }
    }

    @Test
    @DisplayName("5. Forge JAR contains META-INF/mods.toml descriptor")
    void testForgeModDescriptorMetadata() throws Exception {
        Path jarPath = resolveJarPath("forge");
        assertNotNull(jarPath, "mtg-forge.jar must exist");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var modsToml = jar.getJarEntry("META-INF/mods.toml");
            assertNotNull(modsToml, "Forge JAR must contain META-INF/mods.toml");

            try (InputStream is = jar.getInputStream(modsToml)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("modId=\"mtg\"") || content.contains("modId = \"mtg\""), "mods.toml must declare modId 'mtg'");
            }
            assertNotNull(jar.getEntry("com/mtg/forge/ForgeMtgMod.class"), "Forge JAR must contain ForgeMtgMod.class");
        }
    }
}
