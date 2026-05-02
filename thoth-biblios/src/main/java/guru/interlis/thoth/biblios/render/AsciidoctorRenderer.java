package guru.interlis.thoth.biblios.render;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.asciidoctor.jruby.internal.RubyUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders AsciiDoc content to HTML using AsciidoctorJ.
 * Thread-safe after initialization. Should be closed when done.
 */
public final class AsciidoctorRenderer implements AutoCloseable {
    static final String BUNDLED_INTERLIS_ROUGE_LEXER = "ruby/interlis_rouge_lexer.rb";

    private static final String DEFAULT_LANGUAGE = "en";
    private static final Set<String> STANDALONE_MARKERS = Set.of("*", "§", "※", "✱", "✶", "✳");
    private static final Set<String> PDF_BOOLEAN_PRESENCE_ATTRIBUTES = Set.of(
        "toc",
        "sectnums",
        "title-page"
    );
    private static final Set<String> PDF_FALSE_UNSET_ATTRIBUTES = Set.of(
        "chapter-signifier",
        "chapter-refsig",
        "section-refsig",
        "appendix-refsig",
        "part-signifier",
        "part-refsig"
    );
    private static final Pattern NUMERIC_CONUM_PATTERN = Pattern.compile("<(\\d+)>");
    private final Asciidoctor asciidoctor;
    private final Set<Path> loadedRubyRequires = new HashSet<>();
    private final Set<String> loadedBundledRubyRequires = new HashSet<>();

    public AsciidoctorRenderer() {
        this(false);
    }

    public AsciidoctorRenderer(boolean loadBundledRubyRequires) {
        this(
            Asciidoctor.Factory.create(),
            loadBundledRubyRequires ? List.of(BUNDLED_INTERLIS_ROUGE_LEXER) : List.of()
        );
    }

    AsciidoctorRenderer(Asciidoctor asciidoctor, List<String> bundledRubyRequires) {
        this.asciidoctor = asciidoctor;
        try {
            loadBundledRubyRequires(bundledRubyRequires);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Biblios Ruby runtime.", e);
        }
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
        return renderDocument(sourcePath, options, Map.of());
    }

    public RenderedDocument renderDocument(Path sourcePath, RenderOptions options, Map<String, Object> additionalAttributes) throws IOException {
        try {
            RenderOptions resolvedOptions = options != null ? options : RenderOptions.legacyDefaults();
            Options asciidoctorOptions = buildOptions(sourcePath, resolvedOptions, additionalAttributes);

            Document document = asciidoctor.loadFile(sourcePath.toFile(), asciidoctorOptions);
            String html = asciidoctor.convertFile(sourcePath.toFile(), asciidoctorOptions, String.class);
            String normalizedHtml = normalizeCodeBlocksForPrism(html);
            String title = document.getDoctitle();
            String imagesDir = attributeAsString(document.getAttribute("imagesdir"));
            String baseDir = sourcePath.getParent() != null
                ? sourcePath.getParent().toAbsolutePath().normalize().toString()
                : "";
            List<Heading> headings = resolvedOptions.collectHeadings()
                ? extractHeadings(document, resolvedOptions.headingDepth(), resolvedOptions.sectionNumbers())
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
            AttributesBuilder attributes = baseHtmlAttributes(language);

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

    public void writePdf(Path sourcePath, Path targetPath, Map<String, Object> pdfAttributes, String language) throws IOException {
        writePdf(sourcePath, targetPath, pdfAttributes, language, Map.of());
    }

    public void writePdf(Path sourcePath, Path targetPath, Map<String, Object> pdfAttributes, String language,
                         Map<String, Object> additionalAttributes) throws IOException {
        writePdf(sourcePath, targetPath, pdfAttributes, language, additionalAttributes, List.of());
    }

    public void writePdf(Path sourcePath, Path targetPath, Map<String, Object> pdfAttributes, String language,
                         Map<String, Object> additionalAttributes, List<String> rubyRequires) throws IOException {
        try {
            Files.createDirectories(targetPath.toAbsolutePath().normalize().getParent());
            loadRubyRequires(rubyRequires);
            Options options = buildPdfOptions(sourcePath, targetPath, pdfAttributes, language, additionalAttributes);
            asciidoctor.convertFile(sourcePath.toFile(), options);
        } catch (Exception e) {
            throw new IOException("Failed to render PDF from AsciiDoc file: " + sourcePath, e);
        }
    }

    private Options buildOptions(Path sourcePath, RenderOptions options, Map<String, Object> additionalAttributes) {
        AttributesBuilder attributes = baseHtmlAttributes(options.language());

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
        if (additionalAttributes != null) {
            for (Map.Entry<String, Object> entry : additionalAttributes.entrySet()) {
                attributes.attribute(entry.getKey(), entry.getValue());
            }
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

    private Options buildPdfOptions(Path sourcePath, Path targetPath, Map<String, Object> pdfAttributes, String language,
                                    Map<String, Object> additionalAttributes) {
        AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
        String resolvedLanguage = normalizeLanguage(language);
        attributes.attribute("lang", resolvedLanguage);

        Map<String, Object> mergedAttributes = new LinkedHashMap<>();
        mergedAttributes.putAll(defaultLocalizedAttributesForLanguage(resolvedLanguage));
        if (pdfAttributes != null) {
            mergedAttributes.putAll(pdfAttributes);
        }
        if (additionalAttributes != null) {
            mergedAttributes.putAll(additionalAttributes);
        }
        for (Map.Entry<String, Object> entry : mergedAttributes.entrySet()) {
            applyPdfAttribute(attributes, entry.getKey(), entry.getValue());
        }

        return org.asciidoctor.Options.builder()
            .backend("pdf")
            .safe(SafeMode.UNSAFE)
            .toFile(targetPath.toFile())
            .baseDir(sourcePath.getParent().toFile())
            .attributes(attributes.build())
            .build();
    }

    static Map<String, Object> defaultLocalizedAttributesForLanguage(String language) {
        String resolvedLanguage = normalizeLanguage(language).toLowerCase(Locale.ROOT);
        if ("de".equals(resolvedLanguage)) {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("note-caption", "Hinweis@");
            defaults.put("tip-caption", "Tipp@");
            defaults.put("important-caption", "Wichtig@");
            defaults.put("warning-caption", "Warnung@");
            defaults.put("caution-caption", "Vorsicht@");
            return Map.copyOf(defaults);
        }
        return Map.of();
    }

    private void applyPdfAttribute(AttributesBuilder attributes, String key, Object value) {
        if (value instanceof Boolean bool && PDF_BOOLEAN_PRESENCE_ATTRIBUTES.contains(key)) {
            attributes.attribute(bool ? key : key + "!", "");
            return;
        }
        if (value instanceof Boolean bool && !bool && PDF_FALSE_UNSET_ATTRIBUTES.contains(key)) {
            attributes.attribute(key + "!", "");
            return;
        }
        attributes.attribute(key, value);
    }

    void loadRubyRequires(List<String> rubyRequires) throws IOException {
        if (rubyRequires == null || rubyRequires.isEmpty()) {
            return;
        }

        for (String rawPath : rubyRequires) {
            if (rawPath == null || rawPath.isBlank()) {
                continue;
            }

            Path requirePath = Path.of(rawPath).toAbsolutePath().normalize();
            if (loadedRubyRequires.contains(requirePath)) {
                continue;
            }
            if (!Files.exists(requirePath) || !Files.isRegularFile(requirePath)) {
                throw new IOException(
                    "Failed to load PDF Ruby require: file not found: " + requirePath +
                        ". This hook is intended for custom Rouge lexers or other Ruby extensions."
                );
            }

            try (InputStream input = Files.newInputStream(requirePath)) {
                loadRubyStream(input);
                loadedRubyRequires.add(requirePath);
            } catch (Exception e) {
                throw new IOException(
                    "Failed to load PDF Ruby require: " + requirePath +
                        ". This hook is intended for custom Rouge lexers or other Ruby extensions.",
                    e
                );
            }
        }
    }

    private void loadBundledRubyRequires(List<String> bundledRubyRequires) throws IOException {
        if (bundledRubyRequires == null || bundledRubyRequires.isEmpty()) {
            return;
        }

        for (String resourcePath : bundledRubyRequires) {
            if (resourcePath == null || resourcePath.isBlank() || loadedBundledRubyRequires.contains(resourcePath)) {
                continue;
            }

            try (InputStream input = AsciidoctorRenderer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (input == null) {
                    throw new IOException(
                        "Failed to load bundled Ruby require: resource not found: " + resourcePath +
                            ". This hook is intended for built-in Rouge lexers or other Ruby extensions."
                    );
                }
                loadRubyStream(input);
                loadedBundledRubyRequires.add(resourcePath);
            } catch (Exception e) {
                if (e instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException(
                    "Failed to load bundled Ruby require: " + resourcePath +
                        ". This hook is intended for built-in Rouge lexers or other Ruby extensions.",
                    e
                );
            }
        }
    }

    private void loadRubyStream(InputStream input) throws IOException {
        if (!(asciidoctor instanceof JRubyAsciidoctor jrubyAsciidoctor)) {
            throw new IOException("Custom PDF Ruby requires are only supported with the JRuby-based Asciidoctor runtime.");
        }
        RubyUtils.loadRubyClass(jrubyAsciidoctor.getRubyRuntime(), input);
    }

    private AttributesBuilder baseHtmlAttributes(String language) {
        AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
        attributes.attribute("source-highlighter", "null");
        attributes.attribute("icons", "font");
        attributes.attribute("sectanchors", "");
        String resolvedLanguage = normalizeLanguage(language);
        attributes.attribute("lang", resolvedLanguage);
        for (Map.Entry<String, Object> entry : defaultLocalizedAttributesForLanguage(resolvedLanguage).entrySet()) {
            attributes.attribute(entry.getKey(), entry.getValue());
        }
        return attributes;
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
        markStandaloneMarkersBeforeListings(document);
        for (Element code : document.select("pre > code")) {
            Element pre = code.parent();
            replaceNumericCalloutsWithConums(code);
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

    private boolean replaceNumericCalloutsWithConums(Element code) {
        if (code == null) {
            return false;
        }
        return replaceNumericCalloutsRecursive(code);
    }

    private boolean replaceNumericCalloutsRecursive(Node node) {
        if (node instanceof TextNode textNode) {
            return replaceNumericCalloutsInTextNode(textNode);
        }

        boolean replaced = false;
        List<Node> children = new ArrayList<>(node.childNodes());
        for (Node child : children) {
            replaced |= replaceNumericCalloutsRecursive(child);
        }
        return replaced;
    }

    private boolean replaceNumericCalloutsInTextNode(TextNode textNode) {
        String text = textNode.getWholeText();
        Matcher matcher = NUMERIC_CONUM_PATTERN.matcher(text);
        if (!matcher.find()) {
            return false;
        }

        List<Node> replacements = new ArrayList<>();
        int cursor = 0;
        do {
            if (matcher.start() > cursor) {
                replacements.add(new TextNode(text.substring(cursor, matcher.start())));
            }

            String calloutValue = matcher.group(1);
            Element conum = new Element("i").addClass("conum");
            conum.attr("data-value", calloutValue);
            conum.attr("aria-hidden", "true");
            replacements.add(conum);

            Element fallback = new Element("b");
            fallback.text("(" + calloutValue + ")");
            replacements.add(fallback);

            cursor = matcher.end();
        } while (matcher.find());

        if (cursor < text.length()) {
            replacements.add(new TextNode(text.substring(cursor)));
        }

        for (Node replacement : replacements) {
            textNode.before(replacement);
        }
        textNode.remove();
        return true;
    }

    private void markStandaloneMarkersBeforeListings(org.jsoup.nodes.Document document) {
        markStandaloneMarkerParagraphsBeforeListings(document);
        markStandaloneMarkerAnchorsBeforeListings(document);
    }

    private void markStandaloneMarkerParagraphsBeforeListings(org.jsoup.nodes.Document document) {
        for (Element paragraph : document.select("div.paragraph")) {
            Element textParagraph = paragraph.selectFirst("> p");
            if (textParagraph == null || !isStandaloneMarkerParagraph(textParagraph)) {
                continue;
            }
            Element listing = findListingNearMarkerParagraph(paragraph);
            if (listing != null) {
                paragraph.addClass("marker-paragraph");
                listing.addClass("marker-following-marker");
            }
        }
    }

    private void markStandaloneMarkerAnchorsBeforeListings(org.jsoup.nodes.Document document) {
        for (Element anchor : document.select("a[href]")) {
            if (!isStandaloneMarkerAnchor(anchor)) {
                continue;
            }
            Element listing = findListingNearMarkerAnchor(anchor);
            if (listing != null) {
                anchor.addClass("marker-anchor");
                listing.addClass("marker-following-marker");
            }
        }
    }

    private Element findListingNearMarkerParagraph(Element paragraph) {
        Element nextSibling = nextRelevantSibling(paragraph);
        if (isListingBlock(nextSibling)) {
            return nextSibling;
        }
        if (isParagraph(nextSibling)) {
            Element nextAfterParagraph = nextRelevantSibling(nextSibling);
            if (isListingBlock(nextAfterParagraph)) {
                return nextAfterParagraph;
            }
        }
        return null;
    }

    private Element findListingNearMarkerAnchor(Element anchor) {
        Element listingByReference = resolveListingFromHref(anchor);
        if (listingByReference != null) {
            return listingByReference;
        }

        Element nextSibling = nextRelevantSibling(anchor);
        if (isListingBlock(nextSibling)) {
            return nextSibling;
        }
        if (isParagraph(nextSibling)) {
            Element nextAfterParagraph = nextRelevantSibling(nextSibling);
            if (isListingBlock(nextAfterParagraph)) {
                return nextAfterParagraph;
            }
        }
        return null;
    }

    private Element resolveListingFromHref(Element anchor) {
        String href = anchor.attr("href").trim();
        if (!href.startsWith("#") || href.length() <= 1) {
            return null;
        }
        String targetId = href.substring(1);
        Element target = anchor.ownerDocument() != null ? anchor.ownerDocument().getElementById(targetId) : null;
        return isListingBlock(target) ? target : null;
    }

    private Element nextRelevantSibling(Element element) {
        Element next = element != null ? element.nextElementSibling() : null;
        while (isAnchorTargetDiv(next)) {
            next = next.nextElementSibling();
        }
        return next;
    }

    private boolean isAnchorTargetDiv(Element element) {
        if (element == null || !"div".equals(element.tagName()) || !element.hasAttr("id")) {
            return false;
        }
        if (!element.classNames().isEmpty()) {
            return false;
        }
        return element.children().isEmpty() && element.text().isBlank();
    }

    private boolean isParagraph(Element element) {
        return element != null && "div".equals(element.tagName()) && element.hasClass("paragraph");
    }

    private boolean isListingBlock(Element element) {
        return element != null && "div".equals(element.tagName()) && element.hasClass("listingblock");
    }

    private boolean isStandaloneMarkerAnchor(Element anchor) {
        String visibleText = anchor.text().trim();
        if (!STANDALONE_MARKERS.contains(visibleText)) {
            return false;
        }
        String ownText = anchor.ownText();
        return ownText != null && STANDALONE_MARKERS.contains(ownText.trim());
    }

    private boolean isStandaloneMarkerParagraph(Element paragraph) {
        String visibleText = paragraph.text().trim();
        if (!STANDALONE_MARKERS.contains(visibleText)) {
            return false;
        }

        if (paragraph.childrenSize() == 0) {
            return true;
        }
        if (paragraph.childrenSize() != 1 || !"a".equals(paragraph.child(0).tagName())) {
            return false;
        }

        String ownText = paragraph.ownText();
        String linkedText = paragraph.child(0).text().trim();
        return (ownText == null || ownText.isBlank()) && STANDALONE_MARKERS.contains(linkedText);
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
        return extractHeadings(document, maxDepth, true);
    }

    private List<Heading> extractHeadings(Document document, int maxDepth, boolean sectionNumbersEnabled) {
        List<Section> topSections = childSections(document);
        if (topSections.isEmpty()) {
            return List.of();
        }

        int minLevel = topSections.stream()
            .mapToInt(Section::getLevel)
            .min()
            .orElse(1);

        int depth = Math.max(1, Math.min(6, maxDepth));
        return mapSections(topSections, minLevel, depth, sectionNumbersEnabled);
    }

    private List<Heading> mapSections(List<Section> sections, int minLevel, int maxDepth, boolean sectionNumbersEnabled) {
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
            boolean unnumbered = isUnnumberedSection(section, sectionNumbersEnabled);
            boolean appendix = isAppendixSection(section);
            List<Heading> children = mapSections(childSections(section), minLevel, maxDepth, sectionNumbersEnabled);
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

    private boolean isUnnumberedSection(Section section, boolean sectionNumbersEnabled) {
        if (section == null) {
            return false;
        }

        if (sectionNumbersEnabled && !section.isNumbered()) {
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
            return split(true, contentToc, language);
        }

        public static RenderOptions split(boolean sectionNumbers, boolean contentToc, String language) {
            return new RenderOptions(sectionNumbers, contentToc, false, 2, normalizeLanguage(language));
        }

        public static RenderOptions singlePage(boolean contentToc, int headingDepth) {
            return singlePage(contentToc, headingDepth, DEFAULT_LANGUAGE);
        }

        public static RenderOptions singlePage(boolean contentToc, int headingDepth, String language) {
            return singlePage(true, contentToc, headingDepth, language);
        }

        public static RenderOptions singlePage(boolean sectionNumbers, boolean contentToc, int headingDepth, String language) {
            return new RenderOptions(sectionNumbers, contentToc, true, headingDepth, normalizeLanguage(language));
        }
    }

    public record Heading(String id, String title, int level, String sectionNumber, boolean unnumbered, boolean appendix, List<Heading> children) {
    }

    public record RenderedDocument(String html, String title, List<Heading> headings, String imagesDir, String baseDir) {
    }
}
