package com.mtg.cards.casting;

import com.mtg.api.trigger.TriggerEvent;
import com.mtg.domain.CardDefinition;

import java.util.UUID;

/**
 * Fired when a persistent timed enchantment aura expires.
 */
public class EnchantmentExpiredEvent extends TriggerEvent {
    private final UUID ownerId;
    private final CardDefinition card;
    private final UUID enchantmentId;

    public EnchantmentExpiredEvent(Object source, UUID ownerId, CardDefinition card, UUID enchantmentId) {
        super(source);
        this.ownerId = ownerId;
        this.card = card;
        this.enchantmentId = enchantmentId;
    }

    public UUID getOwnerId() { return ownerId; }
    public CardDefinition getCard() { return card; }
    public UUID getEnchantmentId() { return enchantmentId; }
}
