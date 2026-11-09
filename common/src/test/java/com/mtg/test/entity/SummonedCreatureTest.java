package com.mtg.test.entity;

import com.mtg.api.trigger.CombatDamageEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.entity.ISummonedCreature;
import com.mtg.entity.SummonedCreature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Summoned Creature & Keyword Integration Suite")
public class SummonedCreatureTest {

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("1. Stat Scaling & Health: Serra Angel 4/4 has 16 HP and 8 Attack Damage")
    void testCreatureStatScaling() {
        SummonedCreature angel = new SummonedCreature(ownerId, TestCreatures.SERRA_ANGEL);
        assertEquals(4, angel.getPower());
        assertEquals(4, angel.getToughness());
        assertEquals(16.0f, angel.getMaxHealth(), 0.01f);
        assertEquals(16.0f, angel.getHealth(), 0.01f);
        assertEquals(8.0f, angel.getAttackDamage(), 0.01f);

        // Damage angel
        angel.takeDamage(6.0f);
        assertEquals(10.0f, angel.getHealth(), 0.01f);
        assertFalse(angel.isDead());

        // Overheal prevention
        angel.heal(20.0f);
        assertEquals(16.0f, angel.getHealth(), 0.01f, "Healing cannot exceed max health");

        // Lethal damage
        angel.takeDamage(16.0f);
        assertEquals(0.0f, angel.getHealth(), 0.01f);
        assertTrue(angel.isDead());
    }

    @Test
    @DisplayName("2. Summoning Sickness: Non-haste starts sick and clears after ticks; Haste starts ready")
    void testSummoningSickness() {
        // Serra Angel (no Haste)
        SummonedCreature angel = new SummonedCreature(ownerId, TestCreatures.SERRA_ANGEL);
        assertTrue(angel.hasSummoningSickness());
        assertFalse(angel.canAttack(), "Non-haste creature cannot attack with summoning sickness");

        // Tick 20 ticks
        for (int i = 0; i < 20; i++) {
            angel.tick();
        }
        assertFalse(angel.hasSummoningSickness());
        assertTrue(angel.canAttack());

        // Goblin Guide (has Haste)
        SummonedCreature goblin = new SummonedCreature(ownerId, TestCreatures.GOBLIN_GUIDE);
        assertFalse(goblin.hasSummoningSickness(), "Haste bypasses summoning sickness");
        assertTrue(goblin.canAttack(), "Haste creature can attack immediately");
    }

    @Test
    @DisplayName("3. Vigilance: Attacking does NOT tap creature; non-vigilant creature taps")
    void testVigilanceAttackingNoTap() {
        // Serra Angel has Vigilance
        SummonedCreature angel = new SummonedCreature(ownerId, TestCreatures.SERRA_ANGEL);
        angel.clearSummoningSickness();
        assertFalse(angel.isTapped());

        angel.executeAttack(UUID.randomUUID(), false, 20.0f, null);
        assertFalse(angel.isTapped(), "Vigilance creature should not tap upon attacking");
        assertTrue(angel.canAttack(), "Vigilant creature remains ready to attack/defend");

        // Goblin Guide does NOT have Vigilance
        SummonedCreature goblin = new SummonedCreature(ownerId, TestCreatures.GOBLIN_GUIDE);
        assertFalse(goblin.isTapped());

        goblin.executeAttack(UUID.randomUUID(), false, 20.0f, null);
        assertTrue(goblin.isTapped(), "Non-vigilant creature must tap upon attacking");
        assertFalse(goblin.canAttack(), "Tapped creature cannot attack");
    }

    @Test
    @DisplayName("4. Lifelink: Dealing damage heals owner for exact amount dealt")
    void testLifelinkHealingOwner() {
        SummonedCreature nighthawk = new SummonedCreature(ownerId, TestCreatures.VAMPIRE_NIGHTHAWK);
        nighthawk.clearSummoningSickness();
        assertTrue(nighthawk.hasLifelink());

        AtomicReference<Float> healedAmount = new AtomicReference<>(0.0f);
        nighthawk.setOwnerHealConsumer(healedAmount::set);

        float dmg = nighthawk.executeAttack(UUID.randomUUID(), false, 20.0f, null);
        assertEquals(20.0f, dmg, 0.01f); // Deathtouch deals 20.0 lethal damage to 20 HP target
        assertEquals(20.0f, healedAmount.get(), 0.01f, "Lifelink must heal summoner for exact damage dealt");
        assertEquals(dmg, healedAmount.get(), "Lifelink healing must equal exact damage dealt");
    }

    @Test
    @DisplayName("5. Deathtouch: Lethal strike against non-boss; +50% critical against boss")
    void testDeathtouchCombat() {
        SummonedCreature nighthawk = new SummonedCreature(ownerId, TestCreatures.VAMPIRE_NIGHTHAWK);
        nighthawk.clearSummoningSickness();
        assertTrue(nighthawk.hasDeathtouch());

        // Non-boss with 80 HP
        float nonBossDmg = nighthawk.executeAttack(UUID.randomUUID(), false, 80.0f, null);
        assertEquals(80.0f, nonBossDmg, 0.01f, "Deathtouch deals full lethal damage to non-boss");

        nighthawk.untap();

        // Boss mob
        float bossDmg = nighthawk.executeAttack(UUID.randomUUID(), true, 80.0f, null);
        assertEquals(6.0f, bossDmg, 0.01f, "Deathtouch deals 1.5x damage (4.0 * 1.5 = 6.0) to boss");
    }

    @Test
    @DisplayName("6. Toxic N: Venerated Rotpriest applies poison counters via TriggerBus")
    void testToxicPoisonTrigger() {
        TriggerBus bus = new TriggerBus();
        AtomicInteger poisonCounters = new AtomicInteger(0);
        bus.registerListener(CombatDamageEvent.class, event -> {
            if (event.getSource() instanceof ISummonedCreature summon) {
                poisonCounters.addAndGet(summon.getToxicValue());
            }
        });

        SummonedCreature rotpriest = new SummonedCreature(ownerId, TestCreatures.VENERATED_ROTPRIEST);
        rotpriest.setTriggerBus(bus);
        rotpriest.clearSummoningSickness();
        assertEquals(1, rotpriest.getToxicValue());

        UUID targetId = UUID.randomUUID();
        assertEquals(0, poisonCounters.get());

        rotpriest.executeAttack(targetId, false, 20.0f, null);

        assertEquals(1, poisonCounters.get(), "Toxic 1 should apply 1 poison counter to target on combat damage");
    }
}
