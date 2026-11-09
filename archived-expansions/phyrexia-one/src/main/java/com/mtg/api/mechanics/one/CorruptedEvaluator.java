package com.mtg.api.mechanics.one;

import java.util.Collection;
import java.util.UUID;

public final class CorruptedEvaluator {
    public static final int CORRUPTED_THRESHOLD = 3;

    private CorruptedEvaluator() {}

    public static boolean isCorrupted(int poisonCount) {
        return poisonCount >= CORRUPTED_THRESHOLD;
    }

    public static boolean isCorrupted(UUID targetId, PoisonCounterManager manager) {
        if (targetId == null || manager == null) return false;
        return manager.getPoison(targetId) >= CORRUPTED_THRESHOLD;
    }

    public static boolean hasAnyCorruptedOpponent(Collection<UUID> opponents, PoisonCounterManager manager) {
        if (opponents == null || manager == null) return false;
        return opponents.stream().anyMatch(uuid -> isCorrupted(uuid, manager));
    }
}
