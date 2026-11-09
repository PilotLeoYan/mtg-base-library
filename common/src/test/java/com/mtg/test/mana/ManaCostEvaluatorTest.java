package com.mtg.test.mana;

import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;
import com.mtg.mana.ManaCostEvaluator;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Subsystem — ManaCostEvaluator Solving & Atomic Deduction")
public class ManaCostEvaluatorTest {

    @Test
    @DisplayName("Exact colored payment: pool with {R} satisfies and pays {R}")
    void testExactColoredCost() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.RED, 1);

        ManaCost cost = ManaCost.ofRed(1);
        assertTrue(ManaCostEvaluator.canAfford(pool, cost));

        assertTrue(ManaCostEvaluator.deductCost(pool, cost));
        assertEquals(0, pool.get(ManaColor.RED));
        assertEquals(0, pool.getTotalMana());
    }

    @Test
    @DisplayName("Generic mana paid by Colorless first before touching colored mana")
    void testGenericPaidByColorless() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.COLORLESS, 2);
        pool.addMana(ManaColor.GREEN, 2);

        // Cost: {2}{G}
        ManaCost cost = new ManaCost(2, 0, 0, 0, 0, 1, 0, null);
        assertTrue(ManaCostEvaluator.canAfford(pool, cost));

        assertTrue(ManaCostEvaluator.deductCost(pool, cost));
        assertEquals(0, pool.get(ManaColor.COLORLESS), "Colorless mana must be consumed first for generic");
        assertEquals(1, pool.get(ManaColor.GREEN), "Exactly 1 Green mana must remain");
    }

    @Test
    @DisplayName("Generic mana paid by surplus colored mana when Colorless is absent")
    void testGenericPaidBySurplusColoredMana() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.BLUE, 2);
        pool.addMana(ManaColor.GREEN, 1);

        // Cost: {1}{U}{G}
        ManaCost cost = new ManaCost(1, 0, 1, 0, 0, 1, 0, null);
        assertTrue(ManaCostEvaluator.canAfford(pool, cost));

        assertTrue(ManaCostEvaluator.deductCost(pool, cost));
        assertEquals(0, pool.get(ManaColor.GREEN));
        assertEquals(0, pool.get(ManaColor.BLUE)); // 1 for U, 1 for generic {1}
        assertEquals(0, pool.getTotalMana());
    }

    @Test
    @DisplayName("Insufficient colored mana rejects payment without mutating pool")
    void testInsufficientColoredManaRejectsWithoutMutation() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 2);

        ManaCost cost = ManaCost.ofBlue(1);
        assertFalse(ManaCostEvaluator.canAfford(pool, cost));

        assertFalse(ManaCostEvaluator.deductCost(pool, cost));
        assertEquals(2, pool.get(ManaColor.WHITE), "White balance must remain 2 after failed deduction");
        assertEquals(2, pool.getTotalMana());
    }

    @Test
    @DisplayName("Insufficient generic mana rejects payment without mutating pool")
    void testInsufficientGenericManaRejectsWithoutMutation() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.RED, 1);

        ManaCost cost = ManaCost.ofGeneric(2);
        assertFalse(ManaCostEvaluator.canAfford(pool, cost));

        assertFalse(ManaCostEvaluator.deductCost(pool, cost));
        assertEquals(1, pool.get(ManaColor.RED));
        assertEquals(1, pool.getTotalMana());
    }

    @Test
    @DisplayName("Zero mana cost is always affordable and causes no mutation")
    void testZeroManaCostAlwaysAffordable() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        assertTrue(ManaCostEvaluator.canAfford(pool, ManaCost.ZERO));
        assertTrue(ManaCostEvaluator.deductCost(pool, ManaCost.ZERO));
        assertEquals(0, pool.getTotalMana());
    }

    @Test
    @DisplayName("Deduction priority consumes highest balance with GREEN tie-breaker")
    void testDeductionPriorityTieBreaker() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.GREEN, 1);
        pool.addMana(ManaColor.RED, 1);
        pool.addMana(ManaColor.BLACK, 1);
        pool.addMana(ManaColor.BLUE, 1);
        pool.addMana(ManaColor.WHITE, 1);

        ManaCost cost = ManaCost.ofGeneric(1);
        assertTrue(ManaCostEvaluator.deductCost(pool, cost));

        // GREEN has highest tie-break priority
        assertEquals(0, pool.get(ManaColor.GREEN), "Green should be consumed first under tie-break");
        assertEquals(1, pool.get(ManaColor.RED));
        assertEquals(1, pool.get(ManaColor.BLACK));
        assertEquals(1, pool.get(ManaColor.BLUE));
        assertEquals(1, pool.get(ManaColor.WHITE));
    }

    @Test
    @DisplayName("Atomic transactional deduction: zero mutation if generic cost cannot be satisfied despite colored match")
    void testAtomicDeductionPreCalculationZeroMutationsWhenGenericFails() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 2);
        pool.addMana(ManaColor.BLUE, 1);

        // Requires 2 White and 2 Generic, but surplus is only 1 Blue
        ManaCost cost = new ManaCost(2, 2, 0, 0, 0, 0, 0, null);
        assertFalse(ManaCostEvaluator.canAfford(pool, cost));
        assertFalse(ManaCostEvaluator.deductCost(pool, cost));

        // Invariant: Zero partial drain
        assertEquals(2, pool.get(ManaColor.WHITE), "White must remain completely untouched");
        assertEquals(1, pool.get(ManaColor.BLUE), "Blue must remain completely untouched");
        assertEquals(3, pool.getTotalMana());
    }

    @Test
    @DisplayName("Atomic transactional deduction: full rollback if mid-deduction removal fails")
    void testTransactionalDeductionRollbackOnRemovalFailure() {
        class FlakyManaPool extends ManaPool {
            boolean failBlue = true;

            FlakyManaPool(UUID id, int max) {
                super(id, max);
            }

            @Override
            public synchronized boolean removeMana(ManaColor color, int amount) {
                if (color == ManaColor.BLUE && failBlue) {
                    return false;
                }
                return super.removeMana(color, amount);
            }
        }

        FlakyManaPool pool = new FlakyManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.WHITE, 1);
        pool.addMana(ManaColor.BLUE, 1);

        // Cost: {W}{U}
        ManaCost cost = new ManaCost(0, 1, 1, 0, 0, 0, 0, null);
        assertTrue(ManaCostEvaluator.canAfford(pool, cost));

        // deductCost encounters failure on Blue and must rollback White
        assertFalse(ManaCostEvaluator.deductCost(pool, cost));

        // Pool must have both White and Blue restored
        assertEquals(1, pool.get(ManaColor.WHITE), "White must be refunded via rollback");
        assertEquals(1, pool.get(ManaColor.BLUE), "Blue was never drained");
        assertEquals(2, pool.getTotalMana(), "Total mana must be unchanged");
    }
}
