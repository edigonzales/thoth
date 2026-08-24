package guru.interlis.thoth.biblios.catalog;

import java.util.List;
import java.util.Objects;

/**
 * A documentation component (e.g., "Kataster", "API").
 */
public final class DocComponent {
    private final String id;
    private final String displayName;
    private final String defaultVersion;
    private final List<ComponentVersion> versions;
    private final String cardBackgroundColor;

    public DocComponent(String id, String displayName, String defaultVersion, List<ComponentVersion> versions) {
        this(id, displayName, defaultVersion, versions, null);
    }

    public DocComponent(String id, String displayName, String defaultVersion, List<ComponentVersion> versions,
                        String cardBackgroundColor) {
        this.id = Objects.requireNonNull(id, "component id is required");
        this.displayName = Objects.requireNonNull(displayName, "component display name is required");
        this.defaultVersion = defaultVersion;
        Objects.requireNonNull(versions, "versions are required");
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Component '" + id + "' must have at least one version");
        }
        this.versions = List.copyOf(versions);
        this.cardBackgroundColor = cardBackgroundColor;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultVersion() {
        return defaultVersion;
    }

    /**
     * Optional CSS background color for this component's card on the global home page.
     */
    public String cardBackgroundColor() {
        return cardBackgroundColor;
    }

    public List<ComponentVersion> versions() {
        return versions;
    }

    /**
     * Find a specific version by its version string.
     */
    public ComponentVersion getVersion(String version) {
        for (ComponentVersion v : versions) {
            if (v.version().equals(version)) {
                return v;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "DocComponent{id='" + id + "', displayName='" + displayName + "', versions=" + versions.size() + "}";
    }
}
