package guru.interlis.thoth.biblios;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import guru.interlis.thoth.biblios.catalog.*;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.RenderMode;
import guru.interlis.thoth.biblios.config.VersionSwitchMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates the HTML site from a SiteCatalog.
 */
public final class BibliosSiteGenerator implements AutoCloseable {

    private final BibliosConfig config;
    private final SiteCatalog catalog;
    private final Path outputRoot;
    private final Configuration freemarker;

    public BibliosSiteGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot) {
        this.config = config;
        this.catalog = catalog;
        this.outputRoot = outputRoot;

        // Initialize FreeMarker
        freemarker = new Configuration(Configuration.VERSION_2_3_34);
        freemarker.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarker.setLogTemplateExceptions(false);
        freemarker.setWrapUncheckedExceptions(true);
    }

    /**
     * Generate the complete site.
     */
    public void generate() throws IOException {
        System.out.println("[info] Generating site to: " + outputRoot);

        // Clean output if configured
        if (config.output().clean() && Files.exists(outputRoot)) {
            deleteDirectory(outputRoot);
        }
        Files.createDirectories(outputRoot);

        // Copy site assets
        copyAssets();

        // Generate global start page
        generateHomePage();

        // Generate search page
        generateSearchPage();

        // Generate component pages
        for (DocComponent component : catalog.components()) {
            generateComponentPages(component);
        }

        // Generate search index
        generateSearchIndex();

        System.out.println("[info] Site generation complete.");
    }

    private void generateHomePage() throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("siteDescription", config.site().url());
        model.put("basePath", "");
        model.put("catalog", catalogToModel());
        model.put("locale", config.site().defaultLanguage());
        model.put("searchLanguageMode", uiSearchLanguageMode());

        String html = renderTemplate("index.ftl", model);
        writeOutput(Path.of("index.html"), html);
    }

    private void generateSearchPage() throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("basePath", "");
        model.put("locale", config.site().defaultLanguage());
        model.put("docSwitcher", buildDocSwitcher());
        model.put("searchLanguageMode", uiSearchLanguageMode());

        String html = renderTemplate("search.ftl", model);
        writeOutput(Path.of("search/index.html"), html);
    }

    private void generateComponentPages(DocComponent component) throws IOException {
        // Generate component-level landing page (<component>/index.html)
        generateComponentLandingPage(component);

        // Generate version-specific pages (<component>/<version>/...)
        for (ComponentVersion version : component.versions()) {
            generateVersionPages(component, version);
        }
    }

    private void generateComponentLandingPage(DocComponent component) throws IOException {
        Path componentRoot = outputRoot.resolve(component.id());
        Files.createDirectories(componentRoot);
        ComponentVersion defaultVersion = component.getVersion(component.defaultVersion());
        if (defaultVersion == null && !component.versions().isEmpty()) {
            defaultVersion = component.versions().get(0);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("basePath", "");
        model.put("locale", config.site().defaultLanguage());
        model.put("component", componentToModel(component));
        model.put("currentVersion", defaultVersion != null ? versionToModel(defaultVersion) : Map.of());
        model.put("currentComponentId", component.id());
        model.put("currentVersionStr", defaultVersion != null ? defaultVersion.version() : component.defaultVersion());
        model.put("navigation", null);
        model.put("docSwitcher", buildDocSwitcher());
        model.put("versionSwitcher", buildVersionSwitcher(component));
        model.put("searchLanguageMode", uiSearchLanguageMode());

        String html = renderTemplate("component.ftl", model);
        writeOutput(componentRoot.resolve("index.html"), html);
    }

    private void generateVersionPages(DocComponent component, ComponentVersion version) throws IOException {
        // Create output directory for this version
        Path versionRoot = outputRoot.resolve(component.id()).resolve(version.version());
        Files.createDirectories(versionRoot);

        // Generate content pages
        for (DocPage page : version.pages()) {
            generateContentPage(component, version, page, versionRoot);
        }
    }

    private void generateContentPage(DocComponent component, ComponentVersion version, DocPage page, Path versionRoot) throws IOException {
        // Build route path: /<component>/<version>/<page>/
        String prefix = "/" + component.id() + "/" + version.version();
        String routePath = page.route();
        if (routePath.startsWith(prefix)) {
            routePath = routePath.substring(prefix.length());
        }
        if (routePath.startsWith("/")) {
            routePath = routePath.substring(1);
        }
        if (routePath.endsWith("/")) {
            routePath = routePath.substring(0, routePath.length() - 1);
        }

        Path pageDir = versionRoot.resolve(routePath);
        Files.createDirectories(pageDir);

        boolean singlePageMode = version.renderMode() == RenderMode.SINGLE_PAGE;

        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("basePath", "");
        model.put("locale", config.site().defaultLanguage());
        model.put("page", pageToModel(page));
        model.put("currentComponentId", component.id());
        model.put("currentVersionStr", version.version());
        model.put("currentVersion", versionToModel(version));
        model.put("currentPagePath", page.sourcePath());
        model.put(
            "navigation",
            version.navigation() != null
                ? (singlePageMode ? singlePageNavToModel(page.route(), version) : navToModel(component.id(), version))
                : null
        );
        model.put("docSwitcher", buildDocSwitcher());
        model.put("versionSwitcher", buildVersionSwitcher(component, page.sourcePath()));
        model.put("breadcrumbs", breadcrumbsToModel(page.breadcrumbs()));
        model.put("singlePageMode", singlePageMode);
        model.put("chapterBreadcrumbEnabled", singlePageMode);
        model.put("initialChapterId", singlePageMode ? singlePageInitialChapterId(version.navigation()) : "");
        model.put("editUrl", page.editUrl());
        model.put("sourceUrl", page.sourceUrl());
        model.put("showEditLink", config.ui() != null && config.ui().showEditLink());
        model.put("showSourceLink", config.ui() != null && config.ui().showSourceLink());
        model.put("searchLanguageMode", uiSearchLanguageMode());

        if (page.prev() != null) {
            model.put("prevPage", pageToModel(page.prev()));
        }
        if (page.next() != null) {
            model.put("nextPage", pageToModel(page.next()));
        }

        String html = renderTemplate("page.ftl", model);
        writeOutput(pageDir.resolve("index.html"), html);
    }

    private void generateSearchIndex() throws IOException {
        // Simple JSON search index
        StringBuilder json = new StringBuilder("[\n");
        boolean first = true;

        for (DocComponent component : catalog.components()) {
            for (ComponentVersion version : component.versions()) {
                for (DocPage page : version.pages()) {
                    if (!first) json.append(",\n");
                    first = false;
                    json.append("  {")
                        .append("\"component\":\"").append(escapeJson(component.id())).append("\",")
                        .append("\"version\":\"").append(escapeJson(version.version())).append("\",")
                        .append("\"displayVersion\":\"").append(escapeJson(version.displayVersion())).append("\",")
                        .append("\"title\":\"").append(escapeJson(page.title())).append("\",")
                        .append("\"route\":\"").append(escapeJson(page.route())).append("\",")
                        .append("\"content\":\"").append(escapeJson(page.html().replaceAll("<[^>]+>", " "))).append("\"")
                        .append("}");
                }
            }
        }

        json.append("\n]");

        Path searchIndex = outputRoot.resolve("search-index.json");
        Files.writeString(searchIndex, json.toString(), StandardCharsets.UTF_8);
    }

    private void copyAssets() throws IOException {
        // Copy site-assets from resources to output
        Path assetsDest = outputRoot.resolve("site-assets");
        Files.createDirectories(assetsDest);

        copyAsset(assetsDest, "frutiger-light.css");
        copyAsset(assetsDest, "jetbrainsmono.css");
        copyAsset(assetsDest, "styles.css");
        copyAsset(assetsDest, "lunr.min.js");
        copyAsset(assetsDest, "search.js");
    }

    private void copyAsset(Path assetsDest, String fileName) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/site-assets/" + fileName)) {
            if (stream == null) {
                throw new IOException("Missing bundled site asset: " + fileName);
            }
            Files.writeString(assetsDest.resolve(fileName), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private String uiSearchLanguageMode() {
        if (config.ui() == null || config.ui().searchLanguageMode() == null) {
            return "multilingual_safe";
        }
        return config.ui().searchLanguageMode().configValue();
    }

    // Template rendering

    private String renderTemplate(String name, Map<String, Object> model) {
        try {
            Template template = freemarker.getTemplate(name);
            try (StringWriter writer = new StringWriter()) {
                template.process(model, writer);
                return writer.toString();
            }
        } catch (IOException | TemplateException e) {
            throw new RuntimeException("Failed to render template: " + name, e);
        }
    }

    private void writeOutput(Path relativePath, String content) throws IOException {
        Path target = outputRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    // Model converters

    private Map<String, Object> catalogToModel() {
        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> components = new ArrayList<>();
        for (DocComponent c : catalog.components()) {
            components.add(componentToModel(c));
        }
        model.put("components", components);
        return model;
    }

    private Map<String, Object> componentToModel(DocComponent component) {
        Map<String, Object> model = new HashMap<>();
        model.put("id", component.id());
        model.put("displayName", component.displayName());
        model.put("defaultVersion", component.defaultVersion());

        List<Map<String, Object>> versions = new ArrayList<>();
        for (ComponentVersion v : component.versions()) {
            versions.add(versionToModel(v));
        }
        model.put("versions", versions);
        return model;
    }

    private Map<String, Object> versionToModel(ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        model.put("version", version.version());
        model.put("displayVersion", version.displayVersion());
        model.put("branchName", version.branchName());
        return model;
    }

    private Map<String, Object> pageToModel(DocPage page) {
        Map<String, Object> model = new HashMap<>();
        model.put("componentId", page.componentId());
        model.put("version", page.version());
        model.put("sourcePath", page.sourcePath());
        model.put("pageId", page.pageId());
        model.put("title", page.title());
        model.put("navTitle", page.navTitle());
        model.put("route", page.route());
        model.put("html", page.html());
        return model;
    }

    private Map<String, Object> navToModel(String componentId, ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        Map<String, String> routeBySourcePath = new HashMap<>();
        for (DocPage page : version.pages()) {
            routeBySourcePath.put(page.sourcePath(), page.route());
        }
        model.put("items", navItemsToModel(componentId, version, version.navigation().items(), routeBySourcePath));
        return model;
    }

    private Map<String, Object> singlePageNavToModel(String baseRoute, ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        model.put("items", singlePageNavItemsToModel(baseRoute, version.navigation().items()));
        model.put("singlePage", true);
        return model;
    }

    private List<Map<String, Object>> navItemsToModel(String componentId, ComponentVersion version,
                                                      List<guru.interlis.thoth.biblios.nav.NavItem> items,
                                                      Map<String, String> routeBySourcePath) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (guru.interlis.thoth.biblios.nav.NavItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", item.title());
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

    private List<Map<String, Object>> singlePageNavItemsToModel(String baseRoute, List<guru.interlis.thoth.biblios.nav.NavItem> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (guru.interlis.thoth.biblios.nav.NavItem item : items) {
            Map<String, Object> m = new HashMap<>();
            String displayTitle = item.title();
            String chapterTitle = item.rawTitle() != null && !item.rawTitle().isBlank()
                ? item.rawTitle()
                : displayTitle;
            m.put("title", displayTitle);
            m.put("group", item.isGroup());

            if (item.page() != null && !item.page().isBlank()) {
                String chapterId = normalizeChapterId(item.page());
                m.put("page", chapterId);
                m.put("chapter", true);
                m.put("chapterId", chapterId);
                m.put("chapterTitle", chapterTitle);
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

    private List<Map<String, Object>> breadcrumbsToModel(List<DocPage.Breadcrumb> breadcrumbs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocPage.Breadcrumb crumb : breadcrumbs) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", crumb.title());
            m.put("route", crumb.route());
            result.add(m);
        }
        return result;
    }

    private String singlePageInitialChapterId(guru.interlis.thoth.biblios.nav.NavTree nav) {
        if (nav == null || nav.items() == null) {
            return "";
        }
        return firstChapterId(nav.items());
    }

    private String firstChapterId(List<guru.interlis.thoth.biblios.nav.NavItem> items) {
        for (guru.interlis.thoth.biblios.nav.NavItem item : items) {
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

    private String normalizeChapterId(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private List<Map<String, Object>> buildDocSwitcher() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocComponent c : catalog.components()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.id());
            m.put("displayName", c.displayName());
            m.put("defaultVersion", c.defaultVersion());
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> buildVersionSwitcher(DocComponent component) {
        return buildVersionSwitcher(component, null);
    }

    private List<Map<String, Object>> buildVersionSwitcher(DocComponent component, String currentPageSourcePath) {
        List<Map<String, Object>> result = new ArrayList<>();
        VersionSwitchMode mode = config.ui() != null ? config.ui().versionSwitchMode() : VersionSwitchMode.START_PAGE;
        for (ComponentVersion v : component.versions()) {
            Map<String, Object> m = new HashMap<>();
            m.put("version", v.version());
            m.put("displayVersion", v.displayVersion());

            // Equivalent page mode: map source-path across versions, fallback to version root.
            if (mode == VersionSwitchMode.EQUIVALENT_PAGE && currentPageSourcePath != null) {
                var targetPage = v.findPageBySourcePath(currentPageSourcePath);
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

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void deleteDirectory(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                  .forEach(path -> {
                      try {
                          Files.delete(path);
                      } catch (IOException e) {
                          System.err.println("[warn] Failed to delete: " + path);
                      }
                  });
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
