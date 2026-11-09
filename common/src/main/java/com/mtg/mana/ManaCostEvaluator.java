package com.mtg.mana;

import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;

import java.util.*;

/**
 * Pure stateless MTG mana cost evaluation and atomic deduction engine.
 * Solves colored requirements, evaluates generic surplus, and prioritizes
 * greedy deduction (Colorless first, then highest balance with deterministic tie-breaking).
 */
public final class ManaCostEvaluator {

    private ManaCostEvaluator() {}

    public static boolean canAfford(ManaPool pool, ManaCost cost) {
        return canAfford(pool, cost, false);
    }

    public static boolean canAfford(ManaPool pool, ManaCost cost, boolean payPhyrexianWithLife) {
        if (cost == null || cost.totalManaValue() == 0) {
            return true;
        }
        if (pool == null) {
            return false;
        }

        int reqW = cost.white() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().whitePhyrexian() : 0);
        int reqU = cost.blue() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().bluePhyrexian() : 0);
        int reqB = cost.black() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().blackPhyrexian() : 0);
        int reqR = cost.red() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().redPhyrexian() : 0);
        int reqG = cost.green() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().greenPhyrexian() : 0);
        int reqC = cost.colorless() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().colorlessPhyrexian() : 0);

        int poolW = pool.get(ManaColor.WHITE);
        if (poolW < reqW) return false;
        int poolU = pool.get(ManaColor.BLUE);
        if (poolU < reqU) return false;
        int poolB = pool.get(ManaColor.BLACK);
        if (poolB < reqB) return false;
        int poolR = pool.get(ManaColor.RED);
        if (poolR < reqR) return false;
        int poolG = pool.get(ManaColor.GREEN);
        if (poolG < reqG) return false;
        int poolC = pool.get(ManaColor.COLORLESS);
        if (poolC < reqC) return false;

        if (cost.generic() == 0) {
            return true;
        }

        int surplus = (poolW - reqW)
                + (poolU - reqU)
                + (poolB - reqB)
                + (poolR - reqR)
                + (poolG - reqG)
                + (poolC - reqC);

        return surplus >= cost.generic();
    }

    public static boolean deductCost(ManaPool pool, ManaCost cost) {
        return deductCost(pool, cost, false);
    }

    public static boolean deductCost(ManaPool pool, ManaCost cost, boolean payPhyrexianWithLife) {
        if (cost == null || cost.totalManaValue() == 0) {
            return true;
        }
        if (pool == null) {
            return false;
        }

        synchronized (pool) {
            int reqW = cost.white() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().whitePhyrexian() : 0);
            int reqU = cost.blue() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().bluePhyrexian() : 0);
            int reqB = cost.black() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().blackPhyrexian() : 0);
            int reqR = cost.red() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().redPhyrexian() : 0);
            int reqG = cost.green() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().greenPhyrexian() : 0);
            int reqC = cost.colorless() + (!payPhyrexianWithLife && cost.phyrexian() != null ? cost.phyrexian().colorlessPhyrexian() : 0);

            int poolW = pool.get(ManaColor.WHITE);
            int poolU = pool.get(ManaColor.BLUE);
            int poolB = pool.get(ManaColor.BLACK);
            int poolR = pool.get(ManaColor.RED);
            int poolG = pool.get(ManaColor.GREEN);
            int poolC = pool.get(ManaColor.COLORLESS);

            // Step 1: Pre-validation of exact colored requirements
            if (poolW < reqW || poolU < reqU || poolB < reqB || poolR < reqR || poolG < reqG || poolC < reqC) {
                return false;
            }

            int remW = poolW - reqW;
            int remU = poolU - reqU;
            int remB = poolB - reqB;
            int remR = poolR - reqR;
            int remG = poolG - reqG;
            int remC = poolC - reqC;

            long totalSurplus = (long) remW + remU + remB + remR + remG + remC;
            int genericNeeded = cost.generic();
            if (genericNeeded > 0 && totalSurplus < genericNeeded) {
                return false;
            }

            // Step 2: Calculate complete deduction plan without mutating the pool
            int deductW = reqW;
            int deductU = reqU;
            int deductB = reqB;
            int deductR = reqR;
            int deductG = reqG;
            int deductC = reqC;

            if (genericNeeded > 0) {
                // Priority 1: Colorless first
                int fromC = Math.min(genericNeeded, remC);
                deductC += fromC;
                remC -= fromC;
                genericNeeded -= fromC;

                // Priority 2: Highest balance among colored mana
                // Tie-breaker priority: GREEN -> RED -> BLACK -> BLUE -> WHITE
                if (genericNeeded > 0) {
                    List<ManaColorEntry> entries = new ArrayList<>();
                    entries.add(new ManaColorEntry(ManaColor.GREEN, remG, 0));
                    entries.add(new ManaColorEntry(ManaColor.RED, remR, 1));
                    entries.add(new ManaColorEntry(ManaColor.BLACK, remB, 2));
                    entries.add(new ManaColorEntry(ManaColor.BLUE, remU, 3));
                    entries.add(new ManaColorEntry(ManaColor.WHITE, remW, 4));

                    while (genericNeeded > 0) {
                        entries.sort((a, b) -> {
                            int cmp = Integer.compare(b.balance, a.balance);
                            if (cmp != 0) return cmp;
                            return Integer.compare(a.tiePriority, b.tiePriority);
                        });

                        ManaColorEntry top = entries.get(0);
                        if (top.balance <= 0) break;

                        int deduct;
                        if (entries.size() > 1 && entries.get(1).balance > 0) {
                            deduct = Math.min(genericNeeded, Math.max(1, top.balance - entries.get(1).balance));
                        } else {
                            deduct = Math.min(genericNeeded, top.balance);
                        }

                        top.balance -= deduct;
                        top.genericDeducted += deduct;
                        genericNeeded -= deduct;
                    }

                    if (genericNeeded > 0) {
                        return false;
                    }

                    for (ManaColorEntry entry : entries) {
                        switch (entry.color) {
                            case GREEN -> deductG += entry.genericDeducted;
                            case RED -> deductR += entry.genericDeducted;
                            case BLACK -> deductB += entry.genericDeducted;
                            case BLUE -> deductU += entry.genericDeducted;
                            case WHITE -> deductW += entry.genericDeducted;
                            default -> {}
                        }
                    }
                }
            }

            // Step 3: Apply verified deduction plan transactionally with full rollback
            EnumMap<ManaColor, Integer> plannedRemovals = new EnumMap<>(ManaColor.class);
            if (deductW > 0) plannedRemovals.put(ManaColor.WHITE, deductW);
            if (deductU > 0) plannedRemovals.put(ManaColor.BLUE, deductU);
            if (deductB > 0) plannedRemovals.put(ManaColor.BLACK, deductB);
            if (deductR > 0) plannedRemovals.put(ManaColor.RED, deductR);
            if (deductG > 0) plannedRemovals.put(ManaColor.GREEN, deductG);
            if (deductC > 0) plannedRemovals.put(ManaColor.COLORLESS, deductC);

            EnumMap<ManaColor, Integer> appliedRemovals = new EnumMap<>(ManaColor.class);
            boolean allRemoved = true;
            for (Map.Entry<ManaColor, Integer> entry : plannedRemovals.entrySet()) {
                if (!pool.removeMana(entry.getKey(), entry.getValue())) {
                    allRemoved = false;
                    break;
                }
                appliedRemovals.put(entry.getKey(), entry.getValue());
            }

            if (!allRemoved) {
                // Rollback all applied removals to guarantee zero partial drains
                for (Map.Entry<ManaColor, Integer> entry : appliedRemovals.entrySet()) {
                    pool.addMana(entry.getKey(), entry.getValue());
                }
                return false;
            }

            return true;
        }
    }

    private static final class ManaColorEntry {
        final ManaColor color;
        int balance;
        final int tiePriority;
        int genericDeducted;

        ManaColorEntry(ManaColor color, int balance, int tiePriority) {
            this.color = color;
            this.balance = balance;
            this.tiePriority = tiePriority;
            this.genericDeducted = 0;
        }
    }
}
