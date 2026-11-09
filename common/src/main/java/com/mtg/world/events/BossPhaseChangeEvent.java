package com.mtg.world.events;

import com.mtg.api.trigger.TriggerEvent;
import com.mtg.world.boss.BossPhase;

import java.util.Objects;
import java.util.UUID;

/**
 * TriggerBus event posted when a World Boss transitions between combat phases.
 */
public class BossPhaseChangeEvent extends TriggerEvent {
    private final UUID bossEntityId;
    private final String bossType;
    private final BossPhase oldPhase;
    private final BossPhase newPhase;

    public BossPhaseChangeEvent(Object source, UUID bossEntityId, String bossType, BossPhase oldPhase, BossPhase newPhase) {
        super(source);
        this.bossEntityId = Objects.requireNonNull(bossEntityId, "Boss entity ID cannot be null");
        this.bossType = Objects.requireNonNull(bossType, "Boss type cannot be null");
        this.oldPhase = Objects.requireNonNull(oldPhase, "Old phase cannot be null");
        this.newPhase = Objects.requireNonNull(newPhase, "New phase cannot be null");
    }

    public UUID getBossEntityId() {
        return bossEntityId;
    }

    public String getBossType() {
        return bossType;
    }

    public BossPhase getOldPhase() {
        return oldPhase;
    }

    public BossPhase getNewPhase() {
        return newPhase;
    }
}
