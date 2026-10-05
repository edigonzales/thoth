package guru.interlis.thoth.blog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultilingualBlogIntegrationTest {
    @TempDir Path root;

    private Path input() { return root.resolve("input"); }
    private Path output() { return root.resolve("output"); }

    private void write(String path, String content) throws Exception {
        Path file = input().resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private void config() throws Exception {
        write("thoth.properties", """
            site.title=Mein Blog
            site.title.en=English Blog
            site.description=Beschreibung
            site.description.en=English description
            site.baseUrl=https://example.com
            site.language=de
            site.languages=de,en
            site.dateFormat=dd MMMM yyyy
            """);
        Files.createDirectories(input().resolve("blog"));
    }

    private void post(String path, String title, String id, String status, String body) throws Exception {
        write("blog/" + path, "---\n= " + title + "\nAlice\n2026-10-05\n:thoth-status: " + status
            + (id == null ? "" : "\n:thoth-id: " + id) + "\n:thoth-tags: Java\n---\n" + body);
    }

    private String read(String path) throws Exception { return Files.readString(output().resolve(path)); }
    private Document html(String path) throws Exception { return Jsoup.parse(read(path)); }
    private void build(boolean clean) throws Exception {
        try (SiteGenerator generator = new SiteGenerator(input(), output())) { generator.buildAll(clean); }
    }

    @Test
    void buildsLocalizedSiteWithEquivalentPagesFallbacksFeedsAndAssets() throws Exception {
        config();
        post("de/2026/deutscher-titel.adoc", "Deutscher Titel", "shared", "published", "image::images/cover.svg[]");
        post("en/2026/english-title.adoc", "English Title", "shared", "published",
            "image::images/cover.svg[]\n\nlink:../../de/2026/deutscher-titel.adoc#details[Original]");
        post("2026/legacy.adoc", "Nur Deutsch", null, "published", "Legacy body");
        write("blog/de/2026/images/cover.svg", "<svg>German image</svg>");
        write("blog/en/2026/images/cover.svg", "<svg>English image</svg>");
        build(true);

        Document german = html("2026/deutscher-titel/index.html");
        Document english = html("en/2026/english-title/index.html");
        assertEquals("de", german.selectFirst("html").attr("lang"));
        assertEquals("en", english.selectFirst("html").attr("lang"));
        assertEquals("/en/2026/english-title/", german.selectFirst("#language-switch option[lang=en]").val());
        assertEquals("/2026/deutscher-titel/", english.selectFirst("#language-switch option[lang=de]").val());
        assertEquals("https://example.com/en/2026/english-title/", english.selectFirst("link[rel=canonical]").attr("href"));
        assertEquals("https://example.com/2026/deutscher-titel/", english.selectFirst("link[hreflang=de]").attr("href"));
        assertEquals("/2026/images/cover.svg", german.selectFirst(".post-content img").attr("src"));
        assertEquals("/en/2026/images/cover.svg", english.selectFirst(".post-content img").attr("src"));
        assertEquals("/2026/deutscher-titel/#details", english.selectFirst(".post-content a").attr("href"));
        assertEquals("<svg>German image</svg>", read("2026/images/cover.svg"));
        assertEquals("<svg>English image</svg>", read("en/2026/images/cover.svg"));

        Document englishIndex = html("en/index.html");
        assertEquals(List.of("English Title", "Nur Deutsch"), englishIndex.select(".post-title").eachText());
        assertEquals("Only available in Deutsch", englishIndex.selectFirst(".language-notice").text());
        assertEquals("de", englishIndex.select("h2.post-title").get(1).attr("lang"));
        assertEquals(List.of("/en/index.html", "/en/archive.html", "/en/feed.xml"), englishIndex.select(".nav-left a").eachAttr("href"));
        assertEquals("/en/search.html", englishIndex.selectFirst("#search-form").attr("action"));
        assertEquals("/en/assets/search-index.json", englishIndex.body().attr("data-search-index-url"));
        assertTrue(englishIndex.title().contains("English Blog"));
        assertTrue(html("index.html").text().contains("Veröffentlicht am"));
        assertTrue(html("en/archive.html").text().contains("Only available in Deutsch"));
        assertTrue(html("en/tags/java/index.html").text().contains("Only available in Deutsch"));
        assertEquals("/en/tags/java/index.html", english.selectFirst(".post-tags a").attr("href"));
        assertTrue(html("en/search.html").text().contains("Search"));

        String index = read("en/assets/search-index.json");
        assertTrue(index.contains("English Title"));
        assertTrue(index.contains("Nur Deutsch"));
        assertFalse(index.contains("Deutscher Titel"));
        assertTrue(read("feed.xml").contains("<guid isPermaLink=\"false\">2026/deutscher-titel/</guid>"));
        assertTrue(read("en/feed.xml").contains("English description"));
        assertTrue(read("en/feed.xml").contains("<language>en</language>"));
        assertFalse(read("en/feed.xml").contains("Nur Deutsch"));
        assertFalse(read("en/feed.xml").contains("Deutscher Titel</title>"));
        Document legacy = html("2026/legacy/index.html");
        assertEquals("/en/index.html", legacy.selectFirst("#language-switch option[lang=en]").val());
        assertTrue(legacy.selectFirst("#language-switch option[lang=en]").text().contains("keine Übersetzung"));
        assertNull(legacy.selectFirst("link[hreflang=en]"));
    }

    @Test
    void infersTranslationIdentityFromPathsInsideLanguageDirectories() throws Exception {
        config();
        post("de/2026/same.adoc", "Deutsch", null, "published", "Text");
        post("en/2026/same.adoc", "English", null, "published", "Text");
        build(true);
        assertEquals("/en/2026/same/", html("2026/same/index.html").selectFirst("#language-switch option[lang=en]").val());
        assertEquals(1, html("en/index.html").select(".post-card").size());
    }

    @Test
    void watchAddsRetractsAndDeletesTranslationsAndRefreshesOtherVariants() throws Exception {
        config();
        post("de/2026/original.adoc", "Original", "shared", "published", "Text");
        try (SiteGenerator generator = new SiteGenerator(input(), output())) {
            generator.buildAll(true);
            post("en/2026/translation.adoc", "Translation", "shared", "published", "English");
            Path translated = input().resolve("blog/en/2026/translation.adoc");
            generator.handleInputEvent(translated, "CREATE");
            assertEquals("/en/2026/translation/", html("2026/original/index.html").selectFirst("#language-switch option[lang=en]").val());
            assertTrue(read("en/feed.xml").contains("Translation"));
            post("en/2026/translation.adoc", "Unfinished", "shared", "draft", "Secret draft body");
            generator.handleInputEvent(translated, "MODIFY");
            assertFalse(Files.exists(output().resolve("en/2026/translation/index.html")));
            assertNull(html("2026/original/index.html").selectFirst("link[hreflang=en]"));
            assertTrue(read("en/index.html").contains("Original"));
            assertFalse(read("en/feed.xml").contains("Original</title>"));
            assertFalse(read("en/assets/search-index.json").contains("Secret draft body"));
            post("en/2026/translation.adoc", "Restored", "shared", "published", "English");
            generator.handleInputEvent(translated, "MODIFY");
            Files.delete(translated);
            generator.handleInputEvent(translated, "DELETE");
            assertFalse(Files.exists(output().resolve("en/2026/translation/index.html")));
            assertFalse(read("en/feed.xml").contains("Restored"));
            assertNull(html("2026/original/index.html").selectFirst("link[hreflang=en]"));

            write("blog/en/2026/image.svg", "English image");
            Path image = input().resolve("blog/en/2026/image.svg");
            generator.handleInputEvent(image, "CREATE");
            assertEquals("English image", read("en/2026/image.svg"));
            write("blog/en/2026/image.svg", "Changed image");
            generator.handleInputEvent(image, "MODIFY");
            assertEquals("Changed image", read("en/2026/image.svg"));
            Files.delete(image);
            generator.handleInputEvent(image, "DELETE");
            assertFalse(Files.exists(output().resolve("en/2026/image.svg")));
        }
    }

    @Test
    void changingTranslationIdentityRefreshesBothGroupsAndConfigReloadsAllPages() throws Exception {
        config();
        post("de/2026/first.adoc", "First", "first", "published", "Text");
        post("de/2026/second.adoc", "Second", "second", "published", "Text");
        post("en/2026/translated.adoc", "Translated", "first", "published", "Text");
        try (SiteGenerator generator = new SiteGenerator(input(), output())) {
            generator.buildAll(true);
            post("en/2026/translated.adoc", "Translated", "second", "published", "Text");
            generator.handleInputEvent(input().resolve("blog/en/2026/translated.adoc"), "MODIFY");
            assertNull(html("2026/first/index.html").selectFirst("link[hreflang=en]"));
            assertEquals("/en/2026/translated/", html("2026/second/index.html")
                .selectFirst("#language-switch option[lang=en]").val());
            assertEquals("/2026/second/", html("en/2026/translated/index.html")
                .selectFirst("#language-switch option[lang=de]").val());
            String configuration = Files.readString(input().resolve("thoth.properties"));
            write("thoth.properties", configuration.replace("site.title.en=English Blog", "site.title.en=Changed English Blog"));
            generator.handleInputEvent(input().resolve("thoth.properties"), "MODIFY");
            assertTrue(html("en/2026/translated/index.html").title().contains("Changed English Blog"));
        }
    }

    @Test
    void nonCleanBuildRetractsDraftsAndDeletedSourcesAcrossGeneratorInstances() throws Exception {
        config();
        post("2026/original.adoc", "Original", null, "published", "Text");
        post("en/2026/removed.adoc", "Removed", null, "published", "English");
        build(true);
        post("2026/original.adoc", "Original", null, "draft", "Draft");
        Files.delete(input().resolve("blog/en/2026/removed.adoc"));
        build(false);
        assertFalse(Files.exists(output().resolve("2026/original/index.html")));
        assertFalse(Files.exists(output().resolve("en/2026/removed/index.html")));
        assertFalse(Files.exists(output().resolve("tags/java/index.html")));
        assertFalse(read("assets/search-index.json").contains("Original"));
        assertEquals(0, html("en/index.html").select(".post-card").size());
    }

    @Test
    void migrationKeepsTheDefaultUrlAndGuid() throws Exception {
        config();
        post("2026/original.adoc", "Original", null, "published", "Text");
        build(true);
        Files.createDirectories(input().resolve("blog/de/2026"));
        Files.move(input().resolve("blog/2026/original.adoc"), input().resolve("blog/de/2026/original.adoc"));
        build(false);
        assertTrue(Files.exists(output().resolve("2026/original/index.html")));
        assertFalse(Files.exists(output().resolve("de/2026/original/index.html")));
        assertTrue(read("feed.xml").contains("<guid isPermaLink=\"false\">2026/original/</guid>"));
    }

    @Test
    void rejectsDuplicateIdentitiesAndCollidingUrlsBeforeWritingPages() throws Exception {
        config();
        post("de/2026/a.adoc", "First", "same", "published", "Text");
        post("de/2026/b.adoc", "Second", "same", "draft", "Text");
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () -> build(false));
        assertTrue(duplicate.getMessage().contains("Duplicate content id 'same'"));
        assertTrue(duplicate.getMessage().contains("de/2026/a.adoc"));
        assertFalse(Files.exists(output().resolve("2026/a/index.html")));
        Files.delete(input().resolve("blog/de/2026/b.adoc"));
        post("2026/a.adoc", "Legacy", "different", "published", "Text");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> build(false)).getMessage().contains("Output path collision"));
    }

    @Test
    void rejectsCollisionsWithTagPagesAndContentAssets() throws Exception {
        config();
        post("de/tags/java.adoc", "Reserved", null, "published", "Text");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> build(false)).getMessage().contains("tag page"));
        Files.delete(input().resolve("blog/de/tags/java.adoc"));
        write("blog/en/index.html", "This would overwrite the English home page");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> build(false)).getMessage().contains("Output path collision"));
    }
}
