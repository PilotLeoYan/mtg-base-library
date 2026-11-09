package com.mtg.client.hud;

import com.mtg.config.ProgressionConfig;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckCapacityUpgradeCalculator;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Headless view model for the dedicated card hand HUD dock and radial selector menu.
 * Pure mathematical layout and visual state calculations with ZERO client-only imports
 * (net.minecraft.client.*), guaranteeing dedicated server safety and 100% testability in JUnit.
 */
public final class CardHudState {
    public static final int SLOT_WIDTH = 32;
    public static final int SLOT_HEIGHT = 44;
    public static final int SLOT_SPACING = 4;
    public static final int SELECTED_OFFSET_Y = 6;
    public static final int COLOR_GOLD_HIGHLIGHT = 0xFFFFD700;
    public static final int COLOR_STANDARD_BORDER = 0xFF555555;
    public static final int COLOR_LOCKED_BORDER = 0xFF222222;

    private CardHudState() {}

    public record CardSlotViewModel(
        int slotIndex,
        int x,
        int y,
        int width,
        int height,
        boolean isSelected,
        boolean isLocked,
        boolean isEmpty,
        String cardId,
        String cardName,
        String manaCostText,
        float cooldownProgress,
        String cooldownDisplaySeconds,
        float windupProgress,
        int frameColorRgb,
        String tooltipText
    ) {}

    public static List<CardSlotViewModel> computeLayout(PlaneswalkerDeck deck, int screenWidth, int screenHeight, ProgressionConfig config) {
        if (deck == null) {
            return List.of();
        }

        int totalSlots = PlaneswalkerDeck.MAX_CAPACITY;
        int capacity = deck.getCapacity();
        int selectedIndex = deck.getSelectedSlot();

        int totalDockWidth = (totalSlots * SLOT_WIDTH) + ((totalSlots - 1) * SLOT_SPACING);
        int startX = (screenWidth - totalDockWidth) / 2;
        int baseY = screenHeight - SLOT_HEIGHT - 12;

        List<CardSlotViewModel> viewModels = new ArrayList<>(totalSlots);

        for (int i = 0; i < totalSlots; i++) {
            DeckCardSlot slot = deck.getSlot(i);
            boolean isLocked = i >= capacity;
            boolean isSelected = (!isLocked && i == selectedIndex);
            int x = startX + (i * (SLOT_WIDTH + SLOT_SPACING));
            int y = isSelected ? baseY - SELECTED_OFFSET_Y : baseY;

            boolean isEmpty = isLocked || !slot.hasCard();
            String cardId = slot.getCardId();
            String cardName = cardId != null ? formatCardName(cardId) : "";
            float cdProgress = slot.getCooldownProgress();
            String cdSeconds = "";
            if (slot.getCooldownRemainingTicks() > 0) {
                float secs = slot.getCooldownRemainingTicks() / 20.0f;
                cdSeconds = String.format(Locale.ROOT, "%.1fs", secs);
            }

            float wuProgress = slot.getWindupProgress();
            int frameColor = isSelected ? COLOR_GOLD_HIGHLIGHT : (isLocked ? COLOR_LOCKED_BORDER : COLOR_STANDARD_BORDER);

            String tooltip;
            if (isLocked) {
                int unlockCost = DeckCapacityUpgradeCalculator.calculateUpgradeCost(capacity, config);
                tooltip = unlockCost > 0 ? "Cost: " + unlockCost + " XP Levels" : "Locked";
            } else if (!slot.hasCard()) {
                tooltip = "Empty Slot " + (i + 1);
            } else if (slot.getState(capacity) == DeckSlotState.COOLDOWN) {
                tooltip = cardName + " (Cooldown: " + cdSeconds + ")";
            } else if (slot.getState(capacity) == DeckSlotState.WINDUP) {
                tooltip = cardName + " (Channeling...)";
            } else {
                tooltip = cardName;
            }

            viewModels.add(new CardSlotViewModel(
                i, x, y, SLOT_WIDTH, SLOT_HEIGHT,
                isSelected, isLocked, isEmpty,
                cardId, cardName, "",
                cdProgress, cdSeconds, wuProgress,
                frameColor, tooltip
            ));
        }

        return List.copyOf(viewModels);
    }

    public static int calculateHoveredRadialSlot(int mouseX, int mouseY, int centerX, int centerY, int totalSlots) {
        if (totalSlots <= 0) return -1;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;
        if (distSq < 100.0) { // Dead-zone radius 10px
            return -1;
        }

        double angleRad = Math.atan2(dy, dx); // [-PI, PI], 0 is East (+X)
        double angleDeg = Math.toDegrees(angleRad) + 90.0; // 0 is North (-Y)
        if (angleDeg < 0.0) {
            angleDeg += 360.0;
        }

        double sectorAngle = 360.0 / totalSlots;
        int slot = (int) (angleDeg / sectorAngle);
        return Math.clamp(slot, 0, totalSlots - 1);
    }

    private static String formatCardName(String cardId) {
        int colon = cardId.indexOf(':');
        String namePart = colon != -1 ? cardId.substring(colon + 1) : cardId;
        String[] tokens = namePart.split("[_/]");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(token.charAt(0)));
                if (token.length() > 1) {
                    sb.append(token.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return sb.toString();
    }
}
