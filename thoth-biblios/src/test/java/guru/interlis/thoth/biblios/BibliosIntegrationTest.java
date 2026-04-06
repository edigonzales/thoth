package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
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

    private Path repoDir;
    private Path workRoot;
    private Path outputRoot;
    private Path configFile;

    @BeforeEach
    void setUp() throws Exception {
        repoDir = tempDir.resolve("test-repo");
        workRoot = tempDir.resolve("work");
        outputRoot = tempDir.resolve("output");
        configFile = tempDir.resolve("biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        // Create a local Git repository with AsciiDoc content
        createTestRepo(repoDir);
        createTestConfig();
    }

    @AfterEach
    void tearDown() {
        // Cleanup
    }

    @Test
    void fullBuildPipeline() throws Exception {
        // 1. Parse config
        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);
        assertEquals("Integration Test Docs", config.site().title());
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
            assertEquals("Current", version.displayVersion());

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
        assertTrue(Files.exists(outputRoot.resolve("search-index.json")));
        assertTrue(Files.exists(outputRoot.resolve("site-assets/styles.css")));

        // 5. Verify content
        String homePage = Files.readString(outputRoot.resolve("index.html"));
        assertTrue(homePage.contains("Integration Test Docs"));
        assertTrue(homePage.contains("My Documentation"));

        String indexPage = Files.readString(outputRoot.resolve("mydocs/main/index.html"));
        assertTrue(indexPage.contains("Welcome"));

        String searchIndex = Files.readString(outputRoot.resolve("search-index.json"));
        assertTrue(searchIndex.contains("mydocs"));
        assertTrue(searchIndex.contains("Welcome"));
    }

    @Test
    void multiBranchBuild() throws Exception {
        // Add another branch to the test repo
        createSecondBranch(repoDir);

        // Update config to include second branch
        String yaml = """
            site:
              title: Multi-Version Docs
            output:
              dir: %s
              clean: true
            content:
              sources:
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
            """.formatted(outputRoot.toString(), repoDir.toString());
        Files.writeString(configFile, yaml);

        // Parse and build
        BibliosConfigParser parser = new BibliosConfigParser();
        BibliosConfig config = parser.parse(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
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
        }
    }

    private void createTestRepo(Path repoDir) throws Exception {
        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch("main").call()) {
            // Configure user
            StoredConfig config = git.getRepository().getConfig();
            config.setString("user", null, "name", "Test User");
            config.setString("user", null, "email", "test@example.org");
            config.save();

            // Create docs directory
            Path docsDir = repoDir.resolve("docs");
            Files.createDirectories(docsDir);

            // Create nav.yml
            String nav = """
                items:
                  - title: Welcome
                    page: index.adoc
                  - title: User Guide
                    page: guide.adoc
                """;
            Files.writeString(docsDir.resolve("nav.yml"), nav);

            // Create index.adoc
            String indexAdoc = """
                = Welcome
                :doctype: book

                This is the main documentation index.

                == Getting Started

                Welcome to our project. This documentation will help you get started.
                """;
            Files.writeString(docsDir.resolve("index.adoc"), indexAdoc);

            // Create guide.adoc
            String guideAdoc = """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                Follow these steps to install the software.

                == Configuration

                Configure the software to your needs.
                """;
            Files.writeString(docsDir.resolve("guide.adoc"), guideAdoc);

            // Commit
            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Initial commit").call();
        }
    }

    private void createSecondBranch(Path repoDir) throws Exception {
        try (Git git = Git.open(repoDir.toFile())) {
            // Create and checkout v1.x branch
            git.checkout().setCreateBranch(true).setName("v1.x").call();

            // Update content
            Path docsDir = repoDir.resolve("docs");
            String guideAdoc = """
                = User Guide (v1.x)
                :doctype: book

                This is the v1.x user guide.

                == Installation

                Installation instructions for version 1.x.
                """;
            Files.writeString(docsDir.resolve("guide.adoc"), guideAdoc);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Update guide for v1.x").call();

            // Switch back to main
            git.checkout().setName("main").call();
        }
    }

    private void createTestConfig() throws IOException {
        String yaml = """
            site:
              title: Integration Test Docs
              url: https://test.example.org
              default_language: en
            output:
              dir: %s
              clean: true
            content:
              sources:
                - id: mydocs
                  display_name: My Documentation
                  url: file://%s
                  branches:
                    - name: main
                      display_version: Current
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
            """.formatted(outputRoot.toString(), repoDir.toString());
        Files.writeString(configFile, yaml);
    }
}
