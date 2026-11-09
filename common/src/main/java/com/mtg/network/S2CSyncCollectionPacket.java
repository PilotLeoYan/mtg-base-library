package com.mtg.network;

import com.mtg.domain.CardRarity;

import java.io.*;
import java.util.*;

/**
 * Server-to-Client packet synchronizing the player's full card collection,
 * duplicate counters, and wildcard balances.
 */
public record S2CSyncCollectionPacket(
    UUID playerId,
    Set<String> unlockedCardIds,
    Map<CardRarity, Integer> duplicateCounters,
    Map<CardRarity, Integer> wildcardBalances
) implements CustomPacket {
    public static final String CHANNEL = "mtg:sync_collection";

    public S2CSyncCollectionPacket {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        unlockedCardIds = (unlockedCardIds != null) ? Set.copyOf(unlockedCardIds) : Set.of();
        duplicateCounters = (duplicateCounters != null) ? Map.copyOf(duplicateCounters) : Map.of();
        wildcardBalances = (wildcardBalances != null) ? Map.copyOf(wildcardBalances) : Map.of();
    }

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeLong(playerId.getMostSignificantBits());
            dos.writeLong(playerId.getLeastSignificantBits());

            dos.writeInt(unlockedCardIds.size());
            for (String cardId : unlockedCardIds) {
                dos.writeUTF(cardId != null ? cardId : "");
            }

            CardRarity[] rarities = CardRarity.values();
            dos.writeByte(rarities.length);
            for (CardRarity r : rarities) {
                dos.writeByte(r.ordinal());
                dos.writeInt(duplicateCounters.getOrDefault(r, 0));
                dos.writeInt(wildcardBalances.getOrDefault(r, 0));
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode S2CSyncCollectionPacket", e);
        }
    }

    public static S2CSyncCollectionPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Packet data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
            UUID id = new UUID(dis.readLong(), dis.readLong());

            int cardCount = dis.readInt();
            if (cardCount < 0 || cardCount > 10_000) {
                throw new IllegalArgumentException("Invalid collection card count: " + cardCount);
            }
            Set<String> cards = new HashSet<>(Math.min(cardCount, 1024));
            for (int i = 0; i < cardCount; i++) {
                cards.add(dis.readUTF());
            }

            int rarityCount = dis.readByte();
            if (rarityCount < 0 || rarityCount > 16) {
                throw new IllegalArgumentException("Invalid rarity count: " + rarityCount);
            }
            Map<CardRarity, Integer> dups = new EnumMap<>(CardRarity.class);
            Map<CardRarity, Integer> wildcards = new EnumMap<>(CardRarity.class);
            CardRarity[] allRarities = CardRarity.values();

            for (int i = 0; i < rarityCount; i++) {
                int ord = dis.readByte();
                int dupVal = dis.readInt();
                int wcVal = dis.readInt();
                if (ord >= 0 && ord < allRarities.length) {
                    CardRarity r = allRarities[ord];
                    dups.put(r, dupVal);
                    wildcards.put(r, wcVal);
                }
            }

            return new S2CSyncCollectionPacket(id, cards, dups, wildcards);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode S2CSyncCollectionPacket", e);
        }
    }
}
