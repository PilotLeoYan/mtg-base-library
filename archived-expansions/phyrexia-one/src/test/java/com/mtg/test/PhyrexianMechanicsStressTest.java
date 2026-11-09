package com.mtg.test.adversarial;

import com.mtg.api.mechanics.one.*;
import com.mtg.api.trigger.CombatDamageEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.KeywordInstance;
import com.mtg.domain.ManaCost;
import com.mtg.domain.PhyrexianManaCost;
import com.mtg.api.keyword.KeywordDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — Phyrexian Mechanics, SBA, Corrupted & Proliferate")
public class PhyrexianMechanicsStressTest {

    private PoisonCounterManager poisonManager;
    private TriggerBus bus;

    @BeforeEach
    void setup() {
        poisonManager = new PoisonCounterManager();
        bus = new TriggerBus();
    }

    // =========================================================================
    // 1. Lethal SBA Threshold Transitions & Boundaries
    // =========================================================================

    @Test
    @DisplayName("SBA Lethal Threshold: 0 to 9 is non-lethal, exactly 10 is lethal, 11+ remains lethal")
    void testLethalSbaThresholdTransitions() {
        UUID player = UUID.randomUUID();

        for (int i = 0; i < 10; i++) {
            poisonManager.setPoison(player, i);
            assertEquals(i, poisonManager.getPoison(player));
            assertFalse(poisonManager.isLethal(player), "Poison count " + i + " must not be lethal under standard 10-counter SBA");
        }

        poisonManager.setPoison(player, 10);
        assertTrue(poisonManager.isLethal(player), "Poison count 10 must trigger lethal SBA");

        poisonManager.addPoison(player, 1, null, bus);
        assertEquals(11, poisonManager.getPoison(player));
        assertTrue(poisonManager.isLethal(player), "Poison count 11 must remain lethal");

        poisonManager.setPoison(player, 100);
        assertTrue(poisonManager.isLethal(player), "Poison count 100 must remain lethal");
    }

    @Test
    @DisplayName("SBA Lethal Threshold: Entity elimination, reset and cleanup restores non-lethal state")
    void testEntityEliminationAndReset() {
        UUID victim = UUID.randomUUID();
        poisonManager.setPoison(victim, 10);
        assertTrue(poisonManager.isLethal(victim));

        // Reset simulates entity respawn or match reset
        poisonManager.reset(victim);
        assertEquals(0, poisonManager.getPoison(victim));
        assertFalse(poisonManager.isLethal(victim));

        // Multi-entity batch cleanup
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        poisonManager.setPoison(playerA, 12);
        poisonManager.setPoison(playerB, 15);
        assertTrue(poisonManager.isLethal(playerA));
        assertTrue(poisonManager.isLethal(playerB));

        poisonManager.clearAll();
        assertEquals(0, poisonManager.getPoison(playerA));
        assertEquals(0, poisonManager.getPoison(playerB));
        assertFalse(poisonManager.isLethal(playerA));
        assertFalse(poisonManager.isLethal(playerB));
    }

    @Test
    @DisplayName("SBA Configurable Threshold: Custom threshold bounds and disabled toggle enforcement")
    void testCustomThresholdAndDisabledLethalityToggle() {
        UUID entity = UUID.randomUUID();

        // Custom threshold = 5
        poisonManager.setPoison(entity, 4);
        assertFalse(poisonManager.isLethal(entity, 5, true));
        poisonManager.setPoison(entity, 5);
        assertTrue(poisonManager.isLethal(entity, 5, true));

        // Custom threshold = 15
        poisonManager.setPoison(entity, 14);
        assertFalse(poisonManager.isLethal(entity, 15, true));
        poisonManager.setPoison(entity, 15);
        assertTrue(poisonManager.isLethal(entity, 15, true));

        // When toggle is disabled (false), even 999 poison counters is NOT lethal
        poisonManager.setPoison(entity, 999);
        assertFalse(poisonManager.isLethal(entity, 10, false), "When poison lethality is disabled, isLethal must always return false");
        assertFalse(poisonManager.isLethal(entity, 1, false));
    }

    @Test
    @DisplayName("Poison Counter Bounds: Negative increments and set values are clamped to non-negative")
    void testNegativePoisonProtection() {
        UUID entity = UUID.randomUUID();

        // setPoison with negative count clamps to 0
        poisonManager.setPoison(entity, -10);
        assertEquals(0, poisonManager.getPoison(entity));

        // addPoison with negative amount does not reduce existing counters
        poisonManager.setPoison(entity, 5);
        int afterAdd = poisonManager.addPoison(entity, -3, null, bus);
        assertEquals(5, afterAdd);
        assertEquals(5, poisonManager.getPoison(entity));
    }

    @Test
    @DisplayName("Poison Counter Null Safety: Null entityId parameters do not throw NPE")
    void testNullEntityHandling() {
        assertDoesNotThrow(() -> assertEquals(0, poisonManager.getPoison(null)));
        assertDoesNotThrow(() -> poisonManager.setPoison(null, 10));
        assertDoesNotThrow(() -> assertEquals(0, poisonManager.addPoison(null, 5, null, bus)));
        assertDoesNotThrow(() -> assertFalse(poisonManager.isLethal(null)));
        assertDoesNotThrow(() -> poisonManager.reset(null));
    }

    @Test
    @DisplayName("Poison Event Dispatch: Only positive additions post PoisonAddedEvent")
    void testPoisonAddedEventDispatchRules() {
        AtomicInteger eventCount = new AtomicInteger(0);
        bus.registerListener(PoisonAddedEvent.class, event -> eventCount.incrementAndGet());

        UUID entity = UUID.randomUUID();
        UUID source = UUID.randomUUID();

        // Positive addition posts event
        poisonManager.addPoison(entity, 2, source, bus);
        assertEquals(1, eventCount.get());

        // Zero addition does NOT post event
        poisonManager.addPoison(entity, 0, source, bus);
        assertEquals(1, eventCount.get(), "Adding 0 poison must not fire PoisonAddedEvent");

        // Negative addition does NOT post event
        poisonManager.addPoison(entity, -4, source, bus);
        assertEquals(1, eventCount.get(), "Adding negative poison must not fire PoisonAddedEvent");

        // Without bus does not fail
        poisonManager.addPoison(entity, 3, source, null);
        assertEquals(1, eventCount.get());
    }

    // =========================================================================
    // 2. Corrupted Ability Word Evaluations
    // =========================================================================

    @Test
    @DisplayName("Corrupted Evaluation: Exact integer threshold transitions (2 is false, 3 is true)")
    void testCorruptedThresholdTransitions() {
        assertFalse(CorruptedEvaluator.isCorrupted(-5));
        assertFalse(CorruptedEvaluator.isCorrupted(0));
        assertFalse(CorruptedEvaluator.isCorrupted(1));
        assertFalse(CorruptedEvaluator.isCorrupted(2));
        assertTrue(CorruptedEvaluator.isCorrupted(3), "Corrupted must be active at exactly 3 counters");
        assertTrue(CorruptedEvaluator.isCorrupted(4));
        assertTrue(CorruptedEvaluator.isCorrupted(10));
    }

    @Test
    @DisplayName("Corrupted Evaluation: Null safety and multi-opponent collection resilience")
    void testCorruptedNullAndCollectionSafety() {
        UUID opponent = UUID.randomUUID();
        poisonManager.setPoison(opponent, 3);

        // Null target or manager returns false
        assertFalse(CorruptedEvaluator.isCorrupted(null, poisonManager));
        assertFalse(CorruptedEvaluator.isCorrupted(opponent, null));
        assertFalse(CorruptedEvaluator.isCorrupted(null, null));

        // Collection safety
        assertFalse(CorruptedEvaluator.hasAnyCorruptedOpponent(null, poisonManager));
        assertFalse(CorruptedEvaluator.hasAnyCorruptedOpponent(Collections.emptyList(), poisonManager));
        assertFalse(CorruptedEvaluator.hasAnyCorruptedOpponent(List.of(opponent), null));

        // Collection containing null UUIDs does not throw NPE
        List<UUID> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(UUID.randomUUID());
        assertFalse(CorruptedEvaluator.hasAnyCorruptedOpponent(listWithNulls, poisonManager));

        // Mixed collection with one corrupted opponent returns true
        listWithNulls.add(opponent);
        assertTrue(CorruptedEvaluator.hasAnyCorruptedOpponent(listWithNulls, poisonManager));
    }

    @Test
    @DisplayName("Corrupted State Lifecycle: Clean -> Corrupted -> Cleared lifecycle")
    void testCorruptedLifecycleStateTransitions() {
        UUID target = UUID.randomUUID();
        assertFalse(CorruptedEvaluator.isCorrupted(target, poisonManager));

        poisonManager.addPoison(target, 2, null, bus);
        assertFalse(CorruptedEvaluator.isCorrupted(target, poisonManager));

        poisonManager.addPoison(target, 1, null, bus); // Reaches 3
        assertTrue(CorruptedEvaluator.isCorrupted(target, poisonManager));

        // Reset clears corrupted status
        poisonManager.reset(target);
        assertFalse(CorruptedEvaluator.isCorrupted(target, poisonManager));
    }

    // =========================================================================
    // 3. Multi-Target Proliferate & Counter Synchronization
    // =========================================================================

    private static class TestCounterHolder implements ICounterHolder {
        private final UUID id = UUID.randomUUID();
        private final Map<String, Integer> counters = new LinkedHashMap<>();

        TestCounterHolder(Map<String, Integer> initial) {
            if (initial != null) counters.putAll(initial);
        }

        @Override public UUID getHolderId() { return id; }
        @Override public Map<String, Integer> getCounters() { return counters; }
        @Override public int getCounter(String type) { return counters.getOrDefault(type, 0); }
        @Override public void addCounter(String type, int count) {
            counters.put(type, counters.getOrDefault(type, 0) + count);
        }
    }

    @Test
    @DisplayName("Proliferate: Increments all positive counter types across multiple distinct targets")
    void testMultiTargetProliferateExecution() {
        // Target 1: Creature with +1/+1 and loyalty
        TestCounterHolder target1 = new TestCounterHolder(Map.of("+1/+1", 2, "loyalty", 4));

        // Target 2: Player with poison and shield
        TestCounterHolder target2 = new TestCounterHolder(Map.of("poison", 3, "shield", 1));

        // Target 3: Clean entity with 0 counters
        TestCounterHolder target3 = new TestCounterHolder(Collections.emptyMap());

        // Target 4: Entity with a counter of value 0 (should not be proliferated)
        TestCounterHolder target4 = new TestCounterHolder(Map.of("stun", 0));

        AtomicInteger eventFired = new AtomicInteger(0);
        bus.registerListener(ProliferateEvent.class, e -> eventFired.incrementAndGet());

        poisonManager.setPoison(target2.getHolderId(), 3);

        Map<UUID, Map<String, Integer>> report = ProliferateExecutor.proliferate(
            List.of(target1, target2, target3, target4),
            poisonManager,
            bus
        );

        // Target 1 checks
        assertEquals(3, target1.getCounter("+1/+1"));
        assertEquals(5, target1.getCounter("loyalty"));

        // Target 2 checks
        assertEquals(4, target2.getCounter("poison"));
        assertEquals(2, target2.getCounter("shield"));

        // PoisonCounterManager synchronized with holder
        assertEquals(4, poisonManager.getPoison(target2.getHolderId()), "Proliferate must synchronize poisonManager counters");

        // Target 3 & 4 checks
        assertEquals(0, target3.getCounters().size());
        assertEquals(0, target4.getCounter("stun"), "Counter with initial 0 value must not proliferate");

        // Report verification
        assertTrue(report.containsKey(target1.getHolderId()));
        assertTrue(report.containsKey(target2.getHolderId()));
        assertFalse(report.containsKey(target3.getHolderId()));
        assertFalse(report.containsKey(target4.getHolderId()));

        assertEquals(1, eventFired.get(), "ProliferateEvent must fire exactly once");
    }

    @Test
    @DisplayName("Proliferate Null & Empty Safety: Null collection or empty list returns empty report without exception")
    void testProliferateNullAndEmptyInputs() {
        AtomicInteger eventFired = new AtomicInteger(0);
        bus.registerListener(ProliferateEvent.class, e -> eventFired.incrementAndGet());

        Map<UUID, Map<String, Integer>> r1 = ProliferateExecutor.proliferate(null, poisonManager, bus);
        assertNotNull(r1);
        assertTrue(r1.isEmpty());

        Map<UUID, Map<String, Integer>> r2 = ProliferateExecutor.proliferate(Collections.emptyList(), poisonManager, bus);
        assertNotNull(r2);
        assertTrue(r2.isEmpty());

        // Target with empty counters
        TestCounterHolder emptyTarget = new TestCounterHolder(Collections.emptyMap());
        Map<UUID, Map<String, Integer>> r3 = ProliferateExecutor.proliferate(List.of(emptyTarget), poisonManager, bus);
        assertTrue(r3.isEmpty());

        assertEquals(0, eventFired.get(), "ProliferateEvent must not fire when zero counters were proliferated");
    }

    // =========================================================================
    // 4. Phyrexian Mana Life Payment Checks & Safety
    // =========================================================================

    @Test
    @DisplayName("Phyrexian Mana: Multi-color cost calculation and non-negative constructor validation")
    void testPhyrexianManaCostCalculations() {
        // Constructor validation
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(-1, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, -1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, 0, 0, -1));

        // 6-color Phyrexian mana combination
        PhyrexianManaCost cost = new PhyrexianManaCost(1, 2, 0, 1, 0, 1);
        assertEquals(5, cost.totalPhyrexianMana());
        assertEquals(10, cost.totalLifeCost());
        assertEquals(10.0f, cost.calculateDamage());
        assertEquals(15.0f, cost.calculateDamage(3.0f));
    }

    @Test
    @DisplayName("Phyrexian Mana Life Payment: Exact life boundary rule (health > damage, exact is disallowed)")
    void testPhyrexianLifePaymentExactBoundaries() {
        PhyrexianManaCost oneMana = PhyrexianManaCost.ofBlack(1); // 2.0f damage

        // Healthy player (20.0f)
        assertTrue(oneMana.canPayWithLife(20.0f));

        // Player with 3.0f health can afford 2.0f damage (survives at 1.0f)
        assertTrue(oneMana.canPayWithLife(3.0f));

        // Player with 2.001f health survives with fractional health
        assertTrue(oneMana.canPayWithLife(2.001f));

        // Player with exactly 2.0f health CANNOT pay (2.0 > 2.0 is false, would be lethal)
        assertFalse(oneMana.canPayWithLife(2.0f), "Player with exactly 2.0f health cannot pay 2.0f damage as it causes death");

        // Player with 1.999f health CANNOT pay
        assertFalse(oneMana.canPayWithLife(1.999f));

        // Player with 0 or negative health CANNOT pay
        assertFalse(oneMana.canPayWithLife(0.0f));
        assertFalse(oneMana.canPayWithLife(-5.0f));

        // Custom damage per mana check
        // E.g. damage per mana = 4.0f (2 full hearts)
        assertFalse(oneMana.canPayWithLife(4.0f, 4.0f)); // Exact match fails
        assertTrue(oneMana.canPayWithLife(4.5f, 4.0f)); // Above threshold succeeds
        assertFalse(oneMana.canPayWithLife(3.5f, 4.0f)); // Below threshold fails
    }

    // =========================================================================
    // 5. Toxic Combat Damage Trigger Listener Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Toxic Listener: Canceled events, 0 damage, and negative damage do NOT inflict poison")
    void testToxicCombatDamageAdversarialInputs() {
        ToxicCombatDamageListener listener = new ToxicCombatDamageListener(poisonManager, bus);
        bus.registerListener(CombatDamageEvent.class, listener);

        UUID attacker = UUID.randomUUID();
        UUID defender = UUID.randomUUID();
        IToxic toxic3 = () -> 3;

        // Canceled event
        CombatDamageEvent canceledEvent = new CombatDamageEvent(this, toxic3, null, attacker, defender, 5.0f, true);
        canceledEvent.setCanceled(true);
        bus.post(canceledEvent);
        assertEquals(0, poisonManager.getPoison(defender), "Canceled combat event must not inflict poison");

        // Zero damage
        CombatDamageEvent zeroDamage = new CombatDamageEvent(this, toxic3, null, attacker, defender, 0.0f, true);
        bus.post(zeroDamage);
        assertEquals(0, poisonManager.getPoison(defender), "0 damage combat event must not inflict poison");

        // Negative damage
        CombatDamageEvent negDamage = new CombatDamageEvent(this, toxic3, null, attacker, defender, -2.0f, true);
        bus.post(negDamage);
        assertEquals(0, poisonManager.getPoison(defender), "Negative damage combat event must not inflict poison");

        // Attacker with toxic value 0
        IToxic toxic0 = () -> 0;
        CombatDamageEvent zeroToxic = new CombatDamageEvent(this, toxic0, null, attacker, defender, 4.0f, true);
        bus.post(zeroToxic);
        assertEquals(0, poisonManager.getPoison(defender), "Toxic 0 must not inflict poison");

        // CardDefinition with toxic keyword
        CardDefinition toxicCard = CardDefinition.builder("one:toxic_stinger", "one", "Toxic Stinger")
            .addType(CardType.CREATURE)
            .rarity(CardRarity.COMMON)
            .manaCost(ManaCost.ofGreen(1))
            .addKeyword(KeywordInstance.of(PhyrexiaExpansion.TOXIC, 2))
            .build();
        CombatDamageEvent cardCombat = new CombatDamageEvent(this, toxicCard, null, attacker, defender, 2.0f, true);
        bus.post(cardCombat);
        assertEquals(2, poisonManager.getPoison(defender), "CardDefinition with mtg:toxic 2 keyword must inflict 2 poison counters");
    }

    // =========================================================================
    // 6. Concurrency Stress Test on Poison Tracking
    // =========================================================================

    @Test
    @DisplayName("Poison Counter Concurrency: Multi-threaded concurrent poison additions across distinct entities")
    void testConcurrentPoisonTrackingMultiThreaded() throws InterruptedException {
        int threadCount = 8;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            players.add(UUID.randomUUID());
        }

        for (int t = 0; t < threadCount; t++) {
            final UUID player = players.get(t);
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        poisonManager.addPoison(player, 1, null, null);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Concurrent poison operations must complete within 5 seconds");
        executor.shutdown();

        for (UUID player : players) {
            assertEquals(operationsPerThread, poisonManager.getPoison(player));
            assertTrue(poisonManager.isLethal(player));
        }
    }

    @Test
    @DisplayName("Poison Counter Concurrency: Multi-threaded concurrent poison additions to the SAME entity")
    void testConcurrentPoisonOnSameEntity() throws InterruptedException {
        int threadCount = 8;
        int operationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        UUID sharedTarget = UUID.randomUUID();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        poisonManager.addPoison(sharedTarget, 1, null, null);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * operationsPerThread, poisonManager.getPoison(sharedTarget),
            "PoisonCounterManager.addPoison() must be atomic under high contention on the same entity");
    }

    @Test
    @DisplayName("Proliferate: Collection containing null target handles gracefully without NullPointerException")
    void testProliferateCollectionContainingNull() {
        TestCounterHolder target = new TestCounterHolder(Map.of("+1/+1", 1));
        List<ICounterHolder> targetsWithNull = new ArrayList<>();
        targetsWithNull.add(target);
        targetsWithNull.add(null);

        assertDoesNotThrow(() -> {
            Map<UUID, Map<String, Integer>> res = ProliferateExecutor.proliferate(targetsWithNull, poisonManager, bus);
            assertEquals(2, target.getCounter("+1/+1"));
        }, "ProliferateExecutor should skip null targets gracefully");
    }

    @Test
    @DisplayName("Proliferate: Deduplication ensures a permanent is proliferated at most once per MTG CR 701.27a")
    void testProliferateDeduplicationOnDuplicateHolder() {
        TestCounterHolder target = new TestCounterHolder(Map.of("+1/+1", 1));
        List<ICounterHolder> duplicates = new ArrayList<>(List.of(target, target, target));

        Map<UUID, Map<String, Integer>> res = ProliferateExecutor.proliferate(duplicates, poisonManager, bus);
        assertEquals(2, target.getCounter("+1/+1"), "Deduplication must prevent adding multiple counters to the same permanent");
        assertEquals(1, res.size(), "Delta report must only contain one entry for the target");
    }

    @Test
    @DisplayName("Proliferate: Null and empty collections handle gracefully without exceptions")
    void testProliferateNullAndEmptyCollections() {
        assertDoesNotThrow(() -> {
            Map<UUID, Map<String, Integer>> r1 = ProliferateExecutor.proliferate(null, poisonManager, bus);
            assertTrue(r1.isEmpty());

            Map<UUID, Map<String, Integer>> r2 = ProliferateExecutor.proliferate(List.of(), poisonManager, bus);
            assertTrue(r2.isEmpty());

            List<ICounterHolder> onlyNulls = new ArrayList<>();
            onlyNulls.add(null);
            onlyNulls.add(null);
            Map<UUID, Map<String, Integer>> r3 = ProliferateExecutor.proliferate(onlyNulls, poisonManager, bus);
            assertTrue(r3.isEmpty());
        });
    }

    @Test
    @DisplayName("Poison Counter: Arithmetic overflow protection and non-positive amount boundaries")
    void testPoisonCounterManagerOverflowAndBoundaryClamping() {
        UUID target = UUID.randomUUID();

        // Null target returns 0
        assertEquals(0, poisonManager.addPoison(null, 10, null, null));
        assertEquals(0, poisonManager.removePoison(null, 10));

        // Adding Integer.MAX_VALUE
        int maxVal = poisonManager.addPoison(target, Integer.MAX_VALUE, null, null);
        assertEquals(Integer.MAX_VALUE, maxVal);

        // Adding more does not overflow to negative
        int overflowAttempt = poisonManager.addPoison(target, 500, null, null);
        assertEquals(Integer.MAX_VALUE, overflowAttempt);

        // Non-positive addition does not mutate
        assertEquals(Integer.MAX_VALUE, poisonManager.addPoison(target, 0, null, null));
        assertEquals(Integer.MAX_VALUE, poisonManager.addPoison(target, -50, null, null));

        // Atomic removal
        int afterRemoval = poisonManager.removePoison(target, 100);
        assertEquals(Integer.MAX_VALUE - 100, afterRemoval);

        // Excessive removal clamps to 0
        int zeroed = poisonManager.removePoison(target, Integer.MAX_VALUE);
        assertEquals(0, zeroed);

        // Removing negative amount does nothing
        assertEquals(0, poisonManager.removePoison(target, -10));
    }

    @Test
    @DisplayName("Poison Counter Concurrency: 16 threads pounding same entity simultaneously with 16,000 updates")
    void testHighContentionPoison16Threads() throws InterruptedException {
        int threadCount = 16;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        UUID sharedTarget = UUID.randomUUID();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        poisonManager.addPoison(sharedTarget, 1, null, null);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * operationsPerThread, poisonManager.getPoison(sharedTarget),
            "16 threads x 1,000 increments must result in exactly 16,000 poison counters with zero lost updates");
    }
}

