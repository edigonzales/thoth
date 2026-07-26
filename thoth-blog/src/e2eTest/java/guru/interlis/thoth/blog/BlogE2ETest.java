package guru.interlis.thoth.blog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests for thoth-blog.
 * Tests realistic full build flows with actual HTTP server.
 * NOT mock tests — uses real builds, real HTTP server, real output.
 */
class BlogE2ETest {

    @TempDir Path tempDir;

    private DevServer runningServer;

    @AfterEach
    void tearDown() {
        if (runningServer != null) {
            try {
                runningServer.stop();
            } catch (Exception ignored) {
            }
            runningServer = null;
        }
    }

    /**
     * E2E-1: Full blog build from realistic input.
     * Verifies: thoth.properties parsing, post parsing, HTML generation, all artifacts.
     */
    @Test
    void fullBlogBuildFromRealisticInput() throws Exception {
        Path input = setupTestBlogInput("e2e-full");
        Path output = tempDir.resolve("output-full");
        Files.createDirectories(output);

        // Run build
        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Verify all expected files exist
        assertFileExists(output, "index.html");
        assertFileExists(output, "archive.html");
        assertFileExists(output, "search.html");
        assertFileExists(output, "feed.xml");
        assertFileExists(output, "2026/hello/index.html");
        assertFileExists(output, "2026/without-tags/index.html");
        assertFileExists(output, "tags/java/index.html");
        assertFileExists(output, "tags/ai/index.html");
        assertFileExists(output, "assets/search-index.json");
        assertFileExists(output, "assets/styles-light.css");
        assertFileExists(output, "assets/styles-dark.css");
        assertFileExists(output, "assets/theme.js");

        // Verify HTML content
        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("Hello World"));
        assertTrue(index.contains("Alice Author"));
        assertTrue(index.contains("#Java"));
        assertTrue(index.contains("#AI"));

        // Verify feed
        String feed = Files.readString(output.resolve("feed.xml"), StandardCharsets.UTF_8);
        assertTrue(feed.contains("<rss version=\"2.0\""));
        assertTrue(feed.contains("Hello World"));
        assertTrue(feed.contains("<guid isPermaLink=\"false\">2026/hello/</guid>"));

        // Verify search index
        String searchIndex = Files.readString(output.resolve("assets/search-index.json"), StandardCharsets.UTF_8);
        assertTrue(searchIndex.contains("Hello World"));
        assertTrue(searchIndex.contains("Without Tags"));
    }

    /**
     * E2E-2: DevServer (serve) starts and delivers generated pages via HTTP.
     * Verifies: HTTP server starts, serves HTML content, correct routes, assets.
     */
    @Test
    void serveDeliversGeneratedPages() throws Exception {
        Path input = setupTestBlogInput("e2e-serve");
        Path output = tempDir.resolve("output-serve");
        Files.createDirectories(output);

        // Build first
        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Start DevServer on a free port
        int port = findFreePort();
        runningServer = new DevServer(output, port);
        runningServer.start();
        Thread.sleep(1000); // Wait for server to start

        HttpClient client = createHttpClient();

        // Test homepage
        HttpResponse<String> homeResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/index.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, homeResponse.statusCode());
        assertTrue(homeResponse.body().contains("Hello World"));
        assertTrue(homeResponse.body().contains("E2E Test Blog"));

        // Test post page (pretty URL)
        HttpResponse<String> postResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/2026/hello/"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, postResponse.statusCode());
        assertTrue(postResponse.body().contains("Hello World"));
        assertTrue(postResponse.body().contains("Alice Author"));

        // Test tag page
        HttpResponse<String> tagResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/tags/java/index.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, tagResponse.statusCode());
        assertTrue(tagResponse.body().contains("Tag: Java"));

        // Test archive page
        HttpResponse<String> archiveResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/archive.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, archiveResponse.statusCode());
        assertTrue(archiveResponse.body().contains("Blog Archive"));

        // Test RSS feed
        HttpResponse<String> feedResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/feed.xml"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, feedResponse.statusCode());
        assertTrue(feedResponse.body().contains("<rss version=\"2.0\""));
        assertTrue(feedResponse.body().contains("Hello World"));

        // Test search index JSON
        HttpResponse<String> searchResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/assets/search-index.json"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, searchResponse.statusCode());
        assertTrue(searchResponse.body().contains("Hello World"));
        assertTrue(searchResponse.body().contains("Without Tags"));

        // Test CSS asset
        HttpResponse<String> cssResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/assets/styles-light.css"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, cssResponse.statusCode());
        assertTrue(cssResponse.body().contains(".") || cssResponse.body().contains("body"));
    }

    /**
     * E2E-3: DevServer serves pages via pretty URLs (without .html extension).
     * Verifies: /2026/hello/ resolves to 2026/hello/index.html
     */
    @Test
    void serveDeliversPrettyUrlPages() throws Exception {
        Path input = setupTestBlogInput("e2e-pretty");
        Path output = tempDir.resolve("output-pretty");
        Files.createDirectories(output);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        int port = findFreePort();
        runningServer = new DevServer(output, port);
        runningServer.start();
        Thread.sleep(1000);

        HttpClient client = createHttpClient();

        // Test pretty URL without trailing slash (should redirect or serve index)
        HttpResponse<String> response1 = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/2026/hello/"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response1.statusCode());
        assertTrue(response1.body().contains("Hello World"));

        // Test direct index.html access
        HttpResponse<String> response2 = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/2026/hello/index.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response2.statusCode());
        assertTrue(response2.body().contains("Hello World"));
    }

    /**
     * E2E-4: DevServer returns 404 for non-existent pages.
     */
    @Test
    void serveReturns404ForNonExistentPages() throws Exception {
        Path input = setupTestBlogInput("e2e-404");
        Path output = tempDir.resolve("output-404");
        Files.createDirectories(output);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        int port = findFreePort();
        runningServer = new DevServer(output, port);
        runningServer.start();
        Thread.sleep(1000);

        HttpClient client = createHttpClient();

        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/nonexistent/"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, response.statusCode());
    }

    /**
     * E2E-5: Full blog build with code blocks and assets.
     * Verifies: Prism.js assets, code highlighting, copied assets.
     */
    @Test
    void fullBlogWithCodeBlocksAndAssets() throws Exception {
        Path input = tempDir.resolve("e2e-code-input");
        Path output = tempDir.resolve("e2e-code-output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        write(input.resolve("thoth.properties"), """
            site.title=Code Blog
            site.description=Code examples blog
            site.baseUrl=https://code.example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9090
            """);

        write(input.resolve("blog/code-post.adoc"), """
            ---
            = Code Post
            Dev Developer
            2026-02-01
            :thoth-tags: Java,Testing
            ---
            [source,java,linenums]
            ----
            public class Test {
                public void hello() {
                    System.out.println("Hello");
                }
            }
            ----

            [source,groovy]
            ----
            tasks.register('hello') {
                doLast { println 'Hello' }
            }
            ----

            [source,gradle]
            ----
            plugins {
                id 'java'
            }
            ----

            Some inline code: `String s = "test";`

            Enough text here to form a proper teaser for the homepage listing page display.
            """);

        // Custom CSS asset
        write(input.resolve("assets/custom.css"), "body { margin: 0; }");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        // Verify post page
        assertFileExists(output, "code-post/index.html");
        String postHtml = Files.readString(output.resolve("code-post/index.html"), StandardCharsets.UTF_8);
        assertTrue(postHtml.contains("Code Post"));
        assertTrue(postHtml.contains("language-java"));
        assertTrue(postHtml.contains("language-groovy"));
        assertTrue(postHtml.contains("language-gradle"));
        assertTrue(postHtml.contains("line-numbers"));

        // Verify Prism assets
        assertFileExists(output, "assets/prism/prism.css");
        assertFileExists(output, "assets/prism/prism.js");
        assertFileExists(output, "assets/prism/components/prism-groovy.min.js");
        assertFileExists(output, "assets/prism/components/prism-gradle.min.js");

        // Verify custom asset copied
        assertFileExists(output, "assets/custom.css");

        // Verify search index
        String searchIndex = Files.readString(output.resolve("assets/search-index.json"), StandardCharsets.UTF_8);
        assertTrue(searchIndex.contains("Code Post"));
        assertTrue(searchIndex.contains("Testing"));
    }

    /**
     * E2E-6: DevServer serves search page and returns valid search index.
     */
    @Test
    void serveDeliversSearchPageAndIndex() throws Exception {
        Path input = setupTestBlogInput("e2e-search");
        Path output = tempDir.resolve("output-search");
        Files.createDirectories(output);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        int port = findFreePort();
        runningServer = new DevServer(output, port);
        runningServer.start();
        Thread.sleep(1000);

        HttpClient client = createHttpClient();

        // Test search page
        HttpResponse<String> searchPageResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/search.html"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, searchPageResponse.statusCode());
        assertTrue(searchPageResponse.body().contains("Search"));

        // Test search index
        HttpResponse<String> searchIndexResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/assets/search-index.json"))
                .timeout(Duration.ofSeconds(5))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, searchIndexResponse.statusCode());

        // Verify JSON structure
        String json = searchIndexResponse.body();
        assertTrue(json.trim().startsWith("["));
        assertTrue(json.trim().endsWith("]"));
        assertTrue(json.contains("\"title\""));
        assertTrue(json.contains("\"url\""));
        assertTrue(json.contains("\"body\""));
        assertTrue(json.contains("\"tags\""));
    }

    /**
     * E2E-7: Full blog with custom site title and branding.
     * Verifies: site.title split into brand/domain for homepage hero.
     */
    @Test
    void fullBlogWithDotSeparatedSiteTitle() throws Exception {
        Path input = tempDir.resolve("e2e-branding-input");
        Path output = tempDir.resolve("e2e-branding-output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        write(input.resolve("thoth.properties"), """
            site.title=myblog.example.com
            site.description=A branded blog
            site.baseUrl=https://myblog.example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        write(input.resolve("blog/post.adoc"), """
            ---
            = Branded Post
            Jane Doe
            2026-01-01
            ---
            Enough content to form a teaser for the homepage listing page.
            """);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("myblog"));
        assertTrue(index.contains(".example.com"));
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private Path setupTestBlogInput(String name) throws Exception {
        Path input = tempDir.resolve(name + "-input");
        Files.createDirectories(input);

        write(input.resolve("thoth.properties"), """
            site.title=E2E Test Blog
            site.description=End-to-end test blog
            site.baseUrl=https://e2e.example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9090
            """);

        // Post with tags and cover image
        write(input.resolve("blog/2026/hello.adoc"), """
            ---
            = Hello World
            Alice Author
            2026-01-12
            :thoth-status: published
            :thoth-tags: Java,AI
            ---
            image::images/cover.png[Cover]

            This is the first post with enough content to generate a teaser for the homepage listing page.

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
            This post has no tags but still has enough text to form a teaser for the homepage listing page.
            """);

        // Cover image
        writeBinary(
            input.resolve("blog/2026/images/cover.png"),
            Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5l5lEAAAAASUVORK5CYII=")
        );

        return input;
    }

    private void assertFileExists(Path base, String relativePath) throws IOException {
        Path path = base.resolve(relativePath);
        assertTrue(Files.exists(path), "Expected file to exist: " + relativePath);
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeBinary(Path path, byte[] content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }

    private int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8765; // fallback
        }
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }
}
