package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.ServeWatchSupport;
import guru.interlis.thoth.biblios.config.SourceConfig;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves whether a source/branch pair should be read directly from a local working tree.
 */
final class LocalWorkingTreeOverride {
    private static final LocalWorkingTreeOverride DISABLED = new LocalWorkingTreeOverride(false, null, null);

    private final boolean enabled;
    private final Path sourceRoot;
    private final String currentBranch;

    private LocalWorkingTreeOverride(boolean enabled, Path sourceRoot, String currentBranch) {
        this.enabled = enabled;
        this.sourceRoot = sourceRoot;
        this.currentBranch = currentBranch;
    }

    static LocalWorkingTreeOverride disabled() {
        return DISABLED;
    }

    static LocalWorkingTreeOverride resolve(SourceConfig source, Path configPath, boolean localWorkingTreeEnabled) throws IOException {
        if (!localWorkingTreeEnabled) {
            return disabled();
        }
        if (configPath == null) {
            throw new IOException("Local working tree mode requires a config path");
        }

        Path configDir = configPath.toAbsolutePath().normalize().getParent();
        Path localSourceRoot = ServeWatchSupport.toLocalSourceRoot(source.url(), configDir);
        if (localSourceRoot == null) {
            return disabled();
        }

        Path normalizedRoot = localSourceRoot.toAbsolutePath().normalize();
        if (!Files.exists(normalizedRoot) || !Files.isDirectory(normalizedRoot)) {
            throw new IOException(
                "Local working tree source path does not exist or is not a directory for source '" + source.id() + "': " + normalizedRoot
            );
        }

        String branchName;
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            builder.findGitDir(normalizedRoot.toFile());
            if (builder.getGitDir() == null) {
                throw new IOException("No .git directory found");
            }
            try (Repository repository = builder.build()) {
                branchName = repository.getBranch();
            }
        } catch (Exception e) {
            throw new IOException(
                "Failed to detect current branch for local source '" + source.id() + "' at " + normalizedRoot +
                    ". Ensure the path is a valid Git repository.",
                e
            );
        }

        if (branchName == null || branchName.isBlank() || "HEAD".equals(branchName)) {
            throw new IOException(
                "Local source '" + source.id() + "' is in detached HEAD state. Check out a branch to use --use-local-working-tree."
            );
        }

        return new LocalWorkingTreeOverride(true, normalizedRoot, branchName);
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean appliesToBranch(String branchName) {
        return enabled && currentBranch != null && currentBranch.equals(branchName);
    }

    Path sourceRoot() {
        return sourceRoot;
    }

    String currentBranch() {
        return currentBranch;
    }
}
