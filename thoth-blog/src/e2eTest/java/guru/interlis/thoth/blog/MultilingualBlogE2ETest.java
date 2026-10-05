package guru.interlis.thoth.blog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jsoup.Jsoup;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MultilingualBlogE2ETest {
    @TempDir Path root;

    private void write(Path file, String text) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private String post(String title, String status) {
        return "---\n= " + title + "\nAlice\n2026-10-05\n:thoth-id: shared\n:thoth-status: " + status + "\n---\nText";
    }

    @Test
    void servesLanguageTargetsAndRetractsDraftsFromDirectHttpRequests() throws Exception {
        Path input = root.resolve("input");
        Path output = root.resolve("output");
        write(input.resolve("thoth.properties"), """
            site.title=Blog
            site.description=Description
            site.baseUrl=https://example.com
            site.language=de
            site.languages=de,en
            site.dateFormat=yyyy-MM-dd
            """);
        write(input.resolve("blog/de/2026/original.adoc"), post("Original", "published"));
        Path translation = input.resolve("blog/en/2026/translated.adoc");
        write(translation, post("Translated", "published"));
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        URI base = URI.create("http://localhost:" + port);
        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
            DevServer server = new DevServer(output, port);
            server.start();
            try {
                var original = get(client, base.resolve("/2026/original/"));
                assertEquals(200, original.statusCode());
                String target = Jsoup.parse(original.body()).selectFirst("#language-switch option[lang=en]").val();
                var translated = get(client, base.resolve(target));
                assertEquals(200, translated.statusCode());
                assertEquals("en", Jsoup.parse(translated.body()).selectFirst("html").attr("lang"));
                assertEquals(200, get(client, base.resolve("/en/search.html?q=Text")).statusCode());
                assertTrue(get(client, base.resolve("/en/assets/search-index.json")).body().contains("Translated"));
                assertTrue(get(client, base.resolve("/en/feed.xml")).body().contains("Translated"));
                write(translation, post("Unfinished translation", "draft"));
                generator.handleInputEvent(translation, "MODIFY");
                assertEquals(404, get(client, base.resolve(target)).statusCode());
                assertEquals(404, get(client, base.resolve(target + "index.html")).statusCode());
                assertFalse(get(client, base.resolve("/en/assets/search-index.json")).body().contains("Unfinished"));
                assertFalse(get(client, base.resolve("/en/feed.xml")).body().contains("Unfinished"));
                assertEquals("/en/index.html", Jsoup.parse(get(client, base.resolve("/2026/original/")).body())
                    .selectFirst("#language-switch option[lang=en]").val());
            } finally {
                server.stop();
            }
        }
    }

    private HttpResponse<String> get(HttpClient client, URI uri) throws Exception {
        return client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
