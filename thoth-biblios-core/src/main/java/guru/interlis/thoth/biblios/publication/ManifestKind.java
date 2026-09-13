package guru.interlis.thoth.biblios.publication;

/**
 * Kind of a manifest entry.
 */
public enum ManifestKind {
    /** A byte file served as-is. */
    FILE("file"),
    /** A documentation page rendered through the server frame. */
    PAGE("page"),
    /** Internal data that must never be served directly. */
    INTERNAL("internal");

    private final String configValue;

    ManifestKind(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }
}
