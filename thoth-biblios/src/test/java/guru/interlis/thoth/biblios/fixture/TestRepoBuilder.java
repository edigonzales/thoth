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
     * Create a basic split-docs repo with internal cross references that should be rewritten by Biblios.
     */
    public TestRepoBuilder withBasicDocsAndCrossReferences() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path docsDir = repoDir.resolve("docs");

            Files.writeString(docsDir.resolve("index.adoc"), """
                = Welcome
                :doctype: book

                xref:guide.adoc[Guide via xref]

                link:guide.html[Guide via html]

                link:/docs/main/guide/[Guide via absolute route]

                xref:guide.adoc#configuration[Guide section]
                """);

            Files.writeString(docsDir.resolve("guide.adoc"), """
                = User Guide
                :doctype: book

                xref:config.adoc[Config sibling]

                [#configuration]
                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add cross reference fixture").call();
        }

        return this;
    }

    public TestRepoBuilder withInterlisLabInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path docsDir = repoDir.resolve("docs");
            Files.writeString(docsDir.resolve("guide.adoc"), """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                interlis-lab::labs/simple.json[storage-key=biblios-simple,title="Simple Lab"]
                """);
            Files.createDirectories(docsDir.resolve("labs"));
            Files.writeString(docsDir.resolve("labs/simple.json"), "{\"id\":\"simple\"}");

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add INTERLIS lab fixture").call();
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
     * Extend the basic docs fixture with all standard AsciiDoc admonitions in guide.adoc.
     */
    public TestRepoBuilder withAllStandardAdmonitionsInGuide() throws Exception {
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
                Note block for icon-title rendering.
                ====

                [TIP]
                ====
                Tip block for icon-title rendering.
                ====

                [IMPORTANT]
                ====
                Important block for icon-title rendering.
                ====

                [WARNING]
                ====
                Warning block for icon-title rendering.
                ====

                [CAUTION]
                ====
                Caution block for icon-title rendering.
                ====

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add standard admonitions to guide").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with a sidebar block (****) in guide.adoc.
     */
    public TestRepoBuilder withSidebarBlockInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path guide = repoDir.resolve("docs/guide.adoc");
            Files.writeString(guide, """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                Follow these steps to install the software.

                ****
                This guide includes a sidebar block for UI styling tests.
                ****

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add sidebar block to guide").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with a keypoint sidebar block in guide.adoc.
     */
    public TestRepoBuilder withKeypointSidebarBlockInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path guide = repoDir.resolve("docs/guide.adoc");
            Files.writeString(guide, """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                Follow these steps to install the software.

                [.keypoint]
                ****
                This guide includes a keypoint sidebar block for UI styling tests.
                ****

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add keypoint sidebar block to guide").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with a listing callout and colist legend in guide.adoc.
     */
    public TestRepoBuilder withListingCalloutInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path guide = repoDir.resolve("docs/guide.adoc");
            Files.writeString(guide, """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                .Syntaxregeln:
                ----
                ClassDef = 'CLASS' Class-Name
                         [ 'EXTENDS' ClassRef ] '=' <1>
                ----

                <1> Im PDF-Dokument des Referenzhandbuchs (Version 2.1.0) steht an dieser Stelle ClassOrStructureRef.

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add listing callout to guide").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with a source listing callout and colist legend in guide.adoc.
     */
    public TestRepoBuilder withSourceListingCalloutInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path guide = repoDir.resolve("docs/guide.adoc");
            Files.writeString(guide, """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                [source,interlis]
                ----
                ClassDef = 'CLASS' Class-Name
                         [ 'EXTENDS' ClassRef ] '=' <1>
                ----

                <1> Im PDF-Dokument des Referenzhandbuchs (Version 2.1.0) steht an dieser Stelle ClassOrStructureRef.

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add source listing callout to guide").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with a table and caption in guide.adoc.
     */
    public TestRepoBuilder withTableInGuide() throws Exception {
        withBasicDocs();

        try (Git git = Git.open(repoDir.toFile())) {
            Path guide = repoDir.resolve("docs/guide.adoc");
            Files.writeString(guide, """
                = User Guide
                :doctype: book

                This is the user guide.

                == Installation

                Follow these steps to install the software.

                .Deployment Matrix
                |===
                |Environment |Status

                |Development
                |Ready

                |Production
                |Planned
                |===

                == Configuration

                Configure the software to your needs.
                """);

            git.add().addFilepattern("docs/guide.adoc").call();
            git.commit().setMessage("Add table with caption to guide").call();
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

                [unnumbered]
                [.appendix]
                == Anhang A - foo bar

                Im TOC anzeigen.
                
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
     * Extend the basic docs fixture with local image references in split mode.
     */
    public TestRepoBuilder withSplitDocsReferencingImages() throws Exception {
        withBasicDocs();

        Path docsDir = repoDir.resolve("docs");
        Path imagesDir = docsDir.resolve("images");
        Files.createDirectories(imagesDir);
        Files.writeString(imagesDir.resolve("foo.png"), "fake-png-foo");

        Files.writeString(docsDir.resolve("index.adoc"), """
            = Welcome
            :doctype: book
            :imagesdir: images

            This page references a local image.

            image::foo.png[]
            """);

        try (Git git = Git.open(repoDir.toFile())) {
            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add split docs image fixture").call();
        }

        return this;
    }

    /**
     * Extend the basic docs fixture with external and missing image references.
     */
    public TestRepoBuilder withSplitDocsExternalAndMissingImages() throws Exception {
        withBasicDocs();

        Path docsDir = repoDir.resolve("docs");
        Files.writeString(docsDir.resolve("index.adoc"), """
            = Welcome
            :doctype: book
            :imagesdir: images

            image::missing.png[]
            ++++
            <img src="https://example.org/logo.png" alt="external logo">
            ++++
            """);

        try (Git git = Git.open(repoDir.toFile())) {
            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add external/missing image fixture").call();
        }

        return this;
    }

    /**
     * Create a single-page fixture that references an image from an included chapter.
     */
    public TestRepoBuilder withSinglePageDocsWithImages() throws Exception {
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(initialBranch).call()) {
            configureUser(git);

            Path docsDir = repoDir.resolve("docs");
            Path imagesDir = docsDir.resolve("images");
            Files.createDirectories(imagesDir);
            Files.writeString(imagesDir.resolve("single.png"), "fake-png-single");

            Files.writeString(docsDir.resolve("master.adoc"), """
                = Reference Manual
                :imagesdir: images

                include::chapter.adoc[]
                """);

            Files.writeString(docsDir.resolve("chapter.adoc"), """
                == Kapitel

                image::single.png[]
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Add single-page image fixture").call();
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
     * Create a single-page fixture with section nesting up to sect5.
     */
    public TestRepoBuilder withSinglePageDocsUpToSect5() throws Exception {
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(initialBranch).call()) {
            configureUser(git);

            Path docsDir = repoDir.resolve("docs");
            Files.createDirectories(docsDir);

            Files.writeString(docsDir.resolve("master.adoc"), """
                = Reference Manual

                == Level One

                === Level Two

                ==== Level Three

                ===== Level Four

                ====== Level Five

                Nested depth content.
                """);

            git.add().addFilepattern("docs/").call();
            git.commit().setMessage("Initial single-page depth fixture").call();
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
