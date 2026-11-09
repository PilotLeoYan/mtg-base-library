package com.mtg.test.progression;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardRarity;
import com.mtg.progression.CardAwardResult;
import com.mtg.progression.PlayerProgression;
import com.mtg.progression.WildcardTransmutationEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression & Economy — WildcardTransmutationEngine 4:1 Conversion Suite")
public class WildcardTransmutationTest {

    private final ProgressionConfig config = ProgressionConfig.defaults();

    @Test
    @DisplayName("Single duplicate Common increments duplicate counter without granting wildcard")
    void testSingleDuplicateAccumulation() {
        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, "mtg:llanowar_elves", CardRarity.COMMON, 1
        );

        assertEquals(CardAwardResult.Type.DUPLICATE_ACCUMULATED, result.type());
        assertEquals(1, progression.getDuplicateCount(CardRarity.COMMON));
        assertEquals(0, progression.getWildcardCount(CardRarity.UNCOMMON));
        assertEquals(1, result.currentDuplicates());
        assertEquals(4, result.threshold());
        assertFalse(result.wildcardAwarded().isPresent());
    }

    @Test
    @DisplayName("Accumulating 4th duplicate Common converts to 1 Uncommon Wildcard and resets duplicate counter")
    void testThresholdConversionCommonToUncommon() {
        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
        progression.setDuplicateCount(CardRarity.COMMON, 3);

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, "mtg:lightning_bolt", CardRarity.COMMON, 1
        );

        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertEquals(0, progression.getDuplicateCount(CardRarity.COMMON), "Duplicate counter must reset to 0 after conversion");
        assertEquals(1, progression.getWildcardCount(CardRarity.UNCOMMON), "Must award 1 Uncommon Wildcard");
        assertTrue(result.didTransmute());
        assertEquals(CardRarity.UNCOMMON, result.wildcardAwarded().orElseThrow());
    }

    @Test
    @DisplayName("Accumulating 4 duplicate Uncommons converts to 1 Rare Wildcard")
    void testThresholdConversionUncommonToRare() {
        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
        progression.setDuplicateCount(CardRarity.UNCOMMON, 3);

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, "mtg:counterspell", CardRarity.UNCOMMON, 1
        );

        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertEquals(0, progression.getDuplicateCount(CardRarity.UNCOMMON));
        assertEquals(1, progression.getWildcardCount(CardRarity.RARE));
        assertEquals(CardRarity.RARE, result.wildcardAwarded().orElseThrow());
    }

    @Test
    @DisplayName("Accumulating 4 duplicate Rares converts to 1 Mythic Wildcard")
    void testThresholdConversionRareToMythic() {
        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
        progression.setDuplicateCount(CardRarity.RARE, 3);

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, "mtg:birds_of_paradise", CardRarity.RARE, 1
        );

        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertEquals(0, progression.getDuplicateCount(CardRarity.RARE));
        assertEquals(1, progression.getWildcardCount(CardRarity.MYTHIC));
        assertEquals(CardRarity.MYTHIC, result.wildcardAwarded().orElseThrow());
    }

    @Test
    @DisplayName("Accumulating 4 duplicate Mythics converts to 1 Mythic Wildcard (Mythic Vault rollover)")
    void testThresholdConversionMythicVault() {
        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
        progression.setDuplicateCount(CardRarity.MYTHIC, 3);

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, "mtg:black_lotus", CardRarity.MYTHIC, 1
        );

        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertEquals(0, progression.getDuplicateCount(CardRarity.MYTHIC));
        assertEquals(1, progression.getWildcardCount(CardRarity.MYTHIC));
        assertEquals(CardRarity.MYTHIC, result.wildcardAwarded().orElseThrow());
    }

    @Test
    @DisplayName("Multi-card batch overflow: adding 9 duplicate Commons grants 2 Uncommon Wildcards and leaves 1 remainder")
    void testMultiCardBatchOverflow() {
        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, config, "mtg:opt", CardRarity.COMMON, 9
        );

        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertEquals(1, progression.getDuplicateCount(CardRarity.COMMON), "9 % 4 = 1 remainder duplicate");
        assertEquals(2, progression.getWildcardCount(CardRarity.UNCOMMON), "9 / 4 = 2 wildcards granted");
        assertEquals(1, result.currentDuplicates());
        assertEquals(2, result.newWildcardBalance());
    }

    @Test
    @DisplayName("Custom configuration thresholds dynamically adjust conversion math")
    void testCustomConfigThresholds() {
        // Custom threshold: 2 Commons -> 1 Uncommon Wildcard
        ProgressionConfig customConfig = new ProgressionConfig(
                4, 7, 10, 1.5,
                2, 2, 2, 2,
                0.02, 0.01, 0.05, 1.0, 2, 5
        );

        PlayerProgression progression = new PlayerProgression(UUID.randomUUID());
        WildcardTransmutationEngine.processDuplicate(progression, customConfig, "mtg:card_a", CardRarity.COMMON, 1);
        assertEquals(1, progression.getDuplicateCount(CardRarity.COMMON));

        CardAwardResult result = WildcardTransmutationEngine.processDuplicate(
                progression, customConfig, "mtg:card_b", CardRarity.COMMON, 1
        );
        assertEquals(CardAwardResult.Type.DUPLICATE_CONVERTED, result.type());
        assertEquals(0, progression.getDuplicateCount(CardRarity.COMMON));
        assertEquals(1, progression.getWildcardCount(CardRarity.UNCOMMON));
    }
}
