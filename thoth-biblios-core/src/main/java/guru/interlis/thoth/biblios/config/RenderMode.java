package guru.interlis.thoth.biblios.config;

/**
 * Controls how source content is rendered into pages.
 */
public enum RenderMode {
    SPLIT("split"),
    SINGLE_PAGE("single_page");

    private final String configValue;

    RenderMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean isSinglePage() {
        return this == SINGLE_PAGE;
    }

    public static RenderMode parse(String value) {
        if (value == null || value.isBlank()) {
            return SPLIT;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "split" -> SPLIT;
            case "single_page" -> SINGLE_PAGE;
            default -> throw new IllegalArgumentException(
                "Invalid content.sources[].render_mode: '" + value + "'. Allowed values: split, single_page"
            );
        };
    }
}
