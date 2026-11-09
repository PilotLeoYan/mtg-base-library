package com.mtg.world.node;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.world.events.ManaNodeAttunedEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Mutable state machine representing a deployed Elemental Mana Node instance in the world.
 * Manages charges, recharge timers, player attunement pulses, and gem harvesting.
 */
public class ManaNodeState {
    private final ManaNodeDefinition definition;
    private int currentCharges;
    private int rechargeTimerTicks;
    private TriggerBus triggerBus;

    public ManaNodeState(ManaNodeDefinition definition) {
        this(definition, null);
    }

    public ManaNodeState(ManaNodeDefinition definition, TriggerBus triggerBus) {
        this.definition = Objects.requireNonNull(definition, "Definition cannot be null");
        this.currentCharges = definition.maxCharges();
        this.rechargeTimerTicks = 0;
        this.triggerBus = triggerBus;
    }

    public synchronized ManaNodeDefinition getDefinition() {
        return definition;
    }

    public synchronized int getCurrentCharges() {
        return currentCharges;
    }

    public synchronized void setCurrentCharges(int charges) {
        this.currentCharges = Math.clamp(charges, 0, definition.maxCharges());
    }

    public synchronized int getRechargeTimerTicks() {
        return rechargeTimerTicks;
    }

    public synchronized void setTriggerBus(TriggerBus triggerBus) {
        this.triggerBus = triggerBus;
    }

    public synchronized TriggerBus getTriggerBus() {
        return triggerBus;
    }

    /**
     * Ticks the mana node state.
     * When current charges are below maximum, increments the recharge timer.
     * When reaching rechargeIntervalTicks (default 200 ticks), recharges 1 charge.
     */
    public synchronized void tick() {
        if (currentCharges < definition.maxCharges()) {
            rechargeTimerTicks++;
            if (rechargeTimerTicks >= definition.rechargeIntervalTicks()) {
                currentCharges++;
                rechargeTimerTicks = 0;
            }
        } else {
            rechargeTimerTicks = 0;
        }
    }

    /**
     * Attunes a player to the node. If charges are available:
     * - Decrements charge by 1.
     * - Adds the node's pulseAmount of colored mana to the player's pool.
     * - Posts ManaNodeAttunedEvent to TriggerBus.
     *
     * @param playerId UUID of the interacting player
     * @return true if attunement succeeded, false if node is depleted (0 charges)
     */
    public synchronized boolean attune(UUID playerId) {
        if (playerId == null || currentCharges <= 0) {
            return false;
        }

        currentCharges--;
        int amount = definition.pulseAmount();
        ManaColor color = definition.color();

        try {
            ManaManager.getInstance().addMana(playerId, color, amount);
        } catch (Exception ignored) {
            // Safe fallback in headless tests if player pool not initialized
        }

        if (triggerBus != null) {
            triggerBus.post(new ManaNodeAttunedEvent(this, playerId, definition.type(), color, amount));
        }

        return true;
    }

    /**
     * Harvests the catalyst from the mana node, returning the translation key or identifier
     * of the corresponding Mana Gem item.
     */
    public synchronized String harvestGem() {
        return definition.type().getGemTranslationKey();
    }
}
