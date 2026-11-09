package com.mtg.cards.casting;

import com.mtg.api.trigger.TriggerEvent;
import com.mtg.domain.CardDefinition;

import java.util.UUID;

/**
 * Fired when an active channeling spell cast is interrupted, e.g. by incoming player damage.
 */
public class SpellCastInterruptedEvent extends TriggerEvent {
    private final UUID casterId;
    private final CardDefinition card;
    private final double damageAmount;
    private final String reason;

    public SpellCastInterruptedEvent(Object source, UUID casterId, CardDefinition card, double damageAmount, String reason) {
        super(source);
        this.casterId = casterId;
        this.card = card;
        this.damageAmount = damageAmount;
        this.reason = reason != null ? reason : "UNKNOWN";
    }

    public UUID getCasterId() { return casterId; }
    public CardDefinition getCard() { return card; }
    public double getDamageAmount() { return damageAmount; }
    public String getReason() { return reason; }
}
