package com.mtg.test.cards;

import com.mtg.cards.casting.*;
import com.mtg.test.fixtures.TestCardFixtures;
import com.mtg.config.CardsConfig;
import com.mtg.deck.DeckManager;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Card Casting Adversarial Stress & Concurrency Suite")
public class CardCastingAdversarialTest {

    private CardCastingService castingService;
    private UUID playerId;
    private PlaneswalkerDeck deck;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        castingService = new CardCastingService(CardsConfig.defaults());
        CardCastingService.setInstance(castingService);
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

    @Test
    @DisplayName("1. High-Concurrency Packet Spam: 32 concurrent threads hammering the same slot; exactly 1 cast succeeds")
    void testConcurrentCastSpamRace() throws Exception {
        deck.equipCard(0, TestCardFixtures.LIGHTNING_BOLT.id(), 60, 0);
        // Provide exactly 1 Red mana
        manaPool.addMana(ManaColor.RED, 1);

        int threads = 32;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startGate.await();
                    CastResult res = castingService.initiateCast(playerId, 0, CastTarget.self());
                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 concurrent cast should succeed");
        assertEquals(threads - 1, rejectedCount.get(), "All other spam attempts must be rejected");
        assertEquals(0, manaPool.get(ManaColor.RED), "Mana pool should have exactly 0 mana remaining (no double deduction)");
    }

    @Test
    @DisplayName("2. Extreme Config Fuzzing: Boundary values for cooldown multipliers and delays remain safe")
    void testConfigBoundaryCalculations() {
        // Multiplier at min clamp (0.05)
        CardsConfig minConfig = new CardsConfig(0.05, 5.0, 0.5, 0.0, true, 0.1, true, 0.5, 1, true, 16, 100);
        CooldownTracker minTracker = new CooldownTracker(minConfig);
        int landCdMin = minTracker.calculateCooldownTicks(TestCardFixtures.PLAINS, playerId);
        assertTrue(landCdMin >= 1, "Cooldown ticks must be at least 1");

        // Multiplier at max clamp (20.0)
        CardsConfig maxConfig = new CardsConfig(20.0, 300.0, 60.0, 0.50, true, 3.0, true, 20.0, 10, true, 128, 72000);
        CooldownTracker maxTracker = new CooldownTracker(maxConfig);
        int landCdMax = maxTracker.calculateCooldownTicks(TestCardFixtures.PLAINS, playerId);
        assertTrue(landCdMax > 0 && landCdMax < Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("3. Target Fuzzing: Non-finite target coordinates and out-of-range slot indices handled gracefully")
    void testTargetAndSlotFuzzing() {
        // Invalid negative slot
        CastResult negResult = castingService.initiateCast(playerId, -1, CastTarget.self());
        assertFalse(negResult.isSuccess());

        // Out of capacity slot
        CastResult outResult = castingService.initiateCast(playerId, 999, CastTarget.self());
        assertFalse(outResult.isSuccess());

        // Target with NaN and Infinities
        deck.equipCard(0, TestCardFixtures.PLAINS.id(), 1200, 0);
        CastTarget extremeTarget = new CastTarget(-1, Double.NaN, Double.POSITIVE_INFINITY, -1e12, false);
        CastResult extremeResult = castingService.initiateCast(playerId, 0, extremeTarget);
        assertTrue(extremeResult.isSuccess(), "Extreme coordinates should not throw unhandled exceptions");
    }
}
