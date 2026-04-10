package guru.interlis.thoth.biblios.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AsciidoctorRenderer.
 */
class AsciidoctorRendererTest {

    @Test
    void rendersSimpleContent(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Files.writeString(adoc, "= Hello\n\nThis is a test.");

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString("= Hello\n\nThis is a test.");
            assertNotNull(html);
            assertTrue(html.contains("Hello") || html.contains("This is a test"));
        }
    }

    @Test
    void rendersSectionsAndLists(@TempDir Path tempDir) throws Exception {
        String content = """
            = User Guide

            == Introduction

            This is the introduction.

            == Features

            * Feature 1
            * Feature 2
            * Feature 3
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertNotNull(html);
            assertTrue(html.contains("User Guide") || html.contains("Introduction"));
        }
    }

    @Test
    void rendersCodeBlocks() {
        String content = """
            = Code Example

            [source,java]
            ----
            public class Hello {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertNotNull(html);
        }
    }

    @Test
    void handlesEmptyContent() {
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString("");
            // Empty content may return null or empty string
            assertNotNull(html);
        }
    }

    @Test
    void extractsHeadingsFromRenderedDocument(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("master.adoc");
        Files.writeString(adoc, """
            = Manual
            :doctype: book

            == Einleitung

            === Status

            OK
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.singlePage(false, 3)
            );

            assertNotNull(rendered);
            assertFalse(rendered.headings().isEmpty());
            assertEquals("Einleitung", rendered.headings().get(0).title());
            assertEquals("1", rendered.headings().get(0).sectionNumber());
            assertEquals(1, rendered.headings().get(0).level());
            assertFalse(rendered.headings().get(0).children().isEmpty());
            assertEquals("Status", rendered.headings().get(0).children().get(0).title());
            assertEquals("1.1", rendered.headings().get(0).children().get(0).sectionNumber());
            assertFalse(rendered.html().contains("id=\"toc\""));
            assertTrue(rendered.html().contains("class=\"anchor\""));
            assertTrue(rendered.html().matches("(?s).*class=\"anchor\"\\s+href=\"#.+?\".*"));
        }
    }

    @Test
    void keepsUnnumberedSectionsWithoutSectionNumber(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("master.adoc");
        Files.writeString(adoc, """
            = Manual
            :doctype: book

            [unnumbered]
            == Vorwort

            == Einleitung
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.singlePage(false, 2)
            );

            assertEquals("Vorwort", rendered.headings().get(0).title());
            assertTrue(rendered.headings().get(0).sectionNumber().isBlank());
            assertEquals("Einleitung", rendered.headings().get(1).title());
            assertEquals("1", rendered.headings().get(1).sectionNumber());
        }
    }

    @Test
    void rendersNoteAdmonitionMarkup() {
        String content = """
            = Notes

            [NOTE]
            ====
            This is a note block.
            ====
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            String normalized = html.toLowerCase(Locale.ROOT);

            assertTrue(normalized.contains("class=\"admonitionblock note\""));
            assertTrue(normalized.contains("this is a note block"));
            assertEquals("Note", extractNoteCaption(html));
        }
    }

    @Test
    void localizesNoteCaptionFromLanguageAttribute() {
        String content = """
            = Hinweise

            [NOTE]
            ====
            Sprachtest.
            ====
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String htmlEn = renderer.renderString(content, "en");
            String htmlDe = renderer.renderString(content, "de");

            assertEquals("Note", extractNoteCaption(htmlEn));
            assertEquals("Hinweis", extractNoteCaption(htmlDe));
        }
    }

    private String extractNoteCaption(String html) {
        Pattern iconTitleAttribute = Pattern.compile(
            "(?is)<td\\s+class=\"icon\"[^>]*>.*?<i[^>]*\\btitle=\"([^\"]+?)\"[^>]*>"
        );
        Matcher iconTitleMatcher = iconTitleAttribute.matcher(html);
        if (iconTitleMatcher.find()) {
            return iconTitleMatcher.group(1).trim();
        }

        Pattern titleInIconCell = Pattern.compile(
            "(?is)<td\\s+class=\"icon\"[^>]*>\\s*<div\\s+class=\"title\">\\s*([^<]+?)\\s*</div>"
        );
        Matcher iconCellMatcher = titleInIconCell.matcher(html);
        if (iconCellMatcher.find()) {
            return iconCellMatcher.group(1).trim();
        }

        Pattern titleInBlock = Pattern.compile(
            "(?is)<div\\s+class=\"admonitionblock\\s+note\"[^>]*>.*?<div\\s+class=\"title\">\\s*([^<]+?)\\s*</div>"
        );
        Matcher blockMatcher = titleInBlock.matcher(html);
        assertTrue(blockMatcher.find(), "Expected NOTE caption in admonition block.");
        return blockMatcher.group(1).trim();
    }
}
