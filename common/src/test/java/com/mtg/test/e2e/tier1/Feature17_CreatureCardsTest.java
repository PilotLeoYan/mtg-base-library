package com.mtg.test.e2e.tier1;

import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 17: Creature Cards (Player-Aligned Mob Invocation AI, P/T) E2E Test")
public class Feature17_CreatureCardsTest {

    private UUID playerId;
    private SummonManager summonManager;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        summonManager = SummonManager.getInstance();
        summonManager.clear();
    }

    @AfterEach
    void tearDown() {
        summonManager.clear();
    }

    @Test
    @DisplayName("1. Summoning Serra Angel yields player-aligned creature with base 4/4 combat stats")
    void testSerraAngelStats() {
        SummonedCreature angel = new SummonedCreature(playerId, E2ETestExpansionHelper.TEST_SERRA_ANGEL);
        assertEquals(playerId, angel.getOwnerUuid());
        assertTrue(angel.isOwner(playerId));

        assertEquals(4, angel.getPower(), "Serra Angel base power must be 4");
        assertEquals(4, angel.getToughness(), "Serra Angel base toughness must be 4");
    }

    @Test
    @DisplayName("2. Serra Angel possesses FLYING and VIGILANCE keywords")
    void testSerraAngelKeywords() {
        SummonedCreature angel = new SummonedCreature(playerId, E2ETestExpansionHelper.TEST_SERRA_ANGEL);
        assertTrue(angel.hasKeyword("mtg:flying"), "Serra Angel must have Flying");
        assertTrue(angel.hasKeyword("mtg:vigilance"), "Serra Angel must have Vigilance");
    }

    @Test
    @DisplayName("3. SummonManager registers and tracks active invocations by owner UUID")
    void testSummonManagerRegistration() {
        SummonedCreature angel = new SummonedCreature(playerId, E2ETestExpansionHelper.TEST_SERRA_ANGEL);
        summonManager.registerSummon(angel);

        List<SummonedCreature> summons = summonManager.getPlayerSummons(playerId);
        assertEquals(1, summons.size());
        assertEquals(angel.getEntityId(), summons.get(0).getEntityId());
        assertTrue(summonManager.getSummon(angel.getEntityId()).isPresent());
    }

    @Test
    @DisplayName("4. Creature summoning sickness can be cleared on turn stabilization")
    void testSummoningSicknessClearance() {
        SummonedCreature angel = new SummonedCreature(playerId, E2ETestExpansionHelper.TEST_SERRA_ANGEL);
        assertTrue(angel.hasSummoningSickness(), "Creature without Haste has summoning sickness initially");

        angel.clearSummoningSickness();
        assertFalse(angel.hasSummoningSickness(), "Summoning sickness should be cleared");
    }

    @Test
    @DisplayName("5. Taking lethal damage kills creature and marks isDead true")
    void testCreatureLethalDamageResolution() {
        SummonedCreature angel = new SummonedCreature(playerId, E2ETestExpansionHelper.TEST_SERRA_ANGEL);
        summonManager.registerSummon(angel);
        assertFalse(angel.isDead());

        // Deal non-lethal damage (10.0 damage on 40.0 hp)
        angel.takeDamage(10.0f);
        assertFalse(angel.isDead());

        // Deal fatal damage
        angel.takeDamage(100.0f);
        assertTrue(angel.isDead(), "Creature should be dead after lethal damage");
    }
}
