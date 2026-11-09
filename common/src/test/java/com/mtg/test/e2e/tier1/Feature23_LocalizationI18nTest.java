package com.mtg.test.e2e.tier1;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 23: English-First Internationalization (en_us.json) E2E Test")
public class Feature23_LocalizationI18nTest {

    private static Map<String, String> langMap;

    @BeforeAll
    static void loadLangFile() throws Exception {
        InputStream is = Feature23_LocalizationI18nTest.class.getResourceAsStream("/assets/mtg/lang/en_us.json");
        assertNotNull(is, "en_us.json must exist in classpath resources");

        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        langMap = parseSimpleJson(content);
        assertFalse(langMap.isEmpty(), "Localization map must not be empty");
    }

    @Test
    @DisplayName("1. en_us.json loads from classpath and contains user-visible translation keys")
    void testLangFileLoadsSuccessfully() {
        assertNotNull(langMap);
        assertTrue(langMap.size() > 50, "Should have more than 50 localization keys");
    }

    @Test
    @DisplayName("2. Basic land cards across all 5 colors have localized name, type, and text keys")
    void testBasicLandsLocalization() {
        List<String> lands = List.of("plains", "island", "swamp", "mountain", "forest");
        for (String land : lands) {
            String prefix = "mtg.card.base." + land;
            assertTrue(langMap.containsKey(prefix + ".name"), "Missing name for " + land);
            assertTrue(langMap.containsKey(prefix + ".type"), "Missing type for " + land);
            assertTrue(langMap.containsKey(prefix + ".text"), "Missing text for " + land);
        }
    }

    @Test
    @DisplayName("3. Core MTG keywords (Flying, Vigilance, Lifelink, Haste) have name and reminder text")
    void testCoreKeywordsLocalization() {
        List<String> keywords = List.of("flying", "vigilance", "lifelink", "haste", "deathtouch");
        for (String kw : keywords) {
            assertTrue(langMap.containsKey("mtg.keyword." + kw + ".name"), "Missing name for keyword " + kw);
            assertTrue(langMap.containsKey("mtg.keyword." + kw + ".reminder"), "Missing reminder for keyword " + kw);
        }
    }

    @Test
    @DisplayName("4. All 5 MTG mana colors have localized name and symbol tokens")
    void testManaColorsLocalization() {
        List<String> colors = List.of("white", "blue", "black", "red", "green", "colorless");
        for (String c : colors) {
            assertTrue(langMap.containsKey("mtg.mana." + c + ".name"), "Missing name for color " + c);
            assertTrue(langMap.containsKey("mtg.mana.symbol." + c), "Missing symbol for color " + c);
        }
    }

    @Test
    @DisplayName("5. Guidebook chapter titles are externalized in en_us.json")
    void testGuidebookChaptersLocalization() {
        List<String> chapters = List.of("mana", "cards", "deck", "progression", "invocations", "world");
        for (String ch : chapters) {
            assertTrue(langMap.containsKey("mtg.guidebook.chapter." + ch), "Missing guidebook title for chapter " + ch);
        }
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }
}
