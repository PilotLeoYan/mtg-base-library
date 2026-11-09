package com.mtg.test.world;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.api.trigger.TriggerPriority;
import com.mtg.domain.ManaColor;
import com.mtg.world.boss.*;
import com.mtg.world.events.BossDefeatedEvent;
import com.mtg.world.events.BossPhaseChangeEvent;
import com.mtg.world.events.BossSpawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("World Boss Combat Mechanics, Abilities & AI Phases Suite")
public class WorldBossCombatAndAiTest {

    private TriggerBus testBus;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        testBus = new TriggerBus();
        testPlayerId = UUID.randomUUID();
        BossManager.getInstance().clear();
        BossManager.getInstance().setTriggerBus(testBus);
    }

    @Test
    @DisplayName("GenericElementalBoss: 3-phase combat progression, enrage attack scaling, healing & defeat")
    void testGenericElementalBossCombatAndPhases() {
        GenericElementalBoss boss = new GenericElementalBoss(UUID.randomUUID(), "mtg:elemental_avatar_red", ManaColor.RED, 200.0f, 12.0f, testBus);

        assertEquals(200.0f, boss.getMaxHealth());
        assertEquals(200.0f, boss.getCurrentHealth());
        assertEquals(12.0f, boss.getAttackDamage());
        assertEquals(BossPhase.PHASE_1_TACTICAL, boss.getPhase());
        assertEquals(ManaColor.RED, boss.getAffinityColor());

        AtomicBoolean phase2Fired = new AtomicBoolean(false);
        AtomicBoolean phase3Fired = new AtomicBoolean(false);
        testBus.registerListener(BossPhaseChangeEvent.class, TriggerPriority.NORMAL, event -> {
            if (event.getNewPhase() == BossPhase.PHASE_2_ENRAGED) {
                phase2Fired.set(true);
            } else if (event.getNewPhase() == BossPhase.PHASE_3_DESPERATION) {
                phase3Fired.set(true);
            }
        });

        // Deal 70 damage (200 -> 130 HP, ratio = 130/200 = 0.65 <= 0.66)
        boss.takeDamage(testPlayerId, 70.0f);
        assertEquals(130.0f, boss.getCurrentHealth());
        assertEquals(BossPhase.PHASE_2_ENRAGED, boss.getPhase());
        assertTrue(phase2Fired.get(), "BossPhaseChangeEvent for PHASE_2_ENRAGED must be posted to TriggerBus");
        assertEquals(15.0f, boss.getAttackDamage(), 0.01f, "Phase 2 enrage scales attack damage to 1.25x (12 * 1.25 = 15)");

        // Deal 70 more damage (130 -> 60 HP, ratio = 60/200 = 0.30 <= 0.33)
        boss.takeDamage(testPlayerId, 70.0f);
        assertEquals(60.0f, boss.getCurrentHealth());
        assertEquals(BossPhase.PHASE_3_DESPERATION, boss.getPhase());
        assertTrue(phase3Fired.get(), "BossPhaseChangeEvent for PHASE_3_DESPERATION must be posted to TriggerBus");
        assertEquals(18.0f, boss.getAttackDamage(), 0.01f, "Phase 3 desperation scales attack damage to 1.50x (12 * 1.5 = 18)");

        // Heal boss back to Phase 2 (60 + 50 = 110 HP, ratio 110/200 = 0.55)
        boss.heal(50.0f);
        assertEquals(110.0f, boss.getCurrentHealth());
        assertEquals(BossPhase.PHASE_2_ENRAGED, boss.getPhase());
        assertEquals(15.0f, boss.getAttackDamage(), 0.01f);

        // Defeat boss
        AtomicBoolean defeated = new AtomicBoolean(false);
        testBus.registerListener(BossDefeatedEvent.class, TriggerPriority.NORMAL, event -> {
            defeated.set(true);
            assertEquals(testPlayerId, event.getKillerPlayerId());
            assertEquals("mtg:elemental_avatar_red", event.getBossType());
        });

        boss.takeDamage(testPlayerId, 110.0f);
        assertEquals(0.0f, boss.getCurrentHealth());
        assertEquals(BossPhase.DEFEATED, boss.getPhase());
        assertFalse(boss.isAlive());
        assertTrue(defeated.get());
    }

    static class CustomBarrierBoss extends AbstractBossEntity {
        private float divineBarrier = 50.0f;
        private int abilityExecutions = 0;

        public CustomBarrierBoss(UUID id, float baseHp, float baseAtk, TriggerBus bus) {
            super(id, "mtg:barrier_boss", baseHp, baseAtk, bus);
        }

        public float getDivineBarrier() {
            return divineBarrier;
        }

        public int getAbilityExecutions() {
            return abilityExecutions;
        }

        @Override
        public synchronized void takeDamage(UUID attackerId, float amount) {
            if (divineBarrier > 0.0f) {
                if (divineBarrier >= amount) {
                    divineBarrier -= amount;
                    return;
                }
                float remaining = amount - divineBarrier;
                divineBarrier = 0.0f;
                super.takeDamage(attackerId, remaining);
            } else {
                super.takeDamage(attackerId, amount);
            }
        }

        @Override
        protected void executeSpecialAbility(String abilityId) {
            if ("holy_nova".equals(abilityId)) {
                abilityExecutions++;
            }
        }
    }

    @Test
    @DisplayName("AbstractBossEntity: Barrier damage absorption, ability cooldowns & execution tracking")
    void testAbstractBossEntityBarrierAndAbilityCooldowns() {
        CustomBarrierBoss boss = new CustomBarrierBoss(UUID.randomUUID(), 300.0f, 15.0f, testBus);

        assertEquals(50.0f, boss.getDivineBarrier());
        assertEquals(300.0f, boss.getCurrentHealth());

        // First 30 damage absorbed by barrier without HP damage
        boss.takeDamage(testPlayerId, 30.0f);
        assertEquals(20.0f, boss.getDivineBarrier());
        assertEquals(300.0f, boss.getCurrentHealth());

        // Next 40 damage: 20 absorbed by barrier, 20 dealt to HP
        boss.takeDamage(testPlayerId, 40.0f);
        assertEquals(0.0f, boss.getDivineBarrier());
        assertEquals(280.0f, boss.getCurrentHealth());

        // Ability cooldown management
        assertTrue(boss.isAbilityReady("holy_nova"));
        assertEquals(0, boss.getAbilityCooldown("holy_nova"));

        boolean executed = boss.tryExecuteSpecialAbility("holy_nova", 20);
        assertTrue(executed);
        assertEquals(1, boss.getAbilityExecutions());
        assertEquals(20, boss.getAbilityCooldown("holy_nova"));
        assertFalse(boss.isAbilityReady("holy_nova"));

        // While on cooldown, further execution fails
        assertFalse(boss.tryExecuteSpecialAbility("holy_nova", 20));
        assertEquals(1, boss.getAbilityExecutions());

        // Tick 10 ticks -> cooldown becomes 10
        for (int i = 0; i < 10; i++) boss.tick();
        assertEquals(10, boss.getAbilityCooldown("holy_nova"));
        assertFalse(boss.isAbilityReady("holy_nova"));

        // Tick remaining 10 ticks -> cooldown becomes ready (0)
        for (int i = 0; i < 10; i++) boss.tick();
        assertEquals(0, boss.getAbilityCooldown("holy_nova"));
        assertTrue(boss.isAbilityReady("holy_nova"));

        // Second execution succeeds
        assertTrue(boss.tryExecuteSpecialAbility("holy_nova", 20));
        assertEquals(2, boss.getAbilityExecutions());
    }

    static class CustomRetaliatoryBoss extends AbstractBossEntity {
        private float lastRecoilDamage = 0.0f;
        private UUID lastRecoilTarget = null;

        public CustomRetaliatoryBoss(UUID id, float baseHp, float baseAtk, TriggerBus bus) {
            super(id, "mtg:retaliatory_avatar", baseHp, baseAtk, bus);
        }

        public float getLastRecoilDamage() {
            return lastRecoilDamage;
        }

        public UUID getLastRecoilTarget() {
            return lastRecoilTarget;
        }

        @Override
        public synchronized void takeDamage(UUID attackerId, float amount) {
            super.takeDamage(attackerId, amount);
            if (attackerId != null && !attackerId.equals(getId())) {
                this.lastRecoilDamage = amount * 0.5f;
                this.lastRecoilTarget = attackerId;
            }
        }
    }

    @Test
    @DisplayName("AbstractBossEntity Subclass: Retaliatory recoil and self-damage safety")
    void testRetaliatoryBossSubclassMechanics() {
        CustomRetaliatoryBoss boss = new CustomRetaliatoryBoss(UUID.randomUUID(), 200.0f, 10.0f, testBus);

        boss.takeDamage(testPlayerId, 40.0f);
        assertEquals(20.0f, boss.getLastRecoilDamage());
        assertEquals(testPlayerId, boss.getLastRecoilTarget());
        assertEquals(160.0f, boss.getCurrentHealth());

        // Self-damage safety without recursion crash
        assertDoesNotThrow(() -> boss.takeDamage(boss.getId(), 10.0f));
        assertEquals(150.0f, boss.getCurrentHealth());
    }

    @Test
    @DisplayName("BossManager: Dynamic Factory Registration, Color Resolution and Active Boss Management")
    void testBossManagerFactoryRegistrationAndLifecycle() {
        BossManager manager = BossManager.getInstance();
        manager.clear();
        manager.setTriggerBus(testBus);

        // 1. Default creation without factory produces GenericElementalBoss
        IBossEntity defaultWhite = manager.createBossForColor(ManaColor.WHITE);
        assertInstanceOf(GenericElementalBoss.class, defaultWhite);
        assertEquals(ManaColor.WHITE, ((GenericElementalBoss) defaultWhite).getAffinityColor());

        // 2. Register custom boss factory for BLACK
        manager.registerBossFactory(ManaColor.BLACK, (id, bus) ->
                new CustomBarrierBoss(id, 450.0f, 18.0f, bus));

        IBossEntity blackBoss = manager.createBossForColor(ManaColor.BLACK);
        assertInstanceOf(CustomBarrierBoss.class, blackBoss);
        assertEquals(450.0f, blackBoss.getMaxHealth());
        assertEquals(18.0f, ((CustomBarrierBoss) blackBoss).getAttackDamage());

        // 3. Register boss in active registry -> fires BossSpawnEvent
        AtomicBoolean spawnEventFired = new AtomicBoolean(false);
        testBus.registerListener(BossSpawnEvent.class, TriggerPriority.NORMAL, e -> {
            if (e.getBossEntityId().equals(blackBoss.getId())) {
                spawnEventFired.set(true);
            }
        });

        manager.registerBoss(blackBoss);
        assertTrue(spawnEventFired.get());
        assertEquals(1, manager.getAllBosses().size());
        assertTrue(manager.getBoss(blackBoss.getId()).isPresent());
        assertSame(blackBoss, manager.getBoss(blackBoss.getId()).get());

        // 4. Tick all active bosses
        manager.tickAll();

        // 5. Remove boss
        manager.removeBoss(blackBoss.getId());
        assertEquals(0, manager.getAllBosses().size());
        assertTrue(manager.getBoss(blackBoss.getId()).isEmpty());
    }
}
