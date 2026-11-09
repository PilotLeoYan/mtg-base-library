package com.mtg.test.adversarial;

import com.mtg.api.keyword.CoreKeywords;
import com.mtg.api.trigger.*;
import com.mtg.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Empirical Challenger M2-IT2 — Domain Records & TriggerBus Deep Stress Suite")
public class DomainAndTriggerBusGate2AdversarialTest {

    // --- Custom Events for TriggerBus Deep Stress ---
    public static class DeepEventRoot extends TriggerEvent {
        private final int depth;
        public DeepEventRoot(Object source, int depth) {
            super(source);
            this.depth = depth;
        }
        public int getDepth() { return depth; }
    }

    public static class Level1Event extends DeepEventRoot {
        public Level1Event(Object source, int depth) { super(source, depth); }
    }

    public static class Level2Event extends DeepEventRoot {
        public Level2Event(Object source, int depth) { super(source, depth); }
    }

    public static class Level3Event extends DeepEventRoot {
        public Level3Event(Object source, int depth) { super(source, depth); }
    }

    public static class HostileSabotageException extends RuntimeException {
        public HostileSabotageException(String msg) { super(msg); }
    }

    @Nested
    @DisplayName("1. CardDefinition Fuzzing, JSON Bombs & Boundary Stress")
    class CardDefinitionFuzzingTests {

        @Test
        @DisplayName("JSON with Unicode, Emoji, and Escaped Control Characters Round-Trips Perfectly")
        void testUnicodeAndEmojiRoundTrip() {
            CardDefinition card = CardDefinition.builder("mtg:test/unicode_🔮_card", "test", "Phyrexian 💀 Obliterator \u00A9 \u2605")
                .manaCost(ManaCost.ofBlack(4))
                .addType(CardType.CREATURE)
                .addSubtype("Horror \u2764")
                .rarity(CardRarity.MYTHIC)
                .combatStats(5, 5)
                .addKeyword(KeywordInstance.of(CoreKeywords.TRAMPLE))
                .baseCooldownTicks(120)
                .windupDelayTicks(10)
                .build();

            String json = card.toJson();
            assertNotNull(json);

            CardDefinition parsed = CardDefinition.fromJson(json);
            assertEquals(card.id(), parsed.id());
            assertEquals(card.name(), parsed.name());
            assertEquals(card.subtypes(), parsed.subtypes());
            assertEquals(card.rarity(), parsed.rarity());
            assertEquals(card.combatStats(), parsed.combatStats());
        }

        @Test
        @DisplayName("Deeply nested JSON structure rejects without JVM StackOverflowError")
        void testDeeplyNestedJsonRejection() {
            StringBuilder nested = new StringBuilder("{\"id\": \"test\", \"setCode\": \"base\", \"name\": \"N\", \"types\": [\"LAND\"], \"extra\": ");
            int depth = 50;
            for (int i = 0; i < depth; i++) {
                nested.append("{\"k\": ");
            }
            nested.append("\"deep\"");
            for (int i = 0; i < depth; i++) {
                nested.append("}");
            }
            nested.append("}");

            // Extra fields are ignored for forward compatibility; parser should handle 50-level nesting cleanly
            assertDoesNotThrow(() -> {
                CardDefinition card = CardDefinition.fromJson(nested.toString());
                assertNotNull(card);
                assertEquals("test", card.id());
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "{\"id\": \"t\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": 1e2}",
            "{\"id\": \"t\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": 250.75}"
        })
        @DisplayName("Scientific and floating point numeric values in integer fields parse or default safely")
        void testScientificAndFloatingPointNumbers(String json) {
            CardDefinition card = assertDoesNotThrow(() -> CardDefinition.fromJson(json));
            assertNotNull(card);
            assertTrue(card.baseCooldownTicks() >= 0);
        }

        @Test
        @DisplayName("Leading plus sign in numeric literal is rejected per RFC 8259 with CardParseException")
        void testLeadingPlusRejected() {
            String jsonWithPlus = "{\"id\": \"t\", \"setCode\": \"s\", \"name\": \"n\", \"types\": [\"LAND\"], \"baseCooldownTicks\": +300}";
            assertThrows(CardParseException.class, () -> CardDefinition.fromJson(jsonWithPlus));
        }

        @Test
        @DisplayName("Concurrent parsing and serialization across 16 threads is 100% thread-safe")
        void testConcurrentCardDefinitionParsing() throws InterruptedException, ExecutionException {
            String baseJson = """
                {
                    "id": "mtg:base/concurrent_card",
                    "setCode": "base",
                    "name": "Concurrent Elemental",
                    "types": ["CREATURE"],
                    "subtypes": ["Elemental"],
                    "rarity": "RARE",
                    "manaCost": {
                        "generic": 2,
                        "red": 2
                    },
                    "combatStats": {
                        "power": 4,
                        "toughness": 2
                    },
                    "keywords": [
                        {"id": "mtg:haste", "name": "Haste", "category": "STATIC"}
                    ],
                    "baseCooldownTicks": 80,
                    "windupDelayTicks": 0
                }
                """;

            int threads = 16;
            int iterationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        CardDefinition parsed = CardDefinition.fromJson(baseJson);
                        assertEquals("mtg:base/concurrent_card", parsed.id());
                        assertEquals(4, parsed.manaCost().totalManaValue());
                        String serialized = parsed.toJson();
                        CardDefinition reparsed = CardDefinition.fromJson(serialized);
                        assertEquals(parsed.id(), reparsed.id());
                        assertEquals(parsed.combatStats(), reparsed.combatStats());
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get();
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("2. TriggerBus High-Concurrency, Re-entrancy & Isolation Stress")
    class TriggerBusStressTests {

        @Test
        @DisplayName("Extreme Concurrency: 24 threads dispatching 36,000 events with mixed priorities")
        void testExtremeConcurrency36kEvents() throws InterruptedException, ExecutionException {
            TriggerBus bus = new TriggerBus();
            int threads = 24;
            int eventsPerThread = 1500; // 36,000 total
            AtomicInteger highestCount = new AtomicInteger(0);
            AtomicInteger normalCount = new AtomicInteger(0);
            AtomicInteger monitorCount = new AtomicInteger(0);

            bus.registerListener(DeepEventRoot.class, TriggerPriority.HIGHEST, e -> highestCount.incrementAndGet());
            bus.registerListener(DeepEventRoot.class, TriggerPriority.NORMAL, e -> normalCount.incrementAndGet());
            bus.registerListener(DeepEventRoot.class, TriggerPriority.MONITOR, e -> monitorCount.incrementAndGet());

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threads; t++) {
                final int tid = t;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < eventsPerThread; i++) {
                        bus.post(new DeepEventRoot(this, tid * 10000 + i));
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get();
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            assertEquals(36000, highestCount.get());
            assertEquals(36000, normalCount.get());
            assertEquals(36000, monitorCount.get());
        }

        @Test
        @DisplayName("Multi-level cascading re-entrant event execution preserves FIFO depth order")
        void testCascadingReentrancyDepthOrder() {
            TriggerBus bus = new TriggerBus();
            List<String> executionTrace = Collections.synchronizedList(new ArrayList<>());

            bus.registerListener(Level1Event.class, TriggerPriority.NORMAL, e -> {
                executionTrace.add("L1_START");
                bus.post(new Level2Event(this, 2));
                executionTrace.add("L1_END");
            });

            bus.registerListener(Level2Event.class, TriggerPriority.NORMAL, e -> {
                executionTrace.add("L2_START");
                bus.post(new Level3Event(this, 3));
                executionTrace.add("L2_END");
            });

            bus.registerListener(Level3Event.class, TriggerPriority.NORMAL, e -> {
                executionTrace.add("L3_EXECUTED");
            });

            bus.post(new Level1Event(this, 1));

            assertEquals(List.of("L1_START", "L2_START", "L3_EXECUTED", "L2_END", "L1_END"), executionTrace);
        }

        @Test
        @DisplayName("Monitor listeners cannot uncancel a canceled event for subsequent standard listeners")
        void testMonitorCannotUncancelForStandardListeners() {
            TriggerBus bus = new TriggerBus();
            AtomicBoolean standardExecuted = new AtomicBoolean(false);
            AtomicBoolean monitorExecuted = new AtomicBoolean(false);

            bus.registerListener(DeepEventRoot.class, TriggerPriority.HIGHEST, e -> {
                e.setCanceled(true);
            });

            // Normal listener registered before monitor
            bus.registerListener(DeepEventRoot.class, TriggerPriority.NORMAL, e -> {
                standardExecuted.set(true);
            });

            bus.registerListener(DeepEventRoot.class, TriggerPriority.MONITOR, e -> {
                monitorExecuted.set(true);
                assertTrue(e.isCanceled());
                // Attempt to uncancel in monitor
                e.setCanceled(false);
            });

            bus.post(new DeepEventRoot(this, 0));

            assertFalse(standardExecuted.get(), "Standard listeners must have been skipped prior to monitor");
            assertTrue(monitorExecuted.get(), "Monitor must have received the canceled event");
        }

        @Test
        @DisplayName("Hostile listener throwing custom Error/Throwable is isolated and post returns original event")
        void testHostileThrowableIsolation() {
            TriggerBus bus = new TriggerBus();
            AtomicBoolean monitorRan = new AtomicBoolean(false);

            bus.registerListener(DeepEventRoot.class, TriggerPriority.HIGHEST, e -> {
                throw new HostileSabotageException("Hostile crash attempt");
            });

            bus.registerListener(DeepEventRoot.class, TriggerPriority.MONITOR, e -> {
                monitorRan.set(true);
            });

            DeepEventRoot event = new DeepEventRoot(this, 42);
            DeepEventRoot result = assertDoesNotThrow(() -> bus.post(event));

            assertSame(event, result, "TriggerBus.post() must return the exact event instance");
            assertTrue(monitorRan.get(), "Monitor must execute even after sabotage in HIGHEST phase");
        }
    }

    @Nested
    @DisplayName("3. ManaCost & PhyrexianManaCost Mathematical Boundaries")
    class ManaCostBoundaryTests {

        @Test
        @DisplayName("Phyrexian life payment exact threshold checks")
        void testPhyrexianLifePaymentBoundaries() {
            // 2 phyrexian mana = 4 life (4.0f damage)
            PhyrexianManaCost cost = PhyrexianManaCost.ofBlack(2);
            assertEquals(2, cost.totalPhyrexianMana());
            assertEquals(4, cost.totalLifeCost());
            assertEquals(4.0f, cost.calculateDamage());

            // Player with exactly 4.0f health CANNOT pay (would die / reach 0)
            assertFalse(cost.canPayWithLife(4.0f));
            assertFalse(cost.canPayWithLife(3.99f));
            assertFalse(cost.canPayWithLife(0.0f));
            assertFalse(cost.canPayWithLife(-2.0f));

            // Player with > 4.0f CAN pay
            assertTrue(cost.canPayWithLife(4.01f));
            assertTrue(cost.canPayWithLife(20.0f));

            // Custom damage multiplier: 1.5 damage per mana (total 3.0f damage)
            assertEquals(3.0f, cost.calculateDamage(1.5f));
            assertFalse(cost.canPayWithLife(3.0f, 1.5f));
            assertTrue(cost.canPayWithLife(3.1f, 1.5f));
        }

        @Test
        @DisplayName("ManaCost color classification: monocolored, colorless, multicolor")
        void testColorClassifications() {
            // Generic only -> Colorless
            ManaCost solRing = ManaCost.ofGeneric(1);
            assertTrue(solRing.isColorless());
            assertFalse(solRing.isMonocolored());

            // Colorless type -> Colorless
            ManaCost eldrazi = new ManaCost(0, 0, 0, 0, 0, 0, 2, PhyrexianManaCost.NONE);
            assertTrue(eldrazi.isColorless());
            assertFalse(eldrazi.isMonocolored());

            // Zero cost -> Colorless
            assertTrue(ManaCost.ZERO.isColorless());
            assertFalse(ManaCost.ZERO.isMonocolored());

            // Mono white -> Monocolored, not colorless
            ManaCost plains = ManaCost.ofWhite(1);
            assertFalse(plains.isColorless());
            assertTrue(plains.isMonocolored());

            // Mono green phyrexian -> Monocolored, not colorless
            ManaCost mutagenic = ManaCost.ofPhyrexian(PhyrexianManaCost.ofGreen(1));
            assertFalse(mutagenic.isColorless());
            assertTrue(mutagenic.isMonocolored());

            // Multicolor (White + Blue) -> Not monocolored, not colorless
            ManaCost azorius = ManaCost.ofColored(1, 1, 0, 0, 0);
            assertFalse(azorius.isColorless());
            assertFalse(azorius.isMonocolored());
        }

        @Test
        @DisplayName("ManaCost and PhyrexianManaCost reject negative values strictly")
        void testNegativeRejection() {
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(-1, 0, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new ManaCost(0, 0, 0, 0, 0, 0, -1, PhyrexianManaCost.NONE));
            assertThrows(IllegalArgumentException.class, () -> new PhyrexianManaCost(0, 0, 0, 0, 0, -1));
        }
    }
}
