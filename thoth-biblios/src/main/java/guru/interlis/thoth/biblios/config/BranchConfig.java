package guru.interlis.thoth.biblios.config;

import java.util.Objects;

/**
 * Configuration for a single branch/version.
 */
public final class BranchConfig {
    private final String name;
    private final String displayVersion;

    public BranchConfig(String name, String displayVersion) {
        this.name = Objects.requireNonNull(name, "branch.name is required");
        this.displayVersion = displayVersion != null ? displayVersion : name;
    }

    public String name() {
        return name;
    }

    public String displayVersion() {
        return displayVersion;
    }

    @Override
    public String toString() {
        return "BranchConfig{name='" + name + "', displayVersion='" + displayVersion + "'}";
    }
}
