package guru.interlis.thoth.biblios.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global PDF output configuration.
 */
public final class PdfSection {
    private final boolean enabled;
    private final List<String> requires;
    private final Map<String, Object> attributes;

    public PdfSection(boolean enabled, List<String> requires, Map<String, Object> attributes) {
        this.enabled = enabled;
        this.requires = requires != null ? List.copyOf(requires) : List.of();
        this.attributes = attributes != null ? Map.copyOf(new LinkedHashMap<>(attributes)) : Map.of();
    }

    public boolean enabled() {
        return enabled;
    }

    public List<String> requires() {
        return requires;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "PdfSection{enabled=" + enabled + ", requires=" + requires.size() + ", attributes=" + attributes.size() + "}";
    }
}
