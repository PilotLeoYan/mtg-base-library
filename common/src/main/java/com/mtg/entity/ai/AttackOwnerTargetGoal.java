package com.mtg.entity.ai;

import com.mtg.entity.ISummonedCreature;

import java.util.UUID;

/**
 * AI goal assisting the summoner by attacking the entity currently targeted/attacked by the owner.
 */
public class AttackOwnerTargetGoal implements ISummonAiGoal {

    @Override
    public boolean canExecute(ISummonedCreature creature, AiContext context) {
        if (creature == null || !creature.canAttack() || context == null || context.ownerTargetId() == null) {
            return false;
        }
        return FriendlyFireFilter.canAttack(creature, context.ownerTargetId(), context.ownerTargetSummon());
    }

    @Override
    public void tick(ISummonedCreature creature, AiContext context) {
        if (creature == null || context == null) return;
        UUID target = context.ownerTargetId();
        if (target != null && FriendlyFireFilter.canAttack(creature, target, context.ownerTargetSummon())) {
            creature.setTargetEntityId(target);
            creature.setAiState(SummonAiState.ATTACKING);
        }
    }
}
