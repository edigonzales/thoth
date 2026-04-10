package guru.interlis.thoth.biblios.config;

/**
 * Controls the Lunr indexing/search pipeline behavior.
 */
public enum SearchLanguageMode {
    /**
     * Safer default for mixed-language content: no stemming/stop-word filters.
     */
    MULTILINGUAL_SAFE("multilingual_safe"),

    /**
     * English-oriented defaults: Lunr standard pipeline (stemming/stop words).
     */
    ENGLISH_DEFAULT("english_default");

    private final String configValue;

    SearchLanguageMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static SearchLanguageMode parse(String value) {
        if (value == null || value.isBlank()) {
            return MULTILINGUAL_SAFE;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "multilingual_safe" -> MULTILINGUAL_SAFE;
            case "english_default" -> ENGLISH_DEFAULT;
            default -> throw new IllegalArgumentException(
                "Invalid ui.search_language_mode: '" + value + "'. Allowed values: multilingual_safe, english_default"
            );
        };
    }
}
