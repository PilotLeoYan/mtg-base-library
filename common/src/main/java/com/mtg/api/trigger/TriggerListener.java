package com.mtg.api.trigger;

@FunctionalInterface
public interface TriggerListener<T extends TriggerEvent> {
    void onTrigger(T event);
}
