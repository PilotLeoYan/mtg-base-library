package com.mtg.test.trigger;

import com.mtg.api.trigger.*;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TriggerBus Event Dispatching and Priority Suite")
public class TriggerBusDispatchTest {

    private TriggerBus bus;

    @BeforeEach
    void setup() {
        bus = new TriggerBus();
    }

    @Test
    @DisplayName("Land played event dispatches to LandPlayedEvent listener")
    void testLandPlayedEventDispatch() {
        AtomicBoolean called = new AtomicBoolean(false);
        UUID playerId = UUID.randomUUID();
        CardDefinition forest = CardDefinition.builder("mtg:base/forest", "base", "Forest")
            .addType(CardType.LAND)
            .build();

        TriggerListener<LandPlayedEvent> listener = event -> {
            assertEquals(playerId, event.getPlayerId());
            assertEquals(forest, event.getLandCard());
            assertEquals(10, event.getX());
            assertEquals(64, event.getY());
            assertEquals(-20, event.getZ());
            called.set(true);
        };

        bus.registerListener(LandPlayedEvent.class, listener);
        bus.post(new LandPlayedEvent(this, playerId, forest, 10, 64, -20));

        assertTrue(called.get(), "LandPlayedEvent listener should be executed upon posting LandPlayedEvent");
    }

    @Test
    @DisplayName("Cast spell event dispatches to CastSpellEvent listener")
    void testCastSpellEventDispatch() {
        AtomicBoolean called = new AtomicBoolean(false);
        UUID playerId = UUID.randomUUID();
        CardDefinition bolt = CardDefinition.builder("mtg:base/lightning_bolt", "base", "Lightning Bolt")
            .addType(CardType.INSTANT)
            .build();

        TriggerListener<CastSpellEvent> listener = event -> {
            assertEquals(playerId, event.getCasterId());
            assertEquals(bolt, event.getCard());
            assertFalse(event.isKicked());
            assertEquals(0, event.getXValue());
            called.set(true);
        };

        bus.registerListener(CastSpellEvent.class, listener);
        bus.post(new CastSpellEvent(this, playerId, bolt, false, 0));

        assertTrue(called.get(), "CastSpellEvent listener should be executed upon posting CastSpellEvent");
    }

    @Test
    @DisplayName("Combat damage trigger dispatches to combat damage listeners")
    void testCombatDamageTriggerDispatch() {
        AtomicBoolean called = new AtomicBoolean(false);
        UUID attackerId = UUID.randomUUID();
        UUID defenderId = UUID.randomUUID();

        bus.registerListener(CombatDamageEvent.class, event -> {
            assertEquals(attackerId, event.getAttackerId());
            assertEquals(defenderId, event.getDefenderId());
            assertEquals(4.5f, event.getDamageAmount());
            called.set(true);
        });

        bus.post(new CombatDamageEvent(this, attackerId, defenderId, 4.5f));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("Enter the battlefield trigger dispatches successfully")
    void testEnterBattlefieldTriggerDispatch() {
        AtomicBoolean called = new AtomicBoolean(false);
        UUID ownerId = UUID.randomUUID();
        CardDefinition elf = CardDefinition.builder("mtg:base/llanowar_elves", "base", "Llanowar Elves")
            .addType(CardType.CREATURE)
            .build();

        bus.registerListener(EnterBattlefieldEvent.class, event -> {
            assertEquals(ownerId, event.getOwnerId());
            assertEquals(elf, event.getCard());
            called.set(true);
        });

        bus.post(new EnterBattlefieldEvent(this, ownerId, elf, 0, 70, 0));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("Priority ordering: high priority listener executes strictly before low priority listener")
    void testPriorityOrdering() {
        List<String> executionOrder = new ArrayList<>();

        TriggerListener<UpkeepEvent> lowPriority = event -> executionOrder.add("LOW");
        TriggerListener<UpkeepEvent> highPriority = event -> executionOrder.add("HIGH");

        // Register low priority (10) first, then high priority (100)
        bus.registerListener(UpkeepEvent.class, 10, lowPriority);
        bus.registerListener(UpkeepEvent.class, 100, highPriority);

        bus.post(new UpkeepEvent(this, UUID.randomUUID(), 100L));

        assertEquals(List.of("HIGH", "LOW"), executionOrder);
    }

    @Test
    @DisplayName("Enum priority ordering: HIGHEST executes before NORMAL, NORMAL before MONITOR")
    void testEnumPriorityOrdering() {
        List<String> order = new ArrayList<>();

        bus.registerListener(UpkeepEvent.class, TriggerPriority.MONITOR, e -> order.add("MONITOR"));
        bus.registerListener(UpkeepEvent.class, TriggerPriority.NORMAL, e -> order.add("NORMAL"));
        bus.registerListener(UpkeepEvent.class, TriggerPriority.HIGHEST, e -> order.add("HIGHEST"));

        bus.post(new UpkeepEvent(this, UUID.randomUUID(), 1L));

        assertEquals(List.of("HIGHEST", "NORMAL", "MONITOR"), order);
    }

    @Test
    @DisplayName("Unregistering a listener prevents subsequent invocations")
    void testListenerUnregistration() {
        AtomicInteger counter = new AtomicInteger(0);
        TriggerListener<UpkeepEvent> listener = event -> counter.incrementAndGet();

        bus.registerListener(UpkeepEvent.class, listener);
        bus.post(new UpkeepEvent(this, UUID.randomUUID(), 1L));
        assertEquals(1, counter.get());

        bus.unregisterListener(listener);
        bus.post(new UpkeepEvent(this, UUID.randomUUID(), 2L));
        assertEquals(1, counter.get(), "Unregistered listener must not receive further events");
    }

    @Test
    @DisplayName("Exception isolation: faulty listener does not prevent subsequent listeners from executing")
    void testExceptionIsolation() {
        AtomicBoolean secondListenerRan = new AtomicBoolean(false);

        bus.registerListener(UpkeepEvent.class, 100, event -> {
            throw new RuntimeException("Simulated listener failure");
        });

        bus.registerListener(UpkeepEvent.class, 50, event -> {
            secondListenerRan.set(true);
        });

        assertDoesNotThrow(() -> bus.post(new UpkeepEvent(this, UUID.randomUUID(), 1L)));
        assertTrue(secondListenerRan.get(), "Subsequent listeners must execute despite earlier exception");
    }

    @Test
    @DisplayName("Cancellation behavior: canceled event skipped by standard listeners but caught by MONITOR")
    void testCancellationBehavior() {
        AtomicBoolean normalRan = new AtomicBoolean(false);
        AtomicBoolean monitorRan = new AtomicBoolean(false);

        // First listener cancels event
        bus.registerListener(UpkeepEvent.class, TriggerPriority.HIGHEST, event -> event.setCanceled(true));
        // Normal listener should be skipped
        bus.registerListener(UpkeepEvent.class, TriggerPriority.NORMAL, event -> normalRan.set(true));
        // MONITOR listener observes canceled event
        bus.registerListener(UpkeepEvent.class, TriggerPriority.MONITOR, event -> {
            assertTrue(event.isCanceled());
            monitorRan.set(true);
        });

        bus.post(new UpkeepEvent(this, UUID.randomUUID(), 1L));

        assertFalse(normalRan.get(), "Normal listener should be skipped when event is canceled");
        assertTrue(monitorRan.get(), "MONITOR listener must observe canceled events");
    }

    @Test
    @DisplayName("Concurrent dispatch: 1,000 events across 8 parallel threads without race conditions")
    void testConcurrentDispatch() throws InterruptedException, ExecutionException {
        AtomicInteger totalReceived = new AtomicInteger(0);

        bus.registerListener(UpkeepEvent.class, event -> {
            totalReceived.incrementAndGet();
        });

        int numThreads = 8;
        int eventsPerThread = 125;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            futures.add(executor.submit(() -> {
                for (int i = 0; i < eventsPerThread; i++) {
                    bus.post(new UpkeepEvent(this, UUID.randomUUID(), i));
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1000, totalReceived.get(), "All 1,000 concurrently dispatched events must be processed");
    }
}
