package com.mtg.entity.ai;

import com.mtg.entity.ISummonedCreature;

/**
 * AI goal behavior interface for companion creature invocations.
 */
public interface ISummonAiGoal {
    boolean canExecute(ISummonedCreature creature, AiContext context);
    void tick(ISummonedCreature creature, AiContext context);
    default void stop(ISummonedCreature creature, AiContext context) {}
}
