package com.mtg.world.events;

import com.mtg.api.trigger.TriggerEvent;
import com.mtg.domain.ManaColor;
import com.mtg.world.altar.AltarRitualPhase;
import com.mtg.world.altar.AltarRitualType;

import java.util.Objects;
import java.util.UUID;

/**
 * TriggerBus event posted during altar ritual state transitions (offering placed, channeling, completion).
 */
public class AltarRitualEvent extends TriggerEvent {
    private final UUID playerId;
    private final AltarRitualType ritualType;
    private final ManaColor manaColor;
    private final AltarRitualPhase phase;

    public AltarRitualEvent(Object source, UUID playerId, AltarRitualType ritualType, ManaColor manaColor, AltarRitualPhase phase) {
        super(source);
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.ritualType = Objects.requireNonNull(ritualType, "RitualType cannot be null");
        this.manaColor = manaColor; // May be null for Conflux (multi-color)
        this.phase = Objects.requireNonNull(phase, "Phase cannot be null");
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public AltarRitualType getRitualType() {
        return ritualType;
    }

    public ManaColor getManaColor() {
        return manaColor;
    }

    public AltarRitualPhase getPhase() {
        return phase;
    }
}
