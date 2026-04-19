package guru.interlis.thoth.biblios.config;

import guru.interlis.thoth.core.ThothBuildException;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parses biblios.yml into a BibliosConfig object.
 * Uses SnakeYAML Engine for YAML parsing.
 */
public final class BibliosConfigParser {

    private final Load yaml;

    public BibliosConfigParser() {
        this.yaml = new Load(LoadSettings.builder().build());
    }

    /**
     * Parse biblios.yml from the given path.
     *
     * @param configPath path to biblios.yml
     * @return parsed configuration
     * @throws IOException if file cannot be read
     * @throws ThothBuildException if config is invalid
     */
    public BibliosConfig parse(Path configPath) throws IOException {
        Objects.requireNonNull(configPath, "configPath must not be null");
        if (!Files.exists(configPath)) {
            throw new ThothBuildException(
                "Configuration file not found: " + configPath + "\n" +
                "Usage: thoth-biblios build --config <path-to-biblios.yml>",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (!Files.isRegularFile(configPath)) {
            throw new ThothBuildException(
                "Configuration path is not a regular file: " + configPath,
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Object loaded;
            try {
                loaded = yaml.loadFromReader(reader);
            } catch (Exception e) {
                throw new ThothBuildException(
                    "Failed to parse YAML: " + e.getMessage() + "\n" +
                    "Please check the syntax of your biblios.yml file.",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ThothBuildException(
                    "Configuration file must be a YAML mapping (key: value pairs)",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            return parseMap(map, configPath);
        }
    }

    @SuppressWarnings("unchecked")
    private BibliosConfig parseMap(Map<?, ?> map, Path configPath) {
        Map<String, Object> root = castMap(map, "root configuration");

        // Validate required top-level sections
        for (String key : new String[]{"site", "output", "content"}) {
            if (!root.containsKey(key)) {
                throw new ThothBuildException(
                    "Missing required configuration section: '" + key + "'\n" +
                    "Expected sections: site, output, content",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
        }

        // Parse site section
        Map<String, Object> siteMap = getMap(root, "site");
        SiteSection site = new SiteSection(
            getString(siteMap, "title"),
            (String) siteMap.get("url"),
            parseSiteLogo(siteMap, configPath),
            (String) siteMap.get("default_language"),
            (String) siteMap.get("default_component"),
            (String) siteMap.get("default_version")
        );

        // Parse output section
        Map<String, Object> outputMap = getMap(root, "output");
        OutputSection output = new OutputSection(
            getString(outputMap, "dir"),
            getBoolean(outputMap, "clean", false)
        );

        // Parse ui section
        Map<String, Object> uiMap = root.containsKey("ui") ? getMap(root, "ui") : Map.of();
        UiSection ui = new UiSection(
            (String) uiMap.get("theme"),
            getBoolean(uiMap, "show_version_badge", false),
            getBoolean(uiMap, "show_edit_link", false),
            getBoolean(uiMap, "show_source_link", false),
            (String) uiMap.get("edit_url_pattern"),
            (String) uiMap.get("source_url_pattern"),
            parseVersionSwitchMode(uiMap),
            parseSearchLanguageMode(uiMap),
            parseSidebarTocDepth(uiMap),
            parseContentTocMode(uiMap),
            parseSyntaxHighlightingMode(uiMap),
            parsePrismCustomComponents(uiMap, configPath)
        );

        Map<String, Object> pdfMap = root.containsKey("pdf") ? getMap(root, "pdf") : Map.of();
        PdfSection pdf = new PdfSection(
            getBoolean(pdfMap, "enabled", false),
            parsePdfAttributes(pdfMap, configPath, "pdf.attributes")
        );
        Map<String, Object> docxMap = root.containsKey("docx") ? getMap(root, "docx") : Map.of();
        DocxSection docx = new DocxSection(
            getBoolean(docxMap, "enabled", false),
            parseDocxReferenceDoc(docxMap, configPath, "docx.reference_doc"),
            parseDocxFeatures(docxMap, "docx.features")
        );

        // Parse content section
        Map<String, Object> contentMap = getMap(root, "content");
        List<Map<String, Object>> sourcesList = getList(contentMap, "sources");
        if (sourcesList.isEmpty()) {
            throw new ThothBuildException(
                "content.sources must not be empty\n" +
                "At least one documentation source is required.",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        List<SourceConfig> sources = new ArrayList<>();
        for (int i = 0; i < sourcesList.size(); i++) {
            Map<String, Object> sourceMap = sourcesList.get(i);
            String sourceLabel = "content.sources[" + i + "]";
            try {
                sources.add(parseSource(sourceMap, sourceLabel, configPath));
            } catch (ThothBuildException e) {
                throw new ThothBuildException(
                    "Error in " + sourceLabel + ": " + e.getMessage(),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
        }

        ContentSection content = new ContentSection(sources);

        return new BibliosConfig(site, output, ui, pdf, docx, content);
    }

    private String parseSiteLogo(Map<String, Object> siteMap, Path configPath) {
        Object logoValue = siteMap.get("logo");
        if (logoValue == null) {
            return null;
        }
        if (!(logoValue instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for 'site.logo', got: " + logoValue.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        String logo = text.trim();
        if (logo.isEmpty()) {
            throw new ThothBuildException(
                "Configuration 'site.logo' must not be blank",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        if (isRemoteLogoReference(logo)) {
            return logo;
        }

        Path logoPath = resolveLocalLogoPath(logo, configPath);
        if (!Files.exists(logoPath) || !Files.isRegularFile(logoPath)) {
            throw new ThothBuildException(
                "Invalid 'site.logo': local file not found: " + logoPath,
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return logoPath.toAbsolutePath().normalize().toString();
    }

    private boolean isRemoteLogoReference(String logo) {
        try {
            URI uri = new URI(logo);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String normalized = scheme.toLowerCase();
            return normalized.equals("http") || normalized.equals("https") || normalized.equals("data");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private Path resolveLocalLogoPath(String logo, Path configPath) {
        try {
            URI uri = new URI(logo);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Path.of(uri).toAbsolutePath().normalize();
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            // Fall through and treat as regular path string.
        }

        Path logoPath = Path.of(logo);
        if (!logoPath.isAbsolute()) {
            Path configDir = configPath.toAbsolutePath().getParent();
            if (configDir != null) {
                logoPath = configDir.resolve(logoPath);
            }
        }
        return logoPath.toAbsolutePath().normalize();
    }

    @SuppressWarnings("unchecked")
    private SourceConfig parseSource(Map<String, Object> sourceMap, String label, Path configPath) {
        String id = getString(sourceMap, "id");
        String displayName = getString(sourceMap, "display_name");
        String url = getString(sourceMap, "url");

        List<Map<String, Object>> branchesList = getList(sourceMap, "branches");
        if (branchesList.isEmpty()) {
            throw new IllegalArgumentException(label + ".branches must not be empty");
        }

        List<BranchConfig> branches = new ArrayList<>();
        for (int i = 0; i < branchesList.size(); i++) {
            Map<String, Object> branchMap = branchesList.get(i);
            branches.add(new BranchConfig(
                getString(branchMap, "name"),
                (String) branchMap.get("display_version")
            ));
        }

        String startPath = (String) sourceMap.get("start_path");
        String defaultVersion = (String) sourceMap.get("default_version");
        String startPage = (String) sourceMap.get("start_page");
        RenderMode renderMode = parseRenderMode(sourceMap, label);
        SidebarTocNumbersMode sidebarTocNumbers = parseSidebarTocNumbersMode(sourceMap, label);
        SourcePdfSection pdf = parseSourcePdfSection(sourceMap, configPath, label);
        SourceDocxSection docx = parseSourceDocxSection(sourceMap, configPath, label);
        String masterFile = null;
        Object masterFileValue = sourceMap.get("master_file");
        if (masterFileValue != null) {
            if (!(masterFileValue instanceof String text)) {
                throw new ThothBuildException(
                    "Expected string for '" + label + ".master_file', got: " + masterFileValue.getClass().getSimpleName(),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            masterFile = text;
        }

        NavigationConfig navigation = null;
        if (sourceMap.containsKey("navigation")) {
            Map<String, Object> navMap = castMap(sourceMap.get("navigation"), label + ".navigation");
            String navFile = getString(navMap, "file");
            navigation = new NavigationConfig(navFile);
        }

        if (renderMode == RenderMode.SINGLE_PAGE && (masterFile == null || masterFile.isBlank())) {
            throw new ThothBuildException(
                label + ".master_file is required when render_mode is single_page",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        return new SourceConfig(
            id,
            displayName,
            url,
            branches,
            startPath,
            defaultVersion,
            navigation,
            startPage,
            renderMode,
            masterFile,
            sidebarTocNumbers,
            pdf,
            docx
        );
    }

    private SourcePdfSection parseSourcePdfSection(Map<String, Object> sourceMap, Path configPath, String label) {
        if (!sourceMap.containsKey("pdf")) {
            return null;
        }

        Map<String, Object> pdfMap = castMap(sourceMap.get("pdf"), label + ".pdf");
        Boolean enabled = null;
        if (pdfMap.containsKey("enabled")) {
            Object enabledValue = pdfMap.get("enabled");
            if (!(enabledValue instanceof Boolean)) {
                throw new ThothBuildException(
                    "Expected boolean for '" + label + ".pdf.enabled', got: " +
                        (enabledValue == null ? "null" : enabledValue.getClass().getSimpleName()),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            enabled = (Boolean) enabledValue;
        }

        String masterFile = null;
        if (pdfMap.containsKey("master_file")) {
            Object masterFileValue = pdfMap.get("master_file");
            if (!(masterFileValue instanceof String text)) {
                throw new ThothBuildException(
                    "Expected string for '" + label + ".pdf.master_file', got: " +
                        (masterFileValue == null ? "null" : masterFileValue.getClass().getSimpleName()),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            if (text.isBlank()) {
                throw new ThothBuildException(
                    "Configuration '" + label + ".pdf.master_file' must not be blank",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            masterFile = text.trim();
        }

        return new SourcePdfSection(
            enabled,
            masterFile,
            parsePdfAttributes(pdfMap, configPath, label + ".pdf.attributes")
        );
    }

    private SourceDocxSection parseSourceDocxSection(Map<String, Object> sourceMap, Path configPath, String label) {
        if (!sourceMap.containsKey("docx")) {
            return null;
        }

        Map<String, Object> docxMap = castMap(sourceMap.get("docx"), label + ".docx");
        Boolean enabled = parseOptionalBoolean(docxMap, "enabled", label + ".docx.enabled");
        String masterFile = parseOptionalTrimmedString(docxMap, "master_file", label + ".docx.master_file");
        String referenceDoc = parseDocxReferenceDoc(docxMap, configPath, label + ".docx.reference_doc");
        SourceDocxFeaturesSection features = parseSourceDocxFeatures(docxMap, label + ".docx.features");

        return new SourceDocxSection(enabled, masterFile, referenceDoc, features);
    }

    private Boolean parseOptionalBoolean(Map<String, Object> map, String key, String label) {
        if (!map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (!(value instanceof Boolean flag)) {
            throw new ThothBuildException(
                "Expected boolean for '" + label + "', got: " +
                    (value == null ? "null" : value.getClass().getSimpleName()),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return flag;
    }

    private String parseOptionalTrimmedString(Map<String, Object> map, String key, String label) {
        if (!map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for '" + label + "', got: " +
                    (value == null ? "null" : value.getClass().getSimpleName()),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (text.isBlank()) {
            throw new ThothBuildException(
                "Configuration '" + label + "' must not be blank",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return text.trim();
    }

    private String parseDocxReferenceDoc(Map<String, Object> map, Path configPath, String label) {
        String raw = parseOptionalTrimmedString(map, "reference_doc", label);
        if (raw == null) {
            return null;
        }
        Path resolved = resolveLocalPath(raw, configPath);
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new ThothBuildException(
                "Invalid '" + label + "': file not found: " + resolved,
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        String lower = resolved.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!lower.endsWith(".docx")) {
            throw new ThothBuildException(
                "Invalid '" + label + "': expected .docx file, got: " + resolved.getFileName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return resolved.toAbsolutePath().normalize().toString();
    }

    private DocxFeaturesSection parseDocxFeatures(Map<String, Object> map, String label) {
        if (!map.containsKey("features")) {
            return DocxFeaturesSection.defaults();
        }
        Map<String, Object> featuresMap = castMap(map.get("features"), label);
        boolean titlePage = getBoolean(featuresMap, "title_page", false);
        boolean toc = getBoolean(featuresMap, "toc", true);
        boolean changeLog = getBoolean(featuresMap, "change_log", false);
        return new DocxFeaturesSection(titlePage, toc, changeLog);
    }

    private SourceDocxFeaturesSection parseSourceDocxFeatures(Map<String, Object> map, String label) {
        if (!map.containsKey("features")) {
            return null;
        }
        Map<String, Object> featuresMap = castMap(map.get("features"), label);
        return new SourceDocxFeaturesSection(
            parseOptionalBoolean(featuresMap, "title_page", label + ".title_page"),
            parseOptionalBoolean(featuresMap, "toc", label + ".toc"),
            parseOptionalBoolean(featuresMap, "change_log", label + ".change_log")
        );
    }

    // Helper methods for safe type casting and extraction

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            throw new ThothBuildException(
                "Missing required configuration: '" + key + "'",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new ThothBuildException(
                "Expected mapping for '" + key + "', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            throw new ThothBuildException(
                "Missing required configuration: '" + key + "'",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (!(value instanceof List<?>)) {
            throw new ThothBuildException(
                "Expected list for '" + key + "', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map<?, ?>)) {
                throw new ThothBuildException(
                    "Expected mapping in list '" + key + "', got: " + item.getClass().getSimpleName(),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            result.add((Map<String, Object>) item);
        }
        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new ThothBuildException(
                "Missing required configuration: '" + key + "'",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (!(value instanceof String str)) {
            throw new ThothBuildException(
                "Expected string for '" + key + "', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (str.isBlank()) {
            throw new ThothBuildException(
                "Configuration '" + key + "' must not be blank",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return str.trim();
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private VersionSwitchMode parseVersionSwitchMode(Map<String, Object> map) {
        Object value = map.get("version_switch_mode");
        if (value == null) {
            return VersionSwitchMode.START_PAGE;
        }
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for 'ui.version_switch_mode', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        try {
            return VersionSwitchMode.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ThothBuildException(
                e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
    }

    private SearchLanguageMode parseSearchLanguageMode(Map<String, Object> map) {
        Object value = map.get("search_language_mode");
        if (value == null) {
            return SearchLanguageMode.MULTILINGUAL_SAFE;
        }
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for 'ui.search_language_mode', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        try {
            return SearchLanguageMode.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ThothBuildException(
                e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
    }

    private int parseSidebarTocDepth(Map<String, Object> map) {
        Object value = map.get("sidebar_toc_depth");
        if (value == null) {
            return 2;
        }
        final int depth;
        if (value instanceof Number number) {
            depth = number.intValue();
        } else {
            try {
                depth = Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException e) {
                throw new ThothBuildException(
                    "Expected integer for 'ui.sidebar_toc_depth', got: " + value,
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
        }

        if (depth < 1 || depth > 6) {
            throw new ThothBuildException(
                "Invalid ui.sidebar_toc_depth: " + depth + ". Allowed range: 1..6",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return depth;
    }

    private ContentTocMode parseContentTocMode(Map<String, Object> map) {
        Object value = map.get("content_toc");
        if (value == null) {
            return ContentTocMode.OFF;
        }
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for 'ui.content_toc', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        try {
            return ContentTocMode.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ThothBuildException(
                e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
    }

    private SyntaxHighlightingMode parseSyntaxHighlightingMode(Map<String, Object> map) {
        Object value = map.get("syntax_highlighting");
        if (value == null) {
            return SyntaxHighlightingMode.PRISM;
        }
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for 'ui.syntax_highlighting', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        try {
            return SyntaxHighlightingMode.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ThothBuildException(
                e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
    }

    private List<String> parsePrismCustomComponents(Map<String, Object> map, Path configPath) {
        Object value = map.get("prism_custom_components");
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new ThothBuildException(
                "Expected list for 'ui.prism_custom_components', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        List<String> resolvedPaths = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            Object item = rawList.get(i);
            String key = "ui.prism_custom_components[" + i + "]";
            if (!(item instanceof String text)) {
                throw new ThothBuildException(
                    "Expected string for '" + key + "', got: " + (item == null ? "null" : item.getClass().getSimpleName()),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            String rawPath = text.trim();
            if (rawPath.isEmpty()) {
                throw new ThothBuildException(
                    "Configuration '" + key + "' must not be blank",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            Path resolved = resolveLocalPath(rawPath, configPath);
            if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
                throw new ThothBuildException(
                    "Invalid '" + key + "': file not found: " + resolved,
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            if (!resolved.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".js")) {
                throw new ThothBuildException(
                    "Invalid '" + key + "': expected a .js file, got: " + resolved.getFileName(),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            resolvedPaths.add(resolved.toAbsolutePath().normalize().toString());
        }
        return List.copyOf(resolvedPaths);
    }

    private Path resolveLocalPath(String rawPath, Path configPath) {
        try {
            URI uri = new URI(rawPath);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Path.of(uri).toAbsolutePath().normalize();
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            // Fall through and treat as regular path string.
        }

        Path path = Path.of(rawPath);
        if (!path.isAbsolute()) {
            Path configDir = configPath.toAbsolutePath().getParent();
            if (configDir != null) {
                path = configDir.resolve(path);
            }
        }
        return path.toAbsolutePath().normalize();
    }

    private Map<String, Object> parsePdfAttributes(Map<String, Object> parent, Path configPath, String label) {
        Object value = parent.get("attributes");
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new ThothBuildException(
                "Expected mapping for '" + label + "', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String rawKey)) {
                throw new ThothBuildException(
                    "Expected string key in '" + label + "', got: " +
                        (entry.getKey() == null ? "null" : entry.getKey().getClass().getSimpleName()),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            String key = rawKey.trim();
            if (key.isEmpty()) {
                throw new ThothBuildException(
                    "PDF attribute keys in '" + label + "' must not be blank",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            resolved.put(key, normalizePdfAttributeValue(key, entry.getValue(), configPath, label + "." + key));
        }
        return Map.copyOf(resolved);
    }

    private Object normalizePdfAttributeValue(String key, Object value, Path configPath, String label) {
        return switch (key) {
            case "pdf-theme" -> normalizePdfTheme(value, configPath, label);
            case "pdf-themesdir" -> normalizePdfDirectoryValue(value, configPath, label);
            case "pdf-fontsdir" -> normalizePdfFontsDir(value, configPath, label);
            default -> normalizeGenericPdfAttributeValue(value, label);
        };
    }

    private Object normalizePdfTheme(Object value, Path configPath, String label) {
        String raw = requirePdfString(value, label);
        if (raw.startsWith("uri:classloader:")) {
            return raw;
        }
        if (!looksLikePath(raw)) {
            return raw;
        }

        Path resolved = resolveLocalPath(raw, configPath);
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new ThothBuildException(
                "Invalid '" + label + "': file not found: " + resolved,
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return resolved.toString();
    }

    private Object normalizePdfDirectoryValue(Object value, Path configPath, String label) {
        String raw = requirePdfString(value, label);
        if (raw.startsWith("uri:classloader:")) {
            return raw;
        }
        Path resolved = resolveLocalPath(raw, configPath);
        if (!Files.exists(resolved) || !Files.isDirectory(resolved)) {
            throw new ThothBuildException(
                "Invalid '" + label + "': directory not found: " + resolved,
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return resolved.toString();
    }

    private Object normalizePdfFontsDir(Object value, Path configPath, String label) {
        List<String> tokens = new ArrayList<>();
        if (value instanceof List<?> rawList) {
            if (rawList.isEmpty()) {
                throw new ThothBuildException(
                    "Configuration '" + label + "' must not be empty",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            for (int i = 0; i < rawList.size(); i++) {
                tokens.addAll(splitPdfFontTokens(requirePdfString(rawList.get(i), label + "[" + i + "]")));
            }
        } else {
            tokens.addAll(splitPdfFontTokens(requirePdfString(value, label)));
        }

        if (tokens.isEmpty()) {
            throw new ThothBuildException(
                "Configuration '" + label + "' must not be empty",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }

        List<String> resolvedTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.equals("GEM_FONTS_DIR") || token.startsWith("uri:classloader:")) {
                resolvedTokens.add(token);
                continue;
            }
            Path resolved = resolveLocalPath(token, configPath);
            if (!Files.exists(resolved) || !Files.isDirectory(resolved)) {
                throw new ThothBuildException(
                    "Invalid '" + label + "': directory not found: " + resolved,
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            resolvedTokens.add(resolved.toString());
        }
        return String.join(";", resolvedTokens);
    }

    private Object normalizeGenericPdfAttributeValue(Object value, String label) {
        if (value == null) {
            throw new ThothBuildException(
                "Configuration '" + label + "' must not be null",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (value instanceof String text) {
            if (text.isBlank()) {
                throw new ThothBuildException(
                    "Configuration '" + label + "' must not be blank",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            return text.trim();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof List<?> rawList) {
            List<String> items = new ArrayList<>();
            for (int i = 0; i < rawList.size(); i++) {
                items.add(requirePdfString(rawList.get(i), label + "[" + i + "]"));
            }
            if (items.isEmpty()) {
                throw new ThothBuildException(
                    "Configuration '" + label + "' must not be empty",
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
            return String.join(",", items);
        }
        throw new ThothBuildException(
            "Expected scalar or list for '" + label + "', got: " + value.getClass().getSimpleName(),
            ThothBuildException.ErrorSeverity.FATAL,
            "config"
        );
    }

    private String requirePdfString(Object value, String label) {
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for '" + label + "', got: " +
                    (value == null ? "null" : value.getClass().getSimpleName()),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        if (text.isBlank()) {
            throw new ThothBuildException(
                "Configuration '" + label + "' must not be blank",
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        return text.trim();
    }

    private List<String> splitPdfFontTokens(String raw) {
        String[] parts = raw.split("[,;]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String token = part.trim();
            if (!token.isEmpty()) {
                result.add(token);
            }
        }
        return result;
    }

    private boolean looksLikePath(String value) {
        String normalized = value.trim();
        return normalized.startsWith(".")
            || normalized.startsWith("/")
            || normalized.startsWith("file:")
            || normalized.contains("/")
            || normalized.contains("\\")
            || normalized.endsWith(".yml")
            || normalized.endsWith(".yaml");
    }

    private RenderMode parseRenderMode(Map<String, Object> sourceMap, String label) {
        Object value = sourceMap.get("render_mode");
        if (value == null) {
            return RenderMode.SPLIT;
        }
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for '" + label + ".render_mode', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        try {
            return RenderMode.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ThothBuildException(
                e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
    }

    private SidebarTocNumbersMode parseSidebarTocNumbersMode(Map<String, Object> sourceMap, String label) {
        Object value = sourceMap.get("sidebar_toc_numbers");
        if (value == null) {
            return SidebarTocNumbersMode.OFF;
        }
        if (!(value instanceof String text)) {
            throw new ThothBuildException(
                "Expected string for '" + label + ".sidebar_toc_numbers', got: " + value.getClass().getSimpleName(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
        try {
            return SidebarTocNumbersMode.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ThothBuildException(
                e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "config"
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value, String label) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected mapping for " + label + ", got: " + value.getClass().getSimpleName());
        }
        return (Map<String, Object>) value;
    }
}
