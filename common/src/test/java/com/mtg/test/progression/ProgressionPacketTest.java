package com.mtg.test.progression;

import com.mtg.domain.CardRarity;
import com.mtg.network.C2SRedeemWildcardPacket;
import com.mtg.network.S2CSyncCollectionPacket;
import com.mtg.network.S2CUnlockCardPacket;
import com.mtg.progression.CardUnlockSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression & Economy — Packet Serialization Suite")
public class ProgressionPacketTest {

    @Test
    @DisplayName("S2CUnlockCardPacket round-trip preserves unlock and duplicate status")
    void testS2CUnlockCardPacketRoundTrip() {
        // Case A: New unlock
        S2CUnlockCardPacket p1 = new S2CUnlockCardPacket(
                "mtg:lightning_bolt", CardRarity.COMMON, CardUnlockSource.MINING,
                false, 0, 4, false, null, 0
        );
        assertEquals(S2CUnlockCardPacket.CHANNEL, p1.getChannelId());
        S2CUnlockCardPacket d1 = S2CUnlockCardPacket.decode(p1.encode());
        assertEquals(p1.cardId(), d1.cardId());
        assertEquals(p1.rarity(), d1.rarity());
        assertEquals(p1.source(), d1.source());
        assertFalse(d1.isDuplicate());
        assertFalse(d1.wildcardAwarded());
        assertNull(d1.wildcardRarity());

        // Case B: Duplicate transmutation to Rare Wildcard
        S2CUnlockCardPacket p2 = new S2CUnlockCardPacket(
                "mtg:counterspell", CardRarity.UNCOMMON, CardUnlockSource.DUNGEON_CHEST,
                true, 0, 4, true, CardRarity.RARE, 3
        );
        S2CUnlockCardPacket d2 = S2CUnlockCardPacket.decode(p2.encode());
        assertEquals(p2.cardId(), d2.cardId());
        assertTrue(d2.isDuplicate());
        assertTrue(d2.wildcardAwarded());
        assertEquals(CardRarity.RARE, d2.wildcardRarity());
        assertEquals(3, d2.totalWildcardsOfRarity());
    }

    @Test
    @DisplayName("S2CSyncCollectionPacket round-trip preserves cards, duplicates, and wildcards")
    void testS2CSyncCollectionPacketRoundTrip() {
        UUID playerId = UUID.randomUUID();
        Set<String> cards = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            cards.add("mtg:card_" + i);
        }

        Map<CardRarity, Integer> dups = Map.of(
                CardRarity.COMMON, 3,
                CardRarity.UNCOMMON, 1,
                CardRarity.RARE, 2,
                CardRarity.MYTHIC, 0
        );

        Map<CardRarity, Integer> wildcards = Map.of(
                CardRarity.COMMON, 5,
                CardRarity.UNCOMMON, 4,
                CardRarity.RARE, 2,
                CardRarity.MYTHIC, 1
        );

        S2CSyncCollectionPacket packet = new S2CSyncCollectionPacket(playerId, cards, dups, wildcards);
        assertEquals(S2CSyncCollectionPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        S2CSyncCollectionPacket decoded = S2CSyncCollectionPacket.decode(encoded);

        assertEquals(playerId, decoded.playerId());
        assertEquals(cards.size(), decoded.unlockedCardIds().size());
        assertEquals(cards, decoded.unlockedCardIds());
        assertEquals(dups, decoded.duplicateCounters());
        assertEquals(wildcards, decoded.wildcardBalances());
    }

    @Test
    @DisplayName("C2SRedeemWildcardPacket round-trip across all rarities")
    void testC2SRedeemWildcardPacketRoundTrip() {
        for (CardRarity r : CardRarity.values()) {
            C2SRedeemWildcardPacket packet = new C2SRedeemWildcardPacket(r, "mtg:target_" + r.name());
            assertEquals(C2SRedeemWildcardPacket.CHANNEL, packet.getChannelId());

            byte[] encoded = packet.encode();
            C2SRedeemWildcardPacket decoded = C2SRedeemWildcardPacket.decode(encoded);

            assertEquals(r, decoded.rarity());
            assertEquals("mtg:target_" + r.name(), decoded.targetCardId());
        }
    }

    @Test
    @DisplayName("Malformed byte streams fail cleanly with IllegalArgumentException")
    void testCorruptedPacketFailsCleanly() {
        assertThrows(NullPointerException.class, () -> S2CUnlockCardPacket.decode(null));
        assertThrows(IllegalArgumentException.class, () -> S2CUnlockCardPacket.decode(new byte[3]));
        assertThrows(IllegalArgumentException.class, () -> S2CSyncCollectionPacket.decode(new byte[10]));
        assertThrows(IllegalArgumentException.class, () -> C2SRedeemWildcardPacket.decode(new byte[0]));
    }
}
