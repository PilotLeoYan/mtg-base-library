package com.mtg.progression;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardRarity;

import java.util.Objects;

/**
 * Pure mathematical conversion engine for duplicate cards and MTGA-style wildcard transmutations.
 * Converts excess duplicate copies into higher-tier wildcards:
 * Common duplicates -> Uncommon Wildcards
 * Uncommon duplicates -> Rare Wildcards
 * Rare duplicates -> Mythic Wildcards
 * Mythic duplicates -> Mythic Wildcards (Mythic Vault rollover)
 */
public final class WildcardTransmutationEngine {

    private WildcardTransmutationEngine() {}

    public static CardRarity getTargetWildcardRarity(CardRarity rarity) {
        Objects.requireNonNull(rarity, "CardRarity cannot be null");
        return switch (rarity) {
            case COMMON -> CardRarity.UNCOMMON;
            case UNCOMMON -> CardRarity.RARE;
            case RARE, MYTHIC -> CardRarity.MYTHIC;
        };
    }

    public static int getThreshold(ProgressionConfig config, CardRarity rarity) {
        if (config == null) {
            config = ProgressionConfig.defaults();
        }
        Objects.requireNonNull(rarity, "CardRarity cannot be null");
        return switch (rarity) {
            case COMMON -> config.thresholdCommonToUncommon();
            case UNCOMMON -> config.thresholdUncommonToRare();
            case RARE -> config.thresholdRareToMythic();
            case MYTHIC -> config.thresholdMythicVault();
        };
    }

    public static CardAwardResult processDuplicate(
            PlayerProgression progression,
            ProgressionConfig config,
            String cardId,
            CardRarity rarity,
            int copiesToAdd
    ) {
        Objects.requireNonNull(progression, "PlayerProgression cannot be null");
        Objects.requireNonNull(rarity, "CardRarity cannot be null");
        if (config == null) {
            config = ProgressionConfig.defaults();
        }

        int threshold = getThreshold(config, rarity);
        CardRarity targetWildcard = getTargetWildcardRarity(rarity);

        if (copiesToAdd <= 0) {
            int cur = progression.getDuplicateCount(rarity);
            return CardAwardResult.duplicateAccumulated(cardId, rarity, cur, threshold, progression.getWildcardCount(targetWildcard));
        }

        synchronized (progression) {
            int current = progression.getDuplicateCount(rarity) + copiesToAdd;
            if (current >= threshold) {
                int wildcardsGranted = current / threshold;
                int remainder = current % threshold;

                progression.setDuplicateCount(rarity, remainder);
                progression.addWildcards(targetWildcard, wildcardsGranted);

                return CardAwardResult.duplicateConverted(
                        cardId,
                        rarity,
                        remainder,
                        threshold,
                        targetWildcard,
                        progression.getWildcardCount(targetWildcard)
                );
            } else {
                progression.setDuplicateCount(rarity, current);
                return CardAwardResult.duplicateAccumulated(
                        cardId,
                        rarity,
                        current,
                        threshold,
                        progression.getWildcardCount(targetWildcard)
                );
            }
        }
    }
}
