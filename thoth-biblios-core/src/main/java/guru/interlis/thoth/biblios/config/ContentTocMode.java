package guru.interlis.thoth.biblios.config;

/**
 * Controls whether AsciiDoc in-content TOC should be rendered.
 */
public enum ContentTocMode {
    OFF("off"),
    ON("on");

    private final String configValue;

    ContentTocMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean isEnabled() {
        return this == ON;
    }

    public static ContentTocMode parse(String value) {
        if (value == null || value.isBlank()) {
            return OFF;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "off" -> OFF;
            case "on" -> ON;
            default -> throw new IllegalArgumentException(
                "Invalid ui.content_toc: '" + value + "'. Allowed values: off, on"
            );
        };
    }
}
