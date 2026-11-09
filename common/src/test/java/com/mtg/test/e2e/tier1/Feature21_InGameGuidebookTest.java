package com.mtg.test.e2e.tier1;

import com.mtg.guidebook.JournalBookModel;
import com.mtg.guidebook.JournalItemDefinition;
import com.mtg.guidebook.catalog.DefaultJournalContent;
import com.mtg.guidebook.format.FormattedTextSpan;
import com.mtg.guidebook.format.ManaSymbolFormatter;
import com.mtg.guidebook.format.SpanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 21: In-Game Guide Book (mtg:planeswalker_journal) E2E Test")
public class Feature21_InGameGuidebookTest {

    @Test
    @DisplayName("1. Default guidebook contains exactly 6 core chapters matching mod pillars")
    void testGuidebookSixChapters() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();
        assertNotNull(book);
        assertEquals(6, book.getChapters().size(), "Guidebook must contain 6 chapters");

        List<String> expectedChapters = List.of("mana", "cards", "deck", "progression", "invocations", "world");
        for (String chId : expectedChapters) {
            assertTrue(book.getChapter(chId).isPresent(), "Chapter '" + chId + "' must exist in guidebook");
        }
    }

    @Test
    @DisplayName("2. Guidebook entry page retrieval returns formatted page content")
    void testGuidebookEntryPages() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();
        var entryOpt = book.findEntry("mana", "mana_colors");

        assertTrue(entryOpt.isPresent(), "mana_colors entry must exist");
        var entry = entryOpt.get();
        assertTrue(entry.getPageCount() >= 1, "Must have at least 1 page");
        assertTrue(entry.getPage(1).isPresent(), "Page 1 must be present");
    }

    @Test
    @DisplayName("3. ManaSymbolFormatter parses mana symbols ({W}, {U}, {B}, {R}, {G}) into formatted spans")
    void testManaSymbolFormatter() {
        String rawText = "Pay {W} and {U} to activate.";
        List<FormattedTextSpan> spans = ManaSymbolFormatter.format(rawText);

        assertNotNull(spans);
        assertFalse(spans.isEmpty());

        boolean hasManaSymbolSpan = spans.stream().anyMatch(s -> s.type() == SpanType.MANA_SYMBOL);
        assertTrue(hasManaSymbolSpan, "Formatted spans must include parsed MANA_SYMBOL tokens");
    }

    @Test
    @DisplayName("4. Guidebook item definition declares mtg:planeswalker_journal ID and stack size 1")
    void testJournalItemDefinition() {
        assertEquals("mtg:planeswalker_journal", JournalItemDefinition.ITEM_ID);
        assertEquals(1, JournalItemDefinition.MAX_STACK_SIZE, "Guidebook must be non-stackable (max stack 1)");
    }

    @Test
    @DisplayName("5. Searching guidebook entries returns matching chapter and entry models")
    void testGuidebookSearch() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();
        var cardsEntry = book.findEntry("cards", "card_types");
        assertTrue(cardsEntry.isPresent(), "card_types entry must be found under cards chapter");
        assertNotNull(cardsEntry.get().titleKey());
    }
}
