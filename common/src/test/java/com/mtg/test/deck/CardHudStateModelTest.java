package com.mtg.test.deck;

import com.mtg.client.hud.CardHudState;
import com.mtg.config.ProgressionConfig;
import com.mtg.deck.PlaneswalkerDeck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dedicated Card Hand — CardHudState Headless View Model Suite")
public class CardHudStateModelTest {

    @Test
    @DisplayName("CardHudState calculates slot layout, coordinates, offset, and tooltips headlessly")
    void testHudViewModelCalculations() {
        PlaneswalkerDeck deck = new PlaneswalkerDeck(UUID.randomUUID(), 4);
        deck.setSelectedSlot(1);
        deck.equipCard(0, "mtg:lightning_bolt", 60, 0);
        deck.equipCard(1, "mtg:counterspell", 50, 0);
        deck.startCooldown(0, 50); // 50 ticks = 2.5s

        List<CardHudState.CardSlotViewModel> viewModels = CardHudState.computeLayout(
                deck, 800, 600, ProgressionConfig.defaults()
        );

        assertEquals(7, viewModels.size());

        // Slot 0: not selected, cooldown active
        CardHudState.CardSlotViewModel vm0 = viewModels.get(0);
        assertFalse(vm0.isSelected());
        assertFalse(vm0.isLocked());
        assertFalse(vm0.isEmpty());
        assertEquals("mtg:lightning_bolt", vm0.cardId());
        assertEquals("Lightning Bolt", vm0.cardName());
        assertEquals("2.5s", vm0.cooldownDisplaySeconds());
        assertTrue(vm0.cooldownProgress() > 0.8f);
        assertTrue(vm0.tooltipText().contains("Cooldown: 2.5s"));

        // Slot 1: selected, offset raised by 6 px, gold highlight border
        CardHudState.CardSlotViewModel vm1 = viewModels.get(1);
        assertTrue(vm1.isSelected());
        assertEquals(CardHudState.COLOR_GOLD_HIGHLIGHT, vm1.frameColorRgb());
        assertEquals(vm0.y() - CardHudState.SELECTED_OFFSET_Y, vm1.y(),
                "Selected slot must be raised vertically by 6 px");

        // Slot 4: locked, displays unlock cost in tooltip
        CardHudState.CardSlotViewModel vm4 = viewModels.get(4);
        assertTrue(vm4.isLocked());
        assertEquals(CardHudState.COLOR_LOCKED_BORDER, vm4.frameColorRgb());
        assertTrue(vm4.tooltipText().contains("Cost: 10 XP Levels"),
                "Locked slot tooltip must display XP unlock cost");
    }

    @Test
    @DisplayName("calculateHoveredRadialSlot accurately maps mouse coordinates to radial menu sectors")
    void testRadialSlotAngleCalculation() {
        int cx = 200;
        int cy = 200;
        int totalSlots = 4; // 90 degrees per sector: 0 = North, 1 = East, 2 = South, 3 = West

        // Dead-zone check: mouse close to center
        assertEquals(-1, CardHudState.calculateHoveredRadialSlot(202, 203, cx, cy, totalSlots));

        // Directly North (dx = 0, dy = -50): angle = 0 deg -> slot 0
        assertEquals(0, CardHudState.calculateHoveredRadialSlot(200, 150, cx, cy, totalSlots));

        // Directly East (dx = 50, dy = 0): angle = 90 deg -> slot 1
        assertEquals(1, CardHudState.calculateHoveredRadialSlot(250, 200, cx, cy, totalSlots));

        // Directly South (dx = 0, dy = 50): angle = 180 deg -> slot 2
        assertEquals(2, CardHudState.calculateHoveredRadialSlot(200, 250, cx, cy, totalSlots));

        // Directly West (dx = -50, dy = 0): angle = 270 deg -> slot 3
        assertEquals(3, CardHudState.calculateHoveredRadialSlot(150, 200, cx, cy, totalSlots));
    }
}
