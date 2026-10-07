package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicationPackageIntegrationTest {
    @TempDir Path tempDir;
    private String previousWorkDir;

    @BeforeEach
    void setWorkDir() {
        previousWorkDir = System.getProperty("thoth.work.dir");
        System.setProperty("thoth.work.dir", tempDir.resolve("work").toString());
    }

    @AfterEach
    void restoreWorkDir() {
        if (previousWorkDir == null) System.clearProperty("thoth.work.dir");
        else System.setProperty("thoth.work.dir", previousWorkDir);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void packagesProtectedSourcesWithoutWritingStaticSite(boolean includePublic) throws Exception {
        Path config = fixture(includePublic);
        Path output = tempDir.resolve("output");
        Files.createDirectories(output);
        Files.writeString(output.resolve("sentinel.txt"), "existing site");
        Path target = tempDir.resolve("package");
        Files.createDirectories(target);
        Files.writeString(target.resolve("stale.txt"), "old package");

        assertEquals(0, execute(config, target));
        assertEquals(List.of("sentinel.txt"), names(output));
        assertEquals("existing site", Files.readString(output.resolve("sentinel.txt")));
        assertFalse(Files.exists(target.resolve("stale.txt")));
        String manifest = Files.readString(target.resolve("manifest.json"));
        assertTrue(manifest.contains("internal-docs/main/"), manifest);
        assertTrue(manifest.contains("site-assets/styles.css"), manifest);
        assertEquals(includePublic, manifest.contains("public-docs/main/"));
        String catalog = Files.readString(target.resolve("catalog.json"));
        assertTrue(catalog.contains("internal-docs"));
        assertTrue(catalog.contains("\"accessPolicy\":\"internal\""), catalog);
        String fragment = Files.readString(target.resolve("pages/internal-docs/main/index.html"));
        assertTrue(fragment.contains("Welcome"));
        assertFalse(fragment.contains("<!DOCTYPE html>"));
        assertFalse(fragment.contains("site-header"));
        assertArrayEquals(Files.readAllBytes(tempDir.resolve("internal-repo/docs/secret.png")),
            Files.readAllBytes(target.resolve("files/internal-docs/main/secret.png")));
        assertTrue(Files.readString(target.resolve("search-index.json")).contains("internal-docs"));
    }

    @Test
    void packageDoesNotCreateConfiguredSite() throws Exception {
        assertEquals(0, execute(fixture(false), tempDir.resolve("package")));
        assertFalse(Files.exists(tempDir.resolve("output")));
    }

    @Test
    void rejectsUnknownPolicyBeforeWritingAnything() throws Exception {
        Path config = fixture(false);
        Files.writeString(tempDir.resolve("access.yml"), "default: public\n");
        assertNotEquals(0, execute(config, tempDir.resolve("package")));
        assertFalse(Files.exists(tempDir.resolve("package")));
        assertFalse(Files.exists(tempDir.resolve("output")));
    }

    @Test
    void removesTemporaryOutputAfterRenderingFailure() throws Exception {
        Path config = fixture(false);
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates);
        Files.writeString(templates.resolve("index.ftl"), "${missingRequiredValue}");
        assertNotEquals(0, execute(config, tempDir.resolve("package")));
        assertFalse(Files.exists(tempDir.resolve("output")));
    }

    @Test
    void rejectsOverlappingPackageDirectoriesBeforeMutation() throws Exception {
        Path config = fixture(false);
        Path output = tempDir.resolve("output");
        Files.createDirectories(output);
        Files.writeString(output.resolve("sentinel.txt"), "keep");
        for (Path target : List.of(output, output.resolve("package"), tempDir)) {
            assertEquals(2, execute(config, target));
            assertEquals("keep", Files.readString(output.resolve("sentinel.txt")));
        }
        Path alias = tempDir.resolve("alias");
        Files.createSymbolicLink(alias, output);
        assertEquals(2, execute(config, alias.resolve("package")));
        assertFalse(Files.exists(output.resolve("package")));
    }

    @Test
    void packagesPdfAndDocxDownloads() throws Exception {
        Path config = fixture(false);
        Files.writeString(config, "\npdf:\n  enabled: true\ndocx:\n  enabled: true\n", java.nio.file.StandardOpenOption.APPEND);
        Path target = tempDir.resolve("package");
        assertEquals(0, execute(config, target, "--format", "html,pdf,docx", "--docx-version", "main"));
        String manifest = Files.readString(target.resolve("manifest.json"));
        for (String extension : List.of("pdf", "docx")) {
            String route = "internal-docs/main/internal-docs-main." + extension;
            assertTrue(manifest.contains(route));
            assertTrue(Files.size(target.resolve("files").resolve(route)) > 0);
        }
        assertFalse(Files.exists(tempDir.resolve("output")));
    }

    private int execute(Path config, Path target, String... extraArgs) throws Exception {
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        PrintStream original = System.out;
        int result;
        try (PrintStream capture = new PrintStream(log, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            var args = new ArrayList<>(List.of("build", "--config", config.toString(), "--package", target.toString()));
            args.addAll(List.of(extraArgs));
            result = new CommandLine(new ThothBibliosCli()).execute(args.toArray(String[]::new));
        } finally {
            System.setOut(original);
        }
        String output = log.toString(StandardCharsets.UTF_8);
        original.print(output);
        for (String line : output.lines().toList()) {
            if (line.startsWith("[info] output: ")) {
                Path site = Path.of(line.substring("[info] output: ".length()));
                assertTrue(site.getParent().getFileName().toString().startsWith("thoth-biblios-package-"));
                assertFalse(Files.exists(site.getParent()), "Temporary output leaked: " + site);
            }
        }
        return result;
    }

    private Path fixture(boolean includePublic) throws Exception {
        var builder = new BibliosConfigBuilder().withSiteTitle("Package Test").withOutputDir(tempDir.resolve("output"));
        var ids = new ArrayList<>(List.of("internal"));
        if (includePublic) ids.add("public");
        for (String id : ids) {
            Path repo = tempDir.resolve(id + "-repo");
            new TestRepoBuilder(repo).withBasicDocs();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB),
                "png", repo.resolve("docs/secret.png").toFile());
            Files.writeString(repo.resolve("docs/index.adoc"), "\nimage::secret.png[]\n", java.nio.file.StandardOpenOption.APPEND);
            try (var git = org.eclipse.jgit.api.Git.open(repo.toFile())) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage("attachment").setAuthor("Test", "test@example.org").call();
            }
            builder.withSource(new BibliosConfigBuilder.SourceEntry("""
                - id: %s-docs
                  display_name: %s Docs
                  url: file://%s
                  branches:
                    - name: main
                  start_path: docs
                  navigation:
                    file: nav.yml
                  access_policy: %s
                """.formatted(id, id, repo, id)));
        }
        Path config = tempDir.resolve("biblios.yml");
        builder.writeTo(config);
        Files.writeString(tempDir.resolve("access.yml"), """
            default: deny
            policies:
              public:
                mode: public
              internal:
                mode: restricted
                allow:
                  groups:
                    - provider: keycloak-local
                      id: agi-betrieb
            """);
        return config;
    }

    private List<String> names(Path directory) throws Exception {
        try (var files = Files.list(directory)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }
}
