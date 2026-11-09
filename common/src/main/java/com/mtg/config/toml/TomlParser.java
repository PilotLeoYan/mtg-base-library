package com.mtg.config.toml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TomlParser {
    private TomlParser() {}

    public static TomlDocument parse(Path path) throws IOException, TomlParseException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return parse(content);
    }

    public static TomlDocument parse(String toml) throws TomlParseException {
        TomlDocument doc = new TomlDocument();
        if (toml == null || toml.trim().isEmpty()) {
            return doc;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(toml))) {
            String line;
            String currentSection = "";
            List<String> pendingComments = new ArrayList<>();
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    continue;
                }

                if (trimmed.startsWith("#")) {
                    pendingComments.add(trimmed);
                    continue;
                }

                // Section header: [section] (supports inline comments: [section] # comment)
                String headerCandidate = stripInlineComment(trimmed).trim();
                if (headerCandidate.startsWith("[")) {
                    if (!headerCandidate.endsWith("]")) {
                        throw new TomlParseException("Unclosed section header at line " + lineNum + ": " + trimmed);
                    }
                    currentSection = headerCandidate.substring(1, headerCandidate.length() - 1).trim();
                    if (currentSection.isEmpty()) {
                        throw new TomlParseException("Empty section header at line " + lineNum);
                    }
                    for (String comment : pendingComments) {
                        doc.addSectionComment(currentSection, comment);
                    }
                    pendingComments.clear();
                    continue;
                }

                // Key-value pair: key = value
                int eqIdx = findEqualsSign(trimmed);
                if (eqIdx == -1) {
                    throw new TomlParseException("Invalid line at " + lineNum + ": missing '=' operator: " + trimmed);
                }

                String rawKey = trimmed.substring(0, eqIdx).trim();
                String rawVal = trimmed.substring(eqIdx + 1).trim();

                String key = parseKey(rawKey, lineNum);
                Object val = parseValue(rawVal, lineNum);

                doc.set(currentSection, key, val);
            }

            return doc;
        } catch (IOException e) {
            throw new TomlParseException("Failed reading TOML stream: " + e.getMessage(), e);
        }
    }

    private static int findEqualsSign(String line) {
        boolean inQuotes = false;
        char quoteChar = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                if (quoteChar == '\'' || !isEscaped(line, i)) {
                    inQuotes = false;
                }
            } else if (!inQuotes && c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static String parseKey(String rawKey, int lineNum) {
        if (rawKey.isEmpty()) {
            throw new TomlParseException("Empty key at line " + lineNum);
        }
        if (rawKey.startsWith("\"")) {
            int closingIdx = findClosingQuote(rawKey, '"');
            if (closingIdx == -1) {
                throw new TomlParseException("Unclosed double-quoted key at line " + lineNum + ": " + rawKey);
            }
            if (closingIdx != rawKey.length() - 1) {
                throw new TomlParseException("Unexpected characters after closing quote in key at line " + lineNum + ": " + rawKey);
            }
            return unescapeString(rawKey.substring(1, closingIdx));
        }
        if (rawKey.startsWith("'")) {
            int closingIdx = findClosingQuote(rawKey, '\'');
            if (closingIdx == -1) {
                throw new TomlParseException("Unclosed single-quoted key at line " + lineNum + ": " + rawKey);
            }
            if (closingIdx != rawKey.length() - 1) {
                throw new TomlParseException("Unexpected characters after closing quote in key at line " + lineNum + ": " + rawKey);
            }
            return rawKey.substring(1, closingIdx);
        }
        return rawKey;
    }

    private static Object parseValue(String rawVal, int lineNum) {
        // Strip trailing comment if not inside quotes
        String valStr = stripInlineComment(rawVal).trim();
        if (valStr.isEmpty()) {
            throw new TomlParseException("Missing value at line " + lineNum);
        }

        if (valStr.startsWith("\"")) {
            int closingIdx = findClosingQuote(valStr, '"');
            if (closingIdx == -1) {
                throw new TomlParseException("Unclosed double-quoted string at line " + lineNum + ": " + valStr);
            }
            if (closingIdx != valStr.length() - 1) {
                throw new TomlParseException("Unexpected characters after closing quote at line " + lineNum + ": " + valStr);
            }
            return unescapeString(valStr.substring(1, closingIdx));
        }
        if (valStr.startsWith("'")) {
            int closingIdx = findClosingQuote(valStr, '\'');
            if (closingIdx == -1) {
                throw new TomlParseException("Unclosed single-quoted string at line " + lineNum + ": " + valStr);
            }
            if (closingIdx != valStr.length() - 1) {
                throw new TomlParseException("Unexpected characters after closing quote at line " + lineNum + ": " + valStr);
            }
            return valStr.substring(1, closingIdx);
        }
        if ("true".equalsIgnoreCase(valStr)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(valStr)) {
            return Boolean.FALSE;
        }

        try {
            if (valStr.contains(".") || valStr.contains("e") || valStr.contains("E")) {
                return Double.parseDouble(valStr);
            }
            return Long.parseLong(valStr);
        } catch (NumberFormatException e) {
            // If neither boolean nor number nor quoted, could be bare string or malformed
            throw new TomlParseException("Unrecognized or malformed TOML value '" + valStr + "' at line " + lineNum);
        }
    }

    private static String stripInlineComment(String val) {
        boolean inQuotes = false;
        char quoteChar = 0;
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                if (quoteChar == '\'' || !isEscaped(val, i)) {
                    inQuotes = false;
                }
            } else if (!inQuotes && c == '#') {
                return val.substring(0, i);
            }
        }
        return val;
    }

    private static int findClosingQuote(String s, char quoteChar) {
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == quoteChar) {
                if (quoteChar == '\'' || !isEscaped(s, i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isEscaped(String s, int index) {
        int backslashCount = 0;
        for (int i = index - 1; i >= 0 && s.charAt(i) == '\\'; i--) {
            backslashCount++;
        }
        return (backslashCount % 2) != 0;
    }

    private static String unescapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
