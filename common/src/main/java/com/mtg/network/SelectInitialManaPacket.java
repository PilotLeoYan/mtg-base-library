package com.mtg.network;

import com.mtg.domain.ManaColor;

import java.io.*;
import java.util.Objects;

/**
 * Client-to-Server packet communicating the player's initial mana affinity choice.
 */
public record SelectInitialManaPacket(ManaColor selectedColor) implements CustomPacket {
    public static final String CHANNEL = "mtg:select_initial_mana";

    public SelectInitialManaPacket {
        if (selectedColor == null) {
            selectedColor = ManaColor.COLORLESS;
        }
    }

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(selectedColor.ordinal());
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode SelectInitialManaPacket", e);
        }
    }

    public static SelectInitialManaPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            byte ord = dis.readByte();
            ManaColor color = (ord >= 0 && ord < ManaColor.values().length)
                    ? ManaColor.values()[ord] : ManaColor.COLORLESS;
            return new SelectInitialManaPacket(color);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode SelectInitialManaPacket", e);
        }
    }
}
