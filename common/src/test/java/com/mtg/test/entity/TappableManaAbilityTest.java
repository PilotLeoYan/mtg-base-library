package com.mtg.test.entity;

import com.mtg.api.trigger.TapPermanentEvent;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.entity.SummonedCreature;
import com.mtg.entity.TappableManaAbility;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Tappable Mana Ability & Llanowar Elves Suite")
public class TappableManaAbilityTest {

    private UUID ownerId;
    private SummonedCreature elf;
    private TappableManaAbility manaAbility;
    private TriggerBus testBus;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        elf = new SummonedCreature(ownerId, TestCreatures.LLANOWAR_ELVES);
        testBus = new TriggerBus();
        manaAbility = new TappableManaAbility(elf, ManaColor.GREEN, 1, testBus);

        manaPool = ManaManager.getInstance().getOrCreatePlayerMana(ownerId);
        manaPool.clear();
        manaPool.setMaxMana(20);
    }

    @Test
    @DisplayName("1. Summoning Sickness: Cannot activate tap ability immediately on spawn")
    void testCannotTapWithSummoningSickness() {
        assertTrue(elf.hasSummoningSickness(), "Summon starts with summoning sickness");
        assertFalse(manaAbility.canActivate(ownerId), "Tap ability cannot be activated while creature has summoning sickness");
        assertFalse(manaAbility.activate(ownerId));
        assertEquals(0, manaPool.get(ManaColor.GREEN));
    }

    @Test
    @DisplayName("2. Successful Tap: Adds {G} to owner's pool and fires TapPermanentEvent")
    void testSuccessfulTapGeneratesMana() {
        elf.clearSummoningSickness();
        assertTrue(manaAbility.canActivate(ownerId));

        AtomicBoolean tapEventFired = new AtomicBoolean(false);
        testBus.registerListener(TapPermanentEvent.class, event -> {
            if (event.getOwnerId().equals(ownerId)) {
                tapEventFired.set(true);
            }
        });

        boolean activated = manaAbility.activate(ownerId);
        assertTrue(activated, "Tap ability should activate successfully");
        assertTrue(elf.isTapped(), "Creature should now be tapped");
        assertEquals(1, manaPool.get(ManaColor.GREEN), "Owner should receive 1 Green mana");
        assertTrue(tapEventFired.get(), "TapPermanentEvent should be posted on TriggerBus");
    }

    @Test
    @DisplayName("3. Tapped State: Cannot activate tap ability while already tapped")
    void testCannotTapWhileTapped() {
        elf.clearSummoningSickness();
        assertTrue(manaAbility.activate(ownerId));
        assertEquals(1, manaPool.get(ManaColor.GREEN));

        // Attempt second activation while tapped
        assertFalse(manaAbility.canActivate(ownerId));
        assertFalse(manaAbility.activate(ownerId));
        assertEquals(1, manaPool.get(ManaColor.GREEN), "Mana balance should not increase from redundant tap attempt");

        // Untap creature (e.g. at player upkeep)
        elf.untap();
        assertFalse(elf.isTapped());
        assertTrue(manaAbility.canActivate(ownerId));

        // Third activation after untap
        assertTrue(manaAbility.activate(ownerId));
        assertEquals(2, manaPool.get(ManaColor.GREEN));
    }

    @Test
    @DisplayName("4. Ownership Gate: Another player cannot activate summon's tap ability")
    void testNonOwnerCannotActivate() {
        elf.clearSummoningSickness();
        UUID otherPlayerId = UUID.randomUUID();

        assertFalse(manaAbility.canActivate(otherPlayerId), "Non-owner cannot activate tap ability");
        assertFalse(manaAbility.activate(otherPlayerId));
        assertEquals(0, manaPool.get(ManaColor.GREEN));
    }
}
