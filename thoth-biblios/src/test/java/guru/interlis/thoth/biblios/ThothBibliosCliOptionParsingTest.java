package guru.interlis.thoth.biblios;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThothBibliosCliOptionParsingTest {

    @Test
    void buildParsesPdfFlags() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml",
            "--pdf",
            "--pdf-version", "main",
            "--pdf-version", "docs/v1.x"
        );

        assertTrue(parseResult.subcommand().hasMatchedOption("--pdf"));
        assertTrue(parseResult.subcommand().hasMatchedOption("--pdf-version"));
    }

    @Test
    void buildParsesDocxFlags() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml",
            "--docx",
            "--docx-version", "main",
            "--docx-version", "docs/v1.x"
        );

        assertTrue(parseResult.subcommand().hasMatchedOption("--docx"));
        assertTrue(parseResult.subcommand().hasMatchedOption("--docx-version"));
    }

    @Test
    void buildLeavesPdfDisabledByDefault() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        CommandLine.ParseResult parseResult = cli.parseArgs(
            "build",
            "--config", "biblios.yml"
        );

        assertFalse(parseResult.subcommand().hasMatchedOption("--pdf"));
        assertFalse(parseResult.subcommand().hasMatchedOption("--pdf-version"));
        assertFalse(parseResult.subcommand().hasMatchedOption("--docx"));
        assertFalse(parseResult.subcommand().hasMatchedOption("--docx-version"));
    }

    @Test
    void buildRejectsPdfVersionWithoutPdfFlag() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--pdf-version", "main"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsDocxVersionWithoutDocxFlag() {
        CommandLine cli = new CommandLine(new ThothBibliosCli());
        int exitCode = cli.execute(
            "build",
            "--config", "missing.yml",
            "--docx-version", "main"
        );

        assertEquals(2, exitCode);
    }

    @Test
    void buildRejectsDocxFlagWithoutDocxVersion() {
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
}
