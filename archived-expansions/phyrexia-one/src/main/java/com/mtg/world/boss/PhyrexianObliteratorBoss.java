package com.mtg.world.boss;

import com.mtg.api.mechanics.one.PoisonCounterManager;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.world.events.BossDefeatedEvent;
import com.mtg.world.events.BossPhaseChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Phyrexian Obliterator Miniboss encounter (Black / Colorless).
 * A 5/5 horror with Trample and Toxic 2 that inflicts horrific retaliatory recoil damage
 * and poison counters upon any attacker who damages it, transitioning into Corrupted Overload.
 */
public class PhyrexianObliteratorBoss implements IBossEntity {
    public static final String BOSS_TYPE = "phyrexian_obliterator";
    public static final float BASE_HEALTH = 200.0f;
    public static final float BASE_ATTACK_DAMAGE = 10.0f;
    public static final int TOXIC_VALUE = 2;

    private final UUID id;
    private final float maxHealth;
    private float currentHealth;
    private float attackDamage;
    private BossPhase phase;
    private final BossThreatTable threatTable;
    private final PoisonCounterManager poisonManager;
    private TriggerBus triggerBus;
    private double x;
    private double y;
    private double z;
    private float lastRecoilDamageDealt;
    private UUID lastRecoilTarget;

    public PhyrexianObliteratorBoss() {
        this(UUID.randomUUID(), BASE_HEALTH, BASE_ATTACK_DAMAGE, null, new PoisonCounterManager());
    }

    public PhyrexianObliteratorBoss(UUID id, float maxHealth, float attackDamage, TriggerBus triggerBus, PoisonCounterManager poisonManager) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.maxHealth = Math.max(1.0f, maxHealth);
        this.currentHealth = this.maxHealth;
        this.attackDamage = Math.max(1.0f, attackDamage);
        this.phase = BossPhase.PHASE_1_TACTICAL;
        this.threatTable = new BossThreatTable();
        this.triggerBus = triggerBus;
        this.poisonManager = (poisonManager != null) ? poisonManager : new PoisonCounterManager();
        this.lastRecoilDamageDealt = 0.0f;
        this.lastRecoilTarget = null;
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

    public PoisonCounterManager getPoisonManager() {
        return poisonManager;
    }

    public synchronized float getAttackDamage() {
        return attackDamage;
    }

    public synchronized float getLastRecoilDamageDealt() {
        return lastRecoilDamageDealt;
    }

    public synchronized UUID getLastRecoilTarget() {
        return lastRecoilTarget;
    }

    /**
     * Calculates strike damage against target. In Phase 2 (Corrupted Overload),
     * if the target has >= 3 poison counters, strike deals 2x damage!
     */
    public synchronized float calculateStrikeDamage(UUID targetId) {
        float baseDmg = attackDamage;
        if (phase == BossPhase.PHASE_2_ENRAGED && targetId != null) {
            int poison = poisonManager.getPoison(targetId);
            if (poison >= 3) {
                return baseDmg * 2.0f;
            }
        }
        return baseDmg;
    }

    @Override
    public synchronized void takeDamage(UUID attackerId, float amount) {
        if (!isAlive() || amount <= 0.0f) return;

        currentHealth -= amount;
        threatTable.addDamageThreat(attackerId, amount);

        // Retaliatory Backlash: 50% damage recoil + 1 poison counter
        if (attackerId != null) {
            float recoil = amount * 0.50f;
            lastRecoilDamageDealt = recoil;
            lastRecoilTarget = attackerId;
            poisonManager.addPoison(attackerId, 1, id, triggerBus);
        }

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
            attackDamage *= 1.20f;
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
