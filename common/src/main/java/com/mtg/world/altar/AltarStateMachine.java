package com.mtg.world.altar;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardRarity;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import com.mtg.world.boss.*;
import com.mtg.world.events.AltarRitualEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * State machine managing Planeswalker Altar rituals:
 * Mana Replenishment, Planar Conflux Overcharge, and Boss Summoning Trials.
 * Enforces a 60-tick channeling delay and posts events to TriggerBus.
 */
public class AltarStateMachine {
    public static final int CHANNELING_TICKS = 60; // 3 seconds at 20 TPS
    public static final int COOLDOWN_TICKS = 100;   // 5 seconds

    private final AltarDefinition definition;
    private AltarRitualPhase currentPhase;
    private AltarRitualType activeRitualType;
    private ManaColor activeColor;
    private UUID activePlayerId;
    private int channelTimerTicks;
    private int cooldownTimerTicks;
    private TriggerBus triggerBus;
    private double x;
    private double y;
    private double z;

    public AltarStateMachine() {
        this(AltarDefinition.standard(), null);
    }

    public AltarStateMachine(AltarDefinition definition, TriggerBus triggerBus) {
        this.definition = Objects.requireNonNull(definition, "Definition cannot be null");
        this.currentPhase = AltarRitualPhase.IDLE;
        this.activeRitualType = null;
        this.activeColor = null;
        this.activePlayerId = null;
        this.channelTimerTicks = 0;
        this.cooldownTimerTicks = 0;
        this.triggerBus = triggerBus;
    }

    public synchronized AltarDefinition getDefinition() {
        return definition;
    }

    public synchronized AltarRitualPhase getCurrentPhase() {
        return currentPhase;
    }

    public synchronized AltarRitualType getActiveRitualType() {
        return activeRitualType;
    }

    public synchronized ManaColor getActiveColor() {
        return activeColor;
    }

    public synchronized UUID getActivePlayerId() {
        return activePlayerId;
    }

    public synchronized int getChannelTimerTicks() {
        return channelTimerTicks;
    }

    public synchronized void setTriggerBus(TriggerBus triggerBus) {
        this.triggerBus = triggerBus;
    }

    public synchronized TriggerBus getTriggerBus() {
        return triggerBus;
    }

    public synchronized void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Initiates a Mana Replenishment ritual using 1 colored Mana Gem.
     */
    public synchronized boolean initiateReplenishment(UUID playerId, ManaColor color) {
        if (currentPhase != AltarRitualPhase.IDLE || playerId == null || color == null) {
            return false;
        }

        this.activePlayerId = playerId;
        this.activeRitualType = AltarRitualType.MANA_REPLENISHMENT;
        this.activeColor = color;
        this.currentPhase = AltarRitualPhase.CHANNELING;
        this.channelTimerTicks = 0;

        if (triggerBus != null) {
            triggerBus.post(new AltarRitualEvent(this, playerId, activeRitualType, activeColor, currentPhase));
        }
        return true;
    }

    /**
     * Initiates a Planar Conflux Overcharge ritual using 1 of each of 5 colored gems (WUBRG).
     */
    public synchronized boolean initiateConfluxOvercharge(UUID playerId) {
        if (currentPhase != AltarRitualPhase.IDLE || playerId == null) {
            return false;
        }

        this.activePlayerId = playerId;
        this.activeRitualType = AltarRitualType.CONFLUX_OVERCHARGE;
        this.activeColor = null; // Multi-color
        this.currentPhase = AltarRitualPhase.CHANNELING;
        this.channelTimerTicks = 0;

        if (triggerBus != null) {
            triggerBus.post(new AltarRitualEvent(this, playerId, activeRitualType, activeColor, currentPhase));
        }
        return true;
    }

    /**
     * Initiates a Boss Summoning Trial using a Mana Gem catalyst and Planeswalker focus.
     */
    public synchronized boolean initiateBossSummon(UUID playerId, ManaColor catalystColor, int xpLevels) {
        if (currentPhase != AltarRitualPhase.IDLE || playerId == null || catalystColor == null || xpLevels < 10) {
            return false;
        }

        this.activePlayerId = playerId;
        this.activeRitualType = AltarRitualType.BOSS_SUMMON;
        this.activeColor = catalystColor;
        this.currentPhase = AltarRitualPhase.CHANNELING;
        this.channelTimerTicks = 0;

        if (triggerBus != null) {
            triggerBus.post(new AltarRitualEvent(this, playerId, activeRitualType, activeColor, currentPhase));
        }
        return true;
    }

    /**
     * Ticks the altar ritual state machine.
     * When channeling reaches 60 ticks, applies the ritual effect, posts COMPLETED event,
     * and transitions to COOLDOWN.
     */
    public synchronized void tick() {
        if (currentPhase == AltarRitualPhase.CHANNELING) {
            channelTimerTicks++;
            if (channelTimerTicks >= CHANNELING_TICKS) {
                completeRitual();
            }
        } else if (currentPhase == AltarRitualPhase.COOLDOWN) {
            cooldownTimerTicks++;
            if (cooldownTimerTicks >= COOLDOWN_TICKS) {
                currentPhase = AltarRitualPhase.IDLE;
                activeRitualType = null;
                activeColor = null;
                activePlayerId = null;
                channelTimerTicks = 0;
                cooldownTimerTicks = 0;
            }
        }
    }

    private void completeRitual() {
        currentPhase = AltarRitualPhase.COMPLETED;

        if (activeRitualType == AltarRitualType.MANA_REPLENISHMENT && activePlayerId != null && activeColor != null) {
            try {
                ManaPool pool = ManaManager.getInstance().getPlayerMana(activePlayerId);
                if (pool != null) {
                    pool.clear();
                    pool.addMana(activeColor, pool.getMaxMana());
                }
            } catch (Exception ignored) {}
        } else if (activeRitualType == AltarRitualType.CONFLUX_OVERCHARGE && activePlayerId != null) {
            try {
                ManaPool pool = ManaManager.getInstance().getPlayerMana(activePlayerId);
                if (pool != null) {
                    // Overcharge: expand max mana by +5 and add WUBRG mana surge (+1 of each color)
                    pool.setMaxMana(pool.getMaxMana() + 5);
                    pool.addMana(ManaColor.WHITE, 1);
                    pool.addMana(ManaColor.BLUE, 1);
                    pool.addMana(ManaColor.BLACK, 1);
                    pool.addMana(ManaColor.RED, 1);
                    pool.addMana(ManaColor.GREEN, 1);
                }
                // Award 1 Rare wildcard
                ProgressionManager.getInstance().getProgression(activePlayerId).addWildcards(CardRarity.RARE, 1);
            } catch (Exception ignored) {}
        } else if (activeRitualType == AltarRitualType.BOSS_SUMMON && activeColor != null) {
            spawnSummonedBoss(activeColor);
        }

        if (triggerBus != null && activePlayerId != null && activeRitualType != null) {
            triggerBus.post(new AltarRitualEvent(this, activePlayerId, activeRitualType, activeColor, currentPhase));
        }

        // Move to cooldown
        currentPhase = AltarRitualPhase.COOLDOWN;
        cooldownTimerTicks = 0;
    }

    private void spawnSummonedBoss(ManaColor catalystColor) {
        IBossEntity boss = BossManager.getInstance().createBossForColor(catalystColor, triggerBus);
        boss.setPosition(x, y + 2.0, z);
        BossManager.getInstance().registerBoss(boss);
    }
}
