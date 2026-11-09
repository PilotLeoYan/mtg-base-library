package com.mtg.cards.effects;

import com.mtg.domain.CardDefinition;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime active instance of an enchantment field or aura.
 */
public class ActiveEnchantment {

    public enum EnchantmentTargetType {
        CASTER,
        TARGET_ENTITY,
        WORLD_AREA
    }

    private final UUID instanceId;
    private final UUID ownerId;
    private final CardDefinition card;
    private final EnchantmentTargetType targetType;
    private final int targetEntityId;
    private int remainingTicks;
    private final int totalTicks;
    private final IEnchantmentTickListener listener;
    private final AtomicBoolean expired = new AtomicBoolean(false);

    public ActiveEnchantment(UUID ownerId, CardDefinition card, EnchantmentTargetType targetType,
                             int targetEntityId, int durationTicks, IEnchantmentTickListener listener) {
        this.instanceId = UUID.randomUUID();
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
        this.card = Objects.requireNonNull(card, "card cannot be null");
        this.targetType = targetType != null ? targetType : EnchantmentTargetType.CASTER;
        this.targetEntityId = targetEntityId;
        this.remainingTicks = Math.max(1, durationTicks);
        this.totalTicks = this.remainingTicks;
        this.listener = listener;
    }

    public UUID getInstanceId() { return instanceId; }
    public UUID getOwnerId() { return ownerId; }
    public CardDefinition getCard() { return card; }
    public EnchantmentTargetType getTargetType() { return targetType; }
    public int getTargetEntityId() { return targetEntityId; }
    public synchronized int getRemainingTicks() { return remainingTicks; }
    public int getTotalTicks() { return totalTicks; }

    public synchronized float getProgressFraction() {
        return Math.clamp(1.0f - ((float) remainingTicks / (float) totalTicks), 0.0f, 1.0f);
    }

    public synchronized boolean tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (listener != null) {
                listener.onTick(this);
            }
        }
        if (remainingTicks <= 0) {
            return expire();
        }
        return false;
    }

    public void triggerUpkeep() {
        if (listener != null) {
            listener.onUpkeep(this);
        }
    }

    public boolean expire() {
        if (expired.compareAndSet(false, true)) {
            if (listener != null) {
                listener.onExpire(this);
            }
            return true;
        }
        return false;
    }

    public boolean isExpired() {
        return expired.get();
    }
}
