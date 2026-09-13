package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the public-export behavior of the build command:
 * protected sources must abort a regular static export, while
 * {@code --public-export} generates a consistent public-only site.
 */
class PublicExportIntegrationTest {

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
    void buildAbortsWhenProtectedSourcesExist() throws Exception {
        Fixture fixture = createFixture(true);

        int exitCode = executeBuild(fixture);

        assertEquals(2, exitCode);
        assertFalse(Files.exists(fixture.outputDir().resolve("index.html")));
    }

    @Test
    void publicExportBuildsOnlyPublicSources() throws Exception {
        Fixture fixture = createFixture(true);

        int exitCode = executeBuild(fixture, "--public-export");

        assertEquals(0, exitCode);
        assertTrue(Files.exists(fixture.outputDir().resolve("index.html")));
        String home = Files.readString(fixture.outputDir().resolve("index.html"));
        assertTrue(home.contains("Public Docs"));
        assertFalse(home.contains("Internal Docs"));
        assertTrue(Files.exists(fixture.outputDir().resolve("public-docs")));
        assertFalse(Files.exists(fixture.outputDir().resolve("internal-docs")));
    }

    @Test
    void unknownPolicyReferenceFailsTheBuild() throws Exception {
        Fixture fixture = createFixture(false);

        int exitCode = executeBuild(fixture, "--public-export");

        assertNotEquals(0, exitCode);
    }

    private int executeBuild(Fixture fixture, String... extraArgs) {
        java.util.List<String> argv = new java.util.ArrayList<>();
        argv.add("build");
        argv.add("--config");
        argv.add(fixture.configFile().toString());
        argv.add("--output");
        argv.add(fixture.outputDir().toString());
        argv.addAll(java.util.List.of(extraArgs));
        return new CommandLine(new ThothBibliosCli()).execute(argv.toArray(String[]::new));
    }

    private Fixture createFixture(boolean withInternalPolicy) throws Exception {
        previousWorkDir = System.getProperty("thoth.work.dir");
        System.setProperty("thoth.work.dir", tempDir.resolve("work").toString());

        Path publicRepo = tempDir.resolve("public-repo");
        Path internalRepo = tempDir.resolve("internal-repo");
        new TestRepoBuilder(publicRepo).withBasicDocs();
        new TestRepoBuilder(internalRepo).withBasicDocs();

        Path outputDir = tempDir.resolve("output");
        Path configFile = tempDir.resolve("biblios.yml");
        new BibliosConfigBuilder()
            .withSiteTitle("Access Test")
            .withOutputDir(outputDir)
            .withSource(new BibliosConfigBuilder.SourceEntry(sourceYaml(publicRepo, "public-docs", "Public Docs", "public")))
            .withSource(new BibliosConfigBuilder.SourceEntry(sourceYaml(internalRepo, "internal-docs", "Internal Docs", "internal")))
            .writeTo(configFile);

        String policies = withInternalPolicy
            ? """
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
                """
            : """
                default: deny
                policies:
                  public:
                    mode: public
                """;
        Files.writeString(tempDir.resolve("access.yml"), policies);

        return new Fixture(configFile, outputDir);
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

    private record Fixture(Path configFile, Path outputDir) {
    }
}
