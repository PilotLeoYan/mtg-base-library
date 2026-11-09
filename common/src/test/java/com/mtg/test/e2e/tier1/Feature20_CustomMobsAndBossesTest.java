package com.mtg.test.e2e.tier1;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.world.boss.BossManager;
import com.mtg.world.boss.BossPhase;
import com.mtg.world.boss.BossThreatTable;
import com.mtg.world.boss.GenericElementalBoss;
import com.mtg.world.boss.IBossEntity;
import com.mtg.world.events.BossDefeatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 20: Custom Mobs & World Bosses E2E Test")
public class Feature20_CustomMobsAndBossesTest {

    private TriggerBus testBus;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        BossManager.getInstance().clear();
        BossManager.getInstance().setTriggerBus(testBus);
    }

    @Test
    @DisplayName("1. GenericElementalBoss initializes in Phase 1 with baseline stats and color affinity")
    void testGenericElementalBossInitialState() {
        GenericElementalBoss boss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_avatar_red", ManaColor.RED, 200.0f, 12.0f, testBus);
        assertEquals(ManaColor.RED, boss.getAffinityColor());
        assertEquals(BossPhase.PHASE_1_TACTICAL, boss.getPhase());
        assertEquals(200.0f, boss.getMaxHealth());
        assertEquals(12.0f, boss.getAttackDamage());
        assertTrue(boss.isAlive());
    }

    @Test
    @DisplayName("2. GenericElementalBoss enrage scales attack damage upon phase transitions")
    void testGenericElementalBossPhaseProgression() {
        GenericElementalBoss boss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_avatar_red", ManaColor.RED, 200.0f, 12.0f, testBus);

        // Transition to Phase 2: Enraged (health between 33% and 66%)
        boss.takeDamage(UUID.randomUUID(), 80.0f); // 120 / 200 = 60%
        assertEquals(BossPhase.PHASE_2_ENRAGED, boss.getPhase());
        assertEquals(12.0f * 1.25f, boss.getAttackDamage(), 0.01f, "Phase 2 should scale attack damage by 1.25x");

        // Transition to Phase 3: Desperation (health <= 33%)
        boss.takeDamage(UUID.randomUUID(), 80.0f); // 40 / 200 = 20%
        assertEquals(BossPhase.PHASE_3_DESPERATION, boss.getPhase());
        assertEquals(12.0f * 1.50f, boss.getAttackDamage(), 0.01f, "Phase 3 should scale attack damage by 1.50x");
    }

    @Test
    @DisplayName("3. BossManager creates boss for color and supports registering custom boss factories")
    void testBossManagerFactoryRegistration() {
        IBossEntity defaultBoss = BossManager.getInstance().createBossForColor(ManaColor.BLACK, testBus);
        assertNotNull(defaultBoss);
        assertEquals("mtg:elemental_avatar_black", defaultBoss.getBossType());

        // Register custom factory for Green boss
        BossManager.getInstance().registerBossFactory(ManaColor.GREEN, (id, bus) ->
                new GenericElementalBoss(id, "mtg:custom_titania", ManaColor.GREEN, 500.0f, 20.0f, bus));

        IBossEntity customBoss = BossManager.getInstance().createBossForColor(ManaColor.GREEN, testBus);
        assertNotNull(customBoss);
        assertEquals("mtg:custom_titania", customBoss.getBossType());
        assertEquals(500.0f, customBoss.getMaxHealth());
    }

    @Test
    @DisplayName("4. BossThreatTable tracks damage aggro and identifies top threat player")
    void testBossThreatTableAggro() {
        BossThreatTable threatTable = new BossThreatTable();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        threatTable.addDamageThreat(player1, 25.0f);
        threatTable.addDamageThreat(player2, 60.0f);

        assertEquals(player2, threatTable.getHighestThreatTarget().orElse(null), "Player with 60 damage must be top threat");
    }

    @Test
    @DisplayName("5. Boss defeat drops HP to 0 and dispatches BossDefeatedEvent on TriggerBus")
    void testBossDefeatEventDispatched() {
        AtomicBoolean defeatFired = new AtomicBoolean(false);
        testBus.registerListener(BossDefeatedEvent.class, e -> defeatFired.set(true));

        GenericElementalBoss boss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_avatar_red", ManaColor.RED, 200.0f, 12.0f, testBus);
        boss.takeDamage(UUID.randomUUID(), 200.0f);

        assertEquals(0.0f, boss.getCurrentHealth());
        assertFalse(boss.isAlive());
        assertTrue(defeatFired.get(), "BossDefeatedEvent must be dispatched on death");
    }
}
