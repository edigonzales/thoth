package guru.interlis.thoth.biblios.publication;

import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.config.SourceId;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.core.ThothBuildException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Writes a private publication package from a generated static site.
 *
 * <p>Layout:</p>
 * <pre>
 * package/
 *   manifest.json          authoritative path -> documentation mapping
 *   pages/&lt;route&gt;/index.html   pre-rendered content fragments (no frame)
 *   files/&lt;path&gt;          every servable byte file (assets, images, PDF/DOCX)
 *   search-index.json      internal search data (never served directly)
 * </pre>
 *
 * <p>Every file in the static output must be classifiable: page frames and global
 * pages are server-rendered and skipped, site assets are shared, and everything
 * under a component directory belongs to that documentation. Unmapped files abort
 * the build instead of leaking later.</p>
 */
public final class PublicationPackageWriter {
    private static final String FILES_DIR = "files";
    private static final String PAGES_DIR = "pages";

    private final Path packageRoot;
    private final Path outputRoot;
    private final SiteCatalog catalog;
    private final PublicationManifest manifest = new PublicationManifest();
    private final Set<String> pageOutputPaths = new LinkedHashSet<>();
    private final Set<String> componentIds = new HashSet<>();

    public PublicationPackageWriter(Path packageRoot, Path outputRoot, SiteCatalog catalog) {
        this.packageRoot = packageRoot.toAbsolutePath().normalize();
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
        this.catalog = catalog;
        for (DocComponent component : catalog.components()) {
            componentIds.add(SourceId.validate(component.id()));
        }
    }

    public Path packageRoot() {
        return packageRoot;
    }

    public PublicationManifest manifest() {
        return manifest;
    }

    /**
     * Record a documentation page fragment. Called while the static site is generated.
     */
    public void recordPage(String route, String sourceId, String fragmentHtml) throws IOException {
        String routePath = normalizeRoute(route);
        String fragmentFile = PAGES_DIR + "/" + routePath + "index.html";
        writeString(packageRoot.resolve(fragmentFile), fragmentHtml);
        pageOutputPaths.add(routePath + "index.html");
        manifest.add(ManifestEntry.sourcePage(routePath, sourceId));
    }

    /**
     * Prepare the package directory. Removes stale content and must be called
     * before the static site is generated, so that page fragments are not lost.
     */
    public void begin() throws IOException {
        deleteRecursively(packageRoot);
        Files.createDirectories(packageRoot);
    }

    /**
     * Write the public catalog metadata (view models + fragment references).
     * Called by the generator; the catalog is internal data and never served directly.
     */
    public void writeCatalog(String catalogJson) throws IOException {
        writeString(packageRoot.resolve("catalog.json"), catalogJson);
        manifest.add(ManifestEntry.internal("catalog.json"));
    }

    /**
     * Finalize the package: copy all remaining servable files, write the internal
     * search data and the manifest. Must be called after the static site is complete.
     */
    public void finish() throws IOException {
        Files.createDirectories(packageRoot);

        copyInternalSearchIndex();

        Set<String> copied = new HashSet<>();
        try (var stream = Files.walk(outputRoot)) {
            for (Path file : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                String relative = outputRoot.relativize(file).toString().replace('\\', '/');
                classify(relative, file, copied);
            }
        }

        writeString(packageRoot.resolve("manifest.json"), manifest.toJson());
        System.out.println("[info] Publication package: " + packageRoot
            + " (" + manifest.size() + " manifest entries, " + copied.size() + " files)");
    }

    private void classify(String relative, Path file, Set<String> copied) throws IOException {
        if (isServerRendered(relative)) {
            return;
        }
        // Search data is copied as internal payload and never served directly.
        if (relative.equals("search-index.json") || relative.equals("search-index.js")) {
            return;
        }
        if (relative.startsWith("site-assets/")) {
            copyServable(relative, file);
            manifest.add(ManifestEntry.sharedFile(relative));
            copied.add(relative);
            return;
        }
        String firstSegment = relative.contains("/") ? relative.substring(0, relative.indexOf('/')) : relative;
        if (componentIds.contains(firstSegment)) {
            copyServable(relative, file);
            manifest.add(ManifestEntry.sourceFile(relative, firstSegment));
            copied.add(relative);
            return;
        }
        throw new ThothBuildException(
            "Unmapped file in static output: " + relative + "\n"
                + "Every generated file must belong to a documentation source or to shared assets.",
            ThothBuildException.ErrorSeverity.FATAL,
            "package"
        );
    }

    private boolean isServerRendered(String relative) {
        if (relative.equals("index.html") || relative.equals("search/index.html")) {
            return true;
        }
        if (pageOutputPaths.contains(relative)) {
            return true;
        }
        // Component landing pages (<component>/index.html) are server-rendered too.
        int firstSlash = relative.indexOf('/');
        return firstSlash > 0
            && relative.endsWith("/index.html")
            && relative.indexOf('/', firstSlash + 1) < 0;
    }

    private void copyServable(String relative, Path file) throws IOException {
        Path target = packageRoot.resolve(FILES_DIR).resolve(relative).normalize();
        if (!target.startsWith(packageRoot.resolve(FILES_DIR))) {
            throw new ThothBuildException(
                "Unsafe package path: " + relative,
                ThothBuildException.ErrorSeverity.FATAL,
                "package"
            );
        }
        Files.createDirectories(target.getParent());
        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyInternalSearchIndex() throws IOException {
        for (String name : new String[] {"search-index.json", "search-index.js"}) {
            Path source = outputRoot.resolve(name);
            if (Files.exists(source)) {
                Files.copy(source, packageRoot.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                manifest.add(ManifestEntry.internal(name));
            }
        }
    }

    private String normalizeRoute(String route) {
        String normalized = route == null ? "" : route.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private void writeString(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
