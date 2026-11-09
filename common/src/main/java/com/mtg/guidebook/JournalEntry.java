package com.mtg.guidebook;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An individual topic or article within a chapter of the Planeswalker's Journal.
 */
public record JournalEntry(
    String id,
    String titleKey,
    String summaryKey,
    String iconId,
    List<JournalPage> pages
) {
    public JournalEntry {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(titleKey, "TitleKey cannot be null");
        Objects.requireNonNull(summaryKey, "SummaryKey cannot be null");
        iconId = iconId != null ? iconId : "mtg:planeswalker_journal";
        pages = pages != null ? List.copyOf(pages) : Collections.emptyList();
    }

    public int getPageCount() {
        return pages.size();
    }

    public Optional<JournalPage> getPage(int pageNumber) {
        if (pageNumber < 1 || pageNumber > pages.size()) {
            return Optional.empty();
        }
        return Optional.of(pages.get(pageNumber - 1));
    }
}
