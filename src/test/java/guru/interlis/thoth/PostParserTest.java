package guru.interlis.thoth;

import org.asciidoctor.Asciidoctor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostParserTest {

    @Test
    public void parsesFrontMatterAndOverrides() throws Exception {
        Path root = Files.createTempDirectory("thoth-parser-test");
        Path post = root.resolve("blog/post.adoc");
        Files.createDirectories(post.getParent());

        String content = """
            ---
            = Sample Title
            Jane Doe
            2026-01-12
            :thoth-status: draft
            :thoth-tags: Java, AI,  ,MCP
            :thoth-teaser: Custom teaser
            :thoth-cover-image: images/cover.png
            ---
            Body text.
            """;

        Files.writeString(post, content, StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        try {
            PostParser parser = new PostParser(asciidoctor);
            Post parsed = parser.parse(post, root);

            assertEquals("Sample Title", parsed.title());
            assertEquals("Jane Doe", parsed.author());
            assertEquals(LocalDate.parse("2026-01-12"), parsed.date());
            assertEquals("draft", parsed.status());
            assertEquals(3, parsed.tags().size());
            assertEquals("Java", parsed.tags().get(0).name());
            assertEquals("java", parsed.tags().get(0).slug());
            assertEquals("Custom teaser", parsed.teaser());
            assertEquals("/blog/images/cover.png", parsed.coverImage());
            assertEquals("/blog/post/", parsed.url());
            assertEquals("blog/post/", parsed.guid());
        } finally {
            asciidoctor.shutdown();
        }
    }

    // highlight.js-specific tests removed
}
