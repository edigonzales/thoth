package guru.interlis.thoth.biblios.config;

/**
 * Controls syntax highlighting integration for rendered code blocks.
 */
public enum SyntaxHighlightingMode {
    PRISM("prism"),
    OFF("off");

    private final String configValue;

    SyntaxHighlightingMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean isEnabled() {
        return this == PRISM;
    }

    public static SyntaxHighlightingMode parse(String value) {
        if (value == null || value.isBlank()) {
            return PRISM;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "prism" -> PRISM;
            case "off" -> OFF;
            default -> throw new IllegalArgumentException(
                "Invalid ui.syntax_highlighting: '" + value + "'. Allowed values: prism, off"
            );
        };
    }
}
