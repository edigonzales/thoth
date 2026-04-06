package guru.interlis.thoth.biblios.nav;

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
 * Parses nav.yml into a NavTree.
 */
public final class NavParser {

    private final Load yaml;

    public NavParser() {
        this.yaml = new Load(LoadSettings.builder().build());
    }

    /**
     * Parse nav.yml from the given path.
     *
     * @param navPath path to nav.yml
     * @return parsed navigation tree
     * @throws IOException if file cannot be read
     * @throws ThothBuildException if nav file is invalid
     */
    public NavTree parse(Path navPath) throws IOException {
        Objects.requireNonNull(navPath, "navPath must not be null");
        if (!Files.exists(navPath)) {
            throw new ThothBuildException(
                "Navigation file not found: " + navPath + "\n" +
                "Check that the 'navigation.file' in biblios.yml points to the correct path\n" +
                "and that the file exists in the documentation source's 'start_path' directory.",
                ThothBuildException.ErrorSeverity.ERROR,
                "navigation"
            );
        }

        try (Reader reader = Files.newBufferedReader(navPath)) {
            Object loaded;
            try {
                loaded = yaml.loadFromReader(reader);
            } catch (Exception e) {
                throw new ThothBuildException(
                    "Failed to parse navigation YAML: " + e.getMessage() + "\n" +
                    "File: " + navPath + "\n" +
                    "Check the nav.yml syntax. Expected format:\n" +
                    "  items:\n" +
                    "    - title: Page Title\n" +
                    "      page: page.adoc",
                    ThothBuildException.ErrorSeverity.ERROR,
                    "navigation"
                );
            }
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ThothBuildException(
                    "Navigation file must be a YAML mapping (expected top-level 'items:' key)",
                    ThothBuildException.ErrorSeverity.ERROR,
                    "navigation"
                );
            }
            return parseMap(map);
        }
    }

    /**
     * Parse nav.yml from a YAML string.
     */
    public NavTree parseString(String yamlContent) {
        Objects.requireNonNull(yamlContent, "yamlContent must not be null");
        Object loaded = yaml.loadFromString(yamlContent);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Navigation content must be a YAML mapping");
        }
        return parseMap(map);
    }

    @SuppressWarnings("unchecked")
    private NavTree parseMap(Map<?, ?> map) {
        Map<String, Object> root = castMap(map, "nav root");
        List<Map<String, Object>> itemsList = getList(root, "items");

        List<NavItem> items = new ArrayList<>();
        for (int i = 0; i < itemsList.size(); i++) {
            items.add(parseItem(itemsList.get(i), "items[" + i + "]"));
        }

        return new NavTree(items);
    }

    @SuppressWarnings("unchecked")
    private NavItem parseItem(Map<String, Object> itemMap, String label) {
        String title = getString(itemMap, "title", label);
        String page = (String) itemMap.get("page");

        List<NavItem> children = List.of();
        if (itemMap.containsKey("children")) {
            List<Map<String, Object>> childrenList = getList(itemMap, "children");
            children = new ArrayList<>();
            for (int i = 0; i < childrenList.size(); i++) {
                children.add(parseItem(childrenList.get(i), label + ".children[" + i + "]"));
            }
        }

        if (page == null && children.isEmpty()) {
            throw new ThothBuildException(
                "Navigation item " + label + " ('" + title + "') must have either 'page' or 'children'\n" +
                "  - 'page': path to an .adoc file (e.g., 'index.adoc')\n" +
                "  - 'children': a nested list of nav entries",
                ThothBuildException.ErrorSeverity.ERROR,
                "navigation"
            );
        }

        if (page != null && page.isBlank()) {
            throw new ThothBuildException(
                "Navigation item " + label + " ('" + title + "') has an empty 'page' value\n" +
                "  Expected a path to an .adoc file, e.g., 'index.adoc'",
                ThothBuildException.ErrorSeverity.ERROR,
                "navigation"
            );
        }

        return new NavItem(title, page, children);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value, String label) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected mapping for " + label);
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("Expected list for '" + key + "'");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Expected mapping in list '" + key + "'");
            }
            result.add((Map<String, Object>) item);
        }
        return result;
    }

    private String getString(Map<String, Object> map, String key, String label) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required '" + key + "' in " + label);
        }
        return value.toString().trim();
    }
}
