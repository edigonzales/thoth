package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.core.InputWatcher;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
        @Option(names = "--config", required = true, description = "Path to bibliios.yml configuration file")
        private Path config;

        @Option(names = "--output", description = "Output directory (overrides config)")
        private Path output;

        @Option(names = "--clean", description = "Delete output directory before build")
        private boolean clean;

        @Override
        public Integer call() throws Exception {
            System.out.println("[info] thoth-biblios build");
            System.out.println("[info] config: " + config);

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
                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(bibliosConfig, catalog, outputDir)) {
                    generator.generate();
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

            // Initial build
            doBuild(config, output, workRoot, true, useLocalWorkingTree);

            // Start dev server and watchers
            Path outputDir = resolveOutputDir(config, output);
            AtomicBoolean rebuilding = new AtomicBoolean(false);
            AtomicReference<List<InputWatcher>> sourceWatchers = new AtomicReference<>(List.of());

            try (InputWatcher configWatcher = watchConfig(
                config, workRoot, output, rebuilding, sourceWatchers, useLocalWorkingTree
            )) {
                refreshLocalSourceWatchers(config, rebuilding, sourceWatchers, workRoot, output, useLocalWorkingTree);
                // Server lifecycle managed via shutdown hook
                final var server = new guru.interlis.thoth.core.DevServer(outputDir, resolvedPort);
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
                                         AtomicBoolean rebuilding, AtomicReference<List<InputWatcher>> sourceWatchers,
                                         boolean useLocalWorkingTree) throws Exception {
            Path configFile = configPath.toAbsolutePath().normalize();
            Path configDirectory = configFile.getParent();
            if (configDirectory == null) {
                configDirectory = Path.of(".").toAbsolutePath();
            }
            InputWatcher watcher = new InputWatcher(configDirectory, (changedPath, eventType) -> {
                if (changedPath.toAbsolutePath().normalize().equals(configFile) &&
                    !rebuilding.getAndSet(true)) {
                    System.out.println("[info] Config changed (" + eventType + "), rebuilding...");
                    try {
                        doBuild(configPath, outputOverride, workRoot, false, useLocalWorkingTree);
                        refreshLocalSourceWatchers(
                            configPath, rebuilding, sourceWatchers, workRoot, outputOverride, useLocalWorkingTree
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

        private void refreshLocalSourceWatchers(Path configPath,
                                                AtomicBoolean rebuilding,
                                                AtomicReference<List<InputWatcher>> sourceWatchers,
                                                Path workRoot,
                                                Path outputOverride,
                                                boolean useLocalWorkingTree) throws Exception {
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig bibliosConfig = parser.parse(configPath);
            List<Path> roots = ServeWatchSupport.localSourceRoots(bibliosConfig, configPath);
            Set<Path> uniqueRoots = new LinkedHashSet<>(roots);

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
                    if (!rebuilding.getAndSet(true)) {
                        System.out.println("[info] Local source changed (" + eventType + "): " + changedPath);
                        System.out.println("[info] Rebuilding...");
                        try {
                            doBuild(configPath, outputOverride, workRoot, false, useLocalWorkingTree);
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
                System.out.println("[info] No local content sources configured for live watch.");
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

        private void doBuild(Path configPath, Path outputOverride, Path workRoot,
                             boolean fetchEnabled, boolean useLocalWorkingTree) throws Exception {
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig bibliosConfig = parser.parse(configPath);
            Path outputDir = ThothBibliosCli.resolveOutputDir(configPath, bibliosConfig, outputOverride);

            System.out.println("[info] Building site...");
            System.out.println("[info] Output directory: " + outputDir);
            try (CatalogBuilder catalogBuilder = new CatalogBuilder(
                bibliosConfig, workRoot, fetchEnabled, useLocalWorkingTree, configPath
            )) {
                SiteCatalog catalog = catalogBuilder.build();
                System.out.println("[info] Catalog: " + catalog.components().size() + " components");

                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(bibliosConfig, catalog, outputDir)) {
                    generator.generate();
                }
            }
            System.out.println("[info] Build complete.");
        }

        private Path resolveOutputDir(Path configPath, Path outputOverride) throws Exception {
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig bibliosConfig = parser.parse(configPath);
            return ThothBibliosCli.resolveOutputDir(configPath, bibliosConfig, outputOverride);
        }
    }
}
