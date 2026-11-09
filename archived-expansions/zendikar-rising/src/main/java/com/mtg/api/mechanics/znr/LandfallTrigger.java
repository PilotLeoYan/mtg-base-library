package com.mtg.api.mechanics.znr;

import com.mtg.api.trigger.TriggerEvent;
import com.mtg.domain.CardDefinition;
import java.util.UUID;

public class LandfallTrigger extends TriggerEvent {
    private final UUID playerId;
    private final CardDefinition landCard;
    private final int x;
    private final int y;
    private final int z;
    private final boolean blockPlacement;

    public LandfallTrigger(Object source, UUID playerId, CardDefinition landCard, int x, int y, int z, boolean blockPlacement) {
        super(source);
        this.playerId = playerId;
        this.landCard = landCard;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockPlacement = blockPlacement;
    }

    public LandfallTrigger(Object source, UUID playerId, CardDefinition landCard, int x, int y, int z) {
        this(source, playerId, landCard, x, y, z, false);
    }

    public UUID getPlayerId() { return playerId; }
    public CardDefinition getLandCard() { return landCard; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public boolean isBlockPlacement() { return blockPlacement; }
}
