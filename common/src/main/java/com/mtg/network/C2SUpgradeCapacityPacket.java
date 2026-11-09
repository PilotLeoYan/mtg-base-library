package com.mtg.network;

import java.util.Objects;

/**
 * Client-to-Server packet requesting dedicated hand capacity upgrade using XP levels.
 */
public record C2SUpgradeCapacityPacket() implements CustomPacket {
    public static final String CHANNEL = "mtg:upgrade_capacity";

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        return new byte[] { 0 };
    }

    public static C2SUpgradeCapacityPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        return new C2SUpgradeCapacityPacket();
    }
}
