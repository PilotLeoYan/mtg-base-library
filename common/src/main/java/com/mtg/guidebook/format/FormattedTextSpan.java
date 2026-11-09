package com.mtg.guidebook.format;

import java.util.Objects;

/**
 * An individual tokenized span of text with an associated formatting type and metadata.
 */
public record FormattedTextSpan(
    String text,
    SpanType type,
    String data
) {
    public FormattedTextSpan {
        Objects.requireNonNull(text, "Text cannot be null");
        type = type != null ? type : SpanType.TEXT;
        data = data != null ? data : "";
    }

    public static FormattedTextSpan text(String text) {
        return new FormattedTextSpan(text, SpanType.TEXT, "");
    }

    public static FormattedTextSpan symbol(String symbolToken) {
        return new FormattedTextSpan(symbolToken, SpanType.MANA_SYMBOL, symbolToken);
    }

    public static FormattedTextSpan cardLink(String displayText, String cardId) {
        return new FormattedTextSpan(displayText, SpanType.CARD_LINK, cardId);
    }

    public static FormattedTextSpan keyword(String keywordName) {
        return new FormattedTextSpan(keywordName, SpanType.KEYWORD_HIGHLIGHT, keywordName);
    }
}
