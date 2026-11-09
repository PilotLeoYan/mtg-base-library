package com.mtg.entity.ai;

/**
 * High-level AI state for player-aligned creature summons.
 */
public enum SummonAiState {
    IDLE,
    FOLLOWING,
    DEFENDING,
    ATTACKING,
    RETURNING
}
