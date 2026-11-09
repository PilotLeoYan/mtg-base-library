package com.mtg.api.trigger;

public enum TriggerPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST,
    MONITOR // Read-only observation phase, never skips canceled events
}
