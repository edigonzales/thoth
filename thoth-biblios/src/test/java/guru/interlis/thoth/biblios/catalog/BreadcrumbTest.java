package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.nav.NavItem;
import guru.interlis.thoth.biblios.nav.NavParser;
import guru.interlis.thoth.biblios.nav.NavTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for breadcrumb generation.
 */
class BreadcrumbTest {

    @Test
    void buildsBreadcrumbsForLeafPage() {
        NavTree nav = createTestNav();

        List<NavItem> crumbs = nav.buildBreadcrumbs("config.adoc");
        assertEquals(2, crumbs.size());
        assertEquals("User Guide", crumbs.get(0).title());
        assertEquals("Configuration", crumbs.get(1).title());
    }

    @Test
    void buildsBreadcrumbsForTopLevelPage() {
        NavTree nav = createTestNav();

        List<NavItem> crumbs = nav.buildBreadcrumbs("index.adoc");
        assertEquals(1, crumbs.size());
        assertEquals("Introduction", crumbs.get(0).title());
    }

    @Test
    void returnsEmptyForUnknownPage() {
        NavTree nav = createTestNav();

        List<NavItem> crumbs = nav.buildBreadcrumbs("nonexistent.adoc");
        assertTrue(crumbs.isEmpty());
    }

    @Test
    void buildsBreadcrumbsForDeepNestedPage() {
        NavTree nav = createTestNavWithDeepNesting();

        List<NavItem> crumbs = nav.buildBreadcrumbs("advanced.adoc");
        assertEquals(3, crumbs.size());
        assertEquals("Guide", crumbs.get(0).title());
        assertEquals("Advanced Topics", crumbs.get(1).title());
        assertEquals("Advanced Usage", crumbs.get(2).title());
    }

    @Test
    void breadcrumbCurrentHasNullRoute() {
        // In DocPage, breadcrumbs are built with null route for current page
        DocPage.Breadcrumb current = new DocPage.Breadcrumb("Current Page", null);
        assertTrue(current.isCurrent());
        assertNull(current.route());

        DocPage.Breadcrumb link = new DocPage.Breadcrumb("Parent", "/parent/");
        assertFalse(link.isCurrent());
        assertEquals("/parent/", link.route());
    }

    private NavTree createTestNav() {
        return new NavTree(List.of(
            new NavItem("Introduction", "index.adoc", List.of()),
            new NavItem("Getting Started", "getting-started.adoc", List.of()),
            new NavItem("User Guide", null, List.of(
                new NavItem("Installation", "installation.adoc", List.of()),
                new NavItem("Configuration", "config.adoc", List.of()),
                new NavItem("CLI Usage", "cli.adoc", List.of())
            ))
        ));
    }

    private NavTree createTestNavWithDeepNesting() {
        return new NavTree(List.of(
            new NavItem("Home", "index.adoc", List.of()),
            new NavItem("Guide", null, List.of(
                new NavItem("Basics", "basics.adoc", List.of()),
                new NavItem("Advanced Topics", null, List.of(
                    new NavItem("Advanced Usage", "advanced.adoc", List.of())
                ))
            ))
        ));
    }
}
