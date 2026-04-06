package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.nav.NavTree;

import java.util.List;
import java.util.Objects;

/**
 * A specific version of a documentation component.
 */
public final class ComponentVersion {
    private final String componentId;
    private final String version;
    private final String displayVersion;
    private final String branchName;
    private final String startPage;
    private final NavTree navigation;
    private final List<DocPage> pages;

    public ComponentVersion(String componentId, String version, String displayVersion, String branchName,
                            String startPage, NavTree navigation, List<DocPage> pages) {
        this.componentId = Objects.requireNonNull(componentId, "componentId is required");
        this.version = Objects.requireNonNull(version, "version is required");
        this.displayVersion = Objects.requireNonNull(displayVersion, "displayVersion is required");
        this.branchName = Objects.requireNonNull(branchName, "branchName is required");
        this.startPage = startPage != null ? startPage : "index.adoc";
        this.navigation = navigation;
        this.pages = pages != null ? List.copyOf(pages) : List.of();
    }

    public String componentId() {
        return componentId;
    }

    public String version() {
        return version;
    }

    public String displayVersion() {
        return displayVersion;
    }

    public String branchName() {
        return branchName;
    }

    public String startPage() {
        return startPage;
    }

    public NavTree navigation() {
        return navigation;
    }

    public List<DocPage> pages() {
        return pages;
    }

    /**
     * Find a page by its source path.
     */
    public DocPage findPageBySourcePath(String sourcePath) {
        for (DocPage p : pages) {
            if (p.sourcePath().equals(sourcePath)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Find a page by its route.
     */
    public DocPage findPageByRoute(String route) {
        for (DocPage p : pages) {
            if (p.route().equals(route)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ComponentVersion{componentId='" + componentId + "', version='" + version +
               "', displayVersion='" + displayVersion + "', pages=" + pages.size() + "}";
    }
}
