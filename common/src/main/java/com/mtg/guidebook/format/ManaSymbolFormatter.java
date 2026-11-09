package com.mtg.guidebook.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless tokenizer that transforms curly-brace mana tokens (e.g. {W}, {U}, {B}, {R}, {G}, {C}, {T}, {X}, {1}, {W/P})
 * and card links ([card:id]) into structured, render-ready FormattedTextSpans.
 */
public final class ManaSymbolFormatter {
    private ManaSymbolFormatter() {}

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(\\{[WUBRGCTX0-9/P]+\\}|\\[card:[^\\]]+\\]|\\[(?:kw|keyword):[^\\]]+\\])"
    );

    /**
     * Parses a string containing mana symbols and rich guidebook tags into a sequence of formatted spans.
     *
     * @param input Raw text with symbols (e.g. "Pay {2}{R} to cast [card:mtg:base/lightning_bolt].")
     * @return List of FormattedTextSpans representing the parsed content
     */
    public static List<FormattedTextSpan> format(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        List<FormattedTextSpan> spans = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {
            // Text preceding the match
            if (matcher.start() > lastIndex) {
                String plainText = input.substring(lastIndex, matcher.start());
                spans.add(FormattedTextSpan.text(plainText));
            }

            String token = matcher.group(1);
            if (token.startsWith("{") && token.endsWith("}")) {
                spans.add(FormattedTextSpan.symbol(token));
            } else if (token.startsWith("[card:")) {
                String payload = token.substring(6, token.length() - 1);
                String cardId;
                String displayText;
                if (payload.contains("|")) {
                    String[] parts = payload.split("\\|", 2);
                    cardId = parts[0].trim();
                    displayText = parts[1].trim();
                } else {
                    cardId = payload.trim();
                    displayText = cardId;
                }
                spans.add(FormattedTextSpan.cardLink(displayText, cardId));
            } else if (token.startsWith("[kw:") || token.startsWith("[keyword:")) {
                int colonIdx = token.indexOf(':');
                String kw = token.substring(colonIdx + 1, token.length() - 1).trim();
                spans.add(FormattedTextSpan.keyword(kw));
            }

            lastIndex = matcher.end();
        }

        // Remaining trailing text
        if (lastIndex < input.length()) {
            spans.add(FormattedTextSpan.text(input.substring(lastIndex)));
        }

        return Collections.unmodifiableList(spans);
    }

    /**
     * Checks if a token matches standard mana symbol notation.
     */
    public static boolean isManaSymbol(String token) {
        if (token == null) return false;
        return token.matches("\\{[WUBRGCTX0-9/P]+\\}");
    }
}
