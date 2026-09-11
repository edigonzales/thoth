package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.*;
import guru.interlis.thoth.biblios.config.*;
import guru.interlis.thoth.core.ThothBuildException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BibliosGroupsTest {
    @TempDir Path temp;

    private BibliosConfig config(String groups) throws Exception {
        Path file = temp.resolve("biblios.yml");
        Files.writeString(file, """
            site:
              title: Group Test
            output:
              dir: site
            content:
            """ + groups + """
              sources:
                - id: alpha
                  display_name: Alpha
                  card_background_color: "#e8f1ff"
                  url: https://example.org/alpha.git
                  branches: [{name: main}]
                - id: beta
                  display_name: Beta
                  url: https://example.org/beta.git
                  branches: [{name: main}]
                - id: gamma
                  display_name: Gamma
                  url: https://example.org/gamma.git
                  branches: [{name: main}]
            """);
        return new BibliosConfigParser().parse(file);
    }

    private Path generate(String groups, List<String> available) throws Exception {
        BibliosConfig config = config(groups);
        List<DocComponent> components = available.stream().map(id -> {
            SourceConfig source = config.content().sources().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
            return new DocComponent(id, source.displayName(), "main",
                List.of(new ComponentVersion(id, "main", "Aktuell", "main", "index.adoc", null,
                    List.of(new DocPage(id, "main", "index.adoc", null, id + "-index",
                        source.displayName(), source.displayName(), "/" + id + "/main/",
                        "<p>Documentation</p>", List.of(), null, null)))),
                source.cardBackgroundColor());
        }).toList();
        Path output = temp.resolve("site");
        try (var generator = new BibliosSiteGenerator(config, new SiteCatalog(components), output)) {
            generator.generate();
        }
        return output;
    }

    private Document read(Path output, String file) throws Exception {
        return Jsoup.parse(Files.readString(output.resolve(file)));
    }

    @Test
    void absentAndEmptyGroupsKeepFlatRendering() throws Exception {
        for (String groups : List.of("", "  groups: []\n")) {
            Path output = generate(groups, List.of("alpha", "beta", "gamma"));
            Document home = read(output, "index.html");
            assertEquals(List.of("Alpha", "Beta", "Gamma"), home.select(".component-card h2").eachText());
            assertEquals(1, home.select(".components-grid").size());
            assertTrue(home.select(".component-group, #doc-switch optgroup").isEmpty());
            assertFalse(home.text().contains("Weitere"));
            assertEquals(3, home.select("#doc-switch > option:not([disabled])").size());
            assertTrue(read(output, "alpha/main/index.html").select("#doc-switch optgroup").isEmpty());
        }
    }

    @Test
    void groupsShareOrderAcrossPagesAndSelectOnlyFirstActiveOccurrence() throws Exception {
        String groups = """
              groups:
                - title: 'Grundlagen <img src=x> & "Text"'
                  sources: [beta, alpha]
                - title: Entwicklung
                  sources: [alpha]
            """;
        Path output = generate(groups, List.of("alpha", "beta", "gamma"));
        Document home = read(output, "index.html");
        assertEquals(List.of("Grundlagen <img src=x> & \"Text\"", "Entwicklung", "Weitere"),
            home.select(".component-group-title").eachText());
        assertEquals(List.of("Beta", "Alpha", "Alpha", "Gamma"), home.select(".component-card h3").eachText());
        assertTrue(home.select(".component-group-title img").isEmpty());
        assertEquals(4, home.select(".version-tag").size());
        assertEquals(2, home.select(".component-card[style]").size());
        for (String file : List.of("index.html", "search/index.html", "alpha/index.html", "alpha/main/index.html")) {
            Document page = read(output, file);
            assertEquals(List.of("Grundlagen <img src=x> & \"Text\"", "Entwicklung", "Weitere"),
                page.select("#doc-switch optgroup").eachAttr("label"));
            assertEquals(List.of("Beta", "Alpha", "Alpha", "Gamma"),
                page.select("#doc-switch optgroup option").eachText());
            if (file.startsWith("alpha/")) {
                assertEquals(1, page.select("#doc-switch option[selected]").size());
                assertTrue(page.selectFirst("#doc-switch optgroup").select("option").get(1).hasAttr("selected"));
            }
        }
    }

    @Test
    void unavailableGroupsAndEmptyRemainderAreOmitted() throws Exception {
        Path output = generate("""
              groups:
                - title: Missing
                  sources: [beta]
                - title: Available
                  sources: [alpha]
            """, List.of("alpha"));
        Document home = read(output, "index.html");
        assertEquals(List.of("Available"), home.select(".component-group-title").eachText());
        assertEquals(List.of("Available"), home.select("#doc-switch optgroup").eachAttr("label"));
        assertFalse(home.text().contains("Weitere"));
    }

    @Test
    void rejectsInvalidGroups() {
        for (String value : List.of(
            "null", "{}", "[null]", "[{title: ' ', sources: [alpha]}]",
            "[{title: 123, sources: [alpha]}]", "[{title: Test, sources: []}]",
            "[{title: Test, sources: [unknown]}]", "[{title: Test, sources: [alpha, alpha]}]",
            "[{title: Test, sources: [123]}]", "[{title: Test, sources: alpha}]", "[wrong]")) {
            ThothBuildException error = assertThrows(ThothBuildException.class,
                () -> config("  groups: " + value + "\n"), value);
            assertTrue(error.getMessage().contains("content.groups"), error.getMessage());
        }
    }
}
