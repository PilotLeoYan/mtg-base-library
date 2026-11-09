package com.mtg.entity;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.trigger.CombatDamageEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CombatStats;
import com.mtg.entity.ai.SummonAiState;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Concrete domain implementation of a player-aligned summoned creature.
 */
public class SummonedCreature implements ISummonedCreature {
    private final UUID entityId;
    private final UUID ownerUuid;
    private final CardDefinition cardDefinition;
    private final CombatStats baseStats;

    private int powerBonus = 0;
    private int toughnessBonus = 0;
    private float health;

    private boolean summoningSickness;
    private int sicknessTicksRemaining;
    private boolean tapped;
    private boolean dismissed;

    private SummonAiState aiState = SummonAiState.IDLE;
    private UUID targetEntityId = null;

    private double x;
    private double y;
    private double z;
    private int lifetimeTicks = 0;

    private Consumer<Float> ownerHealConsumer = null;
    private TriggerBus triggerBus = null;

    public SummonedCreature(UUID entityId, UUID ownerUuid, CardDefinition cardDefinition, double x, double y, double z) {
        this.entityId = Objects.requireNonNull(entityId, "Entity ID cannot be null");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "Owner UUID cannot be null");
        this.cardDefinition = Objects.requireNonNull(cardDefinition, "CardDefinition cannot be null");
        this.baseStats = cardDefinition.combatStats().orElse(new CombatStats(1, 1));

        this.health = CombatStatScaler.calculateMaxHealth(baseStats.toughness());

        boolean hasHaste = cardDefinition.hasKeyword(CoreKeywords.HASTE.id()) || cardDefinition.hasKeyword("mtg:haste");
        this.summoningSickness = !hasHaste;
        this.sicknessTicksRemaining = hasHaste ? 0 : 20; // 20 ticks (1s) default stabilization
        this.tapped = false;
        this.dismissed = false;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SummonedCreature(UUID ownerUuid, CardDefinition cardDefinition) {
        this(UUID.randomUUID(), ownerUuid, cardDefinition, 0.0, 0.0, 0.0);
    }

    public void setOwnerHealConsumer(Consumer<Float> consumer) {
        this.ownerHealConsumer = consumer;
    }

    public void setTriggerBus(TriggerBus triggerBus) {
        this.triggerBus = triggerBus;
    }

    @Override public UUID getEntityId() { return entityId; }
    @Override public UUID getOwnerUuid() { return ownerUuid; }
    @Override public boolean isOwner(UUID playerId) { return ownerUuid.equals(playerId); }
    @Override public CardDefinition getCardDefinition() { return cardDefinition; }
    @Override public CombatStats getCombatStats() { return new CombatStats(getPower(), getToughness()); }

    @Override
    public synchronized int getPower() {
        return Math.max(0, baseStats.power() + powerBonus);
    }

    @Override
    public synchronized int getToughness() {
        return Math.max(0, baseStats.toughness() + toughnessBonus);
    }

    @Override
    public synchronized float getMaxHealth() {
        return CombatStatScaler.calculateMaxHealth(getToughness());
    }

    @Override
    public synchronized float getHealth() {
        return health;
    }

    @Override
    public synchronized float getAttackDamage() {
        return CombatStatScaler.calculateAttackDamage(getPower());
    }

    @Override
    public synchronized void applyStatModifier(int powerDelta, int toughnessDelta) {
        this.powerBonus += powerDelta;
        this.toughnessBonus += toughnessDelta;
        float maxH = getMaxHealth();
        if (toughnessDelta > 0) {
            this.health = Math.min(maxH, this.health + (toughnessDelta * CombatStatScaler.HP_PER_TOUGHNESS));
        } else if (this.health > maxH) {
            this.health = maxH;
        }
        if (getToughness() <= 0) {
            dismiss();
        }
    }

    @Override
    public synchronized void setHealth(float newHealth) {
        float maxH = getMaxHealth();
        this.health = Math.clamp(newHealth, 0.0f, maxH);
        if (this.health <= 0.0f) {
            this.dismissed = true;
        }
    }

    @Override
    public synchronized void heal(float amount) {
        if (!isDead() && amount > 0.0f) {
            this.health = Math.min(getMaxHealth(), this.health + amount);
        }
    }

    @Override
    public synchronized void takeDamage(float amount) {
        if (amount > 0.0f) {
            setHealth(this.health - amount);
        }
    }

    @Override
    public synchronized boolean isDead() {
        return dismissed || health <= 0.0f || getToughness() <= 0;
    }

    @Override
    public synchronized boolean hasSummoningSickness() {
        return summoningSickness;
    }

    @Override
    public synchronized void clearSummoningSickness() {
        this.summoningSickness = false;
        this.sicknessTicksRemaining = 0;
    }

    @Override
    public synchronized boolean canAttack() {
        return !isDead() && !isTapped() && !summoningSickness && !hasKeyword(CoreKeywords.DEFENDER.id());
    }

    @Override
    public synchronized boolean isTapped() {
        return tapped;
    }

    @Override
    public synchronized void tap() {
        this.tapped = true;
    }

    @Override
    public synchronized boolean tryTap() {
        if (this.tapped) {
            return false;
        }
        this.tapped = true;
        return true;
    }

    @Override
    public synchronized void untap() {
        this.tapped = false;
    }

    public synchronized float executeAttack(UUID targetId, boolean isTargetBoss, float targetMaxHp, Object defenderObj) {
        if (!canAttack()) return 0.0f;

        float dmg = getAttackDamage();
        if (hasDeathtouch()) {
            dmg = CombatStatScaler.calculateDeathtouchDamage(dmg, targetMaxHp, isTargetBoss);
        }

        // Tap permanent unless creature has Vigilance
        if (!hasVigilance()) {
            tap();
        }

        // Lifelink: restore health to summoner
        if (hasLifelink() && ownerHealConsumer != null) {
            ownerHealConsumer.accept(dmg);
        }

        // Post combat damage event to TriggerBus (triggers Toxic, Landfall, etc.)
        if (triggerBus != null) {
            triggerBus.post(new CombatDamageEvent(this, this, defenderObj, this.entityId, targetId, dmg, true));
        }

        return dmg;
    }

    @Override
    public boolean hasKeyword(String keywordId) {
        return cardDefinition.hasKeyword(keywordId);
    }

    @Override
    public boolean isFlying() {
        return hasKeyword(CoreKeywords.FLYING.id()) || hasKeyword("mtg:flying");
    }

    @Override
    public boolean hasVigilance() {
        return hasKeyword(CoreKeywords.VIGILANCE.id()) || hasKeyword("mtg:vigilance");
    }

    @Override
    public boolean hasLifelink() {
        return hasKeyword(CoreKeywords.LIFELINK.id()) || hasKeyword("mtg:lifelink");
    }

    @Override
    public boolean hasDeathtouch() {
        return hasKeyword(CoreKeywords.DEATHTOUCH.id()) || hasKeyword("mtg:deathtouch");
    }

    @Override
    public boolean hasFirstStrike() {
        return hasKeyword(CoreKeywords.FIRST_STRIKE.id()) || hasKeyword("mtg:first_strike");
    }

    @Override
    public boolean hasTrample() {
        return hasKeyword(CoreKeywords.TRAMPLE.id()) || hasKeyword("mtg:trample");
    }

    @Override
    public boolean hasHaste() {
        return hasKeyword(CoreKeywords.HASTE.id()) || hasKeyword("mtg:haste");
    }

    @Override
    public int getToxicValue() {
        return cardDefinition.getKeywordValue("mtg:toxic");
    }

    @Override public synchronized SummonAiState getAiState() { return aiState; }
    @Override public synchronized void setAiState(SummonAiState state) { this.aiState = state != null ? state : SummonAiState.IDLE; }
    @Override public synchronized UUID getTargetEntityId() { return targetEntityId; }
    @Override public synchronized void setTargetEntityId(UUID targetId) { this.targetEntityId = targetId; }

    @Override public synchronized double getX() { return x; }
    @Override public synchronized double getY() { return y; }
    @Override public synchronized double getZ() { return z; }
    @Override public synchronized void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    @Override
    public synchronized double distanceTo(double targetX, double targetY, double targetZ) {
        double dx = this.x - targetX;
        double dy = this.y - targetY;
        double dz = this.z - targetZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override public synchronized int getLifetimeTicks() { return lifetimeTicks; }

    @Override
    public synchronized void tick() {
        if (isDead()) return;
        lifetimeTicks++;

        if (summoningSickness && sicknessTicksRemaining > 0) {
            sicknessTicksRemaining--;
            if (sicknessTicksRemaining <= 0) {
                summoningSickness = false;
            }
        }
    }

    @Override
    public synchronized void dismiss() {
        this.dismissed = true;
        this.health = 0.0f;
    }

    @Override
    public synchronized boolean isDismissed() {
        return dismissed;
    }
}
