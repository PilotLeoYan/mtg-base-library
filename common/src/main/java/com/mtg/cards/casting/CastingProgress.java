package com.mtg.cards.casting;

/**
 * Snapshot of the runtime channeling progress of an active spell cast.
 */
public record CastingProgress(
    float progressFraction,
    int elapsedTicks,
    int remainingTicks,
    int totalTicks,
    boolean finished,
    boolean interrupted
) {
    public static CastingProgress of(int remainingTicks, int totalTicks, boolean finished, boolean interrupted) {
        int total = Math.max(1, totalTicks);
        int remaining = Math.max(0, remainingTicks);
        int elapsed = total - remaining;
        float frac = Math.clamp(1.0f - ((float) remaining / (float) total), 0.0f, 1.0f);
        return new CastingProgress(frac, elapsed, remaining, total, finished, interrupted);
    }
}
