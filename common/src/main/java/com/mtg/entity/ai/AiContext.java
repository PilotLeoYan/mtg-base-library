package com.mtg.entity.ai;

import com.mtg.entity.ISummonedCreature;

import java.util.UUID;

/**
 * Contextual state representing the player summoner and surrounding combat environment.
 */
public record AiContext(
    UUID ownerId,
    double ownerX,
    double ownerY,
    double ownerZ,
    UUID ownerAttackerId,
    ISummonedCreature ownerAttackerSummon,
    UUID ownerTargetId,
    ISummonedCreature ownerTargetSummon
) {
    public static AiContext ofOwnerPos(UUID ownerId, double x, double y, double z) {
        return new AiContext(ownerId, x, y, z, null, null, null, null);
    }

    public static AiContext ofDefend(UUID ownerId, double x, double y, double z, UUID attackerId, ISummonedCreature attackerSummon) {
        return new AiContext(ownerId, x, y, z, attackerId, attackerSummon, null, null);
    }

    public static AiContext ofAttack(UUID ownerId, double x, double y, double z, UUID targetId, ISummonedCreature targetSummon) {
        return new AiContext(ownerId, x, y, z, null, null, targetId, targetSummon);
    }
}
