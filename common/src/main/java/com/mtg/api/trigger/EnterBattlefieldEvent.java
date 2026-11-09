package com.mtg.api.trigger;

import com.mtg.domain.CardDefinition;
import java.util.UUID;

public class EnterBattlefieldEvent extends TriggerEvent {
    private final UUID ownerId;
    private final CardDefinition card;
    private final int x;
    private final int y;
    private final int z;

    public EnterBattlefieldEvent(Object source, UUID ownerId, CardDefinition card, int x, int y, int z) {
        super(source);
        this.ownerId = ownerId;
        this.card = card;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getOwnerId() { return ownerId; }
    public CardDefinition getCard() { return card; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
