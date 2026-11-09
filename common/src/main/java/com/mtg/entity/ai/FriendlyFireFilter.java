package com.mtg.entity.ai;

import com.mtg.entity.ISummonedCreature;

import java.util.UUID;

/**
 * Filter preventing friendly fire attacks against the summoner or friendly summons.
 */
public final class FriendlyFireFilter {
    private FriendlyFireFilter() {}

    public static boolean canAttack(ISummonedCreature attacker, UUID targetEntityId, ISummonedCreature targetSummon) {
        if (attacker == null || targetEntityId == null) {
            return false;
        }
        // Invariant: Never attack owner player
        if (targetEntityId.equals(attacker.getOwnerUuid())) {
            return false;
        }
        // Invariant: Never attack friendly summons of same owner
        if (targetSummon != null && attacker.getOwnerUuid().equals(targetSummon.getOwnerUuid())) {
            return false;
        }
        return true;
    }
}
