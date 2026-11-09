package com.mtg.test.deck;

import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dedicated Card Hand — PlaneswalkerDeck Container Suite")
public class PlaneswalkerDeckTest {

    @Test
    @DisplayName("Initial Deck Capacity: Default 4 unlocked slots, total 7 slots, slots 4..6 locked")
    void testInitialDeckCapacity() {
        UUID playerId = UUID.randomUUID();
        PlaneswalkerDeck deck = new PlaneswalkerDeck(playerId);

        assertEquals(playerId, deck.getPlayerId());
        assertEquals(4, deck.getCapacity());
        assertEquals(7, deck.getAllSlots().size());
        assertEquals(4, deck.getUnlockedSlots().size());

        for (int i = 0; i < 4; i++) {
            assertEquals(DeckSlotState.EMPTY, deck.getSlot(i).getState(deck.getCapacity()));
        }
        for (int i = 4; i < 7; i++) {
            assertEquals(DeckSlotState.LOCKED, deck.getSlot(i).getState(deck.getCapacity()));
        }
    }

    @Test
    @DisplayName("Slot selection and cycling wrap correctly within unlocked capacity bounds")
    void testSlotSelectionAndCycling() {
        PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

        assertEquals(0, deck.getSelectedSlot());
        assertTrue(deck.setSelectedSlot(3));
        assertEquals(3, deck.getSelectedSlot());

        // Selecting out of bounds returns false and does not change selection
        assertFalse(deck.setSelectedSlot(4));
        assertEquals(3, deck.getSelectedSlot());
        assertFalse(deck.setSelectedSlot(-1));
        assertEquals(3, deck.getSelectedSlot());

        // Forward cycling from 3 wraps to 0
        deck.cycleSlot(true);
        assertEquals(0, deck.getSelectedSlot());

        // Backward cycling from 0 wraps to 3
        deck.cycleSlot(false);
        assertEquals(3, deck.getSelectedSlot());
    }

    @Test
    @DisplayName("Equip card transitions slot to READY, unequip transitions to EMPTY, locked equip throws exception")
    void testEquipAndClearCard() {
        PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

        deck.equipCard(0, "mtg:lightning_bolt", 60, 0);
        DeckCardSlot slot0 = deck.getSlot(0);
        assertTrue(slot0.hasCard());
        assertEquals("mtg:lightning_bolt", slot0.getCardId());
        assertEquals(DeckSlotState.READY, slot0.getState(deck.getCapacity()));
        assertTrue(slot0.canCast(deck.getCapacity()));

        // Unequip slot 0
        assertTrue(deck.unequipCard(0));
        assertFalse(slot0.hasCard());
        assertEquals(DeckSlotState.EMPTY, slot0.getState(deck.getCapacity()));

        // Attempting to equip into locked slot 4 throws IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                deck.equipCard(4, "mtg:counterspell", 40, 0)
        );
    }

    @Test
    @DisplayName("Cooldown ticking transitions state from COOLDOWN back to READY at tick 0")
    void testCooldownTicking() {
        PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);
        deck.equipCard(1, "mtg:shock", 3, 0);

        DeckCardSlot slot1 = deck.getSlot(1);
        assertEquals(DeckSlotState.READY, slot1.getState(deck.getCapacity()));

        deck.startCooldown(1, 3);
        assertEquals(DeckSlotState.COOLDOWN, slot1.getState(deck.getCapacity()));
        assertFalse(slot1.canCast(deck.getCapacity()));
        assertEquals(1.0f, slot1.getCooldownProgress(), 0.001f);

        deck.tick(); // tick 1: 2 remaining
        assertEquals(2, slot1.getCooldownRemainingTicks());
        assertEquals(DeckSlotState.COOLDOWN, slot1.getState(deck.getCapacity()));

        deck.tick(); // tick 2: 1 remaining
        assertEquals(1, slot1.getCooldownRemainingTicks());
        assertEquals(DeckSlotState.COOLDOWN, slot1.getState(deck.getCapacity()));

        deck.tick(); // tick 3: 0 remaining
        assertEquals(0, slot1.getCooldownRemainingTicks());
        assertEquals(DeckSlotState.READY, slot1.getState(deck.getCapacity()));
        assertTrue(slot1.canCast(deck.getCapacity()));
    }

    @Test
    @DisplayName("Windup channeling ticks decrement until spell channeling completes")
    void testWindupChannelingTicking() {
        PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);
        deck.equipCard(2, "mtg:wrath_of_god", 100, 2);

        DeckCardSlot slot2 = deck.getSlot(2);
        deck.startWindup(2, 2);

        assertEquals(DeckSlotState.WINDUP, slot2.getState(deck.getCapacity()));
        assertFalse(slot2.canCast(deck.getCapacity()));
        assertEquals(1.0f, slot2.getWindupProgress(), 0.001f);

        deck.tick(); // 1 remaining
        assertEquals(1, slot2.getWindupRemainingTicks());
        assertEquals(DeckSlotState.WINDUP, slot2.getState(deck.getCapacity()));

        deck.tick(); // 0 remaining
        assertEquals(0, slot2.getWindupRemainingTicks());
        assertEquals(DeckSlotState.READY, slot2.getState(deck.getCapacity()));
    }

    @Test
    @DisplayName("JSON serialization round-trip restores deck capacity, selection, and slot state")
    void testJsonSerializationRoundTrip() {
        UUID playerId = UUID.randomUUID();
        PlaneswalkerDeck deck = new PlaneswalkerDeck(playerId, 5);
        deck.setSelectedSlot(2);
        deck.equipCard(0, "mtg:llanowar_elves", 40, 0);
        deck.equipCard(1, "mtg:giant_growth", 30, 10);
        deck.startCooldown(0, 15);
        deck.startWindup(1, 5);

        String json = deck.toJson();
        assertNotNull(json);

        PlaneswalkerDeck restored = PlaneswalkerDeck.fromJson(json);
        assertEquals(playerId, restored.getPlayerId());
        assertEquals(5, restored.getCapacity());
        assertEquals(2, restored.getSelectedSlot());

        assertEquals("mtg:llanowar_elves", restored.getSlot(0).getCardId());
        assertEquals(15, restored.getSlot(0).getCooldownRemainingTicks());
        assertEquals(40, restored.getSlot(0).getMaxCooldownTicks());

        assertEquals("mtg:giant_growth", restored.getSlot(1).getCardId());
        assertEquals(5, restored.getSlot(1).getWindupRemainingTicks());
        assertEquals(10, restored.getSlot(1).getMaxWindupTicks());
    }

    @Test
    @DisplayName("Capacity clamping enforces minimum 1 and maximum 7 slots")
    void testSetCapacityClamping() {
        PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);

        deck.setCapacity(0);
        assertEquals(1, deck.getCapacity());

        deck.setCapacity(10);
        assertEquals(7, deck.getCapacity());

        deck.setCapacity(5);
        assertEquals(5, deck.getCapacity());
    }
}
