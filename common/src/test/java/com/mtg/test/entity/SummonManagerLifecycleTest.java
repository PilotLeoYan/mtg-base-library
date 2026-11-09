package com.mtg.test.entity;

import com.mtg.config.CardsConfig;
import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Summon Manager Lifecycle & Cap Enforcement Suite")
public class SummonManagerLifecycleTest {

    private SummonManager summonManager;
    private UUID playerA;
    private UUID playerB;

    @BeforeEach
    void setUp() {
        summonManager = new SummonManager(CardsConfig.defaults());
        SummonManager.setInstance(summonManager);
        summonManager.clear();

        playerA = UUID.randomUUID();
        playerB = UUID.randomUUID();
    }

    @Test
    @DisplayName("1. Max Summons Cap: Enforces max limit (3) via FIFO auto-dismissal of oldest summon")
    void testMaxSummonsCapFIFO() {
        SummonedCreature s1 = new SummonedCreature(playerA, TestCreatures.LLANOWAR_ELVES);
        SummonedCreature s2 = new SummonedCreature(playerA, TestCreatures.GOBLIN_GUIDE);
        SummonedCreature s3 = new SummonedCreature(playerA, TestCreatures.PHYREXIAN_RAGER);

        summonManager.registerSummon(s1);
        summonManager.registerSummon(s2);
        summonManager.registerSummon(s3);

        assertEquals(3, summonManager.getActiveSummonCount(playerA));
        assertFalse(s1.isDead());
        assertFalse(s2.isDead());
        assertFalse(s3.isDead());

        // Summon 4th creature -> exceeds default cap (3)
        SummonedCreature s4 = new SummonedCreature(playerA, TestCreatures.SERRA_ANGEL);
        summonManager.registerSummon(s4);

        assertEquals(3, summonManager.getActiveSummonCount(playerA), "Active summon count should remain at max cap (3)");

        // Oldest summon (s1) must be auto-dismissed
        assertTrue(s1.isDead(), "Oldest summon s1 should be dismissed via FIFO");
        assertTrue(s1.isDismissed());

        // Summons s2, s3, s4 remain active
        List<SummonedCreature> active = summonManager.getPlayerSummons(playerA);
        assertEquals(3, active.size());
        assertTrue(active.contains(s2));
        assertTrue(active.contains(s3));
        assertTrue(active.contains(s4));
    }

    @Test
    @DisplayName("2. Despawn on Owner Death: All active summons dismissed when owner dies")
    void testDespawnOnOwnerDeath() {
        SummonedCreature s1 = new SummonedCreature(playerA, TestCreatures.LLANOWAR_ELVES);
        SummonedCreature s2 = new SummonedCreature(playerA, TestCreatures.SERRA_ANGEL);
        summonManager.registerSummon(s1);
        summonManager.registerSummon(s2);

        assertEquals(2, summonManager.getActiveSummonCount(playerA));

        // Player dies
        summonManager.onPlayerDeath(playerA);

        assertEquals(0, summonManager.getActiveSummonCount(playerA), "All summons should despawn on owner death");
        assertTrue(s1.isDead());
        assertTrue(s2.isDead());
    }

    @Test
    @DisplayName("3. Despawn on Owner Disconnect: All active summons dismissed when owner logs out")
    void testDespawnOnOwnerDisconnect() {
        SummonedCreature s1 = new SummonedCreature(playerA, TestCreatures.VAMPIRE_NIGHTHAWK);
        summonManager.registerSummon(s1);
        assertEquals(1, summonManager.getActiveSummonCount(playerA));

        // Player disconnects
        summonManager.onPlayerDisconnect(playerA);

        assertEquals(0, summonManager.getActiveSummonCount(playerA));
        assertTrue(s1.isDead());
    }

    @Test
    @DisplayName("4. Multi-Player Isolation: Summons of different players do not interfere with each other")
    void testMultiPlayerIsolation() {
        SummonedCreature sA1 = new SummonedCreature(playerA, TestCreatures.LLANOWAR_ELVES);
        SummonedCreature sA2 = new SummonedCreature(playerA, TestCreatures.SERRA_ANGEL);
        summonManager.registerSummon(sA1);
        summonManager.registerSummon(sA2);

        SummonedCreature sB1 = new SummonedCreature(playerB, TestCreatures.GOBLIN_GUIDE);
        summonManager.registerSummon(sB1);

        assertEquals(2, summonManager.getActiveSummonCount(playerA));
        assertEquals(1, summonManager.getActiveSummonCount(playerB));

        // Player A dies -> Player B summons remain unaffected
        summonManager.onPlayerDeath(playerA);

        assertEquals(0, summonManager.getActiveSummonCount(playerA));
        assertTrue(sA1.isDead());
        assertTrue(sA2.isDead());

        assertEquals(1, summonManager.getActiveSummonCount(playerB), "Player B summons must not be affected by Player A death");
        assertFalse(sB1.isDead());
    }
}
