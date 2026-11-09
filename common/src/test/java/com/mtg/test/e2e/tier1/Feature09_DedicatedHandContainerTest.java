package com.mtg.test.e2e.tier1;

import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckManager;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 09: Dedicated Card Hand Data Container E2E Test")
public class Feature09_DedicatedHandContainerTest {

    private UUID playerId;
    private DeckManager deckManager;
    private PlaneswalkerDeck deck;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        deckManager = DeckManager.getInstance();
        deckManager.clear();
        deck = deckManager.getDeck(playerId);
    }

    @AfterEach
    void tearDown() {
        deckManager.clear();
    }

    @Test
    @DisplayName("1. Dedicated hand starts with 4 active unlocked slots and 7 total slot capacity")
    void testInitialHandSlots() {
        assertEquals(4, deck.getCapacity(), "Initial capacity must be 4 slots");
        assertEquals(PlaneswalkerDeck.MAX_CAPACITY, deck.getAllSlots().size(), "Max possible hand slots must be 7");

        for (int i = 0; i < 4; i++) {
            assertEquals(DeckSlotState.EMPTY, deck.getSlot(i).getState(deck.getCapacity()),
                    "Slots 0-3 must be active and initially EMPTY");
        }
    }

    @Test
    @DisplayName("2. Equipping card into empty active slot transitions state to READY")
    void testEquipCardToEmptySlot() {
        String cardId = E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id();
        boolean equipped = deck.equipCard(0, cardId, 60, 0);

        assertTrue(equipped, "Equipping card in slot 0 should succeed");
        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.READY, slot.getState(deck.getCapacity()));
        assertEquals(cardId, slot.getCardId());
    }

    @Test
    @DisplayName("3. Unequipping active slot clears card and resets state to EMPTY")
    void testUnequipCardFromSlot() {
        String cardId = E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id();
        deck.equipCard(1, cardId, 60, 0);
        assertEquals(DeckSlotState.READY, deck.getSlot(1).getState(deck.getCapacity()));

        deck.unequipCard(1);
        assertEquals(DeckSlotState.EMPTY, deck.getSlot(1).getState(deck.getCapacity()));
        assertNull(deck.getSlot(1).getCardId());
    }

    @Test
    @DisplayName("4. Slots above current capacity are in LOCKED state and reject equipping")
    void testLockedSlotsRejectEquip() {
        DeckCardSlot slot4 = deck.getSlot(4);
        assertEquals(DeckSlotState.LOCKED, slot4.getState(deck.getCapacity()), "Slot 4 must be LOCKED at 4 capacity");

        assertThrows(IllegalStateException.class, () ->
                deck.equipCard(4, E2ETestExpansionHelper.TEST_GIANT_GROWTH.id(), 60, 0),
                "Cannot equip into a LOCKED slot");
    }

    @Test
    @DisplayName("5. Dedicated hand retrieves equipped card IDs across slots reliably")
    void testHandCardLookup() {
        deck.equipCard(0, BasicLandsCatalog.PLAINS.id(), 1200, 0);
        deck.equipCard(2, E2ETestExpansionHelper.TEST_SERRA_ANGEL.id(), 100, 0);

        assertEquals(BasicLandsCatalog.PLAINS.id(), deck.getSlot(0).getCardId());
        assertNull(deck.getSlot(1).getCardId(), "Slot 1 remains empty");
        assertEquals(E2ETestExpansionHelper.TEST_SERRA_ANGEL.id(), deck.getSlot(2).getCardId());
    }
}
