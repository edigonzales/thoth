package guru.interlis.thoth.biblios.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for routing and URL generation.
 */
class RoutingTest {

    @Test
    void buildsIndexRoute() {
        String route = buildRoute("kataster", "main", "index.adoc");
        assertEquals("/kataster/main/", route);
    }

    @Test
    void buildsPageRoute() {
        String route = buildRoute("kataster", "main", "installation.adoc");
        assertEquals("/kataster/main/installation/", route);
    }

    @Test
    void buildsNestedPageRoute() {
        String route = buildRoute("api", "v2.x", "guide/auth.adoc");
        assertEquals("/api/v2.x/guide/auth/", route);
    }

    @Test
    void siteCatalogFindsComponent() {
        ComponentVersion v1 = new ComponentVersion("test", "main", "Latest", "main", "index.adoc", null, List.of());
        ComponentVersion v2 = new ComponentVersion("test", "v1.x", "1.x", "v1.x", "index.adoc", null, List.of());

        DocComponent comp = new DocComponent("test", "Test Docs", "main", List.of(v1, v2));
        SiteCatalog catalog = new SiteCatalog(List.of(comp));

        assertEquals(comp, catalog.findById("test"));
        assertNull(catalog.findById("nonexistent"));
        assertEquals(v1, catalog.getVersion("test", "main"));
    }

    @Test
    void componentVersionFindsPage() {
        DocPage page1 = new DocPage("test", "main", "index.adoc", "uri", "index", "Home", "Home", "/test/main/", "", List.of(), null, null);
        DocPage page2 = new DocPage("test", "main", "about.adoc", "uri", "about", "About", "About", "/test/main/about/", "", List.of(), page1, null);

        ComponentVersion version = new ComponentVersion("test", "main", "Latest", "main", "index.adoc", null, List.of(page1, page2));

        assertEquals(page1, version.findPageBySourcePath("index.adoc"));
        assertEquals(page2, version.findPageByRoute("/test/main/about/"));
        assertNull(version.findPageBySourcePath("missing.adoc"));
    }

    private String buildRoute(String componentId, String version, String pagePath) {
        String base = "/" + componentId + "/" + version + "/";
        String pageWithoutExtension = pagePath.endsWith(".adoc")
            ? pagePath.substring(0, pagePath.length() - 5)
            : pagePath;

        if ("index".equals(pageWithoutExtension)) {
            return base;
        }
        return base + pageWithoutExtension + "/";
    }
}
