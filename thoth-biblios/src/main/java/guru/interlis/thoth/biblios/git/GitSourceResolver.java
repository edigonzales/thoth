package guru.interlis.thoth.biblios.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Manages Git repositories using JGit.
 * Handles cloning, fetching, and branch checkouts.
 */
public final class GitSourceResolver implements AutoCloseable {

    private final Path cacheRoot;
    private Git git;
    private Path repoPath;

    /**
     * @param cacheRoot root directory for cached repositories
     */
    public GitSourceResolver(Path cacheRoot) {
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot is required");
    }

    /**
     * Clone a repository if it doesn't exist, or fetch updates if it does.
     *
     * @param remoteUrl the Git remote URL
     * @param sourceId  the source identifier (used for cache directory naming)
     * @return this resolver for chaining
     * @throws IOException if repository operations fail
     */
    public GitSourceResolver resolve(String remoteUrl, String sourceId) throws IOException {
        Objects.requireNonNull(remoteUrl, "remoteUrl is required");
        Objects.requireNonNull(sourceId, "sourceId is required");

        if (remoteUrl.isBlank()) {
            throw new IOException(
                "Repository URL must not be blank for source: " + sourceId + "\n" +
                "Check the 'url' field in biblios.yml for content.sources id='" + sourceId + "'."
            );
        }

        repoPath = cacheRoot.resolve("repos").resolve(safeDirectoryName(sourceId));
        Files.createDirectories(repoPath);

        Path gitDir = repoPath.resolve(".git");
        if (Files.exists(gitDir)) {
            // Repository exists, fetch updates
            System.out.println("[info] Fetching existing repository for: " + sourceId);
            openAndFetch();
        } else {
            // Clone fresh
            System.out.println("[info] Cloning repository: " + remoteUrl);
            cloneRepository(remoteUrl);
        }

        return this;
    }

    /**
     * Checkout a specific branch.
     *
     * @param branchName the branch to checkout
     * @return this resolver for chaining
     * @throws IOException if checkout fails
     */
    public GitSourceResolver checkout(String branchName) throws IOException {
        ensureRepository();
        try {
            CheckoutCommand checkout = git.checkout();
            checkout.setName(branchName);

            // Try creating the branch locally if it doesn't exist
            checkout.setCreateBranch(false);
            try {
                checkout.call();
            } catch (org.eclipse.jgit.api.errors.RefNotFoundException e) {
                // Try as a remote tracking branch
                System.out.println("[info] Creating local branch from remote: " + branchName);
                checkout.setCreateBranch(true);
                checkout.call();
            }
        } catch (Exception e) {
            throw new IOException(
                "Failed to checkout branch '" + branchName + "' in source: " + e.getMessage() + "\n" +
                "Check that the branch exists in the repository.\n" +
                "Available branches can be checked with: git branch -a",
                e
            );
        }
        return this;
    }

    /**
     * Get the path to the working directory of the checked out repository.
     */
    public Path workTree() {
        ensureRepository();
        return repoPath;
    }

    /**
     * Check if a branch exists in the repository.
     *
     * @param branchName the branch name to check
     * @return true if the branch exists (local or remote)
     */
    public boolean branchExists(String branchName) {
        ensureRepository();
        try {
            // Check local branches
            for (Ref ref : git.branchList().call()) {
                String name = ref.getName();
                if (name.equals("refs/heads/" + branchName)) {
                    return true;
                }
            }
            // Check remote branches
            for (Ref ref : git.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()) {
                String name = ref.getName();
                // refs/remotes/origin/main -> main
                if (name.startsWith("refs/remotes/origin/")) {
                    String remoteBranch = name.substring("refs/remotes/origin/".length());
                    if (remoteBranch.equals(branchName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("[warn] Failed to check branches: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (git != null) {
            git.close();
            git = null;
        }
    }

    // Private helper methods

    private void cloneRepository(String remoteUrl) throws IOException {
        try {
            CloneCommand clone = Git.cloneRepository();
            clone.setURI(remoteUrl);
            clone.setDirectory(repoPath.toFile());
            clone.setCloneAllBranches(true);
            git = clone.call();
        } catch (Exception e) {
            String hint = remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")
                ? "For HTTPS repos, check network access and authentication."
                : remoteUrl.startsWith("file://") || remoteUrl.startsWith("/")
                    ? "For local repos, verify the path exists and is a valid Git repo."
                    : "Check the URL format (supported: https://, file://, git@).";
            throw new IOException(
                "Failed to clone repository '" + remoteUrl + "': " + e.getMessage() + "\n" +
                hint,
                e
            );
        }
    }

    private void openAndFetch() throws IOException {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            builder.setGitDir(repoPath.resolve(".git").toFile());
            builder.readEnvironment();
            builder.findGitDir();

            Repository repository = builder.build();
            git = new Git(repository);

            FetchCommand fetch = git.fetch();
            fetch.setRemote("origin");
            fetch.call();
        } catch (Exception e) {
            System.err.println("[warn] Failed to fetch repository: " + e.getMessage());
            System.err.println("[warn] Continuing with cached content.");
            // Try to open the repository even if fetch failed
            try {
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                builder.setGitDir(repoPath.resolve(".git").toFile());
                Repository repository = builder.build();
                git = new Git(repository);
            } catch (Exception ex) {
                throw new IOException("Failed to open cached repository: " + ex.getMessage(), ex);
            }
        }
    }

    private void ensureRepository() {
        if (git == null) {
            throw new IllegalStateException("Repository not resolved. Call resolve() first.");
        }
    }

    /**
     * Create a safe directory name from a source ID.
     * Replaces problematic characters with underscores.
     */
    private static String safeDirectoryName(String sourceId) {
        return sourceId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
