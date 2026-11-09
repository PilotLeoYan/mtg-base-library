package com.mtg.network;

import java.io.*;
import java.util.Objects;

/**
 * Client-to-Server packet communicating selection of an active card slot (0..capacity-1).
 */
public record C2SSelectCardPacket(int slotIndex) implements CustomPacket {
    public static final String CHANNEL = "mtg:select_card";

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(slotIndex);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode C2SSelectCardPacket", e);
        }
    }

    public static C2SSelectCardPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            return new C2SSelectCardPacket(dis.readByte());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode C2SSelectCardPacket", e);
        }
    }
}
