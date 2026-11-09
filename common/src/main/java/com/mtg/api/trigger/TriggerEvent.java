package com.mtg.api.trigger;

public abstract class TriggerEvent {
    private final long timestamp;
    private final Object source;
    private boolean canceled;

    protected TriggerEvent(Object source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
        this.canceled = false;
    }

    public long getTimestamp() { return timestamp; }
    public Object getSource() { return source; }
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }
}
