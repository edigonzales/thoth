package guru.interlis.thoth.biblios.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Source-specific PDF output configuration.
 */
public final class SourcePdfSection {
    private final Boolean enabled;
    private final String masterFile;
    private final List<String> requires;
    private final Map<String, Object> attributes;

    public SourcePdfSection(Boolean enabled, String masterFile, List<String> requires, Map<String, Object> attributes) {
        this.enabled = enabled;
        this.masterFile = masterFile != null && !masterFile.isBlank() ? masterFile.trim() : null;
        this.requires = requires != null ? List.copyOf(requires) : List.of();
        this.attributes = attributes != null ? Map.copyOf(new LinkedHashMap<>(attributes)) : Map.of();
    }

    public Boolean enabled() {
        return enabled;
    }

    public String masterFile() {
        return masterFile;
    }

    public List<String> requires() {
        return requires;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "SourcePdfSection{enabled=" + enabled + ", masterFile='" + masterFile + "', requires=" + requires.size() + ", attributes=" + attributes.size() + "}";
    }
}
