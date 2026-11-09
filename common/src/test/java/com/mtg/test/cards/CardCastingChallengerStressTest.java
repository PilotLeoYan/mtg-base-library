package com.mtg.test.cards;

import com.mtg.api.trigger.CastSpellEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.casting.*;
import com.mtg.test.fixtures.TestCardFixtures;
import com.mtg.cards.effects.ActiveEnchantment;
import com.mtg.cards.effects.EnchantmentManager;
import com.mtg.cards.effects.IEnchantmentTickListener;
import com.mtg.config.CardsConfig;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckManager;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Challenger Empirical Stress & Concurrency Suite")
public class CardCastingChallengerStressTest {

    private CardCastingService castingService;
    private TriggerBus testBus;
    private UUID playerId;
    private PlaneswalkerDeck deck;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        castingService = new CardCastingService(CardsConfig.defaults());
        castingService.setTriggerBus(testBus);
        CardCastingService.setInstance(castingService);
        EnchantmentManager.getInstance().setTriggerBus(testBus);
        EnchantmentManager.getInstance().clearAll();

        TestCardFixtures.registerAllCards(ProgressionManager.getInstance()::registerCard);
        TestCardFixtures.registerAllEffects();

        playerId = UUID.randomUUID();
        deck = DeckManager.getInstance().getDeck(playerId);
        for (int i = 0; i < deck.getCapacity(); i++) {
            deck.unequipCard(i);
            deck.getSlot(i).resetDelays();
        }

        manaPool = ManaManager.getInstance().getOrCreatePlayerMana(playerId);
        manaPool.clear();
        manaPool.setMaxMana(20);
    }

    // =========================================================================
    // 1. CONCURRENT CARD CASTING & COOLDOWN RECOVERY RACES
    // =========================================================================

    @Test
    @DisplayName("Challenger 1A: Multi-Slot Concurrent Casting with Contended Mana")
    void testMultiSlotConcurrentCastingContendedMana() throws Exception {
        // Equip 2 distinct Instant cards: Slot 0 = Lightning Bolt ({R}), Slot 1 = Healing Salve ({W})
        deck.equipCard(0, TestCardFixtures.LIGHTNING_BOLT.id(), 60, 0);
        deck.equipCard(1, TestCardFixtures.HEALING_SALVE.id(), 60, 0);

        // Player has only 1 Red mana, 0 White mana.
        manaPool.addMana(ManaColor.RED, 1);

        int threads = 32;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger redSuccess = new AtomicInteger(0);
        AtomicInteger whiteSuccess = new AtomicInteger(0);
        AtomicInteger totalRejections = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int slot = i % 2; // Alternating between slot 0 (Red) and slot 1 (White)
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    CastResult res = castingService.initiateCast(playerId, slot, CastTarget.self());
                    if (res.isSuccess()) {
                        if (slot == 0) redSuccess.incrementAndGet();
                        else whiteSuccess.incrementAndGet();
                    } else {
                        totalRejections.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, redSuccess.get(), "Exactly one Red cast must succeed");
        assertEquals(0, whiteSuccess.get(), "White cast must fail due to zero white mana");
        assertEquals(threads - 1, totalRejections.get(), "Remaining attempts must be cleanly rejected");
        assertEquals(0, manaPool.get(ManaColor.RED), "Red mana must be exactly 0 (no double deduction)");
    }

    @Test
    @DisplayName("Challenger 1B: Cooldown Recovery Contention: exactly 1 cast succeeds when cooldown reaches 0")
    void testCooldownRecoveryContention() throws Exception {
        // Equip an instant in slot 0 with 2 ticks of cooldown
        deck.equipCard(0, TestCardFixtures.LIGHTNING_BOLT.id(), 60, 0);
        deck.startCooldown(0, 2);
        manaPool.addMana(ManaColor.RED, 10);

        int castThreads = 20;
        AtomicInteger successCasts = new AtomicInteger(0);
        AtomicInteger cooldownRejections = new AtomicInteger(0);

        // Verify rejections while on cooldown (remaining: 2)
        for (int i = 0; i < castThreads; i++) {
            CastResult res = castingService.initiateCast(playerId, 0, CastTarget.self());
            if (res.status() == CastResult.Status.SLOT_ON_COOLDOWN) {
                cooldownRejections.incrementAndGet();
            }
        }

        // Advance cooldown tick 1 (remaining: 1)
        deck.tick();
        for (int i = 0; i < castThreads; i++) {
            CastResult res = castingService.initiateCast(playerId, 0, CastTarget.self());
            if (res.status() == CastResult.Status.SLOT_ON_COOLDOWN) {
                cooldownRejections.incrementAndGet();
            }
        }

        // Advance cooldown tick 2 (remaining: 0 -> READY)
        deck.tick();

        // 20 concurrent threads race to cast the newly ready slot
        ExecutorService executor = Executors.newFixedThreadPool(castThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(castThreads);

        for (int i = 0; i < castThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    CastResult res = castingService.initiateCast(playerId, 0, CastTarget.self());
                    if (res.isSuccess()) {
                        successCasts.incrementAndGet();
                    } else if (res.status() == CastResult.Status.SLOT_ON_COOLDOWN) {
                        cooldownRejections.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(doneGate.await(5, TimeUnit.SECONDS), "Threads timed out during concurrent cast");
        executor.shutdown();

        assertEquals(1, successCasts.get(), "Exactly one cast must succeed when cooldown recovers to READY");
        assertTrue(cooldownRejections.get() > 0, "Cooldown rejections should occur prior to recovery");
        assertEquals(DeckSlotState.COOLDOWN, deck.getSlot(0).getState(deck.getCapacity()));
    }

    @Test
    @DisplayName("Challenger 1C: Competing Channeling Initiation: max 1 channeling spell per player")
    void testCompetingChannelingInitiation() throws Exception {
        // Equip 4 sorceries across slots 0..3
        deck.equipCard(0, TestCardFixtures.WRATH_OF_GOD.id(), 200, 40);
        deck.equipCard(1, TestCardFixtures.DIVINATION.id(), 100, 30);
        deck.equipCard(2, TestCardFixtures.MIND_ROT.id(), 100, 30);
        deck.equipCard(3, TestCardFixtures.PYROCLASM.id(), 80, 20);

        // Give plenty of all mana
        for (ManaColor c : ManaColor.values()) {
            manaPool.addMana(c, 20);
        }

        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger channelingStarted = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int slot = i % 4;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    CastResult res = castingService.initiateCast(playerId, slot, CastTarget.self());
                    if (res.status() == CastResult.Status.CHANNELING_STARTED) {
                        channelingStarted.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, channelingStarted.get(), "Only 1 spell can be actively channeled by a player at a time");
        assertTrue(castingService.getActiveCast(playerId).isPresent());
    }

    // =========================================================================
    // 2. DAMAGE INTERRUPTION RACES: EXACT TICK BOUNDARY
    // =========================================================================

    @RepeatedTest(50)
    @DisplayName("Challenger 2: Damage Interruption vs Windup Finish Boundary Race")
    void testDamageInterruptionVsCompletionRace() throws Exception {
        UUID testPlayer = UUID.randomUUID();
        PlaneswalkerDeck testDeck = DeckManager.getInstance().getDeck(testPlayer);
        testDeck.equipCard(0, TestCardFixtures.PYROCLASM.id(), 80, 2); // 2 ticks windup
        ManaPool pool = ManaManager.getInstance().getOrCreatePlayerMana(testPlayer);
        pool.clear();
        pool.setMaxMana(20);
        pool.addMana(ManaColor.RED, 1);
        pool.addMana(ManaColor.COLORLESS, 1);

        AtomicInteger interruptedEvents = new AtomicInteger(0);
        AtomicInteger castSpellEvents = new AtomicInteger(0);

        TriggerBus bus = new TriggerBus();
        bus.registerListener(SpellCastInterruptedEvent.class, e -> {
            if (e.getCasterId().equals(testPlayer)) {
                interruptedEvents.incrementAndGet();
            }
        });
        bus.registerListener(CastSpellEvent.class, e -> {
            if (e.getCasterId().equals(testPlayer)) {
                castSpellEvents.incrementAndGet();
            }
        });
        castingService.setTriggerBus(bus);

        CastResult result = castingService.initiateCast(testPlayer, 0, CastTarget.self());
        assertEquals(CastResult.Status.CHANNELING_STARTED, result.status());

        // Tick 1 tick
        testDeck.tick();
        castingService.tick();

        // At tick 2: Windup is finishing. Thread A ticks, Thread B damages player!
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicBoolean damageHandled = new AtomicBoolean(false);

        Future<?> tickTask = executor.submit(() -> {
            try {
                barrier.await();
                testDeck.tick();
                castingService.tick();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> damageTask = executor.submit(() -> {
            try {
                barrier.await();
                boolean interrupted = castingService.handlePlayerDamage(testPlayer, 10.0);
                damageHandled.set(interrupted);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        tickTask.get(2, TimeUnit.SECONDS);
        damageTask.get(2, TimeUnit.SECONDS);
        executor.shutdown();

        int intCount = interruptedEvents.get();
        int castCount = castSpellEvents.get();

        // CLEAN BOUNDARY INVARIANT:
        // Either Clean Interruption: intCount == 1 && castCount == 0 && damageHandled == true
        // OR Clean Completion:      intCount == 0 && castCount == 1 && damageHandled == false
        // NEVER: Both fired (double-resolve) or Neither fired (dropped cast)
        assertFalse(intCount == 1 && castCount == 1,
            "RACE DETECTED: Spell was BOTH resolved (CastSpellEvent) AND interrupted (SpellCastInterruptedEvent)!");
        assertFalse(intCount == 0 && castCount == 0,
            "RACE DETECTED: Spell was dropped in limbo (neither resolved nor interrupted)!");

        if (damageHandled.get()) {
            assertEquals(1, intCount, "If damageHandled is true, SpellCastInterruptedEvent must have fired");
            assertEquals(0, castCount, "If damageHandled is true, CastSpellEvent must NOT have fired");
        } else {
            assertEquals(0, intCount, "If damageHandled is false, SpellCastInterruptedEvent must NOT have fired");
            assertEquals(1, castCount, "If damageHandled is false, CastSpellEvent must have fired");
        }
    }

    @Test
    @DisplayName("Challenger 2B: Deterministic Boundary: Resolved cast cannot be interrupted by post-completion damage")
    void testResolvedCastCannotBeInterrupted() {
        deck.equipCard(0, TestCardFixtures.PYROCLASM.id(), 80, 1);
        manaPool.addMana(ManaColor.RED, 1);
        manaPool.addMana(ManaColor.COLORLESS, 1);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertEquals(CastResult.Status.CHANNELING_STARTED, result.status());

        ActiveCast cast = castingService.getActiveCast(playerId).orElseThrow();
        assertEquals(1, cast.getRemainingWindupTicks());

        // Tick once so windup reaches 0 and state transitions to RESOLVED
        cast.tick();
        assertTrue(cast.isFinished(), "Cast must be finished after windup ticks reach 0");
        assertEquals(ActiveCast.CastState.RESOLVED, cast.getState());

        // Damage arriving after windup is finished MUST NOT interrupt the resolved cast!
        boolean interrupted = castingService.handlePlayerDamage(playerId, 10.0);
        assertFalse(interrupted, "Damage arriving after windup has resolved must NOT interrupt the spell");
        assertNotEquals(ActiveCast.CastState.INTERRUPTED, cast.getState(), "Resolved cast state must not be overwritten to INTERRUPTED");
    }

    @Test
    @DisplayName("Challenger 3A: Concurrent Ticking of 2,000 Enchantments with Exactly-Once Expiration")
    void testConcurrentEnchantmentTickingAndExpiration() throws Exception {
        int enchantmentCount = 2000;
        int durationTicks = 5;
        AtomicInteger expireCallbackCount = new AtomicInteger(0);
        AtomicInteger expiredEventCount = new AtomicInteger(0);

        testBus.registerListener(com.mtg.cards.casting.EnchantmentExpiredEvent.class, e -> {
            expiredEventCount.incrementAndGet();
        });

        List<ActiveEnchantment> enchantments = new ArrayList<>(enchantmentCount);
        for (int i = 0; i < enchantmentCount; i++) {
            UUID owner = UUID.randomUUID();
            ActiveEnchantment ench = new ActiveEnchantment(
                owner,
                TestCardFixtures.GLORIOUS_ANTHEM,
                ActiveEnchantment.EnchantmentTargetType.CASTER,
                -1,
                durationTicks,
                new IEnchantmentTickListener() {
                    @Override
                    public void onExpire(ActiveEnchantment e) {
                        expireCallbackCount.incrementAndGet();
                    }
                }
            );
            enchantments.add(ench);
            EnchantmentManager.getInstance().addEnchantment(ench);
        }

        // Concurrently tick using 8 threads
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int tick = 0; tick < durationTicks + 2; tick++) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    EnchantmentManager.getInstance().tickAll();
                }));
            }
            for (Future<?> f : futures) {
                f.get(5, TimeUnit.SECONDS);
            }
        }
        executor.shutdown();

        // Verify that all 2,000 enchantments expired
        for (ActiveEnchantment ench : enchantments) {
            assertEquals(0, EnchantmentManager.getInstance().getPlayerEnchantments(ench.getOwnerId()).size(),
                "All player enchantments must be empty after expiration");
        }

        // Stress check: did every enchantment trigger expire callback EXACTLY once?
        assertEquals(enchantmentCount, expireCallbackCount.get(),
            "Every enchantment must expire exactly once (no duplicate onExpire callbacks under concurrent ticking)");
        assertEquals(enchantmentCount, expiredEventCount.get(),
            "Every enchantment must produce exactly one EnchantmentExpiredEvent");
    }

    @Test
    @DisplayName("Challenger 3B: EnchantmentManager Memory Leak Resistance & Expiration Map Cleanup")
    @SuppressWarnings("unchecked")
    void testEnchantmentMemoryLeakResistance() throws Exception {
        int playerCount = 500;
        int durationTicks = 2;

        for (int i = 0; i < playerCount; i++) {
            UUID owner = UUID.randomUUID();
            ActiveEnchantment ench = new ActiveEnchantment(
                owner,
                TestCardFixtures.LEVITATION,
                ActiveEnchantment.EnchantmentTargetType.CASTER,
                -1,
                durationTicks,
                null
            );
            EnchantmentManager.getInstance().addEnchantment(ench);
        }

        // Tick to expiration
        for (int t = 0; t < durationTicks + 1; t++) {
            EnchantmentManager.getInstance().tickAll();
        }

        // Inspect internal map via reflection to verify whether empty lists are retained
        Field mapField = EnchantmentManager.class.getDeclaredField("activeEnchantments");
        mapField.setAccessible(true);
        Map<UUID, List<ActiveEnchantment>> map = (Map<UUID, List<ActiveEnchantment>>) mapField.get(EnchantmentManager.getInstance());

        // Check if map still holds 500 keys of empty lists
        int mapSize = map.size();
        System.out.println("EnchantmentManager activeEnchantments map size after all expired: " + mapSize);
        assertEquals(0, mapSize, "Active enchantments map must be completely purged of expired player keys (0 leaked keys)");

        // Every list inside should be empty
        for (Map.Entry<UUID, List<ActiveEnchantment>> entry : map.entrySet()) {
            assertTrue(entry.getValue().isEmpty(), "All retained lists must be empty");
        }
    }
}
