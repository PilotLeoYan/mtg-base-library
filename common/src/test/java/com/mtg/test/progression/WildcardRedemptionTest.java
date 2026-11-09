package com.mtg.test.progression;

import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import com.mtg.progression.PlayerProgression;
import com.mtg.progression.ProgressionManager;
import com.mtg.progression.WildcardRedemptionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression & Economy — Wildcard Redemption Validation Suite")
public class WildcardRedemptionTest {

    private ProgressionManager manager;
    private final CardDefinition wrathOfGod = new CardDefinition(
            "mtg:base/wrath_of_god", "BASE", "Wrath of God",
            new ManaCost(2, 2, 0, 0, 0, 0, 0, null),
            Set.of(CardType.SORCERY), Set.of(), CardRarity.RARE,
            Optional.empty(), List.of(), 80, 0
    );
    private final CardDefinition blackLotus = new CardDefinition(
            "mtg:base/black_lotus", "BASE", "Black Lotus",
            ManaCost.ZERO,
            Set.of(CardType.ARTIFACT), Set.of(), CardRarity.MYTHIC,
            Optional.empty(), List.of(), 60, 0
    );

    @BeforeEach
    void setup() {
        manager = new ProgressionManager();
        manager.registerCard(wrathOfGod);
        manager.registerCard(blackLotus);
    }

    @Test
    @DisplayName("Successful wildcard redemption consumes wildcard and unlocks card permanently")
    void testSuccessfulWildcardRedemption() {
        UUID playerId = UUID.randomUUID();
        PlayerProgression progression = manager.getProgression(playerId);
        progression.addWildcards(CardRarity.RARE, 1);

        assertFalse(progression.hasCard("mtg:base/wrath_of_god"));
        assertEquals(1, progression.getWildcardCount(CardRarity.RARE));

        WildcardRedemptionResult result = manager.redeemWildcard(
                playerId, CardRarity.RARE, "mtg:base/wrath_of_god"
        );

        assertTrue(result.isSuccess());
        assertEquals(WildcardRedemptionResult.Status.SUCCESS, result.status());
        assertTrue(progression.hasCard("mtg:base/wrath_of_god"));
        assertEquals(0, progression.getWildcardCount(CardRarity.RARE));
        assertEquals(0, result.remainingWildcards());
    }

    @Test
    @DisplayName("Rarity mismatch rejects redemption without consuming wildcard")
    void testRarityMismatchRejection() {
        UUID playerId = UUID.randomUUID();
        PlayerProgression progression = manager.getProgression(playerId);
        progression.addWildcards(CardRarity.RARE, 2);

        // Attempting to redeem Mythic card using Rare wildcard
        WildcardRedemptionResult result = manager.redeemWildcard(
                playerId, CardRarity.RARE, "mtg:base/black_lotus"
        );

        assertFalse(result.isSuccess());
        assertEquals(WildcardRedemptionResult.Status.RARITY_MISMATCH, result.status());
        assertEquals(2, progression.getWildcardCount(CardRarity.RARE), "Wildcards must not be consumed on mismatch");
        assertFalse(progression.hasCard("mtg:base/black_lotus"));
    }

    @Test
    @DisplayName("Insufficient wildcards rejects redemption")
    void testInsufficientWildcardsRejection() {
        UUID playerId = UUID.randomUUID();
        PlayerProgression progression = manager.getProgression(playerId);
        assertEquals(0, progression.getWildcardCount(CardRarity.RARE));

        WildcardRedemptionResult result = manager.redeemWildcard(
                playerId, CardRarity.RARE, "mtg:base/wrath_of_god"
        );

        assertFalse(result.isSuccess());
        assertEquals(WildcardRedemptionResult.Status.INSUFFICIENT_WILDCARDS, result.status());
        assertFalse(progression.hasCard("mtg:base/wrath_of_god"));
    }

    @Test
    @DisplayName("Already unlocked card rejects duplicate redemption without wasting wildcards")
    void testAlreadyUnlockedRejection() {
        UUID playerId = UUID.randomUUID();
        PlayerProgression progression = manager.getProgression(playerId);
        progression.addWildcards(CardRarity.RARE, 2);
        progression.unlockCard("mtg:base/wrath_of_god");

        WildcardRedemptionResult result = manager.redeemWildcard(
                playerId, CardRarity.RARE, "mtg:base/wrath_of_god"
        );

        assertFalse(result.isSuccess());
        assertEquals(WildcardRedemptionResult.Status.ALREADY_UNLOCKED, result.status());
        assertEquals(2, progression.getWildcardCount(CardRarity.RARE));
    }

    @Test
    @DisplayName("Redeeming non-existent card ID fails with INVALID_CARD")
    void testInvalidCardIdRejection() {
        UUID playerId = UUID.randomUUID();
        PlayerProgression progression = manager.getProgression(playerId);
        progression.addWildcards(CardRarity.COMMON, 5);

        WildcardRedemptionResult result = manager.redeemWildcard(
                playerId, CardRarity.COMMON, "mtg:non_existent_card"
        );

        assertFalse(result.isSuccess());
        assertEquals(WildcardRedemptionResult.Status.INVALID_CARD, result.status());
        assertEquals(5, progression.getWildcardCount(CardRarity.COMMON));
    }
}
