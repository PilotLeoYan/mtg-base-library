package com.mtg.network;

import java.io.*;
import java.util.Objects;

/**
 * Client-to-Server packet requesting spellcasting from a dedicated hand slot.
 */
public record C2SCastCardPacket(
    int slotIndex,
    int targetEntityId,
    double targetX,
    double targetY,
    double targetZ
) implements CustomPacket {
    public static final String CHANNEL = "mtg:cast_card";

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(slotIndex);
            dos.writeInt(targetEntityId);
            dos.writeDouble(targetX);
            dos.writeDouble(targetY);
            dos.writeDouble(targetZ);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode C2SCastCardPacket", e);
        }
    }

    public static C2SCastCardPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            int slot = dis.readByte();
            int targetEntity = dis.readInt();
            double x = dis.readDouble();
            double y = dis.readDouble();
            double z = dis.readDouble();
            return new C2SCastCardPacket(slot, targetEntity, x, y, z);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode C2SCastCardPacket", e);
        }
    }
}
