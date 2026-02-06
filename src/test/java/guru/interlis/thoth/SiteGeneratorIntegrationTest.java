package guru.interlis.thoth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiteGeneratorIntegrationTest {

    @Test
    public void buildsExpectedSiteArtifacts() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");

        write(input.resolve("thoth.properties"), """
            site.title=Thoth Blog
            site.description=Demo feed
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9090
            """);

        write(input.resolve("blog/2026/post-one.adoc"), """
            ---
            = First Post
            Alice Author
            2026-01-12
            :thoth-status: published
            :thoth-tags: Java,AI
            ---
            image::images/cover.png[Cover]

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

        write(input.resolve("blog/2026/images/cover.png"), "PNG");
        write(input.resolve("blog/2026/site.js"), "console.log('ok');");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertTrue(Files.exists(output.resolve("blog/2026/post-one/index.html")));
        assertTrue(Files.exists(output.resolve("blog/2026/post-two/index.html")));
        assertTrue(Files.exists(output.resolve("index.html")));
        assertTrue(Files.exists(output.resolve("archive.html")));
        assertTrue(Files.exists(output.resolve("search.html")));
        assertTrue(Files.exists(output.resolve("feed.xml")));
        assertTrue(Files.exists(output.resolve("tags/ai/index.html")));
        assertTrue(Files.exists(output.resolve("tags/java/index.html")));

        assertTrue(Files.exists(output.resolve("assets/styles-light.css")));
        assertTrue(Files.exists(output.resolve("assets/styles-dark.css")));
        assertTrue(Files.exists(output.resolve("assets/theme.js")));
        assertTrue(Files.exists(output.resolve("assets/search.js")));
        assertTrue(Files.exists(output.resolve("assets/lunr.min.js")));
        assertTrue(Files.exists(output.resolve("assets/search-index.json")));
        assertTrue(Files.exists(output.resolve("assets/fonts/Inter/Inter-Regular.woff2")));

        assertTrue(Files.exists(output.resolve("blog/2026/images/cover.png")));
        assertTrue(Files.exists(output.resolve("blog/2026/site.js")));

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("First Post"));
        assertTrue(index.contains("Second Post"));
        assertTrue(index.contains("/blog/2026/images/cover.png"));
        assertTrue(index.contains("Manual teaser override"));

        String archive = Files.readString(output.resolve("archive.html"), StandardCharsets.UTF_8);
        assertTrue(archive.contains("First Post"));
        assertTrue(archive.contains("Second Post"));

        String feed = Files.readString(output.resolve("feed.xml"), StandardCharsets.UTF_8);
        assertTrue(feed.contains("<rss version=\"2.0\""));
        assertTrue(feed.contains("atom:link"));
        assertTrue(feed.contains("https://example.com/feed.xml"));
        assertTrue(feed.contains("<guid isPermaLink=\"false\">blog/2026/post-one/</guid>"));
        assertTrue(feed.contains("<link>https://example.com/blog/2026/post-two/</link>"));

        String searchIndex = Files.readString(output.resolve("assets/search-index.json"), StandardCharsets.UTF_8);
        assertTrue(searchIndex.contains("\"title\":\"Second Post\""));
        assertTrue(searchIndex.contains("\"url\":\"/blog/2026/post-one/\""));

        String postHtml = Files.readString(output.resolve("blog/2026/post-one/index.html"), StandardCharsets.UTF_8);
        assertTrue(postHtml.contains("id=\"navbar\""));
        assertTrue(postHtml.contains("id=\"search-input\""));
        assertTrue(postHtml.contains("id=\"theme-toggle\""));

        String searchJs = Files.readString(output.resolve("assets/search.js"), StandardCharsets.UTF_8);
        assertTrue(searchJs.contains("lunrSearch"));
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
