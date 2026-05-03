package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class BibliosArtifactIntegrationTest {

    @TempDir Path tempDir;

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
    void fullBuildPipelineGeneratesPdfArtifactsWithSemanticAttributes() throws Exception {
        Path repoDir = tempDir.resolve("pdf-semantic-repo");
        Path workRoot = tempDir.resolve("pdf-semantic-work");
        Path outputRoot = tempDir.resolve("pdf-semantic-output");
        Path configFile = tempDir.resolve("pdf-semantic-biblios.yml");
        Path themeDir = tempDir.resolve("themes");
        Path themeFile = themeDir.resolve("pdf-theme.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);
        Files.createDirectories(themeDir);
        Files.writeString(themeFile, "extends: default\n");

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("PDF Semantic Docs")
            .withOutputDir(outputRoot)
            .withPdfEnabled(true)
            .withPdfAttributes(Map.of(
                "pdf-theme", "./themes/pdf-theme.yml",
                "toc", true,
                "sectnums", true,
                "chapter-signifier", false
            ))
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
    void generateSupportsPdfAndDocxWithoutHtml() throws Exception {
        Path repoDir = tempDir.resolve("artifact-only-repo");
        Path workRoot = tempDir.resolve("artifact-only-work");
        Path outputRoot = tempDir.resolve("artifact-only-output");
        Path configFile = tempDir.resolve("artifact-only-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("Artifact Only Docs")
            .withOutputDir(outputRoot)
            .withPdfEnabled(true)
            .withDocxEnabled(true)
            .withSingleSourceGitRepo(repoDir, "mydocs", "My Documentation",
                "docs", "main", "main")
            .writeTo(configFile);

        try (CatalogBuilder builder = new CatalogBuilder(config, workRoot)) {
            SiteCatalog catalog = builder.build();
            try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputRoot)) {
                generator.generate(false, true, Set.of("main"), true, Set.of("main"));
            }
        }

        assertFalse(Files.exists(outputRoot.resolve("index.html")));
        assertFalse(Files.exists(outputRoot.resolve("search/index.html")));
        assertFalse(Files.exists(outputRoot.resolve("site-assets/styles.css")));
        assertFalse(Files.exists(outputRoot.resolve("mydocs/main/index.html")));

        Path pdfFile = outputRoot.resolve("mydocs/main/mydocs-main.pdf");
        Path docxFile = outputRoot.resolve("mydocs/main/mydocs-main.docx");
        assertTrue(Files.exists(pdfFile));
        assertTrue(Files.exists(docxFile));
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
        String styles = readDocxEntry(docxFile, "word/styles.xml");
        String numbering = readDocxEntry(docxFile, "word/numbering.xml");
        String settings = readDocxEntry(docxFile, "word/settings.xml");

        assertTrue(xml.contains("SEQ Figure"));
        assertTrue(xml.contains(" REF "));
        assertTrue(xml.contains("\\n \\h"));
        assertTrue(xml.contains(" PAGEREF "));
        assertTrue(xml.contains("w:bookmarkStart"));
        assertTrue(xml.contains("fig_overview"));
        assertTrue(xml.contains("w:sectPr"));
        assertTrue(xml.contains("w:numPr"));
        assertTrue(xml.contains("TOC \\o"));
        assertTrue(xml.contains("\"1-3\""));
        assertTrue(xml.contains("Abbildung "));
        assertFalse(xml.contains("Figure 1: Abbildung"));
        assertFalse(xml.matches("(?s).*<w:bookmarkStart[^>]*w:name=\"fig_overview\"[^>]*/><w:bookmarkEnd.*"));
        assertTrue(styles.contains("w:styleId=\"Heading1\""));
        assertTrue(styles.contains("<w:outlineLvl w:val=\"0\""));
        assertTrue(numbering.contains("%1.%2."));
        assertTrue(settings.contains("w:updateFields"));

        ImageExtent extent = firstImageExtent(xml);
        double ratio = (double) extent.cx() / extent.cy();
        assertEquals(4.0, ratio, 0.02);
        assertTrue(extent.cx() > 5_000_000);
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

    @Test
    void docxRendersTablesAdmonitionsCalloutsAndFootnotes() throws Exception {
        Path repoDir = tempDir.resolve("docx-rich-repo");
        Path workRoot = tempDir.resolve("docx-rich-work");
        Path outputRoot = tempDir.resolve("docx-rich-output");
        Path configFile = tempDir.resolve("docx-rich-biblios.yml");

        Files.createDirectories(repoDir);
        Files.createDirectories(workRoot);
        Files.createDirectories(outputRoot);

        new TestRepoBuilder(repoDir).withBasicDocs();
        prepareDocxRichFixture(repoDir);

        BibliosConfig config = new BibliosConfigBuilder()
            .withSiteTitle("DOCX Rich Docs")
            .withSiteLanguage("en")
            .withOutputDir(outputRoot)
            .withDocxEnabled(true)
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
        String footnotes = readDocxEntry(docxFile, "word/footnotes.xml");

        assertTrue(xml.contains("Deployment Matrix"));
        assertTrue(xml.contains("<w:tbl>"));
        assertTrue(xml.contains("Note block for DOCX export."));
        assertTrue(xml.contains("(1)"));
        assertTrue(xml.contains("Use the default environment."));
        assertTrue(xml.contains("w:footnoteReference"));
        assertTrue(xml.contains("EE F4 FF".replace(" ", "")) || xml.contains("EEF4FF"));
        assertTrue(footnotes.contains("Footnote body for DOCX export."));
    }

    private void prepareDocxReferenceFixture(Path repoDir) throws Exception {
        Files.createDirectories(repoDir.resolve("docs/images"));
        BufferedImage overview = new BufferedImage(1600, 400, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(overview, "png", repoDir.resolve("docs/images/overview.png").toFile());

        try (Git git = Git.open(repoDir.toFile())) {
            Files.writeString(repoDir.resolve("docs/guide.adoc"), """
                = User Guide
                :doctype: book

                Siehe link:#installation[Kapitel 1, Installation].
                Siehe link:#fig-overview[Abbildung].
                Siehe auf Seite link:#fig-overview[dieser Abbildung].

                [#installation]
                == Installation

                Installationshinweise.

                [#fig-overview]
                .Abbildung 7. System overview
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

    private void prepareDocxRichFixture(Path repoDir) throws Exception {
        try (Git git = Git.open(repoDir.toFile())) {
            Files.writeString(repoDir.resolve("docs/guide.adoc"), """
                = User Guide
                :doctype: book

                This paragraph contains a footnote:footnote:[Footnote body for DOCX export.].

                [NOTE]
                ====
                Note block for DOCX export.
                ====

                [source,interlis]
                ----
                MODEL Sample (en)
                END Sample. <1>
                ----

                <1> Use the default environment.

                .Deployment Matrix
                |===
                |Environment |Status

                |Development
                |Ready

                |Production
                |Planned
                |===

                API:: Stable
                CLI:: Fast
                """);

            Files.writeString(repoDir.resolve("docs/nav.yml"), """
                items:
                  - title: Welcome
                    page: index.adoc
                  - title: User Guide
                    page: guide.adoc
                """);
            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add DOCX rich fixture").call();
        }
    }

    private ImageExtent firstImageExtent(String documentXml) {
        Pattern pattern = Pattern.compile("<wp:extent cx=\"(\\d+)\" cy=\"(\\d+)\"");
        Matcher matcher = pattern.matcher(documentXml);
        assertTrue(matcher.find(), "Expected at least one DOCX image extent");
        return new ImageExtent(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)));
    }

    private record ImageExtent(long cx, long cy) {
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
}
