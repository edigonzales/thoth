package guru.interlis.thoth.biblios.render;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders AsciiDoc content to HTML using AsciidoctorJ.
 * Thread-safe after initialization. Should be closed when done.
 */
public final class AsciidoctorRenderer implements AutoCloseable {

    private final Asciidoctor asciidoctor;

    public AsciidoctorRenderer() {
        this.asciidoctor = Asciidoctor.Factory.create();
    }

    /**
     * Render AsciiDoc content from a file to HTML.
     * Uses legacy defaults (section numbering + in-content TOC enabled).
     */
    public String renderFile(Path sourcePath) throws IOException {
        return renderDocument(sourcePath, RenderOptions.legacyDefaults()).html();
    }

    /**
     * Render a file and optionally extract heading structure from the AST.
     */
    public RenderedDocument renderDocument(Path sourcePath, RenderOptions options) throws IOException {
        try {
            RenderOptions resolvedOptions = options != null ? options : RenderOptions.legacyDefaults();
            Options asciidoctorOptions = buildOptions(sourcePath, resolvedOptions);

            Document document = asciidoctor.loadFile(sourcePath.toFile(), asciidoctorOptions);
            String html = asciidoctor.convertFile(sourcePath.toFile(), asciidoctorOptions, String.class);
            String title = document.getDoctitle();
            List<Heading> headings = resolvedOptions.collectHeadings()
                ? extractHeadings(document, resolvedOptions.headingDepth())
                : List.of();

            return new RenderedDocument(
                html != null ? html : "",
                title != null ? title : "",
                List.copyOf(headings)
            );
        } catch (Exception e) {
            throw new IOException("Failed to render AsciiDoc file: " + sourcePath, e);
        }
    }

    /**
     * Render AsciiDoc content from a string to HTML.
     *
     * @param content AsciiDoc content string
     * @return rendered HTML content
     */
    public String renderString(String content) {
        try {
            AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
            attributes.attribute("source-highlighter", "prettify");

            OptionsBuilder options = org.asciidoctor.Options.builder()
                .backend("html5")
                .safe(SafeMode.UNSAFE)
                .standalone(false)
                .toFile(false)
                .attributes(attributes.build());

            return asciidoctor.convert(content, options.build());
        } catch (Exception e) {
            String snippet = content != null && content.length() > 80
                ? content.substring(0, 80) + "..."
                : String.valueOf(content);
            throw new RuntimeException("Failed to render AsciiDoc content: " + snippet, e);
        }
    }

    /**
     * Extract the document title from AsciiDoc content.
     *
     * @param sourcePath path to the .adoc file
     * @return the document title, or null if not found
     */
    public String extractTitle(Path sourcePath) {
        try {
            OptionsBuilder options = org.asciidoctor.Options.builder()
                .safe(SafeMode.UNSAFE)
                .standalone(false);

            Document doc = asciidoctor.loadFile(
                sourcePath.toFile(),
                options.build()
            );
            return doc.getDoctitle();
        } catch (Exception e) {
            System.err.println("[warn] Failed to extract title from: " + sourcePath + " (" + e.getMessage() + ")");
            return null;
        }
    }

    @Override
    public void close() {
        try {
            asciidoctor.close();
        } catch (Exception ignored) {
        }
    }

    private Options buildOptions(Path sourcePath, RenderOptions options) {
        AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
        attributes.attribute("source-highlighter", "prettify");
        attributes.attribute("icons", "font");

        if (options.sectionNumbers()) {
            attributes.attribute("sectnums", "");
            attributes.attribute("sectnumlevels", "6");
        } else {
            attributes.attribute("sectnums!", "");
        }

        if (options.contentToc()) {
            attributes.attribute("toc", "left");
        } else {
            attributes.attribute("toc!", "");
        }

        return org.asciidoctor.Options.builder()
            .backend("html5")
            .safe(SafeMode.UNSAFE)
            .standalone(false)
            .toFile(false)
            .baseDir(sourcePath.getParent().toFile())
            .attributes(attributes.build())
            .build();
    }

    private List<Heading> extractHeadings(Document document, int maxDepth) {
        List<Section> topSections = childSections(document);
        if (topSections.isEmpty()) {
            return List.of();
        }

        int minLevel = topSections.stream()
            .mapToInt(Section::getLevel)
            .min()
            .orElse(1);

        int depth = Math.max(1, Math.min(6, maxDepth));
        return mapSections(topSections, minLevel, depth);
    }

    private List<Heading> mapSections(List<Section> sections, int minLevel, int maxDepth) {
        List<Heading> result = new ArrayList<>();
        int index = 0;
        for (Section section : sections) {
            index++;
            int normalizedLevel = Math.max(1, section.getLevel() - minLevel + 1);
            if (normalizedLevel > maxDepth) {
                continue;
            }

            String title = section.getTitle() != null ? section.getTitle().trim() : "";
            if (title.isEmpty()) {
                continue;
            }

            String id = section.getId();
            if (id == null || id.isBlank()) {
                String slug = slugify(title);
                id = slug.isBlank() ? "section-" + normalizedLevel + "-" + index : slug;
            }

            List<Heading> children = mapSections(childSections(section), minLevel, maxDepth);
            result.add(new Heading(id, title, normalizedLevel, List.copyOf(children)));
        }
        return result;
    }

    private List<Section> childSections(StructuralNode node) {
        List<Section> sections = new ArrayList<>();
        for (StructuralNode block : node.getBlocks()) {
            if (block instanceof Section section) {
                sections.add(section);
            }
        }
        return sections;
    }

    private String slugify(String input) {
        String normalized = input.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .trim()
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-");
        return normalized;
    }

    public record RenderOptions(boolean sectionNumbers, boolean contentToc, boolean collectHeadings, int headingDepth) {
        public static RenderOptions legacyDefaults() {
            return new RenderOptions(true, true, false, 2);
        }

        public static RenderOptions split(boolean contentToc) {
            return new RenderOptions(true, contentToc, false, 2);
        }

        public static RenderOptions singlePage(boolean contentToc, int headingDepth) {
            return new RenderOptions(true, contentToc, true, headingDepth);
        }
    }

    public record Heading(String id, String title, int level, List<Heading> children) {
    }

    public record RenderedDocument(String html, String title, List<Heading> headings) {
    }
}
