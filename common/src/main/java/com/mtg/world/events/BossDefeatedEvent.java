package com.mtg.world.events;

import com.mtg.api.trigger.TriggerEvent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * TriggerBus event posted when a World Boss is defeated.
 */
public class BossDefeatedEvent extends TriggerEvent {
    private final UUID bossEntityId;
    private final String bossType;
    private final UUID killerPlayerId;
    private final List<UUID> participants;

    public BossDefeatedEvent(Object source, UUID bossEntityId, String bossType, UUID killerPlayerId, List<UUID> participants) {
        super(source);
        this.bossEntityId = Objects.requireNonNull(bossEntityId, "Boss entity ID cannot be null");
        this.bossType = Objects.requireNonNull(bossType, "Boss type cannot be null");
        this.killerPlayerId = killerPlayerId;
        this.participants = participants != null ? List.copyOf(participants) : Collections.emptyList();
    }

    public UUID getBossEntityId() {
        return bossEntityId;
    }

    public String getBossType() {
        return bossType;
    }

    public UUID getKillerPlayerId() {
        return killerPlayerId;
    }

    public List<UUID> getParticipants() {
        return participants;
    }
}
