package com.mtg.config.toml;

import java.util.*;

public class TomlDocument {
    private final Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
    private final Map<String, List<String>> sectionComments = new LinkedHashMap<>();

    public Set<String> getSections() {
        return Collections.unmodifiableSet(sections.keySet());
    }

    public Map<String, Object> getSection(String section) {
        return sections.getOrDefault(section, Collections.emptyMap());
    }

    public boolean hasSection(String section) {
        return sections.containsKey(section);
    }

    public void addSectionComment(String section, String comment) {
        sectionComments.computeIfAbsent(section, k -> new ArrayList<>()).add(comment);
    }

    public List<String> getSectionComments(String section) {
        return sectionComments.getOrDefault(section, Collections.emptyList());
    }

    public void set(String section, String key, Object value) {
        sections.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, value);
    }

    public Object get(String section, String key) {
        Map<String, Object> sec = sections.get(section);
        return sec != null ? sec.get(key) : null;
    }

    public String getString(String section, String key, String defaultValue) {
        Object val = get(section, key);
        return val != null ? val.toString() : defaultValue;
    }

    public int getInt(String section, String key, int defaultValue) {
        Object val = get(section, key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public double getDouble(String section, String key, double defaultValue) {
        Object val = get(section, key);
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean getBoolean(String section, String key, boolean defaultValue) {
        Object val = get(section, key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return defaultValue;
    }

    public Map<String, Map<String, Object>> getAllData() {
        return sections;
    }
}
