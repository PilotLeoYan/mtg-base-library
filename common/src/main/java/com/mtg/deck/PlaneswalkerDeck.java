package com.mtg.deck;

import java.util.*;

/**
 * Dedicated Planeswalker Deck container representing the player's dedicated card hand,
 * completely decoupled from the vanilla player inventory and main hand.
 * Supports capacity from 1 to 7 slots (initial 4, upgradeable to 7 via XP).
 */
public final class PlaneswalkerDeck {
    public static final int MIN_CAPACITY = 1;
    public static final int DEFAULT_CAPACITY = 4;
    public static final int MAX_CAPACITY = 7;

    private final UUID playerId;
    private int capacity;
    private int selectedSlot;
    private final List<DeckCardSlot> slots;

    public PlaneswalkerDeck(UUID playerId) {
        this(playerId, DEFAULT_CAPACITY);
    }

    public PlaneswalkerDeck(UUID playerId, int capacity) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.capacity = Math.clamp(capacity, MIN_CAPACITY, MAX_CAPACITY);
        this.selectedSlot = 0;
        List<DeckCardSlot> list = new ArrayList<>(MAX_CAPACITY);
        for (int i = 0; i < MAX_CAPACITY; i++) {
            list.add(new DeckCardSlot(i));
        }
        this.slots = Collections.unmodifiableList(list);
    }

    public PlaneswalkerDeck(UUID playerId, int capacity, int selectedSlot, List<DeckCardSlot> slotList) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.capacity = Math.clamp(capacity, MIN_CAPACITY, MAX_CAPACITY);
        this.selectedSlot = Math.clamp(selectedSlot, 0, this.capacity - 1);
        List<DeckCardSlot> list = new ArrayList<>(MAX_CAPACITY);
        if (slotList != null) {
            for (int i = 0; i < Math.min(slotList.size(), MAX_CAPACITY); i++) {
                list.add(slotList.get(i));
            }
        }
        while (list.size() < MAX_CAPACITY) {
            list.add(new DeckCardSlot(list.size()));
        }
        this.slots = Collections.unmodifiableList(list);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public synchronized int getCapacity() {
        return capacity;
    }

    public synchronized boolean setCapacity(int newCapacity) {
        int clamped = Math.clamp(newCapacity, MIN_CAPACITY, MAX_CAPACITY);
        if (clamped == this.capacity) return false;
        this.capacity = clamped;
        if (selectedSlot >= this.capacity) {
            selectedSlot = this.capacity - 1;
        }
        return true;
    }

    public synchronized boolean upgradeCapacity() {
        if (capacity >= MAX_CAPACITY) {
            return false;
        }
        capacity++;
        return true;
    }

    public synchronized int getSelectedSlot() {
        return selectedSlot;
    }

    public synchronized boolean setSelectedSlot(int index) {
        if (index < 0 || index >= capacity) {
            return false;
        }
        this.selectedSlot = index;
        return true;
    }

    public synchronized void cycleSlot(boolean forward) {
        if (capacity <= 1) {
            selectedSlot = 0;
            return;
        }
        if (forward) {
            selectedSlot = (selectedSlot + 1) % capacity;
        } else {
            selectedSlot = (selectedSlot - 1 + capacity) % capacity;
        }
    }

    public DeckCardSlot getSlot(int index) {
        if (index < 0 || index >= MAX_CAPACITY) {
            throw new IndexOutOfBoundsException("Slot index out of range: " + index + " (0.." + (MAX_CAPACITY - 1) + ")");
        }
        return slots.get(index);
    }

    public synchronized DeckCardSlot getSelectedSlotData() {
        return getSlot(selectedSlot);
    }

    public List<DeckCardSlot> getAllSlots() {
        return slots;
    }

    public synchronized List<DeckCardSlot> getUnlockedSlots() {
        return Collections.unmodifiableList(slots.subList(0, capacity));
    }

    public synchronized boolean equipCard(int slotIndex, String cardId, int baseCooldownTicks, int windupTicks) {
        if (slotIndex < 0 || slotIndex >= MAX_CAPACITY) {
            throw new IndexOutOfBoundsException("Slot index out of range: " + slotIndex);
        }
        if (slotIndex >= capacity) {
            throw new IllegalStateException("Cannot equip card into locked slot: " + slotIndex + " (capacity is " + capacity + ")");
        }
        getSlot(slotIndex).equip(cardId, baseCooldownTicks, windupTicks);
        return true;
    }

    public synchronized boolean unequipCard(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= capacity) {
            return false;
        }
        getSlot(slotIndex).clear();
        return true;
    }

    public synchronized void tick() {
        for (DeckCardSlot slot : slots) {
            slot.tick();
        }
    }

    public synchronized boolean canCastSelected() {
        return getSelectedSlotData().canCast(capacity);
    }

    public synchronized void startCooldown(int slotIndex, int ticks) {
        if (slotIndex >= 0 && slotIndex < capacity) {
            getSlot(slotIndex).startCooldown(ticks);
        }
    }

    public synchronized void startWindup(int slotIndex, int ticks) {
        if (slotIndex >= 0 && slotIndex < capacity) {
            getSlot(slotIndex).startWindup(ticks);
        }
    }

    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"playerId\":\"").append(playerId).append("\",");
        sb.append("\"capacity\":").append(capacity).append(",");
        sb.append("\"selectedSlot\":").append(selectedSlot).append(",");
        sb.append("\"slots\":[");
        for (int i = 0; i < slots.size(); i++) {
            DeckCardSlot slot = slots.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"index\":").append(slot.getSlotIndex()).append(",");
            sb.append("\"cardId\":").append(slot.getCardId() != null ? "\"" + escape(slot.getCardId()) + "\"" : "null").append(",");
            sb.append("\"cooldownRemaining\":").append(slot.getCooldownRemainingTicks()).append(",");
            sb.append("\"maxCooldown\":").append(slot.getMaxCooldownTicks()).append(",");
            sb.append("\"windupRemaining\":").append(slot.getWindupRemainingTicks()).append(",");
            sb.append("\"maxWindup\":").append(slot.getMaxWindupTicks());
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static PlaneswalkerDeck fromJson(String json) {
        Objects.requireNonNull(json, "JSON string cannot be null");
        UUID id = UUID.fromString(extractString(json, "playerId"));
        int cap = extractInt(json, "capacity", DEFAULT_CAPACITY);
        int sel = extractInt(json, "selectedSlot", 0);

        List<DeckCardSlot> slotList = new ArrayList<>(MAX_CAPACITY);
        int slotsIdx = json.indexOf("\"slots\":[");
        if (slotsIdx != -1) {
            int endSlots = json.lastIndexOf(']');
            String arrayContent = json.substring(slotsIdx + 9, endSlots);
            String[] objects = arrayContent.split("},\\{");
            for (String obj : objects) {
                String cleaned = obj.replace("{", "").replace("}", "");
                int idx = extractInt(cleaned, "index", slotList.size());
                String cardId = extractStringOrNull(cleaned, "cardId");
                int cdRem = extractInt(cleaned, "cooldownRemaining", 0);
                int cdMax = extractInt(cleaned, "maxCooldown", 0);
                int wuRem = extractInt(cleaned, "windupRemaining", 0);
                int wuMax = extractInt(cleaned, "maxWindup", 0);
                slotList.add(new DeckCardSlot(idx, cardId, cdRem, cdMax, wuRem, wuMax));
            }
        }

        return new PlaneswalkerDeck(id, cap, sel, slotList);
    }

    private static String extractString(String src, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = src.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = src.indexOf("\"", start);
        return end != -1 ? src.substring(start, end) : "";
    }

    private static String extractStringOrNull(String src, String key) {
        String nullPattern = "\"" + key + "\":null";
        if (src.contains(nullPattern)) return null;
        String val = extractString(src, key);
        return val.isEmpty() ? null : val;
    }

    private static int extractInt(String src, String key, int def) {
        String pattern = "\"" + key + "\":";
        int start = src.indexOf(pattern);
        if (start == -1) return def;
        start += pattern.length();
        int end = start;
        while (end < src.length() && (Character.isDigit(src.charAt(end)) || src.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(src.substring(start, end));
        } catch (Exception e) {
            return def;
        }
    }
}
