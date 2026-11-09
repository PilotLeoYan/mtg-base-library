package com.mtg.config.toml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class TomlWriter {
    private TomlWriter() {}

    public static void write(TomlDocument doc, Path path) throws IOException {
        String content = write(doc);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    public static String write(TomlDocument doc) {
        StringBuilder sb = new StringBuilder();

        for (String section : doc.getSections()) {
            List<String> comments = doc.getSectionComments(section);
            if (!comments.isEmpty()) {
                for (String comment : comments) {
                    sb.append(comment).append("\n");
                }
            }

            if (!section.isEmpty()) {
                sb.append("[").append(section).append("]\n");
            }

            Map<String, Object> map = doc.getSection(section);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = formatKey(entry.getKey());
                String val = formatValue(entry.getValue());
                sb.append(key).append(" = ").append(val).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String formatKey(String key) {
        if (key.contains(":") || key.contains(".") || key.contains(" ") || key.contains("/") || key.contains("-")) {
            return "\"" + escapeString(key) + "\"";
        }
        return key;
    }

    private static String formatValue(Object val) {
        if (val == null) {
            return "\"\"";
        }
        if (val instanceof Boolean b) {
            return b ? "true" : "false";
        }
        if (val instanceof Number) {
            return val.toString();
        }
        return "\"" + escapeString(val.toString()) + "\"";
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
