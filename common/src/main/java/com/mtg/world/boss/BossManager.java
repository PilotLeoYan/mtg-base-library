package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;
import com.mtg.world.events.BossSpawnEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Central registry and lifecycle manager for active World Bosses in the world.
 */
public class BossManager {
    private static final BossManager INSTANCE = new BossManager();

    public static BossManager getInstance() {
        return INSTANCE;
    }

    private final Map<UUID, IBossEntity> activeBosses = new ConcurrentHashMap<>();
    private final Map<ManaColor, BiFunction<UUID, TriggerBus, IBossEntity>> bossFactories = new ConcurrentHashMap<>();
    private TriggerBus triggerBus = new TriggerBus();

    public BossManager() {}

    public synchronized void setTriggerBus(TriggerBus triggerBus) {
        this.triggerBus = (triggerBus != null) ? triggerBus : new TriggerBus();
    }

    public synchronized TriggerBus getTriggerBus() {
        return triggerBus;
    }

    public void registerBossFactory(ManaColor color, BiFunction<UUID, TriggerBus, IBossEntity> factory) {
        Objects.requireNonNull(color, "ManaColor cannot be null");
        Objects.requireNonNull(factory, "Factory cannot be null");
        bossFactories.put(color, factory);
    }

    public IBossEntity createBossForColor(ManaColor color, TriggerBus bus) {
        ManaColor resolvedColor = (color != null) ? color : ManaColor.COLORLESS;
        BiFunction<UUID, TriggerBus, IBossEntity> factory = bossFactories.get(resolvedColor);
        if (factory != null) {
            return factory.apply(UUID.randomUUID(), bus);
        }
        return new GenericElementalBoss(
                UUID.randomUUID(),
                "mtg:elemental_avatar_" + resolvedColor.name().toLowerCase(Locale.ROOT),
                resolvedColor,
                200.0f,
                12.0f,
                bus
        );
    }

    public IBossEntity createBossForColor(ManaColor color) {
        return createBossForColor(color, triggerBus);
    }

    public void registerBoss(IBossEntity boss) {
        if (boss == null) return;
        if (boss.getTriggerBus() == null) {
            boss.setTriggerBus(triggerBus);
        }
        activeBosses.put(boss.getId(), boss);
        if (triggerBus != null) {
            triggerBus.post(new BossSpawnEvent(this, boss.getId(), boss.getBossType(), boss.getX(), boss.getY(), boss.getZ()));
        }
    }

    public Optional<IBossEntity> getBoss(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(activeBosses.get(id));
    }

    public void removeBoss(UUID id) {
        if (id != null) {
            activeBosses.remove(id);
        }
    }

    public Collection<IBossEntity> getAllBosses() {
        return Collections.unmodifiableCollection(activeBosses.values());
    }

    public void tickAll() {
        for (IBossEntity boss : activeBosses.values()) {
            if (boss.isAlive()) {
                boss.tick();
            }
        }
    }

    public void clear() {
        activeBosses.clear();
        bossFactories.clear();
    }
}
