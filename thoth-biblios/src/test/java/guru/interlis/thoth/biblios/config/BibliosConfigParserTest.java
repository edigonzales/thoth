package guru.interlis.thoth.biblios.config;

import guru.interlis.thoth.core.ThothBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BibliosConfigParser.
 */
class BibliosConfigParserTest {

    private final BibliosConfigParser parser = new BibliosConfigParser();

    @Test
    void parsesValidConfig() throws IOException {
        BibliosConfig config = parser.parse(resourcePath("test-biblios.yml"));

        // Site section
        assertEquals("Test Documentation Portal", config.site().title());
        assertEquals("https://docs.example.org", config.site().url());
        assertNull(config.site().logo());
        assertEquals("en", config.site().defaultLanguage());
        assertEquals("alpha", config.site().defaultComponent());
        assertEquals("main", config.site().defaultVersion());

        // Output section
        assertEquals("build/test-site", config.output().dir());
        assertTrue(config.output().clean());

        // UI section
        assertEquals("default", config.ui().theme());
        assertTrue(config.ui().showVersionBadge());
        assertFalse(config.ui().showEditLink());
        assertEquals(VersionSwitchMode.START_PAGE, config.ui().versionSwitchMode());
        assertEquals(SearchLanguageMode.ENGLISH_DEFAULT, config.ui().searchLanguageMode());
        assertEquals(3, config.ui().sidebarTocDepth());
        assertEquals(ContentTocMode.ON, config.ui().contentToc());
        assertEquals(SyntaxHighlightingMode.PRISM, config.ui().syntaxHighlightingMode());
        assertTrue(config.ui().prismCustomComponents().isEmpty());
        assertFalse(config.pdf().enabled());
        assertTrue(config.pdf().attributes().isEmpty());
        assertFalse(config.docx().enabled());
        assertNull(config.docx().referenceDoc());
        assertFalse(config.docx().features().titlePage());
        assertTrue(config.docx().features().toc());
        assertFalse(config.docx().features().changeLog());

        // Content sources
        assertEquals(2, config.content().sources().size());

        // First source
        SourceConfig alpha = config.content().sources().get(0);
        assertEquals("alpha", alpha.id());
        assertEquals("Alpha Docs", alpha.displayName());
        assertEquals("https://github.com/example/alpha.git", alpha.url());
        assertEquals("docs", alpha.startPath());
        assertEquals("main", alpha.defaultVersion());
        assertEquals("index.adoc", alpha.startPage());
        assertEquals(RenderMode.SPLIT, alpha.renderMode());
        assertNull(alpha.masterFile());
        assertEquals(SidebarTocNumbersMode.OFF, alpha.sidebarTocNumbers());
        assertEquals(2, alpha.branches().size());
        assertEquals("main", alpha.branches().get(0).name());
        assertEquals("Latest", alpha.branches().get(0).displayVersion());
        assertEquals("v1.x", alpha.branches().get(1).name());
        assertEquals("1.x Legacy", alpha.branches().get(1).displayVersion());
        assertNotNull(alpha.navigation());
        assertEquals("nav.yml", alpha.navigation().file());

        // Second source
        SourceConfig beta = config.content().sources().get(1);
        assertEquals("beta", beta.id());
        assertEquals("Beta API", beta.displayName());
        assertEquals(1, beta.branches().size());
        assertEquals("Current", beta.branches().get(0).displayVersion());
        assertEquals(RenderMode.SPLIT, beta.renderMode());
        assertEquals(SidebarTocNumbersMode.OFF, beta.sidebarTocNumbers());
        assertNull(beta.pdf());
    }

    @Test
    void rejectsMissingFile() {
        ThothBuildException ex = assertThrows(ThothBuildException.class, () ->
            parser.parse(Path.of("nonexistent.yml"))
        );
        assertTrue(ex.getMessage().contains("not found"));
        assertEquals(ThothBuildException.ErrorSeverity.FATAL, ex.severity());
    }

    @Test
    void rejectsEmptySources(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources: []
            """;
        Path file = tempDir.resolve("empty.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("sources"));
    }

    @Test
    void rejectsMissingRequiredFields(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("incomplete.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("display_name"));
    }

    @Test
    void defaultsDisplayVersionToBranchName(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("default-version.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals("main", config.content().sources().get(0).branches().get(0).displayVersion());
    }

    @Test
    void parsesSiteLogoAsRemoteUrl(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
              logo: https://example.org/logo.svg
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("logo-url.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals("https://example.org/logo.svg", config.site().logo());
    }

    @Test
    void resolvesSiteLogoRelativeToConfigDirectory(@TempDir Path tempDir) throws IOException {
        Path assetsDir = tempDir.resolve("assets");
        Files.createDirectories(assetsDir);
        Path logoFile = assetsDir.resolve("logo.svg");
        Files.writeString(logoFile, "<svg/>");

        String yaml = """
            site:
              title: Test
              logo: assets/logo.svg
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("logo-relative.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(logoFile.toAbsolutePath().normalize().toString(), config.site().logo());
    }

    @Test
    void rejectsMissingLocalSiteLogo(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
              logo: assets/missing-logo.svg
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("logo-missing.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("site.logo"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void rejectsBlankSiteLogo(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
              logo: "   "
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("logo-blank.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("site.logo"));
        assertTrue(ex.getMessage().contains("must not be blank"));
    }

    @Test
    void rejectsNonStringSiteLogo(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
              logo: 123
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("logo-number.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("site.logo"));
        assertTrue(ex.getMessage().contains("Expected string"));
    }

    @Test
    void defaultsStartPathToDot(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("no-startpath.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(".", config.content().sources().get(0).startPath());
    }

    @Test
    void defaultsStartPageToIndex(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("no-startpage.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals("index.adoc", config.content().sources().get(0).startPage());
    }

    @Test
    void parsesVersionSwitchModeEquivalentPage(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              version_switch_mode: equivalent_page
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("equivalent-mode.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(VersionSwitchMode.EQUIVALENT_PAGE, config.ui().versionSwitchMode());
    }

    @Test
    void defaultsVersionSwitchModeToStartPage(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              show_version_badge: true
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("default-switch-mode.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(VersionSwitchMode.START_PAGE, config.ui().versionSwitchMode());
        assertEquals(SearchLanguageMode.MULTILINGUAL_SAFE, config.ui().searchLanguageMode());
        assertEquals(2, config.ui().sidebarTocDepth());
        assertEquals(ContentTocMode.OFF, config.ui().contentToc());
        assertEquals(SyntaxHighlightingMode.PRISM, config.ui().syntaxHighlightingMode());
        assertTrue(config.ui().prismCustomComponents().isEmpty());
        assertFalse(config.pdf().enabled());
        assertTrue(config.pdf().attributes().isEmpty());
    }

    @Test
    void rejectsInvalidVersionSwitchMode(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              version_switch_mode: something_else
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("invalid-switch-mode.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("ui.version_switch_mode"));
        assertTrue(ex.getMessage().contains("start_page"));
        assertTrue(ex.getMessage().contains("equivalent_page"));
    }

    @Test
    void parsesSearchLanguageModeEnglishDefault(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              search_language_mode: english_default
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("search-language-english.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(SearchLanguageMode.ENGLISH_DEFAULT, config.ui().searchLanguageMode());
    }

    @Test
    void defaultsSearchLanguageModeToMultilingualSafe(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              show_version_badge: true
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("search-language-default.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(SearchLanguageMode.MULTILINGUAL_SAFE, config.ui().searchLanguageMode());
    }

    @Test
    void rejectsInvalidSearchLanguageMode(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              search_language_mode: german
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("invalid-search-language.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("ui.search_language_mode"));
        assertTrue(ex.getMessage().contains("multilingual_safe"));
        assertTrue(ex.getMessage().contains("english_default"));
    }

    @Test
    void parsesSinglePageRenderModeAndMasterFile(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
                  start_path: docs
                  render_mode: single_page
                  master_file: master.adoc
            """;
        Path file = tempDir.resolve("single-page.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        SourceConfig source = config.content().sources().get(0);
        assertEquals(RenderMode.SINGLE_PAGE, source.renderMode());
        assertEquals("master.adoc", source.masterFile());
        assertEquals(SidebarTocNumbersMode.OFF, source.sidebarTocNumbers());
    }

    @Test
    void rejectsSinglePageWithoutMasterFile(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
                  start_path: docs
                  render_mode: single_page
            """;
        Path file = tempDir.resolve("single-page-missing-master.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("master_file"));
        assertTrue(ex.getMessage().contains("single_page"));
    }

    @Test
    void rejectsInvalidSidebarTocDepth(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              sidebar_toc_depth: 8
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("invalid-sidebar-depth.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("sidebar_toc_depth"));
        assertTrue(ex.getMessage().contains("1..6"));
    }

    @Test
    void parsesContentTocOn(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              content_toc: on
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("content-toc-on.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(ContentTocMode.ON, config.ui().contentToc());
    }

    @Test
    void rejectsInvalidContentTocMode(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              content_toc: maybe
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("invalid-content-toc.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("ui.content_toc"));
        assertTrue(ex.getMessage().contains("off"));
        assertTrue(ex.getMessage().contains("on"));
    }

    @Test
    void parsesSyntaxHighlightingOff(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              syntax_highlighting: off
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("syntax-highlighting-off.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(SyntaxHighlightingMode.OFF, config.ui().syntaxHighlightingMode());
    }

    @Test
    void rejectsInvalidSyntaxHighlightingMode(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              syntax_highlighting: rouge
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("invalid-syntax-highlighting.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("ui.syntax_highlighting"));
        assertTrue(ex.getMessage().contains("prism"));
        assertTrue(ex.getMessage().contains("off"));
    }

    @Test
    void parsesPrismCustomComponents(@TempDir Path tempDir) throws IOException {
        Path highlightingDir = tempDir.resolve("highlighting");
        Files.createDirectories(highlightingDir);
        Path customLang = highlightingDir.resolve("prism-custom.js");
        Files.writeString(customLang, "Prism.languages.custom = {};");

        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              prism_custom_components:
                - ./highlighting/prism-custom.js
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("custom-components.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(1, config.ui().prismCustomComponents().size());
        assertEquals(customLang.toAbsolutePath().normalize().toString(), config.ui().prismCustomComponents().get(0));
    }

    @Test
    void rejectsMissingPrismCustomComponentFile(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              prism_custom_components:
                - ./highlighting/missing.js
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("missing-custom-component.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("ui.prism_custom_components[0]"));
        assertTrue(ex.getMessage().contains("file not found"));
    }

    @Test
    void rejectsNonJavascriptPrismCustomComponentFile(@TempDir Path tempDir) throws IOException {
        Path highlightingDir = tempDir.resolve("highlighting");
        Files.createDirectories(highlightingDir);
        Path customLang = highlightingDir.resolve("prism-custom.txt");
        Files.writeString(customLang, "not-js");

        String yaml = """
            site:
              title: Test
            output:
              dir: build
            ui:
              prism_custom_components:
                - ./highlighting/prism-custom.txt
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("invalid-custom-component-extension.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains(".js"));
        assertTrue(ex.getMessage().contains("ui.prism_custom_components[0]"));
    }

    @Test
    void parsesSidebarTocNumbersOnPerSource(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
                  render_mode: single_page
                  master_file: master.adoc
                  sidebar_toc_numbers: on
            """;
        Path file = tempDir.resolve("sidebar-toc-numbers-on.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertEquals(SidebarTocNumbersMode.ON, config.content().sources().get(0).sidebarTocNumbers());
    }

    @Test
    void parsesGlobalPdfSection(@TempDir Path tempDir) throws IOException {
        Path themesDir = tempDir.resolve("themes");
        Path fontsDir = tempDir.resolve("fonts");
        Files.createDirectories(themesDir);
        Files.createDirectories(fontsDir);
        Path themeFile = themesDir.resolve("basic-theme.yml");
        Files.writeString(themeFile, "extends: default\n");

        String yaml = """
            site:
              title: Test
            output:
              dir: build
            pdf:
              enabled: true
              attributes:
                pdf-theme: ./themes/basic-theme.yml
                pdf-fontsdir:
                  - ./fonts
                  - GEM_FONTS_DIR
                optimize: default
                compress: true
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("pdf.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertTrue(config.pdf().enabled());
        assertEquals(themeFile.toAbsolutePath().normalize().toString(), config.pdf().attributes().get("pdf-theme"));
        assertEquals(fontsDir.toAbsolutePath().normalize() + ";GEM_FONTS_DIR", config.pdf().attributes().get("pdf-fontsdir"));
        assertEquals("default", config.pdf().attributes().get("optimize"));
        assertEquals(Boolean.TRUE, config.pdf().attributes().get("compress"));
    }

    @Test
    void parsesPerSourcePdfOverrides(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            pdf:
              enabled: true
              attributes:
                optimize: default
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
                  pdf:
                    enabled: false
                    master_file: pdf/book.adoc
                    attributes:
                      compress: true
            """;
        Path file = tempDir.resolve("source-pdf.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        SourceConfig source = config.content().sources().get(0);
        assertNotNull(source.pdf());
        assertEquals(Boolean.FALSE, source.pdf().enabled());
        assertEquals("pdf/book.adoc", source.pdf().masterFile());
        assertEquals(Boolean.TRUE, source.pdf().attributes().get("compress"));
    }

    @Test
    void parsesGlobalDocxSection(@TempDir Path tempDir) throws IOException {
        Path ref = tempDir.resolve("reference.docx");
        Files.writeString(ref, "fake");
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            docx:
              enabled: true
              reference_doc: ./reference.docx
              features:
                title_page: true
                toc: true
                change_log: false
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
            """;
        Path file = tempDir.resolve("docx.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        assertTrue(config.docx().enabled());
        assertEquals(ref.toAbsolutePath().normalize().toString(), config.docx().referenceDoc());
        assertTrue(config.docx().features().titlePage());
        assertTrue(config.docx().features().toc());
        assertFalse(config.docx().features().changeLog());
    }

    @Test
    void parsesPerSourceDocxOverrides(@TempDir Path tempDir) throws IOException {
        Path globalRef = tempDir.resolve("global-reference.docx");
        Path localRef = tempDir.resolve("local-reference.docx");
        Files.writeString(globalRef, "global");
        Files.writeString(localRef, "local");

        String yaml = """
            site:
              title: Test
            output:
              dir: build
            docx:
              enabled: true
              reference_doc: ./global-reference.docx
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
                  docx:
                    enabled: false
                    master_file: docx/book.adoc
                    reference_doc: ./local-reference.docx
                    features:
                      title_page: true
                      change_log: true
            """;
        Path file = tempDir.resolve("source-docx.yml");
        Files.writeString(file, yaml);

        BibliosConfig config = parser.parse(file);
        SourceConfig source = config.content().sources().get(0);
        assertNotNull(source.docx());
        assertEquals(Boolean.FALSE, source.docx().enabled());
        assertEquals("docx/book.adoc", source.docx().masterFile());
        assertEquals(localRef.toAbsolutePath().normalize().toString(), source.docx().referenceDoc());
        assertNotNull(source.docx().features());
        assertEquals(Boolean.TRUE, source.docx().features().titlePage());
        assertNull(source.docx().features().toc());
        assertEquals(Boolean.TRUE, source.docx().features().changeLog());
    }

    @Test
    void rejectsInvalidSidebarTocNumbersMode(@TempDir Path tempDir) throws IOException {
        String yaml = """
            site:
              title: Test
            output:
              dir: build
            content:
              sources:
                - id: test
                  display_name: Test
                  url: https://example.git
                  branches:
                    - name: main
                  sidebar_toc_numbers: maybe
            """;
        Path file = tempDir.resolve("invalid-sidebar-toc-numbers.yml");
        Files.writeString(file, yaml);

        ThothBuildException ex = assertThrows(ThothBuildException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("sidebar_toc_numbers"));
        assertTrue(ex.getMessage().contains("off"));
        assertTrue(ex.getMessage().contains("on"));
    }

    private Path resourcePath(String name) {
        return Path.of(getClass().getClassLoader().getResource(name).getPath());
    }
}
