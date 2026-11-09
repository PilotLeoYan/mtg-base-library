package com.mtg.entity.ai;

import com.mtg.entity.ISummonedCreature;

import java.util.Objects;

/**
 * AI goal causing the summoned creature to follow its owner summoner.
 * Enforces emergency leash teleportation when distance exceeds summonLeashDistanceBlocks (48 blocks).
 */
public class FollowOwnerGoal implements ISummonAiGoal {
    public static final double DEFAULT_FOLLOW_THRESHOLD = 6.0;
    public static final double DEFAULT_STOP_THRESHOLD = 3.0;
    public static final double DEFAULT_LEASH_DISTANCE = 48.0;

    private final double followThreshold;
    private final double stopThreshold;
    private final double leashDistance;

    public FollowOwnerGoal(double followThreshold, double stopThreshold, double leashDistance) {
        this.followThreshold = followThreshold;
        this.stopThreshold = stopThreshold;
        this.leashDistance = leashDistance;
    }

    public FollowOwnerGoal(double leashDistance) {
        this(DEFAULT_FOLLOW_THRESHOLD, DEFAULT_STOP_THRESHOLD, leashDistance);
    }

    public FollowOwnerGoal() {
        this(DEFAULT_FOLLOW_THRESHOLD, DEFAULT_STOP_THRESHOLD, DEFAULT_LEASH_DISTANCE);
    }

    @Override
    public boolean canExecute(ISummonedCreature creature, AiContext context) {
        if (creature == null || creature.isDead() || context == null) return false;
        double dist = creature.distanceTo(context.ownerX(), context.ownerY(), context.ownerZ());
        return dist > followThreshold;
    }

    @Override
    public void tick(ISummonedCreature creature, AiContext context) {
        Objects.requireNonNull(creature, "creature cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        double dist = creature.distanceTo(context.ownerX(), context.ownerY(), context.ownerZ());
        if (dist > leashDistance) {
            // Emergency teleportation to owner to prevent chunk desync/orphaning
            creature.setPosition(context.ownerX(), context.ownerY(), context.ownerZ());
            creature.setAiState(SummonAiState.IDLE);
        } else if (dist > followThreshold) {
            creature.setAiState(SummonAiState.FOLLOWING);
            // Move toward owner
            double dirX = (context.ownerX() - creature.getX()) / dist;
            double dirY = (context.ownerY() - creature.getY()) / dist;
            double dirZ = (context.ownerZ() - creature.getZ()) / dist;
            double step = 0.35; // standard movement step
            creature.setPosition(creature.getX() + dirX * step, creature.getY() + dirY * step, creature.getZ() + dirZ * step);
        } else if (dist <= stopThreshold) {
            creature.setAiState(SummonAiState.IDLE);
        }
    }
}
