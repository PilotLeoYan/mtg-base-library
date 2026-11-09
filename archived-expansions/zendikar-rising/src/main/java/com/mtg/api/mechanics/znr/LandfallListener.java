package com.mtg.api.mechanics.znr;

import com.mtg.api.trigger.TriggerListener;

@FunctionalInterface
public interface LandfallListener extends TriggerListener<LandfallTrigger> {
    void onLandfall(LandfallTrigger trigger);

    @Override
    default void onTrigger(LandfallTrigger event) {
        onLandfall(event);
    }
}
