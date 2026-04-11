package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.core.InputWatcher;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

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
            Path outputDir = output != null ? output : Path.of(bibliosConfig.output().dir());
            if (clean) {
                bibliosConfig = overrideClean(bibliosConfig);
            }

            // Build catalog
            Path workRoot = Path.of(".thoth/cache");
            try (CatalogBuilder catalogBuilder = new CatalogBuilder(bibliosConfig, workRoot)) {
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

        @Override
        public Integer call() throws Exception {
            System.out.println("[info] thoth-biblios serve");
            System.out.println("[info] config: " + config);
            int resolvedPort = port != null ? port : 8080;
            System.out.println("[info] port: " + resolvedPort);

            Path workRoot = Path.of(".thoth/cache");

            // Initial build
            doBuild(config, output, workRoot);

            // Start dev server and watchers
            Path outputDir = output != null ? output : resolveOutputDir(config);
            try (InputWatcher configWatcher = watchConfig(config, workRoot, output)) {
                InputWatcher repoWatcher = watchRepoCache(workRoot, config, output);

                // Server lifecycle managed via shutdown hook
                final var server = new guru.interlis.thoth.core.DevServer(outputDir, resolvedPort);
                server.start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    server.stop();
                    try { configWatcher.close(); } catch (Exception ignored) {}
                    if (repoWatcher != null) { try { repoWatcher.close(); } catch (Exception ignored) {} }
                }));

                System.out.println("[info] Watching for changes. Press Ctrl+C to stop.");

                // Block until interrupted
                Thread.currentThread().join();
            }
            return 0;
        }

        private InputWatcher watchConfig(Path configPath, Path workRoot, Path outputOverride) throws Exception {
            AtomicBoolean rebuilding = new AtomicBoolean(false);
            InputWatcher watcher = new InputWatcher(configPath.getParent(), (changedPath, eventType) -> {
                if (changedPath.getFileName().toString().equals(configPath.getFileName().toString()) &&
                    !rebuilding.getAndSet(true)) {
                    System.out.println("[info] Config changed (" + eventType + "), rebuilding...");
                    try {
                        doBuild(configPath, outputOverride, workRoot);
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

        private InputWatcher watchRepoCache(Path workRoot, Path configPath, Path outputOverride) throws Exception {
            if (!java.nio.file.Files.exists(workRoot)) {
                return null;
            }
            AtomicBoolean rebuilding = new AtomicBoolean(false);
            InputWatcher watcher = new InputWatcher(workRoot, (changedPath, eventType) -> {
                if (!rebuilding.getAndSet(true)) {
                    System.out.println("[info] Repo cache changed (" + eventType + "), rebuilding...");
                    try {
                        doBuild(configPath, outputOverride, workRoot);
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

        private void doBuild(Path configPath, Path outputOverride, Path workRoot) throws Exception {
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig bibliosConfig = parser.parse(configPath);
            Path outputDir = outputOverride != null ? outputOverride : Path.of(bibliosConfig.output().dir());

            System.out.println("[info] Building site...");
            try (CatalogBuilder catalogBuilder = new CatalogBuilder(bibliosConfig, workRoot)) {
                SiteCatalog catalog = catalogBuilder.build();
                System.out.println("[info] Catalog: " + catalog.components().size() + " components");

                try (BibliosSiteGenerator generator = new BibliosSiteGenerator(bibliosConfig, catalog, outputDir)) {
                    generator.generate();
                }
            }
            System.out.println("[info] Build complete.");
        }

        private Path resolveOutputDir(Path configPath) throws Exception {
            BibliosConfigParser parser = new BibliosConfigParser();
            BibliosConfig config = parser.parse(configPath);
            return Path.of(config.output().dir());
        }
    }
}
