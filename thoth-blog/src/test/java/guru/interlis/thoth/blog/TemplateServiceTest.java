package guru.interlis.thoth.blog;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateService.
 * Tests template rendering with various models.
 */
class TemplateServiceTest {

    private final TemplateService service = new TemplateService();

    @Test
    void rendersPostTemplate() {
        Map<String, Object> model = baseModel("Test Post");

        Map<String, Object> post = new LinkedHashMap<>();
        post.put("title", "My Post");
        post.put("author", "Jane Doe");
        post.put("date", "12.01.2026");
        post.put("status", "published");
        post.put("html", "<p>Hello <strong>World</strong></p>");
        post.put("tags", List.of(
            tagEntry("Java", "java"),
            tagEntry("AI", "ai")
        ));
        post.put("url", "/blog/2026/my-post/");

        model.put("post", post);

        String result = service.render("post.ftl", model);

        assertNotNull(result);
        assertTrue(result.contains("My Post"), "Should contain title");
        assertTrue(result.contains("Jane Doe"), "Should contain author");
        assertTrue(result.contains("<p>Hello <strong>World</strong></p>"), "Should contain HTML");
        assertTrue(result.contains("/tags/java/index.html"), "Should contain Java tag link");
        assertTrue(result.contains("/tags/ai/index.html"), "Should contain AI tag link");
        assertTrue(result.contains("12.01.2026"), "Should contain date");
        // post.url is not rendered as visible text in the post template (only in index/tag links)
    }

    @Test
    void rendersIndexTemplate() {
        Map<String, Object> model = baseModel("Thoth Blog");

        model.put("posts", List.of(
            Map.of(
                "title", "First Post",
                "date", "12.01.2026",
                "author", "Alice",
                "wordCount", 100,
                "url", "/blog/2026/first/",
                "tags", List.of(tagEntry("Java", "java")),
                "teaser", "This is the first post teaser."
            ),
            Map.of(
                "title", "Second Post",
                "date", "13.01.2026",
                "author", "Bob",
                "wordCount", 200,
                "url", "/blog/2026/second/",
                "tags", List.of(),
                "teaser", "Second post teaser."
            )
        ));

        String result = service.render("index.ftl", model);

        assertNotNull(result);
        assertTrue(result.contains("First Post"));
        assertTrue(result.contains("Second Post"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Bob"));
        assertTrue(result.contains("/blog/2026/first/"));
        assertTrue(result.contains("/blog/2026/second/"));
        assertTrue(result.contains("#Java"));
    }

    @Test
    void rendersArchiveTemplate() {
        Map<String, Object> model = baseModel("Blog Archive");

        model.put("groups", List.of(
            Map.of(
                "heading", "January 2026",
                "posts", List.of(
                    Map.of("day", "12", "title", "First Post", "url", "/blog/2026/first/"),
                    Map.of("day", "13", "title", "Second Post", "url", "/blog/2026/second/")
                )
            ),
            Map.of(
                "heading", "February 2026",
                "posts", List.of(
                    Map.of("day", "01", "title", "Third Post", "url", "/blog/2026/third/")
                )
            )
        ));

        String result = service.render("archive.ftl", model);

        assertNotNull(result);
        assertTrue(result.contains("Blog Archive"));
        assertTrue(result.contains("January 2026"));
        assertTrue(result.contains("February 2026"));
        assertTrue(result.contains("First Post"));
        assertTrue(result.contains("Second Post"));
        assertTrue(result.contains("Third Post"));
        assertTrue(result.contains("/blog/2026/first/"));
    }

    @Test
    void rendersTagTemplate() {
        Map<String, Object> model = baseModel("Tag: Java");

        model.put("tagName", "Java");
        model.put("posts", List.of(
            Map.of(
                "title", "Java Post",
                "date", "12.01.2026",
                "author", "Jane",
                "wordCount", 150,
                "url", "/blog/2026/java-post/",
                "tags", List.of(tagEntry("Java", "java"))
            )
        ));

        String result = service.render("tag.ftl", model);

        assertNotNull(result);
        assertTrue(result.contains("Tag: Java"));
        assertTrue(result.contains("Java Post"));
        assertTrue(result.contains("/blog/2026/java-post/"));
    }

    @Test
    void rendersSearchTemplate() {
        Map<String, Object> model = baseModel("Search");

        String result = service.render("search.ftl", model);

        assertNotNull(result);
        assertTrue(result.contains("Search"));
    }

    @Test
    void rendersFeedTemplate() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("siteTitle", "Test Blog");
        model.put("siteDescription", "A test blog");
        model.put("siteLanguage", "en-gb");
        model.put("siteLink", "https://example.com");
        model.put("feedSelf", "https://example.com/feed.xml");
        model.put("pubDate", "Mon, 12 Jan 2026 00:00:00 +0100");
        model.put("lastBuildDate", "Mon, 12 Jan 2026 00:00:00 +0100");

        model.put("items", List.of(
            Map.of(
                "title", "First Post",
                "link", "https://example.com/blog/2026/first/",
                "pubDate", "Mon, 12 Jan 2026 00:00:00 +0100",
                "guid", "blog/2026/first/",
                "description", "<p>First post content.</p>"
            ),
            Map.of(
                "title", "Second Post",
                "link", "https://example.com/blog/2026/second/",
                "pubDate", "Mon, 13 Jan 2026 00:00:00 +0100",
                "guid", "blog/2026/second/",
                "description", "<p>Second post content.</p>"
            )
        ));

        String result = service.render("feed.ftl", model);

        assertNotNull(result);
        assertTrue(result.contains("<rss version=\"2.0\""));
        assertTrue(result.contains("atom:link"));
        assertTrue(result.contains("https://example.com/feed.xml"));
        assertTrue(result.contains("First Post"));
        assertTrue(result.contains("Second Post"));
        assertTrue(result.contains("<guid isPermaLink=\"false\">blog/2026/first/</guid>"));
        assertTrue(result.contains("<link>https://example.com/blog/2026/first/</link>"));
    }

    @Test
    void rendersToHtmlFile() throws Exception {
        Path tempDir = Files.createTempDirectory("template-test");
        Path outputFile = tempDir.resolve("output.html");

        Map<String, Object> model = baseModel("Index Page");
        model.put("posts", List.of());

        service.renderToFile("index.ftl", model, outputFile);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        // index.ftl overrides pageTitle with site.title in its layout.page call
        assertTrue(content.contains("Thoth Blog"), "Output should contain site title");
        assertTrue(content.contains("post-grid"), "Output should contain post-grid section");
    }

    private Map<String, String> tagEntry(String name, String slug) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("slug", slug);
        return entry;
    }

    private Map<String, Object> baseModel(String pageTitle) {
        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, Object> site = new LinkedHashMap<>();
        site.put("title", "Thoth Blog");
        site.put("description", "A test blog");
        site.put("baseUrl", "https://example.com");
        site.put("language", "en-gb");
        site.put("brandMain", "Thoth");
        site.put("brandDomain", "");
        model.put("site", site);
        model.put("pageTitle", pageTitle);
        model.put("searchQuery", "");
        return model;
    }
}
