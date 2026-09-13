package guru.interlis.thoth.biblios.config;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** A presentation group referencing documentation source IDs. */
public record ContentGroup(String title, List<String> sources) {
    public ContentGroup {
        Objects.requireNonNull(title, "group.title is required");
        if (title.isBlank()) {
            throw new IllegalArgumentException("group.title must not be blank");
        }
        title = title.trim();
        sources = List.copyOf(sources);
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Group '" + title + "' must contain sources");
        }
        var seen = new HashSet<String>();
        for (String id : sources) {
            if (id.isBlank() || !seen.add(id)) {
                throw new IllegalArgumentException("Group '" + title + "' has a blank or duplicate source ID: " + id);
            }
        }
    }
}
