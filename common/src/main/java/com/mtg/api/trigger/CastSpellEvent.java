package com.mtg.api.trigger;

import com.mtg.domain.CardDefinition;
import java.util.UUID;

public class CastSpellEvent extends TriggerEvent {
    private final UUID casterId;
    private final CardDefinition card;
    private final boolean kicked;
    private final int xValue;

    public CastSpellEvent(Object source, UUID casterId, CardDefinition card, boolean kicked, int xValue) {
        super(source);
        this.casterId = casterId;
        this.card = card;
        this.kicked = kicked;
        this.xValue = xValue;
    }

    public UUID getCasterId() { return casterId; }
    public CardDefinition getCard() { return card; }
    public boolean isKicked() { return kicked; }
    public int getXValue() { return xValue; }
}
