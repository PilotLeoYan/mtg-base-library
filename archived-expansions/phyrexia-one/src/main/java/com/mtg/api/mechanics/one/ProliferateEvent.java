package com.mtg.api.mechanics.one;

import com.mtg.api.trigger.TriggerEvent;
import java.util.Map;
import java.util.UUID;

public class ProliferateEvent extends TriggerEvent {
    private final Map<UUID, Map<String, Integer>> proliferatedCounters;

    public ProliferateEvent(Object source, Map<UUID, Map<String, Integer>> proliferatedCounters) {
        super(source);
        this.proliferatedCounters = (proliferatedCounters == null || proliferatedCounters.isEmpty())
            ? Map.of()
            : Map.copyOf(proliferatedCounters);
    }

    public Map<UUID, Map<String, Integer>> getProliferatedCounters() {
        return proliferatedCounters;
    }
}
