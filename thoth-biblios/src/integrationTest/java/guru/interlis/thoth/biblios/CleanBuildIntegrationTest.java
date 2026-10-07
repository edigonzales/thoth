package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CleanBuildIntegrationTest {
    @TempDir Path tempDir;

    @ParameterizedTest
    @CsvSource({"false,false,false", "false,true,false", "true,false,false", "true,true,false", "false,true,true"})
    void cleansResolvedOutputOnlyWhenRequested(boolean yamlClean, boolean cliClean, boolean override) throws Exception {
        Path repo = tempDir.resolve("repo");
        new TestRepoBuilder(repo).withBasicDocs();
        Path configured = tempDir.resolve("configured");
        Path output = override ? tempDir.resolve("override") : configured;
        Files.createDirectories(configured);
        Files.writeString(configured.resolve("stale.txt"), "keep outside selected output");
        Files.createDirectories(output);
        Files.writeString(output.resolve("stale.txt"), "obsolete");
        Path config = tempDir.resolve("biblios.yml");
        new BibliosConfigBuilder().withOutputDir(configured).withClean(yamlClean)
            .withSingleSourceGitRepo(repo, "docs", "Docs", "docs", "main", "main").writeTo(config);
        var args = new ArrayList<>(List.of("build", "--config", config.toString()));
        if (cliClean) args.add("--clean");
        if (override) args.addAll(List.of("--output", output.toString()));
        String previous = System.getProperty("thoth.work.dir");
        try {
            System.setProperty("thoth.work.dir", tempDir.resolve("work").toString());
            assertEquals(0, new CommandLine(new ThothBibliosCli()).execute(args.toArray(String[]::new)));
        } finally {
            if (previous == null) System.clearProperty("thoth.work.dir");
            else System.setProperty("thoth.work.dir", previous);
        }
        assertEquals(!(yamlClean || cliClean), Files.exists(output.resolve("stale.txt")));
        assertTrue(Files.exists(output.resolve("docs/main/index.html")));
        if (override) assertTrue(Files.exists(configured.resolve("stale.txt")));
    }
}
