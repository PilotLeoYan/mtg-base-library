package com.mtg.network;

import java.util.Objects;

/**
 * Server-to-Client packet prompting a first-world-join player to select their initial mana pool affinity.
 */
public record S2COpenManaPromptPacket() implements CustomPacket {
    public static final String CHANNEL = "mtg:open_mana_prompt";

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    public static S2COpenManaPromptPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        return new S2COpenManaPromptPacket();
    }
}
