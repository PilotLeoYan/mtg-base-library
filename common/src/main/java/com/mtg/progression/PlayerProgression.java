package com.mtg.progression;

import com.mtg.domain.CardRarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe mutable/serializable collection state for a single player.
 * Tracks unlocked card IDs, duplicate counters per rarity, and accumulated wildcard balances.
 */
public final class PlayerProgression {
    private final UUID playerId;
    private final Set<String> unlockedCardIds;
    private final Map<CardRarity, Integer> duplicateCounters;
    private final Map<CardRarity, Integer> wildcardBalances;

    public PlayerProgression(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.unlockedCardIds = ConcurrentHashMap.newKeySet();
        this.duplicateCounters = new ConcurrentHashMap<>();
        this.wildcardBalances = new ConcurrentHashMap<>();

        for (CardRarity rarity : CardRarity.values()) {
            duplicateCounters.put(rarity, 0);
            wildcardBalances.put(rarity, 0);
        }
    }

    public PlayerProgression(UUID playerId, Set<String> unlocked, Map<CardRarity, Integer> dups, Map<CardRarity, Integer> wildcards) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.unlockedCardIds = ConcurrentHashMap.newKeySet();
        if (unlocked != null) {
            this.unlockedCardIds.addAll(unlocked);
        }
        this.duplicateCounters = new ConcurrentHashMap<>();
        this.wildcardBalances = new ConcurrentHashMap<>();

        for (CardRarity rarity : CardRarity.values()) {
            this.duplicateCounters.put(rarity, dups != null ? Math.max(0, dups.getOrDefault(rarity, 0)) : 0);
            this.wildcardBalances.put(rarity, wildcards != null ? Math.max(0, wildcards.getOrDefault(rarity, 0)) : 0);
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean hasCard(String cardId) {
        return cardId != null && unlockedCardIds.contains(cardId);
    }

    public boolean unlockCard(String cardId) {
        if (cardId == null || cardId.isBlank()) return false;
        return unlockedCardIds.add(cardId);
    }

    public Set<String> getUnlockedCards() {
        return Collections.unmodifiableSet(unlockedCardIds);
    }

    public int getDuplicateCount(CardRarity rarity) {
        if (rarity == null) return 0;
        return duplicateCounters.getOrDefault(rarity, 0);
    }

    public void setDuplicateCount(CardRarity rarity, int count) {
        if (rarity == null) return;
        duplicateCounters.put(rarity, Math.max(0, count));
    }

    public int getWildcardCount(CardRarity rarity) {
        if (rarity == null) return 0;
        return wildcardBalances.getOrDefault(rarity, 0);
    }

    public void addWildcards(CardRarity rarity, int amount) {
        if (rarity == null || amount <= 0) return;
        wildcardBalances.merge(rarity, amount, Integer::sum);
    }

    public boolean consumeWildcard(CardRarity rarity, int amount) {
        if (rarity == null || amount < 0) return false;
        if (amount == 0) return true;

        synchronized (wildcardBalances) {
            int current = getWildcardCount(rarity);
            if (current < amount) return false;
            wildcardBalances.put(rarity, current - amount);
            return true;
        }
    }

    public Map<CardRarity, Integer> getDuplicateCounters() {
        return Collections.unmodifiableMap(new EnumMap<>(duplicateCounters));
    }

    public Map<CardRarity, Integer> getWildcardBalances() {
        return Collections.unmodifiableMap(new EnumMap<>(wildcardBalances));
    }
}
