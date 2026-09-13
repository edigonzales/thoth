package guru.interlis.thoth.biblios.server.publication;

import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads a publication package from disk and provides the lookup structures used
 * by the portal: components, page routes, files and search data.
 */
@Component
@ImportRuntimeHints(PackageReflectionHints.class)
public class PublicationPackage {

    private final Path root;
    private final PackageCatalog catalog;
    private final ManifestDocument manifest;
    private final List<SearchIndexEntry> searchIndex;
    private final Map<String, ManifestDocument.Entry> fileEntries = new HashMap<>();
    private final Map<String, PackageCatalog.ComponentEntry> componentsById = new HashMap<>();
    private final Map<String, PageLocation> pagesByRoute = new HashMap<>();
    private final Map<String, String> sourcePolicies = new HashMap<>();

    public PublicationPackage(BibliosServerProperties properties, ObjectMapper objectMapper) {
        this.root = properties.getPackageDir().toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException(
                "Publication package not found: " + root
                    + ". Build one with: thoth-biblios build --package <dir>");
        }
        this.catalog = read(objectMapper, root.resolve("catalog.json"), PackageCatalog.class);
        this.manifest = read(objectMapper, root.resolve("manifest.json"), ManifestDocument.class);
        this.searchIndex = readList(objectMapper, root.resolve("search-index.json"), SearchIndexEntry.class);

        for (ManifestDocument.Entry entry : manifest.entries()) {
            fileEntries.put(entry.path(), entry);
        }
        for (PackageCatalog.SourceEntry source : catalog.sources()) {
            sourcePolicies.put(source.id(), source.accessPolicy());
        }
        for (PackageCatalog.ComponentEntry component : catalog.components()) {
            componentsById.put(component.id(), component);
            for (PackageCatalog.VersionEntry version : component.versions()) {
                for (PackageCatalog.PageEntry page : version.pages()) {
                    pagesByRoute.put(normalizeRoute(page.route()),
                        new PageLocation(component.id(), page, version));
                }
            }
        }
    }

    public Path root() {
        return root;
    }

    public PackageCatalog catalog() {
        return catalog;
    }

    public ManifestDocument manifest() {
        return manifest;
    }

    public List<SearchIndexEntry> searchIndex() {
        return searchIndex;
    }

    /**
     * Manifest lookup for a portal path (no leading slash).
     */
    public Optional<ManifestDocument.Entry> findFile(String path) {
        return Optional.ofNullable(fileEntries.get(path));
    }

    public Map<String, String> sourcePolicies() {
        return sourcePolicies;
    }

    /**
     * Policy name of a source, or {@code null} when the source is unknown.
     * Distinguish from "no policy": check {@link #sourcePolicies()} first.
     */
    public String accessPolicyFor(String sourceId) {
        return sourcePolicies.get(sourceId);
    }

    public boolean hasSource(String sourceId) {
        return sourcePolicies.containsKey(sourceId);
    }

    public PackageCatalog.ComponentEntry component(String componentId) {
        return componentsById.get(componentId);
    }

    public PageLocation page(String route) {
        return pagesByRoute.get(normalizeRoute(route));
    }

    /**
     * Default page route for a version root such as {@code /docs/main/}, if the
     * route is a version root and has pages.
     */
    public String defaultPageRouteForVersionRoot(String route) {
        String normalized = normalizeRoute(route);
        for (PackageCatalog.ComponentEntry component : componentsById.values()) {
            for (PackageCatalog.VersionEntry version : component.versions()) {
                if (normalizeRoute(version.route()).equals(normalized) && version.defaultPageRoute() != null) {
                    return version.defaultPageRoute();
                }
            }
        }
        return null;
    }

    public Path resolvePackagePath(String relativePath) {
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Unsafe package path: " + relativePath);
        }
        return candidate;
    }

    public String readFragment(String relativePath) {
        try {
            return Files.readString(resolvePackagePath(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read package fragment: " + relativePath, e);
        }
    }

    private static String normalizeRoute(String route) {
        String normalized = route == null ? "" : route.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private <T> T read(ObjectMapper objectMapper, Path path, Class<T> type) {
        try {
            return objectMapper.readValue(path.toFile(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path + ": " + e.getMessage(), e);
        }
    }

    private <T> List<T> readList(ObjectMapper objectMapper, Path path, Class<T> elementType) {
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            var collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, elementType);
            List<T> result = objectMapper.readValue(path.toFile(), collectionType);
            return result != null ? List.copyOf(result) : List.of();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * A page route resolved to its component and version.
     */
    public record PageLocation(String componentId, PackageCatalog.PageEntry page,
                               PackageCatalog.VersionEntry version) {
    }

    /**
     * Convenience for tests and diagnostics.
     */
    public Map<String, Object> componentModel(String componentId) {
        PackageCatalog.ComponentEntry entry = componentsById.get(componentId);
        return entry != null ? new LinkedHashMap<>(entry.component()) : Map.of();
    }
}
