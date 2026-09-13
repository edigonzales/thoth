package guru.interlis.thoth.biblios.config;

/**
 * Controls whether sidebar TOC entries should include chapter numbers.
 */
public enum SidebarTocNumbersMode {
    OFF("off"),
    ON("on");

    private final String configValue;

    SidebarTocNumbersMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean isEnabled() {
        return this == ON;
    }

    public static SidebarTocNumbersMode parse(String value) {
        if (value == null || value.isBlank()) {
            return OFF;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "off" -> OFF;
            case "on" -> ON;
            default -> throw new IllegalArgumentException(
                "Invalid content.sources[].sidebar_toc_numbers: '" + value + "'. Allowed values: off, on"
            );
        };
    }
}
