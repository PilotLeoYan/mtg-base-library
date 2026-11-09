package com.mtg.test.e2e.tier1;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.world.altar.AltarPlacement;
import com.mtg.world.altar.AltarRitualPhase;
import com.mtg.world.altar.AltarStateMachine;
import com.mtg.world.node.ManaNodeDefinition;
import com.mtg.world.node.ManaNodeState;
import com.mtg.world.node.ManaNodeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 19: World Content (Mana Nodes & Altars) E2E Test")
public class Feature19_ManaNodesAndAltarsTest {

    private UUID playerId;
    private TriggerBus testBus;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        testBus = new TriggerBus();
        ManaManager.getInstance().reset();
    }

    @AfterEach
    void tearDown() {
        ManaManager.getInstance().reset();
    }

    @Test
    @DisplayName("1. ManaNodeState encapsulates node definition, mana color affinity, and initial charges")
    void testManaNodeStateProperties() {
        ManaNodeDefinition def = ManaNodeDefinition.of(ManaNodeType.PLAINS_NODE);
        ManaNodeState node = new ManaNodeState(def, testBus);

        assertEquals(ManaNodeType.PLAINS_NODE, node.getDefinition().type());
        assertEquals(ManaColor.WHITE, node.getDefinition().color());
        assertEquals(def.maxCharges(), node.getCurrentCharges());
    }

    @Test
    @DisplayName("2. AltarPlacement.isAltarChunk provides deterministic chunk grid positioning")
    void testAltarPlacementChunkFormula() {
        long worldSeed = 123456789L;
        int spacing = 32;
        int separation = 8;

        int altarCountInRegion = 0;
        for (int cx = 0; cx < 64; cx++) {
            for (int cz = 0; cz < 64; cz++) {
                if (AltarPlacement.isAltarChunk(worldSeed, cx, cz, spacing, separation)) {
                    altarCountInRegion++;
                }
            }
        }
        // In a 64x64 chunk region with 32x32 cells, there are (64/32)*(64/32) = 4 cells -> 4 altars
        assertEquals(4, altarCountInRegion, "Should find exactly 4 altars in 2x2 cell region");
    }

    @Test
    @DisplayName("3. AltarStateMachine transitions to CHANNELING phase on ritual initiation")
    void testAltarRitualInitiation() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        boolean started = altar.initiateReplenishment(playerId, ManaColor.WHITE);
        assertTrue(started, "Ritual initiation should succeed");
        assertEquals(AltarRitualPhase.CHANNELING, altar.getCurrentPhase());
    }

    @Test
    @DisplayName("4. Altar channels for 60 ticks before concluding ritual and entering COOLDOWN")
    void testAltarChannelingCycle() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);
        altar.initiateReplenishment(playerId, ManaColor.WHITE);

        for (int tick = 1; tick <= 59; tick++) {
            altar.tick();
            assertEquals(AltarRitualPhase.CHANNELING, altar.getCurrentPhase(), "Phase must be CHANNELING until 60");
        }

        altar.tick(); // Tick 60
        assertEquals(AltarRitualPhase.COOLDOWN, altar.getCurrentPhase(), "Must enter COOLDOWN upon ritual completion");
    }

    @Test
    @DisplayName("5. Mana Replenishment ritual restores player mana to full maximum capacity")
    void testAltarReplenishmentRestoresMana() {
        ManaPool pool = ManaManager.getInstance().getOrCreatePlayerMana(playerId);
        pool.clear();
        pool.setMaxMana(20);
        assertEquals(0, pool.get(ManaColor.WHITE));

        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);
        altar.initiateReplenishment(playerId, ManaColor.WHITE);

        // Advance 60 ticks
        for (int i = 0; i < 60; i++) {
            altar.tick();
        }

        assertEquals(20, pool.get(ManaColor.WHITE), "White mana should be replenished to max cap (20)");
    }
}
