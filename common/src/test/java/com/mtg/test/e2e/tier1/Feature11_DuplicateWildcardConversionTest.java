package com.mtg.test.e2e.tier1;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardRarity;
import com.mtg.progression.CardAwardResult;
import com.mtg.progression.PlayerProgression;
import com.mtg.progression.WildcardTransmutationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 11: Duplicate Protection & Wildcard Conversion E2E Test")
public class Feature11_DuplicateWildcardConversionTest {

    private PlayerProgression progression;
    private ProgressionConfig config;

    @BeforeEach
    void setUp() {
        progression = new PlayerProgression(UUID.randomUUID());
        config = ProgressionConfig.defaults();
    }

    @Test
    @DisplayName("1. Single duplicate card increments duplicate count without awarding wildcard")
    void testSingleDuplicateAccumulation() {
        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, E2ETestExpansionHelper.TEST_LLANOWAR_ELVES.id(), CardRarity.COMMON, 1);

        assertEquals(CardAwardResult.Type.DUPLICATE_ACCUMULATED, result.type());
        assertEquals(1, progression.getDuplicateCount(CardRarity.COMMON));
        assertEquals(0, progression.getWildcardCount(CardRarity.UNCOMMON));
    }

    @Test
    @DisplayName("2. Reaching 4 duplicate Commons transmutes into 1 Uncommon Wildcard")
    void testCommonToUncommonWildcardConversion() {
        progression.setDuplicateCount(CardRarity.COMMON, 3);
        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), CardRarity.COMMON, 1);

        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertTrue(result.didTransmute());
        assertEquals(1, progression.getWildcardCount(CardRarity.UNCOMMON), "Must award 1 Uncommon wildcard");
    }

    @Test
    @DisplayName("3. Duplicate counter resets to 0 after transmutation event")
    void testDuplicateCounterResetAfterConversion() {
        progression.setDuplicateCount(CardRarity.COMMON, 3);
        WildcardTransmutationEngine.processDuplicate(
                progression, config, E2ETestExpansionHelper.TEST_LIGHTNING_BOLT.id(), CardRarity.COMMON, 1);

        assertEquals(0, progression.getDuplicateCount(CardRarity.COMMON),
                "Common duplicate counter must reset to 0 after conversion");
    }

    @Test
    @DisplayName("4. Redeeming wildcard unlocks target card and decrements wildcard balance")
    void testWildcardRedemptionUnlocksCard() {
        progression.addWildcards(CardRarity.UNCOMMON, 1);
        assertEquals(1, progression.getWildcardCount(CardRarity.UNCOMMON));

        String targetCard = E2ETestExpansionHelper.TEST_COUNTERSPELL.id();
        assertFalse(progression.hasCard(targetCard));

        boolean consumed = progression.consumeWildcard(CardRarity.UNCOMMON, 1);
        assertTrue(consumed, "Wildcard consumption must succeed");
        progression.unlockCard(targetCard);

        assertEquals(0, progression.getWildcardCount(CardRarity.UNCOMMON), "Wildcard balance must decrement");
        assertTrue(progression.hasCard(targetCard), "Target card must now be unlocked in progression");
    }

    @Test
    @DisplayName("5. Accumulating 4 duplicate Uncommons transmutes into 1 Rare Wildcard")
    void testUncommonToRareWildcardConversion() {
        progression.setDuplicateCount(CardRarity.UNCOMMON, 3);
        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, E2ETestExpansionHelper.TEST_COUNTERSPELL.id(), CardRarity.UNCOMMON, 1);

        assertTrue(result.didTransmute());
        assertEquals(1, progression.getWildcardCount(CardRarity.RARE), "Must award 1 Rare wildcard");
        assertEquals(0, progression.getDuplicateCount(CardRarity.UNCOMMON));
    }
}
