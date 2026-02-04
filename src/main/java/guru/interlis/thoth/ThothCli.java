package guru.interlis.thoth;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

@Command(
    name = "thoth",
    mixinStandardHelpOptions = true,
    version = "Thoth 1.0.0",
    description = "Thoth - Plain text. Real websites.",
    subcommands = {
        ThothCli.BuildCommand.class,
        ThothCli.ServeCommand.class
    }
)
public final class ThothCli implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ThothCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "build", description = "Builds the static site")
    static final class BuildCommand implements Callable<Integer> {
        @Option(names = "--input", required = true, description = "Input directory")
        private Path input;

        @Option(names = "--output", required = true, description = "Output directory")
        private Path output;

        @Option(names = "--clean", description = "Delete output directory before build")
        private boolean clean;

        @Override
        public Integer call() throws Exception {
            try (SiteGenerator generator = new SiteGenerator(input, output)) {
                generator.buildAll(clean);
                System.out.println("[done] Build finished.");
            }
            return 0;
        }
    }

    @Command(name = "serve", description = "Runs dev server with file watching")
    static final class ServeCommand implements Callable<Integer> {
        @Option(names = "--input", required = true, description = "Input directory")
        private Path input;

        @Option(names = "--output", required = true, description = "Output directory")
        private Path output;

        @Option(names = "--port", description = "Dev server port")
        private Integer port;

        @Override
        public Integer call() throws Exception {
            try (SiteGenerator generator = new SiteGenerator(input, output)) {
                generator.buildAll(false);

                int resolvedPort = generator.resolveServePort(port);
                DevServer server = new DevServer(output, resolvedPort);
                InputWatcher watcher = new InputWatcher(input, generator::handleInputEvent);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        watcher.close();
                    } catch (Exception ignored) {
                    }
                    server.stop();
                }));

                server.start();
                watcher.start();

                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            }
            return 0;
        }
    }
}
