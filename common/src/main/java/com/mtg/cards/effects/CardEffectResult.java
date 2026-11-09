package com.mtg.cards.effects;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Outcome resulting from the execution of a card effect handler.
 */
public record CardEffectResult(
    boolean isSuccess,
    String messageKey,
    Map<String, Object> metadata
) {
    public CardEffectResult {
        messageKey = messageKey == null ? "" : messageKey;
        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public static CardEffectResult success() {
        return new CardEffectResult(true, "mtg.cast.success", Map.of());
    }

    public static CardEffectResult success(String messageKey) {
        return new CardEffectResult(true, messageKey, Map.of());
    }

    public static CardEffectResult success(String messageKey, Map<String, Object> metadata) {
        return new CardEffectResult(true, messageKey, metadata);
    }

    public static CardEffectResult failure(String messageKey) {
        return new CardEffectResult(false, messageKey, Map.of());
    }
}
