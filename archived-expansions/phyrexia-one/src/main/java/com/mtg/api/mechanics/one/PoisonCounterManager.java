package com.mtg.api.mechanics.one;

import com.mtg.api.trigger.TriggerBus;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PoisonCounterManager {
    public static final int LETHAL_POISON_THRESHOLD = 10;
    private final Map<UUID, Integer> poisonCounters = new ConcurrentHashMap<>();

    public int getPoison(UUID entityId) {
        if (entityId == null) return 0;
        return poisonCounters.getOrDefault(entityId, 0);
    }

    public void setPoison(UUID entityId, int count) {
        if (entityId != null) {
            poisonCounters.put(entityId, Math.max(0, count));
        }
    }

    public int addPoison(UUID entityId, int amount, UUID sourceId, TriggerBus triggerBus) {
        if (entityId == null) return 0;
        if (amount <= 0) {
            return getPoison(entityId);
        }

        int newVal = poisonCounters.compute(entityId, (k, current) -> {
            long sum = (long) (current == null ? 0 : current) + amount;
            return (int) Math.min((long) Integer.MAX_VALUE, sum);
        });

        if (triggerBus != null) {
            triggerBus.post(new PoisonAddedEvent(this, entityId, sourceId, amount, newVal));
        }

        return newVal;
    }

    public int removePoison(UUID entityId, int amount) {
        if (entityId == null || amount <= 0) return getPoison(entityId);
        return poisonCounters.compute(entityId, (k, current) -> {
            if (current == null || current <= 0) return 0;
            return Math.max(0, current - amount);
        });
    }

    public void removePoison(UUID entityId) {
        reset(entityId);
    }

    public void clearPoison(UUID entityId) {
        reset(entityId);
    }

    public boolean isLethal(UUID entityId) {
        return getPoison(entityId) >= LETHAL_POISON_THRESHOLD;
    }

    public boolean isLethal(UUID entityId, int threshold, boolean enabled) {
        if (!enabled) return false;
        return getPoison(entityId) >= threshold;
    }

    public void reset(UUID entityId) {
        if (entityId != null) {
            poisonCounters.remove(entityId);
        }
    }

    public void clearAll() {
        poisonCounters.clear();
    }
}
