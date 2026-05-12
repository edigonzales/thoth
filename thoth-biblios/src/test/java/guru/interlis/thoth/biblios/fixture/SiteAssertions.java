package guru.interlis.thoth.biblios.fixture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assertions for verifying generated site structure and content in E2E tests.
 */
public final class SiteAssertions {

    private final Path outputRoot;

    public SiteAssertions(Path outputRoot) {
        this.outputRoot = outputRoot;
    }

    /**
     * Verify that a file exists at the expected relative path.
     */
    public void assertFileExists(String relativePath) {
        Path file = outputRoot.resolve(relativePath);
        assertTrue(Files.exists(file), "Expected file to exist: " + relativePath);
    }

    /**
     * Verify that a file does NOT exist.
     */
    public void assertFileNotExists(String relativePath) {
        Path file = outputRoot.resolve(relativePath);
        assertFalse(Files.exists(file), "Expected file to NOT exist: " + relativePath);
    }

    /**
     * Verify that a file contains specific text.
     */
    public void assertFileContains(String relativePath, String expectedText) {
        Path file = outputRoot.resolve(relativePath);
        assertTrue(Files.exists(file), "Expected file to exist: " + relativePath);
        try {
            String content = Files.readString(file);
            assertTrue(content.contains(expectedText),
                "Expected file '" + relativePath + "' to contain '" + expectedText + "'");
        } catch (IOException e) {
            fail("Failed to read file: " + relativePath, e);
        }
    }

    /**
     * Verify that a file does NOT contain specific text.
     */
    public void assertFileNotContains(String relativePath, String unexpectedText) {
        Path file = outputRoot.resolve(relativePath);
        assertTrue(Files.exists(file), "Expected file to exist: " + relativePath);
        try {
            String content = Files.readString(file);
            assertFalse(content.contains(unexpectedText),
                "Expected file '" + relativePath + "' to NOT contain '" + unexpectedText + "'");
        } catch (IOException e) {
            fail("Failed to read file: " + relativePath, e);
        }
    }

    /**
     * Verify that the global start page (index.html) exists and contains expected elements.
     */
    public void assertGlobalStartPage(String siteTitle, String... componentDisplayNames) {
        assertFileExists("index.html");
        assertFileContains("index.html", siteTitle);
        for (String displayName : componentDisplayNames) {
            assertFileContains("index.html", displayName);
        }
    }

    /**
     * Verify that a component landing page exists.
     */
    public void assertComponentLandingPage(String componentId, String displayName) {
        assertFileExists(componentId + "/index.html");
        assertFileContains(componentId + "/index.html", displayName);
    }

    /**
     * Verify that a documentation page exists at the expected route.
     */
    public void assertDocPage(String componentId, String version, String routePath) {
        String path = componentId + "/" + version + "/" + routePath + "/index.html";
        assertFileExists(path);
    }

    /**
     * Verify that the search index exists and contains expected content.
     */
    public void assertSearchIndex(String... expectedTerms) {
        assertFileExists("search-index.json");
        for (String term : expectedTerms) {
            assertFileContains("search-index.json", term);
        }
    }

    /**
     * Verify that site assets are copied.
     */
    public void assertSiteAssets() {
        assertFileExists("site-assets/styles.css");
        assertFileExists("site-assets/lunr.min.js");
        assertFileExists("site-assets/search.js");
        assertFileExists("site-assets/interlis-lab/interlis-lab.js");
        assertFileExists("site-assets/interlis-lab/ili2c.jar");
    }

    /**
     * Verify that a page contains a doc switcher with expected components.
     */
    public void assertDocSwitcher(String relativePath, String... componentDisplayNames) {
        for (String displayName : componentDisplayNames) {
            assertFileContains(relativePath, displayName);
        }
    }

    /**
     * Verify that a page contains a version switcher with expected versions.
     */
    public void assertVersionSwitcher(String relativePath, String... displayVersions) {
        for (String version : displayVersions) {
            assertFileContains(relativePath, version);
        }
    }

    /**
     * Verify that navigation is present on a page.
     */
    public void assertNavigation(String relativePath, String... navTitles) {
        for (String title : navTitles) {
            assertFileContains(relativePath, title);
        }
    }

    /**
     * Verify that breadcrumbs are present on a page.
     */
    public void assertBreadcrumbs(String relativePath) {
        assertFileContains(relativePath, "breadcrumb");
    }
}
