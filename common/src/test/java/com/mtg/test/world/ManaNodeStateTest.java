package com.mtg.test.world;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.api.trigger.TriggerPriority;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.world.events.ManaNodeAttunedEvent;
import com.mtg.world.node.ManaNodeDefinition;
import com.mtg.world.node.ManaNodeState;
import com.mtg.world.node.ManaNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Elemental Mana Node State Machine and Recharge Suite")
public class ManaNodeStateTest {

    private TriggerBus testBus;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        testPlayerId = UUID.randomUUID();
        ManaManager.getInstance().selectInitialMana(testPlayerId, ManaColor.WHITE);
        ManaManager.getInstance().getPlayerMana(testPlayerId).clear();
    }

    @Test
    @DisplayName("Initial state holds exactly 5 charges and zero recharge timer ticks")
    void testInitialCharges() {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.MOUNTAIN_NODE);
        ManaNodeState state = new ManaNodeState(def, testBus);

        assertEquals(5, state.getCurrentCharges());
        assertEquals(0, state.getRechargeTimerTicks());
        assertEquals(ManaNodeType.MOUNTAIN_NODE, state.getDefinition().type());
        assertEquals(ManaColor.RED, state.getDefinition().color());
    }

    @Test
    @DisplayName("Attuning node decrements charge, pulses mana, and fires ManaNodeAttunedEvent")
    void testAttunementSuccess() {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.MOUNTAIN_NODE, 3);
        ManaNodeState state = new ManaNodeState(def, testBus);

        AtomicBoolean eventFired = new AtomicBoolean(false);
        AtomicInteger pulsedAmount = new AtomicInteger(0);

        testBus.registerListener(ManaNodeAttunedEvent.class, TriggerPriority.NORMAL, event -> {
            eventFired.set(true);
            pulsedAmount.set(event.getAmount());
            assertEquals(testPlayerId, event.getPlayerId());
            assertEquals(ManaColor.RED, event.getManaColor());
            assertEquals(ManaNodeType.MOUNTAIN_NODE, event.getNodeType());
        });

        int manaBefore = ManaManager.getInstance().getPlayerMana(testPlayerId).get(ManaColor.RED);
        boolean success = state.attune(testPlayerId);

        assertTrue(success, "Attunement should succeed when charges are available");
        assertEquals(4, state.getCurrentCharges(), "Charge count must decrease by 1");
        assertTrue(eventFired.get(), "ManaNodeAttunedEvent must be posted to TriggerBus");
        assertEquals(3, pulsedAmount.get(), "Pulsed amount must equal definition amount (3)");

        int manaAfter = ManaManager.getInstance().getPlayerMana(testPlayerId).get(ManaColor.RED);
        assertEquals(manaBefore + 3, manaAfter, "Player mana pool must increase by pulsed amount");
    }

    @Test
    @DisplayName("Node depletion: after 5 attunements, 6th attunement fails with 0 charges")
    void testDepletion() {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.FOREST_NODE);
        ManaNodeState state = new ManaNodeState(def, testBus);

        for (int i = 0; i < 5; i++) {
            assertTrue(state.attune(testPlayerId), "Attunement " + (i + 1) + " must succeed");
        }

        assertEquals(0, state.getCurrentCharges());
        assertFalse(state.attune(testPlayerId), "Attunement must fail when node is depleted");
    }

    @Test
    @DisplayName("200-tick recharge cycle: recharges 1 charge after 200 ticks")
    void testRechargeCycle() {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.ISLAND_NODE);
        ManaNodeState state = new ManaNodeState(def, testBus);

        state.setCurrentCharges(2);
        assertEquals(2, state.getCurrentCharges());

        for (int t = 1; t <= 199; t++) {
            state.tick();
            assertEquals(2, state.getCurrentCharges(), "Must not recharge before 200 ticks");
            assertEquals(t, state.getRechargeTimerTicks());
        }

        state.tick(); // 200th tick
        assertEquals(3, state.getCurrentCharges(), "Must recharge 1 charge at tick 200");
        assertEquals(0, state.getRechargeTimerTicks(), "Timer must reset after recharge");
    }

    @Test
    @DisplayName("Full charge cap: ticking at max charges does not exceed capacity or increment timer")
    void testFullCapTicking() {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.SWAMP_NODE);
        ManaNodeState state = new ManaNodeState(def, testBus);

        assertEquals(5, state.getCurrentCharges());

        for (int t = 0; t < 500; t++) {
            state.tick();
        }

        assertEquals(5, state.getCurrentCharges(), "Charges must never exceed max cap (5)");
        assertEquals(0, state.getRechargeTimerTicks());
    }

    @Test
    @DisplayName("Gem catalyst harvesting returns correct Mana Gem item key")
    void testGemHarvesting() {
        ManaNodeState mountainState = new ManaNodeState(ManaNodeDefinition.of(ManaNodeType.MOUNTAIN_NODE));
        assertEquals("item.mtg.mana_gem_red", mountainState.harvestGem());

        ManaNodeState plainsState = new ManaNodeState(ManaNodeDefinition.of(ManaNodeType.PLAINS_NODE));
        assertEquals("item.mtg.mana_gem_white", plainsState.harvestGem());

        ManaNodeState wastesState = new ManaNodeState(ManaNodeDefinition.of(ManaNodeType.WASTES_NODE));
        assertEquals("item.mtg.mana_gem_colorless", wastesState.harvestGem());
    }
}
