package com.mtg.test.entity;

import com.mtg.entity.SummonedCreature;
import com.mtg.entity.ai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Companion AI Goals & Friendly Fire Prevention Suite")
public class CompanionAiGoalTest {

    private UUID ownerId;
    private SummonedCreature creature;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        creature = new SummonedCreature(ownerId, TestCreatures.SERRA_ANGEL);
        creature.clearSummoningSickness();
        creature.setPosition(0.0, 64.0, 0.0);
    }

    @Test
    @DisplayName("1. FollowOwnerGoal: Idle within stop threshold, moves when beyond follow threshold")
    void testFollowOwnerNormalDistance() {
        FollowOwnerGoal goal = new FollowOwnerGoal(6.0, 3.0, 48.0);

        // Owner at 2 blocks away (<= 3.0 stop threshold)
        AiContext closeContext = AiContext.ofOwnerPos(ownerId, 2.0, 64.0, 0.0);
        assertFalse(goal.canExecute(creature, closeContext));

        // Owner at 10 blocks away (> 6.0 follow threshold)
        AiContext farContext = AiContext.ofOwnerPos(ownerId, 10.0, 64.0, 0.0);
        assertTrue(goal.canExecute(creature, farContext));

        double initialDist = creature.distanceTo(10.0, 64.0, 0.0);
        goal.tick(creature, farContext);

        assertEquals(SummonAiState.FOLLOWING, creature.getAiState());
        double newDist = creature.distanceTo(10.0, 64.0, 0.0);
        assertTrue(newDist < initialDist, "Creature should move closer to owner");
    }

    @Test
    @DisplayName("2. FollowOwnerGoal: Emergency leash teleportation when distance > 48 blocks")
    void testEmergencyLeashTeleportation() {
        FollowOwnerGoal goal = new FollowOwnerGoal(6.0, 3.0, 48.0);

        // Owner teleports 60 blocks away (> 48 blocks leash distance)
        AiContext extremeContext = AiContext.ofOwnerPos(ownerId, 60.0, 64.0, 0.0);
        assertTrue(goal.canExecute(creature, extremeContext));

        goal.tick(creature, extremeContext);

        // Creature should instantly teleport to owner's exact position
        assertEquals(60.0, creature.getX(), 0.001);
        assertEquals(64.0, creature.getY(), 0.001);
        assertEquals(0.0, creature.getZ(), 0.001);
        assertEquals(SummonAiState.IDLE, creature.getAiState());
    }

    @Test
    @DisplayName("3. DefendOwnerGoal: Retaliates against hostile entity that damaged the summoner")
    void testDefendOwnerRetaliation() {
        DefendOwnerGoal goal = new DefendOwnerGoal();
        UUID enemyMobId = UUID.randomUUID();

        AiContext defendContext = AiContext.ofDefend(ownerId, 0.0, 64.0, 0.0, enemyMobId, null);
        assertTrue(goal.canExecute(creature, defendContext));

        goal.tick(creature, defendContext);

        assertEquals(enemyMobId, creature.getTargetEntityId());
        assertEquals(SummonAiState.DEFENDING, creature.getAiState());
    }

    @Test
    @DisplayName("4. AttackOwnerTargetGoal: Assists summoner by targeting owner's attack target")
    void testAttackOwnerTargetAssist() {
        AttackOwnerTargetGoal goal = new AttackOwnerTargetGoal();
        UUID enemyMobId = UUID.randomUUID();

        AiContext attackContext = AiContext.ofAttack(ownerId, 0.0, 64.0, 0.0, enemyMobId, null);
        assertTrue(goal.canExecute(creature, attackContext));

        goal.tick(creature, attackContext);

        assertEquals(enemyMobId, creature.getTargetEntityId());
        assertEquals(SummonAiState.ATTACKING, creature.getAiState());
    }

    @Test
    @DisplayName("5. FriendlyFireFilter: Never targets owner player or friendly summon of the same owner")
    void testFriendlyFirePrevention() {
        SummonedCreature friendlySummon = new SummonedCreature(ownerId, TestCreatures.LLANOWAR_ELVES);

        UUID otherPlayerId = UUID.randomUUID();
        SummonedCreature enemySummon = new SummonedCreature(otherPlayerId, TestCreatures.LLANOWAR_ELVES);

        // Rejects attacking own summoner
        assertFalse(FriendlyFireFilter.canAttack(creature, ownerId, null), "FriendlyFireFilter must reject targeting owner");

        // Rejects attacking friendly summon of same owner
        assertFalse(FriendlyFireFilter.canAttack(creature, friendlySummon.getEntityId(), friendlySummon),
            "FriendlyFireFilter must reject targeting friendly summon");

        // Allows attacking enemy summon of different owner
        assertTrue(FriendlyFireFilter.canAttack(creature, enemySummon.getEntityId(), enemySummon),
            "FriendlyFireFilter must permit targeting enemy summon");

        // Allows attacking regular hostile mob
        UUID hostileMobId = UUID.randomUUID();
        assertTrue(FriendlyFireFilter.canAttack(creature, hostileMobId, null),
            "FriendlyFireFilter must permit targeting hostile mob");
    }
}
