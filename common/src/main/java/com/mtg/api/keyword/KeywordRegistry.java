package com.mtg.api.keyword;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeywordRegistry {
    private final Map<String, KeywordDefinition> registry = new ConcurrentHashMap<>();

    public KeywordRegistry() {
        registerCoreKeywords();
    }

    public void register(KeywordDefinition keyword) {
        Objects.requireNonNull(keyword, "Keyword cannot be null");
        registry.put(keyword.id(), keyword);
    }

    public Optional<KeywordDefinition> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(registry.get(id));
    }

    public boolean has(String id) {
        if (id == null) return false;
        return registry.containsKey(id);
    }

    public Collection<KeywordDefinition> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public void clear() {
        registry.clear();
        registerCoreKeywords();
    }

    private void registerCoreKeywords() {
        register(CoreKeywords.FLYING);
        register(CoreKeywords.DEATHTOUCH);
        register(CoreKeywords.LIFELINK);
        register(CoreKeywords.FIRST_STRIKE);
        register(CoreKeywords.TRAMPLE);
        register(CoreKeywords.HASTE);
        register(CoreKeywords.VIGILANCE);
        register(CoreKeywords.HEXPROOF);
        register(CoreKeywords.REACH);
        register(CoreKeywords.DEFENDER);
    }
}
