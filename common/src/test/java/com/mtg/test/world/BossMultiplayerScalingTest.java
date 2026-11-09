package com.mtg.test.world;

import com.mtg.config.WorldGenConfig;
import com.mtg.world.boss.BossStatScaler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("World Boss Multiplayer Scaling & Stat Scaling Suite")
public class BossMultiplayerScalingTest {

    @Test
    @DisplayName("Solo combatant: scaled health matches base health without group multiplier")
    void testSoloCombatantHealth() {
        WorldGenConfig config = WorldGenConfig.defaults();
        float baseHp = 300.0f;

        float scaledHp = BossStatScaler.calculateScaledMaxHealth(baseHp, 1, config);
        assertEquals(300.0f, scaledHp, 0.01f);
    }

    @Test
    @DisplayName("Group scaling: each additional combatant adds exactly +35% to boss max health")
    void testMultiplayerGroupHealthScaling() {
        WorldGenConfig config = WorldGenConfig.defaults();
        float baseHp = 300.0f;

        // 2 players: 1.0 + 0.35 * 1 = 1.35x
        float hp2 = BossStatScaler.calculateScaledMaxHealth(baseHp, 2, config);
        assertEquals(300.0f * 1.35f, hp2, 0.01f);

        // 3 players: 1.0 + 0.35 * 2 = 1.70x
        float hp3 = BossStatScaler.calculateScaledMaxHealth(baseHp, 3, config);
        assertEquals(300.0f * 1.70f, hp3, 0.01f);

        // 4 players: 1.0 + 0.35 * 3 = 2.05x
        float hp4 = BossStatScaler.calculateScaledMaxHealth(baseHp, 4, config);
        assertEquals(300.0f * 2.05f, hp4, 0.01f);
    }

    @Test
    @DisplayName("Config multipliers scale health and damage multiplicatively")
    void testConfigMultiplierScaling() {
        // WorldGenConfig with 1.5x HP and 1.25x damage
        WorldGenConfig customConfig = new WorldGenConfig(
                true, 0.02, 3,
                true, 32, 8,
                true, 0.05,
                true, 0.01,
                1.5, 1.25,
                true, 10, 3
        );

        float baseHp = 300.0f;
        float baseDamage = 12.0f;

        // 1 player with 1.5x config multiplier = 450.0 HP
        float scaledHp = BossStatScaler.calculateScaledMaxHealth(baseHp, 1, customConfig);
        assertEquals(450.0f, scaledHp, 0.01f);

        // 2 players with 1.5x config: 300 * 1.5 * 1.35 = 607.5 HP
        float scaledHpGroup = BossStatScaler.calculateScaledMaxHealth(baseHp, 2, customConfig);
        assertEquals(607.5f, scaledHpGroup, 0.01f);

        // Scaled attack damage: 12.0 * 1.25 = 15.0
        float scaledDamage = BossStatScaler.calculateScaledAttackDamage(baseDamage, customConfig);
        assertEquals(15.0f, scaledDamage, 0.01f);
    }
}
