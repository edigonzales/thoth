package guru.interlis.thoth.biblios.nav;

import guru.interlis.thoth.core.ThothBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NavParser and NavTree.
 */
class NavParserTest {

    private final NavParser parser = new NavParser();

    @Test
    void parsesValidNav() throws IOException {
        NavTree tree = parser.parse(resourcePath("test-nav.yml"));

        assertEquals(4, tree.items().size());

        // First item - simple page
        NavItem intro = tree.items().get(0);
        assertEquals("Introduction", intro.title());
        assertEquals("index.adoc", intro.page());
        assertFalse(intro.isGroup());

        // Third item - group with children
        NavItem userGuide = tree.items().get(2);
        assertEquals("User Guide", userGuide.title());
        assertNull(userGuide.page());
        assertTrue(userGuide.isGroup());
        assertEquals(3, userGuide.children().size());

        NavItem installation = userGuide.children().get(0);
        assertEquals("Installation", installation.title());
        assertEquals("installation.adoc", installation.page());
    }

    @Test
    void findsPageByPath() throws IOException {
        NavTree tree = parser.parse(resourcePath("test-nav.yml"));

        NavItem found = tree.findByPage("config.adoc");
        assertNotNull(found);
        assertEquals("Configuration", found.title());
    }

    @Test
    void buildsBreadcrumbs() throws IOException {
        NavTree tree = parser.parse(resourcePath("test-nav.yml"));

        List<NavItem> crumbs = tree.buildBreadcrumbs("config.adoc");
        assertEquals(2, crumbs.size());
        assertEquals("User Guide", crumbs.get(0).title());
        assertEquals("Configuration", crumbs.get(1).title());
    }

    @Test
    void findsPrevAndNext() throws IOException {
        NavTree tree = parser.parse(resourcePath("test-nav.yml"));

        NavItem prev = tree.findPrev("getting-started.adoc");
        assertNotNull(prev);
        assertEquals("Introduction", prev.title());

        NavItem next = tree.findNext("getting-started.adoc");
        assertNotNull(next);
        assertEquals("Installation", next.title()); // First child of User Guide
    }

    @Test
    void parsesFromString() {
        String yaml = """
            items:
              - title: Home
                page: index.adoc
              - title: About
                page: about.adoc
            """;

        NavTree tree = parser.parseString(yaml);
        assertEquals(2, tree.items().size());
        assertEquals("Home", tree.items().get(0).title());
    }

    @Test
    void rejectsNavItemWithoutPageOrChildren(@TempDir Path tempDir) throws IOException {
        String yaml = """
            items:
              - title: Broken
            """;
        Path file = tempDir.resolve("broken.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("must have either 'page' or 'children'"));
    }

    @Test
    void returnsNullForNonExistentPage() throws IOException {
        NavTree tree = parser.parse(resourcePath("test-nav.yml"));
        assertNull(tree.findByPage("nonexistent.adoc"));
    }

    @Test
    void rejectsMissingFile() {
        ThothBuildException ex = assertThrows(ThothBuildException.class, () ->
            parser.parse(Path.of("nonexistent-nav.yml"))
        );
        assertTrue(ex.getMessage().contains("not found"));
        assertEquals(ThothBuildException.ErrorSeverity.ERROR, ex.severity());
    }

    private Path resourcePath(String name) {
        return Path.of(getClass().getClassLoader().getResource(name).getPath());
    }
}
