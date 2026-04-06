package guru.interlis.thoth.biblios.catalog;

import java.util.List;
import java.util.Objects;

/**
 * A single rendered documentation page.
 */
public final class DocPage {
    private final String componentId;
    private final String version;
    private final String sourcePath;
    private final String sourceUri;
    private final String pageId;
    private final String title;
    private final String navTitle;
    private final String route;
    private final String html;
    private final List<Breadcrumb> breadcrumbs;
    private final DocPage prev;
    private final DocPage next;

    public DocPage(String componentId, String version, String sourcePath, String sourceUri,
                   String pageId, String title, String navTitle, String route, String html,
                   List<Breadcrumb> breadcrumbs, DocPage prev, DocPage next) {
        this.componentId = Objects.requireNonNull(componentId, "componentId is required");
        this.version = Objects.requireNonNull(version, "version is required");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath is required");
        this.sourceUri = sourceUri;
        this.pageId = Objects.requireNonNull(pageId, "pageId is required");
        this.title = Objects.requireNonNull(title, "page title is required");
        this.navTitle = navTitle != null ? navTitle : title;
        this.route = Objects.requireNonNull(route, "route is required");
        this.html = html != null ? html : "";
        this.breadcrumbs = breadcrumbs != null ? List.copyOf(breadcrumbs) : List.of();
        this.prev = prev;
        this.next = next;
    }

    public String componentId() {
        return componentId;
    }

    public String version() {
        return version;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public String sourceUri() {
        return sourceUri;
    }

    public String pageId() {
        return pageId;
    }

    public String title() {
        return title;
    }

    public String navTitle() {
        return navTitle;
    }

    public String route() {
        return route;
    }

    public String html() {
        return html;
    }

    public List<Breadcrumb> breadcrumbs() {
        return breadcrumbs;
    }

    public DocPage prev() {
        return prev;
    }

    public DocPage next() {
        return next;
    }

    @Override
    public String toString() {
        return "DocPage{componentId='" + componentId + "', version='" + version +
               "', route='" + route + "', title='" + title + "'}";
    }

    /**
     * A single breadcrumb entry.
     */
    public static final class Breadcrumb {
        private final String title;
        private final String route;

        public Breadcrumb(String title, String route) {
            this.title = Objects.requireNonNull(title, "breadcrumb title is required");
            this.route = route;
        }

        public String title() {
            return title;
        }

        public String route() {
            return route;
        }

        public boolean isCurrent() {
            return route == null;
        }

        @Override
        public String toString() {
            return "Breadcrumb{title='" + title + "', route='" + route + "'}";
        }
    }
}
