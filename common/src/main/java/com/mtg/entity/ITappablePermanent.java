package com.mtg.entity;

import java.util.UUID;

/**
 * Interface contract for permanents with activated tap abilities ({T}).
 */
public interface ITappablePermanent {
    boolean canActivate(UUID playerId);
    boolean activate(UUID playerId);
}
