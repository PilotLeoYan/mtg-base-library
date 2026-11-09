package com.mtg.world.events;

import com.mtg.api.trigger.TriggerEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * TriggerBus event posted when a World Boss is spawned or summoned into the world.
 */
public class BossSpawnEvent extends TriggerEvent {
    private final UUID bossEntityId;
    private final String bossType;
    private final double x;
    private final double y;
    private final double z;

    public BossSpawnEvent(Object source, UUID bossEntityId, String bossType, double x, double y, double z) {
        super(source);
        this.bossEntityId = Objects.requireNonNull(bossEntityId, "Boss entity ID cannot be null");
        this.bossType = Objects.requireNonNull(bossType, "Boss type cannot be null");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getBossEntityId() {
        return bossEntityId;
    }

    public String getBossType() {
        return bossType;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
