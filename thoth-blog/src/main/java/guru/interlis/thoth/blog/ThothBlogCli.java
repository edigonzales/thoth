package guru.interlis.thoth.blog;

import guru.interlis.thoth.core.ServeHandle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "thoth-blog",
    mixinStandardHelpOptions = true,
    versionProvider = ThothBlogCli.VersionProvider.class,
    description = "Thoth Blog - Static site generator for AsciiDoc blogs",
    subcommands = {
        ThothBlogCli.BuildCommand.class,
        ThothBlogCli.ServeCommand.class
    }
)
public final class ThothBlogCli implements Callable<Integer> {

    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            String version = ThothBlogCli.class.getPackage().getImplementationVersion();
            return new String[]{ "thoth-blog " + (version != null ? version : "dev") };
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ThothBlogCli()).execute(args);
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
                try (ServeHandle handle = new ServeHandle(output, resolvedPort)) {
                    handle.startServer();
                    handle.addWatcher(input, generator::handleInputEvent);
                    Runtime.getRuntime().addShutdownHook(new Thread(handle::close));
                    handle.awaitShutdown();
                }
            }
            return 0;
        }
    }
}
