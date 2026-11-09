package com.mtg.network;

import java.io.*;
import java.util.*;

/**
 * Server-to-Client packet synchronizing the player's full dedicated Planeswalker Deck state.
 */
public record S2CSyncDeckPacket(
    UUID playerId,
    int capacity,
    int selectedSlot,
    List<SlotSyncData> slots
) implements CustomPacket {
    public static final String CHANNEL = "mtg:sync_deck";

    public record SlotSyncData(
        int slotIndex,
        String cardId,
        int cooldownRemaining,
        int maxCooldown,
        int windupRemaining,
        int maxWindup
    ) {}

    public S2CSyncDeckPacket {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        slots = (slots != null) ? List.copyOf(slots) : List.of();
    }

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeLong(playerId.getMostSignificantBits());
            dos.writeLong(playerId.getLeastSignificantBits());
            dos.writeInt(capacity);
            dos.writeInt(selectedSlot);
            dos.writeInt(slots.size());

            for (SlotSyncData slot : slots) {
                dos.writeByte(slot.slotIndex());
                dos.writeUTF(slot.cardId() != null ? slot.cardId() : "");
                dos.writeInt(slot.cooldownRemaining());
                dos.writeInt(slot.maxCooldown());
                dos.writeInt(slot.windupRemaining());
                dos.writeInt(slot.maxWindup());
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode S2CSyncDeckPacket", e);
        }
    }

    public static S2CSyncDeckPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            UUID id = new UUID(dis.readLong(), dis.readLong());
            int cap = dis.readInt();
            int sel = dis.readInt();
            int size = dis.readInt();
            if (size < 0 || size > 64) {
                throw new IllegalArgumentException("Invalid deck sync slot count: " + size);
            }

            List<SlotSyncData> list = new ArrayList<>(Math.min(size, 64));
            for (int i = 0; i < size; i++) {
                int idx = dis.readByte();
                String cid = dis.readUTF();
                if (cid.isEmpty()) cid = null;
                int cdRem = dis.readInt();
                int cdMax = dis.readInt();
                int wuRem = dis.readInt();
                int wuMax = dis.readInt();
                list.add(new SlotSyncData(idx, cid, cdRem, cdMax, wuRem, wuMax));
            }

            return new S2CSyncDeckPacket(id, cap, sel, list);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode S2CSyncDeckPacket", e);
        }
    }
}
