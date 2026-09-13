package guru.interlis.thoth.biblios.view;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.DocPage;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.VersionSwitchMode;
import guru.interlis.thoth.biblios.nav.NavItem;
import guru.interlis.thoth.biblios.nav.NavTree;
import guru.interlis.thoth.biblios.nav.NavigationText;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the FreeMarker view models shared by the static site generator and the
 * Biblios server. Keeping this in {@code thoth-biblios-core} guarantees that the
 * static export and the server render identical frames from identical data.
 *
 * <p>All routes in the models are site-absolute (for example {@code /docs/main/}).
 * The {@code basePath} parameter is applied by the templates on top of that, so the
 * server can render with {@code basePath = ""} while a static page at depth 2 uses
 * {@code "../.."}.</p>
 */
public final class SiteViewModelFactory {
    private final BibliosConfig config;
    private final SiteCatalog catalog;

    public SiteViewModelFactory(BibliosConfig config, SiteCatalog catalog) {
        this.config = Objects.requireNonNull(config, "config is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public BibliosConfig config() {
        return config;
    }

    public SiteCatalog catalog() {
        return catalog;
    }

    // ---------------------------------------------------------------------
    // Common model
    // ---------------------------------------------------------------------

    public Map<String, Object> commonModel(String basePath, String siteLogoReference) {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("siteLogo", resolveMaybeLocalHref(basePath, siteLogoReference));
        model.put("basePath", basePath);
        model.put("siteRootHref", siteRootHref(basePath));
        model.put("searchPageHref", routeHref(basePath, "/search/"));
        model.put("searchIndexUrl", routeHref(basePath, "/search-index.json"));
        model.put("locale", config.site().defaultLanguage());
        model.put("searchLanguageMode", searchLanguageMode());
        model.put("syntaxHighlightingEnabled", syntaxHighlightingEnabled());
        model.put("prismCustomComponentUrls", prismCustomComponentHrefs(basePath));
        model.put("interlisLabEnabled", false);
        model.put("interlisLabScriptHref", assetHref(basePath, "site-assets/interlis-lab/interlis-lab.js"));
        return model;
    }

    public String searchLanguageMode() {
        if (config.ui() == null || config.ui().searchLanguageMode() == null) {
            return "multilingual_safe";
        }
        return config.ui().searchLanguageMode().configValue();
    }

    public boolean syntaxHighlightingEnabled() {
        if (config.ui() == null || config.ui().syntaxHighlightingMode() == null) {
            return true;
        }
        return config.ui().syntaxHighlightingMode().isEnabled();
    }

    public List<String> prismCustomComponentHrefs(String basePath) {
        if (!syntaxHighlightingEnabled() || config.ui() == null || config.ui().prismCustomComponents().isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (String rawPath : config.ui().prismCustomComponents()) {
            Path source = Path.of(rawPath);
            String fileName = source.getFileName().toString();
            urls.add(assetHref(basePath, "site-assets/prism/custom/" + fileName));
        }
        return List.copyOf(urls);
    }

    // ---------------------------------------------------------------------
    // Catalog / component / version / page models
    // ---------------------------------------------------------------------

    public Map<String, Object> catalogModel() {
        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> components = new ArrayList<>();
        for (DocComponent component : catalog.components()) {
            components.add(componentModel(component));
        }
        model.put("components", components);
        return model;
    }

    public Map<String, Object> componentModel(DocComponent component) {
        Map<String, Object> model = new HashMap<>();
        model.put("id", component.id());
        model.put("displayName", component.displayName());
        model.put("defaultVersion", component.defaultVersion());
        model.put("cardBackgroundColor", component.cardBackgroundColor());

        List<Map<String, Object>> versions = new ArrayList<>();
        for (ComponentVersion version : component.versions()) {
            versions.add(versionModel(version));
        }
        model.put("versions", versions);
        return model;
    }

    public Map<String, Object> versionModel(ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        model.put("version", version.version());
        model.put("displayVersion", version.displayVersion());
        model.put("branchName", version.branchName());
        return model;
    }

    public Map<String, Object> pageModel(DocPage page) {
        return pageModel(page, page.html());
    }

    public Map<String, Object> pageModel(DocPage page, String html) {
        Map<String, Object> model = new HashMap<>();
        model.put("componentId", page.componentId());
        model.put("version", page.version());
        model.put("sourcePath", page.sourcePath());
        model.put("pageId", page.pageId());
        model.put("title", page.title());
        model.put("navTitle", page.navTitle());
        model.put("route", page.route());
        model.put("html", html != null ? html : "");
        return model;
    }

    // ---------------------------------------------------------------------
    // Navigation and switchers
    // ---------------------------------------------------------------------

    public Map<String, Object> navigationModel(String componentId, ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        Map<String, String> routeBySourcePath = new HashMap<>();
        for (DocPage page : version.pages()) {
            routeBySourcePath.put(page.sourcePath(), page.route());
        }
        model.put("items", navItemsToModel(componentId, version, version.navigation().items(), routeBySourcePath));
        return model;
    }

    public Map<String, Object> singlePageNavigationModel(String baseRoute, ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        model.put("items", singlePageNavItemsToModel(baseRoute, version.navigation().items()));
        model.put("singlePage", true);
        return model;
    }

    private List<Map<String, Object>> navItemsToModel(String componentId, ComponentVersion version,
                                                      List<NavItem> items, Map<String, String> routeBySourcePath) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (NavItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", item.title());
            m.put("plainTitle", item.plainTitle());
            m.put("page", item.page());
            if (item.page() != null) {
                String route = routeBySourcePath.get(item.page());
                if (route == null) {
                    route = navFallbackRoute(componentId, version, item.page());
                }
                m.put("route", route);
            }
            if (item.children() != null && !item.children().isEmpty()) {
                m.put("children", navItemsToModel(componentId, version, item.children(), routeBySourcePath));
            }
            m.put("group", item.isGroup());
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> singlePageNavItemsToModel(String baseRoute, List<NavItem> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (NavItem item : items) {
            Map<String, Object> m = new HashMap<>();
            String displayTitle = item.title();
            String chapterTitle = item.rawTitle() != null && !item.rawTitle().isBlank()
                ? item.rawTitle()
                : displayTitle;
            m.put("title", displayTitle);
            m.put("plainTitle", item.plainTitle());
            m.put("group", item.isGroup());

            if (item.page() != null && !item.page().isBlank()) {
                String chapterId = normalizeChapterId(item.page());
                m.put("page", chapterId);
                m.put("chapter", true);
                m.put("chapterId", chapterId);
                m.put("chapterTitle", chapterTitle);
                m.put("plainChapterTitle", item.plainRawTitle());
                m.put("route", baseRoute + "#" + chapterId);
            } else {
                m.put("chapter", false);
            }

            if (item.children() != null && !item.children().isEmpty()) {
                m.put("children", singlePageNavItemsToModel(baseRoute, item.children()));
            }
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> breadcrumbsModel(List<DocPage.Breadcrumb> breadcrumbs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocPage.Breadcrumb crumb : breadcrumbs) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", crumb.title());
            m.put("plainTitle", NavigationText.plainText(crumb.title()));
            m.put("route", crumb.route());
            result.add(m);
        }
        return result;
    }

    public String singlePageInitialChapterId(NavTree nav) {
        if (nav == null || nav.items() == null) {
            return "";
        }
        return firstChapterId(nav.items());
    }

    private String firstChapterId(List<NavItem> items) {
        for (NavItem item : items) {
            if (item.page() != null && !item.page().isBlank()) {
                return normalizeChapterId(item.page());
            }
            if (item.children() != null && !item.children().isEmpty()) {
                String nested = firstChapterId(item.children());
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }
        return "";
    }

    public String normalizeChapterId(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /**
     * Shared presentation order for home cards and the documentation switcher.
     * The catalog itself stays flat so content and exports are never duplicated.
     */
    public List<Map<String, Object>> docSwitcher() {
        if (config.content().groups().isEmpty()) {
            return catalog.components().stream().map(this::componentModel).toList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> assigned = new HashSet<>();
        for (var group : config.content().groups()) {
            List<Map<String, Object>> documents = new ArrayList<>();
            for (String id : group.sources()) {
                assigned.add(id);
                DocComponent component = catalog.findById(id);
                if (component != null) {
                    documents.add(componentModel(component));
                }
            }
            if (!documents.isEmpty()) {
                result.add(Map.of("title", group.title(), "documents", documents));
            }
        }
        List<Map<String, Object>> remaining = new ArrayList<>();
        for (var source : config.content().sources()) {
            DocComponent component = catalog.findById(source.id());
            if (!assigned.contains(source.id()) && component != null) {
                remaining.add(componentModel(component));
            }
        }
        if (!remaining.isEmpty()) {
            result.add(Map.of("title", "Weitere", "documents", remaining));
        }
        return result;
    }

    public List<Map<String, Object>> versionSwitcher(DocComponent component) {
        return versionSwitcher(component, null);
    }

    public List<Map<String, Object>> versionSwitcher(DocComponent component, String currentPageSourcePath) {
        List<Map<String, Object>> result = new ArrayList<>();
        VersionSwitchMode mode = config.ui() != null ? config.ui().versionSwitchMode() : VersionSwitchMode.START_PAGE;
        for (ComponentVersion v : component.versions()) {
            Map<String, Object> m = new HashMap<>();
            m.put("version", v.version());
            m.put("displayVersion", v.displayVersion());

            // Equivalent page mode: map source-path across versions, fallback to version root.
            if (mode == VersionSwitchMode.EQUIVALENT_PAGE && currentPageSourcePath != null) {
                DocPage targetPage = v.findPageBySourcePath(currentPageSourcePath);
                if (targetPage != null) {
                    m.put("route", targetPage.route());
                } else {
                    m.put("route", versionRootRoute(component.id(), v.version()));
                }
            } else {
                m.put("route", versionRootRoute(component.id(), v.version()));
            }

            result.add(m);
        }
        return result;
    }

    private String versionRootRoute(String componentId, String version) {
        return "/" + componentId + "/" + version + "/";
    }

    private String navFallbackRoute(String componentId, ComponentVersion version, String sourcePath) {
        String normalizedPath = sourcePath.replace('\\', '/');
        if (normalizedPath.endsWith(".adoc")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 5);
        }
        if (normalizedPath.equals(version.startPage().replace('\\', '/').replaceFirst("\\.adoc$", ""))) {
            return versionRootRoute(componentId, version.version());
        }
        return versionRootRoute(componentId, version.version()) + normalizedPath + "/";
    }

    // ---------------------------------------------------------------------
    // URL helpers
    // ---------------------------------------------------------------------

    public static String basePathForOutput(Path outputFile) {
        Path parent = outputFile != null ? outputFile.getParent() : null;
        if (parent == null || parent.getNameCount() == 0) {
            return ".";
        }
        return String.join("/", Collections.nCopies(parent.getNameCount(), ".."));
    }

    public static String siteRootHref(String basePath) {
        return basePath + "/";
    }

    public static String routeHref(String basePath, String route) {
        String normalizedRoute = route == null || route.isBlank() ? "/" : route;
        if (!normalizedRoute.startsWith("/")) {
            normalizedRoute = "/" + normalizedRoute;
        }
        if ("/".equals(normalizedRoute)) {
            return siteRootHref(basePath);
        }
        return basePath + normalizedRoute;
    }

    public static String assetHref(String basePath, String assetPath) {
        String normalizedAssetPath = assetPath == null ? "" : assetPath.trim();
        if (normalizedAssetPath.isEmpty()) {
            return siteRootHref(basePath);
        }
        if (normalizedAssetPath.startsWith("/")) {
            normalizedAssetPath = normalizedAssetPath.substring(1);
        }
        return routeHref(basePath, "/" + normalizedAssetPath);
    }

    public static String resolveMaybeLocalHref(String basePath, String href) {
        if (href == null || href.isBlank()) {
            return href;
        }
        if (isExternalHref(href) || href.startsWith("#")) {
            return href;
        }
        if (href.startsWith("/")) {
            return routeHref(basePath, href);
        }
        return href;
    }

    public static boolean isExternalHref(String href) {
        String normalized = href.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
            || normalized.startsWith("https://")
            || normalized.startsWith("mailto:")
            || normalized.startsWith("data:")
            || normalized.startsWith("javascript:")
            || normalized.startsWith("tel:")
            || normalized.startsWith("file:")
            || normalized.startsWith("//");
    }

    static boolean isRemoteReference(String reference) {
        try {
            URI uri = new URI(reference);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String normalized = scheme.toLowerCase(Locale.ROOT);
            return normalized.equals("http") || normalized.equals("https") || normalized.equals("data");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
