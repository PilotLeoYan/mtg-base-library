package com.mtg.api.mechanics.znr;

import com.mtg.domain.ManaCost;

public interface IKickerable {
    boolean hasKicker();
    KickerCost getKickerCost();

    default boolean isKicked(int timesKicked) {
        return timesKicked > 0;
    }

    default ManaCost calculateTotalCost(ManaCost baseCost, int timesKicked) {
        if (timesKicked <= 0 || !hasKicker()) {
            return baseCost;
        }
        KickerCost kicker = getKickerCost();
        int count = Math.min(timesKicked, kicker.maxKickerCount());
        return new ManaCost(
            baseCost.generic() + (kicker.manaCost().generic() * count),
            baseCost.white() + (kicker.manaCost().white() * count),
            baseCost.blue() + (kicker.manaCost().blue() * count),
            baseCost.black() + (kicker.manaCost().black() * count),
            baseCost.red() + (kicker.manaCost().red() * count),
            baseCost.green() + (kicker.manaCost().green() * count),
            baseCost.colorless() + (kicker.manaCost().colorless() * count),
            baseCost.phyrexian()
        );
    }
}
