package com.mtg.world.boss;

import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.ManaColor;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Configurable elemental boss implementation provided by the Base Library.
 * Usable out of the box by Planeswalker Altars, tests, and world encounters for any ManaColor,
 * while allowing expansions to override boss encounters via custom BossManager factories.
 */
public class GenericElementalBoss extends AbstractBossEntity {
    public static final float DEFAULT_BASE_HEALTH = 200.0f;
    public static final float DEFAULT_BASE_ATTACK_DAMAGE = 12.0f;

    private final ManaColor affinityColor;

    public GenericElementalBoss(UUID id, String bossType, ManaColor affinityColor, float baseHealth, float baseAttackDamage, TriggerBus triggerBus) {
        super(id, bossType, baseHealth, baseAttackDamage, triggerBus);
        this.affinityColor = Objects.requireNonNull(affinityColor, "Affinity color cannot be null");
    }

    public GenericElementalBoss(ManaColor affinityColor, float baseHealth, float baseAttackDamage, TriggerBus triggerBus) {
        this(
                UUID.randomUUID(),
                "mtg:elemental_avatar_" + (affinityColor != null ? affinityColor.name().toLowerCase(Locale.ROOT) : "colorless"),
                affinityColor != null ? affinityColor : ManaColor.COLORLESS,
                baseHealth,
                baseAttackDamage,
                triggerBus
        );
    }

    public GenericElementalBoss(ManaColor affinityColor, TriggerBus triggerBus) {
        this(affinityColor, DEFAULT_BASE_HEALTH, DEFAULT_BASE_ATTACK_DAMAGE, triggerBus);
    }

    public GenericElementalBoss(ManaColor affinityColor) {
        this(affinityColor, null);
    }

    public ManaColor getAffinityColor() {
        return affinityColor;
    }

    @Override
    protected void onPhaseChange(BossPhase oldPhase, BossPhase newPhase) {
        super.onPhaseChange(oldPhase, newPhase);
        if (newPhase == BossPhase.PHASE_2_ENRAGED) {
            setAttackDamage(getBaseAttackDamage() * 1.25f);
        } else if (newPhase == BossPhase.PHASE_3_DESPERATION) {
            setAttackDamage(getBaseAttackDamage() * 1.50f);
        }
    }
}
