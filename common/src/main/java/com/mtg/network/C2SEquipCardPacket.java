package com.mtg.network;

import java.io.*;
import java.util.Objects;

/**
 * Client-to-Server packet requesting equipping or swapping a card into a dedicated hand slot.
 */
public record C2SEquipCardPacket(int slotIndex, String cardId) implements CustomPacket {
    public static final String CHANNEL = "mtg:equip_card";

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(slotIndex);
            dos.writeUTF(cardId != null ? cardId : "");
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode C2SEquipCardPacket", e);
        }
    }

    public static C2SEquipCardPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            int slot = dis.readByte();
            String cid = dis.readUTF();
            return new C2SEquipCardPacket(slot, cid.isEmpty() ? null : cid);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode C2SEquipCardPacket", e);
        }
    }
}
