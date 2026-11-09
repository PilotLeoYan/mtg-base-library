package com.mtg.cards.effects;

/**
 * Functional interface for resolving spell, land, or enchantment effects.
 */
@FunctionalInterface
public interface ICardEffectHandler {
    CardEffectResult apply(CastContext context);
}
