package com.mtg.entity;

import com.mtg.api.trigger.TapPermanentEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;

import java.util.Objects;
import java.util.UUID;

/**
 * Tap mana ability for creature permanents, such as Llanowar Elves ({T}: Add {G}).
 */
public class TappableManaAbility implements ITappablePermanent {
    private final ISummonedCreature creature;
    private final ManaColor manaColor;
    private final int amount;
    private final TriggerBus triggerBus;

    public TappableManaAbility(ISummonedCreature creature, ManaColor manaColor, int amount, TriggerBus triggerBus) {
        this.creature = Objects.requireNonNull(creature, "creature cannot be null");
        this.manaColor = Objects.requireNonNull(manaColor, "manaColor cannot be null");
        this.amount = Math.max(1, amount);
        this.triggerBus = triggerBus;
    }

    public TappableManaAbility(ISummonedCreature creature, ManaColor manaColor, int amount) {
        this(creature, manaColor, amount, null);
    }

    public ISummonedCreature getCreature() {
        return creature;
    }

    public ManaColor getManaColor() {
        return manaColor;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public boolean canActivate(UUID playerId) {
        if (playerId == null || !creature.isOwner(playerId)) {
            return false;
        }
        return !creature.isDead() && !creature.isTapped() && !creature.hasSummoningSickness();
    }

    @Override
    public boolean activate(UUID playerId) {
        synchronized (creature) {
            if (!canActivate(playerId)) {
                return false;
            }
            if (!creature.tryTap()) {
                return false;
            }
            ManaManager.getInstance().addMana(creature.getOwnerUuid(), manaColor, amount);
            if (triggerBus != null) {
                triggerBus.post(new TapPermanentEvent(this, creature.getOwnerUuid(), creature));
            }
            return true;
        }
    }
}
