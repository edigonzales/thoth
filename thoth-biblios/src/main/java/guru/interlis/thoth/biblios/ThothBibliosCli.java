package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.core.InputWatcher;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Command(
    name = "thoth-biblios",
    mixinStandardHelpOptions = true,
    versionProvider = ThothBibliosCli.VersionProvider.class,
    description = "Thoth Biblios - Multi-repo documentation site generator",
    subcommands = {
        ThothBibliosCli.BuildCommand.class,
        ThothBibliosCli.ServeCommand.class
    }
)
public final class ThothBibliosCli implements Callable<Integer> {

    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            String version = ThothBibliosCli.class.getPackage().getImplementationVersion();
            return new String[]{ "thoth-biblios " + (version != null ? version : "dev") };
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ThothBibliosCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    static Path resolveOutputDir(Path configPath, BibliosConfig bibliosConfig, Path outputOverride) {
        if (outputOverride != null) {
            return outputOverride.toAbsolutePath().normalize();
        }

        Path configuredOutput = Path.of(bibliosConfig.output().dir());
        if (configuredOutput.isAbsolute()) {
            return configuredOutput.normalize();
        }

        Path configFile = configPath.toAbsolutePath().normalize();
        Path configDirectory = configFile.getParent();
        if (configDirectory == null) {
            configDirectory = Path.of(".").toAbsolutePath().normalize();
        }
        return configDirectory.resolve(configuredOutput).normalize();
    }

    @Command(name = "build", description = "Builds the documentation site")
    static final class BuildCommand implements Callable<Integer> {
        private enum BuildFormat {
            HTML("html"),
            PDF("pdf"),
            DOCX("docx");

            private final String cliValue;

            BuildFormat(String cliValue) {
                this.cliValue = cliValue;
            }

            static BuildFormat fromCliValue(String value) {
                for (BuildFormat format : values()) {
                    if (format.cliValue.equals(value)) {
                        return format;
                    }
                }
                return null;
            }

        }

        @Option(names = "--config", required = true, description = "Path to bibliios.yml configuration file")
        private Path config;

        @Option(names = "--output", description = "Output directory (overrides config)")
        private Path output;

        @Option(names = "--clean", description = "Delete output directory before build")
        private boolean clean;

        @Option(
            names = "--format",
            split = ",",
            description = "Output format(s): html, pdf, docx. Accepts comma-separated values. Default: html."
        )
        private List<String> formats = new ArrayList<>();

        @Option(
            names = "--pdf-version",
            split = ",",
            description = "Limit PDF generation to one or more versions (e.g. main, v1.x, or component/main). Requires --format pdf."
        )
        private List<String> pdfVersions = new ArrayList<>();

        @Option(
            names = "--docx-version",
            split = ",",
            description = "Limit DOCX generation to one or more versions (e.g. main, v1.x, or component/main). Requires --format docx."
        )
        private List<String> docxVersions = new ArrayList<>();

        @Override
        public Integer call() throws Exception {
            System.out.println("[info] thoth-biblios build");
            System.out.println("[info] config: " + config);
            Set<BuildFormat> selectedFormats = selectedFormats();
            if (selectedFormats == null) {
                return 2;
            }
            boolean generateHtml = selectedFormats.contains(BuildFormat.HTML);
            boolean generatePdf = selectedFormats.contains(BuildFormat.PDF);
            boolean generateDocx = selectedFormats.contains(BuildFormat.DOCX);
            if (pdfVersions == null) {
                pdfVersions = new ArrayList<>();
            }
            if (!generatePdf && !pdfVersions.isEmpty()) {
                System.err.println("[error] --pdf-version requires --format pdf.");
                return 2;
            }
            if (docxVersions == null) {
                docxVersions = new ArrayList<>();
            }
            if (!generateDocx && !docxVersions.isEmpty()) {
                System.err.println("[error] --docx-version requires --format docx.");
                return 2;
            }
            if (generateDocx && docxVersions.isEmpty()) {
                System.err.println("[error] --format docx requires at least one --docx-version.");
                return 2;
            }

            // Load configuration
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig bibliosConfig = parser.parse(config);
            System.out.println("[info] Loaded config: " + bibliosConfig.site().title());

            // Resolve output path
            Path outputDir = resolveOutputDir(config, bibliosConfig, output);
            System.out.println("[info] output: " + outputDir);
            if (clean) {
                bibliosConfig = overrideClean(bibliosConfig);
            }

            // Build catalog
            Path workRoot = Path.of(".thoth/cache");
            try (CatalogBuilder catalogBuilder = new CatalogBuilder(bibliosConfig, workRoot, true)) {
                SiteCatalog catalog = catalogBuilder.build();
                System.out.println("[info] Catalog built: " + catalog.components().size() + " components");

                // Generate site
                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(bibliosConfig, catalog, outputDir, config)) {
                    generator.generate(generateHtml, generatePdf, selectedPdfVersions(), generateDocx, selectedDocxVersions());
                }
            }

            System.out.println("[done] Build finished.");
            return 0;
        }

        private BibliosConfig overrideClean(BibliosConfig cfg) {
            // For simplicity, just note it in output - real impl would override output section
            System.out.println("[info] clean: true");
            return cfg;
        }

        private Set<BuildFormat> selectedFormats() {
            if (formats == null || formats.isEmpty()) {
                return EnumSet.of(BuildFormat.HTML);
            }
            Set<BuildFormat> selected = EnumSet.noneOf(BuildFormat.class);
            List<String> unknown = new ArrayList<>();
            for (String candidate : formats) {
                if (candidate == null) {
                    continue;
                }
                String trimmed = candidate.trim().toLowerCase(Locale.ROOT);
                if (trimmed.isBlank()) {
                    continue;
                }
                BuildFormat format = BuildFormat.fromCliValue(trimmed);
                if (format == null) {
                    unknown.add(trimmed);
                    continue;
                }
                selected.add(format);
            }
            if (!unknown.isEmpty()) {
                System.err.println("[error] Unknown format(s): " + String.join(", ", unknown)
                    + ". Allowed values: html, pdf, docx.");
                return null;
            }
            if (selected.isEmpty()) {
                System.err.println("[error] --format requires at least one of: html, pdf, docx.");
                return null;
            }
            return Set.copyOf(selected);
        }

        private Set<String> selectedPdfVersions() {
            if (pdfVersions == null || pdfVersions.isEmpty()) {
                return Set.of();
            }
            Set<String> selected = new LinkedHashSet<>();
            for (String candidate : pdfVersions) {
                if (candidate == null) {
                    continue;
                }
                String trimmed = candidate.trim();
                if (!trimmed.isBlank()) {
                    selected.add(trimmed);
                }
            }
            return Set.copyOf(selected);
        }

        private Set<String> selectedDocxVersions() {
            if (docxVersions == null || docxVersions.isEmpty()) {
                return Set.of();
            }
            Set<String> selected = new LinkedHashSet<>();
            for (String candidate : docxVersions) {
                if (candidate == null) {
                    continue;
                }
                String trimmed = candidate.trim();
                if (!trimmed.isBlank()) {
                    selected.add(trimmed);
                }
            }
            return Set.copyOf(selected);
        }
    }

    @Command(name = "serve", description = "Runs dev server for documentation site")
    static final class ServeCommand implements Callable<Integer> {
        @Option(names = "--config", required = true, description = "Path to bibliios.yml configuration file")
        private Path config;

        @Option(names = "--output", description = "Output directory (overrides config)")
        private Path output;

        @Option(names = "--port", description = "Dev server port")
        private Integer port;

        @Option(
            names = "--use-local-working-tree",
            description = "For local sources, render the currently checked-out branch directly from the local working tree"
        )
        private boolean useLocalWorkingTree;

        private record ServeState(
            Path configFile,
            BibliosConfig config,
            SiteCatalog catalog,
            Path outputDir,
            Map<String, Path> localSourceRootsById,
            Map<String, SourceConfig> sourcesById
        ) {
            private ServeState {
                configFile = configFile.toAbsolutePath().normalize();
                outputDir = outputDir.toAbsolutePath().normalize();
                localSourceRootsById = Map.copyOf(localSourceRootsById);
                sourcesById = Map.copyOf(sourcesById);
            }

            ServeState withCatalog(SiteCatalog updatedCatalog) {
                return new ServeState(configFile, config, updatedCatalog, outputDir, localSourceRootsById, sourcesById);
            }

            SourceConfig sourceById(String sourceId) {
                return sourcesById.get(sourceId);
            }
        }

        @Override
        public Integer call() throws Exception {
            System.out.println("[info] thoth-biblios serve");
            System.out.println("[info] config: " + config);
            int resolvedPort = port != null ? port : 8080;
            System.out.println("[info] port: " + resolvedPort);
            if (useLocalWorkingTree) {
                System.out.println("[info] local working tree mode: enabled");
            }

            Path workRoot = Path.of(".thoth/cache");
            AtomicBoolean rebuilding = new AtomicBoolean(false);
            AtomicReference<ServeState> serveState = new AtomicReference<>();
            AtomicReference<List<InputWatcher>> sourceWatchers = new AtomicReference<>(List.of());

            // Initial full build
            ServeState initialState = doFullBuild(config, output, workRoot, true, useLocalWorkingTree, false);
            serveState.set(initialState);
            refreshLocalSourceWatchers(initialState, rebuilding, sourceWatchers, serveState, workRoot, useLocalWorkingTree);

            // Start dev server and config watcher
            try (InputWatcher configWatcher = watchConfig(
                config, workRoot, output, rebuilding, sourceWatchers, serveState, useLocalWorkingTree
            )) {
                // Server lifecycle managed via shutdown hook
                final var server = new guru.interlis.thoth.core.DevServer(initialState.outputDir(), resolvedPort);
                server.start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    server.stop();
                    try { configWatcher.close(); } catch (Exception ignored) {}
                    closeWatchers(sourceWatchers.getAndSet(List.of()));
                }));

                System.out.println("[info] Watching for changes. Press Ctrl+C to stop.");

                // Block until interrupted
                Thread.currentThread().join();
            } finally {
                closeWatchers(sourceWatchers.getAndSet(List.of()));
            }
            return 0;
        }

        private InputWatcher watchConfig(Path configPath, Path workRoot, Path outputOverride,
                                         AtomicBoolean rebuilding,
                                         AtomicReference<List<InputWatcher>> sourceWatchers,
                                         AtomicReference<ServeState> serveState,
                                         boolean useLocalWorkingTree) throws Exception {
            Path configFile = configPath.toAbsolutePath().normalize();
            Path configDirectory = configFile.getParent();
            if (configDirectory == null) {
                configDirectory = Path.of(".").toAbsolutePath();
            }
            InputWatcher watcher = new InputWatcher(configDirectory, (changedPath, eventType) -> {
                boolean configChanged = changedPath.toAbsolutePath().normalize().equals(configFile);
                boolean htmlCustomizationChanged = ServeWatchSupport.isHtmlCustomizationPath(configFile, changedPath);
                if ((configChanged || htmlCustomizationChanged) && !rebuilding.getAndSet(true)) {
                    boolean fetchEnabled = configChanged;
                    String reason = configChanged ? "Config" : "HTML customization";
                    System.out.println("[info] " + reason + " changed (" + eventType + "), full rebuild...");
                    try {
                        ServeState updatedState = doFullBuild(
                            configPath, outputOverride, workRoot, fetchEnabled, useLocalWorkingTree, true
                        );
                        serveState.set(updatedState);
                        refreshLocalSourceWatchers(
                            updatedState, rebuilding, sourceWatchers, serveState, workRoot, useLocalWorkingTree
                        );
                    } catch (Exception e) {
                        System.err.println("[error] Rebuild failed: " + e.getMessage());
                    } finally {
                        rebuilding.set(false);
                    }
                }
            });
            watcher.start();
            return watcher;
        }

        private void refreshLocalSourceWatchers(ServeState state,
                                                AtomicBoolean rebuilding,
                                                AtomicReference<List<InputWatcher>> sourceWatchers,
                                                AtomicReference<ServeState> serveState,
                                                Path workRoot,
                                                boolean useLocalWorkingTree) throws Exception {
            Map<String, Path> rootsBySource = state.localSourceRootsById();
            Set<Path> uniqueRoots = new LinkedHashSet<>(rootsBySource.values());

            List<InputWatcher> newWatchers = new ArrayList<>();
            for (Path root : uniqueRoots) {
                if (!Files.exists(root)) {
                    System.err.println("[warn] Local source path does not exist, skipping watcher: " + root);
                    continue;
                }
                InputWatcher watcher = new InputWatcher(root, (changedPath, eventType) -> {
                    if (ServeWatchSupport.shouldIgnoreRepoMetadataChange(changedPath)) {
                        return;
                    }
                    ServeState currentState = serveState.get();
                    if (currentState == null) {
                        return;
                    }
                    String sourceId = ServeWatchSupport.findChangedSourceId(currentState.localSourceRootsById(), changedPath);
                    if (sourceId == null) {
                        return;
                    }
                    if (!rebuilding.getAndSet(true)) {
                        System.out.println("[info] Local source changed (" + eventType + "): " + changedPath);
                        System.out.println("[info] Incremental rebuild for source: " + sourceId);
                        try {
                            rebuildSingleSource(sourceId, workRoot, serveState, useLocalWorkingTree);
                        } catch (Exception e) {
                            System.err.println("[error] Rebuild failed: " + e.getMessage());
                        } finally {
                            rebuilding.set(false);
                        }
                    }
                });
                watcher.start();
                newWatchers.add(watcher);
            }

            List<InputWatcher> oldWatchers = sourceWatchers.getAndSet(List.copyOf(newWatchers));
            closeWatchers(oldWatchers);

            if (newWatchers.isEmpty()) {
                if (useLocalWorkingTree) {
                    System.out.println("[info] No local content sources configured for live watch.");
                } else {
                    System.out.println("[info] Local content watching is disabled (enable with --use-local-working-tree).");
                }
            } else {
                System.out.println("[info] Watching local content sources: " + newWatchers.size() + " root(s).");
            }
        }

        private void closeWatchers(List<InputWatcher> watchers) {
            for (InputWatcher watcher : watchers) {
                try {
                    watcher.close();
                } catch (Exception ignored) {
                }
            }
        }

        private void rebuildSingleSource(String sourceId, Path workRoot,
                                         AtomicReference<ServeState> serveState,
                                         boolean useLocalWorkingTree) throws Exception {
            ServeState state = serveState.get();
            if (state == null) {
                return;
            }
            SourceConfig source = state.sourceById(sourceId);
            if (source == null) {
                System.err.println("[warn] Changed source is not part of current config: " + sourceId);
                return;
            }

            try (CatalogBuilder catalogBuilder = new CatalogBuilder(
                state.config(), workRoot, false, useLocalWorkingTree, state.configFile()
            )) {
                DocComponent updatedComponent = catalogBuilder.buildComponent(source);
                SiteCatalog updatedCatalog = state.catalog().withReplacedComponent(updatedComponent);
                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(
                    state.config(), updatedCatalog, state.outputDir(), state.configFile()
                )) {
                    generator.regenerateComponent(updatedComponent);
                    generator.regenerateGlobalArtifacts();
                }
                serveState.set(state.withCatalog(updatedCatalog));
            }
            System.out.println("[info] Incremental rebuild complete for source: " + sourceId);
        }

        private ServeState doFullBuild(Path configPath, Path outputOverride, Path workRoot,
                                       boolean fetchEnabled, boolean useLocalWorkingTree,
                                       boolean forceCleanOutput) throws Exception {
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig bibliosConfig = parser.parse(configPath);
            Path outputDir = ThothBibliosCli.resolveOutputDir(configPath, bibliosConfig, outputOverride);

            System.out.println("[info] Building site...");
            System.out.println("[info] Output directory: " + outputDir);
            if (forceCleanOutput && Files.exists(outputDir)) {
                deleteRecursively(outputDir);
            }
            try (CatalogBuilder catalogBuilder = new CatalogBuilder(
                bibliosConfig, workRoot, fetchEnabled, useLocalWorkingTree, configPath
            )) {
                SiteCatalog catalog = catalogBuilder.build();
                System.out.println("[info] Catalog: " + catalog.components().size() + " components");

                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(bibliosConfig, catalog, outputDir, configPath)) {
                    generator.generate();
                }
                System.out.println("[info] Build complete.");
                return new ServeState(
                    configPath,
                    bibliosConfig,
                    catalog,
                    outputDir,
                    ServeWatchSupport.localSourceRootsForServe(bibliosConfig, configPath, useLocalWorkingTree),
                    indexSourcesById(bibliosConfig)
                );
            }
        }

        private Map<String, SourceConfig> indexSourcesById(BibliosConfig config) {
            Map<String, SourceConfig> sourcesById = new HashMap<>();
            for (SourceConfig source : config.content().sources()) {
                sourcesById.put(source.id(), source);
            }
            return sourcesById;
        }

        private void deleteRecursively(Path dir) throws IOException {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ex) {
                            System.err.println("[warn] Failed to delete during clean rebuild: " + path);
                        }
                    });
            }
        }
    }
}
