package guru.interlis.thoth.biblios.render;

import org.asciidoctor.Asciidoctor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    void renderDocumentAppliesAdditionalRevnumberAttribute(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Files.writeString(adoc, """
            = Versioned

            Version: {revnumber}
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.split(false, "en"),
                Map.of("revnumber", "Latest")
            );
            assertTrue(rendered.html().contains("Version: Latest"));
        }
    }

    @Test
    void renderDocumentAdditionalRevnumberOverridesDocumentAttribute(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Files.writeString(adoc, """
            = Versioned
            :revnumber: from-document

            Version: {revnumber}
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.split(false, "en"),
                Map.of("revnumber", "from-config")
            );
            assertTrue(rendered.html().contains("Version: from-config"));
            assertFalse(rendered.html().contains("Version: from-document"));
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
    void loadsBundledInterlisRougeLexerForHighlighting(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Files.writeString(adoc, """
            = PDF Test

            [source,interlis]
            ----
            MODEL Demo;
            ----
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer(true)) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.split(false, "en"),
                Map.of("source-highlighter", "rouge")
            );

            assertTrue(rendered.html().contains("rouge"), rendered.html());
            assertTrue(rendered.html().contains("<span class=\"k\">MODEL</span>"), rendered.html());
        }
    }

    @Test
    void rejectsMissingBundledRubyRequire() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> new AsciidoctorRenderer(Asciidoctor.Factory.create(), List.of("ruby/missing_lexer.rb"))
        );
        assertTrue(ex.getMessage().contains("initialize Biblios Ruby runtime"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("bundled Ruby require"));
        assertTrue(ex.getCause().getMessage().contains("resource not found"));
    }

    @Test
    void loadRubyRequiresIsIdempotent(@TempDir Path tempDir) throws Exception {
        Path ruby = tempDir.resolve("custom_lexer.rb");
        Files.writeString(ruby, """
            require 'rouge'

            module Rouge
              module Lexers
                class Dummy < RegexLexer
                  title 'Dummy'
                  tag 'dummy'
                  state :root do
                    rule %r/./, Text
                  end
                end
              end
            end
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            renderer.loadRubyRequires(List.of(ruby.toString()));
            renderer.loadRubyRequires(List.of(ruby.toString()));
        }
    }

    @Test
    void rejectsMissingRubyRequireFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.rb");

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            IOException ex = assertThrows(IOException.class, () -> renderer.loadRubyRequires(List.of(missing.toString())));
            assertTrue(ex.getMessage().contains("custom Rouge lexers"));
            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    @Test
    void providesGermanPdfAdmonitionCaptionsAsSoftDefaults() {
        Map<String, Object> defaults = AsciidoctorRenderer.defaultLocalizedAttributesForLanguage("de");

        assertEquals("Hinweis@", defaults.get("note-caption"));
        assertEquals("Tipp@", defaults.get("tip-caption"));
        assertEquals("Wichtig@", defaults.get("important-caption"));
        assertEquals("Warnung@", defaults.get("warning-caption"));
        assertEquals("Vorsicht@", defaults.get("caution-caption"));
    }

    @Test
    void providesNoLocalizedPdfAdmonitionCaptionsForEnglish() {
        assertTrue(AsciidoctorRenderer.defaultLocalizedAttributesForLanguage("en").isEmpty());
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
    void rendersSplitDocumentWithoutSectionNumbersWhenDisabled(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("split.adoc");
        Files.writeString(adoc, """
            = Manual
            :doctype: book

            == Einleitung

            === Status
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.split(false, false, "en")
            );

            assertNotNull(rendered);
            assertFalse(rendered.html().contains("class=\"sectnum\""));
            assertFalse(rendered.html().matches("(?s).*\\b1\\.?\\s+Einleitung\\b.*"));
        }
    }

    @Test
    void doesNotMarkRegularSectionsAsUnnumberedWhenSectionNumbersAreDisabled(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("master.adoc");
        Files.writeString(adoc, """
            = Manual
            :doctype: book

            == Einleitung
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.singlePage(false, false, 2, "en")
            );

            assertEquals(1, rendered.headings().size());
            assertEquals("Einleitung", rendered.headings().get(0).title());
            assertTrue(rendered.headings().get(0).sectionNumber().isBlank());
            assertFalse(rendered.headings().get(0).unnumbered());
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
            assertEquals("Hinweis", extractNoteCaption(htmlDe));
        }
    }

    @Test
    void appliesGermanLocalizedAdmonitionDefaultsToHtmlRendering() {
        String content = """
            = Hinweise

            [TIP]
            ====
            Sprachtest.
            ====
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content, "de");
            assertEquals("Tipp", extractAdmonitionCaption(html, "tip"));
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
        return extractAdmonitionCaption(html, "note");
    }

    private String extractAdmonitionCaption(String html, String type) {
        String normalizedType = type == null ? "note" : type.trim().toLowerCase(Locale.ROOT);
        Pattern iconTitleAttribute = Pattern.compile(
            "(?is)<div\\s+class=\"admonitionblock\\s+" + Pattern.quote(normalizedType) + "\"[^>]*>.*?<td\\s+class=\"icon\"[^>]*>.*?<i[^>]*\\btitle=\"([^\"]+?)\"[^>]*>"
        );
        Matcher iconTitleMatcher = iconTitleAttribute.matcher(html);
        if (iconTitleMatcher.find()) {
            return iconTitleMatcher.group(1).trim();
        }

        Pattern titleInIconCell = Pattern.compile(
            "(?is)<div\\s+class=\"admonitionblock\\s+" + Pattern.quote(normalizedType) + "\"[^>]*>.*?<td\\s+class=\"icon\"[^>]*>\\s*<div\\s+class=\"title\">\\s*([^<]+?)\\s*</div>"
        );
        Matcher iconCellMatcher = titleInIconCell.matcher(html);
        if (iconCellMatcher.find()) {
            return iconCellMatcher.group(1).trim();
        }

        Pattern titleInBlock = Pattern.compile(
            "(?is)<div\\s+class=\"admonitionblock\\s+" + Pattern.quote(normalizedType) + "\"[^>]*>.*?<div\\s+class=\"title\">\\s*([^<]+?)\\s*</div>"
        );
        Matcher blockMatcher = titleInBlock.matcher(html);
        assertTrue(blockMatcher.find(), "Expected caption in admonition block for type: " + normalizedType);
        return blockMatcher.group(1).trim();
    }
}
