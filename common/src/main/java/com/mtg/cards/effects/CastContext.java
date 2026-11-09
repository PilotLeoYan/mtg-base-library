package com.mtg.cards.effects;

import com.mtg.cards.casting.CastTarget;
import com.mtg.domain.CardDefinition;

import java.util.Objects;
import java.util.UUID;

/**
 * Contextual information provided when executing a card effect.
 */
public record CastContext(
    UUID casterId,
    CardDefinition card,
    int slotIndex,
    int targetEntityId,
    double targetX,
    double targetY,
    double targetZ,
    boolean kicked,
    int xValue,
    long gameTick,
    boolean isServerSide
) {
    public CastContext {
        Objects.requireNonNull(casterId, "casterId cannot be null");
        Objects.requireNonNull(card, "card cannot be null");
    }

    public static CastContext of(UUID casterId, CardDefinition card, int slotIndex) {
        return new CastContext(casterId, card, slotIndex, -1, 0.0, 0.0, 0.0, false, 0, System.currentTimeMillis(), true);
    }

    public static CastContext ofTarget(UUID casterId, CardDefinition card, int slotIndex, CastTarget target) {
        Objects.requireNonNull(target, "CastTarget cannot be null");
        return new CastContext(
            casterId,
            card,
            slotIndex,
            target.targetEntityId(),
            target.targetX(),
            target.targetY(),
            target.targetZ(),
            false,
            0,
            System.currentTimeMillis(),
            true
        );
    }
}
