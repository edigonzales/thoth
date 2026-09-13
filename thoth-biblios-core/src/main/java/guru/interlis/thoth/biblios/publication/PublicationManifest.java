package guru.interlis.thoth.biblios.publication;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The publication manifest: the authoritative mapping from portal paths to
 * documentation sources. The server only serves paths listed here; anything
 * unknown or marked {@link PublicationScope#INTERNAL} is not served.
 *
 * <p>The JSON representation is deliberately small and stable:</p>
 * <pre>
 * {
 *   "version": 1,
 *   "entries": [
 *     {"path": "site-assets/styles.css", "scope": "shared", "kind": "file"},
 *     {"path": "agi-betrieb/main/handbuch/", "scope": "source", "source": "agi-betrieb", "kind": "page"}
 *   ]
 * }
 * </pre>
 */
public final class PublicationManifest {
    public static final int FORMAT_VERSION = 1;

    private final Map<String, ManifestEntry> entries = new TreeMap<>();

    public void add(ManifestEntry entry) {
        entries.put(entry.path(), entry);
    }

    public Optional<ManifestEntry> find(String path) {
        if (path == null) {
            return Optional.empty();
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return Optional.ofNullable(entries.get(normalized));
    }

    public Collection<ManifestEntry> entries() {
        return entries.values();
    }

    public int size() {
        return entries.size();
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": ").append(FORMAT_VERSION).append(",\n");
        json.append("  \"entries\": [\n");
        boolean first = true;
        for (ManifestEntry entry : entries.values()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("    {\"path\": \"").append(escape(entry.path())).append("\"")
                .append(", \"scope\": \"").append(entry.scope().name().toLowerCase()).append("\"");
            if (entry.sourceId() != null) {
                json.append(", \"source\": \"").append(escape(entry.sourceId())).append("\"");
            }
            json.append(", \"kind\": \"").append(entry.kind().configValue()).append("\"}");
        }
        json.append("\n  ]\n}");
        return json.toString();
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
