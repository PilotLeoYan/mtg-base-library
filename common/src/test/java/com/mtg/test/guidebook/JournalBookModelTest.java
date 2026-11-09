package com.mtg.test.guidebook;

import com.mtg.guidebook.JournalBookModel;
import com.mtg.guidebook.JournalChapter;
import com.mtg.guidebook.JournalEntry;
import com.mtg.guidebook.catalog.DefaultJournalContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Planeswalker Journal Data Model & Indexing Suite")
public class JournalBookModelTest {

    @Test
    @DisplayName("Default journal contains exactly 6 core chapters matching mod pillars")
    void testSixCoreChaptersPresent() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();
        assertNotNull(book);

        List<JournalChapter> chapters = book.getChapters();
        assertEquals(6, chapters.size(), "Guidebook must contain exactly 6 core chapters");

        assertTrue(book.getChapter("mana").isPresent(), "Chapter 1: mana must exist");
        assertTrue(book.getChapter("cards").isPresent(), "Chapter 2: cards must exist");
        assertTrue(book.getChapter("deck").isPresent(), "Chapter 3: deck must exist");
        assertTrue(book.getChapter("progression").isPresent(), "Chapter 4: progression must exist");
        assertTrue(book.getChapter("invocations").isPresent(), "Chapter 5: invocations must exist");
        assertTrue(book.getChapter("world").isPresent(), "Chapter 6: world must exist");
    }

    @Test
    @DisplayName("Entry and page navigation: chapters contain valid entries with accessible pages")
    void testEntryAndPageNavigation() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();

        Optional<JournalEntry> colorsEntry = book.findEntry("mana", "mana_colors");
        assertTrue(colorsEntry.isPresent());
        assertEquals("mtg.guidebook.entry.mana.colors.title", colorsEntry.get().titleKey());
        assertEquals(2, colorsEntry.get().getPageCount());

        assertTrue(colorsEntry.get().getPage(1).isPresent());
        assertTrue(colorsEntry.get().getPage(2).isPresent());
        assertTrue(colorsEntry.get().getPage(3).isEmpty(), "Out of bounds page must be empty");
    }

    @Test
    @DisplayName("Embedded card index: findEntriesByCardId retrieves entries referencing specific cards")
    void testEmbeddedCardLookups() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();

        // Mountain is embedded in Chapter V invocations and Chapter VI bosses entry
        List<JournalEntry> mountainEntries = book.findEntriesByCardId("mtg:base/mountain");
        assertFalse(mountainEntries.isEmpty(), "Must find entry showcasing Mountain");
        assertTrue(mountainEntries.stream().anyMatch(e -> "world_bosses".equals(e.id())));

        // Plains is embedded in Chapter II types and Chapter V invocations
        List<JournalEntry> plainsEntries = book.findEntriesByCardId("mtg:base/plains");
        assertEquals(2, plainsEntries.size(), "Plains is referenced in cards and invocations chapters");
    }

    @Test
    @DisplayName("Full-text search locates relevant entries across titles, summaries, and page contents")
    void testFullTextSearch() {
        JournalBookModel book = DefaultJournalContent.createDefaultJournal();

        List<JournalEntry> wildcardResults = book.search("wildcard");
        assertFalse(wildcardResults.isEmpty(), "Search for 'wildcard' must match progression entries");

        List<JournalEntry> altarResults = book.search("altar");
        assertFalse(altarResults.isEmpty(), "Search for 'altar' must match world altar entries");

        List<JournalEntry> emptyResults = book.search("xyznonexistentterm");
        assertTrue(emptyResults.isEmpty(), "Search for nonexistent term must return empty list");
    }
}
