package com.mtg.network;

import com.mtg.domain.CardRarity;
import com.mtg.progression.CardUnlockSource;

import java.io.*;
import java.util.Objects;

/**
 * Server-to-Client packet notifying of a new card unlock or duplicate transmutation progress.
 */
public record S2CUnlockCardPacket(
    String cardId,
    CardRarity rarity,
    CardUnlockSource source,
    boolean isDuplicate,
    int currentDuplicates,
    int threshold,
    boolean wildcardAwarded,
    CardRarity wildcardRarity,
    int totalWildcardsOfRarity
) implements CustomPacket {
    public static final String CHANNEL = "mtg:unlock_card";

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeUTF(cardId != null ? cardId : "");
            dos.writeByte(rarity != null ? rarity.ordinal() : 0);
            dos.writeByte(source != null ? source.ordinal() : 0);
            dos.writeBoolean(isDuplicate);
            dos.writeInt(currentDuplicates);
            dos.writeInt(threshold);
            dos.writeBoolean(wildcardAwarded);
            dos.writeByte(wildcardRarity != null ? wildcardRarity.ordinal() : -1);
            dos.writeInt(totalWildcardsOfRarity);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode S2CUnlockCardPacket", e);
        }
    }

    public static S2CUnlockCardPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            String cardId = dis.readUTF();
            byte rarityByte = dis.readByte();
            CardRarity rarity = (rarityByte >= 0 && rarityByte < CardRarity.values().length)
                    ? CardRarity.values()[rarityByte] : CardRarity.COMMON;

            byte srcByte = dis.readByte();
            CardUnlockSource src = (srcByte >= 0 && srcByte < CardUnlockSource.values().length)
                    ? CardUnlockSource.values()[srcByte] : CardUnlockSource.COMMAND;

            boolean isDup = dis.readBoolean();
            int curDups = dis.readInt();
            int thresh = dis.readInt();
            boolean wcAwarded = dis.readBoolean();

            byte wcRarityByte = dis.readByte();
            CardRarity wcRarity = (wcRarityByte >= 0 && wcRarityByte < CardRarity.values().length)
                    ? CardRarity.values()[wcRarityByte] : null;

            int totalWc = dis.readInt();

            return new S2CUnlockCardPacket(cardId, rarity, src, isDup, curDups, thresh, wcAwarded, wcRarity, totalWc);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode S2CUnlockCardPacket", e);
        }
    }
}
