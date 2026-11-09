package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.world.events.BossDefeatedEvent;
import com.mtg.world.events.BossPhaseChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Shivan Dragon World Boss encounter (Red / Mountain).
 * Features aerial flight, Firebreathing power scaling, explosive fireballs,
 * and an enraged ground-pounding second phase with Pyroclasm shockwaves.
 */
public class ShivanDragonBoss implements IBossEntity {
    public static final String BOSS_TYPE = "shivan_dragon";
    public static final float BASE_HEALTH = 300.0f;
    public static final float BASE_ATTACK_DAMAGE = 12.0f;

    private final UUID id;
    private final float maxHealth;
    private float currentHealth;
    private float attackDamage;
    private int firebreathingStacks;
    private boolean flying;
    private BossPhase phase;
    private final BossThreatTable threatTable;
    private TriggerBus triggerBus;
    private double x;
    private double y;
    private double z;
    private boolean pyroclasmTriggered;

    public ShivanDragonBoss() {
        this(UUID.randomUUID(), BASE_HEALTH, BASE_ATTACK_DAMAGE, null);
    }

    public ShivanDragonBoss(UUID id, float maxHealth, float attackDamage, TriggerBus triggerBus) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.maxHealth = Math.max(1.0f, maxHealth);
        this.currentHealth = this.maxHealth;
        this.attackDamage = Math.max(1.0f, attackDamage);
        this.firebreathingStacks = 0;
        this.flying = true;
        this.phase = BossPhase.PHASE_1_TACTICAL;
        this.threatTable = new BossThreatTable();
        this.triggerBus = triggerBus;
        this.pyroclasmTriggered = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getBossType() {
        return BOSS_TYPE;
    }

    @Override
    public synchronized float getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public float getMaxHealth() {
        return maxHealth;
    }

    @Override
    public synchronized BossPhase getPhase() {
        return phase;
    }

    @Override
    public synchronized boolean isAlive() {
        return phase != BossPhase.DEFEATED && currentHealth > 0.0f;
    }

    @Override
    public BossThreatTable getThreatTable() {
        return threatTable;
    }

    public synchronized boolean isFlying() {
        return flying;
    }

    public synchronized void setFlying(boolean flying) {
        this.flying = flying;
    }

    public synchronized int getFirebreathingStacks() {
        return firebreathingStacks;
    }

    /**
     * Activates Firebreathing ({R}: +1/+0), increasing current attack damage by +1.0 per charge.
     */
    public synchronized void activateFirebreathing(int stacks) {
        this.firebreathingStacks = Math.min(10, this.firebreathingStacks + stacks);
    }

    public synchronized float getEffectiveAttackDamage() {
        return attackDamage + (firebreathingStacks * 1.0f);
    }

    /**
     * Spits an explosive fireball at target dealing 8.0 base damage.
     */
    public float castFireball() {
        return 8.0f;
    }

    public synchronized boolean isPyroclasmTriggered() {
        return pyroclasmTriggered;
    }

    @Override
    public synchronized void takeDamage(UUID attackerId, float amount) {
        if (!isAlive() || amount <= 0.0f) return;

        currentHealth -= amount;
        threatTable.addDamageThreat(attackerId, amount);

        if (currentHealth <= 0.0f) {
            currentHealth = 0.0f;
            BossPhase old = phase;
            phase = BossPhase.DEFEATED;
            if (triggerBus != null) {
                triggerBus.post(new BossPhaseChangeEvent(this, id, BOSS_TYPE, old, phase));
                List<UUID> participants = new ArrayList<>(threatTable.getAllThreats().keySet());
                triggerBus.post(new BossDefeatedEvent(this, id, BOSS_TYPE, attackerId, participants));
            }
        } else if (phase == BossPhase.PHASE_1_TACTICAL && currentHealth <= (maxHealth * 0.50f)) {
            BossPhase old = phase;
            phase = BossPhase.PHASE_2_ENRAGED;
            flying = false; // Descends to ground in rage
            attackDamage *= 1.25f; // Permanent enraged attack increase
            firebreathingStacks = 5; // Permanent minimum fire charges
            pyroclasmTriggered = true; // Emits 6.0 Pyroclasm shockwave
            if (triggerBus != null) {
                triggerBus.post(new BossPhaseChangeEvent(this, id, BOSS_TYPE, old, phase));
            }
        }
    }

    @Override
    public synchronized void heal(float amount) {
        if (!isAlive() || amount <= 0.0f) return;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    @Override
    public synchronized void tick() {
        if (!isAlive()) return;
        // Firebreathing charge slow decay if in tactical phase
        if (phase == BossPhase.PHASE_1_TACTICAL && firebreathingStacks > 0) {
            // Maintains charges during active battle
        }
    }

    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getZ() { return z; }
    @Override public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    @Override public void setTriggerBus(TriggerBus triggerBus) { this.triggerBus = triggerBus; }
    @Override public TriggerBus getTriggerBus() { return triggerBus; }
}
