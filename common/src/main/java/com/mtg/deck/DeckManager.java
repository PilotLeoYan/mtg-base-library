package com.mtg.deck;

import com.mtg.config.MtgConfig;
import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.progression.ProgressionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level coordination service for dedicated Planeswalker Decks and hands,
 * implementing interface contracts in PROJECT.md.
 */
public final class DeckManager {
    private static final DeckManager INSTANCE = new DeckManager();
    private final Map<UUID, PlaneswalkerDeck> playerDecks = new ConcurrentHashMap<>();
    private ProgressionConfig config;

    public static DeckManager getInstance() {
        return INSTANCE;
    }

    public DeckManager() {
        this(MtgConfig.getProgression());
    }

    public DeckManager(ProgressionConfig config) {
        this.config = config != null ? config : ProgressionConfig.defaults();
    }

    public void setConfig(ProgressionConfig config) {
        this.config = config != null ? config : ProgressionConfig.defaults();
    }

    public ProgressionConfig getConfig() {
        return config != null ? config : MtgConfig.getProgression();
    }

    public PlaneswalkerDeck getDeck(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return playerDecks.computeIfAbsent(playerId, id -> new PlaneswalkerDeck(id, getConfig().initialHandCapacity()));
    }

    public List<CardDefinition> getHandCards(UUID playerId) {
        PlaneswalkerDeck deck = getDeck(playerId);
        List<CardDefinition> cards = new ArrayList<>();
        ProgressionManager prog = ProgressionManager.getInstance();
        for (DeckCardSlot slot : deck.getUnlockedSlots()) {
            if (slot.hasCard()) {
                prog.getCard(slot.getCardId()).ifPresent(cards::add);
            }
        }
        return Collections.unmodifiableList(cards);
    }

    public boolean unlockCard(UUID playerId, String cardId) {
        return ProgressionManager.getInstance().getProgression(playerId).unlockCard(cardId);
    }

    public int getDuplicateCount(UUID playerId, CardRarity rarity) {
        return ProgressionManager.getInstance().getDuplicateCount(playerId, rarity);
    }

    public int getWildcards(UUID playerId, CardRarity rarity) {
        return ProgressionManager.getInstance().getWildcardCount(playerId, rarity);
    }

    public boolean redeemWildcard(UUID playerId, CardRarity rarity, String targetCardId) {
        return ProgressionManager.getInstance().redeemWildcard(playerId, rarity, targetCardId).isSuccess();
    }

    public boolean upgradeHandCapacity(UUID playerId, int availableXpLevels) {
        PlaneswalkerDeck deck = getDeck(playerId);
        synchronized (deck) {
            int cost = DeckCapacityUpgradeCalculator.calculateUpgradeCost(deck.getCapacity(), getConfig());
            if (cost <= 0 || availableXpLevels < cost) {
                return false;
            }
            return deck.upgradeCapacity();
        }
    }

    public void selectSlot(UUID playerId, int slotIndex) {
        getDeck(playerId).setSelectedSlot(slotIndex);
    }

    public void cycleSlot(UUID playerId, boolean forward) {
        getDeck(playerId).cycleSlot(forward);
    }

    public boolean equipCard(UUID playerId, int slotIndex, String cardId, int baseCooldown, int windup) {
        return getDeck(playerId).equipCard(slotIndex, cardId, baseCooldown, windup);
    }

    public void tickAll() {
        for (PlaneswalkerDeck deck : playerDecks.values()) {
            deck.tick();
        }
    }

    public void clear() {
        playerDecks.clear();
    }
}
