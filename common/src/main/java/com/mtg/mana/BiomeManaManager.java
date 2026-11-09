package com.mtg.mana;

import com.mtg.config.ManaConfig;
import com.mtg.domain.ManaColor;

import java.util.*;

/**
 * Biome Mana Manager responsible for resolving biome color affinities,
 * scheduling periodic regeneration pulses (default 100 ticks),
 * applying land bonuses, enforcing capacity clamping, and handling natural decay.
 */
public class BiomeManaManager {
    private final ManaConfig config;

    public BiomeManaManager() {
        this(ManaConfig.defaults());
    }

    public BiomeManaManager(ManaConfig config) {
        this.config = config != null ? config : ManaConfig.defaults();
    }

    public ManaConfig getConfig() {
        return config;
    }

    public ManaColor getBiomeAffinityColor(String biomeId) {
        if (biomeId == null) {
            return ManaColor.COLORLESS;
        }
        String colorName = config.getBiomeAffinity(biomeId);
        try {
            return ManaColor.valueOf(colorName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ManaColor.COLORLESS;
        }
    }

    public boolean shouldRegen(int currentTick) {
        if (currentTick <= 0) return false;
        int interval = config.biomeRegenIntervalTicks();
        return interval > 0 && (currentTick % interval == 0);
    }

    public boolean shouldDecay(int currentTick) {
        if (currentTick <= 0) return false;
        int interval = config.decayIntervalTicks();
        return interval > 0 && (currentTick % interval == 0);
    }

    public int tickRegen(ManaPool pool, String biomeId, int landBonus) {
        if (pool == null) return 0;
        ManaColor color = getBiomeAffinityColor(biomeId);
        int amount = config.baseRegenAmount() + Math.max(0, landBonus);
        return pool.addMana(color, amount);
    }

    public int tickDecay(ManaPool pool) {
        if (pool == null) return 0;
        int toDecay = config.decayAmount();
        if (toDecay <= 0 || pool.getTotalMana() == 0) return 0;

        int decayed = 0;
        synchronized (pool) {
            // Step 1: Decay Colorless first
            int cBal = pool.get(ManaColor.COLORLESS);
            if (cBal > 0) {
                int deduct = Math.min(toDecay, cBal);
                pool.removeMana(ManaColor.COLORLESS, deduct);
                decayed += deduct;
                toDecay -= deduct;
            }

            // Step 2: Decay highest colored balances
            while (toDecay > 0 && pool.getTotalMana() > 0) {
                ManaColor highestColor = null;
                int maxBal = 0;
                for (ManaColor c : ManaColor.values()) {
                    int bal = pool.get(c);
                    if (bal > maxBal) {
                        maxBal = bal;
                        highestColor = c;
                    }
                }
                if (highestColor == null || maxBal <= 0) break;

                int deduct = Math.min(toDecay, 1);
                pool.removeMana(highestColor, deduct);
                decayed += deduct;
                toDecay -= deduct;
            }
        }
        return decayed;
    }
}
