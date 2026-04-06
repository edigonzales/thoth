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

    private Path resourcePath(String name) {
        return Path.of(getClass().getClassLoader().getResource(name).getPath());
    }
}
