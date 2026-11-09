package com.mtg.test.guidebook;

import com.mtg.guidebook.format.FormattedTextSpan;
import com.mtg.guidebook.format.ManaSymbolFormatter;
import com.mtg.guidebook.format.SpanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Symbol and Guidebook Rich Text Formatter Suite")
public class ManaSymbolFormatterTest {

    @Test
    @DisplayName("Recognizes standard WUBRG, Colorless, Tap, Variable, and Phyrexian mana symbols")
    void testManaSymbolRecognition() {
        String input = "{W} {U} {B} {R} {G} {C} {T} {X} {2} {W/P}";
        List<FormattedTextSpan> spans = ManaSymbolFormatter.format(input);

        // Every alternate span should be a mana symbol
        long symbolCount = spans.stream().filter(s -> s.type() == SpanType.MANA_SYMBOL).count();
        assertEquals(10, symbolCount, "All 10 mana tokens must be parsed as MANA_SYMBOL");

        assertTrue(ManaSymbolFormatter.isManaSymbol("{W}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{U}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{B}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{R}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{G}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{C}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{T}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{X}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{1}"));
        assertTrue(ManaSymbolFormatter.isManaSymbol("{W/P}"));
        assertFalse(ManaSymbolFormatter.isManaSymbol("NotASymbol"));
    }

    @Test
    @DisplayName("Card link tags are correctly parsed into CARD_LINK spans with card ID data")
    void testCardLinkParsing() {
        String input = "Cast [card:mtg:base/lightning_bolt] on the target.";
        List<FormattedTextSpan> spans = ManaSymbolFormatter.format(input);

        assertEquals(3, spans.size());
        assertEquals("Cast ", spans.get(0).text());
        assertEquals(SpanType.TEXT, spans.get(0).type());

        assertEquals("mtg:base/lightning_bolt", spans.get(1).text());
        assertEquals(SpanType.CARD_LINK, spans.get(1).type());
        assertEquals("mtg:base/lightning_bolt", spans.get(1).data());

        assertEquals(" on the target.", spans.get(2).text());
        assertEquals(SpanType.TEXT, spans.get(2).type());
    }

    @Test
    @DisplayName("Card link with custom display text: [card:id|Display Text]")
    void testCardLinkWithCustomDisplayText() {
        String input = "Invoke [card:mtg:base/serra_angel|Holy Angel] into battle.";
        List<FormattedTextSpan> spans = ManaSymbolFormatter.format(input);

        assertEquals(3, spans.size());
        assertEquals("Holy Angel", spans.get(1).text());
        assertEquals(SpanType.CARD_LINK, spans.get(1).type());
        assertEquals("mtg:base/serra_angel", spans.get(1).data());
    }

    @Test
    @DisplayName("Keyword highlight tags: [kw:Flying] and [keyword:Vigilance]")
    void testKeywordHighlightParsing() {
        String input = "Grants [kw:Flying] and [keyword:Vigilance] to target.";
        List<FormattedTextSpan> spans = ManaSymbolFormatter.format(input);

        long kwCount = spans.stream().filter(s -> s.type() == SpanType.KEYWORD_HIGHLIGHT).count();
        assertEquals(2, kwCount);

        FormattedTextSpan kw1 = spans.stream().filter(s -> s.text().equals("Flying")).findFirst().orElseThrow();
        assertEquals("Flying", kw1.data());

        FormattedTextSpan kw2 = spans.stream().filter(s -> s.text().equals("Vigilance")).findFirst().orElseThrow();
        assertEquals("Vigilance", kw2.data());
    }

    @Test
    @DisplayName("Compound complex sentence preserves token sequence and surrounding text exactly")
    void testCompoundSentenceFormatting() {
        String input = "Pay {2}{R} to cast [card:mtg:base/lightning_bolt].";
        List<FormattedTextSpan> spans = ManaSymbolFormatter.format(input);

        assertEquals(6, spans.size());
        assertEquals("Pay ", spans.get(0).text());
        assertEquals("{2}", spans.get(1).text());
        assertEquals("{R}", spans.get(2).text());
        assertEquals(" to cast ", spans.get(3).text());
        assertEquals("mtg:base/lightning_bolt", spans.get(4).text());
        assertEquals(SpanType.CARD_LINK, spans.get(4).type());
        assertEquals(".", spans.get(5).text());
        assertEquals(SpanType.TEXT, spans.get(5).type());
    }

    @Test
    @DisplayName("Null or empty string produces empty span list")
    void testNullOrEmptyInput() {
        assertTrue(ManaSymbolFormatter.format(null).isEmpty());
        assertTrue(ManaSymbolFormatter.format("").isEmpty());
    }
}
