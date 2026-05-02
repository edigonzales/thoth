package guru.interlis.thoth.biblios.config;

/**
 * Controls whether section numbering should be rendered in HTML content.
 */
public enum ContentSectionNumbersMode {
    ON("on"),
    OFF("off");

    private final String configValue;

    ContentSectionNumbersMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean isEnabled() {
        return this == ON;
    }

    public static ContentSectionNumbersMode parse(String value) {
        if (value == null || value.isBlank()) {
            return ON;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "on" -> ON;
            case "off" -> OFF;
            default -> throw new IllegalArgumentException(
                "Invalid ui.content_section_numbers: '" + value + "'. Allowed values: on, off"
            );
        };
    }
}
