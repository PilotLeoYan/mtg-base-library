package com.mtg.test.entity;

import com.mtg.api.trigger.TapPermanentEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.entity.ISummonedCreature;
import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import com.mtg.entity.TappableManaAbility;
import com.mtg.entity.ai.*;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Challenger Empirical Stress & Concurrency Suite")
public class ChallengerCreatureStressTest {

    private SummonManager summonManager;

    @BeforeEach
    void setUp() {
        summonManager = new SummonManager();
        SummonManager.setInstance(summonManager);
    }

    // =========================================================================
    // 1. Summon Cap Concurrency: 20 threads simultaneously summoning creatures
    // =========================================================================
    @Test
    @DisplayName("Challenger 1A: Summon Cap 20-Thread High Concurrency Stress Test")
    void testSummonCapHighConcurrency20Threads() throws Exception {
        UUID playerId = UUID.randomUUID();
        int threadCount = 20;
        int summonsPerThread = 5; // Total 100 summons
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<SummonedCreature> allCreatedSummons = new CopyOnWriteArrayList<>();
        List<SummonedCreature> registrationOrder = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean invariantViolated = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < summonsPerThread; j++) {
                        SummonedCreature creature = new SummonedCreature(playerId, TestCreatures.SERRA_ANGEL);
                        allCreatedSummons.add(creature);
                        synchronized (summonManager) {
                            registrationOrder.add(creature);
                            summonManager.registerSummon(creature);
                        }

                        int activeCount = summonManager.getActiveSummonCount(playerId);
                        if (activeCount > 3) {
                            invariantViolated.set(true);
                        }
                    }
                } catch (Exception e) {
                    invariantViolated.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Threads timed out during concurrent summoning");
        executor.shutdown();

        assertFalse(invariantViolated.get(), "Active summons count exceeded 3 during concurrent registration!");
        assertEquals(3, summonManager.getActiveSummonCount(playerId), "Summon count must be exactly 3 after 100 summons");
        assertEquals(3, summonManager.getPlayerSummons(playerId).size(), "Player summons list size must be exactly 3");

        // Verify that dismissed summons are properly flagged as dead/dismissed
        long dismissedCount = allCreatedSummons.stream().filter(ISummonedCreature::isDead).count();
        assertEquals(97, dismissedCount, "97 out of 100 summons must be dismissed/dead");

        long activeCreatureCount = allCreatedSummons.stream().filter(c -> !c.isDead()).count();
        assertEquals(3, activeCreatureCount, "Exactly 3 creatures must remain alive");

        // Verify entityLookup matches active summons
        for (SummonedCreature active : summonManager.getPlayerSummons(playerId)) {
            Optional<SummonedCreature> lookup = summonManager.getSummon(active.getEntityId());
            assertTrue(lookup.isPresent(), "Active summon must be in entityLookup");
            assertSame(active, lookup.get(), "Active summon instance must match");
        }

        // Verify strict FIFO order: the 3 remaining summons must be the exact last 3 registered
        List<SummonedCreature> activeList = summonManager.getPlayerSummons(playerId);
        assertEquals(registrationOrder.get(97).getEntityId(), activeList.get(0).getEntityId(), "FIFO 1st active summon mismatch");
        assertEquals(registrationOrder.get(98).getEntityId(), activeList.get(1).getEntityId(), "FIFO 2nd active summon mismatch");
        assertEquals(registrationOrder.get(99).getEntityId(), activeList.get(2).getEntityId(), "FIFO 3rd active summon mismatch");
    }

    @Test
    @DisplayName("Challenger 1B: Multi-Player Isolated Summon Cap Concurrency (5 players x 20 threads)")
    void testMultiPlayerSummonCapIsolationConcurrency() throws Exception {
        int playerCount = 5;
        int threadsPerPlayer = 4; // 20 threads total
        int summonsPerThread = 5;

        List<UUID> players = new ArrayList<>();
        for (int p = 0; p < playerCount; p++) {
            players.add(UUID.randomUUID());
        }

        ExecutorService executor = Executors.newFixedThreadPool(playerCount * threadsPerPlayer);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(playerCount * threadsPerPlayer);
        AtomicBoolean invariantViolated = new AtomicBoolean(false);

        for (UUID pid : players) {
            for (int t = 0; t < threadsPerPlayer; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int s = 0; s < summonsPerThread; s++) {
                            SummonedCreature sc = new SummonedCreature(pid, TestCreatures.VAMPIRE_NIGHTHAWK);
                            summonManager.registerSummon(sc);
                            if (summonManager.getActiveSummonCount(pid) > 3) {
                                invariantViolated.set(true);
                            }
                        }
                    } catch (Exception e) {
                        invariantViolated.set(true);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertFalse(invariantViolated.get(), "Active summons count exceeded 3 for some player!");
        for (UUID pid : players) {
            assertEquals(3, summonManager.getActiveSummonCount(pid), "Each player must have exactly 3 active summons");
            assertEquals(3, summonManager.getPlayerSummons(pid).size());
        }
    }

    // =========================================================================
    // 2. Companion AI Leash Boundaries, Extreme Coords, & Friendly Fire
    // =========================================================================
    @Test
    @DisplayName("Challenger 2A: Companion AI Leash Boundaries (48 blocks exact boundary)")
    void testLeashBoundaryPrecision() {
        UUID ownerId = UUID.randomUUID();
        FollowOwnerGoal goal = new FollowOwnerGoal(); // follow: 6.0, stop: 3.0, leash: 48.0

        // Case 1: Inside stop threshold (dist <= 3.0) -> IDLE
        SummonedCreature c1 = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, 0.0, 0.0, 2.0);
        AiContext ctx1 = AiContext.ofOwnerPos(ownerId, 0.0, 0.0, 0.0);
        assertFalse(goal.canExecute(c1, ctx1));
        c1.setAiState(SummonAiState.FOLLOWING);
        goal.tick(c1, ctx1);
        assertEquals(SummonAiState.IDLE, c1.getAiState(), "Creature within 3.0 blocks must become IDLE");
        assertEquals(2.0, c1.getZ(), 0.001, "Position must not change when stopping");

        // Case 2: Between stop (3.0) and follow (6.0) threshold -> remains unchanged
        SummonedCreature c2 = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, 0.0, 0.0, 5.0);
        AiContext ctx2 = AiContext.ofOwnerPos(ownerId, 0.0, 0.0, 0.0);
        assertFalse(goal.canExecute(c2, ctx2), "Between 3.0 and 6.0 should not trigger goal");

        // Case 3: Beyond follow threshold (e.g. 10.0 blocks, <= 48.0) -> Moves towards owner
        SummonedCreature c3 = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, 0.0, 0.0, 10.0);
        AiContext ctx3 = AiContext.ofOwnerPos(ownerId, 0.0, 0.0, 0.0);
        assertTrue(goal.canExecute(c3, ctx3));
        goal.tick(c3, ctx3);
        assertEquals(SummonAiState.FOLLOWING, c3.getAiState());
        assertTrue(c3.getZ() < 10.0, "Creature should have moved closer to owner on Z axis");
        assertEquals(10.0 - 0.35, c3.getZ(), 0.01, "Movement step should be 0.35");

        // Case 4: Exactly 48.0 blocks -> Not yet teleported (> 48 is leash)
        SummonedCreature c4 = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, 48.0, 0.0, 0.0);
        AiContext ctx4 = AiContext.ofOwnerPos(ownerId, 0.0, 0.0, 0.0);
        goal.tick(c4, ctx4);
        assertEquals(SummonAiState.FOLLOWING, c4.getAiState(), "At exactly 48.0, should still be following, not emergency teleported");
        assertEquals(48.0 - 0.35, c4.getX(), 0.01);

        // Case 5: 48.01 blocks -> Emergency teleportation triggered!
        SummonedCreature c5 = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, 48.01, 0.0, 0.0);
        AiContext ctx5 = AiContext.ofOwnerPos(ownerId, 0.0, 0.0, 0.0);
        goal.tick(c5, ctx5);
        assertEquals(SummonAiState.IDLE, c5.getAiState(), "Emergency teleport must reset AI state to IDLE");
        assertEquals(0.0, c5.getX(), 0.001, "Creature must teleport to owner X");
        assertEquals(0.0, c5.getY(), 0.001, "Creature must teleport to owner Y");
        assertEquals(0.0, c5.getZ(), 0.001, "Creature must teleport to owner Z");
    }

    @Test
    @DisplayName("Challenger 2B: Companion AI Teleport Across Negative & Extreme Coordinates")
    void testExtremeCoordinatesTeleportation() {
        UUID ownerId = UUID.randomUUID();
        FollowOwnerGoal goal = new FollowOwnerGoal();

        // Subcase 1: Negative coordinates (-150000, -64, -250000)
        double ownerX = -150000.0;
        double ownerY = -64.0;
        double ownerZ = -250000.0;
        AiContext negCtx = AiContext.ofOwnerPos(ownerId, ownerX, ownerY, ownerZ);
        SummonedCreature negCreature = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, -150100.0, -64.0, -250000.0);

        assertTrue(goal.canExecute(negCreature, negCtx));
        goal.tick(negCreature, negCtx);

        assertEquals(ownerX, negCreature.getX(), 0.001, "Negative coord teleport X mismatch");
        assertEquals(ownerY, negCreature.getY(), 0.001, "Negative coord teleport Y mismatch");
        assertEquals(ownerZ, negCreature.getZ(), 0.001, "Negative coord teleport Z mismatch");
        assertEquals(SummonAiState.IDLE, negCreature.getAiState());

        // Subcase 2: Extreme coordinates (Minecraft 26.3 world border: +/- 29,999,990)
        double borderOwnerX = 29_999_990.0;
        double borderOwnerY = 319.0;
        double borderOwnerZ = 29_999_990.0;
        AiContext borderCtx = AiContext.ofOwnerPos(ownerId, borderOwnerX, borderOwnerY, borderOwnerZ);

        SummonedCreature borderCreature = new SummonedCreature(UUID.randomUUID(), ownerId, TestCreatures.SERRA_ANGEL, -29_999_990.0, -64.0, -29_999_990.0);

        assertTrue(goal.canExecute(borderCreature, borderCtx));
        goal.tick(borderCreature, borderCtx);

        assertEquals(borderOwnerX, borderCreature.getX(), 0.001, "World border teleport X mismatch");
        assertEquals(borderOwnerY, borderCreature.getY(), 0.001, "World border teleport Y mismatch");
        assertEquals(borderOwnerZ, borderCreature.getZ(), 0.001, "World border teleport Z mismatch");
        assertEquals(SummonAiState.IDLE, borderCreature.getAiState());
    }

    @Test
    @DisplayName("Challenger 2C: Friendly Fire Filter under Massive Multi-Target Combat Matrix")
    void testFriendlyFireFilterMassiveMultiTargetCombat() {
        int playerCount = 10;
        int summonsPerPlayer = 3;
        int mobCount = 50;

        List<UUID> players = new ArrayList<>();
        Map<UUID, List<SummonedCreature>> playerSummonsMap = new HashMap<>();

        for (int p = 0; p < playerCount; p++) {
            UUID pid = UUID.randomUUID();
            players.add(pid);
            List<SummonedCreature> sList = new ArrayList<>();
            for (int s = 0; s < summonsPerPlayer; s++) {
                SummonedCreature sc = new SummonedCreature(pid, TestCreatures.SERRA_ANGEL);
                sList.add(sc);
            }
            playerSummonsMap.put(pid, sList);
        }

        List<UUID> hostileMobs = new ArrayList<>();
        for (int m = 0; m < mobCount; m++) {
            hostileMobs.add(UUID.randomUUID());
        }

        DefendOwnerGoal defendGoal = new DefendOwnerGoal();
        AttackOwnerTargetGoal attackGoal = new AttackOwnerTargetGoal();

        // Test every summon against every player, summon, and mob
        for (UUID currentOwner : players) {
            List<SummonedCreature> friendlySummons = playerSummonsMap.get(currentOwner);

            for (SummonedCreature attacker : friendlySummons) {
                attacker.clearSummoningSickness();

                // 1. Cannot attack own summoner
                assertFalse(FriendlyFireFilter.canAttack(attacker, currentOwner, null),
                    "FriendlyFireFilter MUST forbid attacking owner player");

                AiContext selfDefend = AiContext.ofDefend(currentOwner, 0, 0, 0, currentOwner, null);
                assertFalse(defendGoal.canExecute(attacker, selfDefend), "DefendOwnerGoal must reject owner as target");

                // 2. Cannot attack friendly summons belonging to same owner
                for (SummonedCreature friendlyPeer : friendlySummons) {
                    assertFalse(FriendlyFireFilter.canAttack(attacker, friendlyPeer.getEntityId(), friendlyPeer),
                        "FriendlyFireFilter MUST forbid attacking summons of the same owner");
                    AiContext friendlyDefend = AiContext.ofDefend(currentOwner, 0, 0, 0, friendlyPeer.getEntityId(), friendlyPeer);
                    assertFalse(defendGoal.canExecute(attacker, friendlyDefend), "DefendOwnerGoal must reject friendly summons");

                    AiContext friendlyAttack = AiContext.ofAttack(currentOwner, 0, 0, 0, friendlyPeer.getEntityId(), friendlyPeer);
                    assertFalse(attackGoal.canExecute(attacker, friendlyAttack), "AttackOwnerTargetGoal must reject friendly summons");
                }

                // 3. CAN attack hostile enemy players
                for (UUID enemyPlayer : players) {
                    if (!enemyPlayer.equals(currentOwner)) {
                        assertTrue(FriendlyFireFilter.canAttack(attacker, enemyPlayer, null),
                            "FriendlyFireFilter MUST allow attacking enemy players");
                        AiContext enemyDefend = AiContext.ofDefend(currentOwner, 0, 0, 0, enemyPlayer, null);
                        assertTrue(defendGoal.canExecute(attacker, enemyDefend));
                    }
                }

                // 4. CAN attack enemy summons
                for (UUID enemyPlayer : players) {
                    if (!enemyPlayer.equals(currentOwner)) {
                        for (SummonedCreature enemySummon : playerSummonsMap.get(enemyPlayer)) {
                            assertTrue(FriendlyFireFilter.canAttack(attacker, enemySummon.getEntityId(), enemySummon),
                                "FriendlyFireFilter MUST allow attacking enemy summons");
                        }
                    }
                }

                // 5. CAN attack hostile mobs
                for (UUID mobId : hostileMobs) {
                    assertTrue(FriendlyFireFilter.canAttack(attacker, mobId, null),
                        "FriendlyFireFilter MUST allow attacking hostile mobs");
                }
            }
        }
    }

    // =========================================================================
    // 3. Tap Abilities Stress: Concurrent tap attempts & double-tap protection
    // =========================================================================
    @Test
    @DisplayName("Challenger 3A: Tap Ability Concurrent Tap Stress Test on Llanowar Elves")
    void testConcurrentTapAttemptsOnLlanowarElves() throws Exception {
        UUID ownerId = UUID.randomUUID();
        SummonedCreature elf = new SummonedCreature(ownerId, TestCreatures.LLANOWAR_ELVES);
        elf.clearSummoningSickness();

        TriggerBus bus = new TriggerBus();
        AtomicInteger tapEventsCount = new AtomicInteger(0);
        bus.registerListener(TapPermanentEvent.class, e -> tapEventsCount.incrementAndGet());

        TappableManaAbility ability = new TappableManaAbility(elf, ManaColor.GREEN, 1, bus);

        ManaPool pool = ManaManager.getInstance().getOrCreatePlayerMana(ownerId);
        pool.clear();
        pool.setMaxMana(20);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successActivations = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean activated = ability.activate(ownerId);
                    if (activated) {
                        successActivations.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads timed out during concurrent tap");
        executor.shutdown();

        // INVARIANT: Llanowar Elves can ONLY be tapped ONCE before being untapped!
        assertEquals(1, successActivations.get(),
            "Double-tap race condition! Exactly 1 thread should succeed in tapping the creature, but got: " + successActivations.get());
        assertEquals(1, pool.get(ManaColor.GREEN),
            "Mana pool should only gain exactly 1 Green mana, but got: " + pool.get(ManaColor.GREEN));
        assertEquals(1, tapEventsCount.get(),
            "TapPermanentEvent should only be fired once, but was fired: " + tapEventsCount.get());
        assertTrue(elf.isTapped(), "Elf must be tapped");
    }

    @Test
    @DisplayName("Challenger 3B: Tap Ability under Summoning Sickness Concurrent Stress")
    void testConcurrentTapDuringSummoningSickness() throws Exception {
        UUID ownerId = UUID.randomUUID();
        SummonedCreature elf = new SummonedCreature(ownerId, TestCreatures.LLANOWAR_ELVES);
        // Do NOT clear summoning sickness
        assertTrue(elf.hasSummoningSickness());

        TappableManaAbility ability = new TappableManaAbility(elf, ManaColor.GREEN, 1);
        ManaPool pool = ManaManager.getInstance().getOrCreatePlayerMana(ownerId);
        pool.clear();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successActivations = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean activated = ability.activate(ownerId);
                    if (activated) {
                        successActivations.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, successActivations.get(), "0 threads should succeed while creature has summoning sickness");
        assertEquals(0, pool.get(ManaColor.GREEN));
        assertFalse(elf.isTapped(), "Elf must remain untapped");
    }

    @Test
    @DisplayName("Challenger 3C: Concurrent Attack vs Tap Ability on Llanowar Elves")
    void testConcurrentAttackAndTapOnNonVigilantCreature() throws Exception {
        UUID ownerId = UUID.randomUUID();
        SummonedCreature elf = new SummonedCreature(ownerId, TestCreatures.LLANOWAR_ELVES);
        elf.clearSummoningSickness();
        assertFalse(elf.hasVigilance(), "Llanowar Elves does not have vigilance");

        TappableManaAbility ability = new TappableManaAbility(elf, ManaColor.GREEN, 1);
        ManaPool pool = ManaManager.getInstance().getOrCreatePlayerMana(ownerId);
        pool.clear();
        pool.setMaxMana(20);

        UUID enemyTargetId = UUID.randomUUID();

        int rounds = 50;
        int doubleActionCount = 0;

        for (int r = 0; r < rounds; r++) {
            elf.untap();
            pool.clear();

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);

            AtomicBoolean attackSucceeded = new AtomicBoolean(false);
            AtomicBoolean tapSucceeded = new AtomicBoolean(false);

            Thread attackThread = new Thread(() -> {
                try {
                    startLatch.await();
                    float dmg = elf.executeAttack(enemyTargetId, false, 20.0f, null);
                    if (dmg > 0.0f) {
                        attackSucceeded.set(true);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });

            Thread tapThread = new Thread(() -> {
                try {
                    startLatch.await();
                    boolean tapped = ability.activate(ownerId);
                    if (tapped) {
                        tapSucceeded.set(true);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });

            attackThread.start();
            tapThread.start();
            startLatch.countDown();
            assertTrue(doneLatch.await(2, TimeUnit.SECONDS));

            // INVARIANT: In MTG, a non-vigilant creature CANNOT both attack and activate a tap ability simultaneously!
            if (attackSucceeded.get() && tapSucceeded.get()) {
                doubleActionCount++;
            }
        }

        assertEquals(0, doubleActionCount,
            "Creature without Vigilance executed BOTH attack and tap ability in same turn across " + doubleActionCount + " rounds!");
    }
}
