package com.mtg.test.mechanics;

import com.mtg.api.mechanics.one.*;
import com.mtg.api.trigger.CombatDamageEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.PhyrexianManaCost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phyrexia Mechanics, Toxic, Corrupted, Proliferate & SBA Suite")
public class PhyrexiaMechanicsTest {

    private PoisonCounterManager poisonManager;
    private TriggerBus bus;

    @BeforeEach
    void setup() {
        poisonManager = new PoisonCounterManager();
        bus = new TriggerBus();
    }

    @Test
    @DisplayName("Poison counter tracking: accumulation without natural decay")
    void testPoisonCounterTracking() {
        UUID player = UUID.randomUUID();
        assertEquals(0, poisonManager.getPoison(player));

        poisonManager.addPoison(player, 3, null, bus);
        assertEquals(3, poisonManager.getPoison(player));

        poisonManager.addPoison(player, 4, null, bus);
        assertEquals(7, poisonManager.getPoison(player));

        assertFalse(poisonManager.isLethal(player));
    }

    @Test
    @DisplayName("Toxic combat hook adds poison counters to defender on combat damage")
    void testToxicCombatDamage() {
        bus.registerListener(CombatDamageEvent.class, new ToxicCombatDamageListener(poisonManager, bus));

        UUID attackerId = UUID.randomUUID();
        UUID defenderId = UUID.randomUUID();

        IToxic toxicCreature = () -> 2; // Toxic 2
        CombatDamageEvent event = new CombatDamageEvent(this, toxicCreature, null, attackerId, defenderId, 3.0f, true);

        bus.post(event);

        assertEquals(2, poisonManager.getPoison(defenderId), "Defender should receive 2 poison counters from Toxic 2 creature");
    }

    @Test
    @DisplayName("Non-toxic creature dealing combat damage adds zero poison counters")
    void testNonToxicCreatureNoPoison() {
        bus.registerListener(CombatDamageEvent.class, new ToxicCombatDamageListener(poisonManager, bus));

        UUID attackerId = UUID.randomUUID();
        UUID defenderId = UUID.randomUUID();

        Object vanillaCreature = new Object();
        CombatDamageEvent event = new CombatDamageEvent(this, vanillaCreature, null, attackerId, defenderId, 3.0f, true);

        bus.post(event);

        assertEquals(0, poisonManager.getPoison(defenderId));
    }

    @Test
    @DisplayName("State-Based Action: 10 poison counters is lethal, 9 is not")
    void testStateBasedActionLethalAtTenPoison() {
        UUID target = UUID.randomUUID();

        poisonManager.setPoison(target, 9);
        assertFalse(poisonManager.isLethal(target), "9 poison counters must not be lethal");

        poisonManager.addPoison(target, 1, null, bus);
        assertEquals(10, poisonManager.getPoison(target));
        assertTrue(poisonManager.isLethal(target), "10 poison counters must trigger lethal state-based action");
    }

    @Test
    @DisplayName("Configurable poison lethality toggle controls SBA defeat check")
    void testConfigurablePoisonLethalityToggle() {
        UUID target = UUID.randomUUID();
        poisonManager.setPoison(target, 10);

        // When enabled, 10 is lethal
        assertTrue(poisonManager.isLethal(target, 10, true));

        // When disabled via config, 10 is NOT lethal
        assertFalse(poisonManager.isLethal(target, 10, false));

        // With custom threshold 12, 10 is not lethal
        assertFalse(poisonManager.isLethal(target, 12, true));
    }

    @Test
    @DisplayName("Corrupted ability word activates at 3 or more poison counters")
    void testCorruptedCondition() {
        UUID opponent = UUID.randomUUID();

        poisonManager.setPoison(opponent, 0);
        assertFalse(CorruptedEvaluator.isCorrupted(opponent, poisonManager));

        poisonManager.setPoison(opponent, 1);
        assertFalse(CorruptedEvaluator.isCorrupted(opponent, poisonManager));

        poisonManager.setPoison(opponent, 2);
        assertFalse(CorruptedEvaluator.isCorrupted(opponent, poisonManager));

        poisonManager.setPoison(opponent, 3);
        assertTrue(CorruptedEvaluator.isCorrupted(opponent, poisonManager));

        poisonManager.setPoison(opponent, 5);
        assertTrue(CorruptedEvaluator.isCorrupted(opponent, poisonManager));

        // Test multi-opponent evaluator
        UUID cleanOpponent = UUID.randomUUID();
        assertTrue(CorruptedEvaluator.hasAnyCorruptedOpponent(List.of(cleanOpponent, opponent), poisonManager));
    }

    @Test
    @DisplayName("Proliferate increments all existing counter kinds by 1 and ignores unrepresented kinds")
    void testProliferateAction() {
        UUID creatureId = UUID.randomUUID();
        Map<String, Integer> counters = new HashMap<>();
        counters.put("poison", 2);
        counters.put("+1/+1", 1);

        ICounterHolder creature = new ICounterHolder() {
            @Override public UUID getHolderId() { return creatureId; }
            @Override public Map<String, Integer> getCounters() { return counters; }
            @Override public int getCounter(String counterType) { return counters.getOrDefault(counterType, 0); }
            @Override public void addCounter(String counterType, int count) {
                counters.put(counterType, counters.getOrDefault(counterType, 0) + count);
            }
        };

        UUID emptyId = UUID.randomUUID();
        Map<String, Integer> emptyCounters = new HashMap<>();
        ICounterHolder emptyTarget = new ICounterHolder() {
            @Override public UUID getHolderId() { return emptyId; }
            @Override public Map<String, Integer> getCounters() { return emptyCounters; }
            @Override public int getCounter(String counterType) { return emptyCounters.getOrDefault(counterType, 0); }
            @Override public void addCounter(String counterType, int count) {
                emptyCounters.put(counterType, emptyCounters.getOrDefault(counterType, 0) + count);
            }
        };

        Map<UUID, Map<String, Integer>> result = ProliferateExecutor.proliferate(List.of(creature, emptyTarget), poisonManager, bus);

        assertEquals(3, creature.getCounter("poison"));
        assertEquals(2, creature.getCounter("+1/+1"));
        assertEquals(0, creature.getCounter("-1/-1")); // was not present, must not be added

        assertEquals(0, emptyTarget.getCounters().size());
        assertTrue(result.containsKey(creatureId));
        assertFalse(result.containsKey(emptyId));
    }

    @Test
    @DisplayName("Phyrexian mana life payment calculation and safety rules")
    void testPhyrexianManaPayment() {
        PhyrexianManaCost cost = PhyrexianManaCost.ofGreen(1); // 1 {G/P}
        assertEquals(1, cost.totalPhyrexianMana());
        assertEquals(2, cost.totalLifeCost());
        assertEquals(2.0f, cost.calculateDamage()); // 2.0f damage = 1 full heart

        // Player with 6.0f health can afford 2.0f life cost without dying
        float playerHealth = 6.0f;
        assertTrue(cost.canPayWithLife(playerHealth));

        // Player with exactly 2.0f health cannot afford 2.0f life payment (would cause death)
        float lethalHealth = 2.0f;
        assertFalse(cost.canPayWithLife(lethalHealth));

        // Player with 1.5f health cannot afford 2.0f life payment
        float lowHealth = 1.5f;
        assertFalse(cost.canPayWithLife(lowHealth));
    }
}
