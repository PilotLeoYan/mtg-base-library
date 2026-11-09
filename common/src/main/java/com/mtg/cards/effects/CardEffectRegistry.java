package com.mtg.cards.effects;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry mapping card IDs to their executable ICardEffectHandler implementations.
 */
public final class CardEffectRegistry {
    private static final Map<String, ICardEffectHandler> HANDLERS = new ConcurrentHashMap<>();

    private CardEffectRegistry() {}

    public static void register(String cardId, ICardEffectHandler handler) {
        Objects.requireNonNull(cardId, "cardId cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        HANDLERS.put(cardId, handler);
    }

    public static Optional<ICardEffectHandler> get(String cardId) {
        if (cardId == null) return Optional.empty();
        return Optional.ofNullable(HANDLERS.get(cardId));
    }

    public static boolean hasHandler(String cardId) {
        return cardId != null && HANDLERS.containsKey(cardId);
    }

    public static void clear() {
        HANDLERS.clear();
    }
}
