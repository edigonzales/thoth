package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the publication package: page fragments without frame,
 * exhaustively mapped manifest and internal search data.
 */
class PublicationPackageIntegrationTest {

    @TempDir
    Path tempDir;

    private String previousWorkDir;

    @AfterEach
    void restoreWorkDirProperty() {
        if (previousWorkDir == null) {
            System.clearProperty("thoth.work.dir");
        } else {
            System.setProperty("thoth.work.dir", previousWorkDir);
        }
    }

    @Test
    void writesManifestFragmentsAndFiles() throws Exception {
        previousWorkDir = System.getProperty("thoth.work.dir");
        System.setProperty("thoth.work.dir", tempDir.resolve("work").toString());

        Path publicRepo = tempDir.resolve("public-repo");
        Path internalRepo = tempDir.resolve("internal-repo");
        new TestRepoBuilder(publicRepo).withBasicDocs();
        new TestRepoBuilder(internalRepo).withBasicDocs();

        Path outputDir = tempDir.resolve("output");
        Path packageDir = tempDir.resolve("package");
        Path configFile = tempDir.resolve("biblios.yml");
        new BibliosConfigBuilder()
            .withSiteTitle("Package Test")
            .withOutputDir(outputDir)
            .withSource(new BibliosConfigBuilder.SourceEntry(sourceYaml(publicRepo, "public-docs", "Public Docs", "public")))
            .withSource(new BibliosConfigBuilder.SourceEntry(sourceYaml(internalRepo, "internal-docs", "Internal Docs", "internal")))
            .writeTo(configFile);
        Files.writeString(tempDir.resolve("access.yml"), """
            default: deny
            policies:
              public:
                mode: public
              internal:
                mode: restricted
                allow:
                  groups:
                    - provider: entra-kanton
                      id: group-internal
            """);

        int exitCode = new CommandLine(new ThothBibliosCli()).execute(List.of(
            "build",
            "--config", configFile.toString(),
            "--output", outputDir.toString(),
            "--public-export",
            "--package", packageDir.toString()
        ).toArray(String[]::new));

        assertEquals(0, exitCode);

        Path manifest = packageDir.resolve("manifest.json");
        assertTrue(Files.exists(manifest));
        String manifestJson = Files.readString(manifest);
        assertTrue(manifestJson.contains("\"path\": \"site-assets/styles.css\""), manifestJson);
        assertTrue(manifestJson.contains("\"scope\": \"shared\""), manifestJson);
        assertTrue(manifestJson.contains("\"path\": \"public-docs/main/\""), manifestJson);
        assertTrue(manifestJson.contains("\"kind\": \"page\""), manifestJson);
        assertFalse(manifestJson.contains("internal-docs"), manifestJson);
        assertTrue(manifestJson.contains("\"path\": \"search-index.json\""), manifestJson);
        assertTrue(manifestJson.contains("\"scope\": \"internal\""), manifestJson);

        Path fragment = packageDir.resolve("pages/public-docs/main/index.html");
        assertTrue(Files.exists(fragment));
        String fragmentHtml = Files.readString(fragment);
        assertTrue(fragmentHtml.contains("Welcome"), fragmentHtml);
        assertFalse(fragmentHtml.contains("<!DOCTYPE html>"), fragmentHtml);
        assertFalse(fragmentHtml.contains("site-header"), fragmentHtml);

        assertTrue(Files.exists(packageDir.resolve("files/site-assets/styles.css")));
        assertTrue(Files.exists(packageDir.resolve("search-index.json")));
    }

    private static String sourceYaml(Path repoDir, String id, String displayName, String accessPolicy) {
        return """
            - id: %s
              display_name: %s
              url: file://%s
              branches:
                - name: main
                  display_version: main
              start_path: docs
              default_version: main
              navigation:
                file: nav.yml
              access_policy: %s
            """.formatted(id, displayName, repoDir.toString(), accessPolicy);
    }
}
