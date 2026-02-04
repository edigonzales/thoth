package guru.interlis.thoth;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class InputWatcher implements AutoCloseable {
    private final Path root;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyToPath;
    private final AtomicBoolean running;
    private final BiConsumer<Path, String> handler;

    private Thread watchThread;

    public InputWatcher(Path root, BiConsumer<Path, String> handler) throws IOException {
        this.root = root;
        this.handler = handler;
        this.watchService = root.getFileSystem().newWatchService();
        this.keyToPath = new HashMap<>();
        this.running = new AtomicBoolean(false);

        registerRecursively(root);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        watchThread = new Thread(this::watchLoop, "thoth-file-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void watchLoop() {
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                System.err.println("[warn] Watch loop stopped: " + ex.getMessage());
                break;
            }

            Path watchedDir = keyToPath.get(key);
            if (watchedDir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path changedPath = watchedDir.resolve(pathEvent.context());

                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changedPath)) {
                    try {
                        registerRecursively(changedPath);
                    } catch (IOException ex) {
                        System.err.println("[warn] Could not watch new directory " + changedPath + ": " + ex.getMessage());
                    }
                }

                String eventType;
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    eventType = "CREATE";
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    eventType = "MODIFY";
                } else {
                    eventType = "DELETE";
                }

                handler.accept(changedPath, eventType);
            }

            boolean valid = key.reset();
            if (!valid) {
                keyToPath.remove(key);
            }
        }
    }

    private void registerRecursively(Path start) throws IOException {
        Files.walkFileTree(start, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path directory) throws IOException {
        WatchKey key = directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        );
        keyToPath.put(key, directory);
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        if (watchThread != null) {
            watchThread.interrupt();
        }
        watchService.close();
    }
}
