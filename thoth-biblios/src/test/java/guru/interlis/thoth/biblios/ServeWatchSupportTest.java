package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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
        Map<String, Path> roots = ServeWatchSupport.localSourceRootsForServe(config, configFile, true);

        assertEquals(2, roots.size());
        assertEquals(localRepo.toAbsolutePath().normalize(), roots.get("local-file-url"));
        assertEquals(relativeRepo.toAbsolutePath().normalize(), roots.get("local-relative-path"));
    }

    @Test
    void disablesLocalSourceRootsWhenLocalWorkingTreeModeIsOff(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path configFile = tempDir.resolve("biblios.yml");
        Files.writeString(configFile, """
            site:
              title: Test
            output:
              dir: build/site
            content:
              sources:
                - id: local
                  display_name: Local
                  url: file://%s
                  branches:
                    - name: main
            """.formatted(repo.toUri().getPath()));

        BibliosConfig config = new BibliosConfigParser().parse(configFile);
        Map<String, Path> roots = ServeWatchSupport.localSourceRootsForServe(config, configFile, false);
        assertTrue(roots.isEmpty());
    }

    @Test
    void resolvesChangedSourceByLongestMatchingRoot() {
        Path root = Path.of("/tmp/work/repo").toAbsolutePath().normalize();
        Path nestedRoot = root.resolve("docs/sub").normalize();
        Map<String, Path> rootsById = new LinkedHashMap<>();
        rootsById.put("repo", root);
        rootsById.put("nested", nestedRoot);

        assertEquals(
            "nested",
            ServeWatchSupport.findChangedSourceId(rootsById, nestedRoot.resolve("page.adoc"))
        );
        assertEquals(
            "repo",
            ServeWatchSupport.findChangedSourceId(rootsById, root.resolve("docs/index.adoc"))
        );
        assertNull(
            ServeWatchSupport.findChangedSourceId(rootsById, Path.of("/tmp/elsewhere/page.adoc"))
        );
    }

    @Test
    void ignoresGitMetadataChanges() {
        assertTrue(ServeWatchSupport.shouldIgnoreRepoMetadataChange(Path.of("/tmp/repo/.git/refs/heads/main")));
        assertTrue(ServeWatchSupport.shouldIgnoreRepoMetadataChange(Path.of(".git/index")));
        assertFalse(ServeWatchSupport.shouldIgnoreRepoMetadataChange(Path.of("/tmp/repo/docs/index.adoc")));
    }

    @Test
    void detectsHtmlCustomizationPaths(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve("biblios.yml");
        Path assetFile = tempDir.resolve("assets/styles.css");
        Path templateFile = tempDir.resolve("templates/layout.ftl");
        Path contentFile = tempDir.resolve("docs/index.adoc");

        Files.createDirectories(assetFile.getParent());
        Files.createDirectories(templateFile.getParent());
        Files.createDirectories(contentFile.getParent());
        Files.writeString(configFile, "site:\n  title: Test\n");
        Files.writeString(assetFile, "body { color: red; }");
        Files.writeString(templateFile, "<html></html>");
        Files.writeString(contentFile, "= Index\n");

        assertTrue(ServeWatchSupport.isHtmlCustomizationPath(configFile, assetFile));
        assertTrue(ServeWatchSupport.isHtmlCustomizationPath(configFile, templateFile));
        assertFalse(ServeWatchSupport.isHtmlCustomizationPath(configFile, contentFile));
        assertFalse(ServeWatchSupport.isHtmlCustomizationPath(configFile, configFile));
    }
}
