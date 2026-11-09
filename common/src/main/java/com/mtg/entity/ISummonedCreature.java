package com.mtg.entity;

import com.mtg.domain.CardDefinition;
import com.mtg.domain.CombatStats;
import com.mtg.entity.ai.SummonAiState;

import java.util.UUID;

/**
 * Pure domain interface for player-aligned creature invocations.
 */
public interface ISummonedCreature {
    UUID getEntityId();
    UUID getOwnerUuid();
    boolean isOwner(UUID playerId);
    CardDefinition getCardDefinition();

    // Stats & Health
    CombatStats getCombatStats();
    int getPower();
    int getToughness();
    float getMaxHealth();
    float getHealth();
    float getAttackDamage();
    void setHealth(float health);
    void heal(float amount);
    void takeDamage(float amount);
    boolean isDead();
    void applyStatModifier(int powerBonus, int toughnessBonus);

    // Summoning Sickness & Combat Readiness
    boolean hasSummoningSickness();
    void clearSummoningSickness();
    boolean canAttack();

    // Tapping & State
    boolean isTapped();
    void tap();
    void untap();
    default boolean tryTap() {
        if (isTapped()) return false;
        tap();
        return true;
    }

    // Keywords
    boolean hasKeyword(String keywordId);
    boolean isFlying();
    boolean hasVigilance();
    boolean hasLifelink();
    boolean hasDeathtouch();
    boolean hasFirstStrike();
    boolean hasTrample();
    boolean hasHaste();
    int getToxicValue();
    default int getKeywordParameter(String keywordId) {
        return 0;
    }

    // AI & Spatial
    SummonAiState getAiState();
    void setAiState(SummonAiState state);
    UUID getTargetEntityId();
    void setTargetEntityId(UUID targetId);
    double getX();
    double getY();
    double getZ();
    void setPosition(double x, double y, double z);
    double distanceTo(double targetX, double targetY, double targetZ);

    // Lifecycle
    int getLifetimeTicks();
    void tick();
    void dismiss();
    boolean isDismissed();
}
