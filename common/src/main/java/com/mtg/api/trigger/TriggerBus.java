package com.mtg.api.trigger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TriggerBus {
    private static final Logger LOGGER = Logger.getLogger(TriggerBus.class.getName());

    public static final class RegisteredListener<T extends TriggerEvent> {
        final Class<T> eventType;
        final int order;
        final boolean isMonitor;
        final TriggerListener<T> listener;

        RegisteredListener(Class<T> eventType, int order, boolean isMonitor, TriggerListener<T> listener) {
            this.eventType = eventType;
            this.order = order;
            this.isMonitor = isMonitor;
            this.listener = listener;
        }

        public int getOrder() {
            return order;
        }

        public boolean isMonitor() {
            return isMonitor;
        }

        public TriggerListener<T> getListener() {
            return listener;
        }
    }

    private final Map<Class<? extends TriggerEvent>, List<RegisteredListener<?>>> listeners = new ConcurrentHashMap<>();

    public <T extends TriggerEvent> void registerListener(Class<T> eventType, TriggerPriority priority, TriggerListener<T> listener) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");
        int order = switch (priority) {
            case HIGHEST -> 1000;
            case HIGH -> 800;
            case NORMAL -> 500;
            case LOW -> 200;
            case LOWEST -> 100;
            case MONITOR -> Integer.MIN_VALUE;
        };
        boolean isMonitor = (priority == TriggerPriority.MONITOR);
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(new RegisteredListener<>(eventType, order, isMonitor, listener));
    }

    public <T extends TriggerEvent> void registerListener(Class<T> eventType, int priority, TriggerListener<T> listener) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(new RegisteredListener<>(eventType, priority, false, listener));
    }

    public <T extends TriggerEvent> void registerListener(Class<T> eventType, TriggerListener<T> listener) {
        registerListener(eventType, TriggerPriority.NORMAL, listener);
    }

    public void unregisterListener(TriggerListener<?> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        for (List<RegisteredListener<?>> list : listeners.values()) {
            list.removeIf(reg -> reg.listener == listener || reg.listener.equals(listener));
        }
    }

    public <T extends TriggerEvent> void unregisterListener(Class<T> eventType, TriggerListener<T> listener) {
        List<RegisteredListener<?>> list = listeners.get(eventType);
        if (list != null) {
            list.removeIf(reg -> reg.listener == listener || reg.listener.equals(listener));
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends TriggerEvent> T post(T event) {
        Objects.requireNonNull(event, "Event cannot be null");
        Class<?> eventClass = event.getClass();

        List<RegisteredListener<?>> applicable = new ArrayList<>();
        for (Map.Entry<Class<? extends TriggerEvent>, List<RegisteredListener<?>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventClass)) {
                applicable.addAll(entry.getValue());
            }
        }

        // Higher order executes first; monitors execute last
        applicable.sort((a, b) -> {
            if (a.isMonitor && !b.isMonitor) return 1;
            if (!a.isMonitor && b.isMonitor) return -1;
            return Integer.compare(b.order, a.order);
        });

        for (RegisteredListener<?> reg : applicable) {
            if (event.isCanceled() && !reg.isMonitor) {
                continue; // Skip canceled events unless in MONITOR phase
            }
            try {
                ((TriggerListener<T>) reg.listener).onTrigger(event);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Exception thrown in TriggerListener for event " + eventClass.getSimpleName(), t);
            }
        }

        return event;
    }

    public void clear() {
        listeners.clear();
    }
}
