package com.mtg.test.progression;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;
import com.mtg.network.*;
import com.mtg.progression.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression & Wildcard Economy — Adversarial Stress & Fuzzing Suite")
public class ProgressionAdversarialStressTest {

    // =========================================================================
    // 1. Bulk Duplicate Transmutation Stress
    // =========================================================================
    @Nested
    @DisplayName("1. Bulk Duplicate Transmutation Stress & Math Correctness")
    class BulkTransmutationStress {

        @Test
        @DisplayName("Extreme bulk: 1,000 duplicate commons at once -> 250 Uncommon Wildcards, 0 remainder")
        void testBulk1000CommonsAtOnce() {
            PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
            ProgressionConfig config = ProgressionConfig.defaults();

            CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                    progression, config, "mtg:common_bolt", CardRarity.COMMON, 1000
            );

            assertTrue(result.didTransmute());
            assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
            assertEquals(0, progression.getDuplicateCount(CardRarity.COMMON), "1000 % 4 must equal 0 remainder");
            assertEquals(250, progression.getWildcardCount(CardRarity.UNCOMMON), "1000 / 4 must grant 250 Uncommon Wildcards");
            assertEquals(250, result.newWildcardBalance());
            assertEquals(0, result.currentDuplicates());
        }

        @Test
        @DisplayName("Extreme bulk with initial remainder: 3 existing duplicates + 1,000 added -> 250 wildcards, 3 remainder")
        void testBulk1000CommonsWithInitialRemainder() {
            PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
            progression.setDuplicateCount(CardRarity.COMMON, 3);
            ProgressionConfig config = ProgressionConfig.defaults();

            CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                    progression, config, "mtg:common_bolt", CardRarity.COMMON, 1000
            );

            assertTrue(result.didTransmute());
            assertEquals(3, progression.getDuplicateCount(CardRarity.COMMON), "(3 + 1000) % 4 must equal 3 remainder");
            assertEquals(250, progression.getWildcardCount(CardRarity.UNCOMMON), "(3 + 1000) / 4 must grant 250 Uncommon Wildcards");
            assertEquals(250, result.newWildcardBalance());
            assertEquals(3, result.currentDuplicates());
        }

        @Test
        @DisplayName("Extreme bulk: 1,000,000 duplicate commons at once -> 250,000 Uncommon Wildcards, 0 remainder")
        void testBulk1MillionCommons() {
            PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
            ProgressionConfig config = ProgressionConfig.defaults();

            CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                    progression, config, "mtg:common_bolt", CardRarity.COMMON, 1_000_000
            );

            assertEquals(0, progression.getDuplicateCount(CardRarity.COMMON));
            assertEquals(250_000, progression.getWildcardCount(CardRarity.UNCOMMON));
        }

        @Test
        @DisplayName("Bulk duplicate transmutations across all rarity tiers")
        void testBulkAllRarities() {
            ProgressionConfig config = ProgressionConfig.defaults();

            // Uncommon -> Rare
            PlayerProgression p1 = new PlayerProgression(UUID.randomUUID());
            WildcardTransmutationEngine.processDuplicate(p1, config, "mtg:unc", CardRarity.UNCOMMON, 1000);
            assertEquals(250, p1.getWildcardCount(CardRarity.RARE));
            assertEquals(0, p1.getDuplicateCount(CardRarity.UNCOMMON));

            // Rare -> Mythic
            PlayerProgression p2 = new PlayerProgression(UUID.randomUUID());
            WildcardTransmutationEngine.processDuplicate(p2, config, "mtg:rare", CardRarity.RARE, 1000);
            assertEquals(250, p2.getWildcardCount(CardRarity.MYTHIC));
            assertEquals(0, p2.getDuplicateCount(CardRarity.RARE));

            // Mythic -> Mythic (Vault rollover)
            PlayerProgression p3 = new PlayerProgression(UUID.randomUUID());
            WildcardTransmutationEngine.processDuplicate(p3, config, "mtg:mythic", CardRarity.MYTHIC, 1000);
            assertEquals(250, p3.getWildcardCount(CardRarity.MYTHIC));
            assertEquals(0, p3.getDuplicateCount(CardRarity.MYTHIC));
        }

        @Test
        @DisplayName("Zero or negative copiesToAdd leaves state untouched without exceptions")
        void testZeroAndNegativeCopies() {
            PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
            progression.setDuplicateCount(CardRarity.COMMON, 2);
            ProgressionConfig config = ProgressionConfig.defaults();

            CardAwardResult r0 = WildcardTransmutationEngine.processDuplicate(
                    progression, config, "mtg:c", CardRarity.COMMON, 0
            );
            assertEquals(2, progression.getDuplicateCount(CardRarity.COMMON));
            assertEquals(0, progression.getWildcardCount(CardRarity.UNCOMMON));
            assertEquals(CardAwardResult.Type.DUPLICATE_ACCUMULATED, r0.type());

            CardAwardResult rNeg = WildcardTransmutationEngine.processDuplicate(
                    progression, config, "mtg:c", CardRarity.COMMON, -5
            );
            assertEquals(2, progression.getDuplicateCount(CardRarity.COMMON));
            assertEquals(0, progression.getWildcardCount(CardRarity.UNCOMMON));
            assertEquals(CardAwardResult.Type.DUPLICATE_ACCUMULATED, rNeg.type());
        }

        @Test
        @DisplayName("Null arguments in processDuplicate throw appropriate exceptions")
        void testNullArguments() {
            PlayerProgression p = new PlayerProgression(UUID.randomUUID());
            assertThrows(NullPointerException.class, () ->
                    WildcardTransmutationEngine.processDuplicate(null, ProgressionConfig.defaults(), "c", CardRarity.COMMON, 1)
            );
            assertThrows(NullPointerException.class, () ->
                    WildcardTransmutationEngine.processDuplicate(p, ProgressionConfig.defaults(), "c", null, 1)
            );
            // null config should safely fall back to defaults
            assertDoesNotThrow(() ->
                    WildcardTransmutationEngine.processDuplicate(p, null, "c", CardRarity.COMMON, 1)
            );
        }
    }

    // =========================================================================
    // 2. Concurrency & Double-Spend Attacks
    // =========================================================================
    @Nested
    @DisplayName("2. Concurrency & Double-Spend Attacks")
    class ConcurrencyAndDoubleSpendStress {

        @Test
        @DisplayName("Double-spend attack: 50 concurrent threads try to spend 1 Rare Wildcard on 50 different cards")
        void testDoubleSpendAttackDifferentCards() throws InterruptedException {
            ProgressionManager manager = new ProgressionManager();
            UUID playerId = UUID.randomUUID();
            PlayerProgression progression = manager.getProgression(playerId);
            progression.addWildcards(CardRarity.RARE, 1);

            int threadCount = 50;
            List<String> cardIds = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                String id = "mtg:stress_rare_" + i;
                cardIds.add(id);
                manager.registerCard(new CardDefinition(
                        id, "BASE", "Rare " + i, ManaCost.ZERO,
                        Set.of(CardType.INSTANT), Set.of(), CardRarity.RARE,
                        Optional.empty(), List.of(), 60, 0
                ));
            }

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final String targetCardId = cardIds.get(i);
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        WildcardRedemptionResult result = manager.redeemWildcard(playerId, CardRarity.RARE, targetCardId);
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            assertEquals(WildcardRedemptionResult.Status.INSUFFICIENT_WILDCARDS, result.status());
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown(); // Release all 50 threads at once
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Double-spend test timed out");
            executor.shutdown();

            assertEquals(1, successCount.get(), "EXACTLY 1 redemption must succeed!");
            assertEquals(threadCount - 1, failureCount.get(), "All other 49 attempts must fail!");
            assertEquals(0, progression.getWildcardCount(CardRarity.RARE), "Wildcard balance must end at exactly 0");
            assertEquals(1, progression.getUnlockedCards().size(), "Exactly 1 card must be unlocked");
        }

        @Test
        @DisplayName("Double redemption attack: 50 concurrent threads try to redeem the SAME card with 10 wildcards")
        void testDoubleRedemptionSameCard() throws InterruptedException {
            ProgressionManager manager = new ProgressionManager();
            UUID playerId = UUID.randomUUID();
            PlayerProgression progression = manager.getProgression(playerId);
            progression.addWildcards(CardRarity.RARE, 10);

            String targetCardId = "mtg:single_target_rare";
            manager.registerCard(new CardDefinition(
                    targetCardId, "BASE", "Target Rare", ManaCost.ZERO,
                    Set.of(CardType.INSTANT), Set.of(), CardRarity.RARE,
                    Optional.empty(), List.of(), 60, 0
            ));

            int threadCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger alreadyUnlockedCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        WildcardRedemptionResult result = manager.redeemWildcard(playerId, CardRarity.RARE, targetCardId);
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            assertEquals(WildcardRedemptionResult.Status.ALREADY_UNLOCKED, result.status());
                            alreadyUnlockedCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(1, successCount.get(), "Same card must only be redeemed ONCE");
            assertEquals(threadCount - 1, alreadyUnlockedCount.get());
            assertEquals(9, progression.getWildcardCount(CardRarity.RARE), "Only 1 wildcard may be consumed; 9 must remain");
            assertTrue(progression.hasCard(targetCardId));
        }

        @Test
        @DisplayName("Massive concurrent awards (generating wildcards) and redemptions (consuming wildcards)")
        void testConcurrentAwardsAndRedemptions() throws InterruptedException {
            ProgressionManager manager = new ProgressionManager();
            UUID playerId = UUID.randomUUID();

            // Register 1 common card and 200 rare cards
            manager.registerCard(new CardDefinition(
                    "mtg:dupe_common", "BASE", "Common", ManaCost.ZERO,
                    Set.of(CardType.INSTANT), Set.of(), CardRarity.RARE, // We will award this as RARE duplicates
                    Optional.empty(), List.of(), 60, 0
            ));
            // First award unlocks it
            manager.awardCard(playerId, "mtg:dupe_common", CardUnlockSource.MINING);

            int totalRedeemableCards = 200;
            for (int i = 0; i < totalRedeemableCards; i++) {
                manager.registerCard(new CardDefinition(
                        "mtg:redeemable_mythic_" + i, "BASE", "Mythic " + i, ManaCost.ZERO,
                        Set.of(CardType.INSTANT), Set.of(), CardRarity.MYTHIC,
                        Optional.empty(), List.of(), 60, 0
                ));
            }

            int producerThreads = 8;
            int duplicatesPerThread = 100; // 8 * 100 = 800 rare duplicates -> 200 Mythic Wildcards!
            int consumerThreads = 8;

            ExecutorService executor = Executors.newFixedThreadPool(producerThreads + consumerThreads);
            CountDownLatch doneLatch = new CountDownLatch(producerThreads + consumerThreads);
            AtomicInteger successfulRedemptions = new AtomicInteger(0);
            AtomicInteger cardIdIndex = new AtomicInteger(0);

            // Producers: award duplicates of mtg:dupe_common (RARE -> generates MYTHIC wildcards)
            for (int p = 0; p < producerThreads; p++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < duplicatesPerThread; i++) {
                            manager.awardCard(playerId, "mtg:dupe_common", CardUnlockSource.MINING);
                            Thread.yield();
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Consumers: attempt to redeem Mythic wildcards as they become available
            for (int c = 0; c < consumerThreads; c++) {
                executor.submit(() -> {
                    try {
                        while (true) {
                            int idx = cardIdIndex.get();
                            if (idx >= totalRedeemableCards) break;
                            if (cardIdIndex.compareAndSet(idx, idx + 1)) {
                                String cardId = "mtg:redeemable_mythic_" + idx;
                                // Spin-retry until either success or timeout
                                long start = System.currentTimeMillis();
                                while (System.currentTimeMillis() - start < 3000) {
                                    WildcardRedemptionResult res = manager.redeemWildcard(playerId, CardRarity.MYTHIC, cardId);
                                    if (res.isSuccess()) {
                                        successfulRedemptions.incrementAndGet();
                                        break;
                                    }
                                    Thread.yield();
                                }
                            }
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "Concurrent producer-consumer timed out");
            executor.shutdown();

            PlayerProgression prog = manager.getProgression(playerId);
            int remainingWildcards = prog.getWildcardCount(CardRarity.MYTHIC);
            int totalRedeemed = successfulRedemptions.get();

            // Total wildcards generated from 800 duplicates = 800 / 4 = 200.
            assertEquals(200, totalRedeemed + remainingWildcards,
                    "Conservation Law: Total Generated Wildcards (200) == Total Redeemed (" + totalRedeemed + ") + Remaining (" + remainingWildcards + ")");
            assertTrue(prog.getWildcardCount(CardRarity.MYTHIC) >= 0, "Wildcard balance cannot be negative");
        }

        @Test
        @DisplayName("Direct concurrent access on PlayerProgression: addWildcards and consumeWildcard")
        void testPlayerProgressionDirectConcurrentSafety() throws InterruptedException {
            PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
            int threads = 8;
            int amountsPerThread = 500;
            // Total wildcards to add = 4,000
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < amountsPerThread; i++) {
                            progression.addWildcards(CardRarity.RARE, 1);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(threads * amountsPerThread, progression.getWildcardCount(CardRarity.RARE));

            // Now consume all 4,000 concurrently across 8 threads
            ExecutorService consumerExecutor = Executors.newFixedThreadPool(threads);
            CountDownLatch consumerLatch = new CountDownLatch(threads);
            AtomicInteger consumedCount = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                consumerExecutor.submit(() -> {
                    try {
                        for (int i = 0; i < amountsPerThread; i++) {
                            if (progression.consumeWildcard(CardRarity.RARE, 1)) {
                                consumedCount.incrementAndGet();
                            }
                        }
                    } finally {
                        consumerLatch.countDown();
                    }
                });
            }

            assertTrue(consumerLatch.await(10, TimeUnit.SECONDS));
            consumerExecutor.shutdown();

            assertEquals(threads * amountsPerThread, consumedCount.get());
            assertEquals(0, progression.getWildcardCount(CardRarity.RARE));
        }
    }

    // =========================================================================
    // 3. ProgressionDropEngine Robustness & Edge Cases
    // =========================================================================
    @Nested
    @DisplayName("3. ProgressionDropEngine Robustness & Edge Cases")
    class ProgressionDropEngineRobustness {

        private final List<CardDefinition> sampleCards = List.of(
                new CardDefinition("mtg:c_white", "BASE", "White C", ManaCost.ofWhite(1), Set.of(CardType.INSTANT), Set.of(), CardRarity.COMMON, Optional.empty(), List.of(), 60, 0),
                new CardDefinition("mtg:r_black", "BASE", "Black R", ManaCost.ofBlack(3), Set.of(CardType.SORCERY), Set.of(), CardRarity.RARE, Optional.empty(), List.of(), 80, 0)
        );

        @Test
        @DisplayName("Graceful handling of unknown or modded block IDs")
        void testUnknownOrModdedBlocks() {
            ProgressionDropEngine engine = new ProgressionDropEngine(sampleCards);
            ProgressionConfig config = ProgressionConfig.defaults();

            String[] nonOres = {
                    "minecraft:stone", "minecraft:dirt", "minecraft:air", "minecraft:bedrock",
                    "techreborn:machine_casing", "create:brass_casing", "", null
            };

            for (String blockId : nonOres) {
                ProgressionDropEngine.DropRoll roll = engine.evaluateMiningDrop(blockId, config);
                assertFalse(roll.success(), "Non-ore block must not drop cards: " + blockId);
                assertTrue(roll.cardId().isEmpty());
                assertEquals(CardUnlockSource.MINING, roll.source());
            }

            // Unknown modded ores containing 'ore' keyword should evaluate cleanly
            ProgressionConfig guaranteed = new ProgressionConfig(
                    4, 7, 10, 1.5, 4, 4, 4, 4,
                    1.0, 1.0, 1.0, 1.0, 2, 5
            );
            String[] moddedOres = {
                    "create:zinc_ore", "thermal:tin_ore", "techreborn:iridium_ore",
                    "mysticalagriculture:inferium_ore", "custom_mod:ultra_deepslate_ore"
            };

            for (String oreId : moddedOres) {
                assertDoesNotThrow(() -> {
                    ProgressionDropEngine.DropRoll roll = engine.evaluateMiningDrop(oreId, guaranteed);
                    assertTrue(roll.success());
                    assertTrue(roll.cardId().isPresent());
                });
            }
        }

        @Test
        @DisplayName("Graceful handling of unknown or modded entity IDs")
        void testUnknownOrModdedEntities() {
            ProgressionDropEngine engine = new ProgressionDropEngine(sampleCards);
            ProgressionConfig guaranteed = new ProgressionConfig(
                    4, 7, 10, 1.5, 4, 4, 4, 4,
                    1.0, 1.0, 1.0, 1.0, 2, 5
            );

            String[] entities = {
                    "minecraft:pig", "minecraft:cow", "minecraft:armor_stand",
                    "alexsmobs:grizzly_bear", "twilightforest:naga", "twilightforest:ur_ghast",
                    "cataclysm:ender_guardian", "", null
            };

            for (String entityId : entities) {
                assertDoesNotThrow(() -> {
                    ProgressionDropEngine.DropRoll roll = engine.evaluateMobDrop(entityId, guaranteed);
                    assertNotNull(roll);
                });
            }
        }

        @Test
        @DisplayName("Graceful handling of unknown or modded crop IDs")
        void testUnknownOrModdedCrops() {
            ProgressionDropEngine engine = new ProgressionDropEngine(sampleCards);
            ProgressionConfig guaranteed = new ProgressionConfig(
                    4, 7, 10, 1.5, 4, 4, 4, 4,
                    1.0, 1.0, 1.0, 1.0, 2, 5
            );

            String[] crops = {
                    "minecraft:dirt", "minecraft:stone", "pamhc2crops:corn_crop",
                    "mysticalagriculture:inferium_crop", "", null
            };

            for (String cropId : crops) {
                assertDoesNotThrow(() -> {
                    ProgressionDropEngine.DropRoll roll = engine.evaluateFarmingDrop(cropId, guaranteed);
                    assertNotNull(roll);
                });
            }
        }

        @Test
        @DisplayName("Empty card registry does not crash and returns empty rolls")
        void testEmptyCardRegistryRobustness() {
            ProgressionDropEngine emptyEngine = new ProgressionDropEngine(List.of());
            ProgressionConfig guaranteed = new ProgressionConfig(
                    4, 7, 10, 1.5, 4, 4, 4, 4,
                    1.0, 1.0, 1.0, 1.0, 2, 5
            );

            ProgressionDropEngine.DropRoll miningRoll = emptyEngine.evaluateMiningDrop("minecraft:diamond_ore", guaranteed);
            assertFalse(miningRoll.success());
            assertTrue(miningRoll.cardId().isEmpty());

            ProgressionDropEngine.DropRoll farmingRoll = emptyEngine.evaluateFarmingDrop("minecraft:wheat", guaranteed);
            assertFalse(farmingRoll.success());
            assertTrue(farmingRoll.cardId().isEmpty());

            ProgressionDropEngine.DropRoll mobRoll = emptyEngine.evaluateMobDrop("minecraft:ender_dragon", guaranteed);
            assertFalse(mobRoll.success());
            assertTrue(mobRoll.cardId().isEmpty());

            List<String> dungeonLoot = emptyEngine.generateDungeonLoot("minecraft:ancient_city", guaranteed);
            assertTrue(dungeonLoot.isEmpty());
        }

        @Test
        @DisplayName("Single-card registry safely falls back regardless of rarity or color roll")
        void testSingleCardRegistryFallback() {
            CardDefinition loneCard = new CardDefinition(
                    "mtg:lone_card", "BASE", "Lone", ManaCost.ZERO,
                    Set.of(CardType.ARTIFACT), Set.of(), CardRarity.COMMON,
                    Optional.empty(), List.of(), 60, 0
            );
            ProgressionDropEngine singleEngine = new ProgressionDropEngine(List.of(loneCard));
            ProgressionConfig guaranteed = new ProgressionConfig(
                    4, 7, 10, 1.5, 4, 4, 4, 4,
                    1.0, 1.0, 1.0, 1.0, 2, 5
            );

            // Boss drop targets RARE or MYTHIC, but only COMMON exists
            ProgressionDropEngine.DropRoll mobRoll = singleEngine.evaluateMobDrop("minecraft:ender_dragon", guaranteed);
            assertTrue(mobRoll.success());
            assertEquals("mtg:lone_card", mobRoll.cardId().orElseThrow());

            List<String> loot = singleEngine.generateDungeonLoot("minecraft:ancient_city", guaranteed);
            assertEquals(guaranteed.dungeonChestCardCount(), loot.size());
            loot.forEach(cid -> assertEquals("mtg:lone_card", cid));
        }

        @Test
        @DisplayName("Multi-threaded stress on ProgressionDropEngine evaluation")
        void testDropEngineConcurrency() throws InterruptedException {
            ProgressionDropEngine engine = new ProgressionDropEngine(sampleCards);
            ProgressionConfig config = ProgressionConfig.defaults();

            int threads = 16;
            int iterationsPerThread = 2000;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < iterationsPerThread; i++) {
                            engine.evaluateMiningDrop("minecraft:iron_ore", config);
                            engine.evaluateFarmingDrop("minecraft:wheat", config);
                            engine.evaluateMobDrop("minecraft:zombie", config);
                            engine.generateDungeonLoot("minecraft:desert_pyramid", config);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent drop evaluations timed out");
            executor.shutdown();
        }
    }

    // =========================================================================
    // 4. Packet Serialization Fuzzing
    // =========================================================================
    @Nested
    @DisplayName("4. Packet Serialization Fuzzing & Corruption Suite")
    class PacketSerializationFuzzing {

        @Test
        @DisplayName("Truncated byte stream fuzzing across all packets")
        void testTruncatedPacketsFailCleanly() {
            // S2CUnlockCardPacket
            S2CUnlockCardPacket pUnlock = new S2CUnlockCardPacket(
                    "mtg:card_test", CardRarity.RARE, CardUnlockSource.MINING,
                    true, 2, 4, true, CardRarity.MYTHIC, 5
            );
            byte[] unlockBytes = pUnlock.encode();
            for (int len = 0; len < unlockBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(unlockBytes, len);
                assertThrows(IllegalArgumentException.class, () -> S2CUnlockCardPacket.decode(truncated),
                        "Truncated length " + len + " must fail with IllegalArgumentException");
            }

            // S2CSyncCollectionPacket
            S2CSyncCollectionPacket pSync = new S2CSyncCollectionPacket(
                    UUID.randomUUID(), Set.of("mtg:a", "mtg:b"),
                    Map.of(CardRarity.COMMON, 1), Map.of(CardRarity.UNCOMMON, 2)
            );
            byte[] syncBytes = pSync.encode();
            for (int len = 0; len < syncBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(syncBytes, len);
                assertThrows(IllegalArgumentException.class, () -> S2CSyncCollectionPacket.decode(truncated));
            }

            // C2SRedeemWildcardPacket
            C2SRedeemWildcardPacket pRedeem = new C2SRedeemWildcardPacket(CardRarity.RARE, "mtg:target_card");
            byte[] redeemBytes = pRedeem.encode();
            for (int len = 0; len < redeemBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(redeemBytes, len);
                assertThrows(IllegalArgumentException.class, () -> C2SRedeemWildcardPacket.decode(truncated));
            }

            // S2CSyncDeckPacket
            S2CSyncDeckPacket pDeck = new S2CSyncDeckPacket(
                    UUID.randomUUID(), 4, 0,
                    List.of(new S2CSyncDeckPacket.SlotSyncData(0, "mtg:c1", 0, 60, 0, 0))
            );
            byte[] deckBytes = pDeck.encode();
            for (int len = 0; len < deckBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(deckBytes, len);
                assertThrows(IllegalArgumentException.class, () -> S2CSyncDeckPacket.decode(truncated));
            }

            // C2SEquipCardPacket
            C2SEquipCardPacket pEquip = new C2SEquipCardPacket(1, "mtg:c1");
            byte[] equipBytes = pEquip.encode();
            for (int len = 0; len < equipBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(equipBytes, len);
                assertThrows(IllegalArgumentException.class, () -> C2SEquipCardPacket.decode(truncated));
            }

            // C2SCastCardPacket
            C2SCastCardPacket pCast = new C2SCastCardPacket(0, 42, 10.5, 64.0, -100.2);
            byte[] castBytes = pCast.encode();
            for (int len = 0; len < castBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(castBytes, len);
                assertThrows(IllegalArgumentException.class, () -> C2SCastCardPacket.decode(truncated));
            }

            // SelectInitialManaPacket
            SelectInitialManaPacket pMana = new SelectInitialManaPacket(ManaColor.RED);
            byte[] manaBytes = pMana.encode();
            for (int len = 0; len < manaBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(manaBytes, len);
                assertThrows(IllegalArgumentException.class, () -> SelectInitialManaPacket.decode(truncated));
            }

            // C2SUpgradeMaxManaPacket
            C2SUpgradeMaxManaPacket pUpMax = new C2SUpgradeMaxManaPacket(5);
            byte[] upMaxBytes = pUpMax.encode();
            for (int len = 0; len < upMaxBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(upMaxBytes, len);
                assertThrows(IllegalArgumentException.class, () -> C2SUpgradeMaxManaPacket.decode(truncated));
            }

            // C2SSelectCardPacket
            C2SSelectCardPacket pSel = new C2SSelectCardPacket(2);
            byte[] selBytes = pSel.encode();
            for (int len = 0; len < selBytes.length; len++) {
                byte[] truncated = Arrays.copyOf(selBytes, len);
                assertThrows(IllegalArgumentException.class, () -> C2SSelectCardPacket.decode(truncated));
            }

            // S2CSyncManaPacket
            byte[] poolBytes = new byte[45]; // Requires 46
            assertThrows(IllegalArgumentException.class, () -> S2CSyncManaPacket.decode(poolBytes));
        }

        @Test
        @DisplayName("Null byte arrays throw NullPointerException across all packets")
        void testNullByteArraysThrowNPE() {
            assertThrows(NullPointerException.class, () -> S2CUnlockCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CSyncCollectionPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SRedeemWildcardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CSyncDeckPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SEquipCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SCastCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> SelectInitialManaPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SUpgradeMaxManaPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SSelectCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CSyncManaPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SUpgradeCapacityPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2COpenManaPromptPacket.decode(null));
        }

        @Test
        @DisplayName("Random mutation fuzzing: bit flipping on encoded packets never crashes the JVM")
        void testRandomBitMutationFuzzing() {
            Random random = new Random(12345);
            C2SRedeemWildcardPacket packet = new C2SRedeemWildcardPacket(CardRarity.MYTHIC, "mtg:mythic_lotus");
            byte[] original = packet.encode();

            for (int i = 0; i < 1000; i++) {
                byte[] mutated = Arrays.copyOf(original, original.length);
                int bitIndex = random.nextInt(mutated.length * 8);
                mutated[bitIndex / 8] ^= (1 << (bitIndex % 8));

                try {
                    C2SRedeemWildcardPacket decoded = C2SRedeemWildcardPacket.decode(mutated);
                    assertNotNull(decoded);
                } catch (IllegalArgumentException expected) {
                    // Clean rejection is expected for corrupted packets
                }
            }
        }

        @Test
        @DisplayName("VULNERABILITY PROOF: S2CSyncDeckPacket unbounded size allocation triggers OutOfMemoryError")
        void testDeckSyncPacketUnboundedSizeOOMVulnerability() {
            ByteBuffer buffer = ByteBuffer.allocate(28);
            buffer.putLong(12345L); // msb
            buffer.putLong(67890L); // lsb
            buffer.putInt(4); // cap
            buffer.putInt(0); // sel
            buffer.putInt(1_000_000_000); // size: 1 billion slots!

            byte[] maliciousPayload = buffer.array();
            Throwable thrown = assertThrows(Throwable.class, () -> S2CSyncDeckPacket.decode(maliciousPayload));
            if (thrown instanceof OutOfMemoryError) {
                fail("VULNERABILITY CONFIRMED: S2CSyncDeckPacket.decode threw java.lang.OutOfMemoryError instead of IllegalArgumentException when receiving size=1,000,000,000", thrown);
            }
            assertInstanceOf(IllegalArgumentException.class, thrown, "Must reject with IllegalArgumentException");
        }

        @Test
        @DisplayName("VULNERABILITY PROOF: S2CSyncCollectionPacket unbounded cardCount allocation triggers OutOfMemoryError")
        void testCollectionPacketUnboundedCardCountOOM() {
            ByteBuffer buffer = ByteBuffer.allocate(23);
            buffer.putLong(12345L);
            buffer.putLong(67890L);
            buffer.putInt(1_000_000_000); // 1 billion cards
            buffer.putShort((short) 1); // string length 1
            buffer.put((byte) 'a'); // string "a"

            byte[] payload = buffer.array();
            Throwable thrown = assertThrows(Throwable.class, () -> S2CSyncCollectionPacket.decode(payload));
            if (thrown instanceof OutOfMemoryError) {
                fail("VULNERABILITY CONFIRMED: S2CSyncCollectionPacket.decode threw java.lang.OutOfMemoryError instead of IllegalArgumentException when receiving cardCount=1,000,000,000", thrown);
            }
            assertInstanceOf(IllegalArgumentException.class, thrown, "Must reject with IllegalArgumentException");
        }

        private void tryDecodeCatchingCleanly(Runnable decodeAction) {
            try {
                decodeAction.run();
            } catch (IllegalArgumentException | NullPointerException expected) {
                // Expected clean rejection
            } catch (OutOfMemoryError oom) {
                fail("VULNERABILITY: Packet decoder is vulnerable to OutOfMemoryError on untrusted input", oom);
            } catch (Throwable unexpected) {
                fail("Decoder threw unexpected throwable: " + unexpected.getClass().getName(), unexpected);
            }
        }

        @Test
        @DisplayName("Null strings and boundary values in packet construction encode and decode cleanly")
        void testNullAndBoundaryPacketValues() {
            // C2SRedeemWildcardPacket with nulls
            C2SRedeemWildcardPacket p1 = new C2SRedeemWildcardPacket(null, null);
            assertEquals(CardRarity.COMMON, p1.rarity());
            assertEquals("", p1.targetCardId());
            C2SRedeemWildcardPacket d1 = C2SRedeemWildcardPacket.decode(p1.encode());
            assertEquals(CardRarity.COMMON, d1.rarity());
            assertEquals("", d1.targetCardId());

            // S2CUnlockCardPacket with nulls
            S2CUnlockCardPacket p2 = new S2CUnlockCardPacket(null, null, null, false, 0, 4, false, null, 0);
            S2CUnlockCardPacket d2 = S2CUnlockCardPacket.decode(p2.encode());
            assertEquals("", d2.cardId());
            assertEquals(CardRarity.COMMON, d2.rarity());
            assertEquals(CardUnlockSource.MINING, d2.source());
            assertNull(d2.wildcardRarity());

            // C2SEquipCardPacket with null cardId
            C2SEquipCardPacket p3 = new C2SEquipCardPacket(0, null);
            C2SEquipCardPacket d3 = C2SEquipCardPacket.decode(p3.encode());
            assertNull(d3.cardId());

            // SelectInitialManaPacket with null color
            SelectInitialManaPacket p4 = new SelectInitialManaPacket(null);
            assertEquals(ManaColor.COLORLESS, p4.selectedColor());
            SelectInitialManaPacket d4 = SelectInitialManaPacket.decode(p4.encode());
            assertEquals(ManaColor.COLORLESS, d4.selectedColor());
        }
    }
}
