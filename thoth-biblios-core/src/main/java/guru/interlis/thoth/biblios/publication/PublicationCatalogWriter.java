package guru.interlis.thoth.biblios.publication;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.DocPage;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.RenderMode;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.biblios.view.SiteViewModelFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes the catalog metadata needed to render the portal at runtime.
 *
 * <p>The catalog contains view models produced by {@link SiteViewModelFactory}
 * (with an empty base path, because the server always serves from the root) plus
 * per-page references to pre-rendered fragments. The server filters these models
 * against the access rules of the current user and renders the FreeMarker frames.</p>
 */
public final class PublicationCatalogWriter {
    public static final int FORMAT_VERSION = 1;

    private PublicationCatalogWriter() {
    }

    public static String write(BibliosConfig config, SiteCatalog catalog, String siteLogoReference) {
        return write(config, catalog, siteLogoReference, new SiteViewModelFactory(config, catalog));
    }

    public static String write(BibliosConfig config, SiteCatalog catalog, String siteLogoReference,
                               SiteViewModelFactory view) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", FORMAT_VERSION);
        root.put("site", siteModel(config, siteLogoReference));
        root.put("ui", uiModel(config, view));
        root.put("sources", sourcesModel(config));
        root.put("home", homeModel(view));
        root.put("components", componentModels(catalog, view));
        return JsonSupport.write(root);
    }

    private static Map<String, Object> siteModel(BibliosConfig config, String siteLogoReference) {
        Map<String, Object> site = new LinkedHashMap<>();
        site.put("title", config.site().title());
        site.put("url", config.site().url());
        site.put("logo", siteLogoReference);
        site.put("defaultLanguage", config.site().defaultLanguage());
        return site;
    }

    private static Map<String, Object> uiModel(BibliosConfig config, SiteViewModelFactory view) {
        Map<String, Object> ui = new LinkedHashMap<>();
        ui.put("showEditLink", config.ui() != null && config.ui().showEditLink());
        ui.put("showSourceLink", config.ui() != null && config.ui().showSourceLink());
        ui.put("searchLanguageMode", view.searchLanguageMode());
        ui.put("syntaxHighlightingEnabled", view.syntaxHighlightingEnabled());
        ui.put("prismCustomComponentHrefs", view.prismCustomComponentHrefs(""));
        ui.put("interlisLabScriptHref", "/site-assets/interlis-lab/interlis-lab.js");
        return ui;
    }

    private static List<Map<String, Object>> sourcesModel(BibliosConfig config) {
        List<Map<String, Object>> sources = new ArrayList<>();
        for (SourceConfig source : config.content().sources()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", source.id());
            entry.put("accessPolicy", source.accessPolicy());
            sources.add(entry);
        }
        return sources;
    }

    private static Map<String, Object> homeModel(SiteViewModelFactory view) {
        Map<String, Object> home = new LinkedHashMap<>();
        home.put("catalog", view.catalogModel());
        home.put("docSwitcher", view.docSwitcher());
        return home;
    }

    private static List<Map<String, Object>> componentModels(SiteCatalog catalog, SiteViewModelFactory view) {
        List<Map<String, Object>> components = new ArrayList<>();
        for (DocComponent component : catalog.components()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", component.id());
            entry.put("component", view.componentModel(component));
            entry.put("versionSwitcher", view.versionSwitcher(component));
            List<Map<String, Object>> versions = new ArrayList<>();
            for (ComponentVersion version : component.versions()) {
                versions.add(versionModel(component, version, view));
            }
            entry.put("versions", versions);
            components.add(entry);
        }
        return components;
    }

    private static Map<String, Object> versionModel(DocComponent component, ComponentVersion version,
                                                    SiteViewModelFactory view) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("version", version.version());
        entry.put("displayVersion", version.displayVersion());
        entry.put("route", versionRootRoute(component.id(), version.version()));
        entry.put("singlePage", version.renderMode() == RenderMode.SINGLE_PAGE);
        entry.put("startPage", version.startPage());
        entry.put("navigation",
            version.navigation() != null ? view.navigationModel(component.id(), version) : null);
        entry.put("singlePageNavigation",
            version.navigation() != null && version.renderMode() == RenderMode.SINGLE_PAGE
                ? view.singlePageNavigationModel(defaultPageRoute(component, version), version)
                : null);
        entry.put("defaultPageRoute", defaultPageRoute(component, version));

        List<Map<String, Object>> pages = new ArrayList<>();
        for (DocPage page : version.pages()) {
            pages.add(pageEntry(page, version));
        }
        entry.put("pages", pages);
        return entry;
    }

    private static Map<String, Object> pageEntry(DocPage page, ComponentVersion version) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("route", page.route());
        entry.put("fragment", fragmentPath(page.route()));
        entry.put("usesInterlisLab", page.usesInterlisLab());
        entry.put("editUrl", page.editUrl());
        entry.put("sourceUrl", page.sourceUrl());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("componentId", page.componentId());
        model.put("version", page.version());
        model.put("sourcePath", page.sourcePath());
        model.put("pageId", page.pageId());
        model.put("title", page.title());
        model.put("navTitle", page.navTitle());
        model.put("route", page.route());
        entry.put("model", model);
        // Metadata used by the server to build navigation state.
        entry.put("breadcrumbs", breadcrumbs(page));
        entry.put("prevRoute", page.prev() != null ? page.prev().route() : null);
        entry.put("nextRoute", page.next() != null ? page.next().route() : null);
        entry.put("prevTitle", page.prev() != null ? page.prev().title() : null);
        entry.put("nextTitle", page.next() != null ? page.next().title() : null);
        return entry;
    }

    private static List<Map<String, Object>> breadcrumbs(DocPage page) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocPage.Breadcrumb crumb : page.breadcrumbs()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("title", crumb.title());
            entry.put("plainTitle", guru.interlis.thoth.biblios.nav.NavigationText.plainText(crumb.title()));
            entry.put("route", crumb.route());
            result.add(entry);
        }
        return result;
    }

    private static String defaultPageRoute(DocComponent component, ComponentVersion version) {
        String versionRoot = versionRootRoute(component.id(), version.version());
        for (DocPage page : version.pages()) {
            if (page.route().equals(versionRoot)) {
                return page.route();
            }
        }
        return version.pages().isEmpty() ? versionRoot : version.pages().get(0).route();
    }

    private static String versionRootRoute(String componentId, String version) {
        return "/" + componentId + "/" + version + "/";
    }

    private static String fragmentPath(String route) {
        String normalized = route == null ? "" : route.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return "pages/" + normalized + "index.html";
    }
}
