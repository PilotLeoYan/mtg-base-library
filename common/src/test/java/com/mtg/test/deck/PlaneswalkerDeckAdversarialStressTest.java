package com.mtg.test.deck;

import com.mtg.deck.DeckCapacityUpgradeCalculator;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — PlaneswalkerDeck State Machine, Bounds & Concurrency")
public class PlaneswalkerDeckAdversarialStressTest {

    @Nested
    @DisplayName("1. Out-of-Bounds Slot Selection & Bounds Checking")
    class OutOfBoundsStress {

        @Test
        @DisplayName("Selection Bounds: Invalid slot indices are rejected without altering state")
        void testOutOfBoundsSlotSelectionRejection() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);
            assertEquals(0, deck.getSelectedSlot());

            // Negative indices
            assertFalse(deck.setSelectedSlot(-1));
            assertFalse(deck.setSelectedSlot(-100));
            assertFalse(deck.setSelectedSlot(Integer.MIN_VALUE));
            assertEquals(0, deck.getSelectedSlot(), "Selection must not change on negative index");

            // At or beyond unlocked capacity (capacity is 4, valid is 0..3)
            assertFalse(deck.setSelectedSlot(4));
            assertFalse(deck.setSelectedSlot(5));
            assertFalse(deck.setSelectedSlot(6));
            assertFalse(deck.setSelectedSlot(7));
            assertFalse(deck.setSelectedSlot(100));
            assertFalse(deck.setSelectedSlot(Integer.MAX_VALUE));
            assertEquals(0, deck.getSelectedSlot(), "Selection must not change on locked or out-of-range index");

            // Valid selections within capacity
            assertTrue(deck.setSelectedSlot(3));
            assertEquals(3, deck.getSelectedSlot());
            assertTrue(deck.setSelectedSlot(1));
            assertEquals(1, deck.getSelectedSlot());
        }

        @Test
        @DisplayName("Direct Slot Access: getSlot() out of range throws IndexOutOfBoundsException")
        void testGetSlotOutOfBoundsException() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

            assertThrows(IndexOutOfBoundsException.class, () -> deck.getSlot(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> deck.getSlot(7));
            assertThrows(IndexOutOfBoundsException.class, () -> deck.getSlot(100));
            assertThrows(IndexOutOfBoundsException.class, () -> deck.getSlot(Integer.MIN_VALUE));
            assertThrows(IndexOutOfBoundsException.class, () -> deck.getSlot(Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Rapid Cycling: 100,000 cycles forward and backward wrap deterministically")
        void testRapidSlotCyclingDeterminism() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

            for (int i = 0; i < 100_000; i++) {
                deck.cycleSlot(true);
                int sel = deck.getSelectedSlot();
                assertTrue(sel >= 0 && sel < 4, "Selection out of bounds after forward cycle: " + sel);
            }

            for (int i = 0; i < 100_000; i++) {
                deck.cycleSlot(false);
                int sel = deck.getSelectedSlot();
                assertTrue(sel >= 0 && sel < 4, "Selection out of bounds after backward cycle: " + sel);
            }

            // Single capacity deck cycling always stays at 0
            PlaneswalkerDeck singleSlotDeck = new PlaneswalkerDeck(UUID.randomUUID(), 1);
            for (int i = 0; i < 100; i++) {
                singleSlotDeck.cycleSlot(true);
                assertEquals(0, singleSlotDeck.getSelectedSlot());
                singleSlotDeck.cycleSlot(false);
                assertEquals(0, singleSlotDeck.getSelectedSlot());
            }
        }
    }

    @Nested
    @DisplayName("2. Locked Slot Defense & Capacity Shrinking")
    class LockedSlotDefenseStress {

        @Test
        @DisplayName("Locked Slots: Equipping into locked slot index (4..6) throws IllegalStateException")
        void testEquipIntoLockedSlotThrows() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

            // Slots 0..3 are unlocked, 4..6 are locked
            for (int locked = 4; locked < 7; locked++) {
                final int slot = locked;
                assertThrows(IllegalStateException.class, () ->
                        deck.equipCard(slot, "mtg:lightning_bolt", 40, 0),
                        "Equipping into locked slot " + slot + " must throw IllegalStateException"
                );
            }
        }

        @Test
        @DisplayName("Capacity Shrink: Reducing capacity immediately locks previously active slots and adjusts selection")
        void testCapacityShrinkLocksSlotsAndAdjustsSelection() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);
            deck.equipCard(2, "mtg:shock", 20, 0);
            deck.equipCard(3, "mtg:fireball", 40, 0);
            deck.setSelectedSlot(3);
            assertEquals(3, deck.getSelectedSlot());

            // Shrink capacity from 4 down to 2
            assertTrue(deck.setCapacity(2));
            assertEquals(2, deck.getCapacity());

            // Selection was 3, must be pulled back to max available slot (index 1)
            assertEquals(1, deck.getSelectedSlot(), "Selection must adjust downwards to capacity - 1");

            // Slot 2 and 3 are now LOCKED
            assertEquals(DeckSlotState.LOCKED, deck.getSlot(2).getState(deck.getCapacity()));
            assertEquals(DeckSlotState.LOCKED, deck.getSlot(3).getState(deck.getCapacity()));
            assertFalse(deck.getSlot(2).canCast(deck.getCapacity()), "Locked slot must not be castable");
            assertFalse(deck.getSlot(3).canCast(deck.getCapacity()), "Locked slot must not be castable");

            // Attempting to select slot 2 or 3 now fails
            assertFalse(deck.setSelectedSlot(2));
            assertFalse(deck.setSelectedSlot(3));
        }

        @Test
        @DisplayName("Capacity Boundary Clamping: Enforces strictly [1, 7]")
        void testCapacityClampingBoundaries() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

            deck.setCapacity(-50);
            assertEquals(PlaneswalkerDeck.MIN_CAPACITY, deck.getCapacity());

            deck.setCapacity(0);
            assertEquals(PlaneswalkerDeck.MIN_CAPACITY, deck.getCapacity());

            deck.setCapacity(100);
            assertEquals(PlaneswalkerDeck.MAX_CAPACITY, deck.getCapacity());

            deck.setCapacity(Integer.MAX_VALUE);
            assertEquals(PlaneswalkerDeck.MAX_CAPACITY, deck.getCapacity());
        }
    }

    @Nested
    @DisplayName("3. Rapid State Transitions & Delays")
    class RapidStateTransitionsStress {

        @Test
        @DisplayName("State Lifecycle: EMPTY -> READY -> WINDUP -> COOLDOWN -> READY")
        void testFullSlotStateCycle() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);
            DeckCardSlot slot0 = deck.getSlot(0);

            // Initial: EMPTY
            assertEquals(DeckSlotState.EMPTY, slot0.getState(deck.getCapacity()));
            assertFalse(slot0.canCast(deck.getCapacity()));

            // Equip: transitions to READY
            deck.equipCard(0, "mtg:wrath_of_god", 50, 5);
            assertEquals(DeckSlotState.READY, slot0.getState(deck.getCapacity()));
            assertTrue(slot0.canCast(deck.getCapacity()));

            // Start windup (channeling delay)
            deck.startWindup(0, 5);
            assertEquals(DeckSlotState.WINDUP, slot0.getState(deck.getCapacity()));
            assertFalse(slot0.canCast(deck.getCapacity()));
            assertEquals(1.0f, slot0.getWindupProgress(), 0.001f);

            // Tick through windup
            for (int t = 5; t > 1; t--) {
                deck.tick();
                assertEquals(DeckSlotState.WINDUP, slot0.getState(deck.getCapacity()));
                assertTrue(slot0.getWindupProgress() > 0.0f && slot0.getWindupProgress() <= 1.0f);
            }
            deck.tick(); // Windup finishes
            assertEquals(0, slot0.getWindupRemainingTicks());

            // Start cooldown
            deck.startCooldown(0, 10);
            assertEquals(DeckSlotState.COOLDOWN, slot0.getState(deck.getCapacity()));
            assertFalse(slot0.canCast(deck.getCapacity()));

            // Tick through cooldown
            for (int t = 10; t > 1; t--) {
                deck.tick();
                assertEquals(DeckSlotState.COOLDOWN, slot0.getState(deck.getCapacity()));
                assertTrue(slot0.getCooldownProgress() > 0.0f && slot0.getCooldownProgress() <= 1.0f);
            }
            deck.tick(); // Cooldown finishes
            assertEquals(0, slot0.getCooldownRemainingTicks());
            assertEquals(DeckSlotState.READY, slot0.getState(deck.getCapacity()));
            assertTrue(slot0.canCast(deck.getCapacity()));

            // Unequip: transitions back to EMPTY
            assertTrue(deck.unequipCard(0));
            assertEquals(DeckSlotState.EMPTY, slot0.getState(deck.getCapacity()));
        }

        @Test
        @DisplayName("Progress Clamping: Extreme tick inputs maintain progress in [0.0, 1.0]")
        void testProgressClampingUnderExtremeTicks() {
            DeckCardSlot slot = new DeckCardSlot(0);
            slot.equip("mtg:counterspell", 100, 20);

            // Zero ticks
            assertEquals(0.0f, slot.getCooldownProgress());
            assertEquals(0.0f, slot.getWindupProgress());

            // Large tick values
            slot.startCooldown(Integer.MAX_VALUE);
            assertTrue(slot.getCooldownProgress() >= 0.0f && slot.getCooldownProgress() <= 1.0f);

            slot.startWindup(Integer.MAX_VALUE);
            assertTrue(slot.getWindupProgress() >= 0.0f && slot.getWindupProgress() <= 1.0f);
        }
    }

    @Nested
    @DisplayName("4. High-Concurrency Multi-Threaded Stress")
    class ConcurrencyStress {

        @Test
        @DisplayName("Stress: 16 concurrent threads ticking, equipping, unequipping, and cycling do not deadlock or throw")
        void testConcurrentDeckOperations() throws InterruptedException {
            final int threads = 16;
            final int opsPerThread = 2_000;
            final PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 7);

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicBoolean errorDetected = new AtomicBoolean(false);

            try {
                for (int t = 0; t < threads; t++) {
                    final int threadId = t;
                    executor.submit(() -> {
                        try {
                            Random rng = new Random(threadId * 7919L + 31L);
                            for (int i = 0; i < opsPerThread; i++) {
                                int op = rng.nextInt(6);
                                switch (op) {
                                    case 0 -> deck.tick();
                                    case 1 -> deck.cycleSlot(rng.nextBoolean());
                                    case 2 -> {
                                        int slot = rng.nextInt(deck.getCapacity());
                                        deck.equipCard(slot, "mtg:card_" + threadId, 20, 5);
                                    }
                                    case 3 -> {
                                        int slot = rng.nextInt(deck.getCapacity());
                                        deck.unequipCard(slot);
                                    }
                                    case 4 -> {
                                        int slot = rng.nextInt(deck.getCapacity());
                                        deck.startCooldown(slot, 10);
                                    }
                                    case 5 -> {
                                        int sel = deck.getSelectedSlot();
                                        DeckCardSlot data = deck.getSelectedSlotData();
                                        assertNotNull(data);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            errorDetected.set(true);
                            e.printStackTrace();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(15, TimeUnit.SECONDS), "Concurrent deck stress test timed out!");
                assertFalse(errorDetected.get(), "Exception detected during concurrent deck operations!");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Nested
    @DisplayName("5. Deck Capacity XP Upgrades & Hard Cap")
    class DeckCapacityUpgradeStress {

        @Test
        @DisplayName("Upgrade Ladder: 4 -> 5 -> 6 -> 7 and hard cap rejection at 7")
        void testCapacityUpgradeProgression() {
            PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

            assertEquals(10, DeckCapacityUpgradeCalculator.calculateUpgradeCost(4));
            assertTrue(deck.upgradeCapacity());
            assertEquals(5, deck.getCapacity());

            assertEquals(15, DeckCapacityUpgradeCalculator.calculateUpgradeCost(5));
            assertTrue(deck.upgradeCapacity());
            assertEquals(6, deck.getCapacity());

            assertEquals(23, DeckCapacityUpgradeCalculator.calculateUpgradeCost(6));
            assertTrue(deck.upgradeCapacity());
            assertEquals(7, deck.getCapacity());

            // At hard cap 7: cost is -1 and upgradeCapacity() returns false
            assertEquals(-1, DeckCapacityUpgradeCalculator.calculateUpgradeCost(7));
            assertFalse(deck.upgradeCapacity(), "Upgrading at MAX_CAPACITY (7) must return false");
            assertEquals(7, deck.getCapacity());

            assertFalse(DeckCapacityUpgradeCalculator.canUpgrade(7, 1000));
        }

        @Test
        @DisplayName("Insufficient XP rejects capacity upgrade")
        void testInsufficientXpRejected() {
            assertFalse(DeckCapacityUpgradeCalculator.canUpgrade(4, 9)); // Requires 10
            assertTrue(DeckCapacityUpgradeCalculator.canUpgrade(4, 10));
            assertFalse(DeckCapacityUpgradeCalculator.canUpgrade(4, -5));
        }
    }
}
