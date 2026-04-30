package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        assertTrue(homePage.contains("id=\"search-scope-active\""));
        assertTrue(homePage.contains("id=\"search-scope-input\""));
        assertTrue(homePage.contains("name=\"scope\""));
        assertTrue(homePage.contains("name=\"component\""));
        assertTrue(homePage.contains("name=\"version\""));
        assertTrue(homePage.contains("class=\"component-card-default-link\""));
        assertTrue(homePage.contains("href=\"/mydocs/main/\""));
        assertTrue(homePage.contains("href=\"/mydocs/main/\" class=\"version-tag\""));
        String homeHead = extractHead(homePage);
        assertFalse(homeHead.contains("<div class=\"home\">"));

        String searchPage = Files.readString(outputRoot.resolve("search/index.html"));
        assertTrue(searchPage.contains("id=\"search-results\""));
        assertTrue(searchPage.contains("data-search-language-mode=\"multilingual_safe\""));
        assertTrue(searchPage.contains("id=\"search-scope-active\""));
        assertTrue(searchPage.contains("id=\"search-scope-input\""));

        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(indexPage.contains("Welcome"));
        assertFalse(indexPage.contains("class=\"nav-toggle\""));
        assertTrue(indexPage.contains("data-current-component-id=\"mydocs\""));
        assertTrue(indexPage.contains("data-current-version=\"main\""));

        String styles = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(styles.contains(".content {"));
        assertTrue(styles.contains(".doc-content .imageblock > .title,\n.doc-content .listingblock > .title {"));
        assertTrue(styles.contains(".doc-content .imageblock > .content {\n    padding: 0;"));
        assertTrue(styles.contains(".doc-content .imageblock img {\n    display: block;\n    max-width: 100%;\n    height: auto;"));
        assertTrue(styles.contains(".doc-content .listingblock > .content {\n    padding: 0;"));

        String searchIndex = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchIndex.contains("mydocs"));
        assertTrue(searchIndex.contains("\"kind\":\"chapter\""));
        assertTrue(searchIndex.contains("\"pageTitle\":\"Welcome\""));
        assertTrue(searchIndex.contains("\"sectionPath\":\""));
        assertTrue(searchIndex.contains("Getting Started"));
        assertTrue(searchIndex.contains("\"sectionLevel\":1"));
        assertTrue(searchIndex.contains("\"route\":\"/mydocs/main/#"));
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

            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(parsedConfig, catalog, outputRoot)) {
                generator.generate();
            }
        }

        String homePage = Files.readString(outputRoot.resolve("index.html"));
        assertTrue(homePage.contains("class=\"component-card-default-link\""));
        assertTrue(homePage.contains("href=\"/mydocs/main/\""));
        assertTrue(homePage.contains("href=\"/mydocs/main/\" class=\"version-tag\""));
        assertTrue(homePage.contains("href=\"/mydocs/v1.x/\" class=\"version-tag\""));
    }

    @Test
    void fullBuildPipelineGeneratesPdfArtifacts() throws Exception {
        Path repoDir = tempDir.resolve("pdf-repo");
        Path workRoot = tempDir.resolve("pdf-work");
        Path outputRoot = tempDir.resolve("pdf-output");
        Path configFile = tempDir.resolve("pdf-biblios.yml");
        Path themeDir = tempDir.resolve("themes");
        Path themeFile = themeDir.resolve("pdf-theme.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);
        Files.createDirectories(themeDir);
        Files.writeString(themeFile, "extends: default\n");

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("PDF Docs")
            .withOutputDir(outputRoot)
            .withPdfEnabled(true)
            .withPdfAttributes(Map.of("pdf-theme", "./themes/pdf-theme.yml"))
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        Path pdfFile = outputRoot.resolve("mydocs/main/mydocs-main.pdf");
        assertTrue(Files.exists(pdfFile));
        byte[] bytes = Files.readAllBytes(pdfFile);
        assertTrue(bytes.length > 5);
        assertEquals("%PDF-", new String(bytes, 0, 5));
    }

    @Test
    void generateHonorsPdfToggleAndVersionFilter() throws Exception {
        Path repoDir = tempDir.resolve("pdf-filter-repo");
        Path workRoot = tempDir.resolve("pdf-filter-work");
        Path outputRoot = tempDir.resolve("pdf-filter-output");
        Path configFile = tempDir.resolve("pdf-filter-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("PDF Filter Docs")
            .withOutputDir(outputRoot)
            .withPdfEnabled(true)
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

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate(false, Set.of());
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/index.html")));
        assertTrue(Files.exists(outputRoot.resolve("mydocs/v1.x/index.html")));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/mydocs-main.pdf")));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/v1.x/mydocs-v1.x.pdf")));

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate(true, Set.of("v1.x"));
            }
        }

        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/mydocs-main.pdf")));
        assertTrue(Files.exists(outputRoot.resolve("mydocs/v1.x/mydocs-v1.x.pdf")));
    }

    @Test
    void localWorkingTreeOverrideIncludesUncommittedChangesWhenEnabled() throws Exception {
        Path repoDir = tempDir.resolve("local-working-tree-repo");
        Path workRoot = tempDir.resolve("work-local-working-tree");
        Path configFile = tempDir.resolve("local-working-tree-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Local Working Tree Docs")
            .withOutputDir(tempDir.resolve("output-local-working-tree"))
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        // Warm cache with committed content.
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, true, false, configFile)) {
            SiteCatalog catalog = builder.build();
            ComponentVersion main = catalog.findById("mydocs").getVersion("main");
            assertNotNull(main);
            assertTrue(main.findPageBySourcePath("index.adoc").html().contains("This is the main documentation index."));
        }

        // Local, uncommitted edit in the repository working tree.
        Files.writeString(repoDir.resolve("docs/index.adoc"), """
            = Welcome
            :doctype: book

            LOCAL UNCOMMITTED CHANGE
            """);

        // Without local working tree mode: cached content remains visible.
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, false, false, configFile)) {
            SiteCatalog catalog = builder.build();
            ComponentVersion main = catalog.findById("mydocs").getVersion("main");
            assertNotNull(main);
            assertFalse(main.findPageBySourcePath("index.adoc").html().contains("LOCAL UNCOMMITTED CHANGE"));
        }

        // With local working tree mode: uncommitted change is visible.
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, false, true, configFile)) {
            SiteCatalog catalog = builder.build();
            ComponentVersion main = catalog.findById("mydocs").getVersion("main");
            assertNotNull(main);
            assertTrue(main.findPageBySourcePath("index.adoc").html().contains("LOCAL UNCOMMITTED CHANGE"));
        }
    }

    @Test
    void localWorkingTreeOverrideUsesCurrentBranchOnlyAndKeepsOtherBranchesCached() throws Exception {
        Path repoDir = tempDir.resolve("local-working-tree-multi-repo");
        Path workRoot = tempDir.resolve("work-local-working-tree-multi");
        Path configFile = tempDir.resolve("local-working-tree-multi-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);

        new TestRepoBuilder(repoDir)
            .withBasicDocs()
            .withSecondBranch("v1.x");

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Local Working Tree Multi")
            .withOutputDir(tempDir.resolve("output-local-working-tree-multi"))
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

        // Warm cache for both branches.
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, true, false, configFile)) {
            builder.build();
        }

        // Change only main branch locally, without commit.
        Files.writeString(repoDir.resolve("docs/guide.adoc"), """
            = User Guide
            :doctype: book

            LOCAL MAIN GUIDE
            """);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, false, true, configFile)) {
            SiteCatalog catalog = builder.build();
            DocComponent component = catalog.findById("mydocs");
            assertNotNull(component);

            ComponentVersion main = component.getVersion("main");
            ComponentVersion v1x = component.getVersion("v1.x");
            assertNotNull(main);
            assertNotNull(v1x);
            assertTrue(main.findPageBySourcePath("guide.adoc").html().contains("LOCAL MAIN GUIDE"));
            assertTrue(v1x.findPageBySourcePath("guide.adoc").html().contains("This is the v1.x user guide."));
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
        assertTrue(Files.exists(outputRoot.resolve("site-assets/literata.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/atkinson-hyperlegible-next.css")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/ibm-plex-sans.css")));

        assertFalse(Files.exists(outputRoot.resolve("site-assets/frutiger-light.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/frutiger-serif.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/zurich-light.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/zurich.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/zurich-condensed.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/notosans.css")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/inter.css")));
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
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/Literata/Literata-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/Literata/Literata-Italic-Variable.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/AtkinsonHyperlegibleNext/AtkinsonHyperlegibleNext[wght].woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/AtkinsonHyperlegibleNext/AtkinsonHyperlegibleNext-Italic[wght].woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/IBMPlexSansVariable/IBM Plex Sans Var-Roman.woff2")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/fonts/IBMPlexSansVariable/IBM Plex Sans Var-Italic.woff2")));

        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Zurich")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Zurich_Condensed")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/NotoSans")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/fonts/Inter")));
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

        String literataCss = Files.readString(outputRoot.resolve("site-assets/literata.css"));
        assertTrue(literataCss.contains("font-family: \"Literata\";"));
        assertTrue(literataCss.contains("font-weight: 200 900;"));
        assertTrue(literataCss.contains("font-style: normal;"));
        assertTrue(literataCss.contains("font-style: italic;"));
        assertTrue(literataCss.contains("./fonts/Literata/Literata-Variable.woff2"));
        assertTrue(literataCss.contains("./fonts/Literata/Literata-Italic-Variable.woff2"));

        String atkinsonCss = Files.readString(outputRoot.resolve("site-assets/atkinson-hyperlegible-next.css"));
        assertTrue(atkinsonCss.contains("font-family: \"Atkinson Hyperlegible Next\";"));
        assertTrue(atkinsonCss.contains("font-weight: 200 800;"));
        assertTrue(atkinsonCss.contains("font-style: normal;"));
        assertTrue(atkinsonCss.contains("font-style: italic;"));
        assertTrue(atkinsonCss.contains("./fonts/AtkinsonHyperlegibleNext/AtkinsonHyperlegibleNext[wght].woff2"));
        assertTrue(atkinsonCss.contains("./fonts/AtkinsonHyperlegibleNext/AtkinsonHyperlegibleNext-Italic[wght].woff2"));

        String ibmPlexSansCss = Files.readString(outputRoot.resolve("site-assets/ibm-plex-sans.css"));
        assertTrue(ibmPlexSansCss.contains("font-family: \"IBM Plex Sans\";"));
        assertTrue(ibmPlexSansCss.contains("font-weight: 100 700;"));
        assertTrue(ibmPlexSansCss.contains("font-style: normal;"));
        assertTrue(ibmPlexSansCss.contains("font-style: italic;"));
        assertTrue(ibmPlexSansCss.contains("./fonts/IBMPlexSansVariable/IBM Plex Sans Var-Roman.woff2"));
        assertTrue(ibmPlexSansCss.contains("./fonts/IBMPlexSansVariable/IBM Plex Sans Var-Italic.woff2"));

        String stylesCss = Files.readString(outputRoot.resolve("site-assets/styles.css"));
        assertTrue(stylesCss.contains("--font-body: \"Literata\""));
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
        assertTrue(indexPage.contains("/site-assets/literata.css"));
        assertTrue(indexPage.contains("/site-assets/atkinson-hyperlegible-next.css"));
        assertTrue(indexPage.contains("/site-assets/ibm-plex-sans.css"));
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
        assertTrue(searchJs.contains("buildResultHref"));
        assertTrue(searchJs.contains("initDocumentHighlights"));
        assertTrue(searchJs.contains("parseSearchState"));
        assertTrue(searchJs.contains("syncSearchFormState"));
        assertTrue(searchJs.contains("filterDocumentsByScope"));
        assertTrue(searchJs.contains("sectionPath"));
        assertTrue(searchJs.contains("sectionLevel"));
        assertTrue(searchJs.contains("dedupeResultsByRoute"));

        String searchIndex = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchIndex.contains("\"kind\":\"chapter\""));
        assertTrue(searchIndex.contains("\"sectionPath\":\""));
        assertTrue(searchIndex.contains("Status"));
        assertTrue(searchIndex.contains("\"sectionLevel\":2"));
        assertTrue(searchIndex.contains("#_status"));
        assertTrue(searchIndex.contains("\"route\":\"/mydocs/main/#"));
    }

    @Test
    void splitModeSearchFallsBackToPageEntryWhenNoSect1ChaptersExist() throws Exception {
        Path repoDir = tempDir.resolve("split-fallback-repo");
        Path workRoot = tempDir.resolve("split-fallback-work");
        Path outputRoot = tempDir.resolve("split-fallback-output");
        Path configFile = tempDir.resolve("split-fallback-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();
        Files.writeString(repoDir.resolve("docs/index.adoc"), """
            = Welcome
            :doctype: book

            Landing page without section headings.
            """);
        try (Git git = Git.open(repoDir.toFile())) {
            git.add().addFilepattern("docs/index.adoc").call();
            git.commit().setMessage("Remove sect1 chapter from index page").call();
        }

        new BibliosConfigBuilder()
            .withSiteTitle("Split Fallback Integration")
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

        String searchIndex = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchIndex.contains("\"kind\":\"page\""));
        assertTrue(searchIndex.contains("\"pageTitle\":\"Welcome\""));
        assertTrue(searchIndex.contains("\"sectionPath\":\"Welcome\""));
        assertTrue(searchIndex.contains("\"sectionLevel\":0"));
        assertTrue(searchIndex.contains("\"route\":\"/mydocs/main/\""));
    }

    @Test
    void searchIndexLimitsSectionDepthToSect4() throws Exception {
        Path repoDir = tempDir.resolve("single-page-depth-repo");
        Path workRoot = tempDir.resolve("work-single-depth");
        Path outputRoot = tempDir.resolve("output-single-depth");
        Path configFile = tempDir.resolve("single-page-depth-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withSinglePageDocsUpToSect5();

        new BibliosConfigBuilder()
            .withSiteTitle("Single Page Depth Integration")
            .withOutputDir(outputRoot)
            .withSidebarTocDepth(6)
            .withContentToc("off")
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

        String searchIndex = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchIndex.contains("\"sectionPath\":\""));
        assertTrue(searchIndex.contains("Level Four"));
        assertTrue(searchIndex.contains("\"sectionLevel\":4"));
        assertTrue(searchIndex.contains("#_level_four"));
        assertFalse(searchIndex.contains("\"sectionLevel\":5"));
        assertFalse(searchIndex.contains("#_level_five"));
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

    @Test
    void incrementalSourceRegenerationUpdatesOnlyChangedComponent() throws Exception {
        Path repoADir = tempDir.resolve("incremental-repo-a");
        Path repoBDir = tempDir.resolve("incremental-repo-b");
        Path workRoot = tempDir.resolve("incremental-work");
        Path outputRoot = tempDir.resolve("incremental-output");
        Path configFile = tempDir.resolve("incremental-biblios.yml");

        Files.createDirectories(repoADir);
        Files.createDirectories(repoBDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoADir).withBasicDocs();
        new TestRepoBuilder(repoBDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Incremental Integration")
            .withOutputDir(outputRoot)
            .withClean(false)
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: docs-a
                  display_name: Docs A
                  url: file://%s
                  branches:
                    - name: main
                  start_path: docs
                  default_version: main
                """.formatted(repoADir)))
            .withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: docs-b
                  display_name: Docs B
                  url: file://%s
                  branches:
                    - name: main
                  start_path: docs
                  default_version: main
                """.formatted(repoBDir)))
            .writeTo(configFile);

        SiteCatalog catalog;
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, true, true, configFile)) {
            catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate();
            }
        }

        Path docsBGuide = outputRoot.resolve("docs-b/main/guide/index.html");
        byte[] docsBGuideBefore = Files.readAllBytes(docsBGuide);
        FileTime docsBGuideMtimeBefore = Files.getLastModifiedTime(docsBGuide);

        Files.writeString(repoADir.resolve("docs/guide.adoc"), """
            = User Guide
            :doctype: book

            This guide was changed incrementally.
            """);

        SourceConfig sourceA = config.content().sources().stream()
            .filter(source -> source.id().equals("docs-a"))
            .findFirst()
            .orElseThrow();

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, false, true, configFile)) {
            DocComponent updatedComponent = builder.buildComponent(sourceA);
            SiteCatalog updatedCatalog = catalog.withReplacedComponent(updatedComponent);
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, updatedCatalog, outputRoot)) {
                generator.regenerateComponent(updatedComponent);
                generator.regenerateGlobalArtifacts();
            }
            catalog = updatedCatalog;
        }

        String docsAGuide = Files.readString(outputRoot.resolve("docs-a/main/guide/index.html"));
        assertTrue(docsAGuide.contains("This guide was changed incrementally."));
        assertArrayEquals(docsBGuideBefore, Files.readAllBytes(docsBGuide));
        assertEquals(docsBGuideMtimeBefore, Files.getLastModifiedTime(docsBGuide));

        Files.delete(repoADir.resolve("docs/guide.adoc"));
        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot, false, true, configFile)) {
            DocComponent updatedComponent = builder.buildComponent(sourceA);
            SiteCatalog updatedCatalog = catalog.withReplacedComponent(updatedComponent);
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, updatedCatalog, outputRoot)) {
                generator.regenerateComponent(updatedComponent);
                generator.regenerateGlobalArtifacts();
            }
            catalog = updatedCatalog;
        }

        assertFalse(Files.exists(outputRoot.resolve("docs-a/main/guide/index.html")));
    }

    @Test
    void fullCleanConfigRebuildRemovesStaleOutputs() throws Exception {
        Path repoADir = tempDir.resolve("config-rebuild-repo-a");
        Path repoBDir = tempDir.resolve("config-rebuild-repo-b");
        Path workRoot = tempDir.resolve("config-rebuild-work");
        Path outputRoot = tempDir.resolve("config-rebuild-output");
        Path configFile = tempDir.resolve("config-rebuild-biblios.yml");

        Files.createDirectories(repoADir);
        Files.createDirectories(repoBDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoADir).withBasicDocs();
        new TestRepoBuilder(repoBDir).withBasicDocs();

        BibliosConfig initialConfig = new BibliosConfigBuilder()
            .withSiteTitle("Config Rebuild Integration")
            .withOutputDir(outputRoot)
            .withClean(false)
            .withSingleSourceGitRepo(repoADir, "docs-a", "Docs A", "docs", "main", "main")
            .withSingleSourceGitRepo(repoBDir, "docs-b", "Docs B", "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(initialConfig, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(initialConfig, catalog, outputRoot)) {
                generator.generate();
            }
        }
        assertTrue(Files.exists(outputRoot.resolve("docs-b/main/index.html")));

        BibliosConfig updatedConfig = new BibliosConfigBuilder()
            .withSiteTitle("Config Rebuild Integration")
            .withOutputDir(outputRoot)
            .withClean(false)
            .withSingleSourceGitRepo(repoADir, "docs-a", "Docs A", "docs", "main", "main")
            .writeTo(configFile);

        deleteRecursively(outputRoot);
        try (CatalogBuilder builder = new CatalogBuilder(updatedConfig, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(updatedConfig, catalog, outputRoot)) {
                generator.generate();
            }
        }

        assertFalse(Files.exists(outputRoot.resolve("docs-b")));
        assertTrue(Files.exists(outputRoot.resolve("docs-a/main/index.html")));
    }

    @Test
    void docxContainsRefSeqAndPageRefFieldsForInternalFigureReferences() throws Exception {
        Path repoDir = tempDir.resolve("docx-fields-repo");
        Path workRoot = tempDir.resolve("docx-fields-work");
        Path outputRoot = tempDir.resolve("docx-fields-output");
        Path configFile = tempDir.resolve("docx-fields-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();
        prepareDocxReferenceFixture(repoDir);

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("DOCX Field Docs")
            .withOutputDir(outputRoot)
            .withDocxEnabled(true)
            .withDocxFeatures(false, true, false)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation", "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate(false, Set.of(), true, Set.of("main"));
            }
        }

        Path docxFile = outputRoot.resolve("mydocs/main/mydocs-main.docx");
        assertTrue(Files.exists(docxFile));
        String xml = readDocxEntry(docxFile, "word/document.xml");

        assertTrue(xml.contains("SEQ Figure"));
        assertTrue(xml.contains(" REF "));
        assertTrue(xml.contains(" PAGEREF "));
        assertTrue(xml.contains("w:bookmarkStart"));
        assertTrue(xml.contains("fig_overview"));
    }

    @Test
    void docxFailsWhenRenderedHtmlContainsMissingInternalAnchor() throws Exception {
        Path repoDir = tempDir.resolve("docx-missing-ref-repo");
        Path workRoot = tempDir.resolve("docx-missing-ref-work");
        Path outputRoot = tempDir.resolve("docx-missing-ref-output");
        Path configFile = tempDir.resolve("docx-missing-ref-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();
        try (Git git = Git.open(repoDir.toFile())) {
            Files.writeString(repoDir.resolve("docs/guide.adoc"), """
                = User Guide
                :doctype: book

                Broken reference: link:#does_not_exist[Kapitel] auf Seite link:#does_not_exist[dort].
                """);
            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add broken internal link for DOCX").call();
        }

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("DOCX Missing Ref Docs")
            .withOutputDir(outputRoot)
            .withDocxEnabled(true)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation", "docs", "main", "main")
            .writeTo(configFile);

        IOException ex = assertThrows(IOException.class, () -> {
            try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
                SiteCatalog catalog = builder.build();
                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                    generator.generate(false, Set.of(), true, Set.of("main"));
                }
            }
        });

        assertTrue(ex.getMessage().contains("Unresolved"));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/mydocs-main.docx")));
    }

    @Test
    void docxFieldGenerationDoesNotRegressHtmlAndPdfOutputs() throws Exception {
        Path repoDir = tempDir.resolve("docx-no-regression-repo");
        Path workRoot = tempDir.resolve("docx-no-regression-work");
        Path outputRoot = tempDir.resolve("docx-no-regression-output");
        Path configFile = tempDir.resolve("docx-no-regression-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();
        prepareDocxReferenceFixture(repoDir);

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("DOCX Regression Docs")
            .withOutputDir(outputRoot)
            .withPdfEnabled(true)
            .withDocxEnabled(true)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation", "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate(true, Set.of("main"), true, Set.of("main"));
            }
        }

        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/index.html")));
        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/mydocs-main.pdf")));
        assertTrue(Files.exists(outputRoot.resolve("mydocs/main/mydocs-main.docx")));
    }

    private void prepareDocxReferenceFixture(Path repoDir) throws Exception {
        byte[] onePixelPng = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9sN3cJQAAAAASUVORK5CYII="
        );
        Files.createDirectories(repoDir.resolve("docs/images"));
        Files.write(repoDir.resolve("docs/images/overview.png"), onePixelPng);

        try (Git git = Git.open(repoDir.toFile())) {
            Files.writeString(repoDir.resolve("docs/guide.adoc"), """
                = User Guide
                :doctype: book

                Siehe link:#fig-overview[Abbildung].
                Siehe auf Seite link:#fig-overview[dieser Abbildung].

                [#fig-overview]
                .System overview
                image::images/overview.png[]
                """);

            Files.writeString(repoDir.resolve("docs/nav.yml"), """
                items:
                  - title: Welcome
                    page: index.adoc
                  - title: User Guide
                    page: guide.adoc
                """);
            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add DOCX reference fixture").call();
        }
    }

    private String readDocxEntry(Path docxFile, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(docxFile.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            assertNotNull(entry, "Missing DOCX entry: " + entryName);
            try (InputStream in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
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
}
