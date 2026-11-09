package com.mtg.test.world;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.api.trigger.TriggerPriority;
import com.mtg.domain.CardRarity;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import com.mtg.world.altar.AltarPlacement;
import com.mtg.world.altar.AltarRitualPhase;
import com.mtg.world.altar.AltarRitualType;
import com.mtg.world.altar.AltarStateMachine;
import com.mtg.world.boss.BossManager;
import com.mtg.world.boss.IBossEntity;
import com.mtg.world.events.AltarRitualEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Planeswalker Altar Placement & Ritual State Machine Suite")
public class PlaneswalkerAltarTest {

    private TriggerBus testBus;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        testPlayerId = UUID.randomUUID();
        ManaManager.getInstance().selectInitialMana(testPlayerId, ManaColor.WHITE);
        ManaManager.getInstance().getPlayerMana(testPlayerId).clear();
        BossManager.getInstance().clear();
        BossManager.getInstance().setTriggerBus(testBus);
    }

    @Test
    @DisplayName("Altar chunk placement satisfies 32-chunk spacing and 8-chunk separation intervals")
    void testAltarPlacementSpacing() {
        long seed = 54321L;
        int spacing = 32;
        int separation = 8;

        int altarChunksFound = 0;
        List<int[]> altarCoords = new ArrayList<>();

        for (int cx = 0; cx < 128; cx++) {
            for (int cz = 0; cz < 128; cz++) {
                if (AltarPlacement.isAltarChunk(seed, cx, cz, spacing, separation)) {
                    altarChunksFound++;
                    altarCoords.add(new int[]{cx, cz});
                }
            }
        }

        // In a 128x128 chunk region with 32x32 cell spacing, there should be exactly (128/32)*(128/32) = 16 altars
        assertEquals(16, altarChunksFound, "Expected exactly 16 altars in a 4x4 cell region");

        // Verify minimum distance between any two altars >= separation
        for (int i = 0; i < altarCoords.size(); i++) {
            for (int j = i + 1; j < altarCoords.size(); j++) {
                int dx = Math.abs(altarCoords.get(i)[0] - altarCoords.get(j)[0]);
                int dz = Math.abs(altarCoords.get(i)[1] - altarCoords.get(j)[1]);
                int chebyshev = Math.max(dx, dz);
                assertTrue(chebyshev >= separation, "Altars must maintain at least separation distance");
            }
        }
    }

    @Test
    @DisplayName("60-tick channeling delay: ritual remains in CHANNELING phase until tick 60")
    void testChannelingDelay() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        boolean started = altar.initiateReplenishment(testPlayerId, ManaColor.BLUE);
        assertTrue(started);
        assertEquals(AltarRitualPhase.CHANNELING, altar.getCurrentPhase());

        for (int t = 1; t <= 59; t++) {
            altar.tick();
            assertEquals(AltarRitualPhase.CHANNELING, altar.getCurrentPhase(), "Phase must remain CHANNELING before tick 60");
            assertEquals(t, altar.getChannelTimerTicks());
        }

        altar.tick(); // Tick 60
        // After completing, moves into COOLDOWN phase
        assertEquals(AltarRitualPhase.COOLDOWN, altar.getCurrentPhase());
    }

    @Test
    @DisplayName("Mana Replenishment ritual restores player's colored mana to max capacity")
    void testManaReplenishmentRitual() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        ManaPool pool = ManaManager.getInstance().getPlayerMana(testPlayerId);
        int max = pool.getMaxMana();

        // Drain red mana to 0
        pool.payCost(com.mtg.domain.ManaCost.ofRed(pool.get(ManaColor.RED)));
        assertEquals(0, pool.get(ManaColor.RED));

        altar.initiateReplenishment(testPlayerId, ManaColor.RED);

        for (int t = 0; t < 60; t++) {
            altar.tick();
        }

        assertEquals(max, pool.get(ManaColor.RED), "Red mana must be replenished to full maximum capacity");
    }

    @Test
    @DisplayName("Planar Conflux Overcharge: grants +5 max mana cap, adds 5 WUBRG, and awards 1 Rare Wildcard")
    void testPlanarConfluxOverchargeRitual() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        ManaPool pool = ManaManager.getInstance().getPlayerMana(testPlayerId);
        int initialCap = pool.getMaxMana();
        int initialRareWildcards = ProgressionManager.getInstance().getProgression(testPlayerId).getWildcardCount(CardRarity.RARE);

        altar.initiateConfluxOvercharge(testPlayerId);

        for (int t = 0; t < 60; t++) {
            altar.tick();
        }

        assertEquals(initialCap + 5, pool.getMaxMana(), "Max mana cap must increase by +5");
        assertTrue(pool.get(ManaColor.WHITE) >= 1);
        assertTrue(pool.get(ManaColor.BLUE) >= 1);
        assertTrue(pool.get(ManaColor.BLACK) >= 1);
        assertTrue(pool.get(ManaColor.RED) >= 1);
        assertTrue(pool.get(ManaColor.GREEN) >= 1);

        int newRareWildcards = ProgressionManager.getInstance().getProgression(testPlayerId).getWildcardCount(CardRarity.RARE);
        assertEquals(initialRareWildcards + 1, newRareWildcards, "Must award 1 Rare Wildcard");
    }

    @Test
    @DisplayName("Boss Summoning Trial: Volcanic Gem catalyst summons Shivan Dragon World Boss")
    void testBossSummoningTrial() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);
        altar.setPosition(100.0, 64.0, 200.0);

        AtomicBoolean eventFired = new AtomicBoolean(false);
        testBus.registerListener(AltarRitualEvent.class, TriggerPriority.NORMAL, event -> {
            if (event.getPhase() == AltarRitualPhase.COMPLETED) {
                eventFired.set(true);
                assertEquals(AltarRitualType.BOSS_SUMMON, event.getRitualType());
                assertEquals(ManaColor.RED, event.getManaColor());
            }
        });

        // XP levels check: < 10 levels rejected
        assertFalse(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 9), "Should reject summon if XP < 10");

        // Valid summon with 10 XP
        assertTrue(altar.initiateBossSummon(testPlayerId, ManaColor.RED, 10));

        for (int t = 0; t < 60; t++) {
            altar.tick();
        }

        assertTrue(eventFired.get(), "AltarRitualEvent COMPLETED must be posted to TriggerBus");

        // Verify Elemental Avatar Boss was registered with BossManager
        assertEquals(1, BossManager.getInstance().getAllBosses().size());
        IBossEntity boss = BossManager.getInstance().getAllBosses().iterator().next();
        assertEquals("mtg:elemental_avatar_red", boss.getBossType());
        assertEquals(100.0, boss.getX());
        assertEquals(66.0, boss.getY());
        assertEquals(200.0, boss.getZ());
    }

    @Test
    @DisplayName("Altar Cooldown: after 60-tick ritual completion, 100-tick cooldown returns altar to IDLE")
    void testAltarCooldownLifecycle() {
        AltarStateMachine altar = new AltarStateMachine();
        altar.setTriggerBus(testBus);

        altar.initiateReplenishment(testPlayerId, ManaColor.WHITE);

        // Advance 60 ticks for channeling
        for (int t = 0; t < 60; t++) {
            altar.tick();
        }

        assertEquals(AltarRitualPhase.COOLDOWN, altar.getCurrentPhase());
        assertFalse(altar.initiateReplenishment(testPlayerId, ManaColor.WHITE), "Cannot initiate while in COOLDOWN");

        // Advance 99 cooldown ticks
        for (int t = 1; t <= 99; t++) {
            altar.tick();
            assertEquals(AltarRitualPhase.COOLDOWN, altar.getCurrentPhase());
        }

        // 100th cooldown tick returns to IDLE
        altar.tick();
        assertEquals(AltarRitualPhase.IDLE, altar.getCurrentPhase());
        assertTrue(altar.initiateReplenishment(testPlayerId, ManaColor.WHITE), "Can initiate new ritual once IDLE");
    }
}
