package guru.interlis.thoth;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiteGeneratorIntegrationTest {

    @Test
    public void buildsSiteWithoutIndexThumbnailsByDefault() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertCommonSiteArtifacts(output);
        assertFalse(Files.exists(output.resolve("assets/thumbnails/blog/2026/images/cover-thumb.png")));

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("First Post"));
        assertTrue(index.contains("Second Post"));
        assertTrue(index.contains("Third Post"));
        assertTrue(index.contains("Fourth Post"));
        assertTrue(index.contains("Manual teaser override"));
        assertTrue(index.contains("class=\"teaser-more\""));
        assertFalse(index.contains("/assets/thumbnails/blog/2026/images/cover-thumb.png"));
        assertFalse(index.contains("class=\"post-card-body post-card-body--with-cover\""));

        assertCommonCss(output);
        assertGroupedArchive(output);
        assertCommonFeed(output);
        assertCommonSearchIndex(output);
        assertPostFooter(output);
    }

    @Test
    public void buildsSiteWithIndexThumbnailsWhenEnabled() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, true);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertCommonSiteArtifacts(output);
        assertTrue(Files.exists(output.resolve("assets/thumbnails/blog/2026/images/cover-thumb.png")));

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("/assets/thumbnails/blog/2026/images/cover-thumb.png"));
        assertTrue(index.contains("class=\"post-card-body post-card-body--with-cover\""));

        assertCommonCss(output);
        assertGroupedArchive(output);
        assertCommonFeed(output);
        assertCommonSearchIndex(output);
        assertPostFooter(output);
    }

    private void assertCommonSiteArtifacts(Path output) throws Exception {
        assertTrue(Files.exists(output.resolve("blog/2026/post-one/index.html")));
        assertTrue(Files.exists(output.resolve("blog/2026/post-two/index.html")));
        assertTrue(Files.exists(output.resolve("blog/2026/post-three/index.html")));
        assertTrue(Files.exists(output.resolve("blog/2026/post-four/index.html")));
        assertTrue(Files.exists(output.resolve("index.html")));
        assertTrue(Files.exists(output.resolve("archive.html")));
        assertTrue(Files.exists(output.resolve("search.html")));
        assertTrue(Files.exists(output.resolve("feed.xml")));
        assertTrue(Files.exists(output.resolve("tags/ai/index.html")));
        assertTrue(Files.exists(output.resolve("tags/java/index.html")));

        assertTrue(Files.exists(output.resolve("assets/styles-light.css")));
        assertTrue(Files.exists(output.resolve("assets/styles-dark.css")));
        assertTrue(Files.exists(output.resolve("assets/zurich.css")));
        assertTrue(Files.exists(output.resolve("assets/fonts/Zurich/306E22_0_0.woff2")));
        assertTrue(Files.exists(output.resolve("assets/fonts/Zurich/306E22_1_0.woff2")));
        assertTrue(Files.exists(output.resolve("assets/theme.js")));
        assertTrue(Files.exists(output.resolve("assets/search.js")));
        assertTrue(Files.exists(output.resolve("assets/lunr.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/prism.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/prism.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-ini.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-interlis.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-javascript.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-css.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-java.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-highlight/prism-line-highlight.min.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-highlight/prism-line-highlight.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-numbers/prism-line-numbers.min.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-numbers/prism-line-numbers.min.js")));
        assertTrue(Files.exists(output.resolve("assets/search-index.json")));
        assertTrue(Files.exists(output.resolve("assets/fonts/JetBrainsMono/JetBrainsMono-Regular.woff2")));
        assertTrue(Files.exists(output.resolve("assets/fonts/JetBrainsMono/JetBrainsMono-Bold.woff2")));
        assertTrue(Files.exists(output.resolve("assets/fonts/JetBrainsMono/JetBrainsMono-Italic.woff2")));

        assertTrue(Files.exists(output.resolve("blog/2026/images/cover.png")));
        assertTrue(Files.exists(output.resolve("blog/2026/site.js")));
    }

    private void assertCommonCss(Path output) throws Exception {
        String lightCss = Files.readString(output.resolve("assets/styles-light.css"), StandardCharsets.UTF_8);
        assertTrue(lightCss.contains(".post-grid {\n  display: grid;\n  grid-template-columns: 1fr;"));
        assertTrue(lightCss.contains("overflow-wrap: anywhere;"));
        assertTrue(lightCss.contains(".archive-group-posts {"));
        assertTrue(lightCss.contains(".post-footer-separator {"));

        String darkCss = Files.readString(output.resolve("assets/styles-dark.css"), StandardCharsets.UTF_8);
        assertTrue(darkCss.contains(".post-grid {\n  display: grid;\n  grid-template-columns: 1fr;"));
        assertTrue(darkCss.contains("overflow-wrap: anywhere;"));
        assertTrue(darkCss.contains(".archive-group-posts {"));
        assertTrue(darkCss.contains(".post-footer-separator {"));
    }

    private void assertGroupedArchive(Path output) throws Exception {
        String archive = Files.readString(output.resolve("archive.html"), StandardCharsets.UTF_8);
        Document archiveDoc = Jsoup.parse(archive);

        Element archiveHeading = archiveDoc.selectFirst("h1");
        assertNotNull(archiveHeading);
        assertEquals("Blog Archive", archiveHeading.text());
        assertEquals(List.of("March 2026", "January 2026"), archiveDoc.select(".archive-group-heading").eachText());
        assertEquals(
            List.of("17 - Third Post", "16 - Fourth Post"),
            archiveDoc.select(".archive-group").get(0).select("li").eachText()
        );
        assertEquals(
            List.of("13 - Second Post", "12 - First Post"),
            archiveDoc.select(".archive-group").get(1).select("li").eachText()
        );
        assertEquals(
            "/blog/2026/post-three/",
            archiveDoc.select(".archive-group").get(0).selectFirst("a").attr("href")
        );
    }

    private void assertCommonFeed(Path output) throws Exception {
        String feed = Files.readString(output.resolve("feed.xml"), StandardCharsets.UTF_8);
        assertTrue(feed.contains("<rss version=\"2.0\""));
        assertTrue(feed.contains("atom:link"));
        assertTrue(feed.contains("https://example.com/feed.xml"));
        assertTrue(feed.contains("<guid isPermaLink=\"false\">blog/2026/post-one/</guid>"));
        assertTrue(feed.contains("<link>https://example.com/blog/2026/post-two/</link>"));
        assertTrue(feed.contains("<img src=\"https://example.com/blog/2026/images/cover.png\""));
        assertTrue(feed.contains("defaultSrsCode=2056"));
    }

    private void assertCommonSearchIndex(Path output) throws Exception {
        String searchIndex = Files.readString(output.resolve("assets/search-index.json"), StandardCharsets.UTF_8);
        assertTrue(searchIndex.contains("\"title\":\"Second Post\""));
        assertTrue(searchIndex.contains("\"url\":\"/blog/2026/post-one/\""));
    }

    private void assertPostFooter(Path output) throws Exception {
        String postHtml = Files.readString(output.resolve("blog/2026/post-one/index.html"), StandardCharsets.UTF_8);
        assertTrue(postHtml.contains("id=\"navbar\""));
        assertTrue(postHtml.contains("id=\"search-input\""));
        assertTrue(postHtml.contains("id=\"theme-toggle\""));
        assertTrue(postHtml.contains("/assets/prism/prism.css"));
        assertTrue(postHtml.contains("/assets/prism/prism.js"));
        assertTrue(postHtml.contains("/assets/prism/components/prism-interlis.js"));
        assertTrue(postHtml.contains("/assets/prism/plugins/line-highlight/prism-line-highlight.min.js"));
        assertTrue(postHtml.contains("/assets/prism/plugins/line-numbers/prism-line-numbers.min.js"));
        assertTrue(postHtml.contains("/assets/zurich.css"));
        assertTrue(postHtml.contains("Written by Alice Author"));

        Document postDoc = Jsoup.parse(postHtml);
        assertEquals(1, postDoc.select("pre.language-ini.line-numbers > code.language-ini").size());

        Element footer = postDoc.selectFirst(".post-footer");
        assertNotNull(footer);
        assertEquals(List.of("Java", "AI"), footer.select(".post-tags a").eachText());
        assertEquals("/tags/java/index.html", footer.select(".post-tags a").get(0).attr("href"));
        assertTrue(footer.text().contains("Written by Alice Author | Java, AI"));
        assertTrue(footer.select("a.tag").isEmpty());

        String searchJs = Files.readString(output.resolve("assets/search.js"), StandardCharsets.UTF_8);
        assertTrue(searchJs.contains("lunrSearch"));
    }

    private void writeSampleSite(Path input, boolean enableIndexThumbnails) throws Exception {
        String thumbnailConfig = enableIndexThumbnails ? "site.indexThumbnails.enabled=true\n" : "";

        write(input.resolve("thoth.properties"), """
            site.title=Thoth Blog
            site.description=Demo feed
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9090
            """ + thumbnailConfig);

        write(input.resolve("blog/2026/post-one.adoc"), """
            ---
            = First Post
            Alice Author
            2026-01-12
            :thoth-status: published
            :thoth-tags: Java,AI
            ---
            image::images/cover.png[Cover]

            [source,ini,linenums]
            ----
            [ch.ehi.ili2db]
            defaultSrsCode=2056
            createEnumTabs=true
            ----

            First post paragraph with enough content to form a teaser.
            """);

        write(input.resolve("blog/2026/post-two.adoc"), """
            ---
            = Second Post
            Bob Builder
            2026-01-13
            :thoth-status: published
            :thoth-tags: AI
            :thoth-teaser: Manual teaser override
            ---
            Some second post body text.
            """);

        write(input.resolve("blog/2026/post-three.adoc"), """
            ---
            = Third Post
            Carla Creator
            2026-03-17
            :thoth-status: published
            ---
            Third post body text.
            """);

        write(input.resolve("blog/2026/post-four.adoc"), """
            ---
            = Fourth Post
            Dana Developer
            2026-03-16
            :thoth-status: published
            ---
            Fourth post body text.
            """);

        writeBinary(
            input.resolve("blog/2026/images/cover.png"),
            Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5l5lEAAAAASUVORK5CYII=")
        );
        write(input.resolve("blog/2026/site.js"), "console.log('ok');");
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeBinary(Path path, byte[] content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }
}
