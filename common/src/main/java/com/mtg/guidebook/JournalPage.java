package com.mtg.guidebook;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable page record within an entry in the Planeswalker's Journal.
 */
public record JournalPage(
    int pageNumber,
    String titleKey,
    String bodyKey,
    PageContentType contentType,
    List<String> embeddedCardIds,
    Map<String, String> metadata
) {
    public JournalPage {
        Objects.requireNonNull(titleKey, "TitleKey cannot be null");
        Objects.requireNonNull(bodyKey, "BodyKey cannot be null");
        contentType = contentType != null ? contentType : PageContentType.TEXT_ONLY;
        embeddedCardIds = embeddedCardIds != null ? List.copyOf(embeddedCardIds) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static JournalPage text(int pageNumber, String titleKey, String bodyKey) {
        return new JournalPage(pageNumber, titleKey, bodyKey, PageContentType.TEXT_ONLY, Collections.emptyList(), Collections.emptyMap());
    }

    public static JournalPage cardShowcase(int pageNumber, String titleKey, String bodyKey, List<String> cardIds) {
        return new JournalPage(pageNumber, titleKey, bodyKey, PageContentType.CARD_SHOWCASE, cardIds, Collections.emptyMap());
    }

    public static JournalPage reference(int pageNumber, String titleKey, String bodyKey, Map<String, String> tableData) {
        return new JournalPage(pageNumber, titleKey, bodyKey, PageContentType.TABLE_REFERENCE, Collections.emptyList(), tableData);
    }
}
