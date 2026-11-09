package com.mtg.domain;

public record PhyrexianManaCost(
    int whitePhyrexian,
    int bluePhyrexian,
    int blackPhyrexian,
    int redPhyrexian,
    int greenPhyrexian,
    int colorlessPhyrexian
) {
    public static final PhyrexianManaCost NONE = new PhyrexianManaCost(0, 0, 0, 0, 0, 0);

    public PhyrexianManaCost {
        if (whitePhyrexian < 0 || bluePhyrexian < 0 || blackPhyrexian < 0 ||
            redPhyrexian < 0 || greenPhyrexian < 0 || colorlessPhyrexian < 0) {
            throw new IllegalArgumentException("Phyrexian mana values cannot be negative");
        }
    }

    public static PhyrexianManaCost ofWhite(int amount) {
        return new PhyrexianManaCost(amount, 0, 0, 0, 0, 0);
    }

    public static PhyrexianManaCost ofBlue(int amount) {
        return new PhyrexianManaCost(0, amount, 0, 0, 0, 0);
    }

    public static PhyrexianManaCost ofBlack(int amount) {
        return new PhyrexianManaCost(0, 0, amount, 0, 0, 0);
    }

    public static PhyrexianManaCost ofRed(int amount) {
        return new PhyrexianManaCost(0, 0, 0, amount, 0, 0);
    }

    public static PhyrexianManaCost ofGreen(int amount) {
        return new PhyrexianManaCost(0, 0, 0, 0, amount, 0);
    }

    public int totalPhyrexianMana() {
        return whitePhyrexian + bluePhyrexian + blackPhyrexian + redPhyrexian + greenPhyrexian + colorlessPhyrexian;
    }

    public int totalLifeCost() {
        return totalPhyrexianMana() * 2;
    }

    public float calculateDamage() {
        return totalPhyrexianMana() * 2.0f; // 2 damage (1 full heart) per Phyrexian mana
    }

    public float calculateDamage(float damagePerMana) {
        return totalPhyrexianMana() * damagePerMana;
    }

    public boolean canPayWithLife(float currentHealth) {
        return currentHealth > calculateDamage();
    }

    public boolean canPayWithLife(float currentHealth, float damagePerMana) {
        return currentHealth > calculateDamage(damagePerMana);
    }
}
