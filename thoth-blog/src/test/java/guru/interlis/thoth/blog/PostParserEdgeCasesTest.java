package guru.interlis.thoth.blog;

import org.asciidoctor.Asciidoctor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and error handling tests for PostParser.
 */
class PostParserEdgeCasesTest {

    @TempDir Path tempDir;

    private Asciidoctor asciidoctor;
    private PostParser parser;

    @BeforeEach
    void setUp() {
        asciidoctor = Asciidoctor.Factory.create();
        parser = new PostParser(asciidoctor);
    }

    @AfterEach
    void tearDown() {
        asciidoctor.shutdown();
    }

    @Test
    void throwsWhenNoFrontMatterDelimiter() {
        Path post = writePost("missing-delimiter.adoc", """
            = No Delimiter
            Jane Doe
            2026-01-12
            Body text.
            """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(post, tempDir));
    }

    @Test
    void throwsWhenClosingDelimiterMissing() {
        Path post = writePost("no-closing.adoc", """
            ---
            = No Closing Delimiter
            Jane Doe
            2026-01-12
            Body text without closing delimiter.
            """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(post, tempDir));
    }

    @Test
    void throwsWhenHeaderTooShort() {
        Path post = writePost("short-header.adoc", """
            ---
            = Title Only
            ---
            Body.
            """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(post, tempDir));
    }

    @Test
    void throwsWhenTitleLineInvalid() {
        Path post = writePost("bad-title.adoc", """
            ---
            Not A Title
            Jane Doe
            2026-01-12
            ---
            Body.
            """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(post, tempDir));
    }

    @Test
    void throwsWhenDateInvalid() {
        Path post = writePost("bad-date.adoc", """
            ---
            = Title
            Jane Doe
            not-a-date
            ---
            Body.
            """);

        assertThrows(Exception.class, () -> parser.parse(post, tempDir));
    }

    @Test
    void handlesEmptyPostFile() {
        Path post = writePost("empty.adoc", "");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(post, tempDir));
    }

    @Test
    void handlesEmptyTags() throws Exception {
        Path post = writePost("no-tags.adoc", """
            ---
            = No Tags
            Jane Doe
            2026-01-12
            :thoth-tags:
            ---
            Body text.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertTrue(parsed.tags().isEmpty());
    }

    @Test
    void handlesOnlyCommasInTags() throws Exception {
        Path post = writePost("comma-tags.adoc", """
            ---
            = Comma Tags
            Jane Doe
            2026-01-12
            :thoth-tags: , , ,
            ---
            Body text.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertTrue(parsed.tags().isEmpty());
    }

    @Test
    void deduplicatesTags() throws Exception {
        Path post = writePost("dup-tags.adoc", """
            ---
            = Duplicate Tags
            Jane Doe
            2026-01-12
            :thoth-tags: Java,java,JAVA,AI,ai
            ---
            Body text.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertEquals(2, parsed.tags().size());
        assertEquals("Java", parsed.tags().get(0).name());
        assertEquals("AI", parsed.tags().get(1).name());
    }

    @Test
    void generatesCorrectPrettyUrlForNestedPath() throws Exception {
        Path blogDir = tempDir.resolve("blog/2026");
        Files.createDirectories(blogDir);
        Path post = blogDir.resolve("hello-world.adoc");
        Files.writeString(post, """
            ---
            = Hello World
            Jane Doe
            2026-01-12
            ---
            Body text.
            """, StandardCharsets.UTF_8);

        Post parsed = parser.parse(post, tempDir);

        assertEquals("/blog/2026/hello-world/", parsed.url());
        assertEquals("blog/2026/hello-world/", parsed.guid());
        assertEquals(Path.of("blog/2026/hello-world/index.html"), parsed.outputRelativePath());
    }

    @Test
    void generatesCorrectUrlForRootPost() throws Exception {
        Path post = tempDir.resolve("post.adoc");
        Files.writeString(post, """
            ---
            = Root Post
            Jane Doe
            2026-01-12
            ---
            Body text.
            """, StandardCharsets.UTF_8);

        Post parsed = parser.parse(post, tempDir);

        assertEquals("/post/", parsed.url());
        assertEquals("post/", parsed.guid());
        assertEquals(Path.of("post/index.html"), parsed.outputRelativePath());
    }

    @Test
    void usesDefaultStatusWhenMissing() throws Exception {
        Path post = writePost("no-status.adoc", """
            ---
            = No Status
            Jane Doe
            2026-01-12
            ---
            Body text.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertEquals("published", parsed.status());
    }

    @Test
    void plainTextHasReasonableLength() throws Exception {
        Path post = writePost("plain.adoc", """
            ---
            = Plain Text Test
            Jane Doe
            2026-01-12
            ---
            This is a **bold** statement with some _emphasis_.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertNotNull(parsed.plainText());
        assertTrue(parsed.plainText().contains("bold"));
        assertFalse(parsed.plainText().contains("**"));
    }

    @Test
    void tagsContainNameAndSlug() throws Exception {
        Path post = writePost("tag-test.adoc", """
            ---
            = Tag Test
            Jane Doe
            2026-01-12
            :thoth-tags: Java,Machine Learning
            ---
            Body.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertEquals(2, parsed.tags().size());

        TagRef first = parsed.tags().get(0);
        assertEquals("Java", first.name());
        assertEquals("java", first.slug());

        TagRef second = parsed.tags().get(1);
        assertEquals("Machine Learning", second.name());
        assertEquals("machine-learning", second.slug());
    }

    @Test
    void tagsAsTextReturnsCommaSeparatedNames() throws Exception {
        Path post = writePost("tags-text.adoc", """
            ---
            = Tags Text Test
            Jane Doe
            2026-01-12
            :thoth-tags: Java,AI
            ---
            Body.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertEquals("Java, AI", parsed.tagsAsText());
    }

    @Test
    void externalLinksAreNotRewritten() throws Exception {
        Path post = writePost("external-link.adoc", """
            ---
            = External Link
            Jane Doe
            2026-01-12
            ---
            Visit https://example.com[Example].
            """);

        Post parsed = parser.parse(post, tempDir);
        assertTrue(parsed.htmlContent().contains("https://example.com"));
    }

    @Test
    void relativeImageIsRewrittenToAbsoluteSitePath() throws Exception {
        Path imgDir = tempDir.resolve("blog/images");
        Files.createDirectories(imgDir);
        Path post = imgDir.resolve("../post.adoc").normalize();
        Files.createDirectories(post.getParent());
        Files.writeString(post, """
            ---
            = Image Post
            Jane Doe
            2026-01-12
            ---
            image::images/pic.png[Pic]
            """, StandardCharsets.UTF_8);

        Post parsed = parser.parse(post, tempDir);
        assertTrue(parsed.htmlContent().contains("src=\"/blog/images/pic.png\""));
    }

    @Test
    void sourceRelativePathIsCorrect() throws Exception {
        Path blogDir = tempDir.resolve("blog/2026");
        Files.createDirectories(blogDir);
        Path post = blogDir.resolve("test.adoc");
        Files.writeString(post, """
            ---
            = Test
            Jane Doe
            2026-01-12
            ---
            Body.
            """, StandardCharsets.UTF_8);

        Post parsed = parser.parse(post, tempDir);
        assertEquals(Path.of("blog/2026/test.adoc"), parsed.sourceRelativePath());
    }

    @Test
    void dateIsParsedCorrectly() throws Exception {
        Path post = writePost("date-test.adoc", """
            ---
            = Date Test
            Jane Doe
            2026-03-17
            ---
            Body.
            """);

        Post parsed = parser.parse(post, tempDir);
        assertEquals(LocalDate.of(2026, 3, 17), parsed.date());
    }

    private Path writePost(String name, String content) {
        try {
            Path post = tempDir.resolve(name);
            Files.createDirectories(post.getParent());
            Files.writeString(post, content, StandardCharsets.UTF_8);
            return post;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
