package com.mtg.network;

import com.mtg.mana.ManaPool;

import java.util.Objects;

/**
 * Server-to-Client packet synchronizing the player's full ManaPool state.
 */
public record S2CSyncManaPacket(ManaPool pool) implements CustomPacket {
    public static final String CHANNEL = "mtg:sync_mana";

    public S2CSyncManaPacket {
        Objects.requireNonNull(pool, "ManaPool cannot be null");
    }

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        return pool.toBytes();
    }

    public static S2CSyncManaPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        return new S2CSyncManaPacket(ManaPool.fromBytes(data));
    }
}
