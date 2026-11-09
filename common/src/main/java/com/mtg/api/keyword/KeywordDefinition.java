package com.mtg.api.keyword;

import java.util.Objects;

public record KeywordDefinition(
    String id,
    String name,
    String reminderText,
    KeywordCategory category,
    boolean hasParameter
) {
    public KeywordDefinition {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
    }

    public static KeywordDefinition of(String id, String name, String reminderText, KeywordCategory category) {
        return new KeywordDefinition(id, name, reminderText, category, false);
    }

    public static KeywordDefinition parameterized(String id, String name, String reminderText, KeywordCategory category) {
        return new KeywordDefinition(id, name, reminderText, category, true);
    }
}
