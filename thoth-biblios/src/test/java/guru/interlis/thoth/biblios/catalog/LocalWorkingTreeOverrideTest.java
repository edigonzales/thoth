package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.config.BranchConfig;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalWorkingTreeOverrideTest {

    @TempDir Path tempDir;

    @Test
    void resolvesCurrentBranchForLocalSource() throws Exception {
        Path repoDir = tempDir.resolve("repo");
        Path configFile = tempDir.resolve("biblios.yml");
        Files.writeString(configFile, "site:\n  title: test\noutput:\n  dir: out\ncontent:\n  sources: []\n");

        new TestRepoBuilder(repoDir).withBasicDocs();

        SourceConfig source = source("mydocs", "file://" + repoDir, List.of("main", "v1.x"));
        LocalWorkingTreeOverride override = LocalWorkingTreeOverride.resolve(source, configFile, true);

        assertTrue(override.isEnabled());
        assertEquals(repoDir.toAbsolutePath().normalize(), override.sourceRoot());
        assertEquals("main", override.currentBranch());
        assertTrue(override.appliesToBranch("main"));
        assertFalse(override.appliesToBranch("v1.x"));
    }

    @Test
    void returnsDisabledForRemoteSource() throws Exception {
        Path configFile = tempDir.resolve("biblios.yml");
        Files.writeString(configFile, "site:\n  title: test\noutput:\n  dir: out\ncontent:\n  sources: []\n");

        SourceConfig source = source("remote", "https://example.org/repo.git", List.of("main"));
        LocalWorkingTreeOverride override = LocalWorkingTreeOverride.resolve(source, configFile, true);

        assertFalse(override.isEnabled());
        assertNull(override.sourceRoot());
        assertNull(override.currentBranch());
    }

    @Test
    void failsForInvalidLocalRepository() throws Exception {
        Path configFile = tempDir.resolve("biblios.yml");
        Files.writeString(configFile, "site:\n  title: test\noutput:\n  dir: out\ncontent:\n  sources: []\n");
        Path invalidRepo = tempDir.resolve("invalid-repo");
        Files.createDirectories(invalidRepo);

        SourceConfig source = source("invalid", "file://" + invalidRepo, List.of("main"));

        IOException exception = assertThrows(IOException.class,
            () -> LocalWorkingTreeOverride.resolve(source, configFile, true));
        assertTrue(exception.getMessage().contains("Failed to detect current branch"));
    }

    private SourceConfig source(String id, String url, List<String> branches) {
        List<BranchConfig> branchConfigs = branches.stream()
            .map(name -> new BranchConfig(name, name))
            .toList();
        return new SourceConfig(
            id,
            id,
            url,
            branchConfigs,
            "docs",
            "main",
            null,
            "index.adoc"
        );
    }
}
