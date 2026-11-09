package com.mtg.world.events;

import com.mtg.api.trigger.TriggerEvent;
import com.mtg.domain.ManaColor;
import com.mtg.world.node.ManaNodeType;

import java.util.Objects;
import java.util.UUID;

/**
 * TriggerBus event posted when a player attunes to or channels an Elemental Mana Node.
 */
public class ManaNodeAttunedEvent extends TriggerEvent {
    private final UUID playerId;
    private final ManaNodeType nodeType;
    private final ManaColor manaColor;
    private final int amount;

    public ManaNodeAttunedEvent(Object source, UUID playerId, ManaNodeType nodeType, ManaColor manaColor, int amount) {
        super(source);
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.nodeType = Objects.requireNonNull(nodeType, "ManaNodeType cannot be null");
        this.manaColor = Objects.requireNonNull(manaColor, "ManaColor cannot be null");
        this.amount = amount;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ManaNodeType getNodeType() {
        return nodeType;
    }

    public ManaColor getManaColor() {
        return manaColor;
    }

    public int getAmount() {
        return amount;
    }
}
