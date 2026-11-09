package com.mtg.domain;

import com.mtg.api.keyword.KeywordCategory;
import com.mtg.api.keyword.KeywordDefinition;

import java.util.*;

public record CardDefinition(
    String id,
    String setCode,
    String name,
    ManaCost manaCost,
    Set<CardType> types,
    Set<String> subtypes,
    CardRarity rarity,
    Optional<CombatStats> combatStats,
    List<KeywordInstance> keywords,
    int baseCooldownTicks,
    int windupDelayTicks
) {
    public CardDefinition {
        Objects.requireNonNull(id, "Card ID cannot be null");
        Objects.requireNonNull(setCode, "Set code cannot be null");
        Objects.requireNonNull(name, "Card name cannot be null");
        Objects.requireNonNull(manaCost, "Mana cost cannot be null");
        Objects.requireNonNull(rarity, "Card rarity cannot be null");
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("Card must have at least one CardType");
        }
        if (baseCooldownTicks < 0) {
            throw new IllegalArgumentException("Base cooldown ticks cannot be negative");
        }
        if (windupDelayTicks < 0) {
            throw new IllegalArgumentException("Windup delay ticks cannot be negative");
        }

        types = Set.copyOf(types);
        subtypes = subtypes == null ? Set.of() : Set.copyOf(subtypes);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        if (combatStats == null) {
            combatStats = Optional.empty();
        }
    }

    public boolean isCreature() { return types.contains(CardType.CREATURE); }
    public boolean isLand() { return types.contains(CardType.LAND); }
    public boolean isInstant() { return types.contains(CardType.INSTANT); }
    public boolean isSorcery() { return types.contains(CardType.SORCERY); }
    public boolean isEnchantment() { return types.contains(CardType.ENCHANTMENT); }

    public boolean hasKeyword(String keywordId) {
        return keywords.stream().anyMatch(k -> k.getKeywordId().equals(keywordId));
    }

    public int getKeywordValue(String keywordId) {
        return keywords.stream()
            .filter(k -> k.getKeywordId().equals(keywordId))
            .findFirst()
            .map(KeywordInstance::parameterValue)
            .orElse(0);
    }

    public static Builder builder(String id, String setCode, String name) {
        return new Builder(id, setCode, name);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(escapeJson(id)).append("\",\n");
        sb.append("  \"setCode\": \"").append(escapeJson(setCode)).append("\",\n");
        sb.append("  \"name\": \"").append(escapeJson(name)).append("\",\n");
        sb.append("  \"manaCost\": {\n");
        sb.append("    \"generic\": ").append(manaCost.generic()).append(",\n");
        sb.append("    \"white\": ").append(manaCost.white()).append(",\n");
        sb.append("    \"blue\": ").append(manaCost.blue()).append(",\n");
        sb.append("    \"black\": ").append(manaCost.black()).append(",\n");
        sb.append("    \"red\": ").append(manaCost.red()).append(",\n");
        sb.append("    \"green\": ").append(manaCost.green()).append(",\n");
        sb.append("    \"colorless\": ").append(manaCost.colorless()).append(",\n");
        sb.append("    \"phyrexian\": {\n");
        PhyrexianManaCost p = manaCost.phyrexian();
        sb.append("      \"white\": ").append(p.whitePhyrexian()).append(",\n");
        sb.append("      \"blue\": ").append(p.bluePhyrexian()).append(",\n");
        sb.append("      \"black\": ").append(p.blackPhyrexian()).append(",\n");
        sb.append("      \"red\": ").append(p.redPhyrexian()).append(",\n");
        sb.append("      \"green\": ").append(p.greenPhyrexian()).append(",\n");
        sb.append("      \"colorless\": ").append(p.colorlessPhyrexian()).append("\n");
        sb.append("    }\n");
        sb.append("  },\n");

        sb.append("  \"types\": [");
        int ti = 0;
        for (CardType t : types) {
            if (ti++ > 0) sb.append(", ");
            sb.append("\"").append(t.name()).append("\"");
        }
        sb.append("],\n");

        sb.append("  \"subtypes\": [");
        int si = 0;
        for (String s : subtypes) {
            if (si++ > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(s)).append("\"");
        }
        sb.append("],\n");

        sb.append("  \"rarity\": \"").append(rarity.name()).append("\",\n");

        if (combatStats.isPresent()) {
            sb.append("  \"combatStats\": {\"power\": ")
              .append(combatStats.get().power())
              .append(", \"toughness\": ")
              .append(combatStats.get().toughness())
              .append("},\n");
        } else {
            sb.append("  \"combatStats\": null,\n");
        }

        sb.append("  \"keywords\": [");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sb.append(", ");
            KeywordInstance k = keywords.get(i);
            sb.append("{\"id\": \"").append(escapeJson(k.getKeywordId()))
              .append("\", \"name\": \"").append(escapeJson(k.definition().name()))
              .append("\", \"reminder\": \"").append(escapeJson(k.definition().reminderText()))
              .append("\", \"category\": \"").append(k.definition().category().name())
              .append("\", \"hasParam\": ").append(k.definition().hasParameter())
              .append(", \"value\": ").append(k.parameterValue())
              .append("}");
        }
        sb.append("],\n");

        sb.append("  \"baseCooldownTicks\": ").append(baseCooldownTicks).append(",\n");
        sb.append("  \"windupDelayTicks\": ").append(windupDelayTicks).append("\n");
        sb.append("}");
        return sb.toString();
    }

    public static CardDefinition fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new CardParseException("JSON string cannot be null or empty");
        }
        try {
            JsonParser parser = new JsonParser(json);
            Object root = parser.parse();
            if (!(root instanceof Map<?, ?> rootMap)) {
                throw new CardParseException("Root JSON element must be an object");
            }

            String id = (String) rootMap.get("id");
            String setCode = (String) rootMap.get("setCode");
            String name = (String) rootMap.get("name");

            if (id == null || setCode == null || name == null) {
                throw new CardParseException("Missing required card attributes (id, setCode, name)");
            }

            Map<?, ?> manaMap = (Map<?, ?>) rootMap.get("manaCost");
            ManaCost manaCost = ManaCost.ZERO;
            if (manaMap != null) {
                int generic = getInt(manaMap, "generic", 0);
                int white = getInt(manaMap, "white", 0);
                int blue = getInt(manaMap, "blue", 0);
                int black = getInt(manaMap, "black", 0);
                int red = getInt(manaMap, "red", 0);
                int green = getInt(manaMap, "green", 0);
                int colorless = getInt(manaMap, "colorless", 0);

                PhyrexianManaCost phy = PhyrexianManaCost.NONE;
                if (manaMap.get("phyrexian") instanceof Map<?, ?> pMap) {
                    phy = new PhyrexianManaCost(
                        getInt(pMap, "white", 0),
                        getInt(pMap, "blue", 0),
                        getInt(pMap, "black", 0),
                        getInt(pMap, "red", 0),
                        getInt(pMap, "green", 0),
                        getInt(pMap, "colorless", 0)
                    );
                }
                manaCost = new ManaCost(generic, white, blue, black, red, green, colorless, phy);
            }

            Set<CardType> types = new HashSet<>();
            if (rootMap.get("types") instanceof List<?> typeList) {
                for (Object t : typeList) {
                    types.add(CardType.valueOf(t.toString()));
                }
            }

            Set<String> subtypes = new HashSet<>();
            if (rootMap.get("subtypes") instanceof List<?> subList) {
                for (Object s : subList) {
                    subtypes.add(s.toString());
                }
            }

            String rarityStr = (String) rootMap.get("rarity");
            CardRarity rarity = rarityStr != null ? CardRarity.valueOf(rarityStr) : CardRarity.COMMON;

            Optional<CombatStats> combatStats = Optional.empty();
            if (rootMap.get("combatStats") instanceof Map<?, ?> csMap) {
                combatStats = Optional.of(new CombatStats(
                    getInt(csMap, "power", 0),
                    getInt(csMap, "toughness", 0)
                ));
            }

            List<KeywordInstance> keywords = new ArrayList<>();
            if (rootMap.get("keywords") instanceof List<?> kwList) {
                for (Object o : kwList) {
                    if (o instanceof Map<?, ?> kMap) {
                        String kwId = (String) kMap.get("id");
                        Object nameObj = kMap.get("name");
                        String kwName = nameObj != null ? nameObj.toString() : kwId;
                        Object remObj = kMap.get("reminder");
                        String reminder = remObj != null ? remObj.toString() : "";
                        KeywordCategory cat = KeywordCategory.STATIC;
                        if (kMap.get("category") != null) {
                            cat = KeywordCategory.valueOf(kMap.get("category").toString());
                        }
                        boolean hasParam = Boolean.TRUE.equals(kMap.get("hasParam"));
                        int val = getInt(kMap, "value", 0);
                        KeywordDefinition def = new KeywordDefinition(kwId, kwName, reminder, cat, hasParam);
                        keywords.add(new KeywordInstance(def, val));
                    }
                }
            }

            int cooldown = getInt(rootMap, "baseCooldownTicks", 100);
            int windup = getInt(rootMap, "windupDelayTicks", 0);

            return new CardDefinition(id, setCode, name, manaCost, types, subtypes, rarity, combatStats, keywords, cooldown, windup);
        } catch (CardParseException e) {
            throw e;
        } catch (Exception e) {
            throw new CardParseException("Failed to parse CardDefinition from JSON: " + e.getMessage(), e);
        }
    }

    private static int getInt(Map<?, ?> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        return defaultVal;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Lightweight zero-dependency JSON parser
    private static final class JsonParser {
        private final String src;
        private int pos = 0;

        JsonParser(String src) {
            this.src = src;
        }

        Object parse() {
            skipWhitespace();
            if (pos >= src.length()) throw new CardParseException("Unexpected end of JSON input");
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            throw new CardParseException("Unexpected character '" + c + "' at position " + pos);
        }

        private void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isWhitespace(c)) {
                    pos++;
                } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                    while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                } else {
                    break;
                }
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            match('{');
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                match(':');
                skipWhitespace();
                Object value = parse();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    pos++;
                    return map;
                }
                match(',');
            }
            throw new CardParseException("Unterminated object in JSON at position " + pos);
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            match('[');
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                skipWhitespace();
                list.add(parse());
                skipWhitespace();
                if (peek(']')) {
                    pos++;
                    return list;
                }
                match(',');
            }
            throw new CardParseException("Unterminated array in JSON at position " + pos);
        }

        private String parseString() {
            match('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= src.length()) throw new CardParseException("Incomplete escape sequence");
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new CardParseException("Unterminated string in JSON");
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new CardParseException("Invalid boolean token at position " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new CardParseException("Invalid null token at position " + pos);
        }

        private Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == 'e' || src.charAt(pos) == 'E' || src.charAt(pos) == '+')) {
                pos++;
            }
            String numStr = src.substring(start, pos);
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            }
            return Long.parseLong(numStr);
        }

        private void match(char expected) {
            if (pos >= src.length() || src.charAt(pos) != expected) {
                throw new CardParseException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }

        private boolean peek(char c) {
            return pos < src.length() && src.charAt(pos) == c;
        }
    }

    public static final class Builder {
        private final String id;
        private final String setCode;
        private final String name;
        private ManaCost manaCost = ManaCost.ZERO;
        private final Set<CardType> types = new HashSet<>();
        private final Set<String> subtypes = new HashSet<>();
        private CardRarity rarity = CardRarity.COMMON;
        private Optional<CombatStats> combatStats = Optional.empty();
        private final List<KeywordInstance> keywords = new ArrayList<>();
        private int baseCooldownTicks = 100;
        private int windupDelayTicks = 0;

        public Builder(String id, String setCode, String name) {
            this.id = id;
            this.setCode = setCode;
            this.name = name;
        }

        public Builder manaCost(ManaCost manaCost) {
            this.manaCost = Objects.requireNonNull(manaCost, "Mana cost cannot be null");
            return this;
        }

        public Builder addType(CardType type) {
            this.types.add(Objects.requireNonNull(type, "Card type cannot be null"));
            return this;
        }

        public Builder types(Collection<CardType> types) {
            this.types.addAll(types);
            return this;
        }

        public Builder addSubtype(String subtype) {
            this.subtypes.add(Objects.requireNonNull(subtype, "Subtype cannot be null"));
            return this;
        }

        public Builder subtypes(Collection<String> subtypes) {
            this.subtypes.addAll(subtypes);
            return this;
        }

        public Builder rarity(CardRarity rarity) {
            this.rarity = Objects.requireNonNull(rarity, "Card rarity cannot be null");
            return this;
        }

        public Builder combatStats(CombatStats combatStats) {
            this.combatStats = Optional.ofNullable(combatStats);
            return this;
        }

        public Builder combatStats(int power, int toughness) {
            this.combatStats = Optional.of(new CombatStats(power, toughness));
            return this;
        }

        public Builder addKeyword(KeywordInstance keyword) {
            this.keywords.add(Objects.requireNonNull(keyword, "Keyword cannot be null"));
            return this;
        }

        public Builder keywords(Collection<KeywordInstance> keywords) {
            this.keywords.addAll(keywords);
            return this;
        }

        public Builder baseCooldownTicks(int ticks) {
            this.baseCooldownTicks = ticks;
            return this;
        }

        public Builder windupDelayTicks(int ticks) {
            this.windupDelayTicks = ticks;
            return this;
        }

        public CardDefinition build() {
            return new CardDefinition(id, setCode, name, manaCost, types, subtypes, rarity, combatStats, keywords, baseCooldownTicks, windupDelayTicks);
        }
    }
}
