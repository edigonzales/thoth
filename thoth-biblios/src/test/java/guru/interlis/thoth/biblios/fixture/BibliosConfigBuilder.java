package guru.interlis.thoth.biblios.fixture;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for creating biblios.yml configuration files for testing.
 */
public final class BibliosConfigBuilder {

    private String siteTitle = "Test Docs";
    private String siteUrl = "https://test.example.org";
    private String siteLanguage = "en";
    private Path outputDir;
    private boolean clean = true;
    private final java.util.ArrayList<SourceEntry> sources = new java.util.ArrayList<>();

    public BibliosConfigBuilder withSiteTitle(String title) {
        this.siteTitle = title;
        return this;
    }

    public BibliosConfigBuilder withSiteUrl(String url) {
        this.siteUrl = url;
        return this;
    }

    public BibliosConfigBuilder withOutputDir(Path dir) {
        this.outputDir = dir;
        return this;
    }

    public BibliosConfigBuilder withClean(boolean clean) {
        this.clean = clean;
        return this;
    }

    public BibliosConfigBuilder withSource(SourceEntry source) {
        this.sources.add(source);
        return this;
    }

    public BibliosConfigBuilder withSingleSourceGitRepo(Path repoDir, String sourceId, String displayName,
                                                        String startPath, String defaultVersion,
                                                        String... branches) {
        StringBuilder branchesYaml = new StringBuilder();
        for (String branch : branches) {
            branchesYaml.append("          - name: %s\n".formatted(branch));
            branchesYaml.append("            display_version: %s\n".formatted(branch));
        }

        String sourceYaml = """
            - id: %s
              display_name: %s
              url: file://%s
              branches:
            %s
              start_path: %s
              default_version: %s
              navigation:
                file: nav.yml
            """.formatted(sourceId, displayName, repoDir.toString(), branchesYaml, startPath, defaultVersion);

        return withSource(new SourceEntry(sourceYaml));
    }

    public BibliosConfig writeTo(Path configFile) throws IOException {
        if (outputDir == null) {
            throw new IllegalStateException("Output directory must be set");
        }
        if (sources.isEmpty()) {
            throw new IllegalStateException("At least one source must be added");
        }

        StringBuilder sourcesYaml = new StringBuilder();
        for (SourceEntry source : sources) {
            // Indent each source entry by 4 spaces to nest under content.sources
            for (String line : source.yaml().split("\n")) {
                sourcesYaml.append("    ").append(line).append("\n");
            }
        }

        String yaml = """
            site:
              title: %s
              url: %s
              default_language: %s
            output:
              dir: %s
              clean: %s
            content:
              sources:
            %s""".formatted(siteTitle, siteUrl, siteLanguage, outputDir.toString(), clean,
                sourcesYaml.toString().stripTrailing());

        Files.writeString(configFile, yaml);

        // Parse and return the config
        try {
            BibliosConfigParser parser = new BibliosConfigParser();
            return parser.parse(configFile);
        } catch (Exception e) {
            throw new IOException("Failed to parse generated config: " + configFile, e);
        }
    }

    public record SourceEntry(String yaml) {
    }
}
