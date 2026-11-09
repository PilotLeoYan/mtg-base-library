package com.mtg.test.deck;

import com.mtg.network.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dedicated Card Hand — Packet Serialization & Networking Suite")
public class DeckPacketSerializationTest {

    @Test
    @DisplayName("S2CSyncDeckPacket round-trip preserves capacity, selection, and all 7 slot payloads")
    void testSyncDeckPacketRoundTrip() {
        UUID playerId = UUID.randomUUID();
        List<S2CSyncDeckPacket.SlotSyncData> slots = new ArrayList<>();
        slots.add(new S2CSyncDeckPacket.SlotSyncData(0, "mtg:lightning_bolt", 20, 60, 0, 0));
        slots.add(new S2CSyncDeckPacket.SlotSyncData(1, "mtg:counterspell", 0, 40, 5, 20));
        slots.add(new S2CSyncDeckPacket.SlotSyncData(2, null, 0, 0, 0, 0));
        slots.add(new S2CSyncDeckPacket.SlotSyncData(3, "mtg:opt", 0, 10, 0, 0));
        slots.add(new S2CSyncDeckPacket.SlotSyncData(4, null, 0, 0, 0, 0));
        slots.add(new S2CSyncDeckPacket.SlotSyncData(5, null, 0, 0, 0, 0));
        slots.add(new S2CSyncDeckPacket.SlotSyncData(6, null, 0, 0, 0, 0));

        S2CSyncDeckPacket packet = new S2CSyncDeckPacket(playerId, 4, 1, slots);
        assertEquals(S2CSyncDeckPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        S2CSyncDeckPacket decoded = S2CSyncDeckPacket.decode(encoded);

        assertEquals(playerId, decoded.playerId());
        assertEquals(4, decoded.capacity());
        assertEquals(1, decoded.selectedSlot());
        assertEquals(7, decoded.slots().size());

        S2CSyncDeckPacket.SlotSyncData s0 = decoded.slots().get(0);
        assertEquals(0, s0.slotIndex());
        assertEquals("mtg:lightning_bolt", s0.cardId());
        assertEquals(20, s0.cooldownRemaining());
        assertEquals(60, s0.maxCooldown());

        S2CSyncDeckPacket.SlotSyncData s1 = decoded.slots().get(1);
        assertEquals(1, s1.slotIndex());
        assertEquals("mtg:counterspell", s1.cardId());
        assertEquals(5, s1.windupRemaining());
        assertEquals(20, s1.maxWindup());

        assertNull(decoded.slots().get(2).cardId());
    }

    @Test
    @DisplayName("C2SSelectCardPacket encode and decode round-trip")
    void testSelectCardPacketRoundTrip() {
        C2SSelectCardPacket packet = new C2SSelectCardPacket(3);
        assertEquals(C2SSelectCardPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        C2SSelectCardPacket decoded = C2SSelectCardPacket.decode(encoded);

        assertEquals(3, decoded.slotIndex());
    }

    @Test
    @DisplayName("C2SCastCardPacket encode and decode round-trip")
    void testCastCardPacketRoundTrip() {
        C2SCastCardPacket packet = new C2SCastCardPacket(2, 42, 100.5, 64.0, -200.75);
        assertEquals(C2SCastCardPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        C2SCastCardPacket decoded = C2SCastCardPacket.decode(encoded);

        assertEquals(2, decoded.slotIndex());
        assertEquals(42, decoded.targetEntityId());
        assertEquals(100.5, decoded.targetX(), 0.001);
        assertEquals(64.0, decoded.targetY(), 0.001);
        assertEquals(-200.75, decoded.targetZ(), 0.001);
    }

    @Test
    @DisplayName("C2SUpgradeCapacityPacket encode and decode round-trip")
    void testUpgradeCapacityPacketRoundTrip() {
        C2SUpgradeCapacityPacket packet = new C2SUpgradeCapacityPacket();
        assertEquals(C2SUpgradeCapacityPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        C2SUpgradeCapacityPacket decoded = C2SUpgradeCapacityPacket.decode(encoded);
        assertNotNull(decoded);
    }

    @Test
    @DisplayName("C2SEquipCardPacket encode and decode round-trip")
    void testEquipCardPacketRoundTrip() {
        C2SEquipCardPacket packet = new C2SEquipCardPacket(1, "mtg:dark_ritual");
        assertEquals(C2SEquipCardPacket.CHANNEL, packet.getChannelId());

        byte[] encoded = packet.encode();
        C2SEquipCardPacket decoded = C2SEquipCardPacket.decode(encoded);

        assertEquals(1, decoded.slotIndex());
        assertEquals("mtg:dark_ritual", decoded.cardId());
    }

    @Test
    @DisplayName("Malformed byte streams fail cleanly with IllegalArgumentException")
    void testMalformedDeckPacketsThrowException() {
        assertThrows(NullPointerException.class, () -> S2CSyncDeckPacket.decode(null));
        assertThrows(IllegalArgumentException.class, () -> S2CSyncDeckPacket.decode(new byte[5]));
        assertThrows(IllegalArgumentException.class, () -> C2SSelectCardPacket.decode(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> C2SCastCardPacket.decode(new byte[10]));
        assertThrows(IllegalArgumentException.class, () -> C2SEquipCardPacket.decode(new byte[0]));
    }
}
