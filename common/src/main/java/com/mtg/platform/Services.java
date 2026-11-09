package com.mtg.platform;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public final class Services {
    private static final Map<Class<?>, Object> SERVICE_CACHE = new ConcurrentHashMap<>();

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    private Services() {}

    @SuppressWarnings("unchecked")
    public static <T> T load(Class<T> clazz) {
        return (T) SERVICE_CACHE.computeIfAbsent(clazz, Services::discoverService);
    }

    private static <T> T discoverService(Class<T> clazz) {
        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        if (contextCl != null) {
            var loader = ServiceLoader.load(clazz, contextCl).findFirst();
            if (loader.isPresent()) {
                return loader.get();
            }
        }

        return ServiceLoader.load(clazz, clazz.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Unable to locate service provider for interface: " + clazz.getName() +
                ". Ensure META-INF/services/" + clazz.getName() + " is present on the classpath."
            ));
    }
}
