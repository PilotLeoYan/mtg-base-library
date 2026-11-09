package com.mtg.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dedicated Server Safety and Headless Execution Tests")
public class DedicatedServerSafetyTest {

    @Test
    @DisplayName("Headless JVM detection operates cleanly without display hardware")
    void testHeadlessEnvironmentDetection() {
        boolean isHeadless = GraphicsEnvironment.isHeadless();
        String headlessProp = System.getProperty("java.awt.headless");
        assertTrue(isHeadless || "true".equalsIgnoreCase(headlessProp),
                "Headless flag or display absence should be detected");
    }

    @Test
    @DisplayName("IPlatformHelper environment contract consistency")
    void testPlatformEnvironmentContract() {
        IPlatformHelper platform = Services.PLATFORM;
        assertNotNull(platform);

        boolean isDedicatedServer = platform.isDedicatedServer();
        boolean isClient = platform.isClient();

        assertFalse(isDedicatedServer && isClient,
                "A runtime environment cannot be simultaneously a dedicated server and a client");
    }

    @Test
    @DisplayName("Zero-Client-Import Rule: No client classes imported in common server packages")
    void testZeroClientImportsInServerPackages() throws Exception {
        List<Class<?>> nonClientClasses = scanPackageClasses("com.mtg");
        List<String> violations = new ArrayList<>();

        for (Class<?> clazz : nonClientClasses) {
            String className = clazz.getName();
            if (className.startsWith("com.mtg.client.")) {
                continue;
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (isForbiddenClientType(field.getType().getName())) {
                    violations.add(className + " has client-only field: " + field.getName() + " of type " + field.getType().getName());
                }
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (isForbiddenClientType(method.getReturnType().getName())) {
                    violations.add(className + " has client-only return type in method: " + method.getName());
                }
                for (Class<?> paramType : method.getParameterTypes()) {
                    if (isForbiddenClientType(paramType.getName())) {
                        violations.add(className + " has client-only parameter in method: " + method.getName() + " (" + paramType.getName() + ")");
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Dedicated server safety violation! Client classes referenced in common/server paths:\n" +
                String.join("\n", violations));
    }

    private boolean isForbiddenClientType(String typeName) {
        return typeName.startsWith("net.minecraft.client.")
                || typeName.startsWith("com.mojang.blaze3d.")
                || typeName.contains(".client.gui.")
                || typeName.endsWith(".Screen");
    }

    private List<Class<?>> scanPackageClasses(String packageName) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            if (directory.exists() && directory.isDirectory()) {
                scanDir(directory, packageName, classes);
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
                    Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
                    classes.add(clazz);
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
