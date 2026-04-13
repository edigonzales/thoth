package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full Biblios build pipeline.
 * Uses local Git repositories to test Git fetching, catalog building, and site generation.
 */
class BibliosIntegrationTest {

    @TempDir Path tempDir;

    @Test
    void fullBuildPipeline() throws Exception {
        Path repoDir = tempDir.resolve("test-repo");
        Path workRoot = tempDir.resolve("work");
        Path outputRoot = tempDir.resolve("output");
        Path configFile = tempDir.resolve("biblios.yml");
        Path logoDir = tempDir.resolve("branding");
        Path logoFile = logoDir.resolve("logo.svg");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);
        Files.createDirectories(logoDir);
        Files.writeString(logoFile, "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");

        // Create a local Git repository with AsciiDoc content
        new TestRepoBuilder(repoDir).withBasicDocs();

        // Create config
        new BibliosConfigBuilder()
            .withSiteTitle("Integration Test Docs")
            .withSiteLogo("branding/logo.svg")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        // 1. Parse config
        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);
        assertEquals("Integration Test Docs", config.site().title());
        assertEquals(logoFile.toAbsolutePath().normalize().toString(), config.site().logo());
        assertEquals(1, config.content().sources().size());

        // 2. Build catalog
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();

            // Verify catalog
            assertEquals(1, catalog.components().size());
            DocComponent component = catalog.findById("mydocs");
            assertNotNull(component);
            assertEquals("My Documentation", component.displayName());
            assertEquals(1, component.versions().size());

            ComponentVersion version = component.getVersion("main");
            assertNotNull(version);
            assertEquals("main", version.version());
            assertEquals("main", version.displayVersion());

            // Verify pages
            assertFalse(version.pages().isEmpty());
            assertTrue(version.pages().stream().anyMatch(p -> p.sourcePath().equals("index.adoc")));
            assertTrue(version.pages().stream().anyMatch(p -> p.sourcePath().equals("guide.adoc")));

            // Verify routing
            var indexPage = version.findPageBySourcePath("index.adoc");
            assertNotNull(indexPage);
            assertEquals("/mydocs/main/", indexPage.route());

            var guidePage = version.findPageBySourcePath("guide.adoc");
            assertNotNull(guidePage);
            assertEquals("/mydocs/main/guide/", guidePage.route());

            // 3. Generate site
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        // 4. Verify output
        assertTrue(Files.exists(outputRoot.resolve("index.html")));
        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/index.html")));
        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/guide/index.html")));
        assertTrue(Files.exists(outputRoot.resolve("search/index.html")));
        assertTrue(Files.exists(outputRoot.resolve("search-index.json")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/styles.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/site-logo.svg")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/lunr.min.js")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/search.js")));

        // 5. Verify content
        String homePage = Files.readString(outputRoot.resolve("index.html"));
        assertTrue(homePage.contains("Integration Test Docs"));
        assertTrue(homePage.contains("My Documentation"));
        assertTrue(homePage.contains("class=\"brand-logo\""));
        assertTrue(homePage.contains("src=\"/site-assets/site-logo.svg\""));
        assertTrue(homePage.contains("action=\"/search/\""));
        String homeHead = extractHead(homePage);
        assertFalse(homeHead.contains("<div class=\"home\">"));

        String searchPage = Files.readString(outputRoot.resolve("search/index.html"));
        assertTrue(searchPage.contains("id=\"search-results\""));
        assertTrue(searchPage.contains("data-search-language-mode=\"multilingual_safe\""));

        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(indexPage.contains("Welcome"));

        String styles = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(styles.contains(".content {"));
        assertTrue(styles.contains(".doc-content .imageblock > .content {\n    padding: 0;"));
        assertTrue(styles.contains(".doc-content .listingblock > .content {\n    padding: 0;"));

        String searchIndex = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchIndex.contains("mydocs"));
        assertTrue(searchIndex.contains("Welcome"));
    }

    @Test
    void multiBranchBuild() throws Exception {
        Path repoDir = tempDir.resolve("test-repo");
        Path workRoot = tempDir.resolve("work");
        Path outputRoot = tempDir.resolve("output");
        Path configFile = tempDir.resolve("biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        // Create a local Git repository with two branches
        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        // Create config
        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Multi-Version Docs")
            .withOutputDir(outputRoot)
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: mydocs
                  display_name: Multi-Version Docs
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

        // Parse and build
        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig parsedConfig = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(parsedConfig, workRoot)) {
            SiteCatalog catalog = builder.build();

            DocComponent component = catalog.findById("mydocs");
            assertNotNull(component);
            assertEquals(2, component.versions().size());

            ComponentVersion main = component.getVersion("main");
            ComponentVersion v1x = component.getVersion("v1.x");
            assertNotNull(main);
            assertNotNull(v1x);
            assertEquals("Latest", main.displayVersion());
            assertEquals("Version 1.x", v1x.displayVersion());
            assertNotNull(v1x.findPageBySourcePath("guide.adoc"));
            assertTrue(v1x.findPageBySourcePath("guide.adoc").html().contains("v1.x"));
        }
    }

    @Test
    void multiSourceBuild() throws Exception {
        Path repo1Dir = tempDir.resolve("repo1");
        Path repo2Dir = tempDir.resolve("repo2");
        Path workRoot = tempDir.resolve("work");
        Path outputRoot = tempDir.resolve("output");
        Path configFile = tempDir.resolve("biblios.yml");

        Files.createDirectories(repo1Dir);
        Files.createDirectories(repo2Dir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        // Create two separate repos
        new TestRepoBuilder(repo1Dir).withBasicDocs();
        new TestRepoBuilder(repo2Dir).withBasicDocs();

        // Create config with two sources
        new BibliosConfigBuilder()
            .withSiteTitle("Multi-Source Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repo1Dir, "docs-a", "Documentation A",
                "docs", "main", "main")
            .withSingleSourceGitRepo(repo2Dir, "docs-b", "Documentation B",
                "docs", "main", "main")
            .writeTo(configFile);

        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();

            assertEquals(2, catalog.components().size());
            assertNotNull(catalog.findById("docs-a"));
            assertNotNull(catalog.findById("docs-b"));
            assertEquals("Documentation A", catalog.findById("docs-a").displayName());
            assertEquals("Documentation B", catalog.findById("docs-b").displayName());
        }
    }

    @Test
    void includesPrismAssetsByDefault() throws Exception {
        Path repoDir = tempDir.resolve("prism-default-repo");
        Path workRoot = tempDir.resolve("work-prism-default");
        Path outputRoot = tempDir.resolve("output-prism-default");
        Path configFile = tempDir.resolve("prism-default-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Prism Default Docs")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism/prism.js")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism-overrides.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism/components/prism-interlis.js")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism/plugins/toolbar/prism-toolbar.min.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism/plugins/toolbar/prism-toolbar.min.js")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/icons/bootstrap-copy.svg")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/icons/bootstrap-check.svg")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/JetBrainsMono/JetBrainsMono-Regular.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/JetBrainsMono/JetBrainsMono-Bold.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/JetBrainsMono/JetBrainsMono-Italic.woff2")));

        String jetbrainsMonoCss = Files.readString(outputRoot.resolve("site-assets/jetbrainsmono.css"));
        assertTrue(jetbrainsMonoCss.contains("font-family: \"JetBrains Mono\";"));
        assertTrue(jetbrainsMonoCss.contains("font-weight: 400;"));
        assertTrue(jetbrainsMonoCss.contains("font-style: normal;"));
        assertTrue(jetbrainsMonoCss.contains("font-weight: 700;"));
        assertTrue(jetbrainsMonoCss.contains("font-style: italic;"));
        assertTrue(jetbrainsMonoCss.contains("./fonts/JetBrainsMono/JetBrainsMono-Regular.woff2"));
        assertTrue(jetbrainsMonoCss.contains("./fonts/JetBrainsMono/JetBrainsMono-Bold.woff2"));
        assertTrue(jetbrainsMonoCss.contains("./fonts/JetBrainsMono/JetBrainsMono-Italic.woff2"));

        String overridesCss = Files.readString(outputRoot.resolve("site-assets/prism-overrides.css"));
        assertTrue(overridesCss.contains("background: rgb(242, 242, 242)"));
        assertTrue(overridesCss.contains("font-size: 0.9rem"));
        assertTrue(overridesCss.contains("code[class*=\"language-\"]"));
        assertTrue(overridesCss.contains(".doc-content pre > code"));
        assertTrue(overridesCss.contains(".doc-content :not(pre) > code"));
        assertTrue(overridesCss.contains("font-family: \"JetBrains Mono\", \"SFMono-Regular\", Menlo, Consolas, \"Liberation Mono\", monospace !important;"));
        assertTrue(overridesCss.contains(".token.operator"));
        assertTrue(overridesCss.contains("background: transparent"));
        assertTrue(overridesCss.contains("padding-top: 0.25rem"));
        assertTrue(overridesCss.contains("padding-right: 0.25rem"));
        assertTrue(overridesCss.contains("/site-assets/icons/bootstrap-copy.svg"));
        assertTrue(overridesCss.contains("/site-assets/icons/bootstrap-check.svg"));

        String stylesCss = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(stylesCss.contains(".doc-content pre {\n    background: rgb(242, 242, 242);"));
        assertTrue(stylesCss.contains("padding: 1em;"));
        assertTrue(stylesCss.contains("margin: .5em 0;"));
        assertTrue(stylesCss.contains("font-size: 0.9rem;"));
        assertTrue(stylesCss.contains("font-family: \"JetBrains Mono\", \"SFMono-Regular\", Menlo, Consolas, \"Liberation Mono\", monospace;"));

        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(indexPage.contains("/site-assets/prism/prism.js"));
        assertTrue(indexPage.contains("/site-assets/prism-overrides.css"));
        assertTrue(indexPage.contains("/site-assets/prism/components/prism-interlis.js"));
        assertTrue(indexPage.contains("/site-assets/prism/plugins/toolbar/prism-toolbar.min.css"));
        assertTrue(indexPage.contains("/site-assets/prism/plugins/toolbar/prism-toolbar.min.js"));
        assertTrue(indexPage.contains("/site-assets/prism/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js"));
    }

    @Test
    void disablesPrismAssetsWhenConfiguredOff() throws Exception {
        Path repoDir = tempDir.resolve("prism-off-repo");
        Path workRoot = tempDir.resolve("work-prism-off");
        Path outputRoot = tempDir.resolve("output-prism-off");
        Path configFile = tempDir.resolve("prism-off-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Prism Off Docs")
            .withOutputDir(outputRoot)
            .withSyntaxHighlightingMode("off")
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertFalse(Files.exists(outputRoot.resolve("site-assets/prism/prism.js")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/prism-overrides.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/icons/bootstrap-copy.svg")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/icons/bootstrap-check.svg")));
        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertFalse(indexPage.contains("/site-assets/prism/prism.js"));
        assertFalse(indexPage.contains("/site-assets/prism-overrides.css"));
        assertFalse(indexPage.contains("/site-assets/prism/plugins/toolbar/prism-toolbar.min.css"));
        assertFalse(indexPage.contains("/site-assets/prism/plugins/toolbar/prism-toolbar.min.js"));
        assertFalse(indexPage.contains("/site-assets/prism/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js"));
    }

    @Test
    void copiesAndLoadsCustomPrismComponents() throws Exception {
        Path repoDir = tempDir.resolve("prism-custom-repo");
        Path workRoot = tempDir.resolve("work-prism-custom");
        Path outputRoot = tempDir.resolve("output-prism-custom");
        Path configFile = tempDir.resolve("prism-custom-biblios.yml");
        Path highlightingDir = tempDir.resolve("highlighting");
        Path customLanguage = highlightingDir.resolve("prism-customdsl.js");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);
        Files.createDirectories(highlightingDir);
        Files.writeString(customLanguage, "Prism.languages.customdsl={keyword:/foo/};");

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Prism Custom Docs")
            .withOutputDir(outputRoot)
            .withPrismCustomComponents(List.of("./highlighting/prism-customdsl.js"))
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("site-assets/prism/custom/prism-customdsl.js")));
        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(indexPage.contains("/site-assets/prism/custom/prism-customdsl.js"));
    }

    @Test
    void copiesReferencedRelativeImagesInSplitMode() throws Exception {
        Path repoDir = tempDir.resolve("split-images-repo");
        Path workRoot = tempDir.resolve("work-split-images");
        Path outputRoot = tempDir.resolve("output-split-images");
        Path configFile = tempDir.resolve("split-images-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSplitDocsReferencingImages();

        new BibliosConfigBuilder()
            .withSiteTitle("Split Image Integration")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/images/foo.png")));
        String html = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(html.contains("<img src=\"images/foo.png\""));
    }

    @Test
    void copiesReferencedRelativeImagesInSinglePageMode() throws Exception {
        Path repoDir = tempDir.resolve("single-page-images-repo");
        Path workRoot = tempDir.resolve("work-single-images");
        Path outputRoot = tempDir.resolve("output-single-images");
        Path configFile = tempDir.resolve("single-page-images-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSinglePageDocsWithImages();

        new BibliosConfigBuilder()
            .withSiteTitle("Single Page Images Integration")
            .withOutputDir(outputRoot)
            .withSinglePageSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "master.adoc", "main")
            .writeTo(configFile);

        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/images/single.png")));
        String html = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(html.contains("<img src=\"images/single.png\""));
    }

    @Test
    void skipsExternalImagesAndContinuesWhenLocalImageMissing() throws Exception {
        Path repoDir = tempDir.resolve("split-external-missing-images-repo");
        Path workRoot = tempDir.resolve("work-external-missing-images");
        Path outputRoot = tempDir.resolve("output-external-missing-images");
        Path configFile = tempDir.resolve("external-missing-images-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSplitDocsExternalAndMissingImages();

        new BibliosConfigBuilder()
            .withSiteTitle("External Missing Images Integration")
            .withOutputDir(outputRoot)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        String html = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(html.contains("<img src=\"images/missing.png\""));
        assertTrue(html.contains("<img src=\"https://example.org/logo.png\""));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/images/missing.png")));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/https:/example.org/logo.png")));
    }

    @Test
    void singlePageModeBuildPipeline() throws Exception {
        Path repoDir = tempDir.resolve("single-page-repo");
        Path workRoot = tempDir.resolve("work-single");
        Path outputRoot = tempDir.resolve("output-single");
        Path configFile = tempDir.resolve("single-page-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSinglePageDocs();

        new BibliosConfigBuilder()
            .withSiteTitle("Single Page Integration")
            .withOutputDir(outputRoot)
            .withSidebarTocDepth(3)
            .withContentToc("off")
            .withSinglePageSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "master.adoc", "main")
            .writeTo(configFile);

        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            DocComponent component = catalog.findById("mydocs");
            assertNotNull(component);

            ComponentVersion version = component.getVersion("main");
            assertNotNull(version);
            assertEquals(1, version.pages().size());
            assertTrue(version.renderMode().isSinglePage());
            assertFalse(version.navigation().items().isEmpty());
        }

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/index.html")));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/guide/index.html")));

        String html = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(html.contains("data-single-page-mode=\"true\""));
        assertTrue(html.contains("data-chapter-breadcrumb=\"enabled\""));
        assertTrue(html.contains("id=\"chapter-breadcrumb-root\""));
        assertTrue(html.contains("id=\"chapter-breadcrumb-trail\""));
        assertTrue(html.contains("id=\"chapter-breadcrumb-current\""));
        assertTrue(html.contains("href=\"/mydocs/main/#"));
        assertTrue(html.contains("class=\"anchor\""));
        assertTrue(html.matches("(?s).*class=\"anchor\"\\s+href=\"#.+?\".*"));
        assertFalse(html.matches("(?s).*\\b1\\.?\\s+Einleitung\\b.*"));
        assertFalse(html.contains("id=\"toc\""));
    }

    @Test
    void singlePageModeShowsAppendixRoleButSkipsOtherUnnumberedChaptersInSidebar() throws Exception {
        Path repoDir = tempDir.resolve("single-page-unnumbered-repo");
        Path workRoot = tempDir.resolve("work-single-unnumbered");
        Path outputRoot = tempDir.resolve("output-single-unnumbered");
        Path configFile = tempDir.resolve("single-page-unnumbered-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSinglePageDocsIncludingUnnumberedChapter();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Single Page Unnumbered Integration")
            .withOutputDir(outputRoot)
            .withSidebarTocDepth(3)
            .withContentToc("off")
            .withSinglePageSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "master.adoc", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        String html = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(html.contains("data-chapter-title=\"Einleitung\""));
        assertTrue(html.contains("data-chapter-title=\"Grundprinzipien\""));
        assertFalse(html.contains("data-chapter-title=\"Erweiterungen von INTERLIS 2.4 gegenüber INTERLIS 2.3\""));
        assertTrue(html.contains("data-chapter-title=\"Anhang A - foo bar\""));
        assertTrue(html.contains("id=\"chapter-breadcrumb-current\">Einleitung<"));
        assertTrue(html.contains("href=\"/mydocs/main/#_anhang_a_foo_bar\""));
        assertTrue(html.contains("Erweiterungen von INTERLIS 2.4 gegenüber INTERLIS 2.3"));
    }

    @Test
    void singlePageSidebarTocNumbersOnPrefixesTitles() throws Exception {
        Path repoDir = tempDir.resolve("single-page-numbered-repo");
        Path workRoot = tempDir.resolve("work-single-numbered");
        Path outputRoot = tempDir.resolve("output-single-numbered");
        Path configFile = tempDir.resolve("single-page-numbered-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSinglePageNumberedDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Single Page Numbered Integration")
            .withOutputDir(outputRoot)
            .withSidebarTocDepth(3)
            .withContentToc("off")
            .withSinglePageSourceGitRepoWithTocNumbers(repoDir, "mydocs", "My Documentation",
                "docs", "main", "master.adoc", "on", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        String html = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(html.matches("(?s).*\\b1\\.\\s+Einleitung\\b.*"));
        assertTrue(html.matches("(?s).*\\b1\\.1\\.\\s+Status\\b.*"));
        assertTrue(html.contains("data-chapter-title=\"Einleitung\""));
    }

    private String extractHead(String html) {
        int headStart = html.indexOf("<head>");
        int headEnd = html.indexOf("</head>");
        if (headStart < 0 || headEnd < 0 || headEnd <= headStart) {
            return "";
        }
        return html.substring(headStart, headEnd);
    }
}
