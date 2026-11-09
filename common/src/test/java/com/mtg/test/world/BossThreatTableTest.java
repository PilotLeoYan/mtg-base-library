package com.mtg.test.world;

import com.mtg.world.boss.BossThreatTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Boss Threat Table & Leash Decay Suite")
public class BossThreatTableTest {

    private BossThreatTable threatTable;
    private UUID playerA;
    private UUID playerB;
    private UUID playerC;

    @BeforeEach
    void setUp() {
        threatTable = new BossThreatTable();
        playerA = UUID.randomUUID();
        playerB = UUID.randomUUID();
        playerC = UUID.randomUUID();
    }

    @Test
    @DisplayName("Threat generation: physical damage (1.0x), healing (0.5x), and spell mana (2.0x)")
    void testThreatGeneration() {
        // Player A deals 15 damage -> 15.0 threat
        threatTable.addDamageThreat(playerA, 15.0f);
        assertEquals(15.0f, threatTable.getThreat(playerA));

        // Player B heals 20 HP -> 10.0 threat
        threatTable.addHealingThreat(playerB, 20.0f);
        assertEquals(10.0f, threatTable.getThreat(playerB));

        // Player C casts a 6 CMC spell -> 12.0 threat
        threatTable.addSpellThreat(playerC, 6);
        assertEquals(12.0f, threatTable.getThreat(playerC));

        // Highest threat target is Player A (15.0)
        Optional<UUID> highest = threatTable.getHighestThreatTarget();
        assertTrue(highest.isPresent());
        assertEquals(playerA, highest.get());
    }

    @Test
    @DisplayName("Target selection within 32-block engagement radius ignores distant players")
    void testEngagementRadiusTargeting() {
        threatTable.addDamageThreat(playerA, 100.0f); // High threat but far away
        threatTable.addDamageThreat(playerB, 50.0f);  // Lower threat but close

        Map<UUID, Double> distances = new HashMap<>();
        distances.put(playerA, 40.0); // > 32 blocks
        distances.put(playerB, 15.0); // <= 32 blocks

        Optional<UUID> target = threatTable.getHighestThreatTarget(distances, 32.0);
        assertTrue(target.isPresent());
        assertEquals(playerB, target.get(), "Target must be selected from entities within 32 blocks");
    }

    @Test
    @DisplayName("Leash threat decay: entities beyond 48 blocks lose 10% threat per cycle")
    void testLeashDecay() {
        threatTable.addDamageThreat(playerA, 100.0f);
        threatTable.addDamageThreat(playerB, 100.0f);

        Map<UUID, Double> distances = new HashMap<>();
        distances.put(playerA, 10.0); // In range
        distances.put(playerB, 55.0); // Beyond 48 blocks leash

        // Apply 10% decay
        threatTable.applyLeashDecay(distances, 48.0, 0.10f);

        assertEquals(100.0f, threatTable.getThreat(playerA), "In-range player threat must not decay");
        assertEquals(90.0f, threatTable.getThreat(playerB), 0.01f, "Leashed player threat must decay by 10%");

        // Apply second decay cycle
        threatTable.applyLeashDecay(distances, 48.0, 0.10f);
        assertEquals(81.0f, threatTable.getThreat(playerB), 0.01f);
    }
}
