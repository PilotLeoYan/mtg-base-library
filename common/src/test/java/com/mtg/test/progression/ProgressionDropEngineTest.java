package com.mtg.test.progression;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import com.mtg.progression.CardUnlockSource;
import com.mtg.progression.ProgressionDropEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression & Economy — ProgressionDropEngine Gameplay Source Suite")
public class ProgressionDropEngineTest {

    private ProgressionDropEngine engine;
    private ProgressionConfig config;

    @BeforeEach
    void setup() {
        List<CardDefinition> cards = List.of(
                createCard("mtg:white_common", ManaCost.ofWhite(1), CardRarity.COMMON),
                createCard("mtg:white_uncommon", ManaCost.ofWhite(2), CardRarity.UNCOMMON),
                createCard("mtg:blue_common", ManaCost.ofBlue(1), CardRarity.COMMON),
                createCard("mtg:blue_rare", ManaCost.ofBlue(3), CardRarity.RARE),
                createCard("mtg:black_common", ManaCost.ofBlack(1), CardRarity.COMMON),
                createCard("mtg:black_rare", ManaCost.ofBlack(3), CardRarity.RARE),
                createCard("mtg:black_mythic", ManaCost.ofBlack(4), CardRarity.MYTHIC),
                createCard("mtg:red_common", ManaCost.ofRed(1), CardRarity.COMMON),
                createCard("mtg:red_rare", ManaCost.ofRed(3), CardRarity.RARE),
                createCard("mtg:green_common", ManaCost.ofGreen(1), CardRarity.COMMON),
                createCard("mtg:green_rare", ManaCost.ofGreen(3), CardRarity.RARE),
                createCard("mtg:colorless_mythic", ManaCost.ZERO, CardRarity.MYTHIC)
        );

        engine = new ProgressionDropEngine(cards, new Random(42));
        config = ProgressionConfig.defaults();
    }

    private static CardDefinition createCard(String id, ManaCost cost, CardRarity rarity) {
        return new CardDefinition(
                id, "BASE", "name." + id, cost,
                Set.of(CardType.INSTANT), Set.of(), rarity,
                Optional.empty(), List.of(), 60, 0
        );
    }

    @Test
    @DisplayName("Mining ancient debris yields Rare or Mythic cards")
    void testMiningDropDebrisYieldsHighTier() {
        // High drop chance for test
        ProgressionConfig highDrop = new ProgressionConfig(
                4, 7, 10, 1.5, 4, 4, 4, 4,
                1.0, 1.0, 1.0, 1.0, 2, 5
        );

        for (int i = 0; i < 20; i++) {
            ProgressionDropEngine.DropRoll roll = engine.evaluateMiningDrop("minecraft:ancient_debris", highDrop);
            assertTrue(roll.success());
            assertEquals(CardUnlockSource.MINING, roll.source());
            String cardId = roll.cardId().orElseThrow();
            assertTrue(cardId.contains("rare") || cardId.contains("mythic"),
                    "Ancient debris must drop Rare or Mythic: " + cardId);
        }
    }

    @Test
    @DisplayName("Farming wheat yields Green or White cards")
    void testFarmingDropAffinities() {
        ProgressionConfig highDrop = new ProgressionConfig(
                4, 7, 10, 1.5, 4, 4, 4, 4,
                1.0, 1.0, 1.0, 1.0, 2, 5
        );

        for (int i = 0; i < 20; i++) {
            ProgressionDropEngine.DropRoll roll = engine.evaluateFarmingDrop("minecraft:wheat", highDrop);
            assertTrue(roll.success());
            assertEquals(CardUnlockSource.FARMING, roll.source());
            String cardId = roll.cardId().orElseThrow();
            assertTrue(cardId.contains("green") || cardId.contains("white"),
                    "Wheat must yield Green or White cards: " + cardId);
        }
    }

    @Test
    @DisplayName("Guaranteed boss drops honor 100% drop rate and Rare/Mythic tier")
    void testGuaranteedBossDropHonor() {
        ProgressionConfig bossConfig = new ProgressionConfig(
                4, 7, 10, 1.5, 4, 4, 4, 4,
                0.02, 0.01, 0.05, 1.00, 2, 5
        );

        for (int i = 0; i < 50; i++) {
            ProgressionDropEngine.DropRoll roll = engine.evaluateMobDrop("minecraft:ender_dragon", bossConfig);
            assertTrue(roll.success(), "Boss drops must be 100% guaranteed");
            assertEquals(CardUnlockSource.BOSS_DROP, roll.source());
            String cardId = roll.cardId().orElseThrow();
            assertTrue(cardId.contains("rare") || cardId.contains("mythic"),
                    "Boss drop must be Rare or Mythic: " + cardId);
        }
    }

    @Test
    @DisplayName("Zero drop chance config produces exactly 0 drops over 1,000 rolls")
    void testZeroDropChanceHonor() {
        ProgressionConfig zeroDrop = new ProgressionConfig(
                4, 7, 10, 1.5, 4, 4, 4, 4,
                0.0, 0.0, 0.0, 0.0, 2, 5
        );

        for (int i = 0; i < 1000; i++) {
            assertFalse(engine.evaluateMiningDrop("minecraft:diamond_ore", zeroDrop).success());
            assertFalse(engine.evaluateFarmingDrop("minecraft:carrots", zeroDrop).success());
            assertFalse(engine.evaluateMobDrop("minecraft:zombie", zeroDrop).success());
        }
    }

    @Test
    @DisplayName("Dungeon loot generator produces configured card count")
    void testDungeonLootCount() {
        List<String> loot = engine.generateDungeonLoot("minecraft:ancient_city", config);
        assertEquals(config.dungeonChestCardCount(), loot.size());
    }
}
