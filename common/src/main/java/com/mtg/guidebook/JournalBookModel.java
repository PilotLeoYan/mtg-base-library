package com.mtg.guidebook;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-safe root data model and index for the Planeswalker's Journal wiki book.
 * Contains chapters, entries, pages, and rich search/indexing capabilities.
 */
public class JournalBookModel {
    private final String bookId;
    private final String titleKey;
    private final List<JournalChapter> chapters = new CopyOnWriteArrayList<>();

    public JournalBookModel() {
        this("mtg:planeswalker_journal", "mtg.guidebook.title");
    }

    public JournalBookModel(String bookId, String titleKey) {
        this.bookId = Objects.requireNonNull(bookId, "BookId cannot be null");
        this.titleKey = Objects.requireNonNull(titleKey, "TitleKey cannot be null");
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public void addChapter(JournalChapter chapter) {
        if (chapter != null) {
            chapters.add(chapter);
        }
    }

    public List<JournalChapter> getChapters() {
        return Collections.unmodifiableList(chapters);
    }

    public Optional<JournalChapter> getChapter(String chapterId) {
        if (chapterId == null) return Optional.empty();
        return chapters.stream()
                .filter(c -> c.id().equalsIgnoreCase(chapterId))
                .findFirst();
    }

    public Optional<JournalEntry> findEntry(String chapterId, String entryId) {
        return getChapter(chapterId).flatMap(c -> c.getEntry(entryId));
    }

    /**
     * Finds all entries across all chapters that showcase or reference the given card ID.
     */
    public List<JournalEntry> findEntriesByCardId(String cardId) {
        if (cardId == null || cardId.isBlank()) return Collections.emptyList();

        List<JournalEntry> results = new ArrayList<>();
        for (JournalChapter chapter : chapters) {
            for (JournalEntry entry : chapter.entries()) {
                boolean containsCard = entry.pages().stream()
                        .anyMatch(page -> page.embeddedCardIds().stream()
                                .anyMatch(id -> id.equalsIgnoreCase(cardId)));
                if (containsCard) {
                    results.add(entry);
                }
            }
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Full-text search across entry IDs, title keys, summary keys, and page contents.
     */
    public List<JournalEntry> search(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        String q = query.toLowerCase(Locale.ROOT).trim();
        List<JournalEntry> matched = new ArrayList<>();

        for (JournalChapter chapter : chapters) {
            for (JournalEntry entry : chapter.entries()) {
                boolean hit = entry.id().toLowerCase(Locale.ROOT).contains(q)
                        || entry.titleKey().toLowerCase(Locale.ROOT).contains(q)
                        || entry.summaryKey().toLowerCase(Locale.ROOT).contains(q);

                if (!hit) {
                    hit = entry.pages().stream().anyMatch(p ->
                            p.titleKey().toLowerCase(Locale.ROOT).contains(q)
                            || p.bodyKey().toLowerCase(Locale.ROOT).contains(q)
                            || p.embeddedCardIds().stream().anyMatch(id -> id.toLowerCase(Locale.ROOT).contains(q))
                    );
                }

                if (hit) {
                    matched.add(entry);
                }
            }
        }
        return Collections.unmodifiableList(matched);
    }
}
