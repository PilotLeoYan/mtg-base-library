package com.mtg.api;

import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.domain.CardDefinition;
import com.mtg.guidebook.JournalBookModel;
import com.mtg.world.boss.BossManager;

import java.util.function.Consumer;

/**
 * Interface contract for all MTG expansion sets (Base, Zendikar Rising, Phyrexia, etc.).
 */
public interface IExpansionSet {
    /**
     * Unique set code, matched case-insensitively. The library reserves "base".
     */
    String getSetCode();

    /**
     * Full human-readable expansion set name.
     */
    String getSetName();

    /**
     * Registers all cards belonging to this expansion set.
     */
    void registerCards(Consumer<CardDefinition> registry);

    /**
     * Registers card effect handlers associated with cards in this expansion set.
     */
    default void registerEffects() {}

    /**
     * Registers custom boss entities or boss factories introduced by this expansion set.
     */
    default void registerBosses(BossManager bossManager) {}

    /**
     * Registers guidebook chapters, entries, or pages introduced by this expansion set.
     */
    default void registerGuidebookContent(JournalBookModel guidebook) {}

    /**
     * Registers any mechanics/keyword abilities introduced by this expansion set.
     */
    void registerKeywords(KeywordRegistry registry);

    /**
     * Registers trigger listeners and lifecycle hooks needed by this expansion set.
     */
    void registerTriggers(TriggerBus bus);

    /**
     * Release or evaluation priority order (higher priority initializes earlier).
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Returns true if this expansion is enabled in mod configuration.
     */
    default boolean isEnabled() {
        return true;
    }
}
