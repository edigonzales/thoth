package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServeWatchSupportTest {

    @Test
    void detectsLocalSourcesAndFiltersRemoteUrls(@TempDir Path tempDir) throws Exception {
        Path localRepo = tempDir.resolve("local-repo");
        Path relativeRepo = tempDir.resolve("relative-repo");
        Files.createDirectories(localRepo);
        Files.createDirectories(relativeRepo);

        Path configFile = tempDir.resolve("biblios.yml");
        Files.writeString(configFile, """
            site:
              title: Test
            output:
              dir: build/site
            content:
              sources:
                - id: local-file-url
                  display_name: Local File URL
                  url: file://%s
                  branches:
                    - name: main
                - id: local-relative-path
                  display_name: Local Relative Path
                  url: relative-repo
                  branches:
                    - name: main
                - id: remote-https
                  display_name: Remote HTTPS
                  url: https://example.org/repo.git
                  branches:
                    - name: main
                - id: remote-ssh
                  display_name: Remote SSH
                  url: git@example.org:team/repo.git
                  branches:
                    - name: main
            """.formatted(localRepo.toUri().getPath()));

        BibliosConfig config = new BibliosConfigParser().parse(configFile);
        List<Path> roots = ServeWatchSupport.localSourceRoots(config, configFile);

        assertEquals(2, roots.size());
        assertTrue(roots.contains(localRepo.toAbsolutePath().normalize()));
        assertTrue(roots.contains(relativeRepo.toAbsolutePath().normalize()));
    }

    @Test
    void ignoresGitMetadataChanges() {
        assertTrue(ServeWatchSupport.shouldIgnoreRepoMetadataChange(Path.of("/tmp/repo/.git/refs/heads/main")));
        assertTrue(ServeWatchSupport.shouldIgnoreRepoMetadataChange(Path.of(".git/index")));
        assertFalse(ServeWatchSupport.shouldIgnoreRepoMetadataChange(Path.of("/tmp/repo/docs/index.adoc")));
    }
}
