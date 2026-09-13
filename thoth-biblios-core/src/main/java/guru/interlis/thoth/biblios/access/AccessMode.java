package guru.interlis.thoth.biblios.access;

import java.util.Locale;

/**
 * Access mode of a documentation source.
 *
 * <p>Modes are deliberately coarse: a documentation (source with all its versions,
 * pages and files) is either public, readable by any authenticated principal, or
 * restricted to explicitly listed groups and users.</p>
 */
public enum AccessMode {
    /** Readable by everyone, including anonymous visitors. */
    PUBLIC("public"),
    /** Readable by any authenticated principal from an allowed identity provider. */
    AUTHENTICATED("authenticated"),
    /** Readable only by explicitly listed groups or users. */
    RESTRICTED("restricted");

    private final String configValue;

    AccessMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    /**
     * Parse a mode from configuration, case-insensitively.
     *
     * @throws IllegalArgumentException if the value is unknown
     */
    public static AccessMode parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("access mode must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AccessMode mode : values()) {
            if (mode.configValue.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
            "Unknown access mode '" + value + "'. Allowed values: public, authenticated, restricted.");
    }
}
