package com.mtg.api.mechanics.one;

import com.mtg.api.trigger.CombatDamageEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.api.trigger.TriggerListener;
import com.mtg.domain.CardDefinition;
import java.util.UUID;

public class ToxicCombatDamageListener implements TriggerListener<CombatDamageEvent> {
    private final PoisonCounterManager poisonManager;
    private final TriggerBus triggerBus;

    public ToxicCombatDamageListener(PoisonCounterManager poisonManager, TriggerBus triggerBus) {
        this.poisonManager = poisonManager;
        this.triggerBus = triggerBus;
    }

    @Override
    public void onTrigger(CombatDamageEvent event) {
        if (event.isCanceled() || event.getDamageAmount() <= 0) {
            return;
        }

        int toxicValue = 0;
        if (event.getAttacker() instanceof IToxic toxic) {
            toxicValue = toxic.getToxicValue();
        } else if (event.getAttacker() instanceof CardDefinition cardDef) {
            toxicValue = cardDef.getKeywordValue("mtg:toxic");
        }

        if (toxicValue > 0 && event.getDefenderId() != null) {
            UUID attackerId = event.getAttackerId();
            poisonManager.addPoison(event.getDefenderId(), toxicValue, attackerId, triggerBus);
        }
    }
}
