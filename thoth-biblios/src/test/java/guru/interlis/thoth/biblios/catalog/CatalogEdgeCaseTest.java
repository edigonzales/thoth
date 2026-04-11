package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.config.*;
import guru.interlis.thoth.biblios.nav.NavTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for catalog edge cases.
 */
class CatalogEdgeCaseTest {

    @Test
    void handlesEmptyPagesList() {
        ComponentVersion version = new ComponentVersion(
            "test", "main", "Latest", "main", "index.adoc", null, List.of()
        );
        assertTrue(version.pages().isEmpty());
        assertNull(version.findPageBySourcePath("index.adoc"));
    }

    @Test
    void handlesNullNavigation() {
        ComponentVersion version = new ComponentVersion(
            "test", "main", "Latest", "main", "index.adoc", null, List.of()
        );
        assertNull(version.navigation());
    }

    @Test
    void handlesMultipleVersions() {
        ComponentVersion v1 = new ComponentVersion("test", "v1", "1.0", "v1", "index.adoc", null, List.of());
        ComponentVersion v2 = new ComponentVersion("test", "v2", "2.0", "v2", "index.adoc", null, List.of());

        DocComponent comp = new DocComponent("test", "Test", "v2", List.of(v1, v2));
        SiteCatalog catalog = new SiteCatalog(List.of(comp));

        assertEquals(2, comp.versions().size());
        assertNotNull(catalog.getVersion("test", "v1"));
        assertNotNull(catalog.getVersion("test", "v2"));
        assertNull(catalog.getVersion("test", "v3"));
    }

    @Test
    void handlesMissingComponent() {
        ComponentVersion v1 = new ComponentVersion("test", "main", "Main", "main", "index.adoc", null, List.of());
        DocComponent comp = new DocComponent("test", "Test", "main", List.of(v1));
        SiteCatalog catalog = new SiteCatalog(List.of(comp));

        assertNull(catalog.findById("nonexistent"));
    }

    @Test
    void rejectsEmptyVersionsList() {
        assertThrows(IllegalArgumentException.class, () ->
            new DocComponent("test", "Test", "main", List.of())
        );
    }

    @Test
    void handlesSpecialCharactersInPagePath() {
        DocPage page = new DocPage(
            "test", "main", "guide/advanced-topics.adoc", "uri",
            "guide-advanced-topics", "Advanced Topics", "Advanced Topics",
            "/test/main/guide/advanced-topics/", "<p>Content</p>",
            List.of(), null, null
        );

        assertEquals("/test/main/guide/advanced-topics/", page.route());
        assertEquals("guide/advanced-topics.adoc", page.sourcePath());
    }
}
