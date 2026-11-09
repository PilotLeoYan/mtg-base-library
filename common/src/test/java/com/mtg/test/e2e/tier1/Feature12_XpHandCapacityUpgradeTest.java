package com.mtg.test.e2e.tier1;

import com.mtg.config.ProgressionConfig;
import com.mtg.deck.DeckCapacityUpgradeCalculator;
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

@DisplayName("Tier 1 - Feature 12: XP Hand Capacity Upgrades E2E Test")
public class Feature12_XpHandCapacityUpgradeTest {

    private UUID playerId;
    private DeckManager deckManager;
    private PlaneswalkerDeck deck;
    private ProgressionConfig config;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        deckManager = DeckManager.getInstance();
        deckManager.clear();
        deck = deckManager.getDeck(playerId);
        config = ProgressionConfig.defaults();
    }

    @AfterEach
    void tearDown() {
        deckManager.clear();
    }

    @Test
    @DisplayName("1. Dedicated hand starts with 4 base capacity slots")
    void testInitialHandCapacityIsFour() {
        assertEquals(4, deck.getCapacity());
        assertEquals(DeckSlotState.LOCKED, deck.getSlot(4).getState(deck.getCapacity()));
    }

    @Test
    @DisplayName("2. Cost calculation for expanding capacity from 4 to 5 slots")
    void testFirstCapacityUpgradeCost() {
        int cost = DeckCapacityUpgradeCalculator.calculateUpgradeCost(4, config);
        assertEquals(config.handUpgradeBaseXpCost(), cost, "Step 0 cost should equal base XP cost (10)");
        assertTrue(DeckCapacityUpgradeCalculator.canUpgrade(4, 15, config));
    }

    @Test
    @DisplayName("3. Upgrading hand capacity expands capacity from 4 to 5 and unlocks slot 4")
    void testUnlockSlotFive() {
        boolean upgraded = deckManager.upgradeHandCapacity(playerId, 15);
        assertTrue(upgraded, "Upgrade should succeed with sufficient XP");
        assertEquals(5, deck.getCapacity(), "Capacity should expand to 5");

        DeckCardSlot slot4 = deck.getSlot(4);
        assertEquals(DeckSlotState.EMPTY, slot4.getState(deck.getCapacity()), "Slot 4 must now be unlocked and EMPTY");
    }

    @Test
    @DisplayName("4. Newly unlocked slot 4 successfully equips a card")
    void testEquipCardIntoUnlockedSlotFour() {
        deckManager.upgradeHandCapacity(playerId, 15);
        boolean equipped = deck.equipCard(4, E2ETestExpansionHelper.TEST_SERRA_ANGEL.id(), 100, 0);

        assertTrue(equipped, "Should now be able to equip into slot 4");
        assertEquals(DeckSlotState.READY, deck.getSlot(4).getState(deck.getCapacity()));
        assertEquals(E2ETestExpansionHelper.TEST_SERRA_ANGEL.id(), deck.getSlot(4).getCardId());
    }

    @Test
    @DisplayName("5. Maximum capacity reaches 7 slots and rejects further expansion with -1")
    void testMaxCapacitySevenBoundary() {
        deck.setCapacity(7);
        assertEquals(7, deck.getCapacity());

        int cost = DeckCapacityUpgradeCalculator.calculateUpgradeCost(7, config);
        assertEquals(-1, cost, "Hard cap 7 slots must return -1 cost");
        assertFalse(deckManager.upgradeHandCapacity(playerId, 100), "Cannot upgrade beyond 7 slots");
    }
}
