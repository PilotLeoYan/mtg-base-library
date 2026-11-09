package com.mtg.entity;

import com.mtg.config.CardsConfig;
import com.mtg.config.MtgConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central coordinator service for player-aligned creature invocations.
 * Enforces maxActiveSummonsPerPlayer limit with FIFO auto-dismissal,
 * handles lifecycle cleanup on player death/disconnect, and manages active summons.
 */
public class SummonManager {
    private static volatile SummonManager instance;

    private CardsConfig config;
    private final Map<UUID, List<SummonedCreature>> playerSummons = new ConcurrentHashMap<>();
    private final Map<UUID, SummonedCreature> entityLookup = new ConcurrentHashMap<>();

    public static SummonManager getInstance() {
        if (instance == null) {
            synchronized (SummonManager.class) {
                if (instance == null) {
                    instance = new SummonManager();
                }
            }
        }
        return instance;
    }

    public static void setInstance(SummonManager custom) {
        instance = custom;
    }

    public SummonManager() {
        this(MtgConfig.getCards());
    }

    public SummonManager(CardsConfig config) {
        this.config = config != null ? config.sanitize() : CardsConfig.defaults();
    }

    public void setConfig(CardsConfig config) {
        this.config = config != null ? config.sanitize() : CardsConfig.defaults();
    }

    public CardsConfig getConfig() {
        return config != null ? config : MtgConfig.getCards();
    }

    /**
     * Registers a newly summoned creature.
     * Enforces the maxActiveSummonsPerPlayer cap using FIFO auto-dismissal.
     */
    public synchronized boolean registerSummon(SummonedCreature creature) {
        Objects.requireNonNull(creature, "creature cannot be null");
        UUID ownerId = creature.getOwnerUuid();
        List<SummonedCreature> summons = playerSummons.computeIfAbsent(ownerId, k -> new CopyOnWriteArrayList<>());

        // Prune any dead or dismissed summons first
        summons.removeIf(ISummonedCreature::isDead);

        int maxCap = getConfig().maxActiveSummonsPerPlayer();
        // FIFO auto-dismissal of oldest summon
        while (summons.size() >= maxCap && !summons.isEmpty()) {
            SummonedCreature oldest = summons.removeFirst();
            oldest.dismiss();
            entityLookup.remove(oldest.getEntityId());
        }

        summons.add(creature);
        entityLookup.put(creature.getEntityId(), creature);
        return true;
    }

    public int getActiveSummonCount(UUID playerId) {
        if (playerId == null) return 0;
        List<SummonedCreature> list = playerSummons.get(playerId);
        if (list == null) return 0;
        list.removeIf(ISummonedCreature::isDead);
        return list.size();
    }

    public List<SummonedCreature> getPlayerSummons(UUID playerId) {
        if (playerId == null) return Collections.emptyList();
        List<SummonedCreature> list = playerSummons.get(playerId);
        if (list == null) return Collections.emptyList();
        list.removeIf(ISummonedCreature::isDead);
        return Collections.unmodifiableList(list);
    }

    public Optional<SummonedCreature> getSummon(UUID entityId) {
        if (entityId == null) return Optional.empty();
        return Optional.ofNullable(entityLookup.get(entityId));
    }

    public void onPlayerDeath(UUID playerId) {
        if (playerId == null) return;
        List<SummonedCreature> list = playerSummons.remove(playerId);
        if (list != null) {
            for (SummonedCreature c : list) {
                c.dismiss();
                entityLookup.remove(c.getEntityId());
            }
        }
    }

    public void onPlayerDisconnect(UUID playerId) {
        onPlayerDeath(playerId);
    }

    public void untapAll(UUID playerId) {
        if (playerId == null) return;
        List<SummonedCreature> list = playerSummons.get(playerId);
        if (list != null) {
            for (SummonedCreature c : list) {
                if (!c.isDead()) {
                    c.untap();
                    c.clearSummoningSickness();
                }
            }
        }
    }

    public void tick() {
        for (Iterator<Map.Entry<UUID, SummonedCreature>> it = entityLookup.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, SummonedCreature> entry = it.next();
            SummonedCreature creature = entry.getValue();
            if (creature.isDead()) {
                it.remove();
                List<SummonedCreature> list = playerSummons.get(creature.getOwnerUuid());
                if (list != null) {
                    list.remove(creature);
                }
            } else {
                creature.tick();
            }
        }
    }

    public void clear() {
        for (SummonedCreature c : entityLookup.values()) {
            c.dismiss();
        }
        playerSummons.clear();
        entityLookup.clear();
    }
}
