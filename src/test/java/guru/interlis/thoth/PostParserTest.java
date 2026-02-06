package guru.interlis.thoth;

import org.asciidoctor.Asciidoctor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    public void normalizesCodeBlocksForPrism() throws Exception {
        Path root = Files.createTempDirectory("thoth-parser-prism-test");
        Path post = root.resolve("blog/code.adoc");
        Files.createDirectories(post.getParent());

        String content = """
            ---
            = Prism Test
            Jane Doe
            2026-01-12
            ---
            [source,js]
            ----
            const value = 1;
            ----

            [source,css]
            ----
            body { color: red; }
            ----
            """;

        Files.writeString(post, content, StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        try {
            PostParser parser = new PostParser(asciidoctor);
            Post parsed = parser.parse(post, root);
            Document html = Jsoup.parseBodyFragment(parsed.htmlContent());

            assertTrue(html.select("pre.language-javascript > code.language-javascript").size() == 1);
            assertTrue(html.select("pre.language-css > code.language-css").size() == 1);
        } finally {
            asciidoctor.shutdown();
        }
    }

    @Test
    public void appliesLineNumbersOnlyWhenRequested() throws Exception {
        Path root = Files.createTempDirectory("thoth-parser-linenums-test");
        Path post = root.resolve("blog/linenums.adoc");
        Files.createDirectories(post.getParent());

        String content = """
            ---
            = Line Numbers
            Jane Doe
            2026-01-12
            ---
            [source,ini,linenums]
            ----
            [ch.ehi.ili2db]
            defaultSrsCode=2056
            ----

            [source,ini]
            ----
            [ch.ehi.ili2db]
            createEnumTabs=true
            ----
            """;

        Files.writeString(post, content, StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        try {
            PostParser parser = new PostParser(asciidoctor);
            Post parsed = parser.parse(post, root);
            Document html = Jsoup.parseBodyFragment(parsed.htmlContent());

            var codeBlocks = html.select("pre.language-ini");
            assertEquals(2, codeBlocks.size());
            assertTrue(codeBlocks.get(0).classNames().contains("line-numbers"));
            assertFalse(codeBlocks.get(1).classNames().contains("line-numbers"));
        } finally {
            asciidoctor.shutdown();
        }
    }
}
