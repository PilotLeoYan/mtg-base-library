package com.mtg.world.node;

import com.mtg.domain.ManaColor;

import java.util.Objects;

/**
 * Immutable definition of an Elemental Mana Node.
 */
public record ManaNodeDefinition(
    ManaNodeType type,
    ManaColor color,
    int maxCharges,
    int rechargeIntervalTicks,
    int pulseAmount
) {
    public static final int DEFAULT_MAX_CHARGES = 5;
    public static final int DEFAULT_RECHARGE_INTERVAL_TICKS = 200;
    public static final int DEFAULT_PULSE_AMOUNT = 3;

    public ManaNodeDefinition {
        Objects.requireNonNull(type, "ManaNodeType cannot be null");
        Objects.requireNonNull(color, "ManaColor cannot be null");
    }

    public static ManaNodeDefinition of(ManaNodeType type) {
        return new ManaNodeDefinition(type, type.getManaColor(), DEFAULT_MAX_CHARGES, DEFAULT_RECHARGE_INTERVAL_TICKS, DEFAULT_PULSE_AMOUNT);
    }

    public static ManaNodeDefinition of(ManaNodeType type, int pulseAmount) {
        return new ManaNodeDefinition(type, type.getManaColor(), DEFAULT_MAX_CHARGES, DEFAULT_RECHARGE_INTERVAL_TICKS, pulseAmount);
    }
}
