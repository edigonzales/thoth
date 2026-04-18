package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.DocPage;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.PdfSection;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.biblios.config.SourcePdfSection;
import guru.interlis.thoth.biblios.render.AsciidoctorRenderer;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generates PDF artifacts for component versions when configured.
 */
public final class BibliosPdfGenerator {
    private final BibliosConfig config;
    private final SiteCatalog catalog;
    private final Path outputRoot;
    private final Map<String, SourceConfig> sourceConfigById;

    public BibliosPdfGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot) {
        this.config = config;
        this.catalog = catalog;
        this.outputRoot = outputRoot;
        this.sourceConfigById = indexSourceConfigs(config);
    }

    public void generate() throws IOException {
        generate(Set.of());
    }

    public void generate(Set<String> selectedVersions) throws IOException {
        Set<String> filters = normalizeFilters(selectedVersions);
        Set<String> unmatched = new HashSet<>(filters);
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            for (DocComponent component : catalog.components()) {
                SourceConfig source = sourceConfigById.get(component.id());
                if (source == null) {
                    System.err.println("[warn] Missing source config for component: " + component.id() + ", skipping PDF generation.");
                    continue;
                }
                EffectivePdfConfig effective = resolveEffectivePdfConfig(source);
                if (!effective.enabled()) {
                    continue;
                }
                for (ComponentVersion version : component.versions()) {
                    if (!matchesFilter(component, version, filters)) {
                        continue;
                    }
                    markMatched(component, version, unmatched);
                    generateVersionPdf(component, version, source, effective, renderer);
                }
            }
        }
        for (String missing : unmatched) {
            System.err.println("[warn] No PDF version matched filter: " + missing);
        }
    }

    private void generateVersionPdf(DocComponent component,
                                    ComponentVersion version,
                                    SourceConfig source,
                                    EffectivePdfConfig effective,
                                    AsciidoctorRenderer renderer) throws IOException {
        Path versionRoot = outputRoot.resolve(component.id()).resolve(version.version());
        Files.createDirectories(versionRoot);

        Path outputFile = versionRoot.resolve(pdfFileName(component, version));
        Path explicitMaster = resolveExplicitMaster(version, source, effective.masterFile());
        if (explicitMaster != null) {
            renderer.writePdf(explicitMaster, outputFile, effective.attributes(), config.site().defaultLanguage());
            System.out.println("[pdf] " + component.id() + "/" + version.version() + " -> " + outputRoot.relativize(outputFile));
            return;
        }

        if (version.pages().isEmpty()) {
            System.err.println("[warn] No pages found for " + component.id() + "/" + version.version() + ", skipping PDF generation.");
            return;
        }

        Path temporaryMaster = createAggregateMaster(component, version);
        try {
            renderer.writePdf(temporaryMaster, outputFile, effective.attributes(), config.site().defaultLanguage());
            System.out.println("[pdf] " + component.id() + "/" + version.version() + " -> " + outputRoot.relativize(outputFile));
        } finally {
            Files.deleteIfExists(temporaryMaster);
        }
    }

    private Path resolveExplicitMaster(ComponentVersion version, SourceConfig source, String explicitMasterFile) throws IOException {
        String masterFile = explicitMasterFile;
        if (masterFile == null || masterFile.isBlank()) {
            if (source.renderMode().isSinglePage()) {
                masterFile = source.masterFile();
            } else {
                return null;
            }
        }

        Path docRoot = deriveDocRoot(version);
        if (docRoot == null) {
            throw new IOException("Failed to determine documentation root for " + source.id() + "/" + version.version());
        }

        Path masterPath = docRoot.resolve(masterFile).normalize();
        if (!Files.exists(masterPath) || !Files.isRegularFile(masterPath)) {
            throw new IOException(
                "PDF master file not found for " + source.id() + "/" + version.version() + ": " + masterPath
            );
        }
        return masterPath;
    }

    private Path createAggregateMaster(DocComponent component, ComponentVersion version) throws IOException {
        Path tempFile = Files.createTempFile("thoth-biblios-pdf-", ".adoc");
        StringBuilder document = new StringBuilder();
        document.append("= ").append(component.displayName()).append(": ").append(version.displayVersion()).append("\n");
        document.append(":doctype: book\n\n");
        for (DocPage page : version.pages()) {
            Path sourceFile = resolveSourceFile(page);
            document.append("include::").append(toIncludeTarget(sourceFile)).append("[]\n\n");
        }
        Files.writeString(tempFile, document.toString(), StandardCharsets.UTF_8);
        return tempFile;
    }

    private Path deriveDocRoot(ComponentVersion version) {
        if (version.pages().isEmpty()) {
            return null;
        }
        DocPage firstPage = version.pages().get(0);
        Path sourceFile = resolveSourceFile(firstPage);
        if (sourceFile == null) {
            return null;
        }

        Path root = sourceFile.toAbsolutePath().normalize();
        Path relativeSource = Path.of(firstPage.sourcePath()).normalize();
        for (int i = 0; i < relativeSource.getNameCount(); i++) {
            root = root.getParent();
            if (root == null) {
                return null;
            }
        }
        return root;
    }

    private Path resolveSourceFile(DocPage page) {
        if (page.sourceUri() == null || page.sourceUri().isBlank()) {
            return null;
        }
        URI sourceUri = URI.create(page.sourceUri());
        if (!"file".equalsIgnoreCase(sourceUri.getScheme())) {
            return null;
        }
        return Path.of(sourceUri).toAbsolutePath().normalize();
    }

    private String toIncludeTarget(Path sourceFile) {
        String raw = sourceFile.toAbsolutePath().normalize().toString().replace('\\', '/');
        return raw.replace(" ", "\\ ");
    }

    private EffectivePdfConfig resolveEffectivePdfConfig(SourceConfig source) {
        PdfSection global = config.pdf();
        SourcePdfSection local = source.pdf();

        boolean enabled = global != null && global.enabled();
        if (local != null && local.enabled() != null) {
            enabled = local.enabled();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        if (global != null) {
            attributes.putAll(global.attributes());
        }
        if (local != null) {
            attributes.putAll(local.attributes());
        }

        String masterFile = local != null ? local.masterFile() : null;
        return new EffectivePdfConfig(enabled, masterFile, Map.copyOf(attributes));
    }

    private String pdfFileName(DocComponent component, ComponentVersion version) {
        return component.id() + "-" + version.version() + ".pdf";
    }

    private Set<String> normalizeFilters(Set<String> selectedVersions) {
        if (selectedVersions == null || selectedVersions.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String candidate : selectedVersions) {
            if (candidate == null) {
                continue;
            }
            String trimmed = candidate.trim();
            if (!trimmed.isBlank()) {
                normalized.add(trimmed);
            }
        }
        return Set.copyOf(normalized);
    }

    private boolean matchesFilter(DocComponent component, ComponentVersion version, Set<String> filters) {
        if (filters.isEmpty()) {
            return true;
        }
        return filters.contains(version.version()) || filters.contains(component.id() + "/" + version.version());
    }

    private void markMatched(DocComponent component, ComponentVersion version, Set<String> unmatched) {
        unmatched.remove(version.version());
        unmatched.remove(component.id() + "/" + version.version());
    }

    private Map<String, SourceConfig> indexSourceConfigs(BibliosConfig cfg) {
        Map<String, SourceConfig> indexed = new LinkedHashMap<>();
        for (SourceConfig source : cfg.content().sources()) {
            indexed.put(source.id(), source);
        }
        return Map.copyOf(indexed);
    }

    private record EffectivePdfConfig(boolean enabled, String masterFile, Map<String, Object> attributes) {
    }
}
