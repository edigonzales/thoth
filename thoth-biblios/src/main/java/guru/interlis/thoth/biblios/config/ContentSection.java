package guru.interlis.thoth.biblios.config;

import java.util.List;
import java.util.Objects;

/**
 * Content sources configuration.
 */
public final class ContentSection {
    private final List<SourceConfig> sources;

    public ContentSection(List<SourceConfig> sources) {
        Objects.requireNonNull(sources, "content.sources is required");
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("content.sources must not be empty");
        }
        this.sources = List.copyOf(sources);
    }

    public List<SourceConfig> sources() {
        return sources;
    }

    @Override
    public String toString() {
        return "ContentSection{sources=" + sources.size() + "}";
    }
}
