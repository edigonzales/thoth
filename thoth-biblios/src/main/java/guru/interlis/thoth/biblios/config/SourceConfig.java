package guru.interlis.thoth.biblios.config;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for a single documentation source (Git repository).
 */
public final class SourceConfig {
    private final String id;
    private final String displayName;
    private final String url;
    private final List<BranchConfig> branches;
    private final String startPath;
    private final String defaultVersion;
    private final NavigationConfig navigation;
    private final String startPage;

    public SourceConfig(String id, String displayName, String url, List<BranchConfig> branches,
                        String startPath, String defaultVersion, NavigationConfig navigation, String startPage) {
        this.id = Objects.requireNonNull(id, "source.id is required");
        this.displayName = Objects.requireNonNull(displayName, "source.display_name is required");
        this.url = Objects.requireNonNull(url, "source.url is required");
        Objects.requireNonNull(branches, "source.branches is required");
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("source.branches must not be empty for: " + id);
        }
        this.branches = List.copyOf(branches);
        this.startPath = startPath != null ? startPath : ".";
        this.defaultVersion = defaultVersion;
        this.navigation = navigation;
        this.startPage = startPage != null ? startPage : "index.adoc";
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String url() {
        return url;
    }

    public List<BranchConfig> branches() {
        return branches;
    }

    public String startPath() {
        return startPath;
    }

    public String defaultVersion() {
        return defaultVersion;
    }

    public NavigationConfig navigation() {
        return navigation;
    }

    public String startPage() {
        return startPage;
    }

    @Override
    public String toString() {
        return "SourceConfig{id='" + id + "', displayName='" + displayName + "', branches=" + branches.size() + "}";
    }
}
