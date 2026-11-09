package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;

import java.util.UUID;

/**
 * Common domain contract for MTG World Bosses and Minibosses.
 * Pure server-safe interface decoupled from Minecraft entity internals.
 */
public interface IBossEntity {
    UUID getId();

    String getBossType();

    float getCurrentHealth();

    float getMaxHealth();

    BossPhase getPhase();

    boolean isAlive();

    BossThreatTable getThreatTable();

    void takeDamage(UUID attackerId, float amount);

    void heal(float amount);

    void tick();

    double getX();

    double getY();

    double getZ();

    void setPosition(double x, double y, double z);

    void setTriggerBus(TriggerBus triggerBus);

    TriggerBus getTriggerBus();
}
