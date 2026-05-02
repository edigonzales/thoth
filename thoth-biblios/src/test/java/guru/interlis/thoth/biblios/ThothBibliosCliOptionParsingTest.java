package guru.interlis.thoth.biblios;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThothBibliosCliOptionParsingTest {

    @Test
    void buildParsesFormatPdf() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml",
            "--format", "pdf",
            "--pdf-version", "main",
            "--pdf-version", "docs/v1.x"
        );

        assertTrue(parseResult.subcommand().hasMatchedOption("--format"));
        assertTrue(parseResult.subcommand().hasMatchedOption("--pdf-version"));
    }

    @Test
    void buildParsesFormatDocx() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml",
            "--format", "docx",
            "--docx-version", "main",
            "--docx-version", "docs/v1.x"
        );

        assertTrue(parseResult.subcommand().hasMatchedOption("--format"));
        assertTrue(parseResult.subcommand().hasMatchedOption("--docx-version"));
    }

    @Test
    void buildParsesCombinedFormats() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml",
            "--format", "html,pdf,docx",
            "--pdf-version", "main",
            "--docx-version", "main"
        );

        assertTrue(parseResult.subcommand().hasMatchedOption("--format"));
        assertTrue(parseResult.subcommand().hasMatchedOption("--pdf-version"));
        assertTrue(parseResult.subcommand().hasMatchedOption("--docx-version"));
    }

    @Test
    void buildUsesHtmlDefaultWhenFormatMissing() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml"
        );

        assertFalse(parseResult.subcommand().hasMatchedOption("--format"));
        assertFalse(parseResult.subcommand().hasMatchedOption("--pdf-version"));
        assertFalse(parseResult.subcommand().hasMatchedOption("--docx-version"));
    }

    @Test
    void buildRejectsPdfVersionWithoutPdfFormat() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--pdf-version", "main"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsDocxVersionWithoutDocxFormat() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--docx-version", "main"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsDocxFormatWithoutDocxVersion() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--format", "docx"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsUnknownFormat() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--format", "epub"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsRemovedPdfFlag() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--pdf"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsRemovedDocxFlag() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--docx"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void serveParsesUseLocalWorkingTreeFlag() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "serve",
            "--config", "biblios.yml",
            "--use-local-working-tree"
        );

        assertTrue(parseResult.subcommand().hasMatchedOption("--use-local-working-tree"));
    }

    @Test
    void serveLeavesUseLocalWorkingTreeDisabledByDefault() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "serve",
            "--config", "biblios.yml"
        );

        assertFalse(parseResult.subcommand().hasMatchedOption("--use-local-working-tree"));
    }

    @Test
    void serveRejectsFormatOption() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "serve",
            "--config", "biblios.yml",
            "--format", "html"
        );

        assertEquals(2, exitCode);
    }
}
