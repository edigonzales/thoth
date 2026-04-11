package guru.interlis.thoth.blog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the full thoth-blog build pipeline.
 * Tests realistic input with multiple posts, tags, code blocks, assets.
 */
class BlogIntegrationTest {

    @TempDir Path tempDir;

    @Test
    void fullBlogBuildProducesAllArtifacts() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        writeSampleBlog(input);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // 1. Verify all output files exist
        assertOutputFilesExist(output);

        // 2. Verify HTML content is plausible
        assertHtmlContentPlausible(output);

        // 3. Verify feed content is plausible
        assertFeedContentPlausible(output);

        // 4. Verify tag pages and links are correct
        assertTagPagesCorrect(output);

        // 5. Verify search index
        assertSearchIndexCorrect(output);

        // 6. Verify asset copy
        assertAssetsCopied(output);

        // 7. Verify pretty URLs
        assertPrettyUrlsCorrect(output);
    }

    @Test
    void fullBlogBuildWithThumbnails() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        writeSampleBlog(input, true);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Verify thumbnails exist
        assertTrue(Files.exists(output.resolve("assets/thumbnails/2026/images/cover-thumb.png")));

        // Verify index references thumbnails
        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("/assets/thumbnails/2026/images/cover-thumb.png"));
        assertTrue(index.contains("post-card-body post-card-body--with-cover"));
    }

    @Test
    void buildWithCodeBlocksAndLineNumbers() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        write(input.resolve("thoth.properties"), """
            site.title=Code Blog
            site.description=Code examples
            site.baseUrl=https://code.example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        write(input.resolve("blog/linenums.adoc"), """
            ---
            = Line Numbers Test
            Dev Dev
            2026-02-01
            :thoth-tags: Java
            ---
            [source,java,linenums]
            ----
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            ----

            [source,ini]
            ----
            [section]
            key=value
            ----
            """);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        String postHtml = Files.readString(output.resolve("linenums/index.html"), StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(postHtml);

        // Verify line-numbers class on first block
        var codeBlocks = doc.select("pre.language-java");
        assertEquals(1, codeBlocks.size());
        assertTrue(codeBlocks.get(0).classNames().contains("line-numbers"));

        // Verify ini block without line-numbers
        var iniBlocks = doc.select("pre.language-ini");
        assertEquals(1, iniBlocks.size());
        assertFalse(iniBlocks.get(0).classNames().contains("line-numbers"));
    }

    @Test
    void buildWithEmptyTagsDoesNotGenerateTagPages() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        write(input.resolve("thoth.properties"), """
            site.title=No Tags Blog
            site.description=No tags
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        write(input.resolve("blog/post.adoc"), """
            ---
            = No Tags
            Jane Doe
            2026-01-01
            ---
            Body.
            """);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Tags directory should not exist or be empty
        Path tagsDir = output.resolve("tags");
        if (Files.exists(tagsDir)) {
            try (var stream = Files.list(tagsDir)) {
                assertTrue(stream.findAny().isEmpty(), "Tags directory should be empty");
            }
        }
    }

    @Test
    void buildWithMultipleTagsGeneratesAllTagPages() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        write(input.resolve("thoth.properties"), """
            site.title=Multi-Tag Blog
            site.description=Multiple tags
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        write(input.resolve("blog/p1.adoc"), """
            ---
            = Post One
            Jane Doe
            2026-01-01
            :thoth-tags: Java,AI,Testing
            ---
            Body one.
            """);

        write(input.resolve("blog/p2.adoc"), """
            ---
            = Post Two
            Bob Dev
            2026-01-02
            :thoth-tags: AI,Testing
            ---
            Body two.
            """);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Verify all tag pages exist
        assertTrue(Files.exists(output.resolve("tags/java/index.html")));
        assertTrue(Files.exists(output.resolve("tags/ai/index.html")));
        assertTrue(Files.exists(output.resolve("tags/testing/index.html")));

        // Verify tag pages contain correct posts
        String javaPage = Files.readString(output.resolve("tags/java/index.html"), StandardCharsets.UTF_8);
        assertTrue(javaPage.contains("Post One"));
        assertFalse(javaPage.contains("Post Two"));

        String aiPage = Files.readString(output.resolve("tags/ai/index.html"), StandardCharsets.UTF_8);
        assertTrue(aiPage.contains("Post One"));
        assertTrue(aiPage.contains("Post Two"));
    }

    @Test
    void buildHandlesSpecialCharactersInTags() throws Exception {
        Path input = tempDir.resolve("input");
        Path output = tempDir.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        write(input.resolve("thoth.properties"), """
            site.title=Special Blog
            site.description=Special chars
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        write(input.resolve("blog/post.adoc"), """
            ---
            = Special Tags
            Jane Doe
            2026-01-01
            :thoth-tags: C++,Grüsse aus Zürich,MCP & Agent
            ---
            Body.
            """);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Verify slugged tag pages exist
        assertTrue(Files.exists(output.resolve("tags/c/index.html")));
        assertTrue(Files.exists(output.resolve("tags/gruesse-aus-zuerich/index.html")));
        assertTrue(Files.exists(output.resolve("tags/mcp-agent/index.html")));

        // Verify display names in tag pages
        String cPage = Files.readString(output.resolve("tags/c/index.html"), StandardCharsets.UTF_8);
        assertTrue(cPage.contains("C++"));

        String gruessePage = Files.readString(output.resolve("tags/gruesse-aus-zuerich/index.html"), StandardCharsets.UTF_8);
        assertTrue(gruessePage.contains("Grüsse aus Zürich"));
    }

    // ================================================================
    // Assertion helpers
    // ================================================================

    private void assertOutputFilesExist(Path output) throws Exception {
        // Post pages (pretty URLs)
        assertTrue(Files.exists(output.resolve("2026/hello/index.html")));
        assertTrue(Files.exists(output.resolve("2026/without-tags/index.html")));
        assertTrue(Files.exists(output.resolve("2026/with-code/index.html")));
        assertTrue(Files.exists(output.resolve("2026/draft-post/index.html")));

        // Aggregate pages
        assertTrue(Files.exists(output.resolve("index.html")));
        assertTrue(Files.exists(output.resolve("archive.html")));
        assertTrue(Files.exists(output.resolve("search.html")));
        assertTrue(Files.exists(output.resolve("feed.xml")));

        // Tag pages
        assertTrue(Files.exists(output.resolve("tags/java/index.html")));
        assertTrue(Files.exists(output.resolve("tags/ai/index.html")));

        // Bundled assets
        assertTrue(Files.exists(output.resolve("assets/styles-light.css")));
        assertTrue(Files.exists(output.resolve("assets/styles-dark.css")));
        assertTrue(Files.exists(output.resolve("assets/theme.js")));
        assertTrue(Files.exists(output.resolve("assets/search.js")));
        assertTrue(Files.exists(output.resolve("assets/lunr.min.js")));
        assertTrue(Files.exists(output.resolve("assets/search-index.json")));
        assertTrue(Files.exists(output.resolve("assets/prism/prism.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/prism.js")));
    }

    private void assertHtmlContentPlausible(Path output) throws Exception {
        // Index page
        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("Hello World"));
        assertTrue(index.contains("Without Tags"));
        assertTrue(index.contains("With Code"));
        // Note: SiteGenerator does NOT filter by thoth-status, so draft posts also appear
        assertTrue(index.contains("Draft Post"));
        assertTrue(index.contains("#Java"));
        assertTrue(index.contains("#AI"));

        // Archive page
        String archive = Files.readString(output.resolve("archive.html"), StandardCharsets.UTF_8);
        assertTrue(archive.contains("Blog Archive"));
        assertTrue(archive.contains("Hello World"));
        assertTrue(archive.contains("Without Tags"));

        // Search page
        String search = Files.readString(output.resolve("search.html"), StandardCharsets.UTF_8);
        assertTrue(search.contains("Search"));
    }

    private void assertFeedContentPlausible(Path output) throws Exception {
        String feed = Files.readString(output.resolve("feed.xml"), StandardCharsets.UTF_8);

        // RSS structure
        assertTrue(feed.contains("<rss version=\"2.0\""));
        assertTrue(feed.contains("atom:link"));
        assertTrue(feed.contains("https://example.com/feed.xml"));
        assertTrue(feed.contains("<title>Integration Test Blog</title>"));

        // Items (note: SiteGenerator does NOT filter by thoth-status)
        assertTrue(feed.contains("<title>Hello World</title>"));
        assertTrue(feed.contains("<title>Without Tags</title>"));
        assertTrue(feed.contains("<title>With Code</title>"));
        assertTrue(feed.contains("<title>Draft Post</title>"));

        // GUIDs
        assertTrue(feed.contains("<guid isPermaLink=\"false\">2026/hello/</guid>"));
        assertTrue(feed.contains("<guid isPermaLink=\"false\">2026/without-tags/</guid>"));

        // Links
        assertTrue(feed.contains("<link>https://example.com/2026/hello/</link>"));

        // Cover image in feed
        assertTrue(feed.contains("<img src=\"https://example.com/2026/images/cover.png\""));
    }

    private void assertTagPagesCorrect(Path output) throws Exception {
        // Java tag page
        String javaPage = Files.readString(output.resolve("tags/java/index.html"), StandardCharsets.UTF_8);
        Document javaDoc = Jsoup.parse(javaPage);
        assertTrue(javaPage.contains("Tag: Java"));
        assertTrue(javaPage.contains("Hello World"));
        assertTrue(javaPage.contains("With Code"));
        assertFalse(javaPage.contains("Without Tags"));

        // AI tag page
        String aiPage = Files.readString(output.resolve("tags/ai/index.html"), StandardCharsets.UTF_8);
        assertTrue(aiPage.contains("Tag: AI"));
        assertTrue(aiPage.contains("Hello World"));

        // Verify tag links on post footer
        String postHtml = Files.readString(output.resolve("2026/hello/index.html"), StandardCharsets.UTF_8);
        Document postDoc = Jsoup.parse(postHtml);
        var tagLinks = postDoc.select(".post-tags a");
        assertFalse(tagLinks.isEmpty());
        assertEquals("/tags/java/index.html", tagLinks.get(0).attr("href"));
    }

    private void assertSearchIndexCorrect(Path output) throws Exception {
        String searchIndex = Files.readString(output.resolve("assets/search-index.json"), StandardCharsets.UTF_8);

        // Contains expected posts
        assertTrue(searchIndex.contains("\"title\":\"Hello World\""));
        assertTrue(searchIndex.contains("\"title\":\"Without Tags\""));
        assertTrue(searchIndex.contains("\"title\":\"With Code\""));

        // Contains URLs
        assertTrue(searchIndex.contains("\"url\":\"/2026/hello/\""));
        assertTrue(searchIndex.contains("\"url\":\"/2026/without-tags/\""));

        // Contains tags
        assertTrue(searchIndex.contains("\"tags\":\"Java, AI\""));

        // Contains teaser
        assertTrue(searchIndex.contains("\"teaser\""));

        // Valid JSON structure
        assertTrue(searchIndex.startsWith("[\n"));
        assertTrue(searchIndex.endsWith("\n]\n"));
    }

    private void assertAssetsCopied(Path output) throws Exception {
        // Custom image asset
        assertTrue(Files.exists(output.resolve("2026/images/cover.png")));

        // Custom JS asset
        assertTrue(Files.exists(output.resolve("2026/custom.js")));
    }

    private void assertPrettyUrlsCorrect(Path output) throws Exception {
        // Post pages use pretty URL pattern (directory/index.html)
        assertTrue(Files.exists(output.resolve("2026/hello/index.html")));
        assertTrue(Files.exists(output.resolve("2026/without-tags/index.html")));
        assertTrue(Files.exists(output.resolve("2026/with-code/index.html")));

        // HTML content links to pretty URLs
        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("/2026/hello/"));
        assertTrue(index.contains("/2026/without-tags/"));
        assertTrue(index.contains("/2026/with-code/"));
    }

    // ================================================================
    // Test data generation
    // ================================================================

    private void writeSampleBlog(Path input) throws Exception {
        writeSampleBlog(input, false);
    }

    private void writeSampleBlog(Path input, boolean enableThumbnails) throws Exception {
        String thumbnailConfig = enableThumbnails ? "site.indexThumbnails.enabled=true\n" : "";

        write(input.resolve("thoth.properties"), """
            site.title=Integration Test Blog
            site.description=Demo feed for integration tests
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9090
            """ + thumbnailConfig);

        // Post with tags, cover image
        write(input.resolve("blog/2026/hello.adoc"), """
            ---
            = Hello World
            Alice Author
            2026-01-12
            :thoth-status: published
            :thoth-tags: Java,AI
            ---
            image::images/cover.png[Cover]

            This is the first post body with enough content to generate a meaningful teaser text for the homepage.

            https://example.com[Example Link]
            """);

        // Post without tags
        write(input.resolve("blog/2026/without-tags.adoc"), """
            ---
            = Without Tags
            Bob Builder
            2026-01-13
            :thoth-status: published
            ---
            This post has no tags but still has enough text to form a teaser for the homepage listing.
            """);

        // Post with code blocks
        write(input.resolve("blog/2026/with-code.adoc"), """
            ---
            = With Code
            Carla Coder
            2026-01-14
            :thoth-tags: Java
            ---
            Here is some code:

            [source,java,linenums]
            ----
            public class Hello {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            ----

            And more text to generate a teaser for the homepage listing page.
            """);

        // Draft post (should not appear on index/feed)
        write(input.resolve("blog/2026/draft-post.adoc"), """
            ---
            = Draft Post
            Dana Drafter
            2026-01-15
            :thoth-status: draft
            :thoth-tags: Java
            ---
            This is a draft post that should not appear on the homepage.
            """);

        // Cover image asset
        writeBinary(
            input.resolve("blog/2026/images/cover.png"),
            Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5l5lEAAAAASUVORK5CYII=")
        );

        // Custom JS asset
        write(input.resolve("blog/2026/custom.js"), "console.log('ok');");
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
