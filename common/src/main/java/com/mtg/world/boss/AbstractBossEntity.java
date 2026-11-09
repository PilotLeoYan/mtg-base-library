package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.config.WorldGenConfig;
import com.mtg.world.events.BossDefeatedEvent;
import com.mtg.world.events.BossPhaseChangeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract extensible base implementation of IBossEntity.
 * Provides reusable 3-phase progression (PHASE_1 > 66% HP, PHASE_2 33%-66% HP, PHASE_3_ENRAGE <= 33% HP),
 * threat management, multiplayer stat scaling, TriggerBus event firing, ability cooldown tracking,
 * and lifecycle hooks.
 */
public abstract class AbstractBossEntity implements IBossEntity {
    private final UUID id;
    private final String bossType;
    private final float baseHealth;
    private final float baseAttackDamage;

    private float maxHealth;
    private float currentHealth;
    private float attackDamage;
    private BossPhase phase;
    private final BossThreatTable threatTable;
    private TriggerBus triggerBus;
    private double x;
    private double y;
    private double z;

    private final Map<String, Integer> abilityCooldowns = new ConcurrentHashMap<>();

    public AbstractBossEntity(UUID id, String bossType, float baseHealth, float baseAttackDamage, TriggerBus triggerBus) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.bossType = Objects.requireNonNull(bossType, "Boss type cannot be null");
        this.baseHealth = Math.max(1.0f, baseHealth);
        this.baseAttackDamage = Math.max(1.0f, baseAttackDamage);
        this.maxHealth = this.baseHealth;
        this.currentHealth = this.maxHealth;
        this.attackDamage = this.baseAttackDamage;
        this.phase = BossPhase.PHASE_1_TACTICAL;
        this.threatTable = new BossThreatTable();
        this.triggerBus = triggerBus;
    }

    public AbstractBossEntity(String bossType, float baseHealth, float baseAttackDamage, TriggerBus triggerBus) {
        this(UUID.randomUUID(), bossType, baseHealth, baseAttackDamage, triggerBus);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getBossType() {
        return bossType;
    }

    public float getBaseHealth() {
        return baseHealth;
    }

    public float getBaseAttackDamage() {
        return baseAttackDamage;
    }

    @Override
    public synchronized float getCurrentHealth() {
        return currentHealth;
    }

    public synchronized void setCurrentHealth(float currentHealth) {
        this.currentHealth = Math.max(0.0f, Math.min(maxHealth, currentHealth));
        if (this.currentHealth <= 0.0f) {
            BossPhase old = phase;
            phase = BossPhase.DEFEATED;
            onPhaseChange(old, phase);
        } else {
            updatePhase();
        }
    }

    @Override
    public synchronized float getMaxHealth() {
        return maxHealth;
    }

    public synchronized void setMaxHealth(float maxHealth) {
        this.maxHealth = Math.max(1.0f, maxHealth);
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }
    }

    public synchronized float getAttackDamage() {
        return attackDamage;
    }

    public synchronized void setAttackDamage(float attackDamage) {
        this.attackDamage = Math.max(0.0f, attackDamage);
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

    /**
     * Scales max health and attack damage based on active combatants and world config.
     */
    public synchronized void scaleStats(int activeCombatants, WorldGenConfig config) {
        this.maxHealth = BossStatScaler.scaleMaxHealth(this.baseHealth, activeCombatants, config);
        this.currentHealth = this.maxHealth;
        this.attackDamage = BossStatScaler.scaleAttackDamage(this.baseAttackDamage, config);
    }

    @Override
    public synchronized void takeDamage(UUID attackerId, float amount) {
        if (!isAlive() || amount <= 0.0f) return;

        currentHealth -= amount;
        if (attackerId != null) {
            threatTable.addDamageThreat(attackerId, amount);
        }

        if (currentHealth <= 0.0f) {
            currentHealth = 0.0f;
            BossPhase old = phase;
            phase = BossPhase.DEFEATED;
            onPhaseChange(old, phase);
            if (triggerBus != null) {
                triggerBus.post(new BossPhaseChangeEvent(this, id, bossType, old, phase));
                List<UUID> participants = new ArrayList<>(threatTable.getAllThreats().keySet());
                triggerBus.post(new BossDefeatedEvent(this, id, bossType, attackerId, participants));
            }
        } else {
            updatePhase();
        }
    }

    @Override
    public synchronized void heal(float amount) {
        if (!isAlive() || amount <= 0.0f) return;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
        updatePhase();
    }

    /**
     * Re-evaluates current combat phase according to standard thresholds:
     * PHASE_1 > 66% HP, PHASE_2 33%-66% HP, PHASE_3_ENRAGE <= 33% HP.
     */
    protected synchronized void updatePhase() {
        if (!isAlive()) return;
        float ratio = currentHealth / maxHealth;
        BossPhase newPhase;
        if (ratio > 0.66f) {
            newPhase = BossPhase.PHASE_1_TACTICAL;
        } else if (ratio > 0.33f) {
            newPhase = BossPhase.PHASE_2_ENRAGED;
        } else {
            newPhase = BossPhase.PHASE_3_DESPERATION;
        }

        if (newPhase != phase) {
            BossPhase old = phase;
            phase = newPhase;
            onPhaseChange(old, newPhase);
            if (triggerBus != null) {
                triggerBus.post(new BossPhaseChangeEvent(this, id, bossType, old, newPhase));
            }
        }
    }

    /**
     * Hook invoked whenever the boss transitions between combat phases.
     */
    protected void onPhaseChange(BossPhase oldPhase, BossPhase newPhase) {
        // Customizable by subclasses
    }

    // --- Ability Cooldown Tracking ---

    public synchronized void setAbilityCooldown(String abilityId, int ticks) {
        if (ticks <= 0) {
            abilityCooldowns.remove(abilityId);
        } else {
            abilityCooldowns.put(abilityId, ticks);
        }
    }

    public synchronized int getAbilityCooldown(String abilityId) {
        return abilityCooldowns.getOrDefault(abilityId, 0);
    }

    public synchronized boolean isAbilityReady(String abilityId) {
        return getAbilityCooldown(abilityId) <= 0;
    }

    public synchronized boolean tryExecuteSpecialAbility(String abilityId, int cooldownTicks) {
        if (!isAlive() || !isAbilityReady(abilityId)) {
            return false;
        }
        executeSpecialAbility(abilityId);
        setAbilityCooldown(abilityId, cooldownTicks);
        return true;
    }

    /**
     * Hook invoked to execute a special ability by ID.
     */
    protected void executeSpecialAbility(String abilityId) {
        // Customizable by subclasses
    }

    @Override
    public synchronized void tick() {
        if (!isAlive()) return;

        Iterator<Map.Entry<String, Integer>> it = abilityCooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        onTick();
    }

    /**
     * Hook invoked every server tick while boss is alive.
     */
    protected void onTick() {
        // Customizable by subclasses
    }

    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getZ() { return z; }
    @Override public synchronized void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    @Override public synchronized void setTriggerBus(TriggerBus triggerBus) { this.triggerBus = triggerBus; }
    @Override public synchronized TriggerBus getTriggerBus() { return triggerBus; }
}
