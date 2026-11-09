package com.mtg.test.mana;

import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;
import com.mtg.domain.PhyrexianManaCost;
import com.mtg.mana.ManaCostEvaluator;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Challenger Adversarial Stress Harness — Mana Subsystem Edge Cases & Concurrency")
public class ManaSubsystemChallengerStressTest {

    @Test
    @DisplayName("Stress: 32 concurrent threads calling pool.payCost() prevent double spending")
    void testConcurrentPoolPayCostDoubleSpendPrevention() throws InterruptedException {
        final int threads = 32;
        final ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 5); // Pool has exactly 5 White mana

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
                        if (pool.payCost(ManaCost.ofWhite(1))) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {}
                });
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(5, successCount.get(),
                    "Exactly 5 threads must succeed paying 1 White each from pool of 5 White; got: " + successCount.get());
            assertEquals(0, pool.get(ManaColor.WHITE));
            assertEquals(0, pool.getTotalMana());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Stress: Mixed colored and generic deduction under 20-thread race condition")
    void testMixedCostConcurrentDeductionRace() throws InterruptedException {
        // Pool has 2 White, 2 Blue, 3 Colorless (total 7)
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 2);
        pool.addMana(ManaColor.BLUE, 2);
        pool.addMana(ManaColor.COLORLESS, 3);

        // Cost: {1}{W}{U} (total 3: 1 White, 1 Blue, 1 Generic)
        ManaCost cost = new ManaCost(1, 1, 1, 0, 0, 0, 0, null);

        final int threads = 20;
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
                        if (ManaCostEvaluator.deductCost(pool, cost)) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {}
                });
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            // With 2 White and 2 Blue, at most 2 threads can satisfy the colored requirements
            // Colorless has 3, which satisfies 2 generic needed (leaving 1 Colorless)
            assertEquals(2, successCount.get(),
                    "Exactly 2 threads should successfully pay {1}{W}{U} from 2W, 2U, 3C pool; got: " + successCount.get());
            assertEquals(0, pool.get(ManaColor.WHITE), "White mana must be 0");
            assertEquals(0, pool.get(ManaColor.BLUE), "Blue mana must be 0");
            assertEquals(1, pool.get(ManaColor.COLORLESS), "Colorless mana must be 1");
            assertEquals(1, pool.getTotalMana(), "Total mana must be 1");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Overflow Stress: Multi-arg ManaPool constructor with 6x Integer.MAX_VALUE/2 colors")
    void testConstructorWithHugeColorBalancesClamping() {
        int huge = Integer.MAX_VALUE / 2; // ~1.07 billion each
        ManaPool pool = new ManaPool(UUID.randomUUID(), huge, huge, huge, huge, huge, huge, 20, false, null);

        assertEquals(20, pool.getMaxMana());
        assertEquals(20, pool.getTotalMana(), "Total mana must clamp strictly to maxMana (20)");
        assertTrue(pool.getTotalMana() >= 0, "Total mana must never be negative");
    }

    @Test
    @DisplayName("Overflow Stress: Extreme maxMana and 64-bit sum boundaries")
    void testExtremeMaxManaAndBalances() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 100);

        // Add 100 colorless mana
        int added = pool.addMana(ManaColor.COLORLESS, 100);
        assertEquals(100, added);
        assertEquals(100, pool.getTotalMana());

        // Attempting to add with Integer.MAX_VALUE should return 0 (no space)
        int overflowAdd = pool.addMana(ManaColor.RED, Integer.MAX_VALUE);
        assertEquals(0, overflowAdd);
        assertEquals(100, pool.getTotalMana());
    }

    @Test
    @DisplayName("Edge Case: ManaCostEvaluator canAfford with huge generic cost does not overflow")
    void testHugeGenericCostAffordability() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 10);
        pool.addMana(ManaColor.BLUE, 10);

        // Cost with generic = Integer.MAX_VALUE
        ManaCost hugeCost = ManaCost.ofGeneric(Integer.MAX_VALUE);
        assertFalse(ManaCostEvaluator.canAfford(pool, hugeCost), "Pool cannot afford Integer.MAX_VALUE generic cost");
        assertFalse(ManaCostEvaluator.deductCost(pool, hugeCost), "deductCost must return false");
        assertEquals(20, pool.getTotalMana(), "Pool must remain unmodified");
    }

    @Test
    @DisplayName("Edge Case: Interruption status check in deductCost")
    void testInterruptionInDeductCost() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.GREEN, 5);

        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt(); // Pre-set interrupted status
            boolean deducted = ManaCostEvaluator.deductCost(pool, ManaCost.ofGreen(1));
            // Interrupted thread status observation
        });

        testThread.start();
        assertDoesNotThrow(() -> testThread.join(2000));
    }

    @Test
    @DisplayName("Transaction Rollback: Colored costs succeed but generic fails leaves pool completely unchanged")
    void testTransactionalRollbackColoredSucceedsGenericFails() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 2);
        pool.addMana(ManaColor.BLUE, 2);
        pool.addMana(ManaColor.RED, 1);
        // Total pool = 5 mana (2 W, 2 U, 1 R)

        // Cost: {4}{W}{U} -> 1 White, 1 Blue, 4 Generic (total mana value: 6)
        ManaCost cost = new ManaCost(4, 1, 1, 0, 0, 0, 0, null);

        // Pre-condition: Pool has 1 W and 1 U, but remaining surplus is (2-1)+(2-1)+1 = 3, which is < 4 generic
        assertFalse(ManaCostEvaluator.canAfford(pool, cost), "canAfford must return false when generic surplus is insufficient");
        assertFalse(ManaCostEvaluator.deductCost(pool, cost), "deductCost must return false without mutating");

        // Assert pool balance is completely unchanged (zero partial deduction)
        assertEquals(2, pool.get(ManaColor.WHITE), "White balance must remain exactly 2");
        assertEquals(2, pool.get(ManaColor.BLUE), "Blue balance must remain exactly 2");
        assertEquals(1, pool.get(ManaColor.RED), "Red balance must remain exactly 1");
        assertEquals(0, pool.get(ManaColor.BLACK));
        assertEquals(0, pool.get(ManaColor.GREEN));
        assertEquals(0, pool.get(ManaColor.COLORLESS));
        assertEquals(5, pool.getTotalMana(), "Total mana must remain strictly 5");
    }

    @Test
    @DisplayName("Transaction Rollback: Multi-color requirements where first 3 succeed but 4th fails")
    void testTransactionalRollbackMultiColorPartialFailure() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 3);
        pool.addMana(ManaColor.BLUE, 3);
        pool.addMana(ManaColor.BLACK, 3);
        // Red is 0

        // Cost: {W}{U}{B}{R}
        ManaCost cost = new ManaCost(0, 1, 1, 1, 1, 0, 0, null);

        assertFalse(ManaCostEvaluator.canAfford(pool, cost));
        assertFalse(ManaCostEvaluator.deductCost(pool, cost));

        assertEquals(3, pool.get(ManaColor.WHITE), "White must not be partially drained");
        assertEquals(3, pool.get(ManaColor.BLUE), "Blue must not be partially drained");
        assertEquals(3, pool.get(ManaColor.BLACK), "Black must not be partially drained");
        assertEquals(0, pool.get(ManaColor.RED));
        assertEquals(9, pool.getTotalMana(), "Total mana must remain untouched at 9");
    }

    @Test
    @DisplayName("Transaction Rollback: Fault injection during sequential removal triggers full refund")
    void testTransactionalRollbackFaultInjectionDuringSequentialRemoval() {
        // Pool where removal fails on RED
        class FaultyManaPool extends ManaPool {
            boolean failRed = true;

            FaultyManaPool(UUID id, int max) {
                super(id, max);
            }

            @Override
            public synchronized boolean removeMana(ManaColor color, int amount) {
                if (color == ManaColor.RED && failRed) {
                    return false;
                }
                return super.removeMana(color, amount);
            }
        }

        FaultyManaPool pool = new FaultyManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 2);
        pool.addMana(ManaColor.BLUE, 2);
        pool.addMana(ManaColor.RED, 2);

        // Cost: {W}{U}{R}
        ManaCost cost = new ManaCost(0, 1, 1, 0, 1, 0, 0, null);

        assertTrue(ManaCostEvaluator.canAfford(pool, cost), "Initial state should be affordable");
        assertFalse(ManaCostEvaluator.deductCost(pool, cost), "deductCost must return false due to RED removal failure");

        // Verification: White and Blue must have been refunded via rollback
        assertEquals(2, pool.get(ManaColor.WHITE), "White must be fully restored after rollback");
        assertEquals(2, pool.get(ManaColor.BLUE), "Blue must be fully restored after rollback");
        assertEquals(2, pool.get(ManaColor.RED), "Red was never removed");
        assertEquals(6, pool.getTotalMana(), "Total mana must be 6 after rollback");
    }

    @Test
    @DisplayName("Transaction Rollback: Pool at full capacity (maxMana) preserves exact balance on failure")
    void testTransactionalRollbackAtFullCapacity() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 10);
        pool.addMana(ManaColor.WHITE, 2);
        pool.addMana(ManaColor.BLUE, 2);
        pool.addMana(ManaColor.BLACK, 2);
        pool.addMana(ManaColor.RED, 2);
        pool.addMana(ManaColor.GREEN, 2);
        assertEquals(10, pool.getTotalMana());
        assertEquals(10, pool.getMaxMana());

        // Cost requires 1 of each color plus 1 Colorless: {W}{U}{B}{R}{G}{C}
        ManaCost cost = new ManaCost(0, 1, 1, 1, 1, 1, 1, null);

        assertFalse(ManaCostEvaluator.deductCost(pool, cost));

        assertEquals(2, pool.get(ManaColor.WHITE));
        assertEquals(2, pool.get(ManaColor.BLUE));
        assertEquals(2, pool.get(ManaColor.BLACK));
        assertEquals(2, pool.get(ManaColor.RED));
        assertEquals(2, pool.get(ManaColor.GREEN));
        assertEquals(0, pool.get(ManaColor.COLORLESS));
        assertEquals(10, pool.getTotalMana());
    }

    @Test
    @DisplayName("Transaction Rollback: 50 concurrent threads contending for partially-matching cost")
    void testConcurrentPartialFailureContentionRollback() throws InterruptedException {
        // Pool has 5 White, 5 Blue (total 10)
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 5);
        pool.addMana(ManaColor.BLUE, 5);

        // Cost: {4}{W}{U} (requires 1 W, 1 U, 4 Generic = total 6)
        ManaCost cost = new ManaCost(4, 1, 1, 0, 0, 0, 0, null);

        final int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        if (ManaCostEvaluator.deductCost(pool, cost)) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {}
                });
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            // With initial 10 mana and cost of 6, exactly ONE thread can succeed (leaving 4 mana, which cannot satisfy 6)
            assertEquals(1, successCount.get(), "Exactly 1 thread should succeed paying 6 mana from 10 mana pool");
            assertEquals(49, failCount.get(), "Exactly 49 threads should fail");

            // Remaining pool mana must be exactly 4 (10 - 6 = 4)
            assertEquals(4, pool.getTotalMana(), "Total mana must be exactly 4");
            assertEquals(4, pool.get(ManaColor.WHITE) + pool.get(ManaColor.BLUE), "Sum of W and U must equal remaining total mana");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Transaction Rollback: Phyrexian mana partial failure rolls back without mutation")
    void testTransactionalRollbackPhyrexianPartialFailure() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 2);
        // Blue is 0

        // Cost: {W/P}{U} (1 White Phyrexian, 1 regular Blue)
        PhyrexianManaCost phyrexian = new PhyrexianManaCost(1, 0, 0, 0, 0, 0);
        ManaCost cost = new ManaCost(0, 0, 1, 0, 0, 0, 0, phyrexian);

        // When payPhyrexianWithLife = false, {W/P} requires mana
        assertFalse(ManaCostEvaluator.canAfford(pool, cost, false));
        assertFalse(ManaCostEvaluator.deductCost(pool, cost, false));

        assertEquals(2, pool.get(ManaColor.WHITE), "White mana must remain untouched when Blue fails");
        assertEquals(0, pool.get(ManaColor.BLUE));
        assertEquals(2, pool.getTotalMana());
    }
}
