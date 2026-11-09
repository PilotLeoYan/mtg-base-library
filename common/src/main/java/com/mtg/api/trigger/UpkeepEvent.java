package com.mtg.api.trigger;

import java.util.UUID;

public class UpkeepEvent extends TriggerEvent {
    private final UUID playerId;
    private final long tickNumber;

    public UpkeepEvent(Object source, UUID playerId, long tickNumber) {
        super(source);
        this.playerId = playerId;
        this.tickNumber = tickNumber;
    }

    public UUID getPlayerId() { return playerId; }
    public long getTickNumber() { return tickNumber; }
}
