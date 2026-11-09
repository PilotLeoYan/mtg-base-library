package com.mtg.mana;

import com.mtg.config.ManaConfig;
import com.mtg.config.MtgConfig;
import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central runtime coordinator service for player ManaPools.
 * Coordinates initial affinity selection, biome regeneration, casting verification,
 * and XP capacity upgrades according to PROJECT.md interface contracts.
 */
public class ManaManager {
    private static volatile ManaManager instance;

    private ManaConfig config;
    private BiomeManaManager biomeManaManager;
    private final Map<UUID, ManaPool> pools = new ConcurrentHashMap<>();

    public static ManaManager getInstance() {
        if (instance == null) {
            synchronized (ManaManager.class) {
                if (instance == null) {
                    instance = new ManaManager();
                }
            }
        }
        return instance;
    }

    public static void setInstance(ManaManager customInstance) {
        instance = customInstance;
    }

    public ManaManager() {
        this(MtgConfig.getMana());
    }

    public ManaManager(ManaConfig config) {
        this.config = config != null ? config : ManaConfig.defaults();
        this.biomeManaManager = new BiomeManaManager(this.config);
    }

    public void setConfig(ManaConfig config) {
        this.config = config != null ? config : ManaConfig.defaults();
        this.biomeManaManager = new BiomeManaManager(this.config);
    }

    public ManaConfig getConfig() {
        return config;
    }

    public BiomeManaManager getBiomeManaManager() {
        return biomeManaManager;
    }

    public ManaPool getPlayerMana(UUID playerId) {
        return getOrCreatePlayerMana(playerId);
    }

    public ManaPool getOrCreatePlayerMana(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return pools.computeIfAbsent(playerId, id -> new ManaPool(id, config.initialMaxMana()));
    }

    public boolean hasChosenInitialMana(UUID playerId) {
        return getOrCreatePlayerMana(playerId).hasChosenInitialAffinity();
    }

    public boolean selectInitialMana(UUID playerId, ManaColor color) {
        if (color == null) return false;
        ManaPool pool = getOrCreatePlayerMana(playerId);
        synchronized (pool) {
            if (pool.hasChosenInitialAffinity()) {
                return false; // Prevent re-selection / exploit
            }
            int seedMana = pool.getMaxMana();
            pool.setInitialAffinity(color, seedMana);
            return true;
        }
    }

    public boolean canCast(UUID playerId, ManaCost cost) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        return pool.canPay(cost);
    }

    public boolean payCost(UUID playerId, ManaCost cost) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        return pool.payCost(cost);
    }

    public void addMana(UUID playerId, ManaColor color, int amount) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        pool.addMana(color, amount);
    }

    public void tickBiomeRegeneration(UUID playerId, String biomeId) {
        tickBiomeRegeneration(playerId, biomeId, 0);
    }

    public void tickBiomeRegeneration(UUID playerId, Object biomeId) {
        if (biomeId != null) {
            tickBiomeRegeneration(playerId, biomeId.toString(), 0);
        }
    }

    public void tickBiomeRegeneration(UUID playerId, String biomeId, int landBonus) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        biomeManaManager.tickRegen(pool, biomeId, landBonus);
    }

    public void tick(UUID playerId, String biomeId, int currentTick) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        if (biomeManaManager.shouldRegen(currentTick)) {
            biomeManaManager.tickRegen(pool, biomeId, 0);
        }
        if (biomeManaManager.shouldDecay(currentTick)) {
            biomeManaManager.tickDecay(pool);
        }
    }

    public int getNextUpgradeCost(UUID playerId) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        return XpUpgradeCalculator.getNextUpgradeCost(pool.getMaxMana(), config);
    }

    public boolean upgradeMaxMana(UUID playerId, int availableXpLevels) {
        ManaPool pool = getOrCreatePlayerMana(playerId);
        synchronized (pool) {
            int cost = XpUpgradeCalculator.getNextUpgradeCost(pool.getMaxMana(), config);
            if (!XpUpgradeCalculator.canUpgrade(pool.getMaxMana(), config.hardCapMaxMana(), availableXpLevels, cost)) {
                return false;
            }
            int newMax = XpUpgradeCalculator.calculateNewMaxMana(
                    pool.getMaxMana(),
                    config.hardCapMaxMana(),
                    XpUpgradeCalculator.DEFAULT_UPGRADE_STEP
            );
            pool.setMaxMana(newMax);
            return true;
        }
    }

    public void reset() {
        pools.clear();
    }
}
