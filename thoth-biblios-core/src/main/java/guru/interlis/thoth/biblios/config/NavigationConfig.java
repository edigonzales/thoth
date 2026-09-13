package guru.interlis.thoth.biblios.config;

import java.util.Objects;

/**
 * Navigation configuration for a source.
 */
public final class NavigationConfig {
    private final String file;

    public NavigationConfig(String file) {
        this.file = Objects.requireNonNull(file, "navigation.file is required");
    }

    public String file() {
        return file;
    }

    @Override
    public String toString() {
        return "NavigationConfig{file='" + file + "'}";
    }
}
