package com.mtg.network;

import com.mtg.domain.CardRarity;

import java.io.*;
import java.util.Objects;

/**
 * Client-to-Server packet requesting redemption of a wildcard for a specific card of matching rarity.
 */
public record C2SRedeemWildcardPacket(
    CardRarity rarity,
    String targetCardId
) implements CustomPacket {
    public static final String CHANNEL = "mtg:redeem_wildcard";

    public C2SRedeemWildcardPacket {
        if (rarity == null) {
            rarity = CardRarity.COMMON;
        }
        if (targetCardId == null) {
            targetCardId = "";
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
            dos.writeByte(rarity.ordinal());
            dos.writeUTF(targetCardId);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode C2SRedeemWildcardPacket", e);
        }
    }

    public static C2SRedeemWildcardPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            byte ord = dis.readByte();
            CardRarity r = (ord >= 0 && ord < CardRarity.values().length) ? CardRarity.values()[ord] : CardRarity.COMMON;
            String cardId = dis.readUTF();
            return new C2SRedeemWildcardPacket(r, cardId);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode C2SRedeemWildcardPacket", e);
        }
    }
}
