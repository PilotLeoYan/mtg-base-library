package com.mtg.cards.effects;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.casting.EnchantmentExpiredEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service managing persistent timed enchantment auras and field effects.
 */
public class EnchantmentManager {
    private static final TriggerBus DEFAULT_BUS = new TriggerBus();
    private static volatile EnchantmentManager instance;

    private final Map<UUID, List<ActiveEnchantment>> activeEnchantments = new ConcurrentHashMap<>();
    private TriggerBus triggerBus = DEFAULT_BUS;

    public static EnchantmentManager getInstance() {
        if (instance == null) {
            synchronized (EnchantmentManager.class) {
                if (instance == null) {
                    instance = new EnchantmentManager();
                }
            }
        }
        return instance;
    }

    public static void setInstance(EnchantmentManager custom) {
        instance = custom;
    }

    public void setTriggerBus(TriggerBus triggerBus) {
        this.triggerBus = triggerBus != null ? triggerBus : DEFAULT_BUS;
    }

    public void addEnchantment(ActiveEnchantment enchantment) {
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");
        activeEnchantments.computeIfAbsent(enchantment.getOwnerId(), k -> new CopyOnWriteArrayList<>())
            .add(enchantment);
    }

    public List<ActiveEnchantment> getPlayerEnchantments(UUID ownerId) {
        if (ownerId == null) return List.of();
        List<ActiveEnchantment> list = activeEnchantments.get(ownerId);
        if (list == null) return List.of();
        return Collections.unmodifiableList(list);
    }

    public boolean hasEnchantment(UUID ownerId, String cardId) {
        if (ownerId == null || cardId == null) return false;
        List<ActiveEnchantment> list = activeEnchantments.get(ownerId);
        if (list == null) return false;
        return list.stream().anyMatch(e -> e.getCard().id().equals(cardId));
    }

    public void tickAll() {
        for (Map.Entry<UUID, List<ActiveEnchantment>> entry : activeEnchantments.entrySet()) {
            UUID ownerId = entry.getKey();
            List<ActiveEnchantment> list = entry.getValue();
            Iterator<ActiveEnchantment> it = list.iterator();
            while (it.hasNext()) {
                ActiveEnchantment ench = it.next();
                boolean expired = ench.tick();
                if (expired) {
                    ench.expire();
                    list.remove(ench);
                    if (triggerBus != null) {
                        triggerBus.post(new EnchantmentExpiredEvent(this, ownerId, ench.getCard(), ench.getInstanceId()));
                    }
                } else if (ench.isExpired()) {
                    list.remove(ench);
                }
            }
            if (list.isEmpty()) {
                activeEnchantments.computeIfPresent(ownerId, (k, v) -> v.isEmpty() ? null : v);
            }
        }
    }

    public void triggerUpkeep(UUID ownerId) {
        if (ownerId == null) return;
        List<ActiveEnchantment> list = activeEnchantments.get(ownerId);
        if (list != null) {
            for (ActiveEnchantment ench : list) {
                ench.triggerUpkeep();
            }
        }
    }

    public void removePlayerEnchantments(UUID ownerId) {
        if (ownerId == null) return;
        List<ActiveEnchantment> list = activeEnchantments.remove(ownerId);
        if (list != null) {
            for (ActiveEnchantment e : list) {
                e.expire();
            }
        }
    }

    public void clearAll() {
        for (List<ActiveEnchantment> list : activeEnchantments.values()) {
            for (ActiveEnchantment e : list) {
                e.expire();
            }
        }
        activeEnchantments.clear();
    }
}
