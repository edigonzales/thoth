package guru.interlis.thoth.biblios.fixture;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper for creating biblios.yml configuration files for testing.
 */
public final class BibliosConfigBuilder {

    private String siteTitle = "Test Docs";
    private String siteUrl = "https://test.example.org";
    private String siteLanguage = "en";
    private String siteLogo = null;
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
    private String contentSectionNumbers = null;
    private Integer contentSectionNumberDepth = null;
    private String syntaxHighlightingMode = null;
    private final ArrayList<String> prismCustomComponents = new ArrayList<>();
    private boolean pdfEnabled = false;
    private final LinkedHashMap<String, Object> pdfAttributes = new LinkedHashMap<>();
    private boolean docxEnabled = false;
    private String docxReferenceDoc = null;
    private final LinkedHashMap<String, Object> docxFeatures = new LinkedHashMap<>();
    private final java.util.ArrayList<SourceEntry> sources = new java.util.ArrayList<>();

    public BibliosConfigBuilder withSiteTitle(String title) {
        this.siteTitle = title;
        return this;
    }

    public BibliosConfigBuilder withSiteUrl(String url) {
        this.siteUrl = url;
        return this;
    }

    public BibliosConfigBuilder withSiteLanguage(String language) {
        this.siteLanguage = language;
        return this;
    }

    public BibliosConfigBuilder withSiteLogo(String logo) {
        this.siteLogo = logo;
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

    public BibliosConfigBuilder withContentSectionNumbers(String mode) {
        this.contentSectionNumbers = mode;
        return this;
    }

    public BibliosConfigBuilder withContentSectionNumberDepth(int depth) {
        this.contentSectionNumberDepth = depth;
        return this;
    }

    public BibliosConfigBuilder withSyntaxHighlightingMode(String mode) {
        this.syntaxHighlightingMode = mode;
        return this;
    }

    public BibliosConfigBuilder withPrismCustomComponents(List<String> components) {
        this.prismCustomComponents.clear();
        if (components != null) {
            this.prismCustomComponents.addAll(components);
        }
        return this;
    }

    public BibliosConfigBuilder withPdfEnabled(boolean enabled) {
        this.pdfEnabled = enabled;
        return this;
    }

    public BibliosConfigBuilder withPdfAttributes(Map<String, Object> attributes) {
        this.pdfAttributes.clear();
        if (attributes != null) {
            this.pdfAttributes.putAll(attributes);
        }
        return this;
    }

    public BibliosConfigBuilder withDocxEnabled(boolean enabled) {
        this.docxEnabled = enabled;
        return this;
    }

    public BibliosConfigBuilder withDocxReferenceDoc(String referenceDoc) {
        this.docxReferenceDoc = referenceDoc;
        return this;
    }

    public BibliosConfigBuilder withDocxFeatures(Boolean titlePage, Boolean toc, Boolean changeLog) {
        this.docxFeatures.clear();
        if (titlePage != null) {
            this.docxFeatures.put("title_page", titlePage);
        }
        if (toc != null) {
            this.docxFeatures.put("toc", toc);
        }
        if (changeLog != null) {
            this.docxFeatures.put("change_log", changeLog);
        }
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
        return withSinglePageSourceGitRepoWithTocNumbers(
            repoDir, sourceId, displayName, startPath, defaultVersion, masterFile, null, branches
        );
    }

    public BibliosConfigBuilder withSinglePageSourceGitRepoWithTocNumbers(Path repoDir, String sourceId, String displayName,
                                                                           String startPath, String defaultVersion, String masterFile,
                                                                           String sidebarTocNumbers, String... branches) {
        StringBuilder branchesYaml = new StringBuilder();
        for (String branch : branches) {
            branchesYaml.append("          - name: %s\n".formatted(branch));
            branchesYaml.append("            display_version: %s\n".formatted(branch));
        }

        String sidebarTocNumbersYaml = "";
        if (sidebarTocNumbers != null && !sidebarTocNumbers.isBlank()) {
            sidebarTocNumbersYaml = "  sidebar_toc_numbers: %s\n".formatted(sidebarTocNumbers.trim());
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
            %s""".formatted(sourceId, displayName, repoDir.toString(), branchesYaml, startPath, defaultVersion,
            masterFile, sidebarTocNumbersYaml);

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
            || sidebarTocDepth != null || contentToc != null || contentSectionNumbers != null
            || contentSectionNumberDepth != null || syntaxHighlightingMode != null
            || !prismCustomComponents.isEmpty()) {
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
            if (contentSectionNumbers != null) {
                uiSection.append("  content_section_numbers: %s\n".formatted(contentSectionNumbers));
            }
            if (contentSectionNumberDepth != null) {
                uiSection.append("  content_section_number_depth: %s\n".formatted(contentSectionNumberDepth));
            }
            if (syntaxHighlightingMode != null) {
                uiSection.append("  syntax_highlighting: %s\n".formatted(syntaxHighlightingMode));
            }
            if (editUrlPattern != null) {
                uiSection.append("  edit_url_pattern: \"%s\"\n".formatted(editUrlPattern));
            }
            if (sourceUrlPattern != null) {
                uiSection.append("  source_url_pattern: \"%s\"\n".formatted(sourceUrlPattern));
            }
            if (!prismCustomComponents.isEmpty()) {
                uiSection.append("  prism_custom_components:\n");
                for (String path : prismCustomComponents) {
                    uiSection.append("    - \"%s\"\n".formatted(escapeYaml(path)));
                }
            }
        }

        StringBuilder pdfSection = new StringBuilder();
        if (pdfEnabled || !pdfAttributes.isEmpty()) {
            pdfSection.append("pdf:\n");
            pdfSection.append("  enabled: %s\n".formatted(pdfEnabled));
            if (!pdfAttributes.isEmpty()) {
                pdfSection.append("  attributes:\n");
                appendYamlMap(pdfSection, 4, pdfAttributes);
            }
        }

        StringBuilder docxSection = new StringBuilder();
        if (docxEnabled || docxReferenceDoc != null || !docxFeatures.isEmpty()) {
            docxSection.append("docx:\n");
            docxSection.append("  enabled: %s\n".formatted(docxEnabled));
            if (docxReferenceDoc != null && !docxReferenceDoc.isBlank()) {
                docxSection.append("  reference_doc: \"%s\"\n".formatted(escapeYaml(docxReferenceDoc)));
            }
            if (!docxFeatures.isEmpty()) {
                docxSection.append("  features:\n");
                appendYamlMap(docxSection, 4, docxFeatures);
            }
        }

        String yaml = """
            site:
              title: %s
              url: %s
              default_language: %s
            %s
            output:
              dir: %s
              clean: %s
            %s%s%scontent:
              sources:
            %s""".formatted(siteTitle, siteUrl, siteLanguage, siteLogoYaml(), outputDir.toString(), clean,
                uiSection.toString(), pdfSection.toString(), docxSection.toString(), sourcesYaml.toString().stripTrailing());

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

    private String siteLogoYaml() {
        if (siteLogo == null || siteLogo.isBlank()) {
            return "";
        }
        return "  logo: \"%s\"\n".formatted(escapeYaml(siteLogo));
    }

    private String escapeYaml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void appendYamlMap(StringBuilder builder, int indent, Map<String, Object> values) {
        String prefix = " ".repeat(indent);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            appendYamlValue(builder, prefix, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendYamlValue(StringBuilder builder, String prefix, String key, Object value) {
        if (value instanceof List<?> list) {
            builder.append(prefix).append(key).append(":\n");
            for (Object item : list) {
                builder.append(prefix).append("  - ").append(formatYamlScalar(item)).append("\n");
            }
            return;
        }
        if (value instanceof Map<?, ?> nested) {
            builder.append(prefix).append(key).append(":\n");
            appendYamlMap(builder, prefix.length() + 2, (Map<String, Object>) nested);
            return;
        }
        builder.append(prefix).append(key).append(": ").append(formatYamlScalar(value)).append("\n");
    }

    private String formatYamlScalar(Object value) {
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        return "\"%s\"".formatted(escapeYaml(String.valueOf(value)));
    }
}
