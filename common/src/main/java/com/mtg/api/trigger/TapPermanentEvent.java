package com.mtg.api.trigger;

import java.util.UUID;

public class TapPermanentEvent extends TriggerEvent {
    private final UUID ownerId;
    private final Object permanent;

    public TapPermanentEvent(Object source, UUID ownerId, Object permanent) {
        super(source);
        this.ownerId = ownerId;
        this.permanent = permanent;
    }

    public UUID getOwnerId() { return ownerId; }
    public Object getPermanent() { return permanent; }
}
