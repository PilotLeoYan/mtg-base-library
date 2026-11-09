package com.mtg.network;

import java.io.*;
import java.util.Objects;

/**
 * Client-to-Server packet requesting a permanent max mana capacity upgrade using XP levels.
 */
public record C2SUpgradeMaxManaPacket(int offeredXpLevels) implements CustomPacket {
    public static final String CHANNEL = "mtg:upgrade_max_mana";

    public C2SUpgradeMaxManaPacket {
        offeredXpLevels = Math.max(0, offeredXpLevels);
    }

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(offeredXpLevels);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode C2SUpgradeMaxManaPacket", e);
        }
    }

    public static C2SUpgradeMaxManaPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            return new C2SUpgradeMaxManaPacket(dis.readInt());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode C2SUpgradeMaxManaPacket", e);
        }
    }
}
