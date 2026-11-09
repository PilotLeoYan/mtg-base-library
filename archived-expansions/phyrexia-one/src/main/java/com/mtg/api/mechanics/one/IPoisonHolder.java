package com.mtg.api.mechanics.one;

import java.util.UUID;

public interface IPoisonHolder {
    UUID getHolderId();
    int getPoisonCounters();
    void setPoisonCounters(int count);

    default void addPoisonCounters(int amount) {
        setPoisonCounters(getPoisonCounters() + Math.max(0, amount));
    }

    default boolean isPoisonLethal() {
        return getPoisonCounters() >= PoisonCounterManager.LETHAL_POISON_THRESHOLD;
    }
}
