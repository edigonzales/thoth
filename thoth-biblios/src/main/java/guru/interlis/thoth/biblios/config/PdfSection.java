package guru.interlis.thoth.biblios.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global PDF output configuration.
 */
public final class PdfSection {
    private final boolean enabled;
    private final Map<String, Object> attributes;

    public PdfSection(boolean enabled, Map<String, Object> attributes) {
        this.enabled = enabled;
        this.attributes = attributes != null ? Map.copyOf(new LinkedHashMap<>(attributes)) : Map.of();
    }

    public boolean enabled() {
        return enabled;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "PdfSection{enabled=" + enabled + ", attributes=" + attributes.size() + "}";
    }
}
