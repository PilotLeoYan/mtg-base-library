package com.mtg.guidebook.catalog;

import com.mtg.guidebook.*;

import java.util.List;
import java.util.Map;

/**
 * Builds the authoritative 6 core chapters of the Planeswalker's Journal wiki book,
 * containing accurate gameplay formulas, mana regeneration ticks, cooldown rules,
 * duplicate protection ratios, and world boss tactics.
 */
public final class DefaultJournalContent {
    private DefaultJournalContent() {}

    public static JournalBookModel createDefaultJournal() {
        JournalBookModel book = new JournalBookModel("mtg:planeswalker_journal", "mtg.guidebook.title");

        book.addChapter(buildChapter1Mana());
        book.addChapter(buildChapter2Cards());
        book.addChapter(buildChapter3Deck());
        book.addChapter(buildChapter4Progression());
        book.addChapter(buildChapter5Invocations());
        book.addChapter(buildChapter6World());

        return book;
    }

    private static JournalChapter buildChapter1Mana() {
        JournalEntry colorsEntry = new JournalEntry(
                "mana_colors",
                "mtg.guidebook.entry.mana.colors.title",
                "mtg.guidebook.entry.mana.colors.summary",
                "item.mtg.mana_gem_white",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.mana.colors_1.title", "mtg.guidebook.page.mana.colors_1.body"),
                        JournalPage.text(2, "mtg.guidebook.page.mana.colors_2.title", "mtg.guidebook.page.mana.colors_2.body")
                )
        );

        JournalEntry regenEntry = new JournalEntry(
                "biome_regeneration",
                "mtg.guidebook.entry.mana.regen.title",
                "mtg.guidebook.entry.mana.regen.summary",
                "item.mtg.mana_gem_green",
                List.of(
                        JournalPage.reference(1, "mtg.guidebook.page.mana.regen_1.title", "mtg.guidebook.page.mana.regen_1.body",
                                Map.of("Plains", "{W}", "Ocean", "{U}", "Swamp", "{B}", "Mountain", "{R}", "Forest", "{G}"))
                )
        );

        JournalEntry xpUpgradeEntry = new JournalEntry(
                "mana_capacity_upgrades",
                "mtg.guidebook.entry.mana.upgrades.title",
                "mtg.guidebook.entry.mana.upgrades.summary",
                "item.mtg.mana_gem_blue",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.mana.upgrades_1.title", "mtg.guidebook.page.mana.upgrades_1.body")
                )
        );

        return new JournalChapter(
                "mana",
                "mtg.guidebook.chapter.mana",
                "mtg.guidebook.chapter.mana.subtitle",
                "item.mtg.mana_gem_white",
                List.of(colorsEntry, regenEntry, xpUpgradeEntry)
        );
    }

    private static JournalChapter buildChapter2Cards() {
        JournalEntry typesEntry = new JournalEntry(
                "card_types",
                "mtg.guidebook.entry.cards.types.title",
                "mtg.guidebook.entry.cards.types.summary",
                "item.mtg.card_hand",
                List.of(
                        JournalPage.cardShowcase(1, "mtg.guidebook.page.cards.types_1.title", "mtg.guidebook.page.cards.types_1.body",
                                List.of("mtg:base/plains", "mtg:base/island", "mtg:base/swamp")),
                        JournalPage.cardShowcase(2, "mtg.guidebook.page.cards.types_2.title", "mtg.guidebook.page.cards.types_2.body",
                                List.of("mtg:base/mountain", "mtg:base/forest", "mtg:base/wastes"))
                )
        );

        JournalEntry delaysEntry = new JournalEntry(
                "casting_delays",
                "mtg.guidebook.entry.cards.delays.title",
                "mtg.guidebook.entry.cards.delays.summary",
                "item.mtg.mana_gem_red",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.cards.delays_1.title", "mtg.guidebook.page.cards.delays_1.body"),
                        JournalPage.text(2, "mtg.guidebook.page.cards.delays_2.title", "mtg.guidebook.page.cards.delays_2.body")
                )
        );

        return new JournalChapter(
                "cards",
                "mtg.guidebook.chapter.cards",
                "mtg.guidebook.chapter.cards.subtitle",
                "item.mtg.card_hand",
                List.of(typesEntry, delaysEntry)
        );
    }

    private static JournalChapter buildChapter3Deck() {
        JournalEntry handEntry = new JournalEntry(
                "dedicated_hand",
                "mtg.guidebook.entry.deck.hand.title",
                "mtg.guidebook.entry.deck.hand.summary",
                "item.mtg.card_hand",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.deck.hand_1.title", "mtg.guidebook.page.deck.hand_1.body")
                )
        );

        JournalEntry slotsEntry = new JournalEntry(
                "slot_states",
                "mtg.guidebook.entry.deck.slots.title",
                "mtg.guidebook.entry.deck.slots.summary",
                "item.mtg.mana_gem_colorless",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.deck.slots_1.title", "mtg.guidebook.page.deck.slots_1.body")
                )
        );

        JournalEntry capacityEntry = new JournalEntry(
                "capacity_upgrades",
                "mtg.guidebook.entry.deck.capacity.title",
                "mtg.guidebook.entry.deck.capacity.summary",
                "item.mtg.card_hand",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.deck.capacity_1.title", "mtg.guidebook.page.deck.capacity_1.body")
                )
        );

        return new JournalChapter(
                "deck",
                "mtg.guidebook.chapter.deck",
                "mtg.guidebook.chapter.deck.subtitle",
                "item.mtg.card_hand",
                List.of(handEntry, slotsEntry, capacityEntry)
        );
    }

    private static JournalChapter buildChapter4Progression() {
        JournalEntry acquisitionEntry = new JournalEntry(
                "card_acquisition",
                "mtg.guidebook.entry.progression.acquisition.title",
                "mtg.guidebook.entry.progression.acquisition.summary",
                "item.mtg.mana_gem_green",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.progression.acquisition_1.title", "mtg.guidebook.page.progression.acquisition_1.body")
                )
        );

        JournalEntry wildcardEntry = new JournalEntry(
                "wildcard_transmutation",
                "mtg.guidebook.entry.progression.wildcards.title",
                "mtg.guidebook.entry.progression.wildcards.summary",
                "item.mtg.mana_gem_white",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.progression.wildcards_1.title", "mtg.guidebook.page.progression.wildcards_1.body"),
                        JournalPage.text(2, "mtg.guidebook.page.progression.wildcards_2.title", "mtg.guidebook.page.progression.wildcards_2.body")
                )
        );

        return new JournalChapter(
                "progression",
                "mtg.guidebook.chapter.progression",
                "mtg.guidebook.chapter.progression.subtitle",
                "item.mtg.mana_gem_green",
                List.of(acquisitionEntry, wildcardEntry)
        );
    }

    private static JournalChapter buildChapter5Invocations() {
        JournalEntry statsEntry = new JournalEntry(
                "combat_scaling",
                "mtg.guidebook.entry.invocations.stats.title",
                "mtg.guidebook.entry.invocations.stats.summary",
                "item.mtg.mana_gem_red",
                List.of(
                        JournalPage.cardShowcase(1, "mtg.guidebook.page.invocations.stats_1.title", "mtg.guidebook.page.invocations.stats_1.body",
                                List.of("mtg:base/plains", "mtg:base/forest"))
                )
        );

        JournalEntry aiEntry = new JournalEntry(
                "companion_ai",
                "mtg.guidebook.entry.invocations.ai.title",
                "mtg.guidebook.entry.invocations.ai.summary",
                "item.mtg.mana_gem_blue",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.invocations.ai_1.title", "mtg.guidebook.page.invocations.ai_1.body"),
                        JournalPage.text(2, "mtg.guidebook.page.invocations.ai_2.title", "mtg.guidebook.page.invocations.ai_2.body")
                )
        );

        return new JournalChapter(
                "invocations",
                "mtg.guidebook.chapter.invocations",
                "mtg.guidebook.chapter.invocations.subtitle",
                "item.mtg.mana_gem_red",
                List.of(statsEntry, aiEntry)
        );
    }

    private static JournalChapter buildChapter6World() {
        JournalEntry nodesEntry = new JournalEntry(
                "elemental_nodes",
                "mtg.guidebook.entry.world.nodes.title",
                "mtg.guidebook.entry.world.nodes.summary",
                "item.mtg.mana_gem_white",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.world.nodes_1.title", "mtg.guidebook.page.world.nodes_1.body")
                )
        );

        JournalEntry altarsEntry = new JournalEntry(
                "planeswalker_altars",
                "mtg.guidebook.entry.world.altars.title",
                "mtg.guidebook.entry.world.altars.summary",
                "block.mtg.mana_altar",
                List.of(
                        JournalPage.text(1, "mtg.guidebook.page.world.altars_1.title", "mtg.guidebook.page.world.altars_1.body"),
                        JournalPage.text(2, "mtg.guidebook.page.world.altars_2.title", "mtg.guidebook.page.world.altars_2.body")
                )
        );

        JournalEntry bossesEntry = new JournalEntry(
                "world_bosses",
                "mtg.guidebook.entry.world.bosses.title",
                "mtg.guidebook.entry.world.bosses.summary",
                "item.mtg.mana_gem_black",
                List.of(
                        JournalPage.cardShowcase(1, "mtg.guidebook.page.world.bosses_1.title", "mtg.guidebook.page.world.bosses_1.body",
                                List.of("mtg:base/mountain", "mtg:base/swamp")),
                        JournalPage.text(2, "mtg.guidebook.page.world.bosses_2.title", "mtg.guidebook.page.world.bosses_2.body")
                )
        );

        return new JournalChapter(
                "world",
                "mtg.guidebook.chapter.world",
                "mtg.guidebook.chapter.world.subtitle",
                "block.mtg.mana_altar",
                List.of(nodesEntry, altarsEntry, bossesEntry)
        );
    }
}
