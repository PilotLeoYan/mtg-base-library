package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.world.events.BossDefeatedEvent;
import com.mtg.world.events.BossPhaseChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Demon of Death's Gate World Boss encounter (Black / Swamp).
 * Massive 9/9 flying horror featuring Trample, Dark Ritual sacrificial healing,
 * Life Siphon pulses, and an Abyssal Gate convergence summoning Shadow Thralls.
 */
public class DemonDeathsGateBoss implements IBossEntity {
    public static final String BOSS_TYPE = "demon_of_deaths_gate";
    public static final float BASE_HEALTH = 450.0f;
    public static final float BASE_ATTACK_DAMAGE = 18.0f;

    private final UUID id;
    private final float maxHealth;
    private float currentHealth;
    private float attackDamage;
    private boolean flying;
    private BossPhase phase;
    private final BossThreatTable threatTable;
    private TriggerBus triggerBus;
    private double x;
    private double y;
    private double z;
    private boolean darknessShroudActive;
    private int thrallsSummoned;

    public DemonDeathsGateBoss() {
        this(UUID.randomUUID(), BASE_HEALTH, BASE_ATTACK_DAMAGE, null);
    }

    public DemonDeathsGateBoss(UUID id, float maxHealth, float attackDamage, TriggerBus triggerBus) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.maxHealth = Math.max(1.0f, maxHealth);
        this.currentHealth = this.maxHealth;
        this.attackDamage = Math.max(1.0f, attackDamage);
        this.flying = true;
        this.phase = BossPhase.PHASE_1_TACTICAL;
        this.threatTable = new BossThreatTable();
        this.triggerBus = triggerBus;
        this.darknessShroudActive = false;
        this.thrallsSummoned = 0;
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

    public synchronized float getAttackDamage() {
        return attackDamage;
    }

    public synchronized boolean isDarknessShroudActive() {
        return darknessShroudActive;
    }

    public synchronized int getThrallsSummoned() {
        return thrallsSummoned;
    }

    /**
     * Dark Ritual Sacrifice: consumes a minion or shade to instantly heal 40 HP.
     */
    public synchronized void sacrificeMinion() {
        if (!isAlive()) return;
        heal(40.0f);
    }

    /**
     * Life Siphon Pulse: drains 4.0 HP from each provided target, healing the demon by the total.
     */
    public synchronized float lifeSiphon(int targetCount) {
        if (!isAlive() || targetCount <= 0) return 0.0f;
        float drained = targetCount * 4.0f;
        heal(drained);
        return drained;
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
        } else if (phase == BossPhase.PHASE_1_TACTICAL && currentHealth <= (maxHealth * 0.40f)) {
            BossPhase old = phase;
            phase = BossPhase.PHASE_2_ENRAGED;
            darknessShroudActive = true;
            thrallsSummoned = 3;
            attackDamage *= 1.30f;
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
    }

    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getZ() { return z; }
    @Override public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    @Override public void setTriggerBus(TriggerBus triggerBus) { this.triggerBus = triggerBus; }
    @Override public TriggerBus getTriggerBus() { return triggerBus; }
}
