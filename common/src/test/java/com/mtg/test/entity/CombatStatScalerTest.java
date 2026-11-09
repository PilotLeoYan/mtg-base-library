package com.mtg.test.entity;

import com.mtg.entity.CombatStatScaler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Combat Stat Scaler Test Suite")
public class CombatStatScalerTest {

    @Test
    @DisplayName("1. Stat Scaling: 1 Toughness = 4.0 HP, 1 Power = 2.0 damage")
    void testStandardStatScaling() {
        // Serra Angel: 4/4 -> 16 HP, 8 damage
        assertEquals(16.0f, CombatStatScaler.calculateMaxHealth(4), 0.01f);
        assertEquals(8.0f, CombatStatScaler.calculateAttackDamage(4), 0.01f);

        // Llanowar Elves: 1/1 -> 4 HP, 2 damage
        assertEquals(4.0f, CombatStatScaler.calculateMaxHealth(1), 0.01f);
        assertEquals(2.0f, CombatStatScaler.calculateAttackDamage(1), 0.01f);

        // 0 Power creature (e.g. Birds of Paradise: 0/1)
        assertEquals(4.0f, CombatStatScaler.calculateMaxHealth(1), 0.01f);
        assertEquals(0.0f, CombatStatScaler.calculateAttackDamage(0), 0.01f);
    }

    @Test
    @DisplayName("2. Deathtouch Scaling: Lethal against non-bosses, +50% critical against bosses")
    void testDeathtouchScaling() {
        float baseDmg = 4.0f;
        float mobMaxHp = 100.0f;

        // Non-boss: deals full lethal damage (100 HP)
        float nonBossDmg = CombatStatScaler.calculateDeathtouchDamage(baseDmg, mobMaxHp, false);
        assertEquals(100.0f, nonBossDmg, 0.01f, "Deathtouch should deal lethal strike to non-boss");

        // Boss (e.g. Ender Dragon, Wither): deals 1.5x base damage (6.0 damage) instead of one-shotting
        float bossDmg = CombatStatScaler.calculateDeathtouchDamage(baseDmg, mobMaxHp, true);
        assertEquals(6.0f, bossDmg, 0.01f, "Deathtouch should apply 1.5x critical multiplier to bosses");
    }
}
