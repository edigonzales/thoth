package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.SourceConfig;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServeWatchSupport {
    private ServeWatchSupport() {
    }

    static Map<String, Path> localSourceRootsForServe(BibliosConfig config, Path configPath, boolean useLocalWorkingTree) {
        if (!useLocalWorkingTree) {
            return Map.of();
        }
        return localSourceRootsById(config, configPath);
    }

    static Map<String, Path> localSourceRootsById(BibliosConfig config, Path configPath) {
        Map<String, Path> roots = new LinkedHashMap<>();
        Path configDir = configPath.toAbsolutePath().getParent();
        for (SourceConfig source : config.content().sources()) {
            Path sourceRoot = toLocalSourceRoot(source.url(), configDir);
            if (sourceRoot != null) {
                roots.put(source.id(), sourceRoot.normalize());
            }
        }
        return roots;
    }

    static String findChangedSourceId(Map<String, Path> localSourceRootsById, Path changedPath) {
        if (localSourceRootsById == null || localSourceRootsById.isEmpty() || changedPath == null) {
            return null;
        }
        Path normalizedChangedPath = changedPath.toAbsolutePath().normalize();
        return localSourceRootsById.entrySet().stream()
            .filter(entry -> normalizedChangedPath.startsWith(entry.getValue().toAbsolutePath().normalize()))
            .max(Comparator.comparingInt(entry -> entry.getValue().getNameCount()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public static Path toLocalSourceRoot(String sourceUrl, Path configDir) {
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

    static boolean shouldIgnoreLocalSourceChange(Path configPath, Path outputDir, Path workRoot, Path changedPath) {
        if (changedPath == null) {
            return false;
        }
        Path normalizedChangedPath = changedPath.toAbsolutePath().normalize();
        if (shouldIgnoreRepoMetadataChange(normalizedChangedPath)) {
            return true;
        }
        if (configPath != null && normalizedChangedPath.equals(configPath.toAbsolutePath().normalize())) {
            return true;
        }
        if (isHtmlCustomizationPath(configPath, normalizedChangedPath)) {
            return true;
        }
        if (outputDir != null && normalizedChangedPath.startsWith(outputDir.toAbsolutePath().normalize())) {
            return true;
        }
        return workRoot != null && normalizedChangedPath.startsWith(workRoot.toAbsolutePath().normalize());
    }

    static boolean isHtmlCustomizationPath(Path configPath, Path changedPath) {
        if (configPath == null || changedPath == null) {
            return false;
        }
        Path configDirectory = configDirectory(configPath);
        if (configDirectory == null) {
            return false;
        }
        Path normalizedChangedPath = changedPath.toAbsolutePath().normalize();
        return normalizedChangedPath.startsWith(configDirectory.resolve("assets").normalize())
            || normalizedChangedPath.startsWith(configDirectory.resolve("templates").normalize());
    }

    private static Path configDirectory(Path configPath) {
        Path normalized = configPath.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) {
            return normalized;
        }
        return normalized.getParent();
    }

    private static boolean isRemoteUrl(String value) {
        return value.contains("://") || value.startsWith("git@");
    }
}
