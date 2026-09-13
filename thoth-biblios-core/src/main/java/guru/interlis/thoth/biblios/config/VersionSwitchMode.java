package guru.interlis.thoth.biblios.config;

/**
 * Controls how version switching routes are generated.
 */
public enum VersionSwitchMode {
    /**
     * Always switch to the target version start page.
     */
    START_PAGE,

    /**
     * Try to switch to an equivalent page in the target version, fall back to start page.
     */
    EQUIVALENT_PAGE;

    public static VersionSwitchMode parse(String value) {
        if (value == null || value.isBlank()) {
            return START_PAGE;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "start_page" -> START_PAGE;
            case "equivalent_page" -> EQUIVALENT_PAGE;
            default -> throw new IllegalArgumentException(
                "Invalid ui.version_switch_mode: '" + value + "'. Allowed values: start_page, equivalent_page"
            );
        };
    }
}
