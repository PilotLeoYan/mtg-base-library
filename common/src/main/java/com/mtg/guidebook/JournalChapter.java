package com.mtg.guidebook;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Major thematic section of the Planeswalker's Journal containing related entries.
 */
public record JournalChapter(
    String id,
    String titleKey,
    String subtitleKey,
    String iconId,
    List<JournalEntry> entries
) {
    public JournalChapter {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(titleKey, "TitleKey cannot be null");
        subtitleKey = subtitleKey != null ? subtitleKey : "";
        iconId = iconId != null ? iconId : "mtg:planeswalker_journal";
        entries = entries != null ? List.copyOf(entries) : Collections.emptyList();
    }

    public int getEntryCount() {
        return entries.size();
    }

    public Optional<JournalEntry> getEntry(String entryId) {
        if (entryId == null) return Optional.empty();
        return entries.stream()
                .filter(e -> e.id().equalsIgnoreCase(entryId))
                .findFirst();
    }
}
