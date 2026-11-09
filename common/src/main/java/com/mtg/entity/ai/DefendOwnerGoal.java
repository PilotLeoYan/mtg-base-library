package com.mtg.entity.ai;

import com.mtg.entity.ISummonedCreature;

import java.util.UUID;

/**
 * AI goal triggering retaliation against entities that attack the summoner.
 */
public class DefendOwnerGoal implements ISummonAiGoal {

    @Override
    public boolean canExecute(ISummonedCreature creature, AiContext context) {
        if (creature == null || !creature.canAttack() || context == null || context.ownerAttackerId() == null) {
            return false;
        }
        return FriendlyFireFilter.canAttack(creature, context.ownerAttackerId(), context.ownerAttackerSummon());
    }

    @Override
    public void tick(ISummonedCreature creature, AiContext context) {
        if (creature == null || context == null) return;
        UUID attacker = context.ownerAttackerId();
        if (attacker != null && FriendlyFireFilter.canAttack(creature, attacker, context.ownerAttackerSummon())) {
            creature.setTargetEntityId(attacker);
            creature.setAiState(SummonAiState.DEFENDING);
        }
    }
}
