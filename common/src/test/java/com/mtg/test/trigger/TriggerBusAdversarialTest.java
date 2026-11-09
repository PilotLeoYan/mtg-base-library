package com.mtg.test.trigger;

import com.mtg.api.trigger.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — TriggerBus Concurrency, Cancellation & Re-entrancy")
public class TriggerBusAdversarialTest {

    private TriggerBus bus;

    // Custom test events
    public static class TestBaseEvent extends TriggerEvent {
        private final int payload;
        public TestBaseEvent(Object source, int payload) {
            super(source);
            this.payload = payload;
        }
        public int getPayload() { return payload; }
    }

    public static class TestSubEvent extends TestBaseEvent {
        public TestSubEvent(Object source, int payload) {
            super(source, payload);
        }
    }

    public static class CascadingEventA extends TriggerEvent {
        public CascadingEventA(Object source) { super(source); }
    }

    public static class CascadingEventB extends TriggerEvent {
        public CascadingEventB(Object source) { super(source); }
    }

    @BeforeEach
    void setUp() {
        bus = new TriggerBus();
    }

    @Nested
    @DisplayName("1. Event Cancellation & Monitor Phase Semantics")
    class CancellationSemanticsTests {

        @Test
        @DisplayName("Cancellation at HIGHEST priority skips all lower priorities but delivers to all MONITOR listeners")
        void testHighestCancellationSkipsAllExceptMonitors() {
            List<String> executionLog = Collections.synchronizedList(new ArrayList<>());

            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGHEST, event -> {
                executionLog.add("HIGHEST");
                event.setCanceled(true);
            });
            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGH, event -> executionLog.add("HIGH"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, event -> executionLog.add("NORMAL"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOW, event -> executionLog.add("LOW"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOWEST, event -> executionLog.add("LOWEST"));

            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, event -> {
                assertTrue(event.isCanceled(), "Monitor must observe cancellation state true");
                executionLog.add("MONITOR_1");
            });
            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, event -> {
                assertTrue(event.isCanceled());
                executionLog.add("MONITOR_2");
            });

            TestBaseEvent event = bus.post(new TestBaseEvent(this, 1));

            assertTrue(event.isCanceled());
            assertEquals(List.of("HIGHEST", "MONITOR_1", "MONITOR_2"), executionLog);
        }

        @Test
        @DisplayName("Cancellation at NORMAL priority allows HIGHEST and HIGH, skips LOW and LOWEST, delivers to MONITOR")
        void testNormalCancellationAllowsHigherSkipsLower() {
            List<String> executionLog = Collections.synchronizedList(new ArrayList<>());

            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGHEST, event -> executionLog.add("HIGHEST"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGH, event -> executionLog.add("HIGH"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, event -> {
                executionLog.add("NORMAL");
                event.setCanceled(true);
            });
            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOW, event -> executionLog.add("LOW"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOWEST, event -> executionLog.add("LOWEST"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, event -> executionLog.add("MONITOR"));

            bus.post(new TestBaseEvent(this, 10));

            assertEquals(List.of("HIGHEST", "HIGH", "NORMAL", "MONITOR"), executionLog);
        }

        @Test
        @DisplayName("Pre-canceled event skips all standard priorities and directly executes MONITOR listeners")
        void testPreCanceledEventDirectToMonitors() {
            List<String> executionLog = Collections.synchronizedList(new ArrayList<>());

            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGHEST, event -> executionLog.add("HIGHEST"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, event -> executionLog.add("NORMAL"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOWEST, event -> executionLog.add("LOWEST"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, event -> executionLog.add("MONITOR"));

            TestBaseEvent event = new TestBaseEvent(this, 99);
            event.setCanceled(true);

            bus.post(event);

            assertEquals(List.of("MONITOR"), executionLog);
        }

        @Test
        @DisplayName("Non-canceled event executes every phase strictly in descending priority order down to MONITOR")
        void testFullNonCanceledPipelineOrder() {
            List<String> executionLog = Collections.synchronizedList(new ArrayList<>());

            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOW, event -> executionLog.add("LOW"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGHEST, event -> executionLog.add("HIGHEST"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, event -> executionLog.add("MONITOR"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, event -> executionLog.add("NORMAL"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.LOWEST, event -> executionLog.add("LOWEST"));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGH, event -> executionLog.add("HIGH"));

            bus.post(new TestBaseEvent(this, 0));

            assertEquals(List.of("HIGHEST", "HIGH", "NORMAL", "LOW", "LOWEST", "MONITOR"), executionLog);
        }
    }

    @Nested
    @DisplayName("2. High-Load Multi-Threaded Dispatch")
    class HighLoadConcurrencyTests {

        @Test
        @DisplayName("16 threads dispatching 20,000 events concurrently achieves 100% receipt without race condition")
        void testHeavyConcurrentDispatch() throws InterruptedException, ExecutionException {
            int numThreads = 16;
            int eventsPerThread = 1250; // Total 20,000 events
            AtomicInteger totalReceived = new AtomicInteger(0);
            AtomicInteger monitorReceived = new AtomicInteger(0);

            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, event -> {
                totalReceived.incrementAndGet();
            });

            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, event -> {
                monitorReceived.incrementAndGet();
            });

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < eventsPerThread; i++) {
                        bus.post(new TestBaseEvent(this, threadId * 10000 + i));
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get();
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            assertEquals(20000, totalReceived.get(), "All 20,000 events must be received by normal listener");
            assertEquals(20000, monitorReceived.get(), "All 20,000 events must be received by monitor listener");
        }
    }

    @Nested
    @DisplayName("3. Dynamic Registration & Unregistration during Active Dispatch")
    class DynamicRegistrationConcurrencyTests {

        @Test
        @DisplayName("Concurrent registration, unregistration, and dispatch across 12 threads without CME or stalls")
        void testConcurrentRegistrationUnregistrationAndDispatch() throws InterruptedException, ExecutionException {
            int workerCount = 12;
            ExecutorService executor = Executors.newFixedThreadPool(workerCount);
            List<Future<?>> futures = new ArrayList<>();
            AtomicBoolean stop = new AtomicBoolean(false);
            AtomicInteger dispatches = new AtomicInteger(0);

            TriggerListener<TestBaseEvent> persistentListener = event -> dispatches.incrementAndGet();
            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, persistentListener);

            // 4 Threads posting events continuously
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    while (!stop.get()) {
                        bus.post(new TestBaseEvent(this, 1));
                    }
                }));
            }

            // 4 Threads dynamically registering and unregistering transient listeners
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    List<TriggerListener<TestBaseEvent>> registered = new ArrayList<>();
                    for (int k = 0; k < 100 && !stop.get(); k++) {
                        TriggerListener<TestBaseEvent> l = event -> {};
                        bus.registerListener(TestBaseEvent.class, TriggerPriority.LOW, l);
                        registered.add(l);
                        if (registered.size() > 5) {
                            TriggerListener<TestBaseEvent> toRemove = registered.remove(0);
                            bus.unregisterListener(toRemove);
                        }
                    }
                    // Clean up
                    for (TriggerListener<TestBaseEvent> l : registered) {
                        bus.unregisterListener(l);
                    }
                }));
            }

            // 4 Threads verifying listener queries and typed unregistration
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    for (int k = 0; k < 100 && !stop.get(); k++) {
                        TriggerListener<TestBaseEvent> l = event -> {};
                        bus.registerListener(TestBaseEvent.class, 50, l);
                        bus.unregisterListener(TestBaseEvent.class, l);
                    }
                }));
            }

            // Let it run for 1 second under full load
            Thread.sleep(1000);
            stop.set(true);

            for (Future<?> f : futures) {
                f.get();
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertTrue(dispatches.get() > 1000, "Should have safely dispatched thousands of events under concurrent registration/unregistration");
        }

        @Test
        @DisplayName("Self-unregistering listener inside onTrigger() removes itself cleanly and does not execute again")
        void testSelfUnregisteringListener() {
            AtomicInteger executions = new AtomicInteger(0);

            TriggerListener<TestBaseEvent> selfRemoving = new TriggerListener<>() {
                @Override
                public void onTrigger(TestBaseEvent event) {
                    executions.incrementAndGet();
                    bus.unregisterListener(this);
                }
            };

            bus.registerListener(TestBaseEvent.class, selfRemoving);

            for (int i = 0; i < 10; i++) {
                bus.post(new TestBaseEvent(this, i));
            }

            assertEquals(1, executions.get(), "Self-unregistering listener must only run once across 10 dispatches");
        }

        @Test
        @DisplayName("Dynamically registering a listener inside onTrigger() takes effect for subsequent events")
        void testDynamicRegistrationInsideTrigger() {
            AtomicInteger dynamicInvocations = new AtomicInteger(0);

            bus.registerListener(TestBaseEvent.class, event -> {
                if (event.getPayload() == 1) {
                    bus.registerListener(TestBaseEvent.class, e -> dynamicInvocations.incrementAndGet());
                }
            });

            bus.post(new TestBaseEvent(this, 1));
            // First event: dynamic listener was registered during dispatch
            assertEquals(0, dynamicInvocations.get());

            bus.post(new TestBaseEvent(this, 2));
            // Second event: dynamic listener receives event
            assertEquals(1, dynamicInvocations.get());

            bus.post(new TestBaseEvent(this, 3));
            assertEquals(2, dynamicInvocations.get());
        }
    }

    @Nested
    @DisplayName("4. Re-entrant & Cascading Event Dispatch")
    class ReentrantDispatchTests {

        @Test
        @DisplayName("Nested cascading event dispatch (EventA -> EventB) completes cleanly in correct order")
        void testCascadingEventDispatch() {
            List<String> log = new ArrayList<>();

            bus.registerListener(CascadingEventA.class, event -> {
                log.add("START_A");
                bus.post(new CascadingEventB(this));
                log.add("END_A");
            });

            bus.registerListener(CascadingEventB.class, event -> {
                log.add("RUN_B");
            });

            bus.post(new CascadingEventA(this));

            assertEquals(List.of("START_A", "RUN_B", "END_A"), log);
        }
    }

    @Nested
    @DisplayName("5. Exception Isolation under Hostile Listeners")
    class ExceptionIsolationTests {

        @Test
        @DisplayName("Faulty listener throwing RuntimeException does not crash caller or block other listeners")
        void testRuntimeExceptionIsolation() {
            AtomicBoolean firstRan = new AtomicBoolean(false);
            AtomicBoolean thirdRan = new AtomicBoolean(false);

            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGHEST, e -> firstRan.set(true));
            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGH, e -> {
                throw new IllegalStateException("Simulated sabotage");
            });
            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, e -> thirdRan.set(true));

            TestBaseEvent result = assertDoesNotThrow(() -> bus.post(new TestBaseEvent(this, 42)));

            assertNotNull(result);
            assertTrue(firstRan.get());
            assertTrue(thirdRan.get());
        }

        @Test
        @DisplayName("Faulty listener throwing Error (Throwable) is caught and does not kill dispatch loop")
        void testThrowableIsolation() {
            AtomicBoolean monitorRan = new AtomicBoolean(false);

            bus.registerListener(TestBaseEvent.class, TriggerPriority.NORMAL, e -> {
                throw new AssertionError("Simulated assertion crash");
            });
            bus.registerListener(TestBaseEvent.class, TriggerPriority.MONITOR, e -> monitorRan.set(true));

            assertDoesNotThrow(() -> bus.post(new TestBaseEvent(this, 1)));
            assertTrue(monitorRan.get(), "Monitor listener must run even after an Error in an earlier listener");
        }
    }

    @Nested
    @DisplayName("6. Polymorphic Event Hierarchy Dispatch")
    class PolymorphismTests {

        @Test
        @DisplayName("Subclass event dispatches to listeners of superclass and subclass, respecting global priorities")
        void testPolymorphicHierarchyDispatch() {
            List<String> log = new ArrayList<>();

            // Registered for superclass TriggerEvent at NORMAL
            bus.registerListener(TriggerEvent.class, TriggerPriority.NORMAL, e -> log.add("ROOT_NORMAL"));

            // Registered for TestBaseEvent at HIGH
            bus.registerListener(TestBaseEvent.class, TriggerPriority.HIGH, e -> log.add("BASE_HIGH"));

            // Registered for TestSubEvent at HIGHEST
            bus.registerListener(TestSubEvent.class, TriggerPriority.HIGHEST, e -> log.add("SUB_HIGHEST"));

            // Posting leaf subclass
            bus.post(new TestSubEvent(this, 1));

            // Must execute HIGHEST -> HIGH -> NORMAL across the hierarchy
            assertEquals(List.of("SUB_HIGHEST", "BASE_HIGH", "ROOT_NORMAL"), log);

            log.clear();

            // Posting middle base event
            bus.post(new TestBaseEvent(this, 2));

            // Only BASE_HIGH and ROOT_NORMAL should be called (SUB_HIGHEST is not a superclass of TestBaseEvent)
            assertEquals(List.of("BASE_HIGH", "ROOT_NORMAL"), log);
        }
    }

    @Nested
    @DisplayName("7. Bus Clear Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("bus.clear() purges all listeners and subsequent dispatches invoke nothing")
        void testClearPurgesAllListeners() {
            AtomicInteger invocations = new AtomicInteger(0);

            bus.registerListener(TestBaseEvent.class, e -> invocations.incrementAndGet());
            bus.registerListener(TestSubEvent.class, e -> invocations.incrementAndGet());

            bus.post(new TestSubEvent(this, 1));
            assertEquals(2, invocations.get());

            bus.clear();

            bus.post(new TestSubEvent(this, 2));
            bus.post(new TestBaseEvent(this, 3));
            assertEquals(2, invocations.get(), "No listeners should execute after bus.clear()");
        }
    }
}
