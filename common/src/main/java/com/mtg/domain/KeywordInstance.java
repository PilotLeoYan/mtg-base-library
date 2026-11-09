package com.mtg.domain;

import com.mtg.api.keyword.KeywordDefinition;
import java.util.Objects;

public record KeywordInstance(KeywordDefinition definition, int parameterValue) {
    public KeywordInstance {
        Objects.requireNonNull(definition, "Keyword definition cannot be null");
    }

    public static KeywordInstance of(KeywordDefinition definition) {
        return new KeywordInstance(definition, 0);
    }

    public static KeywordInstance of(KeywordDefinition definition, int value) {
        return new KeywordInstance(definition, value);
    }

    public String getKeywordId() {
        return definition.id();
    }
}
