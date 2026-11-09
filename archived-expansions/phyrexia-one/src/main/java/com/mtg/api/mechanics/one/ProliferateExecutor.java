package com.mtg.api.mechanics.one;

import com.mtg.api.trigger.TriggerBus;
import java.util.*;

public final class ProliferateExecutor {
    private ProliferateExecutor() {}

    /**
     * Proliferates chosen targets: adds one additional counter of each kind already present.
     */
    public static Map<UUID, Map<String, Integer>> proliferate(
        Collection<ICounterHolder> targets,
        PoisonCounterManager poisonManager,
        TriggerBus bus
    ) {
        Map<UUID, Map<String, Integer>> deltaReport = new HashMap<>();
        if (targets == null || targets.isEmpty()) {
            return deltaReport;
        }

        Set<UUID> visitedHolders = new HashSet<>();
        for (ICounterHolder target : targets) {
            if (target == null) {
                continue;
            }

            UUID holderId = target.getHolderId();
            if (holderId != null && !visitedHolders.add(holderId)) {
                continue;
            }

            Map<String, Integer> counters = target.getCounters();
            if (counters == null || counters.isEmpty()) {
                continue;
            }

            Map<String, Integer> added = new HashMap<>();
            for (Map.Entry<String, Integer> entry : counters.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String counterType = entry.getKey();
                Integer count = entry.getValue();
                if (counterType == null || count == null || count <= 0) {
                    continue;
                }

                target.addCounter(counterType, 1);
                added.put(counterType, 1);

                // If target has poison counters, also update poison manager
                if ("poison".equalsIgnoreCase(counterType) && poisonManager != null && holderId != null) {
                    poisonManager.addPoison(holderId, 1, null, bus);
                }
            }

            if (!added.isEmpty() && holderId != null) {
                deltaReport.put(holderId, added);
            }
        }

        if (bus != null && !deltaReport.isEmpty()) {
            bus.post(new ProliferateEvent(ProliferateExecutor.class, deltaReport));
        }

        return deltaReport;
    }
}
