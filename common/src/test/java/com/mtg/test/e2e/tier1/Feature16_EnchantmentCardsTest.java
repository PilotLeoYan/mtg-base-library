package com.mtg.test.e2e.tier1;

import com.mtg.cards.effects.ActiveEnchantment;
import com.mtg.cards.effects.EnchantmentManager;
import com.mtg.cards.effects.IEnchantmentTickListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 16: Enchantment Cards (Persistent Timed Aura Duration) E2E Test")
public class Feature16_EnchantmentCardsTest {

    private UUID playerId;
    private EnchantmentManager enchantmentManager;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        enchantmentManager = EnchantmentManager.getInstance();
        enchantmentManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        enchantmentManager.clearAll();
    }

    @Test
    @DisplayName("1. Adding ActiveEnchantment registers timed aura under player UUID")
    void testRegisterActiveEnchantment() {
        ActiveEnchantment anthem = new ActiveEnchantment(
                playerId,
                E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM,
                ActiveEnchantment.EnchantmentTargetType.CASTER,
                0,
                1200,
                null
        );

        enchantmentManager.addEnchantment(anthem);
        List<ActiveEnchantment> auras = enchantmentManager.getPlayerEnchantments(playerId);

        assertEquals(1, auras.size(), "Player must have 1 active enchantment");
        assertEquals(E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM.id(), auras.get(0).getCard().id());
        assertEquals(1200, auras.get(0).getRemainingTicks());
    }

    @Test
    @DisplayName("2. hasEnchantment queries confirm active status of specific enchantment card")
    void testHasEnchantmentQuery() {
        assertFalse(enchantmentManager.hasEnchantment(playerId, E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM.id()));

        ActiveEnchantment anthem = new ActiveEnchantment(
                playerId, E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM,
                ActiveEnchantment.EnchantmentTargetType.CASTER, 0, 600, null
        );
        enchantmentManager.addEnchantment(anthem);

        assertTrue(enchantmentManager.hasEnchantment(playerId, E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM.id()));
    }

    @Test
    @DisplayName("3. Ticking EnchantmentManager decrements active enchantment remaining duration")
    void testEnchantmentTickDown() {
        ActiveEnchantment aura = new ActiveEnchantment(
                playerId, E2ETestExpansionHelper.TEST_RANCOR,
                ActiveEnchantment.EnchantmentTargetType.CASTER, 0, 100, null
        );
        enchantmentManager.addEnchantment(aura);

        for (int i = 0; i < 10; i++) {
            enchantmentManager.tickAll();
        }

        assertEquals(90, aura.getRemainingTicks(), "Remaining duration should decrease from 100 to 90");
    }

    @Test
    @DisplayName("4. Enchantment expires and automatically removes when duration elapses to 0")
    void testEnchantmentExpirationAndRemoval() {
        AtomicBoolean expired = new AtomicBoolean(false);
        IEnchantmentTickListener listener = new IEnchantmentTickListener() {
            @Override
            public void onExpire(ActiveEnchantment enchantment) {
                expired.set(true);
            }
        };

        ActiveEnchantment shortAura = new ActiveEnchantment(
                playerId, E2ETestExpansionHelper.TEST_LEVITATION,
                ActiveEnchantment.EnchantmentTargetType.CASTER, 0, 2, listener
        );
        enchantmentManager.addEnchantment(shortAura);

        enchantmentManager.tickAll(); // remaining: 1
        assertFalse(shortAura.isExpired());
        assertEquals(1, enchantmentManager.getPlayerEnchantments(playerId).size());

        enchantmentManager.tickAll(); // remaining: 0 -> expires
        assertTrue(shortAura.isExpired(), "Aura must be marked expired");
        assertTrue(expired.get(), "onExpire callback must fire on listener");
        assertEquals(0, enchantmentManager.getPlayerEnchantments(playerId).size(), "Expired aura must be removed from manager");
    }

    @Test
    @DisplayName("5. removePlayerEnchantments clears all active auras for player")
    void testRemovePlayerEnchantments() {
        enchantmentManager.addEnchantment(new ActiveEnchantment(playerId, E2ETestExpansionHelper.TEST_GLORIOUS_ANTHEM, ActiveEnchantment.EnchantmentTargetType.CASTER, 0, 600, null));
        enchantmentManager.addEnchantment(new ActiveEnchantment(playerId, E2ETestExpansionHelper.TEST_PHYREXIAN_ARENA, ActiveEnchantment.EnchantmentTargetType.CASTER, 0, 600, null));

        assertEquals(2, enchantmentManager.getPlayerEnchantments(playerId).size());
        enchantmentManager.removePlayerEnchantments(playerId);
        assertEquals(0, enchantmentManager.getPlayerEnchantments(playerId).size(), "All enchantments must be cleared for player");
    }
}
