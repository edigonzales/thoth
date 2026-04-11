package guru.interlis.thoth.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

/**
 * Utility for orchestrating DevServer + InputWatcher lifecycle in CLI serve commands.
 * Reduces boilerplate in product-specific serve implementations.
 *
 * <p>Typical usage:
 * <pre>{@code
 * try (ServeHandle handle = new ServeHandle(outputDir, port)) {
 *     handle.startServer();
 *     handle.addWatcher(inputDir, generator::handleInputEvent);
 *     handle.awaitShutdown();
 * }
 * }</pre>
 */
public final class ServeHandle implements AutoCloseable {

    private final Path outputDir;
    private final int port;
    private final List<InputWatcher> watchers = new ArrayList<>();

    private DevServer server;

    public ServeHandle(Path outputDir, int port) {
        this.outputDir = outputDir;
        this.port = port;
    }

    /** Start the HTTP dev server. */
    public void startServer() throws IOException {
        server = new DevServer(outputDir, port);
        server.start();
    }

    /** Register an InputWatcher for the given root path. */
    public void addWatcher(Path root, BiConsumer<Path, String> handler) throws IOException {
        InputWatcher watcher = new InputWatcher(root, handler);
        watchers.add(watcher);
        watcher.start();
    }

    /** Block until a shutdown signal is received (Ctrl+C). */
    public void awaitShutdown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }

    @Override
    public void close() {
        for (InputWatcher watcher : watchers) {
            try {
                watcher.close();
            } catch (Exception ignored) {
            }
        }
        if (server != null) {
            server.stop();
        }
    }
}
