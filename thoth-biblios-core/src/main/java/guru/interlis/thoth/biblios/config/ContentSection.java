package guru.interlis.thoth.biblios.config;

import java.util.List;
import java.util.Objects;

/**
 * Content sources configuration.
 */
public final class ContentSection {
    private final List<SourceConfig> sources;
    private final List<ContentGroup> groups;

    public ContentSection(List<SourceConfig> sources) {
        this(sources, List.of());
    }

    public ContentSection(List<SourceConfig> sources, List<ContentGroup> groups) {
        Objects.requireNonNull(sources, "content.sources is required");
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("content.sources must not be empty");
        }
        this.sources = List.copyOf(sources);
        this.groups = List.copyOf(groups);
        var ids = sources.stream().map(SourceConfig::id).collect(java.util.stream.Collectors.toSet());
        for (ContentGroup group : groups) {
            for (String id : group.sources()) {
                if (!ids.contains(id)) {
                    throw new IllegalArgumentException("Group '" + group.title() + "' references unknown source: " + id);
                }
            }
        }
    }

    public List<SourceConfig> sources() {
        return sources;
    }

    public List<ContentGroup> groups() {
        return groups;
    }

    @Override
    public String toString() {
        return "ContentSection{sources=" + sources.size() + "}";
    }
}
