package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.access.AccessService;
import guru.interlis.thoth.biblios.server.publication.PackageCatalog;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import guru.interlis.thoth.biblios.server.view.FrameRenderer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds and renders the portal frames (home, component landing, page, search)
 * with the access-filtered catalog and navigation.
 */
@Component
public class PortalFrames {

    private final PublicationPackage publicationPackage;
    private final AccessService accessService;
    private final FrameRenderer renderer;

    public PortalFrames(PublicationPackage publicationPackage, AccessService accessService, FrameRenderer renderer) {
        this.publicationPackage = publicationPackage;
        this.accessService = accessService;
        this.renderer = renderer;
    }

    /**
     * Identity and session data needed by the layout (logout form, CSRF token).
     */
    public record FrameSession(PrincipalIdentity principal, String csrfParameterName, String csrfToken) {
    }

    public String home(FrameSession session) {
        Set<String> allowed = accessService.allowedSourceIds(session.principal());
        Map<String, Object> model = commonModel(session);
        model.put("catalog", filterCatalog(publicationPackage.catalog().home().catalog(), allowed));
        model.put("docSwitcher", filterDocSwitcher(publicationPackage.catalog().home().docSwitcher(), allowed));
        model.put("siteDescription", publicationPackage.catalog().site().url());
        return renderer.render("index.ftl", model);
    }

    public String component(PackageCatalog.ComponentEntry entry, FrameSession session) {
        Set<String> allowed = accessService.allowedSourceIds(session.principal());
        Map<String, Object> componentModel = entry.component();
        Map<String, Object> currentVersion = currentVersion(componentModel);
        Map<String, Object> model = commonModel(session);
        model.put("component", componentModel);
        model.put("currentVersion", currentVersion);
        model.put("currentComponentId", entry.id());
        model.put("currentVersionStr", currentVersion.getOrDefault("version", ""));
        model.put("navigation", null);
        model.put("docSwitcher", filterDocSwitcher(publicationPackage.catalog().home().docSwitcher(), allowed));
        model.put("versionSwitcher", entry.versionSwitcher());
        return renderer.render("component.ftl", model);
    }

    public String page(PublicationPackage.PageLocation location, FrameSession session) {
        PackageCatalog.PageEntry page = location.page();
        PackageCatalog.VersionEntry version = location.version();
        Set<String> allowed = accessService.allowedSourceIds(session.principal());

        Map<String, Object> model = commonModel(session);
        Map<String, Object> pageModel = new LinkedHashMap<>(page.model());
        pageModel.put("html", publicationPackage.readFragment(page.fragment()));
        model.put("page", pageModel);
        model.put("currentComponentId", location.componentId());
        model.put("currentVersionStr", version.version());
        model.put("currentVersion", Map.of(
            "version", version.version(),
            "displayVersion", version.displayVersion()
        ));
        model.put("currentPagePath", page.model().get("sourcePath"));
        model.put("navigation", version.singlePage() ? version.singlePageNavigation() : version.navigation());
        model.put("docSwitcher", filterDocSwitcher(publicationPackage.catalog().home().docSwitcher(), allowed));
        model.put("versionSwitcher", versionSwitcher(location));
        model.put("breadcrumbs", page.breadcrumbs());
        model.put("singlePageMode", version.singlePage());
        model.put("chapterBreadcrumbEnabled", version.singlePage());
        model.put("initialChapterId", version.initialChapterId() != null ? version.initialChapterId() : "");
        model.put("editUrl", page.editUrl());
        model.put("sourceUrl", page.sourceUrl());
        model.put("showEditLink", publicationPackage.catalog().ui().showEditLink());
        model.put("showSourceLink", publicationPackage.catalog().ui().showSourceLink());
        model.put("interlisLabEnabled", page.usesInterlisLab());
        if (page.prevRoute() != null) {
            model.put("prevPage", Map.of("route", page.prevRoute(),
                "title", page.prevTitle() != null ? page.prevTitle() : ""));
        }
        if (page.nextRoute() != null) {
            model.put("nextPage", Map.of("route", page.nextRoute(),
                "title", page.nextTitle() != null ? page.nextTitle() : ""));
        }
        return renderer.render("page.ftl", model);
    }

    public String search(FrameSession session) {
        Set<String> allowed = accessService.allowedSourceIds(session.principal());
        Map<String, Object> model = commonModel(session);
        model.put("docSwitcher", filterDocSwitcher(publicationPackage.catalog().home().docSwitcher(), allowed));
        return renderer.render("search.ftl", model);
    }

    private List<Map<String, Object>> versionSwitcher(PublicationPackage.PageLocation location) {
        PackageCatalog.ComponentEntry component = publicationPackage.component(location.componentId());
        return component != null ? component.versionSwitcher() : List.of();
    }

    private Map<String, Object> currentVersion(Map<String, Object> componentModel) {
        Object versions = componentModel.get("versions");
        String defaultVersion = String.valueOf(componentModel.getOrDefault("defaultVersion", ""));
        if (versions instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> versionModel
                    && defaultVersion.equals(versionModel.get("version"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) versionModel;
                    return result;
                }
            }
            if (!list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) first;
                return result;
            }
        }
        return Map.of("version", "", "displayVersion", "");
    }

    private Map<String, Object> commonModel(FrameSession session) {
        PackageCatalog.Site site = publicationPackage.catalog().site();
        PackageCatalog.Ui ui = publicationPackage.catalog().ui();
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", site.title());
        model.put("siteLogo", site.logo());
        model.put("basePath", "");
        model.put("siteRootHref", "/");
        model.put("searchPageHref", "/search/");
        model.put("searchIndexUrl", "/api/search-index");
        model.put("locale", site.defaultLanguage());
        model.put("searchLanguageMode", ui.searchLanguageMode());
        model.put("syntaxHighlightingEnabled", ui.syntaxHighlightingEnabled());
        model.put("prismCustomComponentUrls",
            ui.prismCustomComponentHrefs() != null ? ui.prismCustomComponentHrefs() : List.of());
        model.put("interlisLabEnabled", false);
        model.put("interlisLabScriptHref", ui.interlisLabScriptHref());
        boolean authenticated = session != null && session.principal() != null;
        boolean csrfAvailable = session != null
            && session.csrfParameterName() != null
            && session.csrfToken() != null;
        model.put("showLogout", authenticated && csrfAvailable);
        model.put("logoutUrl", "/logout");
        if (csrfAvailable) {
            model.put("csrfParameterName", session.csrfParameterName());
            model.put("csrfToken", session.csrfToken());
        }
        return model;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterCatalog(Map<String, Object> catalogModel, Set<String> allowed) {
        Map<String, Object> result = new LinkedHashMap<>(catalogModel);
        Object components = catalogModel.get("components");
        if (components instanceof List<?> list) {
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> component && allowed.contains(String.valueOf(component.get("id")))) {
                    filtered.add((Map<String, Object>) component);
                }
            }
            result.put("components", filtered);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterDocSwitcher(List<Map<String, Object>> docSwitcher, Set<String> allowed) {
        if (docSwitcher == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : docSwitcher) {
            Object documents = entry.get("documents");
            if (documents instanceof List<?> list) {
                List<Map<String, Object>> filtered = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> component && allowed.contains(String.valueOf(component.get("id")))) {
                        filtered.add((Map<String, Object>) component);
                    }
                }
                if (!filtered.isEmpty()) {
                    Map<String, Object> group = new LinkedHashMap<>(entry);
                    group.put("documents", filtered);
                    result.add(group);
                }
            } else if (allowed.contains(String.valueOf(entry.get("id")))) {
                result.add(entry);
            }
        }
        return result;
    }
}
