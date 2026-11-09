package com.mtg.test.i18n;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("English-First Localization (en_us.json) Integrity Suite")
public class LocalizationIntegrityTest {

    private static Map<String, String> langMap;

    @BeforeAll
    static void loadLanguageFile() throws Exception {
        InputStream stream = LocalizationIntegrityTest.class.getResourceAsStream("/assets/mtg/lang/en_us.json");
        assertNotNull(stream, "en_us.json must be present in classpath at /assets/mtg/lang/en_us.json");

        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertNotNull(json);
        assertFalse(json.trim().isEmpty());

        langMap = parseSimpleJson(json);
        assertFalse(langMap.isEmpty(), "Localization map must contain translated keys");
    }

    @Test
    @DisplayName("Verify en_us.json is valid and contains essential entries")
    void testJsonFileValidAndParsable() {
        assertTrue(langMap.containsKey("itemGroup.mtg"));
        assertTrue(langMap.containsKey("mod.mtg.name"));
        assertTrue(langMap.containsKey("mod.mtg.description"));
        assertTrue(langMap.size() >= 50, "en_us.json should contain comprehensive keys (found " + langMap.size() + ")");
    }

    @Test
    @DisplayName("Verify all required key taxonomy categories are present")
    void testRequiredCategoriesPresent() {
        String[] requiredPrefixes = {
            "itemGroup.mtg",
            "item.mtg.",
            "block.mtg.",
            "entity.mtg.",
            "mtg.mana.",
            "mtg.stat.",
            "mtg.keyword.",
            "mtg.card.",
            "mtg.gui.",
            "mtg.guidebook.",
            "death.attack.mtg."
        };

        for (String prefix : requiredPrefixes) {
            boolean hasCategory = langMap.keySet().stream().anyMatch(k -> k.startsWith(prefix));
            assertTrue(hasCategory, "en_us.json must contain keys with prefix: " + prefix);
        }
    }

    @Test
    @DisplayName("Verify keyword mechanic keys are present for evergreen, ZNR, and ONE keywords")
    void testKeywordMechanicsPresent() {
        String[] keywords = {
            "flying", "deathtouch", "lifelink", "trample", "haste", "vigilance",
            "first_strike", "hexproof", "landfall", "kicker", "toxic", "poison",
            "corrupted", "proliferate"
        };

        for (String kw : keywords) {
            String nameKey = "mtg.keyword." + kw + ".name";
            assertTrue(langMap.containsKey(nameKey), "Missing keyword name key: " + nameKey);
        }
    }

    @Test
    @DisplayName("Verify base and expansion cards have complete name, type, and text entries")
    void testCardKeysPresent() {
        String[] cards = {
            "mtg.card.base.plains",
            "mtg.card.base.lightning_bolt",
            "mtg.card.base.wrath_of_god",
            "mtg.card.base.serra_angel",
            "mtg.card.znr.lotus_cobra",
            "mtg.card.znr.roost_of_drakes",
            "mtg.card.one.venerated_rotpriest",
            "mtg.card.one.drown_in_ichor"
        };

        for (String card : cards) {
            assertTrue(langMap.containsKey(card + ".name"), "Card missing .name: " + card);
            assertTrue(langMap.containsKey(card + ".type"), "Card missing .type: " + card);
            assertTrue(langMap.containsKey(card + ".text"), "Card missing .text: " + card);
        }
    }

    @Test
    @DisplayName("Verify string format specifiers match expected arguments without errors")
    void testFormatSpecifiersMatch() {
        // String placeholders
        String manaInsufficient = langMap.get("mtg.mana.insufficient");
        assertNotNull(manaInsufficient);
        assertDoesNotThrow(() -> String.format(manaInsufficient, "Lightning Bolt", "{R}"));

        // Integer placeholders
        String manaCap = langMap.get("mtg.mana.cap_increased");
        assertNotNull(manaCap);
        assertDoesNotThrow(() -> String.format(manaCap, 10));

        // Float placeholders
        String cooldown = langMap.get("mtg.tooltip.cooldown");
        assertNotNull(cooldown);
        assertDoesNotThrow(() -> String.format(cooldown, 3.5f));

        // Death message positional placeholders
        String deathMsg = langMap.get("death.attack.mtg.spell_damage");
        assertNotNull(deathMsg);
        assertDoesNotThrow(() -> String.format(deathMsg, "Player", "Opponent", "Lightning Bolt"));
    }

    // Simple JSON parser for flat key-value strings
    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2).replace("\\\"", "\"").replace("\\n", "\n");
            result.put(key, val);
        }
        return result;
    }
}
