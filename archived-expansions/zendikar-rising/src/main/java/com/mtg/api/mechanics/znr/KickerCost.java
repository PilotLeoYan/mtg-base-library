package com.mtg.api.mechanics.znr;

import com.mtg.domain.ManaCost;
import java.util.Objects;

public record KickerCost(ManaCost manaCost, boolean isMultikicker, int maxKickerCount) {
    public KickerCost {
        Objects.requireNonNull(manaCost, "Kicker mana cost cannot be null");
        if (maxKickerCount < 1) {
            throw new IllegalArgumentException("Max kicker count must be at least 1");
        }
    }

    public static KickerCost simple(ManaCost manaCost) {
        return new KickerCost(manaCost, false, 1);
    }

    public static KickerCost multikicker(ManaCost manaCost) {
        return new KickerCost(manaCost, true, Integer.MAX_VALUE);
    }
}
