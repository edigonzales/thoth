package guru.interlis.thoth.biblios.publication;

import java.util.Objects;

/**
 * A single manifest entry: maps a portal URL path to the documentation that owns it.
 *
 * <p>Paths are relative to the portal root and use forward slashes, for example
 * {@code site-assets/styles.css} or {@code agi-betrieb/main/images/plan.png}.
 * Page entries use the page route as path, for example {@code agi-betrieb/main/handbuch/}.</p>
 */
public record ManifestEntry(String path, PublicationScope scope, String sourceId, ManifestKind kind) {

    public ManifestEntry {
        Objects.requireNonNull(path, "manifest path is required");
        Objects.requireNonNull(scope, "manifest scope is required");
        Objects.requireNonNull(kind, "manifest kind is required");
        if (scope == PublicationScope.SOURCE && (sourceId == null || sourceId.isBlank())) {
            throw new IllegalArgumentException("source-scoped manifest entry requires a source id: " + path);
        }
        if (scope != PublicationScope.SOURCE && sourceId != null && !sourceId.isBlank()) {
            throw new IllegalArgumentException("non-source manifest entry must not carry a source id: " + path);
        }
    }

    public static ManifestEntry sourceFile(String path, String sourceId) {
        return new ManifestEntry(normalize(path), PublicationScope.SOURCE, sourceId, ManifestKind.FILE);
    }

    public static ManifestEntry sourcePage(String path, String sourceId) {
        return new ManifestEntry(normalize(path), PublicationScope.SOURCE, sourceId, ManifestKind.PAGE);
    }

    public static ManifestEntry sharedFile(String path) {
        return new ManifestEntry(normalize(path), PublicationScope.SHARED, null, ManifestKind.FILE);
    }

    public static ManifestEntry internal(String path) {
        return new ManifestEntry(normalize(path), PublicationScope.INTERNAL, null, ManifestKind.INTERNAL);
    }

    private static String normalize(String path) {
        String normalized = path == null ? "" : path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
