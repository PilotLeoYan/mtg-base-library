package com.mtg.test.mana;

import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Subsystem — ManaPool State & Invariant Verification")
public class ManaPoolTest {

    @Test
    @DisplayName("Initial State: Fresh pool has 0 for all colors, 20 max capacity, unchosen affinity")
    void testInitialState() {
        UUID playerId = UUID.randomUUID();
        ManaPool pool = new ManaPool(playerId, 20);

        assertEquals(playerId, pool.getPlayerId());
        assertEquals(20, pool.getMaxMana());
        assertEquals(0, pool.getTotalMana());
        for (ManaColor color : ManaColor.values()) {
            assertEquals(0, pool.get(color), "Expected 0 for " + color);
        }
        assertFalse(pool.hasChosenInitialAffinity());
        assertNull(pool.getChosenAffinity());
    }

    @Test
    @DisplayName("Adding mana within capacity increases balance and total accurately")
    void testAddManaWithinCapacity() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        int added = pool.addMana(ManaColor.WHITE, 5);

        assertEquals(5, added);
        assertEquals(5, pool.get(ManaColor.WHITE));
        assertEquals(5, pool.getTotalMana());
    }

    @Test
    @DisplayName("Adding mana beyond max capacity clamps strictly to maxMana")
    void testAddManaOverflowClampedToMaxCapacity() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        int added = pool.addMana(ManaColor.RED, 25);

        assertEquals(20, added);
        assertEquals(20, pool.get(ManaColor.RED));
        assertEquals(20, pool.getTotalMana());

        // Adding more to a full pool adds 0
        int secondAdd = pool.addMana(ManaColor.BLUE, 5);
        assertEquals(0, secondAdd);
        assertEquals(20, pool.getTotalMana());
        assertEquals(0, pool.get(ManaColor.BLUE));
    }

    @Test
    @DisplayName("Multiple colors respect total capacity constraint")
    void testMultipleColorsTotalCapacityEnforcement() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.RED, 10);
        int blueAdded = pool.addMana(ManaColor.BLUE, 15);

        assertEquals(10, blueAdded);
        assertEquals(10, pool.get(ManaColor.RED));
        assertEquals(10, pool.get(ManaColor.BLUE));
        assertEquals(20, pool.getTotalMana());
    }

    @Test
    @DisplayName("Remove mana succeeds when sufficient and fails without mutating when insufficient")
    void testRemoveManaSuccessAndFailure() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.GREEN, 8);

        assertTrue(pool.removeMana(ManaColor.GREEN, 5));
        assertEquals(3, pool.get(ManaColor.GREEN));
        assertEquals(3, pool.getTotalMana());

        // Attempting to remove more than balance fails
        assertFalse(pool.removeMana(ManaColor.GREEN, 4));
        assertEquals(3, pool.get(ManaColor.GREEN), "Balance must remain unchanged after failed removal");

        // Zero removal succeeds
        assertTrue(pool.removeMana(ManaColor.GREEN, 0));
        assertEquals(3, pool.get(ManaColor.GREEN));

        // Negative removal fails
        assertFalse(pool.removeMana(ManaColor.GREEN, -1));
        assertEquals(3, pool.get(ManaColor.GREEN));
    }

    @Test
    @DisplayName("Set initial affinity marks state and seeds starting mana")
    void testSetInitialAffinity() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.setInitialAffinity(ManaColor.BLACK, 20);

        assertTrue(pool.hasChosenInitialAffinity());
        assertEquals(ManaColor.BLACK, pool.getChosenAffinity());
        assertEquals(20, pool.get(ManaColor.BLACK));
        assertEquals(20, pool.getTotalMana());
    }

    @Test
    @DisplayName("Binary serialization round-trip preserves all state fields")
    void testBinarySerializationRoundTrip() {
        UUID playerId = UUID.randomUUID();
        ManaPool original = new ManaPool(playerId, 2, 4, 6, 8, 10, 5, 50, true, ManaColor.GREEN);

        byte[] serialized = original.toBytes();
        assertEquals(46, serialized.length, "Expected 46-byte compact binary encoding");

        ManaPool deserialized = ManaPool.fromBytes(serialized);
        assertEquals(original.getPlayerId(), deserialized.getPlayerId());
        assertEquals(original.get(ManaColor.WHITE), deserialized.get(ManaColor.WHITE));
        assertEquals(original.get(ManaColor.BLUE), deserialized.get(ManaColor.BLUE));
        assertEquals(original.get(ManaColor.BLACK), deserialized.get(ManaColor.BLACK));
        assertEquals(original.get(ManaColor.RED), deserialized.get(ManaColor.RED));
        assertEquals(original.get(ManaColor.GREEN), deserialized.get(ManaColor.GREEN));
        assertEquals(original.get(ManaColor.COLORLESS), deserialized.get(ManaColor.COLORLESS));
        assertEquals(original.getMaxMana(), deserialized.getMaxMana());
        assertEquals(original.hasChosenInitialAffinity(), deserialized.hasChosenInitialAffinity());
        assertEquals(original.getChosenAffinity(), deserialized.getChosenAffinity());
    }

    @Test
    @DisplayName("Map/NBT adapter round-trip preserves pool fields")
    void testMapSerializationRoundTrip() {
        UUID playerId = UUID.randomUUID();
        ManaPool original = new ManaPool(playerId, 1, 2, 3, 4, 5, 0, 30, true, ManaColor.BLUE);

        Map<String, Object> map = original.toMap();
        ManaPool deserialized = ManaPool.fromMap(playerId, map);

        assertEquals(original.getPlayerId(), deserialized.getPlayerId());
        assertEquals(original.get(ManaColor.WHITE), deserialized.get(ManaColor.WHITE));
        assertEquals(original.get(ManaColor.BLUE), deserialized.get(ManaColor.BLUE));
        assertEquals(original.get(ManaColor.BLACK), deserialized.get(ManaColor.BLACK));
        assertEquals(original.get(ManaColor.RED), deserialized.get(ManaColor.RED));
        assertEquals(original.get(ManaColor.GREEN), deserialized.get(ManaColor.GREEN));
        assertEquals(original.get(ManaColor.COLORLESS), deserialized.get(ManaColor.COLORLESS));
        assertEquals(original.getMaxMana(), deserialized.getMaxMana());
        assertEquals(original.hasChosenInitialAffinity(), deserialized.hasChosenInitialAffinity());
        assertEquals(original.getChosenAffinity(), deserialized.getChosenAffinity());
    }

    @Test
    @DisplayName("Reducing maxMana trims excess mana from pool")
    void testClampingOnMaxManaReduction() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 30);
        pool.addMana(ManaColor.COLORLESS, 10);
        pool.addMana(ManaColor.WHITE, 10);
        assertEquals(20, pool.getTotalMana());

        pool.setMaxMana(15);
        assertEquals(15, pool.getMaxMana());
        assertEquals(15, pool.getTotalMana(), "Total mana must clamp to new capacity 15");
        // Colorless is trimmed first: was 10, trimmed by 5 -> 5 remaining
        assertEquals(5, pool.get(ManaColor.COLORLESS));
        assertEquals(10, pool.get(ManaColor.WHITE));
    }

    @Test
    @DisplayName("Clear resets all balances to zero")
    void testClear() {
        ManaPool pool = new ManaPool(UUID.randomUUID(), 20);
        pool.addMana(ManaColor.RED, 10);
        pool.addMana(ManaColor.GREEN, 5);

        pool.clear();
        assertEquals(0, pool.getTotalMana());
        for (ManaColor c : ManaColor.values()) {
            assertEquals(0, pool.get(c));
        }
    }
}
