package guru.interlis.thoth.biblios;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared helpers for DOCX field instructions and deterministic bookmark naming.
 */
final class DocxFieldSupport {

    private DocxFieldSupport() {
    }

    static String refInstruction(String bookmark) {
        return " REF " + bookmark + " \\h ";
    }

    static String refNumberInstruction(String bookmark) {
        return " REF " + bookmark + " \\n \\h ";
    }

    static String pageRefInstruction(String bookmark) {
        return " PAGEREF " + bookmark + " \\h ";
    }

    static String seqInstruction(String sequenceName) {
        return " SEQ " + sequenceName + " \\* ARABIC ";
    }

    static final class BookmarkRegistry {
        private final Map<String, String> byRaw = new LinkedHashMap<>();
        private final Set<String> usedNames = new LinkedHashSet<>();

        String nameFor(String rawKey) {
            String key = rawKey != null ? rawKey.trim() : "";
            if (key.isBlank()) {
                throw new IllegalArgumentException("bookmark key must not be blank");
            }
            String existing = byRaw.get(key);
            if (existing != null) {
                return existing;
            }

            String normalized = normalizeBookmarkBase(key);
            String candidate = normalized;
            int suffix = 2;
            while (usedNames.contains(candidate)) {
                String suffixText = "_" + suffix;
                int maxBase = Math.max(1, 40 - suffixText.length());
                String base = normalized.length() > maxBase ? normalized.substring(0, maxBase) : normalized;
                candidate = base + suffixText;
                suffix++;
            }

            byRaw.put(key, candidate);
            usedNames.add(candidate);
            return candidate;
        }

        private String normalizeBookmarkBase(String raw) {
            String normalized = raw.replaceAll("[^A-Za-z0-9_]", "_");
            if (normalized.isBlank()) {
                normalized = "bookmark";
            }
            if (!Character.isLetter(normalized.charAt(0))) {
                normalized = "b_" + normalized;
            }
            if (normalized.length() > 40) {
                normalized = normalized.substring(0, 40);
            }
            return normalized;
        }
    }
}
