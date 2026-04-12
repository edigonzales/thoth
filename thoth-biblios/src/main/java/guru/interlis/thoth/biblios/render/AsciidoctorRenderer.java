package guru.interlis.thoth.biblios.render;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

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

    private static final String DEFAULT_LANGUAGE = "en";
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
            String normalizedHtml = normalizeCodeBlocksForPrism(html);
            String title = document.getDoctitle();
            String imagesDir = attributeAsString(document.getAttribute("imagesdir"));
            String baseDir = sourcePath.getParent() != null
                ? sourcePath.getParent().toAbsolutePath().normalize().toString()
                : "";
            List<Heading> headings = resolvedOptions.collectHeadings()
                ? extractHeadings(document, resolvedOptions.headingDepth())
                : List.of();

            return new RenderedDocument(
                normalizedHtml,
                title != null ? title : "",
                List.copyOf(headings),
                imagesDir,
                baseDir
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
        return renderString(content, DEFAULT_LANGUAGE);
    }

    /**
     * Render AsciiDoc content from a string to HTML with an explicit language.
     *
     * @param content AsciiDoc content string
     * @param language document language (for localized labels)
     * @return rendered HTML content
     */
    public String renderString(String content, String language) {
        try {
            AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
            attributes.attribute("source-highlighter", "null");
            attributes.attribute("icons", "font");
            attributes.attribute("sectanchors", "");
            String resolvedLanguage = normalizeLanguage(language);
            attributes.attribute("lang", resolvedLanguage);

            OptionsBuilder options = org.asciidoctor.Options.builder()
                .backend("html5")
                .safe(SafeMode.UNSAFE)
                .standalone(false)
                .toFile(false)
                .attributes(attributes.build());

            return normalizeCodeBlocksForPrism(asciidoctor.convert(content, options.build()));
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
        attributes.attribute("source-highlighter", "null");
        attributes.attribute("icons", "font");
        attributes.attribute("sectanchors", "");
        String resolvedLanguage = normalizeLanguage(options.language());
        attributes.attribute("lang", resolvedLanguage);

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

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        return language.trim();
    }

    private static String attributeAsString(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        return text != null ? text.trim() : "";
    }

    private String normalizeCodeBlocksForPrism(String html) {
        if (html == null || html.isBlank()) {
            return html != null ? html : "";
        }
        org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(html);
        for (Element code : document.select("pre > code")) {
            Element pre = code.parent();
            String language = detectLanguage(code, pre);
            if (language == null) {
                continue;
            }
            String normalizedLanguage = normalizeLanguageAlias(language);
            code.addClass("language-" + normalizedLanguage);
            code.removeAttr("data-lang");
            if (pre != null && "pre".equals(pre.tagName())) {
                pre.addClass("language-" + normalizedLanguage);
                pre.removeAttr("data-lang");
            }
        }
        return document.body().html();
    }

    private String detectLanguage(Element code, Element pre) {
        String fromCode = detectLanguageFromElement(code);
        if (fromCode != null) {
            return fromCode;
        }
        return detectLanguageFromElement(pre);
    }

    private String detectLanguageFromElement(Element element) {
        if (element == null) {
            return null;
        }

        String dataLang = element.attr("data-lang").trim();
        if (!dataLang.isEmpty()) {
            return dataLang;
        }

        String language = element.attr("language").trim();
        if (!language.isEmpty()) {
            return language;
        }

        String lang = element.attr("lang").trim();
        if (!lang.isEmpty()) {
            return lang;
        }

        for (String className : element.classNames()) {
            if (className.startsWith("language-")) {
                return className.substring("language-".length());
            }
            if (className.startsWith("lang-")) {
                return className.substring("lang-".length());
            }
            if (className.startsWith("highlight-source-")) {
                return className.substring("highlight-source-".length());
            }
        }
        return null;
    }

    private String normalizeLanguageAlias(String language) {
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "yml" -> "yaml";
            case "sh", "shell", "shell-session", "zsh", "bash" -> "bash";
            case "html", "xml", "svg", "mathml" -> "markup";
            default -> normalized;
        };
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

            String sectionNumber = normalizeSectionNumber(section);
            boolean unnumbered = isUnnumberedSection(section);
            boolean appendix = isAppendixSection(section);
            List<Heading> children = mapSections(childSections(section), minLevel, maxDepth);
            result.add(new Heading(id, title, normalizedLevel, sectionNumber, unnumbered, appendix, List.copyOf(children)));
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

    private String normalizeSectionNumber(Section section) {
        if (section == null || !section.isNumbered()) {
            return "";
        }
        String raw = section.getSectnum();
        if (raw == null || raw.isBlank()) {
            raw = section.getNumeral();
        }
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim();
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private boolean isUnnumberedSection(Section section) {
        if (section == null) {
            return false;
        }

        if (!section.isNumbered()) {
            return true;
        }

        // Fallback for documents where the role is preserved in the AST.
        return section.hasRole("unnumbered");
    }

    private boolean isAppendixSection(Section section) {
        if (section == null) {
            return false;
        }
        return section.hasRole("appendix");
    }

    public record RenderOptions(
        boolean sectionNumbers,
        boolean contentToc,
        boolean collectHeadings,
        int headingDepth,
        String language
    ) {
        public static RenderOptions legacyDefaults() {
            return legacyDefaults(DEFAULT_LANGUAGE);
        }

        public static RenderOptions legacyDefaults(String language) {
            return new RenderOptions(true, true, false, 2, normalizeLanguage(language));
        }

        public static RenderOptions split(boolean contentToc) {
            return split(contentToc, DEFAULT_LANGUAGE);
        }

        public static RenderOptions split(boolean contentToc, String language) {
            return new RenderOptions(true, contentToc, false, 2, normalizeLanguage(language));
        }

        public static RenderOptions singlePage(boolean contentToc, int headingDepth) {
            return singlePage(contentToc, headingDepth, DEFAULT_LANGUAGE);
        }

        public static RenderOptions singlePage(boolean contentToc, int headingDepth, String language) {
            return new RenderOptions(true, contentToc, true, headingDepth, normalizeLanguage(language));
        }
    }

    public record Heading(String id, String title, int level, String sectionNumber, boolean unnumbered, boolean appendix, List<Heading> children) {
    }

    public record RenderedDocument(String html, String title, List<Heading> headings, String imagesDir, String baseDir) {
    }
}
