package com.mtg.test.mana;

import com.mtg.config.ManaConfig;
import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;
import com.mtg.mana.ManaCostEvaluator;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.mana.XpUpgradeCalculator;
import com.mtg.network.SelectInitialManaPacket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — Mana Subsystem Invariants, Concurrency & Arithmetic Boundaries")
public class ManaSubsystemAdversarialStressTest {

    @Nested
    @DisplayName("1. Total Mana Capacity Invariant Under Concurrency")
    class TotalManaCapacityInvariantStress {

        @Test
        @DisplayName("Stress: 16 concurrent threads rapidly adding and spending mana never violate totalMana <= maxMana")
        void testTotalManaCapacityInvariantUnderConcurrentAddAndSpend() throws InterruptedException {
            final int threads = 16;
            final int iterationsPerThread = 1_000;
            final int maxCapacity = 30;
            final ManaPool pool = new ManaPool(UUID.randomUUID(), maxCapacity);

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicBoolean invariantViolated = new AtomicBoolean(false);
            AtomicInteger totalAdded = new AtomicInteger(0);

            try {
                for (int t = 0; t < threads; t++) {
                    final int threadId = t;
                    executor.submit(() -> {
                        try {
                            Random rng = new Random(threadId * 31L + 17L);
                            ManaColor[] colors = ManaColor.values();
                            for (int i = 0; i < iterationsPerThread; i++) {
                                ManaColor color = colors[rng.nextInt(colors.length)];
                                if (rng.nextBoolean()) {
                                    int amount = rng.nextInt(10) + 1;
                                    int added = pool.addMana(color, amount);
                                    totalAdded.addAndGet(added);
                                } else {
                                    int amount = rng.nextInt(5) + 1;
                                    pool.removeMana(color, amount);
                                }

                                int currentTotal = pool.getTotalMana();
                                if (currentTotal > pool.getMaxMana() || currentTotal < 0) {
                                    invariantViolated.set(true);
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent stress test timed out");
                assertFalse(invariantViolated.get(), "totalMana exceeded maxMana or became negative during concurrent execution!");

                int finalTotal = pool.getTotalMana();
                assertTrue(finalTotal >= 0 && finalTotal <= pool.getMaxMana(),
                        "Final totalMana (" + finalTotal + ") must be between 0 and maxMana (" + pool.getMaxMana() + ")");

                int sumOfColors = 0;
                for (ManaColor c : ManaColor.values()) {
                    int bal = pool.get(c);
                    assertTrue(bal >= 0, "Color balance for " + c + " must not be negative: " + bal);
                    sumOfColors += bal;
                }
                assertEquals(sumOfColors, pool.getTotalMana(), "Sum of individual color balances must match getTotalMana()");
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("Stress: Concurrent setMaxMana reduction while threads add mana strictly preserves invariant")
        void testConcurrentCapacityReductionUnderLoad() throws InterruptedException {
            final int threads = 8;
            final ManaPool pool = new ManaPool(UUID.randomUUID(), 50);
            ExecutorService executor = Executors.newFixedThreadPool(threads + 1);
            CountDownLatch latch = new CountDownLatch(threads + 1);
            AtomicBoolean invariantViolated = new AtomicBoolean(false);

            try {
                // Thread 0 continuously fluctuates maxMana between 10 and 50
                executor.submit(() -> {
                    try {
                        Random rng = new Random(42);
                        for (int i = 0; i < 500; i++) {
                            int newCap = 10 + rng.nextInt(41);
                            pool.setMaxMana(newCap);
                            synchronized (pool) {
                                if (pool.getTotalMana() > pool.getMaxMana()) {
                                    invariantViolated.set(true);
                                }
                            }
                            Thread.yield();
                        }
                    } finally {
                        latch.countDown();
                    }
                });

                // Threads 1..8 continuously add mana
                for (int t = 1; t <= threads; t++) {
                    final int threadId = t;
                    executor.submit(() -> {
                        try {
                            Random rng = new Random(threadId * 100L);
                            ManaColor[] colors = ManaColor.values();
                            for (int i = 0; i < 500; i++) {
                                ManaColor color = colors[rng.nextInt(colors.length)];
                                pool.addMana(color, rng.nextInt(15) + 1);
                                synchronized (pool) {
                                    if (pool.getTotalMana() > pool.getMaxMana()) {
                                        invariantViolated.set(true);
                                    }
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(10, TimeUnit.SECONDS));
                assertFalse(invariantViolated.get(), "totalMana exceeded maxMana during concurrent capacity reduction!");
                assertTrue(pool.getTotalMana() <= pool.getMaxMana());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Nested
    @DisplayName("2. ManaCostEvaluator Greedy Solver & Concurrency Safety")
    class ManaCostEvaluatorStress {

        @Test
        @DisplayName("Greedy Solver: Generic cost consumes Colorless first, then colored mana")
        void testGenericCostPriorityColorlessFirst() {
            ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
            pool.addMana(ManaColor.COLORLESS, 3);
            pool.addMana(ManaColor.GREEN, 5);

            // Cost: {4}{G}
            ManaCost cost = new ManaCost(4, 0, 0, 0, 0, 1, 0, null);
            assertTrue(ManaCostEvaluator.canAfford(pool, cost));
            assertTrue(ManaCostEvaluator.deductCost(pool, cost));

            // Colorless: was 3, generic needed 4 -> 3 consumed from C, 1 consumed from G.
            // Required G: 1 consumed from G. Total G consumed: 2.
            assertEquals(0, pool.get(ManaColor.COLORLESS), "All colorless must be consumed first");
            assertEquals(3, pool.get(ManaColor.GREEN), "5 - 1(req) - 1(generic) = 3 green remaining");
            assertEquals(3, pool.getTotalMana());
        }

        @Test
        @DisplayName("Greedy Solver: Deterministic tie-breaking across equal surplus balances (GREEN -> RED -> BLACK -> BLUE -> WHITE)")
        void testDeterministicTieBreakingOrder() {
            // Scenario 1: All 5 colors have balance 2. Generic cost {1}.
            // GREEN has highest priority, so 1 GREEN should be deducted.
            ManaPool pool1 = new ManaPool(UUID.randomUUID(), 20);
            pool1.addMana(ManaColor.GREEN, 2);
            pool1.addMana(ManaColor.RED, 2);
            pool1.addMana(ManaColor.BLACK, 2);
            pool1.addMana(ManaColor.BLUE, 2);
            pool1.addMana(ManaColor.WHITE, 2);

            assertTrue(ManaCostEvaluator.deductCost(pool1, ManaCost.ofGeneric(1)));
            assertEquals(1, pool1.get(ManaColor.GREEN), "GREEN must break tie first");
            assertEquals(2, pool1.get(ManaColor.RED));
            assertEquals(2, pool1.get(ManaColor.BLACK));
            assertEquals(2, pool1.get(ManaColor.BLUE));
            assertEquals(2, pool1.get(ManaColor.WHITE));

            // Scenario 2: All 5 colors have balance 2. Generic cost {2}.
            // GREEN deducted first, then RED (next tie-breaker).
            ManaPool pool2 = new ManaPool(UUID.randomUUID(), 20);
            pool2.addMana(ManaColor.GREEN, 2);
            pool2.addMana(ManaColor.RED, 2);
            pool2.addMana(ManaColor.BLACK, 2);
            pool2.addMana(ManaColor.BLUE, 2);
            pool2.addMana(ManaColor.WHITE, 2);

            assertTrue(ManaCostEvaluator.deductCost(pool2, ManaCost.ofGeneric(2)));
            assertEquals(1, pool2.get(ManaColor.GREEN));
            assertEquals(1, pool2.get(ManaColor.RED));
            assertEquals(2, pool2.get(ManaColor.BLACK));
            assertEquals(2, pool2.get(ManaColor.BLUE));
            assertEquals(2, pool2.get(ManaColor.WHITE));
        }

        @Test
        @DisplayName("Greedy Solver: Insufficient mana must return false and make zero mutations")
        void testInsufficientManaLeavesPoolUntouched() {
            ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
            pool.addMana(ManaColor.WHITE, 2);
            pool.addMana(ManaColor.BLUE, 1);

            // Requires 2 White and 2 Generic, but surplus is only 1 Blue
            ManaCost cost = new ManaCost(2, 2, 0, 0, 0, 0, 0, null);
            assertFalse(ManaCostEvaluator.canAfford(pool, cost));
            assertFalse(ManaCostEvaluator.deductCost(pool, cost));

            assertEquals(2, pool.get(ManaColor.WHITE), "White must remain untouched");
            assertEquals(1, pool.get(ManaColor.BLUE), "Blue must remain untouched");
            assertEquals(3, pool.getTotalMana());
        }

        @Test
        @DisplayName("Concurrency Vulnerability Check: Deterministic TOCTOU double-spend in deductCost")
        void testDeterministicToctouDoubleSpend() throws Exception {
            ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
            pool.addMana(ManaColor.WHITE, 1);

            CountDownLatch checkedLatch = new CountDownLatch(1);
            CountDownLatch drainLatch = new CountDownLatch(1);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                // Thread 1 checks affordability, then waits for external drain before deducting
                Future<Boolean> thread1Deduct = executor.submit(() -> {
                    // Step 1: Thread 1 checks canAfford (Time-of-Check)
                    boolean canAfford = ManaCostEvaluator.canAfford(pool, ManaCost.ofWhite(1));
                    if (!canAfford) {
                        return false;
                    }
                    // Signal that affordability check succeeded
                    checkedLatch.countDown();
                    // Wait for Thread 2 to drain the mana before deductCost is called
                    if (!drainLatch.await(2, TimeUnit.SECONDS)) {
                        return false;
                    }
                    // Step 2: Thread 1 attempts to deduct cost (Time-of-Use)
                    return ManaCostEvaluator.deductCost(pool, ManaCost.ofWhite(1));
                });

                // Wait until Thread 1 has checked canAfford
                assertTrue(checkedLatch.await(2, TimeUnit.SECONDS));

                // Thread 2 drains the mana while Thread 1 is between canAfford and deductCost
                assertTrue(pool.removeMana(ManaColor.WHITE, 1));
                assertEquals(0, pool.get(ManaColor.WHITE));

                // Release Thread 1 to proceed to deductCost
                drainLatch.countDown();

                boolean thread1Success = thread1Deduct.get(2, TimeUnit.SECONDS);

                // Thread 1 must NOT succeed because Thread 2 already spent the White mana!
                assertFalse(thread1Success,
                        "TOCTOU double-spend vulnerability: Thread 1 returned true from deductCost() after the mana was already drained by Thread 2!");
                assertEquals(0, pool.get(ManaColor.WHITE));
                assertEquals(0, pool.getTotalMana());
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("Concurrency Vulnerability Check: Concurrent deductCost must NOT allow double-spending")
        void testConcurrentDeductCostDoubleSpendPrevention() throws InterruptedException {
            final int threads = 10;
            final ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
            pool.addMana(ManaColor.WHITE, 1); // Pool only has 1 White mana!

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);

            try {
                for (int i = 0; i < threads; i++) {
                    executor.submit(() -> {
                        readyLatch.countDown();
                        try {
                            startLatch.await();
                            if (ManaCostEvaluator.deductCost(pool, ManaCost.ofWhite(1))) {
                                successCount.incrementAndGet();
                            }
                        } catch (InterruptedException ignored) {}
                    });
                }

                assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
                startLatch.countDown(); // Release all threads simultaneously
                executor.shutdown();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

                // Exactly ONE thread should have successfully spent the 1 White mana!
                assertEquals(1, successCount.get(),
                        "Double-spend detected! Exactly 1 thread must succeed when pool has only 1 White mana, but "
                                + successCount.get() + " succeeded.");
                assertEquals(0, pool.get(ManaColor.WHITE));
                assertEquals(0, pool.getTotalMana());
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("Concurrency Vulnerability Check: Concurrent generic deductCost must NOT allow double-spending")
        void testConcurrentGenericDeductCostDoubleSpendPrevention() throws InterruptedException {
            final int threads = 10;
            final ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
            pool.addMana(ManaColor.COLORLESS, 5); // Pool only has 5 Colorless mana!

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);

            try {
                for (int i = 0; i < threads; i++) {
                    executor.submit(() -> {
                        readyLatch.countDown();
                        try {
                            startLatch.await();
                            if (ManaCostEvaluator.deductCost(pool, ManaCost.ofGeneric(5))) {
                                successCount.incrementAndGet();
                            }
                        } catch (InterruptedException ignored) {}
                    });
                }

                assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
                startLatch.countDown();
                executor.shutdown();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

                assertEquals(1, successCount.get(),
                        "Double-spend on generic mana detected! Exactly 1 thread must succeed, but "
                                + successCount.get() + " succeeded.");
                assertEquals(0, pool.get(ManaColor.COLORLESS));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Nested
    @DisplayName("3. Arithmetic Overflow & Extreme Value Boundaries")
    class ArithmeticOverflowStress {

        @Test
        @DisplayName("Arithmetic Overflow: Extreme values in ManaPool.set() and addMana() clamp safely without wrapping")
        void testExtremeValuesClampSafely() {
            ManaPool pool = new ManaPool(UUID.randomUUID(), 20);

            int added = pool.addMana(ManaColor.WHITE, Integer.MAX_VALUE);
            assertEquals(20, added, "addMana with Integer.MAX_VALUE must clamp to remaining capacity 20");
            assertEquals(20, pool.get(ManaColor.WHITE));
            assertEquals(20, pool.getTotalMana());

            pool.set(ManaColor.BLUE, Integer.MAX_VALUE);
            // Since total is already 20, availableCapacity for BLUE is 0
            assertEquals(0, pool.get(ManaColor.BLUE));
            assertEquals(20, pool.getTotalMana());
        }

        @Test
        @DisplayName("Arithmetic Overflow: Multi-argument ManaPool constructor with huge sums must clamp to maxMana")
        void testMultiArgConstructorSumOverflow() {
            UUID id = UUID.randomUUID();
            // Passing 1,000,000,000 for 3 colors sums to 3 billion which overflows a 32-bit signed int to negative
            ManaPool pool = new ManaPool(id, 1_000_000_000, 1_000_000_000, 1_000_000_000, 0, 0, 0, 20, false, null);

            assertTrue(pool.getTotalMana() >= 0, "Total mana must never be negative due to integer overflow");
            assertTrue(pool.getTotalMana() <= pool.getMaxMana(),
                    "Total mana (" + pool.getTotalMana() + ") must not exceed maxMana (" + pool.getMaxMana() + ")");
        }

        @Test
        @DisplayName("XP Upgrade Calculator: Extreme tier levels or maxMana values do not wrap to negative")
        void testXpUpgradeCalculatorExtremeValues() {
            // At hard cap, cost is -1
            assertEquals(-1, XpUpgradeCalculator.getNextUpgradeCost(100));
            assertEquals(-1, XpUpgradeCalculator.getNextUpgradeCost(105));

            // With extreme max mana below hard cap
            int cost = XpUpgradeCalculator.getNextUpgradeCost(95);
            assertTrue(cost > 0, "Upgrade cost at level 95 must be positive: " + cost);

            // New max mana calculation
            int newMax = XpUpgradeCalculator.calculateNewMaxMana(95, 100, 5);
            assertEquals(100, newMax);

            // Step exceeding hard cap clamps strictly to hardCap
            int clampedMax = XpUpgradeCalculator.calculateNewMaxMana(98, 100, 10);
            assertEquals(100, clampedMax);
        }

        @Test
        @DisplayName("ManaCost record rejects negative values at construction")
        void testManaCostNegativeRejection() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ManaCost(-1, 0, 0, 0, 0, 0, 0, null)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new ManaCost(0, -1, 0, 0, 0, 0, 0, null)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new ManaCost(0, 0, 0, 0, 0, 0, -5, null)
            );
        }
    }

    @Nested
    @DisplayName("4. Replay Attacks on Initial Mana Selection")
    class InitialManaReplayAttackStress {

        @Test
        @DisplayName("Replay Attack: Calling selectInitialMana multiple times sequentially is rejected")
        void testSequentialReplayAttack() {
            ManaManager manager = new ManaManager();
            UUID playerId = UUID.randomUUID();

            assertTrue(manager.selectInitialMana(playerId, ManaColor.GREEN));
            assertTrue(manager.hasChosenInitialMana(playerId));

            // Immediate replay attack with different color
            assertFalse(manager.selectInitialMana(playerId, ManaColor.RED),
                    "Replay attack must be rejected when initial mana has already been chosen");
            assertEquals(ManaColor.GREEN, manager.getPlayerMana(playerId).getChosenAffinity());
            int expectedSeed = manager.getPlayerMana(playerId).getMaxMana();
            assertEquals(expectedSeed, manager.getPlayerMana(playerId).get(ManaColor.GREEN));
            assertEquals(0, manager.getPlayerMana(playerId).get(ManaColor.RED));
            assertEquals(expectedSeed, manager.getPlayerMana(playerId).getTotalMana());
        }

        @Test
        @DisplayName("Replay Attack: Concurrent multi-threaded selectInitialMana allows exactly one winner")
        void testConcurrentReplayAttack() throws InterruptedException {
            final int threads = 16;
            final ManaManager manager = new ManaManager();
            final UUID playerId = UUID.randomUUID();

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);

            try {
                ManaColor[] colors = ManaColor.values();
                for (int i = 0; i < threads; i++) {
                    final ManaColor color = colors[i % colors.length];
                    executor.submit(() -> {
                        readyLatch.countDown();
                        try {
                            startLatch.await();
                            if (manager.selectInitialMana(playerId, color)) {
                                successCount.incrementAndGet();
                            }
                        } catch (InterruptedException ignored) {}
                    });
                }

                assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
                startLatch.countDown();
                executor.shutdown();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

                // Exactly ONE thread must win the initial selection race
                assertEquals(1, successCount.get(),
                        "Concurrent replay attack permitted multiple initial selections! Successes: " + successCount.get());

                ManaPool pool = manager.getPlayerMana(playerId);
                assertTrue(pool.hasChosenInitialAffinity());
                assertEquals(pool.getMaxMana(), pool.getTotalMana(),
                        "Seed mana must equal initial max mana cap, no duplicate seeding permitted");
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("Replay Attack: Network packet decoding resilience")
        void testNetworkPacketReplayDecoding() {
            SelectInitialManaPacket packet = new SelectInitialManaPacket(ManaColor.BLUE);
            byte[] encoded = packet.encode();

            SelectInitialManaPacket decoded1 = SelectInitialManaPacket.decode(encoded);
            SelectInitialManaPacket decoded2 = SelectInitialManaPacket.decode(encoded);

            assertEquals(ManaColor.BLUE, decoded1.selectedColor());
            assertEquals(ManaColor.BLUE, decoded2.selectedColor());
        }
    }
}
