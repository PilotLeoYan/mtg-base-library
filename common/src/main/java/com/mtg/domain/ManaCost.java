package com.mtg.domain;

import java.util.Objects;

/**
 * Pure MTG domain model representing mana costs (Generic, WUBRG, Colorless, Phyrexian).
 */
public record ManaCost(
    int generic,
    int white,
    int blue,
    int black,
    int red,
    int green,
    int colorless,
    PhyrexianManaCost phyrexian
) {
    public static final ManaCost ZERO = new ManaCost(0, 0, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE);

    // Backward-compatible constructor for M1 tests and code
    public ManaCost(int white, int blue, int black, int red, int green, int colorless) {
        this(0, white, blue, black, red, green, colorless, PhyrexianManaCost.NONE);
    }

    public ManaCost {
        if (generic < 0 || white < 0 || blue < 0 || black < 0 || red < 0 || green < 0 || colorless < 0) {
            throw new IllegalArgumentException("Mana cost values cannot be negative");
        }
        if (phyrexian == null) {
            phyrexian = PhyrexianManaCost.NONE;
        }
    }

    public static ManaCost ofGeneric(int generic) {
        return new ManaCost(generic, 0, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofWhite(int white) {
        return new ManaCost(0, white, 0, 0, 0, 0, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofBlue(int blue) {
        return new ManaCost(0, 0, blue, 0, 0, 0, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofBlack(int black) {
        return new ManaCost(0, 0, 0, black, 0, 0, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofRed(int red) {
        return new ManaCost(0, 0, 0, 0, red, 0, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofGreen(int green) {
        return new ManaCost(0, 0, 0, 0, 0, green, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofColored(int white, int blue, int black, int red, int green) {
        return new ManaCost(0, white, blue, black, red, green, 0, PhyrexianManaCost.NONE);
    }

    public static ManaCost ofPhyrexian(PhyrexianManaCost phyrexian) {
        return new ManaCost(0, 0, 0, 0, 0, 0, 0, phyrexian);
    }

    public int totalManaValue() {
        int phyrexianCount = (phyrexian != null) ? phyrexian.totalPhyrexianMana() : 0;
        return generic + white + blue + black + red + green + colorless + phyrexianCount;
    }

    public boolean hasPhyrexian() {
        return phyrexian != null && phyrexian.totalPhyrexianMana() > 0;
    }

    public boolean isColorless() {
        return white == 0 && blue == 0 && black == 0 && red == 0 && green == 0 && !hasPhyrexian();
    }

    public boolean isMonocolored() {
        int colors = 0;
        if (white > 0 || (phyrexian != null && phyrexian.whitePhyrexian() > 0)) colors++;
        if (blue > 0 || (phyrexian != null && phyrexian.bluePhyrexian() > 0)) colors++;
        if (black > 0 || (phyrexian != null && phyrexian.blackPhyrexian() > 0)) colors++;
        if (red > 0 || (phyrexian != null && phyrexian.redPhyrexian() > 0)) colors++;
        if (green > 0 || (phyrexian != null && phyrexian.greenPhyrexian() > 0)) colors++;
        return colors == 1;
    }
}
