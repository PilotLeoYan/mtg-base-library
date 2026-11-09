package com.mtg.test.e2e.tier1;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.progression.CardUnlockSource;
import com.mtg.progression.ProgressionDropEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 10: Card Progression (Mining, Farming, Mobs, Chests) E2E Test")
public class Feature10_CardProgressionSourcesTest {

    private ProgressionDropEngine dropEngine;
    private ProgressionConfig config;

    @BeforeEach
    void setUp() {
        List<CardDefinition> cards = E2ETestExpansionHelper.getAllTestCards();
        // Deterministic random generator for test repeatability
        dropEngine = new ProgressionDropEngine(cards, new Random(12345));
        config = ProgressionConfig.defaults();
    }

    @Test
    @DisplayName("1. Mining diamond ore evaluates drop source as MINING")
    void testMiningOreProgressionDrop() {
        var roll = dropEngine.evaluateMiningDrop("minecraft:diamond_ore", config);
        assertEquals(CardUnlockSource.MINING, roll.source());
        assertNotNull(roll.cardId());
    }

    @Test
    @DisplayName("2. Harvesting wheat crops evaluates drop source as FARMING")
    void testFarmingCropProgressionDrop() {
        var roll = dropEngine.evaluateFarmingDrop("minecraft:wheat", config);
        assertEquals(CardUnlockSource.FARMING, roll.source());
    }

    @Test
    @DisplayName("3. Slaying hostile mob evaluates drop source as MOB_DROP")
    void testMobSlayingProgressionDrop() {
        var roll = dropEngine.evaluateMobDrop("minecraft:zombie", config);
        assertEquals(CardUnlockSource.MOB_DROP, roll.source());
    }

    @Test
    @DisplayName("4. Slaying world boss evaluates drop source as BOSS_DROP")
    void testWorldBossProgressionDrop() {
        var roll = dropEngine.evaluateMobDrop("mtg:elemental_avatar_red", config);
        assertEquals(CardUnlockSource.BOSS_DROP, roll.source());
    }

    @Test
    @DisplayName("5. Looting dungeon chest generates non-null dungeon loot cards")
    void testDungeonChestLootProgressionDrop() {
        List<String> loot = dropEngine.generateDungeonLoot("minecraft:chests/simple_dungeon", config);
        assertNotNull(loot);
    }
}
