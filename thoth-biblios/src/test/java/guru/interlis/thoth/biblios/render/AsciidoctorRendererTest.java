package guru.interlis.thoth.biblios.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AsciidoctorRenderer.
 */
class AsciidoctorRendererTest {

    @Test
    void rendersSimpleContent(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("test.adoc");
        Files.writeString(adoc, "= Hello\n\nThis is a test.");

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString("= Hello\n\nThis is a test.");
            assertNotNull(html);
            assertTrue(html.contains("Hello") || html.contains("This is a test"));
        }
    }

    @Test
    void rendersSectionsAndLists(@TempDir Path tempDir) throws Exception {
        String content = """
            = User Guide

            == Introduction

            This is the introduction.

            == Features

            * Feature 1
            * Feature 2
            * Feature 3
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertNotNull(html);
            assertTrue(html.contains("User Guide") || html.contains("Introduction"));
        }
    }

    @Test
    void rendersCodeBlocks() {
        String content = """
            = Code Example

            [source,java]
            ----
            public class Hello {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            ----
            """;

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString(content);
            assertNotNull(html);
        }
    }

    @Test
    void handlesEmptyContent() {
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            String html = renderer.renderString("");
            // Empty content may return null or empty string
            assertNotNull(html);
        }
    }

    @Test
    void extractsHeadingsFromRenderedDocument(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("master.adoc");
        Files.writeString(adoc, """
            = Manual
            :doctype: book

            == Einleitung

            === Status

            OK
            """);

        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.singlePage(false, 3)
            );

            assertNotNull(rendered);
            assertFalse(rendered.headings().isEmpty());
            assertEquals("Einleitung", rendered.headings().get(0).title());
            assertEquals(1, rendered.headings().get(0).level());
            assertFalse(rendered.headings().get(0).children().isEmpty());
            assertEquals("Status", rendered.headings().get(0).children().get(0).title());
            assertFalse(rendered.html().contains("id=\"toc\""));
        }
    }
}
