package com.mtg.mana;

import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Pure domain state representation of a player's Mana Pool.
 * Holds colored balances (WUBRGC), max mana capacity, and starting affinity selection.
 * Enforces the invariant: totalMana <= maxMana at all times.
 */
public class ManaPool {
    public static final int DEFAULT_MAX_MANA = 20;

    private final UUID playerId;
    private int white;
    private int blue;
    private int black;
    private int red;
    private int green;
    private int colorless;
    private int maxMana;
    private boolean hasChosenInitialAffinity;
    private ManaColor chosenAffinity;

    public ManaPool(UUID playerId) {
        this(playerId, DEFAULT_MAX_MANA);
    }

    public ManaPool(UUID playerId, int maxMana) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.maxMana = Math.max(1, maxMana);
        this.white = 0;
        this.blue = 0;
        this.black = 0;
        this.red = 0;
        this.green = 0;
        this.colorless = 0;
        this.hasChosenInitialAffinity = false;
        this.chosenAffinity = null;
    }

    public ManaPool(UUID playerId, int white, int blue, int black, int red, int green, int colorless,
                    int maxMana, boolean hasChosenInitialAffinity, ManaColor chosenAffinity) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.maxMana = Math.max(1, maxMana);
        this.white = Math.min(this.maxMana, Math.max(0, white));
        this.blue = Math.min(this.maxMana, Math.max(0, blue));
        this.black = Math.min(this.maxMana, Math.max(0, black));
        this.red = Math.min(this.maxMana, Math.max(0, red));
        this.green = Math.min(this.maxMana, Math.max(0, green));
        this.colorless = Math.min(this.maxMana, Math.max(0, colorless));
        this.hasChosenInitialAffinity = hasChosenInitialAffinity;
        this.chosenAffinity = chosenAffinity;
        clampToMax();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public synchronized int get(ManaColor color) {
        if (color == null) return 0;
        return switch (color) {
            case WHITE -> white;
            case BLUE -> blue;
            case BLACK -> black;
            case RED -> red;
            case GREEN -> green;
            case COLORLESS -> colorless;
        };
    }

    public synchronized void set(ManaColor color, int amount) {
        if (color == null) return;
        int clampedAmount = Math.max(0, amount);
        int current = get(color);
        int otherMana = getTotalMana() - current;
        int availableCapacity = Math.max(0, maxMana - otherMana);
        int finalAmount = Math.min(clampedAmount, availableCapacity);

        setDirect(color, finalAmount);
    }

    private void setDirect(ManaColor color, int amount) {
        switch (color) {
            case WHITE -> white = amount;
            case BLUE -> blue = amount;
            case BLACK -> black = amount;
            case RED -> red = amount;
            case GREEN -> green = amount;
            case COLORLESS -> colorless = amount;
        }
    }

    public synchronized int addMana(ManaColor color, int amount) {
        if (color == null || amount <= 0) return 0;
        int currentTotal = getTotalMana();
        int space = maxMana - currentTotal;
        if (space <= 0) return 0;

        int toAdd = Math.min(amount, space);
        setDirect(color, get(color) + toAdd);
        return toAdd;
    }

    public synchronized boolean removeMana(ManaColor color, int amount) {
        if (color == null || amount < 0) return false;
        if (amount == 0) return true;
        int current = get(color);
        if (current < amount) return false;

        setDirect(color, current - amount);
        return true;
    }

    public synchronized int getTotalMana() {
        long sum = (long) white + blue + black + red + green + colorless;
        if (sum > maxMana) return maxMana;
        if (sum < 0) return 0;
        return (int) sum;
    }

    public synchronized int getMaxMana() {
        return maxMana;
    }

    public synchronized void setMaxMana(int maxMana) {
        this.maxMana = Math.max(1, maxMana);
        clampToMax();
    }

    private void clampToMax() {
        long totalLong = (long) white + blue + black + red + green + colorless;
        if (totalLong <= maxMana && totalLong >= 0) return;

        long excess = totalLong - maxMana;
        // Priority to trim: Colorless first, then Green, Red, Black, Blue, White
        ManaColor[] trimOrder = {ManaColor.COLORLESS, ManaColor.GREEN, ManaColor.RED, ManaColor.BLACK, ManaColor.BLUE, ManaColor.WHITE};
        for (ManaColor c : trimOrder) {
            int current = get(c);
            if (current > 0) {
                long trim = Math.min((long) current, excess);
                setDirect(c, (int) (current - trim));
                excess -= trim;
                if (excess <= 0) break;
            }
        }
    }

    public synchronized boolean hasChosenInitialAffinity() {
        return hasChosenInitialAffinity;
    }

    public synchronized ManaColor getChosenAffinity() {
        return chosenAffinity;
    }

    public synchronized void setInitialAffinity(ManaColor affinity, int startingMana) {
        this.chosenAffinity = Objects.requireNonNull(affinity, "Affinity color cannot be null");
        this.hasChosenInitialAffinity = true;
        if (startingMana > 0) {
            addMana(affinity, startingMana);
        }
    }

    public synchronized boolean canPay(ManaCost cost) {
        return ManaCostEvaluator.canAfford(this, cost);
    }

    public synchronized boolean payCost(ManaCost cost) {
        return ManaCostEvaluator.deductCost(this, cost);
    }

    public synchronized void clear() {
        white = 0;
        blue = 0;
        black = 0;
        red = 0;
        green = 0;
        colorless = 0;
    }

    public synchronized byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(46);
        buffer.putLong(playerId.getMostSignificantBits());
        buffer.putLong(playerId.getLeastSignificantBits());
        buffer.putInt(white);
        buffer.putInt(blue);
        buffer.putInt(black);
        buffer.putInt(red);
        buffer.putInt(green);
        buffer.putInt(colorless);
        buffer.putInt(maxMana);
        buffer.put((byte) (hasChosenInitialAffinity ? 1 : 0));
        buffer.put((byte) (chosenAffinity != null ? chosenAffinity.ordinal() : -1));
        return buffer.array();
    }

    public static ManaPool fromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "Byte array cannot be null");
        if (bytes.length < 46) {
            throw new IllegalArgumentException("Byte array length " + bytes.length + " is shorter than required 46 bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        UUID id = new UUID(msb, lsb);
        int w = buffer.getInt();
        int u = buffer.getInt();
        int b = buffer.getInt();
        int r = buffer.getInt();
        int g = buffer.getInt();
        int c = buffer.getInt();
        int max = buffer.getInt();
        boolean hasChosen = buffer.get() == 1;
        byte affinityOrdinal = buffer.get();
        ManaColor affinity = (affinityOrdinal >= 0 && affinityOrdinal < ManaColor.values().length)
                ? ManaColor.values()[affinityOrdinal] : null;

        return new ManaPool(id, w, u, b, r, g, c, max, hasChosen, affinity);
    }

    public synchronized Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("playerId", playerId.toString());
        map.put("white", white);
        map.put("blue", blue);
        map.put("black", black);
        map.put("red", red);
        map.put("green", green);
        map.put("colorless", colorless);
        map.put("maxMana", maxMana);
        map.put("hasChosenInitialAffinity", hasChosenInitialAffinity);
        map.put("chosenAffinity", chosenAffinity != null ? chosenAffinity.name() : "");
        return map;
    }

    public static ManaPool fromMap(UUID playerId, Map<String, Object> map) {
        Objects.requireNonNull(map, "Map cannot be null");
        UUID id = playerId;
        if (id == null && map.containsKey("playerId")) {
            id = UUID.fromString(map.get("playerId").toString());
        }
        if (id == null) {
            id = UUID.randomUUID();
        }

        int w = getInt(map, "white", 0);
        int u = getInt(map, "blue", 0);
        int b = getInt(map, "black", 0);
        int r = getInt(map, "red", 0);
        int g = getInt(map, "green", 0);
        int c = getInt(map, "colorless", 0);
        int max = getInt(map, "maxMana", DEFAULT_MAX_MANA);
        boolean hasChosen = Boolean.parseBoolean(String.valueOf(map.getOrDefault("hasChosenInitialAffinity", "false")));
        String affStr = String.valueOf(map.getOrDefault("chosenAffinity", ""));
        ManaColor affinity = null;
        if (!affStr.isBlank()) {
            try {
                affinity = ManaColor.valueOf(affStr);
            } catch (IllegalArgumentException ignored) {}
        }

        return new ManaPool(id, w, u, b, r, g, c, max, hasChosen, affinity);
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
