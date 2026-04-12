package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThothBibliosCliOutputPathResolutionTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesRelativeConfigOutputAgainstConfigDirectoryEvenWhenCwdDiffers() throws Exception {
        Path configDir = tempDir.resolve("nested/config");
        Path configFile = configDir.resolve("biblios.yml");
        writeConfig(configFile, "./site");
        BibliosConfig config = new BibliosConfigParser().parse(configFile);

        String previousUserDir = System.getProperty("user.dir");
        Path alternateCwd = tempDir.resolve("other-cwd");
        Files.createDirectories(alternateCwd);
        System.setProperty("user.dir", alternateCwd.toString());
        try {
            Path resolved = ThothBibliosCli.resolveOutputDir(configFile, config, null);
            assertEquals(configDir.resolve("site").toAbsolutePath().normalize(), resolved);
        } finally {
            System.setProperty("user.dir", previousUserDir);
        }
    }

    @Test
    void keepsAbsoluteConfigOutputUnchanged() throws Exception {
        Path absoluteOutput = tempDir.resolve("absolute-site").toAbsolutePath().normalize();
        Path configFile = tempDir.resolve("biblios.yml");
        writeConfig(configFile, absoluteOutput.toString());
        BibliosConfig config = new BibliosConfigParser().parse(configFile);

        Path resolved = ThothBibliosCli.resolveOutputDir(configFile, config, null);
        assertEquals(absoluteOutput, resolved);
    }

    @Test
    void outputOverrideTakesPrecedenceAndUsesCliCwdSemantics() throws Exception {
        Path configFile = tempDir.resolve("biblios.yml");
        writeConfig(configFile, "./site");
        BibliosConfig config = new BibliosConfigParser().parse(configFile);

        String previousUserDir = System.getProperty("user.dir");
        Path cliCwd = tempDir.resolve("cli-cwd");
        Files.createDirectories(cliCwd);
        System.setProperty("user.dir", cliCwd.toString());
        try {
            Path resolved = ThothBibliosCli.resolveOutputDir(configFile, config, Path.of("override-site"));
            assertEquals(Path.of("override-site").toAbsolutePath().normalize(), resolved);
        } finally {
            System.setProperty("user.dir", previousUserDir);
        }
    }

    @Test
    void repeatedResolutionIsStableForBuildAndServePaths() throws Exception {
        Path configDir = tempDir.resolve("project/config");
        Path configFile = configDir.resolve("biblios.yml");
        writeConfig(configFile, "site");

        BibliosConfig firstParse = new BibliosConfigParser().parse(configFile);
        BibliosConfig secondParse = new BibliosConfigParser().parse(configFile);

        Path buildResolved = ThothBibliosCli.resolveOutputDir(configFile, firstParse, null);
        Path serveResolved = ThothBibliosCli.resolveOutputDir(configFile, secondParse, null);

        assertEquals(configDir.resolve("site").toAbsolutePath().normalize(), buildResolved);
        assertEquals(buildResolved, serveResolved);
    }

    private void writeConfig(Path configFile, String outputDir) throws Exception {
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, """
            site:
              title: Test Docs
              url: https://example.org
              default_language: en
            output:
              dir: %s
              clean: true
            content:
              sources:
                - id: docs
                  display_name: Docs
                  url: https://example.org/docs.git
                  branches:
                    - name: main
                      display_version: Latest
                  start_path: docs
                  default_version: main
                  navigation:
                    file: nav.yml
            """.formatted(outputDir));
    }
}
