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
    private boolean showEditLink = false;
    private boolean showSourceLink = false;
    private String editUrlPattern = null;
    private String sourceUrlPattern = null;
    private String versionSwitchMode = null;
    private String searchLanguageMode = null;
    private Integer sidebarTocDepth = null;
    private String contentToc = null;
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

    public BibliosConfigBuilder withEditLink(boolean show, String pattern) {
        this.showEditLink = show;
        this.editUrlPattern = show ? pattern : null;
        return this;
    }

    public BibliosConfigBuilder withSourceLink(boolean show, String pattern) {
        this.showSourceLink = show;
        this.sourceUrlPattern = show ? pattern : null;
        return this;
    }

    public BibliosConfigBuilder withVersionSwitchMode(String mode) {
        this.versionSwitchMode = mode;
        return this;
    }

    public BibliosConfigBuilder withSearchLanguageMode(String mode) {
        this.searchLanguageMode = mode;
        return this;
    }

    public BibliosConfigBuilder withSidebarTocDepth(int depth) {
        this.sidebarTocDepth = depth;
        return this;
    }

    public BibliosConfigBuilder withContentToc(String mode) {
        this.contentToc = mode;
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

    public BibliosConfigBuilder withSinglePageSourceGitRepo(Path repoDir, String sourceId, String displayName,
                                                            String startPath, String defaultVersion, String masterFile,
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
              render_mode: single_page
              master_file: %s
            """.formatted(sourceId, displayName, repoDir.toString(), branchesYaml, startPath, defaultVersion, masterFile);

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

        StringBuilder uiSection = new StringBuilder();
        if (showEditLink || showSourceLink || versionSwitchMode != null || searchLanguageMode != null
            || sidebarTocDepth != null || contentToc != null) {
            uiSection.append("ui:\n");
            uiSection.append("  show_edit_link: %s\n".formatted(showEditLink));
            uiSection.append("  show_source_link: %s\n".formatted(showSourceLink));
            if (versionSwitchMode != null) {
                uiSection.append("  version_switch_mode: %s\n".formatted(versionSwitchMode));
            }
            if (searchLanguageMode != null) {
                uiSection.append("  search_language_mode: %s\n".formatted(searchLanguageMode));
            }
            if (sidebarTocDepth != null) {
                uiSection.append("  sidebar_toc_depth: %s\n".formatted(sidebarTocDepth));
            }
            if (contentToc != null) {
                uiSection.append("  content_toc: %s\n".formatted(contentToc));
            }
            if (editUrlPattern != null) {
                uiSection.append("  edit_url_pattern: \"%s\"\n".formatted(editUrlPattern));
            }
            if (sourceUrlPattern != null) {
                uiSection.append("  source_url_pattern: \"%s\"\n".formatted(sourceUrlPattern));
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
            %scontent:
              sources:
            %s""".formatted(siteTitle, siteUrl, siteLanguage, outputDir.toString(), clean,
                uiSection.toString(), sourcesYaml.toString().stripTrailing());

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
