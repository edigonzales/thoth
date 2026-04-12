package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.SourceConfig;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ServeWatchSupport {
    private ServeWatchSupport() {
    }

    static List<Path> localSourceRoots(BibliosConfig config, Path configPath) {
        List<Path> roots = new ArrayList<>();
        Path configDir = configPath.toAbsolutePath().getParent();
        for (SourceConfig source : config.content().sources()) {
            Path sourceRoot = toLocalSourceRoot(source.url(), configDir);
            if (sourceRoot != null) {
                roots.add(sourceRoot.normalize());
            }
        }
        return roots;
    }

    static Path toLocalSourceRoot(String sourceUrl, Path configDir) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        String raw = sourceUrl.trim();

        if (raw.startsWith("file://")) {
            try {
                return Path.of(URI.create(raw));
            } catch (Exception ignored) {
                return null;
            }
        }

        if (isRemoteUrl(raw)) {
            return null;
        }

        Path path = Path.of(raw);
        if (path.isAbsolute()) {
            return path;
        }
        if (configDir == null) {
            return path.toAbsolutePath();
        }
        return configDir.resolve(path).toAbsolutePath();
    }

    static boolean shouldIgnoreRepoMetadataChange(Path changedPath) {
        if (changedPath == null) {
            return false;
        }
        for (Path part : changedPath.normalize()) {
            if (".git".equals(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRemoteUrl(String value) {
        return value.contains("://") || value.startsWith("git@");
    }
}
