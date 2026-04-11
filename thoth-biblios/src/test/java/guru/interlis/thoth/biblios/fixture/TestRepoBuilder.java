package guru.interlis.thoth.biblios.fixture;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for creating local Git repositories for testing.
 * Creates repos with AsciiDoc content, nav.yml, and multiple branches.
 */
public final class TestRepoBuilder {

    private final Path repoDir;
    private String initialBranch = "main";

    public TestRepoBuilder(Path repoDir) {
        this.repoDir = repoDir;
    }

    public TestRepoBuilder withInitialBranch(String branch) {
        this.initialBranch = branch;
        return this;
    }

    /**
     * Create a basic documentation repo with nav.yml and AsciiDoc files.
     */
    public TestRepoBuilder withBasicDocs() throws Exception {
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(initialBranch).call()) {
            configureUser(git);

            Path docsDir = repoDir.resolve("docs");
            Files.createDirectories(docsDir);

            // nav.yml
            String nav = """
                items:
                  - title: Welcome
                    page: index.adoc
                  - title: User Guide
                    page: guide.adoc
                  - title: Advanced
                    children:
                      - title: Configuration
                        page: config.adoc
                      - title: API Reference
                        page: api.adoc
                """;
            Files.writeString(docsDir.resolve("nav.yml"), nav);

            // index.adoc
            Files.writeString(docsDir.resolve("index.adoc"), """
                = Welcome
                :doctype: book

                This is the main documentation index.

                == Getting Started

                Welcome to our project. This documentation will help you get started.
                """);

            // guide.adoc
            Files.writeString(docsDir.resolve("guide.adoc"), """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                Follow these steps to install the software.

                == Configuration

                Configure the software to your needs.
                """);

            // config.adoc
            Files.writeString(docsDir.resolve("config.adoc"), """
                = Configuration
                :doctype: book

                Advanced configuration options.

                == Settings

                Configure settings in your config file.
                """);

            // api.adoc
            Files.writeString(docsDir.resolve("api.adoc"), """
                = API Reference
                :doctype: book

                API reference documentation.

                == Endpoints

                List of available API endpoints.
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Initial commit").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with a NOTE admonition in guide.adoc.
     */
    public TestRepoBuilder withNoteAdmonitionInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path guide = repoDir.resolve("docs/guide.adoc");
            Files.writeString(guide, """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                Follow these steps to install the software.

                [NOTE]
                ====
                This guide includes an important note for localization tests.
                ====

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add NOTE admonition to guide").call();
        }

        return this;
    }

    /**
     * Create a repo configured for single-page rendering via master.adoc includes.
     */
    public TestRepoBuilder withSinglePageDocs() throws Exception {
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(initialBranch).call()) {
            configureUser(git);

            Path docsDir = repoDir.resolve("docs");
            Files.createDirectories(docsDir);

            Files.writeString(docsDir.resolve("master.adoc"), """
                = Reference Manual

                include::vorwort.adoc[]
                include::einleitung.adoc[]
                include::grundprinzipien.adoc[]
                """);

            Files.writeString(docsDir.resolve("vorwort.adoc"), """
                Dieses Vorwort ist absichtlich ohne eigene Kapitelueberschrift.
                """);

            Files.writeString(docsDir.resolve("einleitung.adoc"), """
                == Einleitung

                === Status

                Genehmigt.
                """);

            Files.writeString(docsDir.resolve("grundprinzipien.adoc"), """
                == Grundprinzipien

                === Modellierung

                Grundlagen der Modellierung.
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Initial single-page docs").call();
        }

        return this;
    }

    /**
     * Create a repo for single-page mode with an unnumbered chapter subtree.
     */
    public TestRepoBuilder withSinglePageDocsIncludingUnnumberedChapter() throws Exception {
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(initialBranch).call()) {
            configureUser(git);

            Path docsDir = repoDir.resolve("docs");
            Files.createDirectories(docsDir);

            Files.writeString(docsDir.resolve("master.adoc"), """
                = Reference Manual
                :doctype: book

                == Einleitung

                === Status

                Genehmigt.
                
                [unnumbered]
                == Erweiterungen von INTERLIS 2.4 gegenüber INTERLIS 2.3

                Nicht im TOC anzeigen.
                
                == Grundprinzipien

                === Modellierung

                Grundlagen der Modellierung.
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Single-page docs with unnumbered chapter").call();
        }

        return this;
    }

    /**
     * Create a repo with a single master file where section numbers are available from Asciidoctor AST.
     */
    public TestRepoBuilder withSinglePageNumberedDocs() throws Exception {
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(initialBranch).call()) {
            configureUser(git);

            Path docsDir = repoDir.resolve("docs");
            Files.createDirectories(docsDir);

            Files.writeString(docsDir.resolve("master.adoc"), """
                = Reference Manual

                == Einleitung

                === Status

                == Grundprinzipien
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Initial single-page numbered docs").call();
        }

        return this;
    }

    /**
     * Create a second branch with different content.
     */
    public TestRepoBuilder withSecondBranch(String branchName) throws Exception {
        try (Git git = Git.open(repoDir.toFile())) {
            git.checkout().setCreateBranch(true).setName(branchName).call();

            Path docsDir = repoDir.resolve("docs");

            // Override guide.adoc for this branch
            Files.writeString(docsDir.resolve("guide.adoc"), """
                = User Guide (%s)
                :doctype: book

                This is the %s user guide.

                == Installation

                Installation instructions for %s.
                """.formatted(branchName, branchName, branchName));

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Update for " + branchName).call();

            // Switch back to initial branch
            git.checkout().setName(initialBranch).call();
        }

        return this;
    }

    /**
     * Add a second documentation root with different content.
     */
    public TestRepoBuilder withAltDocs(Path altDocsDir, String altNav, String altIndex) throws Exception {
        Files.createDirectories(altDocsDir);

        Files.writeString(altDocsDir.resolve("nav.yml"), altNav);
        Files.writeString(altDocsDir.resolve("index.adoc"), altIndex);

        try (Git git = Git.open(repoDir.toFile())) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Add alt docs").call();
        }

        return this;
    }

    /**
     * Get the repository directory.
     */
    public Path getRepoDir() {
        return repoDir;
    }

    private void configureUser(Git git) throws Exception {
        StoredConfig config = git.getRepository().getConfig();
        config.setString("user", null, "name", "Test User");
        config.setString("user", null, "email", "test@example.org");
        config.save();
    }
}
