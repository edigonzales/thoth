package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WorkingTreeBuildIntegrationTest {
    @TempDir Path tempDir;

    @Test
    void buildUsesWorkingTreeOnlyWhenRequestedAndCleansDeletedPages() throws Exception {
        String previous = System.getProperty("thoth.work.dir");
        System.setProperty("thoth.work.dir", tempDir.resolve("cache").toString());
        try {
            Path repo = tempDir.resolve("repo");
            new TestRepoBuilder(repo).withBasicDocs();
            Files.writeString(repo.resolve("docs/index.adoc"), "= Welcome\n\nUNCOMMITTED_MARKER\n");
            Path nav = repo.resolve("docs/nav.yml");
            String originalNav = Files.readString(nav);
            Files.writeString(nav, originalNav + "  - title: Extra\n    page: extra.adoc\n");
            Path extra = repo.resolve("docs/extra.adoc");
            Files.writeString(extra, "= Extra\n\nUNTRACKED_MARKER\n");
            Path output = tempDir.resolve("site");
            Path config = tempDir.resolve("biblios.yml");
            new BibliosConfigBuilder().withOutputDir(output)
                .withSingleSourceGitRepo(repo, "docs", "Docs", "docs", "main", "main")
                .writeTo(config);
            assertEquals(0, build(config, false));
            assertFalse(html(output).contains("UNCOMMITTED_MARKER"));
            assertFalse(html(output).contains("UNTRACKED_MARKER"));
            assertEquals(0, build(config, true));
            assertTrue(html(output).contains("UNCOMMITTED_MARKER"));
            assertTrue(html(output).contains("UNTRACKED_MARKER"));
            Files.delete(extra);
            Files.writeString(nav, originalNav);
            assertEquals(0, build(config, true));
            assertFalse(html(output).contains("UNTRACKED_MARKER"));
        } finally {
            if (previous == null) System.clearProperty("thoth.work.dir");
            else System.setProperty("thoth.work.dir", previous);
        }
    }

    private int build(Path config, boolean workingTree) {
        var args = new java.util.ArrayList<>(java.util.List.of("build", "--config", config.toString()));
        if (workingTree) args.add("--use-local-working-tree");
        return new CommandLine(new ThothBibliosCli()).execute(args.toArray(String[]::new));
    }

    private String html(Path output) throws Exception {
        StringBuilder text = new StringBuilder();
        try (var files = Files.walk(output)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".html")).toList())
                text.append(Files.readString(file));
        }
        return text.toString();
    }
}
