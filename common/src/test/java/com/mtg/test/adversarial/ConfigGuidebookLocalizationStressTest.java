package com.mtg.test.adversarial;

import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.config.*;
import com.mtg.config.toml.TomlWriter;
import com.mtg.domain.CardDefinition;
import com.mtg.guidebook.*;
import com.mtg.guidebook.catalog.DefaultJournalContent;
import com.mtg.guidebook.format.FormattedTextSpan;
import com.mtg.guidebook.format.ManaSymbolFormatter;
import com.mtg.guidebook.format.SpanType;
import com.mtg.network.S2CSyncConfigPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Stress Suite — Config Sync Fuzzing, Guidebook Robustness & i18n Completeness")
public class ConfigGuidebookLocalizationStressTest {

    @AfterEach
    void tearDown() {
        MtgConfig.resetToDefaults();
    }

    // =========================================================================
    // 1. S2CSyncConfigPacket Adversarial Fuzzing & Boundary Stress
    // =========================================================================

    @Nested
    @DisplayName("1. S2CSyncConfigPacket Adversarial Fuzzing")
    class PacketFuzzingTests {

        @Test
        @DisplayName("Packet Constructor: null parameters safely fallback to default configs")
        void testNullConstructorFallbacks() {
            S2CSyncConfigPacket packet = new S2CSyncConfigPacket(null, null, null, null);

            assertNotNull(packet.manaConfig(), "ManaConfig must fallback to defaults");
            assertNotNull(packet.cardsConfig(), "CardsConfig must fallback to defaults");
            assertNotNull(packet.progressionConfig(), "ProgressionConfig must fallback to defaults");
            assertNotNull(packet.worldGenConfig(), "WorldGenConfig must fallback to defaults");

            assertEquals(100, packet.manaConfig().biomeRegenIntervalTicks());
            assertEquals(1.0, packet.cardsConfig().cardCooldownMultiplier());
            assertEquals(4, packet.progressionConfig().initialHandCapacity());
            assertEquals(0.02, packet.worldGenConfig().manaNodeChunkChance());
        }

        @Test
        @DisplayName("Extreme Out-of-Bounds Clamping: negative ticks, massive numbers and out-of-range rates")
        void testExtremeValuesClamping() {
            Map<String, Double> extremeMultipliers = new HashMap<>();
            extremeMultipliers.put("minecraft:desert", 1000.0);
            extremeMultipliers.put("minecraft:ocean", -99.0);
            extremeMultipliers.put("minecraft:void", 0.0);

            ManaConfig extremeMana = new ManaConfig(
                    -100, 500, -10, -500, -5,
                    -1, 10000, -50, 100.0,
                    -10.0, false,
                    ManaConfig.defaultAffinities(),
                    extremeMultipliers
            );

            CardsConfig extremeCards = new CardsConfig(
                    -50.0, -10.0, -5.0, -1.0,
                    false, -10.0, false, -5.0,
                    -10, false, -50, -100
            );

            ProgressionConfig extremeProg = new ProgressionConfig(
                    -10, -5, -20, -5.0,
                    -1, -1, -1, -1,
                    -5.0, 100.0, -1.0, 50.0,
                    -5, -1
            );

            WorldGenConfig extremeWorld = new WorldGenConfig(
                    false, -5.0, -10,
                    false, -50, -20,
                    false, -1.0,
                    false, -1.0,
                    -5.0, -5.0,
                    false, -10, -5
            );

            S2CSyncConfigPacket packet = new S2CSyncConfigPacket(extremeMana, extremeCards, extremeProg, extremeWorld);

            // Verify constructor sanitization
            assertEquals(20, packet.manaConfig().biomeRegenIntervalTicks(), "biomeRegenIntervalTicks clamped to min 20");
            assertEquals(20, packet.manaConfig().baseRegenAmount(), "baseRegenAmount clamped to max 20");
            assertEquals(0, packet.manaConfig().landManaBonus(), "landManaBonus clamped to min 0");
            assertEquals(1, packet.manaConfig().initialMaxMana(), "initialMaxMana clamped to min 1");
            assertEquals(100, packet.manaConfig().hardCapMaxMana(), "hardCapMaxMana clamped to max 100");
            assertEquals(10.0, packet.manaConfig().getBiomeMultiplier("minecraft:desert"), "Biome mult clamped to 10.0");
            assertEquals(0.1, packet.manaConfig().getBiomeMultiplier("minecraft:ocean"), "Biome mult clamped to 0.1");

            assertEquals(0.05, packet.cardsConfig().cardCooldownMultiplier(), "cardCooldownMultiplier clamped to min 0.05");
            assertEquals(5.0, packet.cardsConfig().landCooldownSeconds(), "landCooldownSeconds clamped to min 5.0");
            assertEquals(0.5, packet.cardsConfig().instantBaseCooldownSeconds(), "instantBaseCooldownSeconds clamped to min 0.5");
            assertEquals(0.0, packet.cardsConfig().duplicateCooldownReduction(), "duplicateCooldownReduction clamped to min 0.0");
            assertEquals(0.1, packet.cardsConfig().sorceryDelayPerManaSeconds(), "sorceryDelayPerManaSeconds clamped to min 0.1");
            assertEquals(0.5, packet.cardsConfig().damageInterruptThreshold(), "damageInterruptThreshold clamped to min 0.5");
            assertEquals(1, packet.cardsConfig().maxActiveSummonsPerPlayer(), "maxActiveSummonsPerPlayer clamped to min 1");
            assertEquals(16, packet.cardsConfig().summonLeashDistanceBlocks(), "summonLeashDistanceBlocks clamped to min 16");
            assertEquals(100, packet.cardsConfig().enchantmentBaseDurationTicks(), "enchantmentBaseDurationTicks clamped to min 100");

            assertEquals(1, packet.progressionConfig().initialHandCapacity(), "initialHandCapacity clamped to min 1");
            assertEquals(4, packet.progressionConfig().maxHandCapacity(), "maxHandCapacity clamped to min 4");
            assertEquals(5, packet.progressionConfig().handUpgradeBaseXpCost(), "handUpgradeBaseXpCost clamped to min 5");
            assertEquals(1.0, packet.progressionConfig().handUpgradeXpMultiplier(), "handUpgradeXpMultiplier clamped to min 1.0");
            assertEquals(2, packet.progressionConfig().thresholdCommonToUncommon(), "threshold clamped to min 2");
            assertEquals(0.0, packet.progressionConfig().miningCardDropChance(), "miningCardDropChance clamped to min 0.0");
            assertEquals(1.0, packet.progressionConfig().farmingCardDropChance(), "farmingCardDropChance clamped to max 1.0");
            assertEquals(0, packet.progressionConfig().dungeonChestCardCount(), "dungeonChestCardCount clamped to min 0");
            assertEquals(1, packet.progressionConfig().starterPackCardCount(), "starterPackCardCount clamped to min 1");

            assertEquals(0.001, packet.worldGenConfig().manaNodeChunkChance(), "manaNodeChunkChance clamped to min 0.001");
            assertEquals(1, packet.worldGenConfig().manaNodePulseAmount(), "manaNodePulseAmount clamped to min 1");
            assertEquals(16, packet.worldGenConfig().altarSpacingChunks(), "altarSpacingChunks clamped to min 16");
            assertEquals(4, packet.worldGenConfig().altarSeparationChunks(), "altarSeparationChunks clamped to min 4");
            assertEquals(0.001, packet.worldGenConfig().customMobSpawnRate(), "customMobSpawnRate clamped to min 0.001");
            assertEquals(0.0001, packet.worldGenConfig().bossSpawnRate(), "bossSpawnRate clamped to min 0.0001");
            assertEquals(0.2, packet.worldGenConfig().bossHealthMultiplier(), "bossHealthMultiplier clamped to min 0.2");
            assertEquals(0.2, packet.worldGenConfig().bossDamageMultiplier(), "bossDamageMultiplier clamped to min 0.2");
            assertEquals(1, packet.worldGenConfig().poisonLethalThreshold(), "poisonLethalThreshold clamped to min 1");
            assertEquals(1, packet.worldGenConfig().corruptedThreshold(), "corruptedThreshold clamped to min 1");

            // Encode and round-trip
            byte[] encoded = packet.encode();
            S2CSyncConfigPacket decoded = S2CSyncConfigPacket.decode(encoded);

            assertEquals(packet.manaConfig().biomeRegenIntervalTicks(), decoded.manaConfig().biomeRegenIntervalTicks());
            assertEquals(packet.cardsConfig().cardCooldownMultiplier(), decoded.cardsConfig().cardCooldownMultiplier());
            assertEquals(packet.progressionConfig().initialHandCapacity(), decoded.progressionConfig().initialHandCapacity());
            assertEquals(packet.worldGenConfig().manaNodeChunkChance(), decoded.worldGenConfig().manaNodeChunkChance());
        }

        @Test
        @DisplayName("Corrupted TOML Payload Strings: Syntax errors fall back to default configs per section")
        void testCorruptedTomlPayloadsFallbackToDefaults() throws IOException {
            byte[] rawBytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {

                // Mana section: corrupted TOML syntax
                writeToml(dos, "broken_syntax_without_equals\n[unclosed_section");
                // Cards section: completely empty string
                writeToml(dos, "");
                // Progression section: invalid value types and NaN
                writeToml(dos, "initial_hand_capacity = \"not_a_number\"\nthreshold_common_to_uncommon = NaN");
                // WorldGen section: whitespace and comments only
                writeToml(dos, "# Just a comment line\n   \n\t");

                dos.flush();
                rawBytes = baos.toByteArray();
            }

            S2CSyncConfigPacket decoded = assertDoesNotThrow(() -> S2CSyncConfigPacket.decode(rawBytes),
                    "decode must not throw unhandled exception on corrupted TOML strings");

            // Verify fallbacks to defaults
            assertEquals(ManaConfig.defaults().biomeRegenIntervalTicks(), decoded.manaConfig().biomeRegenIntervalTicks());
            assertEquals(CardsConfig.defaults().cardCooldownMultiplier(), decoded.cardsConfig().cardCooldownMultiplier());
            assertEquals(ProgressionConfig.defaults().initialHandCapacity(), decoded.progressionConfig().initialHandCapacity());
            assertEquals(WorldGenConfig.defaults().manaNodeChunkChance(), decoded.worldGenConfig().manaNodeChunkChance());
        }

        @Test
        @DisplayName("Buffer Overflow & Truncation Attempts: Rejects invalid lengths and EOF gracefully")
        void testBufferOverflowAndTruncationRejections() {
            // Case A: Null input
            assertThrows(NullPointerException.class, () -> S2CSyncConfigPacket.decode(null));

            // Case B: Truncated empty byte array
            assertThrows(UncheckedIOException.class, () -> S2CSyncConfigPacket.decode(new byte[0]));

            // Case C: Partial length header (fewer than 4 bytes)
            assertThrows(UncheckedIOException.class, () -> S2CSyncConfigPacket.decode(new byte[]{0, 0, 1}));

            // Case D: Malicious negative length
            byte[] negativeLengthBytes = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            assertThrows(UncheckedIOException.class, () -> S2CSyncConfigPacket.decode(negativeLengthBytes));

            // Case E: Massive length attempt (> 1,000,000 bytes) to trigger OutOfMemory
            byte[] massiveLengthBytes = new byte[]{0x00, 0x1E, (byte) 0x84, (byte) 0x81}; // 2,000,001
            assertThrows(UncheckedIOException.class, () -> S2CSyncConfigPacket.decode(massiveLengthBytes));

            // Case F: Length claims 100 bytes but stream terminates immediately
            byte[] eofBytes = new byte[]{0x00, 0x00, 0x00, 0x64, 0x01, 0x02};
            assertThrows(UncheckedIOException.class, () -> S2CSyncConfigPacket.decode(eofBytes));
        }

        @Test
        @DisplayName("Client Handle Execution: Authoritative server config overrides client singletons")
        void testClientHandleExecution() {
            ManaConfig serverMana = new ManaConfig(
                    35, 2, 2, 120, 1,
                    8, 25, 4, 1.2,
                    2.0, false,
                    ManaConfig.defaultAffinities()
            );

            S2CSyncConfigPacket packet = new S2CSyncConfigPacket(
                    serverMana,
                    CardsConfig.defaults(),
                    ProgressionConfig.defaults(),
                    WorldGenConfig.defaults()
            );

            packet.handle(null);

            assertEquals(35, MtgConfig.getMana().biomeRegenIntervalTicks());
            assertEquals(8, MtgConfig.getMana().initialMaxMana());
        }

        private static void writeToml(DataOutputStream dos, String toml) throws IOException {
            byte[] bytes = toml.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }

    // =========================================================================
    // 2. ManaSymbolFormatter & Guidebook Search Stress
    // =========================================================================

    @Nested
    @DisplayName("2. ManaSymbolFormatter & Guidebook Search Stress")
    class GuidebookStressTests {

        @Test
        @DisplayName("Malformed Mana Tokens: Nested braces, unclosed tokens, empty braces and invalid glyphs")
        void testMalformedManaTokens() {
            // Nested braces: {{W}} -> outer '{' and '}' plain text, inner '{W}' is symbol
            List<FormattedTextSpan> spans = ManaSymbolFormatter.format("{{W}}");
            assertEquals(3, spans.size());
            assertEquals("{", spans.get(0).text());
            assertEquals(SpanType.TEXT, spans.get(0).type());
            assertEquals("{W}", spans.get(1).text());
            assertEquals(SpanType.MANA_SYMBOL, spans.get(1).type());
            assertEquals("}", spans.get(2).text());
            assertEquals(SpanType.TEXT, spans.get(2).type());

            // Unclosed token: {W -> plain text
            List<FormattedTextSpan> unclosed = ManaSymbolFormatter.format("Cost is {W without closing brace");
            assertEquals(1, unclosed.size());
            assertEquals("Cost is {W without closing brace", unclosed.get(0).text());
            assertEquals(SpanType.TEXT, unclosed.get(0).type());

            // Empty braces: {} -> plain text
            List<FormattedTextSpan> empty = ManaSymbolFormatter.format("Empty token {} test");
            assertEquals(1, empty.size());
            assertEquals("Empty token {} test", empty.get(0).text());
            assertEquals(SpanType.TEXT, empty.get(0).type());

            // Invalid color glyph: {Z} -> plain text
            List<FormattedTextSpan> invalidGlyph = ManaSymbolFormatter.format("Invalid {Z} symbol");
            assertEquals(1, invalidGlyph.size());
            assertEquals("Invalid {Z} symbol", invalidGlyph.get(0).text());
            assertEquals(SpanType.TEXT, invalidGlyph.get(0).type());

            // Deeply nested braces: {{{2}{U}}}
            List<FormattedTextSpan> deep = ManaSymbolFormatter.format("{{{2}{U}}}");
            assertTrue(deep.stream().anyMatch(s -> s.type() == SpanType.MANA_SYMBOL && s.text().equals("{2}")));
            assertTrue(deep.stream().anyMatch(s -> s.type() == SpanType.MANA_SYMBOL && s.text().equals("{U}")));

            // Hybrid and phyrexian tokens: {W/P}, {2/R}
            List<FormattedTextSpan> phyrexian = ManaSymbolFormatter.format("Pay {W/P} or {2/R}");
            assertTrue(phyrexian.stream().anyMatch(s -> s.type() == SpanType.MANA_SYMBOL && s.text().equals("{W/P}")));
        }

        @Test
        @DisplayName("Malformed Card Links & Keywords: Unclosed brackets, empty payloads, and pipe variants")
        void testMalformedCardLinksAndKeywords() {
            // Empty card payload: [card:] -> plain text
            List<FormattedTextSpan> emptyCard = ManaSymbolFormatter.format("Link [card:] here");
            assertEquals(1, emptyCard.size());
            assertEquals("Link [card:] here", emptyCard.get(0).text());

            // Unclosed card link: [card:mtg:serra_angel -> plain text
            List<FormattedTextSpan> unclosedCard = ManaSymbolFormatter.format("Unclosed [card:mtg:serra_angel card");
            assertEquals(1, unclosedCard.size());
            assertEquals("Unclosed [card:mtg:serra_angel card", unclosedCard.get(0).text());

            // Valid card link with title: [card:mtg:base/lightning_bolt|Lightning Bolt]
            List<FormattedTextSpan> validCardWithTitle = ManaSymbolFormatter.format("Cast [card:mtg:base/lightning_bolt|Lightning Bolt] now");
            FormattedTextSpan cardSpan = validCardWithTitle.stream()
                    .filter(s -> s.type() == SpanType.CARD_LINK)
                    .findFirst()
                    .orElseThrow();
            assertEquals("Lightning Bolt", cardSpan.text());
            assertEquals("mtg:base/lightning_bolt", cardSpan.data());

            // Valid keyword: [kw:Flying] and [keyword:First Strike]
            List<FormattedTextSpan> kwSpans = ManaSymbolFormatter.format("Features [kw:Flying] and [keyword:First Strike]");
            assertTrue(kwSpans.stream().anyMatch(s -> s.type() == SpanType.KEYWORD_HIGHLIGHT && s.text().equals("Flying")));
            assertTrue(kwSpans.stream().anyMatch(s -> s.type() == SpanType.KEYWORD_HIGHLIGHT && s.text().equals("First Strike")));

            // Empty input
            assertTrue(ManaSymbolFormatter.format(null).isEmpty());
            assertTrue(ManaSymbolFormatter.format("").isEmpty());
        }

        @Test
        @DisplayName("Guidebook Search: Empty strings, Unicode, 10,000-char deep queries, and case insensitivity")
        void testGuidebookSearchStress() {
            JournalBookModel model = DefaultJournalContent.createDefaultJournal();

            // Null and empty queries return empty list immediately
            assertTrue(model.search(null).isEmpty());
            assertTrue(model.search("").isEmpty());
            assertTrue(model.search("   ").isEmpty());
            assertTrue(model.search("\t\n").isEmpty());

            // Deep query: 10,000 characters search query
            String deepQuery = "a".repeat(10_000);
            assertDoesNotThrow(() -> {
                List<JournalEntry> results = model.search(deepQuery);
                assertTrue(results.isEmpty());
            }, "10,000 character search query must complete cleanly without StackOverflow");

            // Unicode search queries
            assertDoesNotThrow(() -> {
                List<JournalEntry> results = model.search("完成化プレインズウォーカー🌲☀️💀🌋🌊");
                assertTrue(results.isEmpty());
            }, "Unicode characters must parse and search cleanly");

            // Case-insensitivity verification
            List<JournalEntry> lowerResults = model.search("mana");
            List<JournalEntry> upperResults = model.search("MANA");
            List<JournalEntry> mixedResults = model.search("mAnA");

            assertFalse(lowerResults.isEmpty(), "Search for 'mana' must find entries");
            assertEquals(lowerResults.size(), upperResults.size(), "Search must be strictly case-insensitive");
            assertEquals(lowerResults.size(), mixedResults.size());

            // Search by card ID
            List<JournalEntry> cardEntries = model.findEntriesByCardId("mtg:base/lightning_bolt");
            assertNotNull(cardEntries);
            assertTrue(model.findEntriesByCardId("").isEmpty());
            assertTrue(model.findEntriesByCardId(null).isEmpty());
        }

        @Test
        @DisplayName("Memory Efficiency & Immutable Data Structures: Journal collections reject mutation")
        void testJournalImmutability() {
            JournalBookModel model = DefaultJournalContent.createDefaultJournal();

            // Model chapters immutability
            List<JournalChapter> chapters = model.getChapters();
            assertThrows(UnsupportedOperationException.class, () -> chapters.add(null));

            // Search results immutability
            List<JournalEntry> searchResults = model.search("mana");
            assertThrows(UnsupportedOperationException.class, () -> searchResults.add(null));

            // Card lookup immutability
            List<JournalEntry> cardResults = model.findEntriesByCardId("mtg:base/lightning_bolt");
            assertThrows(UnsupportedOperationException.class, () -> cardResults.add(null));

            // Chapter entries immutability
            JournalChapter chapter = chapters.get(0);
            assertThrows(UnsupportedOperationException.class, () -> chapter.entries().add(null));

            // Entry pages immutability
            JournalEntry entry = chapter.entries().get(0);
            assertThrows(UnsupportedOperationException.class, () -> entry.pages().add(null));

            // Page embeddedCardIds and metadata immutability
            JournalPage page = entry.pages().get(0);
            assertThrows(UnsupportedOperationException.class, () -> page.embeddedCardIds().add("hacked"));
            assertThrows(UnsupportedOperationException.class, () -> page.metadata().put("key", "val"));
        }

        @Test
        @DisplayName("Thread-Safety: Concurrent multi-threaded reads and searches on JournalBookModel")
        void testConcurrentGuidebookAccess() throws InterruptedException, ExecutionException {
            JournalBookModel model = DefaultJournalContent.createDefaultJournal();
            int threadCount = 16;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Callable<Boolean>> tasks = new ArrayList<>();

            for (int i = 0; i < 50; i++) {
                final int idx = i;
                tasks.add(() -> {
                    String query = (idx % 2 == 0) ? "mana" : "cards";
                    List<JournalEntry> hits = model.search(query);
                    assertFalse(hits.isEmpty());

                    List<JournalChapter> ch = model.getChapters();
                    assertEquals(6, ch.size());

                    Optional<JournalChapter> c = model.getChapter("mana");
                    assertTrue(c.isPresent());

                    return true;
                });
            }

            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            for (Future<Boolean> future : futures) {
                assertTrue(future.get());
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    // =========================================================================
    // 3. Localization Completeness Audit
    // =========================================================================

    @Nested
    @DisplayName("3. Localization Completeness Audit")
    class LocalizationAuditTests {

        private static Map<String, String> langMap;

        @BeforeAll
        static void loadLangMap() throws IOException {
            try (InputStream stream = ConfigGuidebookLocalizationStressTest.class.getResourceAsStream("/assets/mtg/lang/en_us.json")) {
                assertNotNull(stream, "en_us.json must exist in classpath");
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                langMap = parseJson(json);
                assertFalse(langMap.isEmpty());
            }
        }

        @Test
        @DisplayName("100% of registered cards in Base Library have non-blank name, type, and text entries")
        void testRegisteredCardsCompleteness() {
            List<CardDefinition> allCards = new ArrayList<>(BasicLandsCatalog.getAllCards());

            assertFalse(allCards.isEmpty(), "Card catalog must contain registered cards");

            List<String> violations = new ArrayList<>();

            for (CardDefinition card : allCards) {
                String id = card.id();
                String raw = id.replace("mtg:", "");
                String[] parts = raw.split("/");
                String prefix = "mtg.card." + parts[0] + "." + parts[1];

                String nameKey = prefix + ".name";
                String typeKey = prefix + ".type";
                String textKey = prefix + ".text";

                checkNonBlank(nameKey, violations);
                checkNonBlank(typeKey, violations);
                checkNonBlank(textKey, violations);
            }

            assertTrue(violations.isEmpty(), "Card localization violations detected:\n" + String.join("\n", violations));
        }

        @Test
        @DisplayName("All 22 spell cast outcome keys (mtg.cast.*) are defined and non-blank")
        void testSpellCastOutcomeKeys() {
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

            List<String> missing = new ArrayList<>();
            for (String key : castKeys) {
                if (!langMap.containsKey(key) || langMap.get(key).trim().isEmpty()) {
                    missing.add("Missing/empty cast key: " + key);
                }
            }

            assertTrue(missing.isEmpty(), "Missing spell cast keys:\n" + String.join("\n", missing));
        }

        @Test
        @DisplayName("All Mythic World Boss entities are defined in entity.mtg.* and non-blank")
        void testBossEntitiesDefined() {
            String[] bossEntities = {
                "entity.mtg.shivan_dragon",
                "entity.mtg.demon_of_deaths_gate",
                "entity.mtg.serra_avatar",
                "entity.mtg.phyrexian_obliterator"
            };

            for (String entity : bossEntities) {
                assertTrue(langMap.containsKey(entity), "Missing boss entity key: " + entity);
                assertFalse(langMap.get(entity).trim().isEmpty(), "Boss entity key must not be blank: " + entity);
            }
        }

        @Test
        @DisplayName("All 6 Guidebook chapters have non-blank titles and subtitles")
        void testGuidebookChaptersDefined() {
            String[] chapters = { "mana", "cards", "deck", "progression", "invocations", "world" };

            for (String ch : chapters) {
                String title = "mtg.guidebook.chapter." + ch;
                String sub = "mtg.guidebook.chapter." + ch + ".subtitle";

                assertTrue(langMap.containsKey(title), "Missing chapter title: " + title);
                assertFalse(langMap.get(title).trim().isEmpty(), "Chapter title must not be blank: " + title);

                assertTrue(langMap.containsKey(sub), "Missing chapter subtitle: " + sub);
                assertFalse(langMap.get(sub).trim().isEmpty(), "Chapter subtitle must not be blank: " + sub);
            }
        }

        private static void checkNonBlank(String key, List<String> violations) {
            if (!langMap.containsKey(key)) {
                violations.add("Missing key: " + key);
            } else if (langMap.get(key).trim().isEmpty()) {
                violations.add("Blank translation value for key: " + key);
            }
        }

        private static Map<String, String> parseJson(String json) {
            Map<String, String> map = new LinkedHashMap<>();
            Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
            Matcher m = p.matcher(json);
            while (m.find()) {
                map.put(m.group(1), m.group(2));
            }
            return map;
        }
    }
}
