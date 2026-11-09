package com.mtg.api.mechanics.one;

import java.util.Map;
import java.util.UUID;

public interface ICounterHolder {
    UUID getHolderId();
    Map<String, Integer> getCounters();
    int getCounter(String counterType);
    void addCounter(String counterType, int count);
}
