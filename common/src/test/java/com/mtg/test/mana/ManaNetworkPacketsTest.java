package com.mtg.test.mana;

import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaPool;
import com.mtg.network.C2SUpgradeMaxManaPacket;
import com.mtg.network.S2COpenManaPromptPacket;
import com.mtg.network.S2CSyncManaPacket;
import com.mtg.network.SelectInitialManaPacket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Subsystem — Network Packet Serialization Suite")
public class ManaNetworkPacketsTest {

    @Test
    @DisplayName("S2COpenManaPromptPacket encode and decode round-trip")
    void testOpenManaPromptPacketSerialization() {
        S2COpenManaPromptPacket packet = new S2COpenManaPromptPacket();
        assertEquals(S2COpenManaPromptPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        assertNotNull(encoded);

        S2COpenManaPromptPacket decoded = S2COpenManaPromptPacket.decode(encoded);
        assertNotNull(decoded);
    }

    @Test
    @DisplayName("SelectInitialManaPacket round-trip across all ManaColors")
    void testSelectInitialManaPacketRoundTrip() {
        for (ManaColor color : ManaColor.values()) {
            SelectInitialManaPacket packet = new SelectInitialManaPacket(color);
            assertEquals(SelectInitialManaPacket.CHANNEL, packet.getChannelId());

            byte[] encoded = packet.encode();
            SelectInitialManaPacket decoded = SelectInitialManaPacket.decode(encoded);

            assertEquals(color, decoded.selectedColor());
        }
    }

    @Test
    @DisplayName("S2CSyncManaPacket round-trip preserves all pool balances and state")
    void testSyncManaPacketRoundTrip() {
        UUID playerId = UUID.randomUUID();
        ManaPool pool = new ManaPool(playerId, 3, 5, 7, 9, 11, 2, 45, true, ManaColor.BLACK);

        S2CSyncManaPacket packet = new S2CSyncManaPacket(pool);
        assertEquals(S2CSyncManaPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        S2CSyncManaPacket decoded = S2CSyncManaPacket.decode(encoded);

        ManaPool decodedPool = decoded.pool();
        assertEquals(playerId, decodedPool.getPlayerId());
        assertEquals(3, decodedPool.get(ManaColor.WHITE));
        assertEquals(5, decodedPool.get(ManaColor.BLUE));
        assertEquals(7, decodedPool.get(ManaColor.BLACK));
        assertEquals(9, decodedPool.get(ManaColor.RED));
        assertEquals(11, decodedPool.get(ManaColor.GREEN));
        assertEquals(2, decodedPool.get(ManaColor.COLORLESS));
        assertEquals(45, decodedPool.getMaxMana());
        assertTrue(decodedPool.hasChosenInitialAffinity());
        assertEquals(ManaColor.BLACK, decodedPool.getChosenAffinity());
    }

    @Test
    @DisplayName("C2SUpgradeMaxManaPacket round-trip preserves offered XP level quantity")
    void testUpgradeMaxManaPacketRoundTrip() {
        C2SUpgradeMaxManaPacket packet = new C2SUpgradeMaxManaPacket(15);
        assertEquals(C2SUpgradeMaxManaPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        C2SUpgradeMaxManaPacket decoded = C2SUpgradeMaxManaPacket.decode(encoded);

        assertEquals(15, decoded.offeredXpLevels());
    }

    @Test
    @DisplayName("Decoding malformed or truncated byte arrays throws clean exception")
    void testMalformedDataHandling() {
        assertThrows(NullPointerException.class, () -> SelectInitialManaPacket.decode(null));
        assertThrows(IllegalArgumentException.class, () -> SelectInitialManaPacket.decode(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> S2CSyncManaPacket.decode(new byte[10]));
        assertThrows(IllegalArgumentException.class, () -> C2SUpgradeMaxManaPacket.decode(new byte[1]));
    }
}
