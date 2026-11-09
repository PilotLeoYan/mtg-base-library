package com.mtg.cards.effects;

/**
 * Lifecycle callbacks for active timed enchantments.
 */
public interface IEnchantmentTickListener {
    default void onTick(ActiveEnchantment enchantment) {}
    default void onUpkeep(ActiveEnchantment enchantment) {}
    default void onExpire(ActiveEnchantment enchantment) {}
}
