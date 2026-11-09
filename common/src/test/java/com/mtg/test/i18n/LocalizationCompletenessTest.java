package com.mtg.test.i18n;

import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.domain.CardDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Comprehensive Localization Completeness & Audit Suite")
public class LocalizationCompletenessTest {

    private static Map<String, String> langMap;

    @BeforeAll
    static void loadLanguageFile() throws Exception {
        InputStream stream = LocalizationCompletenessTest.class.getResourceAsStream("/assets/mtg/lang/en_us.json");
        assertNotNull(stream, "en_us.json must be present in classpath");

        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        langMap = parseSimpleJson(json);
        assertFalse(langMap.isEmpty(), "Localization map must not be empty");
    }

    @Test
    @DisplayName("100% of registered cards in Base Library have name, type, and text entries")
    void testAllRegisteredCardsHaveCompleteKeys() {
        List<CardDefinition> allCards = new ArrayList<>(BasicLandsCatalog.getAllCards());

        List<String> missingKeys = new ArrayList<>();

        for (CardDefinition card : allCards) {
            String id = card.id(); // e.g. "mtg:base/lightning_bolt" or "mtg:znr/lotus_cobra"
            String keyPrefix = cardIdToLangPrefix(id);

            String nameKey = keyPrefix + ".name";
            String typeKey = keyPrefix + ".type";
            String textKey = keyPrefix + ".text";

            if (!langMap.containsKey(nameKey)) missingKeys.add("Missing name key: " + nameKey);
            if (!langMap.containsKey(typeKey)) missingKeys.add("Missing type key: " + typeKey);
            if (!langMap.containsKey(textKey)) missingKeys.add("Missing text key: " + textKey);
        }

        assertTrue(missingKeys.isEmpty(), "Registered cards have missing localization keys:\n" + String.join("\n", missingKeys));
    }

    @Test
    @DisplayName("Modal Double-Faced Card (MDFC) reverse face Kazandu Valley has complete localization entries")
    void testMdfcReverseFaceLocalization() {
        assertTrue(langMap.containsKey("mtg.card.znr.kazandu_valley.name"));
        assertTrue(langMap.containsKey("mtg.card.znr.kazandu_valley.type"));
        assertTrue(langMap.containsKey("mtg.card.znr.kazandu_valley.text"));
        assertTrue(langMap.containsKey("mtg.card.znr.kazandu_valley.flavor"));
    }

    @Test
    @DisplayName("All 21 spell cast outcome keys (mtg.cast.*) are defined and non-empty")
    void testSpellCastOutcomeKeysPresent() {
        String[] castKeys = {
            "mtg.cast.plains.success",
            "mtg.cast.island.success",
            "mtg.cast.swamp.success",
            "mtg.cast.mountain.success",
            "mtg.cast.forest.success",
            "mtg.cast.wastes.success",
            "mtg.cast.giant_growth.success",
            "mtg.cast.lightning_bolt.success",
            "mtg.cast.counterspell.success",
            "mtg.cast.unsummon.success",
            "mtg.cast.dark_ritual.success",
            "mtg.cast.healing_salve.success",
            "mtg.cast.wrath_of_god.success",
            "mtg.cast.divination.success",
            "mtg.cast.mind_rot.success",
            "mtg.cast.pyroclasm.success",
            "mtg.cast.glorious_anthem.success",
            "mtg.cast.phyrexian_arena.success",
            "mtg.cast.rancor.success",
            "mtg.cast.levitation.success",
            "mtg.cast.creature.success",
            "mtg.cast.success"
        };

        for (String key : castKeys) {
            assertTrue(langMap.containsKey(key), "Missing spell cast outcome key: " + key);
            assertFalse(langMap.get(key).isBlank(), "Spell cast outcome key must not be blank: " + key);
        }
    }

    @Test
    @DisplayName("All Mythic World Boss entities are defined in entity.mtg.*")
    void testBossEntitiesPresent() {
        String[] bossEntities = {
            "entity.mtg.shivan_dragon",
            "entity.mtg.demon_of_deaths_gate",
            "entity.mtg.serra_avatar",
            "entity.mtg.phyrexian_obliterator"
        };

        for (String entity : bossEntities) {
            assertTrue(langMap.containsKey(entity), "Missing boss entity key: " + entity);
        }
    }

    @Test
    @DisplayName("All 6 Guidebook chapters and subtitles are defined in mtg.guidebook.chapter.*")
    void testGuidebookChaptersPresent() {
        String[] chapters = { "mana", "cards", "deck", "progression", "invocations", "world" };

        for (String chapter : chapters) {
            String titleKey = "mtg.guidebook.chapter." + chapter;
            String subtitleKey = "mtg.guidebook.chapter." + chapter + ".subtitle";
            assertTrue(langMap.containsKey(titleKey), "Missing guidebook chapter title key: " + titleKey);
            assertTrue(langMap.containsKey(subtitleKey), "Missing guidebook chapter subtitle key: " + subtitleKey);
        }
    }

    private static String cardIdToLangPrefix(String cardId) {
        // e.g. "mtg:base/lightning_bolt" -> "mtg.card.base.lightning_bolt"
        // e.g. "mtg:znr/lotus_cobra" -> "mtg.card.znr.lotus_cobra"
        // e.g. "mtg:one/venerated_rotpriest" -> "mtg.card.one.venerated_rotpriest"
        String raw = cardId.replace("mtg:", "");
        String[] parts = raw.split("/");
        return "mtg.card." + parts[0] + "." + parts[1];
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }
}
