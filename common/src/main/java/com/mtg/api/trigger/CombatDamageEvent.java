package com.mtg.api.trigger;

import java.util.UUID;

public class CombatDamageEvent extends TriggerEvent {
    private final Object attacker;
    private final Object defender;
    private final UUID attackerId;
    private final UUID defenderId;
    private final float damageAmount;
    private final boolean combatDamage;

    public CombatDamageEvent(Object source, Object attacker, Object defender, UUID attackerId, UUID defenderId, float damageAmount, boolean combatDamage) {
        super(source);
        this.attacker = attacker;
        this.defender = defender;
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.damageAmount = damageAmount;
        this.combatDamage = combatDamage;
    }

    public CombatDamageEvent(Object source, UUID attackerId, UUID defenderId, float damageAmount) {
        this(source, null, null, attackerId, defenderId, damageAmount, true);
    }

    public Object getAttacker() { return attacker; }
    public Object getDefender() { return defender; }
    public UUID getAttackerId() { return attackerId; }
    public UUID getDefenderId() { return defenderId; }
    public float getDamageAmount() { return damageAmount; }
    public boolean isCombatDamage() { return combatDamage; }
}
