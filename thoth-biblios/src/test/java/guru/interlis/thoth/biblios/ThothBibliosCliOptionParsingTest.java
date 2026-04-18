package guru.interlis.thoth.biblios;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThothBibliosCliOptionParsingTest {

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
