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
    private final String editUrl;
    private final String sourceUrl;
    private final String imagesDir;
    private final String sourceBaseDir;
    private final boolean usesInterlisLab;

    public DocPage(String componentId, String version, String sourcePath, String sourceUri,
                   String pageId, String title, String navTitle, String route, String html,
                   List<Breadcrumb> breadcrumbs, DocPage prev, DocPage next) {
        this(componentId, version, sourcePath, sourceUri, pageId, title, navTitle, route, html,
             breadcrumbs, prev, next, null, null, "", "", false);
    }

    public DocPage(String componentId, String version, String sourcePath, String sourceUri,
                   String pageId, String title, String navTitle, String route, String html,
                   List<Breadcrumb> breadcrumbs, DocPage prev, DocPage next,
                   String editUrl, String sourceUrl) {
        this(componentId, version, sourcePath, sourceUri, pageId, title, navTitle, route, html,
            breadcrumbs, prev, next, editUrl, sourceUrl, "", "", false);
    }

    public DocPage(String componentId, String version, String sourcePath, String sourceUri,
                   String pageId, String title, String navTitle, String route, String html,
                   List<Breadcrumb> breadcrumbs, DocPage prev, DocPage next,
                   String editUrl, String sourceUrl, String imagesDir, String sourceBaseDir) {
        this(componentId, version, sourcePath, sourceUri, pageId, title, navTitle, route, html,
            breadcrumbs, prev, next, editUrl, sourceUrl, imagesDir, sourceBaseDir, false);
    }

    public DocPage(String componentId, String version, String sourcePath, String sourceUri,
                   String pageId, String title, String navTitle, String route, String html,
                   List<Breadcrumb> breadcrumbs, DocPage prev, DocPage next,
                   String editUrl, String sourceUrl, String imagesDir, String sourceBaseDir,
                   boolean usesInterlisLab) {
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
        this.editUrl = editUrl;
        this.sourceUrl = sourceUrl;
        this.imagesDir = imagesDir != null ? imagesDir.trim() : "";
        this.sourceBaseDir = sourceBaseDir != null ? sourceBaseDir.trim() : "";
        this.usesInterlisLab = usesInterlisLab;
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

    /**
     * URL to edit this page source file.
     * May be null if not configured.
     */
    public String editUrl() {
        return editUrl;
    }

    /**
     * URL to view the raw source file in the repository.
     * May be null if not configured.
     */
    public String sourceUrl() {
        return sourceUrl;
    }

    /**
     * Effective imagesdir used while rendering this page.
     * Empty when not set.
     */
    public String imagesDir() {
        return imagesDir;
    }

    /**
     * Absolute source document directory used as Asciidoctor baseDir.
     * Empty when unknown.
     */
    public String sourceBaseDir() {
        return sourceBaseDir;
    }

    public boolean usesInterlisLab() {
        return usesInterlisLab;
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
