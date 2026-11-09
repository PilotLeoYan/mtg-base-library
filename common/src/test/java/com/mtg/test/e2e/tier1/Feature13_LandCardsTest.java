package com.mtg.test.e2e.tier1;

import com.mtg.api.trigger.LandPlayedEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.casting.CardCastingService;
import com.mtg.cards.casting.CastResult;
import com.mtg.cards.casting.CastTarget;
import com.mtg.cards.catalog.BasicLandsCatalog;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 13: Land Cards (Mana Boost, Surge, LandPlayedEvent, Cooldown) E2E Test")
public class Feature13_LandCardsTest {

    private UUID playerId;
    private CardCastingService castingService;
    private TriggerBus testBus;
    private PlaneswalkerDeck deck;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        testBus = new TriggerBus();
        castingService = new CardCastingService(CardsConfig.defaults());
        castingService.setTriggerBus(testBus);
        CardCastingService.setInstance(castingService);

        BasicLandsCatalog.registerAllCards(ProgressionManager.getInstance()::registerCard);
        BasicLandsCatalog.registerAllEffects();

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
    @DisplayName("1. Casting Plains provides +1 White mana surge to player mana pool")
    void testPlainsLandManaSurge() {
        deck.equipCard(0, BasicLandsCatalog.PLAINS.id(), 1200, 0);
        int initialWhite = manaPool.get(ManaColor.WHITE);

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertTrue(result.isSuccess(), "Land casting should succeed");
        assertEquals(initialWhite + 1, manaPool.get(ManaColor.WHITE), "Should receive +1 White mana surge");
    }

    @Test
    @DisplayName("2. Casting Land card increases player maximum mana cap by +1")
    void testLandMaxManaCapBoost() {
        deck.equipCard(0, BasicLandsCatalog.FOREST.id(), 1200, 0);
        int initialCap = manaPool.getMaxMana();

        castingService.initiateCast(playerId, 0, CastTarget.self());
        assertEquals(initialCap + 1, manaPool.getMaxMana(), "Playing Land should boost max mana cap by +1");
    }

    @Test
    @DisplayName("3. Land cards resolve instantly with 0 windup delay ticks")
    void testLandZeroWindupDelay() {
        deck.equipCard(0, BasicLandsCatalog.ISLAND.id(), 1200, 0);
        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());

        assertEquals(CastResult.Status.SUCCESS_INSTANT, result.status(), "Land should resolve instantly");
    }

    @Test
    @DisplayName("4. Casting Land enforces 1200 ticks (60s) post-cast cooldown on slot")
    void testLandCooldownEnforcement() {
        deck.equipCard(0, BasicLandsCatalog.MOUNTAIN.id(), 1200, 0);
        castingService.initiateCast(playerId, 0, CastTarget.self());

        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(1200, slot.getCooldownRemainingTicks(), "Land cooldown should be 1200 ticks");
    }

    @Test
    @DisplayName("5. Casting Land dispatches LandPlayedEvent on TriggerBus")
    void testLandfallEventDispatched() {
        deck.equipCard(0, BasicLandsCatalog.SWAMP.id(), 1200, 0);
        AtomicBoolean landPlayedFired = new AtomicBoolean(false);

        testBus.registerListener(LandPlayedEvent.class, e -> landPlayedFired.set(true));

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertTrue(result.isSuccess(), "Land casting should succeed");
        assertTrue(landPlayedFired.get(), "LandPlayedEvent must fire on TriggerBus");
    }
}
