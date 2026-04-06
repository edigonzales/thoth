package guru.interlis.thoth.biblios.config;

import guru.interlis.thoth.core.ThothBuildException;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
            return parseMap(map);
        }
    }

    @SuppressWarnings("unchecked")
    private BibliosConfig parseMap(Map<?, ?> map) {
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
            getBoolean(uiMap, "show_edit_link", false)
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
                sources.add(parseSource(sourceMap, sourceLabel));
            } catch (ThothBuildException e) {
                throw new ThothBuildException(
                    "Error in " + sourceLabel + ": " + e.getMessage(),
                    ThothBuildException.ErrorSeverity.FATAL,
                    "config"
                );
            }
        }

        ContentSection content = new ContentSection(sources);

        return new BibliosConfig(site, output, ui, content);
    }

    @SuppressWarnings("unchecked")
    private SourceConfig parseSource(Map<String, Object> sourceMap, String label) {
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

        NavigationConfig navigation = null;
        if (sourceMap.containsKey("navigation")) {
            Map<String, Object> navMap = castMap(sourceMap.get("navigation"), label + ".navigation");
            String navFile = getString(navMap, "file");
            navigation = new NavigationConfig(navFile);
        }

        return new SourceConfig(id, displayName, url, branches, startPath, defaultVersion, navigation, startPage);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value, String label) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected mapping for " + label + ", got: " + value.getClass().getSimpleName());
        }
        return (Map<String, Object>) value;
    }
}
