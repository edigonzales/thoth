package guru.interlis.thoth.biblios.access;

import java.util.Locale;

/**
 * Default behavior for documentation sources that do not reference an explicit policy.
 */
public enum AccessDefault {
    /** Sources without {@code access_policy} are not readable by anyone. */
    DENY("deny"),
    /** Sources without {@code access_policy} are public. */
    PUBLIC("public");

    private final String configValue;

    AccessDefault(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static AccessDefault parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("access default must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AccessDefault candidate : values()) {
            if (candidate.configValue.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(
            "Unknown access default '" + value + "'. Allowed values: deny, public.");
    }
}
