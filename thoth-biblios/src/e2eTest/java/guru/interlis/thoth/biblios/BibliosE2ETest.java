package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.SiteAssertions;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import guru.interlis.thoth.core.DevServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests for thoth-biblios.
 * Tests realistic full build flows with local Git repositories.
 * NOT mock tests — uses real Git repos, real builds, real HTML output.
 */
class BibliosE2ETest {

    @TempDir Path tempDir;

    private Path workRoot;
    private Path outputRoot;
    private Path configFile;
    private DevServer runningServer;

    @AfterEach
    void tearDown() {
        if (runningServer != null) {
            try {
                runningServer.stop();
            } catch (Exception ignored) {
            }
            runningServer = null;
        }
    }

    /**
     * E2E-1: Full build from a single source with multiple pages.
     * Verifies: loading biblios.yml, processing docs, HTML output, assets.
     */
    @Test
    void fullBuildSingleSource() throws Exception {
        Path repoDir = setupTestRepo("single-repo");
        setupOutputDirs();
        Path logoDir = tempDir.resolve("branding");
        Path logoFile = logoDir.resolve("logo.svg");
        Files.createDirectories(logoDir);
        Files.writeString(logoFile, "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");

        new BibliosConfigBuilder()
            .withSiteTitle("Single Source Docs")
            .withSiteLogo("branding/logo.svg")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        // Run full build
        BibliosConfig config = parseConfig();
        buildAndGenerate(config);

        // Assert output
        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertGlobalStartPage("Single Source Docs", "My Documentation");
        assertions.assertSiteAssets();
        assertions.assertFileExists("site-assets/site-logo.svg");
        assertions.assertFileContains("index.html", "class=\"brand-logo\"");
        assertions.assertFileContains("index.html", "src=\"/site-assets/site-logo.svg\"");
        assertions.assertFileContains("site-assets/styles.css", ".brand-logo {");
        assertions.assertFileContains("site-assets/styles.css", "width: 36px;");
        assertions.assertFileContains("site-assets/styles.css", "height: 36px;");
        assertEquals(logoFile.toAbsolutePath().normalize().toString(), config.site().logo());
        assertions.assertFileContains("site-assets/styles.css",
            ".doc-content ul li + li,\n" +
                ".doc-content ol li + li {\n" +
                "    margin-top: 0.4em;\n" +
                "}");
        assertions.assertFileContains("site-assets/styles.css",
            ".doc-content ul li > p,\n" +
                ".doc-content ol li > p {\n" +
                "    margin-bottom: 0;\n" +
                "}");
        assertions.assertFileNotContains("site-assets/styles.css",
            ".doc-content li + li {\n" +
                "    margin-top: 0.5rem;\n" +
                "}");
        assertions.assertDocPage("mydocs", "main", "");
        assertions.assertDocPage("mydocs", "main", "guide");
        assertions.assertSearchIndex("mydocs", "Welcome", "User Guide");
        assertions.assertFileExists("search/index.html");
        assertions.assertFileContains("index.html", "action=\"/search/\"");
        assertions.assertFileContains("search/index.html", "id=\"search-results\"");

        // Ensure page content is not rendered into <head>
        String homePage = Files.readString(outputRoot.resolve("index.html"));
        assertFalse(extractHead(homePage).contains("<div class=\"home\">"));
        String guidePage = Files.readString(outputRoot.resolve("mydocs/main/guide/index.html"));
        assertFalse(extractHead(guidePage).contains("<article class=\"doc-page\">"));
    }

    /**
     * E2E-2: Full build from TWO content sources.
     * Verifies: multi-repo processing, global start page with both sources.
     */
    @Test
    void fullBuildTwoContentSources() throws Exception {
        Path repo1Dir = setupTestRepo("repo-a");
        Path repo2Dir = setupTestRepo("repo-b");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Multi-Source Portal")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repo1Dir, "docs-a", "Documentation A",
                "docs", "main", "main")
            .withSingleSourceGitRepo(repo2Dir, "docs-b", "Documentation B",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertGlobalStartPage("Multi-Source Portal", "Documentation A", "Documentation B");
        assertions.assertComponentLandingPage("docs-a", "Documentation A");
        assertions.assertComponentLandingPage("docs-b", "Documentation B");
        assertions.assertDocPage("docs-a", "main", "");
        assertions.assertDocPage("docs-b", "main", "");
        assertions.assertDocPage("docs-a", "main", "guide");
        assertions.assertDocPage("docs-b", "main", "guide");
        assertions.assertSearchIndex("docs-a", "docs-b", "Welcome");

        // Verify home page doc switcher has placeholder selected and no project preselected
        String homePage = Files.readString(outputRoot.resolve("index.html"));
        assertTrue(optionSelected(homePage, ""), "Home page must select doc switcher placeholder.");
        assertFalse(optionSelected(homePage, "/docs-a/main/"), "Home page must not preselect docs-a.");
        assertFalse(optionSelected(homePage, "/docs-b/main/"), "Home page must not preselect docs-b.");

        // Verify doc switcher on pages
        assertions.assertDocSwitcher("docs-a/main/index.html", "Documentation A", "Documentation B");
        assertions.assertDocSwitcher("docs-b/main/index.html", "Documentation A", "Documentation B");
    }

    /**
     * E2E-3: Multiple versions via branches.
     * Verifies: branch resolution, display_version, version switcher.
     */
    @Test
    void multipleVersionsViaBranches() throws Exception {
        Path repoDir = setupTestRepo("multi-version-repo");
        setupOutputDirs();

        // Create repo with two branches
        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Versioned Docs")
            .withOutputDir(outputRoot)
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: mydocs
                  display_name: My Documentation
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Latest
                    - name: v1.x
                      display_version: Version 1.x
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
                """.formatted(repoDir.toString())))
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        // Verify both versions exist
        assertions.assertDocPage("mydocs", "main", "");
        assertions.assertDocPage("mydocs", "v1.x", "");
        assertions.assertDocPage("mydocs", "main", "guide");
        assertions.assertDocPage("mydocs", "v1.x", "guide");

        // Verify version switcher
        assertions.assertVersionSwitcher("mydocs/main/index.html", "Latest", "Version 1.x");
        assertions.assertVersionSwitcher("mydocs/v1.x/index.html", "Latest", "Version 1.x");

        // Verify content differs between versions
        assertions.assertFileContains("mydocs/main/guide/index.html", "User Guide");
        assertions.assertFileContains("mydocs/v1.x/guide/index.html", "This is the v1.x user guide.");
    }

    /**
     * E2E-4: Complete HTML site structure verification.
     * Verifies: start page, component pages, navigation, breadcrumbs, search.
     */
    @Test
    void completeHtmlSiteStructure() throws Exception {
        Path repoDir = setupTestRepo("structure-repo");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Structure Test Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);

        // Global start page
        assertions.assertGlobalStartPage("Structure Test Docs", "My Documentation");
        assertions.assertFileContains("index.html", "<h1");
        assertions.assertFileContains("index.html", "Structure Test Docs");

        // Component landing page
        assertions.assertComponentLandingPage("mydocs", "My Documentation");
        assertions.assertFileContains("mydocs/index.html", "My Documentation");

        // Documentation pages
        assertions.assertDocPage("mydocs", "main", "");
        assertions.assertDocPage("mydocs", "main", "guide");

        // Navigation on content pages
        assertions.assertNavigation("mydocs/main/guide/index.html",
            "Welcome", "User Guide", "Advanced");
        assertions.assertFileContains("mydocs/main/guide/index.html", "href=\"/mydocs/main/\"");
        assertions.assertFileNotContains("mydocs/main/guide/index.html", "href=\"/mydocs/main/index/\"");

        // Breadcrumbs
        assertions.assertBreadcrumbs("mydocs/main/guide/index.html");
        assertions.assertFileNotContains("mydocs/main/guide/index.html", "href=\"///\"");

        // Search index
        assertions.assertSearchIndex("mydocs", "Welcome", "User Guide", "Configuration");
    }

    /**
     * E2E-5: Single-page mode renders one page with heading-based sidebar and chapter breadcrumb.
     */
    @Test
    void singlePageModeRendersHeadingSidebarAndChapterBreadcrumb() throws Exception {
        Path repoDir = tempDir.resolve("single-page-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withSinglePageDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Single Page Portal")
            .withOutputDir(outputRoot)
            .withSidebarTocDepth(3)
            .withContentToc("off")
            .withSinglePageSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "master.adoc", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertDocPage("mydocs", "main", "");
        assertions.assertFileNotExists("mydocs/main/einleitung/index.html");
        assertions.assertFileContains("mydocs/main/index.html", "data-single-page-mode=\"true\"");
        assertions.assertFileContains("mydocs/main/index.html", "data-chapter-breadcrumb=\"enabled\"");
        assertions.assertFileContains("mydocs/main/index.html", "data-chapter-id=");
        assertions.assertFileContains("mydocs/main/index.html", "id=\"chapter-breadcrumb-root\"");
        assertions.assertFileContains("mydocs/main/index.html", "id=\"chapter-breadcrumb-trail\"");
        assertions.assertFileContains("mydocs/main/index.html", "id=\"chapter-breadcrumb-current\"");
        assertions.assertFileContains("mydocs/main/index.html", "href=\"/mydocs/main/#");
        assertions.assertFileContains("mydocs/main/index.html", "class=\"anchor\"");
        assertions.assertFileContains("mydocs/main/index.html", "Einleitung");
        assertions.assertFileContains("mydocs/main/index.html", "Grundprinzipien");
        assertions.assertFileNotContains("mydocs/main/index.html", ">1 Einleitung<");
        assertions.assertFileNotContains("mydocs/main/index.html", "id=\"toc\"");
        assertions.assertFileContains("site-assets/styles.css", "scroll-margin-top: var(--anchor-offset);");
        assertions.assertFileContains("site-assets/styles.css", "content: \"\\00A7\";");
        assertions.assertFileContains("site-assets/styles.css", "h2:hover > a.anchor");
        assertions.assertFileContains("site-assets/styles.css", ".breadcrumbs {");
        assertions.assertFileContains("site-assets/styles.css", "top: var(--site-header-height);");
        assertions.assertFileContains("site-assets/styles.css", ".nav-toggle");
        assertions.assertFileContains("site-assets/styles.css", ".nav-item.is-collapsible.is-collapsed > .nav-subtree");
        assertions.assertFileContains("site-assets/search.js", "IntersectionObserver");
        assertions.assertFileContains("site-assets/search.js", "setBranchExpanded");
        assertions.assertFileContains("site-assets/search.js", "bindCollapsibleToggles");
        assertions.assertFileNotContains("site-assets/search.js", "history.replaceState");
    }

    @Test
    void singlePageModeShowsAppendixRoleButHidesOtherUnnumberedChaptersFromSidebar() throws Exception {
        Path repoDir = tempDir.resolve("single-page-unnumbered-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withSinglePageDocsIncludingUnnumberedChapter();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Single Page Unnumbered Portal")
            .withOutputDir(outputRoot)
            .withSidebarTocDepth(3)
            .withContentToc("off")
            .withSinglePageSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "master.adoc", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/index.html", "data-chapter-title=\"Einleitung\"");
        assertions.assertFileContains("mydocs/main/index.html", "data-chapter-title=\"Grundprinzipien\"");
        assertions.assertFileNotContains("mydocs/main/index.html", "data-chapter-title=\"Erweiterungen von INTERLIS 2.4 gegenüber INTERLIS 2.3\"");
        assertions.assertFileContains("mydocs/main/index.html", "data-chapter-title=\"Anhang A - foo bar\"");
        assertions.assertFileNotContains("mydocs/main/index.html", "aria-label=\"Toggle subsections for Anhang A - foo bar\"");
        assertions.assertFileContains("mydocs/main/index.html", "href=\"/mydocs/main/#_anhang_a_foo_bar\"");
        assertions.assertFileContains("mydocs/main/index.html", "id=\"chapter-breadcrumb-current\">Einleitung<");
        assertions.assertFileContains("mydocs/main/index.html", "Erweiterungen von INTERLIS 2.4 gegenüber INTERLIS 2.3");
    }

    /**
     * E2E-6: Search language mode is exposed to the rendered HTML.
     * Verifies: ui.search_language_mode reaches templates and can drive client-side mode selection.
     */
    @Test
    void searchLanguageModeIsRendered() throws Exception {
        Path repoDir = setupTestRepo("search-mode-repo");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Search Mode Docs")
            .withOutputDir(outputRoot)
            .withSearchLanguageMode("english_default")
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("index.html", "data-search-language-mode=\"english_default\"");
        assertions.assertFileContains("search/index.html", "data-search-language-mode=\"english_default\"");
        assertions.assertFileContains("mydocs/main/index.html", "data-search-language-mode=\"english_default\"");
    }

    /**
     * E2E-7: NOTE admonitions are rendered and styled with localized caption.
     */
    @Test
    void noteAdmonitionIsStyledAndLocalized() throws Exception {
        Path repoDir = tempDir.resolve("note-admonition-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withNoteAdmonitionInGuide();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Localized Note Docs")
            .withSiteLanguage("de")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"admonitionblock note\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "title=\"Note\"");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .admonitionblock.note {");
        assertions.assertFileContains("site-assets/styles.css", "#f1fafe");
        assertions.assertFileContains("site-assets/styles.css", "#d4e7f4");
        assertions.assertFileContains("site-assets/styles.css", "#1E4ED8");
        assertions.assertFileContains("site-assets/styles.css", "background-color: #60A6FA");
        assertions.assertFileContains("site-assets/styles.css", "mask-image: url(\"data:image/svg+xml");
        assertions.assertFileContains("site-assets/styles.css", "-webkit-mask-image: url(\"data:image/svg+xml");
        assertions.assertFileContains("site-assets/styles.css", "class='bi bi-info-circle'");
        assertions.assertFileContains("site-assets/styles.css", "M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16");
        assertions.assertFileContains("site-assets/styles.css", "m8.93 6.588-2.29.287-.082.38");
        assertions.assertFileContains("site-assets/styles.css", "1.178-.252 1.465-.598");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .admonitionblock.note td.icon .title::before");
    }

    /**
     * E2E-7b: All standard admonition icon titles are rendered using chapter heading font.
     */
    @Test
    void standardAdmonitionIconTitlesUseChapterHeadingFont() throws Exception {
        Path repoDir = tempDir.resolve("all-admonitions-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withAllStandardAdmonitionsInGuide();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("All Admonitions Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"admonitionblock note\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"admonitionblock tip\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"admonitionblock important\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"admonitionblock warning\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"admonitionblock caution\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "title=\"Note\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "title=\"Tip\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "title=\"Important\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "title=\"Warning\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "title=\"Caution\"");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .admonitionblock td.icon i[class*=\"icon-\"]::after {");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .admonitionblock td.icon .title {");
        assertions.assertFileContains("site-assets/styles.css", "content: attr(title);");
        assertions.assertFileContains("site-assets/styles.css", "font-family: var(--font-chapter-headings);");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content span.small {");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .paragraph.small > p {");
        assertions.assertFileContains("site-assets/styles.css", "font-size: 0.85rem;");
        assertions.assertFileContains("site-assets/styles.css", "line-height: 1.4;");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .admonitionblock.tip {");
        assertions.assertFileContains("site-assets/styles.css", "#FFF7D6");
        assertions.assertFileContains("site-assets/styles.css", "#F2D27A");
        assertions.assertFileContains("site-assets/styles.css", "#9A6700");
        assertions.assertFileContains("site-assets/styles.css", "background-color: #F4B400");
        assertions.assertFileContains("site-assets/styles.css", "class='bi bi-lightbulb'");
        assertions.assertFileContains("site-assets/styles.css", "M2 6a6 6 0 1 1 10.174 4.31");
        assertions.assertFileContains("site-assets/styles.css", "M7 14.5h2v.5a1 1 0 0 1-2 0z");
    }

    /**
     * E2E-8: Tables render with caption and dedicated table styles.
     */
    @Test
    void tableCaptionAndGridStylesAreApplied() throws Exception {
        Path repoDir = tempDir.resolve("table-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withTableInGuide();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Table Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"tableblock");
        assertions.assertFileContains("mydocs/main/guide/index.html", "<caption class=\"title\">Table");
        assertions.assertFileContains("mydocs/main/guide/index.html", "Deployment Matrix");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content table.tableblock {");
        assertions.assertFileContains("site-assets/styles.css", "border-collapse: collapse;");
        assertions.assertFileContains("site-assets/styles.css", "border: 1px solid var(--color-border);");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content table.tableblock th,");
        assertions.assertFileContains("site-assets/styles.css", "line-height: 1.15;");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content table.tableblock td > p,");
        assertions.assertFileContains("site-assets/styles.css", "margin-bottom: 0;");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content table.tableblock > caption.title {");
        assertions.assertFileContains("site-assets/styles.css", "font-style: italic;");
        assertions.assertFileContains("site-assets/styles.css", "font-size: 0.9rem;");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .imageblock > .title {");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .admonitionblock.note {");
    }

    /**
     * E2E-9: Sidebar blocks (****) are rendered and styled.
     */
    @Test
    void sidebarBlockIsStyled() throws Exception {
        Path repoDir = tempDir.resolve("sidebar-block-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withSidebarBlockInGuide();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Sidebar Block Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"sidebarblock\"");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .sidebarblock {");
        assertions.assertFileContains("site-assets/styles.css", "--color-sidebarblock-bg: var(--ng-50);");
        assertions.assertFileContains("site-assets/styles.css", "--color-sidebarblock-border: var(--ng-100);");
    }

    /**
     * E2E-9b: Keypoint sidebar block keeps base sidebar style and adds dedicated keypoint style.
     */
    @Test
    void keypointSidebarBlockIsStyledWithoutAffectingOtherSidebarBlocks() throws Exception {
        Path repoDir = tempDir.resolve("keypoint-sidebar-block-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withKeypointSidebarBlockInGuide();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Keypoint Sidebar Block Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"sidebarblock keypoint\"");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .sidebarblock.keypoint {");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .sidebarblock.keypoint::before {");
        assertions.assertFileContains("site-assets/styles.css", "top: calc((1em * var(--lh-body) - var(--keypoint-triangle-height)) / 2);");
        assertions.assertFileContains("site-assets/styles.css", "margin: 1.25rem 0 1.25rem 1.5rem;");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .sidebarblock {");
        assertions.assertFileContains("site-assets/styles.css", "border: 1px solid var(--color-sidebarblock-border);");
    }

    /**
     * E2E-10: Listing callouts render marker circles and colist legend styling.
     */
    @Test
    void listingCalloutsAreRenderedAndStyled() throws Exception {
        Path repoDir = tempDir.resolve("listing-callout-repo");
        Files.createDirectories(repoDir);
        setupOutputDirs();

        new TestRepoBuilder(repoDir).withListingCalloutInGuide();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Listing Callout Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"conum\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "data-value=\"1\"");
        assertions.assertFileContains("mydocs/main/guide/index.html", "class=\"colist");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content i.conum::before {");
        assertions.assertFileContains("site-assets/styles.css", "content: attr(data-value);");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content i.conum + b {");
        assertions.assertFileContains("site-assets/styles.css", ".doc-content .colist > table td:first-child {");
    }

    /**
     * E2E-7: Global search index is correctly populated.
     * Verifies: search-index.json contains entries from all components/versions.
     */
    @Test
    void globalSearchIndexPopulated() throws Exception {
        Path repo1Dir = setupTestRepo("search-repo-a");
        Path repo2Dir = setupTestRepo("search-repo-b");
        setupOutputDirs();

        // Create repo2 with second branch
        new TestRepoBuilder(repo2Dir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Search Test Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repo1Dir, "docs-a", "Documentation A",
                "docs", "main", "main")
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: docs-b
                  display_name: Documentation B
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Latest
                    - name: v1.x
                      display_version: Version 1.x
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
                """.formatted(repo2Dir.toString())))
            .writeTo(configFile);

        buildAndGenerate(config);

        // Verify search index
        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertSearchIndex("docs-a", "docs-b", "Welcome", "User Guide");

        // Verify JSON structure
        String searchJson = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchJson.contains("docs-a"));
        assertTrue(searchJson.contains("docs-b"));
        assertTrue(searchJson.contains("main"));
        assertTrue(searchJson.contains("v1.x"));
        assertTrue(searchJson.contains("Latest"));
        assertTrue(searchJson.contains("Welcome"));
        assertTrue(searchJson.contains("User Guide"));
    }

    /**
     * E2E-8: DevServer (serve) can start and deliver generated pages.
     * Verifies: HTTP server starts, serves HTML content, correct routes.
     */
    @Test
    void serveDeliversGeneratedPages() throws Exception {
        Path repoDir = setupTestRepo("serve-repo");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Serve Test Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        // Build first
        buildAndGenerate(config);

        // Start DevServer on a free port
        int port = findFreePort();
        runningServer = new DevServer(outputRoot, port);
        runningServer.start();
        Thread.sleep(1000); // Wait for server to start

        // Create HTTP client
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Test global start page
        HttpResponse<String> homeResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/index.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, homeResponse.statusCode());
        assertTrue(homeResponse.body().contains("Serve Test Docs"));
        assertTrue(homeResponse.body().contains("My Documentation"));

        // Test documentation page
        HttpResponse<String> docResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/mydocs/main/index.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, docResponse.statusCode());
        assertTrue(docResponse.body().contains("Welcome"));

        // Test search index
        HttpResponse<String> searchResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/search-index.json"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, searchResponse.statusCode());
        assertTrue(searchResponse.body().contains("mydocs"));

        // Test search page with query parameter
        HttpResponse<String> searchPageResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/search/?q=Welcome"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, searchPageResponse.statusCode());
        assertTrue(searchPageResponse.body().contains("id=\"search-results\""));
        assertTrue(searchPageResponse.body().contains("action=\"/search/\""));

        // Test CSS asset
        HttpResponse<String> cssResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/site-assets/styles.css"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, cssResponse.statusCode());
        assertTrue(cssResponse.body().contains("body") || cssResponse.body().contains("."));
    }

    /**
     * E2E-9: Version switcher links to correct version start pages.
     * Verifies: switching from main to v1.x leads to correct version's start page.
     */
    @Test
    void versionSwitcherLinksCorrectly() throws Exception {
        Path repoDir = setupTestRepo("switcher-repo");
        setupOutputDirs();

        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Switcher Test Docs")
            .withOutputDir(outputRoot)
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: mydocs
                  display_name: My Documentation
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Latest
                    - name: v1.x
                      display_version: Version 1.x
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
                """.formatted(repoDir.toString())))
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);

        // Verify both version start pages exist
        assertions.assertFileExists("mydocs/main/index.html");
        assertions.assertFileExists("mydocs/v1.x/index.html");

        // Verify version switcher contains links to both versions
        String mainPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(mainPage.contains("/mydocs/main/"));
        assertTrue(mainPage.contains("/mydocs/v1.x/"));

        String v1xPage = Files.readString(outputRoot.resolve("mydocs/v1.x/index.html"));
        assertTrue(v1xPage.contains("/mydocs/main/"));
        assertTrue(v1xPage.contains("/mydocs/v1.x/"));
    }

    /**
     * E2E-8: Doc switcher links to correct component default versions.
     * Verifies: switching from docs-a to docs-b leads to docs-b's default version.
     */
    @Test
    void docSwitcherLinksToDefaultVersion() throws Exception {
        Path repo1Dir = setupTestRepo("docswitch-repo-a");
        Path repo2Dir = setupTestRepo("docswitch-repo-b");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Doc Switcher Test")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repo1Dir, "docs-a", "Documentation A",
                "docs", "main", "main")
            .withSingleSourceGitRepo(repo2Dir, "docs-b", "Documentation B",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);

        // Verify doc switcher on docs-a page links to docs-b
        String docsAPage = Files.readString(outputRoot.resolve("docs-a/main/index.html"));
        assertTrue(docsAPage.contains("Documentation B"));
        assertTrue(docsAPage.contains("/docs-b/main/"));
        assertTrue(optionSelected(docsAPage, "/docs-a/main/"), "docs-a page must keep docs-a selected.");
        assertFalse(optionSelected(docsAPage, ""), "docs-a page must not select placeholder.");

        // Verify doc switcher on docs-b page links to docs-a
        String docsBPage = Files.readString(outputRoot.resolve("docs-b/main/index.html"));
        assertTrue(docsBPage.contains("Documentation A"));
        assertTrue(docsBPage.contains("/docs-a/main/"));
        assertTrue(optionSelected(docsBPage, "/docs-b/main/"), "docs-b page must keep docs-b selected.");
        assertFalse(optionSelected(docsBPage, ""), "docs-b page must not select placeholder.");
    }

    /**
     * E2E-9: Edit and Source links are rendered when configured.
     * Verifies: edit_url_pattern and source_url_pattern produce correct URLs on pages.
     */
    @Test
    void editAndSourceLinksRendered() throws Exception {
        Path repoDir = setupTestRepo("editlink-repo");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Edit Link Test Docs")
            .withOutputDir(outputRoot)
            .withEditLink(true, "https://github.com/example/docs/edit/{branch}/{path}")
            .withSourceLink(true, "https://github.com/example/docs/blob/{branch}/{path}")
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertDocPage("mydocs", "main", "");
        assertions.assertDocPage("mydocs", "main", "guide");

        // Verify edit link on guide page
        String guidePage = Files.readString(outputRoot.resolve("mydocs/main/guide/index.html"));
        assertTrue(guidePage.contains("Edit this page"));
        assertTrue(guidePage.contains("https://github.com/example/docs/edit/main/docs/guide.adoc"));

        // Verify source link on guide page
        assertTrue(guidePage.contains("View source"));
        assertTrue(guidePage.contains("https://github.com/example/docs/blob/main/docs/guide.adoc"));

        // Verify NO edit/source links on index page when pattern is set but page is index
        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        // Edit/source links should still appear on index page since they have source paths
        assertTrue(indexPage.contains("Edit this page") || indexPage.contains("View source"));
    }

    /**
     * E2E-10: Edit/Source links are absent when not configured.
     */
    @Test
    void editAndSourceLinksAbsentWhenNotConfigured() throws Exception {
        Path repoDir = setupTestRepo("noeditlink-repo");
        setupOutputDirs();

        // Default BibliosConfigBuilder does NOT set edit/source links
        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("No Edit Link Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertDocPage("mydocs", "main", "guide");

        String guidePage = Files.readString(outputRoot.resolve("mydocs/main/guide/index.html"));
        assertFalse(guidePage.contains("Edit this page"));
        assertFalse(guidePage.contains("View source"));
    }

    /**
     * E2E-11: Version switcher navigates to equivalent page (same source path) in target version.
     * Verifies: when on guide page in main, version switcher links to guide page in v1.x.
     */
    @Test
    void versionSwitcherNavigatesToEquivalentPage() throws Exception {
        Path repoDir = setupTestRepo("versionmap-repo");
        setupOutputDirs();

        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Version Map Test Docs")
            .withOutputDir(outputRoot)
            .withVersionSwitchMode("equivalent_page")
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: mydocs
                  display_name: My Documentation
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Latest
                    - name: v1.x
                      display_version: Version 1.x
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
                """.formatted(repoDir.toString())))
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);

        // Both versions have guide.adoc
        assertions.assertDocPage("mydocs", "main", "guide");
        assertions.assertDocPage("mydocs", "v1.x", "guide");

        // Version switcher on main/guide should link to v1.x/guide (same source path)
        String mainGuide = Files.readString(outputRoot.resolve("mydocs/main/guide/index.html"));
        assertTrue(mainGuide.contains("/mydocs/v1.x/guide/"),
            "Version switcher on main/guide should link to v1.x/guide");

        // Version switcher on v1.x/guide should link to main/guide
        String v1xGuide = Files.readString(outputRoot.resolve("mydocs/v1.x/guide/index.html"));
        assertTrue(v1xGuide.contains("/mydocs/main/guide/"),
            "Version switcher on v1.x/guide should link to main/guide");
    }

    /**
     * E2E-12: Default version switch mode (start_page) links to version root.
     */
    @Test
    void versionSwitcherDefaultsToStartPageMode() throws Exception {
        Path repoDir = setupTestRepo("versionstart-repo");
        setupOutputDirs();

        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Version Start Mode Test Docs")
            .withOutputDir(outputRoot)
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: mydocs
                  display_name: My Documentation
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Latest
                    - name: v1.x
                      display_version: Version 1.x
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
                """.formatted(repoDir.toString())))
            .writeTo(configFile);

        buildAndGenerate(config);

        String mainGuide = Files.readString(outputRoot.resolve("mydocs/main/guide/index.html"));
        assertTrue(mainGuide.contains("option value=\"/mydocs/v1.x/\""),
            "Default switch mode should link to target version root.");
        assertFalse(mainGuide.contains("option value=\"/mydocs/v1.x/guide/\""),
            "Default switch mode must not map to equivalent page.");
    }

    /**
     * E2E-13: start_page is mapped to /<component>/<version>/ regardless of filename.
     */
    @Test
    void customStartPageMapsToVersionRoot() throws Exception {
        Path repoDir = setupTestRepo("custom-startpage-repo");
        setupOutputDirs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Custom Start Page Test")
            .withOutputDir(outputRoot)
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: mydocs
                  display_name: My Documentation
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Latest
                  start_path: docs
                  default_version: main
                  start_page: guide.adoc
                  navigation:
                    file: nav.yml
                """.formatted(repoDir.toString())))
            .writeTo(configFile);

        buildAndGenerate(config);

        SiteAssertions assertions = new SiteAssertions(outputRoot);
        assertions.assertFileContains("mydocs/main/index.html", "User Guide");
        assertions.assertFileNotExists("mydocs/main/guide/index.html");
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private Path setupTestRepo(String name) throws Exception {
        Path repoDir = tempDir.resolve(name);
        Files.createDirectories(repoDir);
        new TestRepoBuilder(repoDir).withBasicDocs();
        return repoDir;
    }

    private void setupOutputDirs() throws IOException {
        workRoot = tempDir.resolve("work");
        outputRoot = tempDir.resolve("output");
        configFile = tempDir.resolve("biblios.yml");
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);
    }

    private BibliosConfig parseConfig() throws Exception {
        BibliosConfigParser parser = new BibliosConfigParser();
        return parser.parse(configFile);
    }

    private void buildAndGenerate(BibliosConfig config) throws Exception {
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }
    }

    private int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8765; // fallback
        }
    }

    private String extractHead(String html) {
        int headStart = html.indexOf("<head>");
        int headEnd = html.indexOf("</head>");
        if (headStart < 0 || headEnd < 0 || headEnd <= headStart) {
            return "";
        }
        return html.substring(headStart, headEnd);
    }

    private boolean optionSelected(String html, String value) {
        Pattern pattern = Pattern.compile(
            "<option\\b[^>]*value=\"" + Pattern.quote(value) + "\"[^>]*\\bselected\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        return pattern.matcher(html).find();
    }
}
