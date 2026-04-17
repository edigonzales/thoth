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
        assertFalse(indexPage.contains("class=\"nav-toggle\""));

        String styles = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(styles.contains(".content {"));
        assertTrue(styles.contains(".doc-content .imageblock > .content {\n    padding: 0;"));
        assertTrue(styles.contains(".doc-content .imageblock img {\n    display: block;\n    max-width: 100%;\n    height: auto;"));
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

        assertTrue(Files.exists(outputRoot.resolve("site-assets/jetbrainsmono.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/merriweather.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/source-sans-3.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/notoserif.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/open-sans.css")));

        assertFalse(Files.exists(outputRoot.resolve("site-assets/frutiger-light.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/frutiger-serif.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/zurich-light.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/zurich.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/zurich-condensed.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/notosans.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/inter.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/literata.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/inter-tight.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/geist.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/familjen-grotesk.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/source-serif-4.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/spectral.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/texgyreheroes.css")));

        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/JetBrainsMono/JetBrainsMono-Regular.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/JetBrainsMono/JetBrainsMono-Bold.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/JetBrainsMono/JetBrainsMono-Italic.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/Merriweather/Merriweather-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/Merriweather/Merriweather-Italic-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/SourceSans3/SourceSans3-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/SourceSans3/SourceSans3-Italic-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/NotoSerif/NotoSerif-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/NotoSerif/NotoSerif-Italic-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/OpenSans/OpenSans-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/OpenSans/OpenSans-Italic-Variable.woff2")));

        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Zurich")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Zurich_Condensed")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/NotoSans")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Inter")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Literata")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/InterTight")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Geist")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/FamiljenGrotesk")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/SourceSerif4")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Spectral")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/texgyreheroes")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/FrutigerSerif")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/PublicSans")));

        String jetbrainsMonoCss = Files.readString(outputRoot.resolve("site-assets/jetbrainsmono.css"));
        assertTrue(jetbrainsMonoCss.contains("font-family: \"JetBrains Mono\";"));
        assertTrue(jetbrainsMonoCss.contains("./fonts/JetBrainsMono/JetBrainsMono-Regular.woff2"));

        String merriweatherCss = Files.readString(outputRoot.resolve("site-assets/merriweather.css"));
        assertTrue(merriweatherCss.contains("font-family: \"Merriweather\";"));
        assertTrue(merriweatherCss.contains("./fonts/Merriweather/Merriweather-Variable.woff2"));

        String sourceSans3Css = Files.readString(outputRoot.resolve("site-assets/source-sans-3.css"));
        assertTrue(sourceSans3Css.contains("font-family: \"Source Sans 3\";"));
        assertTrue(sourceSans3Css.contains("./fonts/SourceSans3/SourceSans3-Variable.woff2"));

        String notoSerifCss = Files.readString(outputRoot.resolve("site-assets/notoserif.css"));
        assertTrue(notoSerifCss.contains("font-family: \"Noto Serif\";"));
        assertTrue(notoSerifCss.contains("./fonts/NotoSerif/NotoSerif-Variable.woff2"));
        assertTrue(notoSerifCss.contains("./fonts/NotoSerif/NotoSerif-Italic-Variable.woff2"));

        String openSansCss = Files.readString(outputRoot.resolve("site-assets/open-sans.css"));
        assertTrue(openSansCss.contains("font-family: \"Open Sans\";"));
        assertTrue(openSansCss.contains("font-weight: 300 800;"));
        assertTrue(openSansCss.contains("font-style: normal;"));
        assertTrue(openSansCss.contains("font-style: italic;"));
        assertTrue(openSansCss.contains("./fonts/OpenSans/OpenSans-Variable.woff2"));
        assertTrue(openSansCss.contains("./fonts/OpenSans/OpenSans-Italic-Variable.woff2"));

        String stylesCss = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(stylesCss.contains("--font-body: \"Noto Serif\""));
        assertTrue(stylesCss.contains("--font-sidebar-toc: \"Open Sans\""));
        assertTrue(stylesCss.contains("--font-chapter-headings: \"Open Sans\""));
        assertTrue(stylesCss.contains("--font-navbar: \"Open Sans\""));
        assertTrue(stylesCss.contains("--font-index-page: \"Open Sans\""));
        assertTrue(stylesCss.contains("--font-breadcrumb: \"Open Sans\""));
        assertTrue(stylesCss.contains("--font-conum: \"Open Sans\""));
        assertTrue(stylesCss.contains("font-family: \"JetBrains Mono\", \"SFMono-Regular\", Menlo, Consolas, \"Liberation Mono\", monospace;"));
        assertFalse(stylesCss.contains("TeX Gyre Heros"));

        String overridesCss = Files.readString(outputRoot.resolve("site-assets/prism-overrides.css"));
        assertTrue(overridesCss.contains("font-family: \"JetBrains Mono\", \"SFMono-Regular\", Menlo, Consolas, \"Liberation Mono\", monospace !important;"));

        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(indexPage.contains("/site-assets/jetbrainsmono.css"));
        assertTrue(indexPage.contains("/site-assets/merriweather.css"));
        assertTrue(indexPage.contains("/site-assets/source-sans-3.css"));
        assertTrue(indexPage.contains("/site-assets/notoserif.css"));
        assertTrue(indexPage.contains("/site-assets/open-sans.css"));
        assertFalse(indexPage.contains("/site-assets/zurich.css"));
        assertFalse(indexPage.contains("/site-assets/notosans.css"));
        assertFalse(indexPage.contains("/site-assets/texgyreheroes.css"));
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

        String stylesCss = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(stylesCss.contains(".nav-toggle"));
        assertTrue(stylesCss.contains(".nav-item.is-collapsible.is-collapsed > .nav-subtree"));

        String searchJs = Files.readString(outputRoot.resolve("site-assets/search.js"));
        assertTrue(searchJs.contains("setBranchExpanded"));
        assertTrue(searchJs.contains("bindCollapsibleToggles"));
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
        assertFalse(html.contains("aria-label=\"Toggle subsections for Anhang A - foo bar\""));
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
