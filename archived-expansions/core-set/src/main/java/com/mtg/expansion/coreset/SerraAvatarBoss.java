package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.world.events.BossDefeatedEvent;
import com.mtg.world.events.BossPhaseChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Serra Avatar World Boss encounter (White / Plains).
 * Dynamic P/T entity whose attack damage scales with health, protected by a Divine Aegis barrier,
 * wielding Holy Smite beams and casting Wrath of God shockwaves in Phase 2.
 */
public class SerraAvatarBoss implements IBossEntity {
    public static final String BOSS_TYPE = "serra_avatar";
    public static final float BASE_HEALTH = 350.0f;
    public static final float INITIAL_AEGIS_SHIELD = 60.0f;

    private final UUID id;
    private final float maxHealth;
    private float currentHealth;
    private float divineAegisShield;
    private boolean flying;
    private BossPhase phase;
    private final BossThreatTable threatTable;
    private TriggerBus triggerBus;
    private double x;
    private double y;
    private double z;
    private boolean wrathWaveTriggered;
    private int activeHolyPylons;

    public SerraAvatarBoss() {
        this(UUID.randomUUID(), BASE_HEALTH, null);
    }

    public SerraAvatarBoss(UUID id, float maxHealth, TriggerBus triggerBus) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.maxHealth = Math.max(1.0f, maxHealth);
        this.currentHealth = this.maxHealth;
        this.divineAegisShield = INITIAL_AEGIS_SHIELD;
        this.flying = true;
        this.phase = BossPhase.PHASE_1_TACTICAL;
        this.threatTable = new BossThreatTable();
        this.triggerBus = triggerBus;
        this.wrathWaveTriggered = false;
        this.activeHolyPylons = 0;
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

    public synchronized float getDivineAegisShield() {
        return divineAegisShield;
    }

    public synchronized boolean isWrathWaveTriggered() {
        return wrathWaveTriggered;
    }

    public synchronized int getActiveHolyPylons() {
        return activeHolyPylons;
    }

    public synchronized void destroyHolyPylon() {
        if (activeHolyPylons > 0) {
            activeHolyPylons--;
        }
    }

    /**
     * Dynamic attack damage scaling with health: damage = Math.max(4.0f, (currentHp / maxHp) * 20.0f).
     */
    public synchronized float getDynamicAttackDamage() {
        return Math.max(4.0f, (currentHealth / maxHealth) * 20.0f);
    }

    /**
     * Holy Smite radiant beam: deals 10.0 damage.
     */
    public float castHolySmite() {
        return 10.0f;
    }

    @Override
    public synchronized void takeDamage(UUID attackerId, float amount) {
        if (!isAlive() || amount <= 0.0f) return;

        float remainingDamage = amount;

        // Divine Aegis absorbs damage first
        if (divineAegisShield > 0.0f) {
            if (divineAegisShield >= remainingDamage) {
                divineAegisShield -= remainingDamage;
                remainingDamage = 0.0f;
            } else {
                remainingDamage -= divineAegisShield;
                divineAegisShield = 0.0f;
            }
        }

        if (remainingDamage > 0.0f) {
            currentHealth -= remainingDamage;
        }

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
            wrathWaveTriggered = true;
            activeHolyPylons = 3;
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
        // Pylons regenerate health
        if (activeHolyPylons > 0 && currentHealth < maxHealth) {
            heal(1.0f * activeHolyPylons);
        }
    }

    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getZ() { return z; }
    @Override public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    @Override public void setTriggerBus(TriggerBus triggerBus) { this.triggerBus = triggerBus; }
    @Override public TriggerBus getTriggerBus() { return triggerBus; }
}
