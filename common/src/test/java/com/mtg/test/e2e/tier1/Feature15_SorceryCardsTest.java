package com.mtg.test.e2e.tier1;

import com.mtg.cards.casting.ActiveCast;
import com.mtg.cards.casting.CardCastingService;
import com.mtg.cards.casting.CastResult;
import com.mtg.cards.casting.CastTarget;
import com.mtg.config.CardsConfig;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckManager;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 15: Sorcery Cards (Scaled Cast Delay Channeling) E2E Test")
public class Feature15_SorceryCardsTest {

    private UUID playerId;
    private CardCastingService castingService;
    private PlaneswalkerDeck deck;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        castingService = new CardCastingService(CardsConfig.defaults());
        CardCastingService.setInstance(castingService);

        E2ETestExpansionHelper.registerAllCards(ProgressionManager.getInstance()::registerCard);
        E2ETestExpansionHelper.registerAllEffects();

        deck = DeckManager.getInstance().getDeck(playerId);
        for (int i = 0; i < PlaneswalkerDeck.MAX_CAPACITY; i++) {
            deck.unequipCard(i);
            deck.getSlot(i).resetDelays();
        }

        manaPool = ManaManager.getInstance().getOrCreatePlayerMana(playerId);
        manaPool.clear();
        manaPool.setMaxMana(20);
    }

    @AfterEach
    void tearDown() {
        castingService.clear();
        DeckManager.getInstance().clear();
        ManaManager.getInstance().reset();
    }

    @Test
    @DisplayName("1. Casting 2-mana Sorcery (Pyroclasm) initiates channeling with CHANNELING_STARTED status")
    void testPyroclasmWindupDelayEnforcement() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_PYROCLASM.id(), 100, 20);
        manaPool.addMana(ManaColor.RED, 2);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertTrue(result.isSuccess());
        assertEquals(CastResult.Status.CHANNELING_STARTED, result.status(), "Sorcery must enter windup delay channeling");
        assertTrue(result.activeCast().isPresent());
        assertTrue(result.activeCast().get().getTotalWindupTicks() > 0, "Windup delay ticks must be > 0");
    }

    @Test
    @DisplayName("2. Dedicated hand slot enters WINDUP state during channeling")
    void testSlotStateDuringChanneling() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_PYROCLASM.id(), 100, 20);
        manaPool.addMana(ManaColor.RED, 2);

        castingService.initiateCast(playerId, 0, CastTarget.self());
        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.WINDUP, slot.getState(deck.getCapacity()), "Slot must be in WINDUP state");
    }

    @Test
    @DisplayName("3. Ticking casting service and deck advances windup progress")
    void testChannelingProgressAdvance() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_PYROCLASM.id(), 100, 20);
        manaPool.addMana(ManaColor.RED, 2);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        ActiveCast cast = result.activeCast().orElseThrow();
        int initialRemaining = cast.getRemainingWindupTicks();

        // Advance 5 ticks
        for (int i = 0; i < 5; i++) {
            deck.tick();
            castingService.tick();
        }

        assertEquals(initialRemaining - 5, cast.getRemainingWindupTicks(), "Remaining cast delay should decrease by 5");
    }

    @Test
    @DisplayName("4. Channeling completes after windup delay and resolves spell")
    void testChannelingCompletionAndSpellResolution() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_PYROCLASM.id(), 100, 20);
        manaPool.addMana(ManaColor.RED, 2);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        ActiveCast cast = result.activeCast().orElseThrow();
        int totalWindup = cast.getTotalWindupTicks();

        for (int i = 0; i < totalWindup; i++) {
            deck.tick();
            castingService.tick();
        }

        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(0, slot.getWindupRemainingTicks(), "Windup delay must reach 0");
        assertNotEquals(DeckSlotState.WINDUP, slot.getState(deck.getCapacity()), "Slot must leave WINDUP state");
    }

    @Test
    @DisplayName("5. Sorcery slot enters post-cast cooldown after channeling completes")
    void testSorceryPostCastCooldown() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_PYROCLASM.id(), 100, 20);
        manaPool.addMana(ManaColor.RED, 2);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        ActiveCast cast = result.activeCast().orElseThrow();

        for (int i = 0; i < cast.getTotalWindupTicks(); i++) {
            deck.tick();
            castingService.tick();
        }

        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()), "Slot must enter COOLDOWN");
        assertEquals(100, slot.getCooldownRemainingTicks(), "Post-cast cooldown must equal base cooldown ticks");
    }
}
