package com.mtg.test.world;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.api.trigger.TriggerPriority;
import com.mtg.config.WorldGenConfig;
import com.mtg.domain.CardRarity;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import com.mtg.world.altar.*;
import com.mtg.world.boss.*;
import com.mtg.world.events.*;
import com.mtg.world.node.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial World Content, Altars & Boss Stress Suite")
public class AdversarialWorldContentStressTest {

    private TriggerBus testBus;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        testPlayerId = UUID.randomUUID();
        ManaManager.getInstance().selectInitialMana(testPlayerId, ManaColor.WHITE);
        ManaManager.getInstance().getPlayerMana(testPlayerId).clear();
        BossManager.getInstance().clear();
        BossManager.getInstance().setTriggerBus(testBus);
    }

    // =========================================================================
    // SECTION 1: ELEMENTAL MANA NODES CONCURRENCY & INVARIANTS
    // =========================================================================

    @Test
    @DisplayName("ManaNode Concurrent Attunement: 20 threads contending for 5 charges")
    void testConcurrentManaNodeAttunement_20ThreadsContention() throws InterruptedException {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.MOUNTAIN_NODE, 3);
        ManaNodeState state = new ManaNodeState(def, testBus);

        AtomicInteger eventCount = new AtomicInteger(0);
        testBus.registerListener(ManaNodeAttunedEvent.class, TriggerPriority.NORMAL, event -> {
            eventCount.incrementAndGet();
        });

        int numThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        UUID[] players = new UUID[numThreads];
        for (int i = 0; i < numThreads; i++) {
            players[i] = UUID.randomUUID();
            ManaManager.getInstance().selectInitialMana(players[i], ManaColor.WHITE);
            ManaManager.getInstance().getPlayerMana(players[i]).clear();
        }

        for (int i = 0; i < numThreads; i++) {
            final UUID pid = players[i];
            executor.submit(() -> {
                try {
                    startGate.await();
                    boolean res = state.attune(pid);
                    if (res) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(5, TimeUnit.SECONDS), "Threads must complete within timeout");
        executor.shutdown();

        // Exactly 5 threads must succeed, 15 must fail
        assertEquals(5, successCount.get(), "Exactly 5 attunements should succeed from 5 initial charges");
        assertEquals(15, failureCount.get(), "15 attunements should fail due to charge exhaustion");
        assertEquals(0, state.getCurrentCharges(), "Current charges must reach exactly 0, never negative");
        assertEquals(5, eventCount.get(), "Exactly 5 ManaNodeAttunedEvents should be posted to TriggerBus");
    }

    @Test
    @DisplayName("ManaNode Charge Starvation: zero charges reject all concurrent attempts")
    void testChargeStarvation_ConcurrentAttemptsOnDepletedNode() throws InterruptedException {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.FOREST_NODE);
        ManaNodeState state = new ManaNodeState(def, testBus);
        state.setCurrentCharges(0); // Depleted

        int numThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    if (state.attune(UUID.randomUUID())) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, successCount.get(), "Zero attunements must succeed on depleted node");
        assertEquals(20, failureCount.get());
        assertEquals(0, state.getCurrentCharges());
    }

    @Test
    @DisplayName("ManaNode Recharge Timers: concurrent ticking and max charge clamping")
    void testConcurrentRechargeTimerTicking() throws InterruptedException {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.ISLAND_NODE);
        ManaNodeState state = new ManaNodeState(def, testBus);
        state.setCurrentCharges(0);

        int numThreads = 10;
        int ticksPerThread = 200; // 10 * 200 = 2000 ticks total
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int t = 0; t < ticksPerThread; t++) {
                        state.tick();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Node must reach max cap (5 charges) and not exceed it
        assertEquals(5, state.getCurrentCharges(), "Node must be recharged to full capacity");
        assertTrue(state.getRechargeTimerTicks() >= 0 && state.getRechargeTimerTicks() < 200,
                "Timer ticks must remain within valid interval range [0, 200)");
    }

    @Test
    @DisplayName("ManaNode Coordinate Hashing: extensive 10,000 chunk fuzzing across negative/positive coords")
    void testManaNodeCoordinateHashing_ExtensiveFuzzAndNegativeCoordinates() {
        long worldSeed = 0xCAFEBABE12345678L;
        WorldGenConfig config = WorldGenConfig.defaults();

        int spawnedCount = 0;
        int totalChunks = 0;

        for (int cx = -50; cx < 50; cx++) {
            for (int cz = -50; cz < 50; cz++) {
                totalChunks++;
                Optional<ManaNodePlacement.PlacementResult> res1 =
                        ManaNodePlacement.evaluateChunk(worldSeed, cx, cz, "minecraft:forest", config);
                Optional<ManaNodePlacement.PlacementResult> res2 =
                        ManaNodePlacement.evaluateChunk(worldSeed, cx, cz, "minecraft:forest", config);

                // Determinism assertion
                assertEquals(res1.isPresent(), res2.isPresent(), "Placement must be strictly deterministic");
                if (res1.isPresent()) {
                    spawnedCount++;
                    ManaNodePlacement.PlacementResult r1 = res1.get();
                    ManaNodePlacement.PlacementResult r2 = res2.get();

                    assertEquals(r1.blockX(), r2.blockX());
                    assertEquals(r1.blockZ(), r2.blockZ());
                    assertEquals(r1.localX(), r2.localX());
                    assertEquals(r1.localZ(), r2.localZ());
                    assertEquals(r1.nodeType(), r2.nodeType());

                    // Coordinate invariants
                    assertTrue(r1.localX() >= 0 && r1.localX() < 16, "Local X must be in [0, 15]");
                    assertTrue(r1.localZ() >= 0 && r1.localZ() < 16, "Local Z must be in [0, 15]");
                    assertEquals((cx << 4) + r1.localX(), r1.blockX(), "BlockX must match chunk offset + localX");
                    assertEquals((cz << 4) + r1.localZ(), r1.blockZ(), "BlockZ must match chunk offset + localZ");
                }
            }
        }

        assertEquals(10000, totalChunks);
        // Default chance is 0.02 (2%). Across 10,000 chunks, expected ~200 nodes.
        // Assert within wide bounds [100, 350]
        assertTrue(spawnedCount >= 100 && spawnedCount <= 350,
                "Spawn count across 10,000 chunks must follow chance distribution (actual: " + spawnedCount + ")");

        // Test with disabled config
        WorldGenConfig disabledConfig = new WorldGenConfig(
                false, 0.02, 3, true, 32, 8, true, 0.05, true, 0.01, 1.0, 1.0, true, 10, 3);
        for (int cx = -10; cx <= 10; cx++) {
            for (int cz = -10; cz <= 10; cz++) {
                Optional<ManaNodePlacement.PlacementResult> res =
                        ManaNodePlacement.evaluateChunk(worldSeed, cx, cz, "minecraft:forest", disabledConfig);
                assertTrue(res.isEmpty(), "Disabled mana node config must never produce nodes");
            }
        }
    }

    // =========================================================================
    // SECTION 2: PLANESWALKER ALTAR CONCURRENCY & RITUAL LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Planeswalker Altar Concurrent Rituals: 20 threads contending on single altar")
    void testPlaneswalkerAltarConcurrentRituals_20ThreadsContention() throws InterruptedException {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        int numThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    UUID p = UUID.randomUUID();
                    boolean res;
                    if (index % 3 == 0) {
                        res = altar.initiateReplenishment(p, ManaColor.BLUE);
                    } else if (index % 3 == 1) {
                        res = altar.initiateConfluxOvercharge(p);
                    } else {
                        res = altar.initiateBossSummon(p, ManaColor.RED, 15);
                    }

                    if (res) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Invariant: Exactly ONE ritual initiation succeeds, all others fail
        assertEquals(1, successCount.get(), "Exactly one ritual must succeed initiation");
        assertEquals(19, failureCount.get(), "All other 19 attempts must be rejected");
        assertEquals(AltarRitualPhase.CHANNELING, altar.getCurrentPhase(), "Altar must be in CHANNELING phase");
        assertNotNull(altar.getActivePlayerId());
        assertNotNull(altar.getActiveRitualType());
    }

    @Test
    @DisplayName("Planeswalker Altar Parameter Validation & State Gating")
    void testPlaneswalkerAltarParameterValidationAndStateGating() {
        AltarStateMachine altar = new AltarStateMachine();

        // Null checks
        assertFalse(altar.initiateReplenishment(null, ManaColor.BLUE));
        assertFalse(altar.initiateReplenishment(testPlayerId, null));
        assertFalse(altar.initiateConfluxOvercharge(null));
        assertFalse(altar.initiateBossSummon(null, ManaColor.RED, 15));
        assertFalse(altar.initiateBossSummon(testPlayerId, null, 15));

        // XP Focus boundary checks
        assertFalse(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 0));
        assertFalse(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 9), "XP < 10 must fail");
        assertTrue(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 10), "XP >= 10 must succeed");

        // Altar is now CHANNELING. Cannot initiate any ritual!
        assertEquals(AltarRitualPhase.CHANNELING, altar.getCurrentPhase());
        assertFalse(altar.initiateReplenishment(testPlayerId, ManaColor.WHITE));
        assertFalse(altar.initiateConfluxOvercharge(testPlayerId));
        assertFalse(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 20));

        // Advance 60 ticks to complete ritual -> enters COOLDOWN
        for (int t = 0; t < 60; t++) {
            altar.tick();
        }
        assertEquals(AltarRitualPhase.COOLDOWN, altar.getCurrentPhase());

        // In COOLDOWN: all initiations must still fail
        assertFalse(altar.initiateReplenishment(testPlayerId, ManaColor.WHITE));
        assertFalse(altar.initiateConfluxOvercharge(testPlayerId));
        assertFalse(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 20));

        // Advance 100 ticks to reset to IDLE
        for (int t = 0; t < 100; t++) {
            altar.tick();
        }
        assertEquals(AltarRitualPhase.IDLE, altar.getCurrentPhase());

        // Now initiation succeeds again
        assertTrue(altar.initiateReplenishment(testPlayerId, ManaColor.WHITE));
    }

    @Test
    @DisplayName("Planar Conflux Overcharge: 5 sequential cycles increase cap by +25 and award 5 Rare wildcards")
    void testConfluxOverchargeMultiCycleProgressionStress() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        ManaPool pool = ManaManager.getInstance().getPlayerMana(testPlayerId);
        int initialCap = pool.getMaxMana();
        int initialWildcards = ProgressionManager.getInstance().getProgression(testPlayerId).getWildcardCount(CardRarity.RARE);

        for (int cycle = 1; cycle <= 5; cycle++) {
            assertTrue(altar.initiateConfluxOvercharge(testPlayerId), "Initiation must succeed on cycle " + cycle);

            // Channeling (60 ticks)
            for (int t = 0; t < 60; t++) {
                altar.tick();
            }

            assertEquals(initialCap + (cycle * 5), pool.getMaxMana(),
                    "Max mana cap must increase by +5 each cycle (cycle " + cycle + ")");
            assertEquals(initialWildcards + cycle,
                    ProgressionManager.getInstance().getProgression(testPlayerId).getWildcardCount(CardRarity.RARE),
                    "Rare wildcards must increase by +1 each cycle (cycle " + cycle + ")");

            // Cooldown (100 ticks)
            for (int t = 0; t < 100; t++) {
                altar.tick();
            }
            assertEquals(AltarRitualPhase.IDLE, altar.getCurrentPhase());
        }
    }

    // =========================================================================
    // SECTION 3: WORLD BOSS COMBAT, THREAT TABLE & CONCURRENCY
    // =========================================================================

    @Test
    @DisplayName("World Boss Concurrent Damage: 20 threads attacking Elemental Boss simultaneously")
    void testConcurrentBossDamage_20ThreadsOnElementalBoss() throws InterruptedException {
        GenericElementalBoss boss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_avatar_red", ManaColor.RED, 300.0f, 12.0f, testBus);

        AtomicInteger phaseChangeEvents = new AtomicInteger(0);
        testBus.registerListener(BossPhaseChangeEvent.class, TriggerPriority.NORMAL, event -> {
            phaseChangeEvents.incrementAndGet();
        });

        int numThreads = 20;
        float damagePerThread = 10.0f; // 20 * 10 = 200 damage (takes boss from 300 to 100 HP, crossing 66% threshold)
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        UUID[] attackers = new UUID[numThreads];
        for (int i = 0; i < numThreads; i++) {
            attackers[i] = UUID.randomUUID();
        }

        for (int i = 0; i < numThreads; i++) {
            final UUID attacker = attackers[i];
            executor.submit(() -> {
                try {
                    startGate.await();
                    boss.takeDamage(attacker, damagePerThread);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Invariants
        assertEquals(100.0f, boss.getCurrentHealth(), 0.01f, "Final health must equal exactly 300 - 200 = 100 HP");
        assertEquals(BossPhase.PHASE_2_ENRAGED, boss.getPhase(), "Boss must be in PHASE_2_ENRAGED");
        assertEquals(15.0f, boss.getAttackDamage(), 0.01f, "Phase 2 enrage scales attack damage to 1.25x (12 * 1.25 = 15)");

        // Enrage event must have fired EXACTLY ONCE despite 20 concurrent threads
        assertEquals(1, phaseChangeEvents.get(), "Phase change to ENRAGED must fire exactly once");

        // Threat table must accurately hold 20 entries of 10.0f threat each
        BossThreatTable table = boss.getThreatTable();
        assertEquals(20, table.getAllThreats().size());
        for (UUID attacker : attackers) {
            assertEquals(10.0f, table.getThreat(attacker), 0.01f);
        }

        // Now defeat boss with 20 threads dealing another 10 damage each (total 200 damage)
        AtomicInteger defeatEvents = new AtomicInteger(0);
        testBus.registerListener(BossDefeatedEvent.class, TriggerPriority.NORMAL, event -> {
            defeatEvents.incrementAndGet();
        });

        ExecutorService executor2 = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate2 = new CountDownLatch(1);
        CountDownLatch endGate2 = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final UUID attacker = attackers[i];
            executor2.submit(() -> {
                try {
                    startGate2.await();
                    boss.takeDamage(attacker, damagePerThread);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate2.countDown();
                }
            });
        }

        startGate2.countDown();
        assertTrue(endGate2.await(5, TimeUnit.SECONDS));
        executor2.shutdown();

        assertEquals(0.0f, boss.getCurrentHealth(), 0.01f, "Current health must reach 0, never negative");
        assertEquals(BossPhase.DEFEATED, boss.getPhase());
        assertFalse(boss.isAlive());
        assertEquals(1, defeatEvents.get(), "BossDefeatedEvent must fire exactly once");
    }

    @Test
    @DisplayName("Boss Threat Table: exact distance boundaries (32.0 and 48.0) and decay pruning")
    void testBossThreatTable_ExactDistanceBoundariesAndDecay() {
        BossThreatTable table = new BossThreatTable();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        table.addDamageThreat(p1, 50.0f);
        table.addDamageThreat(p2, 60.0f);
        table.addDamageThreat(p3, 70.0f);

        Map<UUID, Double> distances = new HashMap<>();
        distances.put(p1, 32.0);      // Exact engagement boundary
        distances.put(p2, 32.0001);   // Just outside engagement boundary
        distances.put(p3, 48.0);      // Exact leash boundary

        // Engagement targeting: 32.0 blocks
        // p2 is outside (32.0001), p3 is outside (48.0), only p1 is within <= 32.0
        Optional<UUID> target = table.getHighestThreatTarget(distances, 32.0);
        assertTrue(target.isPresent());
        assertEquals(p1, target.get(), "Only p1 at <= 32.0 is eligible for targeting");

        // Leash decay: 48.0 blocks threshold, 10% decay
        // p1 (32.0 <= 48.0) does NOT decay
        // p2 (32.0001 <= 48.0) does NOT decay
        // p3 (48.0 <= 48.0) does NOT decay! (because dist > leashDistance is false)
        table.applyLeashDecay(distances, 48.0, 0.10f);
        assertEquals(50.0f, table.getThreat(p1));
        assertEquals(60.0f, table.getThreat(p2));
        assertEquals(70.0f, table.getThreat(p3), "p3 at exactly 48.0 must not decay");

        // Move p3 to 48.0001 (beyond leash)
        distances.put(p3, 48.0001);
        table.applyLeashDecay(distances, 48.0, 0.10f);
        assertEquals(63.0f, table.getThreat(p3), 0.01f, "p3 beyond 48.0 must decay by 10% (70 -> 63)");

        // Repeated decay cycles should prune p3 once threat <= 0.05f
        for (int i = 0; i < 75; i++) {
            table.applyLeashDecay(distances, 48.0, 0.10f);
        }
        assertEquals(0.0f, table.getThreat(p3), "p3 must be pruned when threat <= 0.05f");
        assertFalse(table.getAllThreats().containsKey(p3), "p3 must be completely removed from map");
    }

    static class CustomBarrierBoss extends AbstractBossEntity {
        private float divineBarrier = 60.0f;

        public CustomBarrierBoss(UUID id, float baseHp, float baseAtk, TriggerBus bus) {
            super(id, "mtg:barrier_boss", baseHp, baseAtk, bus);
        }

        public float getDivineBarrier() {
            return divineBarrier;
        }

        @Override
        public synchronized void takeDamage(UUID attackerId, float amount) {
            if (divineBarrier > 0.0f) {
                if (divineBarrier >= amount) {
                    divineBarrier -= amount;
                    return;
                }
                float remaining = amount - divineBarrier;
                divineBarrier = 0.0f;
                super.takeDamage(attackerId, remaining);
            } else {
                super.takeDamage(attackerId, amount);
            }
        }
    }

    @Test
    @DisplayName("Boss Phase Transitions: exact mathematical threshold boundaries")
    void testBossPhaseTransitions_ExactThresholdBoundaries() {
        // 1. Generic Elemental Boss (Red): 300 HP, 66% threshold = 198 HP, 33% threshold = 99 HP
        GenericElementalBoss redBoss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_red", ManaColor.RED, 300.0f, 12.0f, testBus);
        redBoss.takeDamage(testPlayerId, 101.99f);
        assertEquals(BossPhase.PHASE_1_TACTICAL, redBoss.getPhase(), "HP 198.01 must be tactical (> 66%)");
        redBoss.takeDamage(testPlayerId, 0.01f);
        assertEquals(198.0f, redBoss.getCurrentHealth(), 0.01f);
        assertEquals(BossPhase.PHASE_2_ENRAGED, redBoss.getPhase(), "HP exactly 198.0 must transition to ENRAGED (<= 66%)");

        // 2. Generic Elemental Boss (Black): 450 HP, 66% threshold = 297 HP
        GenericElementalBoss blackBoss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_black", ManaColor.BLACK, 450.0f, 18.0f, testBus);
        blackBoss.takeDamage(testPlayerId, 152.99f);
        assertEquals(BossPhase.PHASE_1_TACTICAL, blackBoss.getPhase(), "HP 297.01 must be tactical");
        blackBoss.takeDamage(testPlayerId, 0.01f);
        assertEquals(297.0f, blackBoss.getCurrentHealth(), 0.01f);
        assertEquals(BossPhase.PHASE_2_ENRAGED, blackBoss.getPhase(), "HP exactly 297.0 must transition to ENRAGED");

        // 3. Custom Barrier Boss: 350 HP, Barrier = 60, 66% threshold = 231 HP
        CustomBarrierBoss barrierBoss = new CustomBarrierBoss(UUID.randomUUID(), 350.0f, 14.0f, testBus);
        barrierBoss.takeDamage(testPlayerId, 60.0f); // Absorbed by barrier
        assertEquals(350.0f, barrierBoss.getCurrentHealth(), "Barrier absorbed 60 damage, HP intact");
        assertEquals(0.0f, barrierBoss.getDivineBarrier());

        barrierBoss.takeDamage(testPlayerId, 118.99f);
        assertEquals(BossPhase.PHASE_1_TACTICAL, barrierBoss.getPhase(), "HP 231.01 must be tactical");
        barrierBoss.takeDamage(testPlayerId, 0.01f);
        assertEquals(231.0f, barrierBoss.getCurrentHealth(), 0.01f);
        assertEquals(BossPhase.PHASE_2_ENRAGED, barrierBoss.getPhase(), "HP exactly 231.0 must transition to ENRAGED");

        // 4. BossManager Created Boss (Green): 200 HP, 66% threshold = 132 HP
        IBossEntity greenBoss = BossManager.getInstance().createBossForColor(ManaColor.GREEN, testBus);
        greenBoss.takeDamage(testPlayerId, 67.0f);
        assertEquals(BossPhase.PHASE_1_TACTICAL, greenBoss.getPhase(), "HP 133.0 must be tactical");
        greenBoss.takeDamage(testPlayerId, 1.0f);
        assertEquals(132.0f, greenBoss.getCurrentHealth(), 0.01f);
        assertEquals(BossPhase.PHASE_2_ENRAGED, greenBoss.getPhase(), "HP exactly 132.0 must transition to ENRAGED");
    }

    static class RetaliatoryBoss extends AbstractBossEntity {
        private float lastRecoilDamageDealt = 0.0f;
        private UUID lastRecoilTarget = null;
        private final Map<UUID, Integer> recoilCounters = new HashMap<>();

        public RetaliatoryBoss(UUID id, float baseHealth, float baseAttackDamage, TriggerBus triggerBus) {
            super(id, "mtg:retaliatory_boss", baseHealth, baseAttackDamage, triggerBus);
        }

        @Override
        public synchronized void takeDamage(UUID attackerId, float amount) {
            super.takeDamage(attackerId, amount);
            if (attackerId != null && !attackerId.equals(getId())) {
                this.lastRecoilDamageDealt = amount * 0.5f;
                this.lastRecoilTarget = attackerId;
                recoilCounters.merge(attackerId, 1, Integer::sum);
            }
        }

        public float getLastRecoilDamageDealt() { return lastRecoilDamageDealt; }
        public UUID getLastRecoilTarget() { return lastRecoilTarget; }
        public int getRecoilCount(UUID target) { return recoilCounters.getOrDefault(target, 0); }
    }

    @Test
    @DisplayName("AbstractBossEntity: Retaliatory Backlash recoil and recursion safety")
    void testRetaliatoryBacklashAndLoopPrevention() {
        RetaliatoryBoss boss = new RetaliatoryBoss(
                UUID.randomUUID(), 200.0f, 10.0f, testBus);

        // Standard attack
        boss.takeDamage(testPlayerId, 40.0f);
        assertEquals(20.0f, boss.getLastRecoilDamageDealt(), "Recoil must be exactly 50% of 40 = 20");
        assertEquals(testPlayerId, boss.getLastRecoilTarget());
        assertEquals(1, boss.getRecoilCount(testPlayerId));

        // Null attacker (e.g. environment damage) must not crash or increment recoil count
        int countBefore = boss.getRecoilCount(testPlayerId);
        boss.takeDamage(null, 10.0f);
        assertEquals(countBefore, boss.getRecoilCount(testPlayerId));

        // Self-damage / simulated recoil loop test:
        assertDoesNotThrow(() -> {
            boss.takeDamage(boss.getId(), 10.0f);
        }, "Self-damage must execute safely without recursion crash");
    }

    @Test
    @DisplayName("BossManager: Dynamic Factory Registration and Fallback Creation per ManaColor")
    void testBossManagerFactoryRegistrationAndColorDispatch() {
        BossManager manager = BossManager.getInstance();
        manager.clear();

        // 1. Default creation without custom factory produces GenericElementalBoss
        IBossEntity defaultRed = manager.createBossForColor(ManaColor.RED, testBus);
        assertInstanceOf(GenericElementalBoss.class, defaultRed);
        assertEquals(ManaColor.RED, ((GenericElementalBoss) defaultRed).getAffinityColor());

        // 2. Register custom boss factory for BLUE
        manager.registerBossFactory(ManaColor.BLUE, (id, bus) ->
                new GenericElementalBoss(id, "mtg:custom_blue_titan", ManaColor.BLUE, 500.0f, 25.0f, bus));

        IBossEntity customBlue = manager.createBossForColor(ManaColor.BLUE, testBus);
        assertInstanceOf(GenericElementalBoss.class, customBlue);
        assertEquals("mtg:custom_blue_titan", customBlue.getBossType());
        assertEquals(500.0f, customBlue.getMaxHealth());
        assertEquals(25.0f, ((GenericElementalBoss) customBlue).getAttackDamage());

        // 3. Register boss in active registry and verify BossSpawnEvent
        AtomicBoolean spawned = new AtomicBoolean(false);
        testBus.registerListener(BossSpawnEvent.class, TriggerPriority.NORMAL, e -> spawned.set(true));

        manager.registerBoss(customBlue);
        assertTrue(spawned.get());
        assertTrue(manager.getBoss(customBlue.getId()).isPresent());
        assertEquals(1, manager.getAllBosses().size());

        // 4. Tick and remove
        manager.tickAll();
        manager.removeBoss(customBlue.getId());
        assertTrue(manager.getBoss(customBlue.getId()).isEmpty());
    }

    @Test
    @DisplayName("Boss Multiplayer Scaling Math: group multiplier, solo bounds & configuration clamping")
    void testBossMultiplayerScalingMath_ExtremeBoundariesAndFuzzing() {
        WorldGenConfig config = WorldGenConfig.defaults();
        float baseHp = 300.0f;

        // Boundary: 0 or negative combatants clamps to solo (1x)
        assertEquals(300.0f, BossStatScaler.calculateScaledMaxHealth(baseHp, 0, config), 0.01f);
        assertEquals(300.0f, BossStatScaler.calculateScaledMaxHealth(baseHp, -10, config), 0.01f);

        // Solo combatant (1 player) = 1.0x
        assertEquals(300.0f, BossStatScaler.calculateScaledMaxHealth(baseHp, 1, config), 0.01f);

        // 20 combatants: 1.0 + 0.35 * 19 = 7.65x
        float hp20 = BossStatScaler.calculateScaledMaxHealth(baseHp, 20, config);
        assertEquals(300.0f * 7.65f, hp20, 0.01f);

        // 100 combatants: 1.0 + 0.35 * 99 = 35.65x
        float hp100 = BossStatScaler.calculateScaledMaxHealth(baseHp, 100, config);
        assertEquals(300.0f * 35.65f, hp100, 0.01f);

        // Null config defaults to 1.0 multiplier
        assertEquals(300.0f * 7.65f, BossStatScaler.calculateScaledMaxHealth(baseHp, 20, null), 0.01f);
        assertEquals(12.0f, BossStatScaler.calculateScaledAttackDamage(12.0f, null), 0.01f);
    }
}
