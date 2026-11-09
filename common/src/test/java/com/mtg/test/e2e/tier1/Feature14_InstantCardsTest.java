package com.mtg.test.e2e.tier1;

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

@DisplayName("Tier 1 - Feature 14: Instant Cards (Fast Reflex, 0s Cast Delay) E2E Test")
public class Feature14_InstantCardsTest {

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
    @DisplayName("1. Lightning Bolt instant resolves immediately with SUCCESS_INSTANT")
    void testLightningBoltInstantResolution() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        manaPool.addMana(ManaColor.RED, 1);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertTrue(result.isSuccess(), "Instant cast must succeed");
        assertEquals(CastResult.Status.SUCCESS_INSTANT, result.status(), "Instant spell must resolve with 0 delay");
    }

    @Test
    @DisplayName("2. Instant spell incurs fast reflex cooldown of 60 ticks (3s)")
    void testInstantReflexCooldown() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        manaPool.addMana(ManaColor.RED, 1);

        castingService.initiateCast(playerId, 0, CastTarget.self());
        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(60, slot.getCooldownRemainingTicks(), "Reflex cooldown must be 60 ticks");
    }

    @Test
    @DisplayName("3. Upfront mana deduction strictly consumes colored mana cost")
    void testInstantManaDeduction() {
        deck.equipCard(0, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), 60, 0);
        manaPool.addMana(ManaColor.RED, 3);

        castingService.initiateCast(playerId, 0, CastTarget.self());
        assertEquals(2, manaPool.get(ManaColor.RED), "Should consume exactly 1 Red mana, leaving 2");
    }

    @Test
    @DisplayName("4. Giant Growth instant consumes {G} and resolves cleanly")
    void testGiantGrowthResolution() {
        deck.equipCard(1, E2ETestExpansionHelper.TEST_GIANT_GROWTH.id(), 60, 0);
        manaPool.addMana(ManaColor.GREEN, 2);

        CastResult result = castingService.initiateCast(playerId, 1, CastTarget.self());
        assertTrue(result.isSuccess());
        assertEquals(1, manaPool.get(ManaColor.GREEN), "Should consume 1 Green mana");
    }

    @Test
    @DisplayName("5. Counterspell instant consumes {U}{U} and resolves cleanly")
    void testCounterspellResolution() {
        deck.equipCard(2, E2ETestExpansionHelper.TEST_COUNTERSPELL.id(), 60, 0);
        manaPool.addMana(ManaColor.BLUE, 2);

        CastResult result = castingService.initiateCast(playerId, 2, CastTarget.self());
        assertTrue(result.isSuccess());
        assertEquals(0, manaPool.get(ManaColor.BLUE), "Should consume 2 Blue mana");
    }
}
