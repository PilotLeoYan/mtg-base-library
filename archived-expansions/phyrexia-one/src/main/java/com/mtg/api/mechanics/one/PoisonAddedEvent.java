package com.mtg.api.mechanics.one;

import com.mtg.api.trigger.TriggerEvent;
import java.util.UUID;

public class PoisonAddedEvent extends TriggerEvent {
    private final UUID targetId;
    private final UUID sourceId;
    private final int amountAdded;
    private final int totalPoison;

    public PoisonAddedEvent(Object source, UUID targetId, UUID sourceId, int amountAdded, int totalPoison) {
        super(source);
        this.targetId = targetId;
        this.sourceId = sourceId;
        this.amountAdded = amountAdded;
        this.totalPoison = totalPoison;
    }

    public UUID getTargetId() { return targetId; }
    public UUID getSourceId() { return sourceId; }
    public int getAmountAdded() { return amountAdded; }
    public int getTotalPoison() { return totalPoison; }
}
