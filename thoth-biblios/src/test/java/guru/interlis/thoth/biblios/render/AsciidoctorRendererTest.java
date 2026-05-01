package guru.interlis.thoth.biblios.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
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
            assertTrue(html.contains("language-java"));
        }
    }

    @Test
    void normalizesCodeBlockLanguagesForPrism() {
        String content = """
            = Language Mapping

            [source,xml]
            ----
            <a>foo</a>
            ----

            [source,interlis]
            ----
            MODEL Demo;
            END Demo.
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertNotNull(html);
            assertTrue(html.contains("language-markup"));
            assertTrue(html.contains("language-interlis"));
        }
    }

    @Test
    void rendersNumericConumsInsideSourceListings() {
        String content = """
            = Listing Callout

            [source,interlis]
            ----
            ClassDef = 'CLASS' Class-Name
                     [ 'EXTENDS' ClassRef ] '=' <1>
            ----

            <1> ClassOrStructureRef.
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertNotNull(html);

            org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(html);
            Element listing = document.selectFirst(".listingblock");
            assertNotNull(listing);
            assertNotNull(listing.selectFirst("i.conum[data-value=1]"));
            assertNotNull(listing.selectFirst("pre > code.language-interlis"), listing.outerHtml());
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
    void writesPdfDocument(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Path pdf = tempDir.resolve("test.pdf");
        Files.writeString(adoc, """
            = PDF Test
            :doctype: book

            Hello PDF.
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            renderer.writePdf(adoc, pdf, Map.of(), "en");
        }

        assertTrue(Files.exists(pdf));
        byte[] prefix = Files.readAllBytes(pdf);
        assertTrue(prefix.length > 5);
        assertEquals("%PDF-", new String(prefix, 0, 5));
    }

    @Test
    void writesPdfDocumentWithSemanticAttributes(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Path pdf = tempDir.resolve("test.pdf");
        Files.writeString(adoc, """
            = PDF Test
            :doctype: book

            == Introduction

            Hello PDF.
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            renderer.writePdf(adoc, pdf, Map.of(
                "toc", true,
                "sectnums", true,
                "chapter-signifier", false
            ), "en");
        }

        assertTrue(Files.exists(pdf));
        byte[] prefix = Files.readAllBytes(pdf);
        assertTrue(prefix.length > 5);
        assertEquals("%PDF-", new String(prefix, 0, 5));
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
            assertTrue(rendered.headings().get(0).unnumbered());
            assertFalse(rendered.headings().get(0).appendix());
            assertEquals("Einleitung", rendered.headings().get(1).title());
            assertEquals("1", rendered.headings().get(1).sectionNumber());
            assertFalse(rendered.headings().get(1).unnumbered());
            assertFalse(rendered.headings().get(1).appendix());
        }
    }

    @Test
    void marksAppendixRoleOnUnnumberedSections(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("master.adoc");
        Files.writeString(adoc, """
            = Manual
            :doctype: book

            [unnumbered]
            [.appendix]
            == Anhang A - foo bar
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.singlePage(false, 2)
            );

            assertEquals("Anhang A - foo bar", rendered.headings().get(0).title());
            assertTrue(rendered.headings().get(0).unnumbered());
            assertTrue(rendered.headings().get(0).appendix());
            assertTrue(rendered.headings().get(0).sectionNumber().isBlank());
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
    void doesNotForceCustomNoteCaptionFromLanguageAttribute() {
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
            assertEquals("Note", extractNoteCaption(htmlDe));
        }
    }

    @Test
    void keepsDocumentLevelNoteCaptionOverride() {
        String content = """
            = Hinweise
            :note-caption: Merke

            [NOTE]
            ====
            Sprachtest.
            ====
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content, "de");
            assertEquals("Merke", extractNoteCaption(html));
        }
    }

    @Test
    void rendersSidebarBlockMarkup() {
        String content = """
            = Sidebar

            ****
            This is a sidebar block.
            ****
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            String normalized = html.toLowerCase(Locale.ROOT);

            assertTrue(normalized.contains("class=\"sidebarblock\""));
            assertTrue(normalized.contains("this is a sidebar block"));
        }
    }

    @Test
    void rendersParagraphRoleSmallMarkup() {
        String content = """
            = Small Paragraph

            [.small]
            This paragraph should render with the small role.
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertTrue(html.contains("This paragraph should render with the small role."));
            assertTrue(html.matches("(?s).*class=\"[^\"]*paragraph[^\"]*small[^\"]*\".*"));
        }
    }

    @Test
    void marksStandaloneMarkerParagraphBeforeListingBlock() {
        String content = """
            = Marker

            ※

            .Syntaxregeln:
            ----
            Name = Letter { Letter | Digit | '_' }.
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertTrue(html.contains("class=\"paragraph marker-paragraph\""));
            assertTrue(html.contains("<p>※</p>"));
            assertTrue(html.contains("listingblock"));
            assertTrue(html.matches("(?s).*class=\"[^\"]*listingblock[^\"]*marker-following-marker[^\"]*\".*"));
        }
    }

    @Test
    void marksSingleLinkMarkerParagraphBeforeListingBlock() {
        String content = """
            = Marker

            [[syntax-anchor]]
            == Ziel

            <<syntax-anchor,※>>

            .Syntaxregeln:
            ----
            Name = Letter { Letter | Digit | '_' }.
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertTrue(html.contains("class=\"paragraph marker-paragraph\""));
            assertTrue(html.contains(">※</a>"));
            assertTrue(html.matches("(?s).*class=\"[^\"]*listingblock[^\"]*marker-following-marker[^\"]*\".*"));
        }
    }

    @Test
    void marksStandaloneMarkerParagraphBeforeSyntaxParagraphAndListingBlock() {
        String content = """
            = Marker

            ※

            Syntaxregeln:

            ----
            Name = Letter { Letter | Digit | '_' }.
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertTrue(html.contains("class=\"paragraph marker-paragraph\""));
            assertTrue(html.contains("<p>Syntaxregeln:</p>"));
            assertTrue(html.contains("listingblock"));
            assertTrue(html.matches("(?s).*class=\"[^\"]*listingblock[^\"]*marker-following-marker[^\"]*\".*"));
        }
    }

    @Test
    void marksStandaloneAnchorMarkerLinkedToRawHtmlListingBlock() {
        String content = """
            = Marker

            ++++
            <div class="sect2">
              <a href="#3_3_C1">※</a>
              <div id="2_3_C1"></div>
              <div id="3_3_C1" class="listingblock">
                <div class="title">Syntaxregel:</div>
                <div class="content">
                  <pre>INTERLIS2Def = 'INTERLIS' Version-Dec ';'</pre>
                </div>
              </div>
            </div>
            ++++
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertTrue(html.matches("(?s).*<a[^>]*class=\"[^\"]*marker-anchor[^\"]*\"[^>]*href=\"#3_3_C1\"[^>]*>※</a>.*")
                || html.matches("(?s).*<a[^>]*href=\"#3_3_C1\"[^>]*class=\"[^\"]*marker-anchor[^\"]*\"[^>]*>※</a>.*"));
            assertTrue(html.matches("(?s).*<div[^>]*id=\"3_3_C1\"[^>]*class=\"[^\"]*listingblock[^\"]*marker-following-marker[^\"]*\"[^>]*>.*")
                || html.matches("(?s).*<div[^>]*class=\"[^\"]*listingblock[^\"]*marker-following-marker[^\"]*\"[^>]*id=\"3_3_C1\"[^>]*>.*"));
        }
    }

    @Test
    void doesNotMarkNormalParagraphBeforeListingBlock() {
        String content = """
            = Marker

            Hinweis.

            .Syntaxregeln:
            ----
            Name = Letter { Letter | Digit | '_' }.
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertFalse(html.contains("class=\"paragraph marker-paragraph\""));
        }
    }

    @Test
    void doesNotMarkMarkerParagraphWithoutFollowingListingBlock() {
        String content = """
            = Marker

            ※

            Nachfolgender Fliesstext.
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertFalse(html.contains("class=\"paragraph marker-paragraph\""));
        }
    }

    @Test
    void doesNotMarkStandaloneAnchorMarkerWithoutRelatedListingBlock() {
        String content = """
            = Marker

            ++++
            <a href="#missing">※</a>
            <div id="missing"></div>
            ++++
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertFalse(html.contains("marker-anchor"));
            assertFalse(html.contains("marker-following-marker"));
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
