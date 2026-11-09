package com.mtg.test.cards;

import com.mtg.api.trigger.*;
import com.mtg.cards.casting.*;
import com.mtg.cards.effects.EnchantmentManager;
import com.mtg.config.CardsConfig;
import com.mtg.deck.DeckCardSlot;
import com.mtg.deck.DeckManager;
import com.mtg.deck.DeckSlotState;
import com.mtg.deck.PlaneswalkerDeck;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import com.mtg.test.fixtures.TestCardFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Card Casting Engine & Delays/Cooldowns Test Suite")
public class CardCastingEngineTest {

    private CardCastingService castingService;
    private TriggerBus testBus;
    private UUID playerId;
    private PlaneswalkerDeck deck;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        castingService = new CardCastingService(CardsConfig.defaults());
        castingService.setTriggerBus(testBus);
        CardCastingService.setInstance(castingService);
        EnchantmentManager.getInstance().setTriggerBus(testBus);

        // Register base cards
        TestCardFixtures.registerAllCards(ProgressionManager.getInstance()::registerCard);
        TestCardFixtures.registerAllEffects();

        playerId = UUID.randomUUID();
        deck = DeckManager.getInstance().getDeck(playerId);
        // Clear slots
        for (int i = 0; i < deck.getCapacity(); i++) {
            deck.unequipCard(i);
            deck.getSlot(i).resetDelays();
        }

        manaPool = ManaManager.getInstance().getOrCreatePlayerMana(playerId);
        manaPool.clear();
        manaPool.setMaxMana(20);
    }

    @Test
    @DisplayName("1. Land Casting: 0 delay, 1200t (60s) cooldown, mana surge, max mana cap boost, fires LandPlayedEvent")
    void testLandCasting() {
        // Equip basic Plains in slot 0
        deck.equipCard(0, TestCardFixtures.PLAINS.id(), 1200, 0);

        AtomicBoolean landPlayedFired = new AtomicBoolean(false);

        testBus.registerListener(LandPlayedEvent.class, event -> landPlayedFired.set(true));

        int initialWhite = manaPool.get(ManaColor.WHITE);
        int initialMax = manaPool.getMaxMana();

        CastResult result = castingService.initiateCast(playerId, 0, CastTarget.position(10.0, 64.0, 20.0));

        assertTrue(result.isSuccess(), "Land casting should succeed");
        assertEquals(CastResult.Status.SUCCESS_INSTANT, result.status(), "Land cast should resolve instantly (0 delay)");

        // Verify slot transitioned to COOLDOWN (1200 ticks = 60s)
        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(1200, slot.getCooldownRemainingTicks(), "Land cooldown should be 1200 ticks");

        // Verify mana surge (+1 White) and max mana cap boost (+1)
        assertEquals(initialWhite + 1, manaPool.get(ManaColor.WHITE), "Plains should add +1 White mana surge");
        assertEquals(initialMax + 1, manaPool.getMaxMana(), "Plains should boost max mana cap by 1");

        // Verify events fired
        assertTrue(landPlayedFired.get(), "LandPlayedEvent must be posted on TriggerBus");
    }

    @Test
    @DisplayName("2. Instant Casting: 0 delay, 60t (3s) reflex cooldown, upfront mana deduction, fires CastSpellEvent")
    void testInstantCasting() {
        deck.equipCard(1, TestCardFixtures.LIGHTNING_BOLT.id(), 60, 0);
        manaPool.addMana(ManaColor.RED, 2);

        AtomicBoolean spellCastFired = new AtomicBoolean(false);
        testBus.registerListener(CastSpellEvent.class, event -> {
            if (event.getCard().id().equals(TestCardFixtures.LIGHTNING_BOLT.id())) {
                spellCastFired.set(true);
            }
        });

        CastResult result = castingService.initiateCast(playerId, 1, CastTarget.position(5.0, 64.0, 5.0));

        assertTrue(result.isSuccess());
        assertEquals(CastResult.Status.SUCCESS_INSTANT, result.status());

        // Verify Red mana deducted (from 2 to 1)
        assertEquals(1, manaPool.get(ManaColor.RED), "1 Red mana should be deducted");

        // Verify fast reflex cooldown (60 ticks = 3s base)
        DeckCardSlot slot = deck.getSlot(1);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(60, slot.getCooldownRemainingTicks());

        // Verify CastSpellEvent
        assertTrue(spellCastFired.get(), "CastSpellEvent must be posted");
    }

    @Test
    @DisplayName("3. Sorcery Channeling: Channel delay scaled by mana (10 ticks/mana point), windup phase, completion")
    void testSorceryChannelingAndCompletion() {
        // Wrath of God: {2}{W}{W} (CMC 4) -> 4 * 10 = 40 ticks windup delay
        deck.equipCard(2, TestCardFixtures.WRATH_OF_GOD.id(), 200, 40);
        manaPool.addMana(ManaColor.WHITE, 2);
        manaPool.addMana(ManaColor.COLORLESS, 2);

        AtomicBoolean spellFired = new AtomicBoolean(false);
        testBus.registerListener(CastSpellEvent.class, event -> {
            if (event.getCard().id().equals(TestCardFixtures.WRATH_OF_GOD.id())) {
                spellFired.set(true);
            }
        });

        CastResult result = castingService.initiateCast(playerId, 2, CastTarget.self());

        assertTrue(result.isSuccess());
        assertEquals(CastResult.Status.CHANNELING_STARTED, result.status(), "Sorcery cast should begin channeling");
        assertTrue(result.activeCast().isPresent());

        ActiveCast activeCast = result.activeCast().get();
        assertEquals(40, activeCast.getTotalWindupTicks(), "Windup ticks should be 40 ticks (4 mana * 10t)");
        assertEquals(40, activeCast.getRemainingWindupTicks());

        // Mana deducted upfront
        assertEquals(0, manaPool.get(ManaColor.WHITE));
        assertEquals(0, manaPool.get(ManaColor.COLORLESS));

        // Slot should be in WINDUP
        DeckCardSlot slot = deck.getSlot(2);
        assertEquals(DeckSlotState.WINDUP, slot.getState(deck.getCapacity()));
        assertFalse(spellFired.get(), "Spell effect should not execute during channeling");

        // Advance 20 ticks (50% progress)
        for (int i = 0; i < 20; i++) {
            deck.tick();
            castingService.tick();
        }
        assertEquals(20, activeCast.getRemainingWindupTicks());
        assertEquals(0.5f, activeCast.getProgressFraction(), 0.01f);
        assertEquals(DeckSlotState.WINDUP, slot.getState(deck.getCapacity()));
        assertFalse(spellFired.get());

        // Advance remaining 20 ticks -> completion
        for (int i = 0; i < 20; i++) {
            deck.tick();
            castingService.tick();
        }

        assertTrue(spellFired.get(), "Spell should execute upon completing channeling");
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(200, slot.getCooldownRemainingTicks(), "Slot should enter base cooldown (200 ticks)");
        assertTrue(castingService.getActiveCast(playerId).isEmpty(), "ActiveCast should be purged on completion");
    }

    @Test
    @DisplayName("4. Channeling Damage Interruption: Damage >= 4.0 aborts channeling, fires SpellCastInterruptedEvent")
    void testChannelingDamageInterruption() {
        deck.equipCard(2, TestCardFixtures.WRATH_OF_GOD.id(), 200, 40);
        manaPool.addMana(ManaColor.WHITE, 2);
        manaPool.addMana(ManaColor.COLORLESS, 2);

        CastResult result = castingService.initiateCast(playerId, 2, CastTarget.self());
        assertTrue(result.isSuccess());

        AtomicReference<SpellCastInterruptedEvent> interruptEvent = new AtomicReference<>();
        testBus.registerListener(SpellCastInterruptedEvent.class, interruptEvent::set);

        // Advance 10 ticks
        for (int i = 0; i < 10; i++) {
            deck.tick();
            castingService.tick();
        }

        // Damage < threshold (e.g. 2.0 damage < 4.0 threshold) -> does NOT interrupt
        boolean interruptedMinor = castingService.handlePlayerDamage(playerId, 2.0);
        assertFalse(interruptedMinor, "Minor damage (<4.0) should not interrupt channeling");
        assertTrue(castingService.getActiveCast(playerId).isPresent());
        assertNull(interruptEvent.get());

        // Damage >= threshold (e.g. 4.5 damage >= 4.0 threshold) -> INTERRUPTS
        boolean interruptedMajor = castingService.handlePlayerDamage(playerId, 4.5);
        assertTrue(interruptedMajor, "Major damage (>=4.0) must interrupt channeling");

        // Verify ActiveCast removed
        assertTrue(castingService.getActiveCast(playerId).isEmpty());
        assertNotNull(interruptEvent.get(), "SpellCastInterruptedEvent must be fired");
        assertEquals(playerId, interruptEvent.get().getCasterId());
        assertEquals(4.5, interruptEvent.get().getDamageAmount());

        // Slot windup cancelled, penalty cooldown (20 ticks) applied
        DeckCardSlot slot = deck.getSlot(2);
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(20, slot.getCooldownRemainingTicks(), "Slot should receive 20 ticks penalty cooldown");
    }

    @Test
    @DisplayName("5. Enchantment Persistence & Expiration: 1200 ticks duration, ticks down, fires EnchantmentExpiredEvent")
    void testEnchantmentPersistenceAndExpiration() {
        deck.equipCard(3, TestCardFixtures.GLORIOUS_ANTHEM.id(), 200, 20);
        manaPool.addMana(ManaColor.WHITE, 2);
        manaPool.addMana(ManaColor.COLORLESS, 1);

        AtomicBoolean expiredFired = new AtomicBoolean(false);
        testBus.registerListener(EnchantmentExpiredEvent.class, event -> {
            if (event.getCard().id().equals(TestCardFixtures.GLORIOUS_ANTHEM.id())) {
                expiredFired.set(true);
            }
        });

        CastResult result = castingService.initiateCast(playerId, 3, CastTarget.self());
        assertTrue(result.isSuccess());

        // Resolve channeling (20 ticks)
        for (int i = 0; i < 20; i++) {
            deck.tick();
            castingService.tick();
        }

        // Verify active enchantment registered in EnchantmentManager with 1200 ticks duration
        assertTrue(EnchantmentManager.getInstance().hasEnchantment(playerId, TestCardFixtures.GLORIOUS_ANTHEM.id()));
        assertEquals(1, EnchantmentManager.getInstance().getPlayerEnchantments(playerId).size());
        assertEquals(1200, EnchantmentManager.getInstance().getPlayerEnchantments(playerId).getFirst().getRemainingTicks());

        // Tick 600 ticks
        for (int i = 0; i < 600; i++) {
            EnchantmentManager.getInstance().tickAll();
        }
        assertEquals(600, EnchantmentManager.getInstance().getPlayerEnchantments(playerId).getFirst().getRemainingTicks());
        assertFalse(expiredFired.get());

        // Tick remaining 600 ticks -> expiration
        for (int i = 0; i < 600; i++) {
            EnchantmentManager.getInstance().tickAll();
        }

        assertTrue(expiredFired.get(), "EnchantmentExpiredEvent must be posted upon duration expiration");
        assertFalse(EnchantmentManager.getInstance().hasEnchantment(playerId, TestCardFixtures.GLORIOUS_ANTHEM.id()));
    }

    @Test
    @DisplayName("6. Mana Insufficiency Gate: Rejection with zero deduction and slot stays READY")
    void testManaInsufficiencyGate() {
        deck.equipCard(1, TestCardFixtures.LIGHTNING_BOLT.id(), 60, 0);
        manaPool.clear(); // Empty pool

        CastResult result = castingService.initiateCast(playerId, 1, CastTarget.self());

        assertFalse(result.isSuccess());
        assertEquals(CastResult.Status.INSUFFICIENT_MANA, result.status());

        // Verify slot state is still READY
        DeckCardSlot slot = deck.getSlot(1);
        assertEquals(DeckSlotState.READY, slot.getState(deck.getCapacity()));
        assertEquals(0, slot.getCooldownRemainingTicks());
    }

    @Test
    @DisplayName("7. Casting State Machine Transitions: EMPTY -> READY -> WINDUP -> COOLDOWN -> READY")
    void testCastingStateMachineTransitions() {
        DeckCardSlot slot = deck.getSlot(0);
        assertEquals(DeckSlotState.EMPTY, slot.getState(deck.getCapacity()));

        // Equip card
        deck.equipCard(0, TestCardFixtures.DIVINATION.id(), 100, 30);
        assertEquals(DeckSlotState.READY, slot.getState(deck.getCapacity()));

        // Initiate channeling
        manaPool.addMana(ManaColor.BLUE, 1);
        manaPool.addMana(ManaColor.COLORLESS, 2);
        CastResult cast = castingService.initiateCast(playerId, 0, CastTarget.self());
        assertTrue(cast.isSuccess());
        assertEquals(DeckSlotState.WINDUP, slot.getState(deck.getCapacity()));

        // Advance channeling to completion (30 ticks)
        for (int i = 0; i < 30; i++) {
            deck.tick();
            castingService.tick();
        }
        assertEquals(DeckSlotState.COOLDOWN, slot.getState(deck.getCapacity()));
        assertEquals(100, slot.getCooldownRemainingTicks());

        // Advance cooldown to 0 (100 ticks)
        for (int i = 0; i < 100; i++) {
            deck.tick();
        }
        assertEquals(DeckSlotState.READY, slot.getState(deck.getCapacity()), "Slot should return to READY once cooldown expires");
    }
}
