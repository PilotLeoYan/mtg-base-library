package com.mtg.test.progression;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import com.mtg.progression.CardUnlockSource;
import com.mtg.progression.PlayerProgression;
import com.mtg.progression.ProgressionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression & Economy — High Concurrency Multi-Threaded Stress Suite")
public class ProgressionConcurrencyTest {

    @Test
    @DisplayName("16 concurrent threads repeatedly adding cards and transmuting wildcards maintain invariant consistency")
    void testConcurrentCardAwardingAndTransmutation() throws InterruptedException {
        ProgressionManager manager = new ProgressionManager();
        UUID playerId = UUID.randomUUID();

        // Register cards
        for (int i = 0; i < 10; i++) {
            manager.registerCard(new CardDefinition(
                    "mtg:common_" + i, "BASE", "name." + i, ManaCost.ZERO,
                    Set.of(CardType.INSTANT), Set.of(), CardRarity.COMMON,
                    Optional.empty(), List.of(), 10, 0
            ));
        }

        // Unlock all cards once so subsequent awards are duplicates
        for (int i = 0; i < 10; i++) {
            manager.awardCard(playerId, "mtg:common_" + i, CardUnlockSource.MINING);
        }

        int threads = 16;
        int operationsPerThread = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successfulAwards = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String cardId = "mtg:common_" + ((threadIdx + i) % 10);
                        manager.awardCard(playerId, cardId, CardUnlockSource.MINING);
                        successfulAwards.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent test timed out");
        executor.shutdown();

        assertEquals(threads * operationsPerThread, successfulAwards.get());

        PlayerProgression progression = manager.getProgression(playerId);
        int remainingCommons = progression.getDuplicateCount(CardRarity.COMMON);
        int uncommons = progression.getWildcardCount(CardRarity.UNCOMMON);
        int totalProcessed = (uncommons * 4) + remainingCommons;

        assertEquals(threads * operationsPerThread, totalProcessed,
                "Invariant: (Uncommon Wildcards * 4) + remaining Common duplicates must equal total duplicate copies awarded");
        assertTrue(remainingCommons >= 0 && remainingCommons < 4, "Remaining commons must be strictly within [0, 3]");
    }
}
