package com.mtg.api.trigger;

import com.mtg.domain.CardDefinition;
import java.util.UUID;

public class LandPlayedEvent extends TriggerEvent {
    private final UUID playerId;
    private final CardDefinition landCard;
    private final int x;
    private final int y;
    private final int z;

    public LandPlayedEvent(Object source, UUID playerId, CardDefinition landCard, int x, int y, int z) {
        super(source);
        this.playerId = playerId;
        this.landCard = landCard;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getPlayerId() { return playerId; }
    public CardDefinition getLandCard() { return landCard; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
