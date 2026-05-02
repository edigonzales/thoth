package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.DocPage;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.DocxFeaturesSection;
import guru.interlis.thoth.biblios.config.DocxSection;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.biblios.config.SourceDocxFeaturesSection;
import guru.interlis.thoth.biblios.config.SourceDocxSection;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.render.AsciidoctorRenderer;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFootnote;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.apache.xmlbeans.impl.xb.xmlschema.SpaceAttribute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates DOCX artifacts for component versions when configured.
 */
public final class BibliosDocxGenerator {
    private static final Pattern XREF_PATTERN = Pattern.compile("<<([^,>]+)(?:,[^>]+)?>>");
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

    private final BibliosConfig config;
    private final SiteCatalog catalog;
    private final Path outputRoot;
    private final Map<String, SourceConfig> sourceConfigById;

    public BibliosDocxGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot) {
        this.config = config;
        this.catalog = catalog;
        this.outputRoot = outputRoot;
        this.sourceConfigById = indexSourceConfigs(config);
    }

    public void generate(Set<String> selectedVersions) throws IOException {
        Set<String> filters = normalizeFilters(selectedVersions);
        Set<String> unmatched = new HashSet<>(filters);
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            for (DocComponent component : catalog.components()) {
                SourceConfig source = sourceConfigById.get(component.id());
                if (source == null) {
                    System.err.println("[warn] Missing source config for component: " + component.id() + ", skipping DOCX generation.");
                    continue;
                }
                EffectiveDocxConfig effective = resolveEffectiveDocxConfig(source);
                if (!effective.enabled()) {
                    continue;
                }
                for (ComponentVersion version : component.versions()) {
                    if (!matchesFilter(component, version, filters)) {
                        continue;
                    }
                    markMatched(component, version, unmatched);
                    generateVersionDocx(component, version, source, effective, renderer);
                }
            }
        }

        for (String missing : unmatched) {
            System.err.println("[warn] No DOCX version matched filter: " + missing);
        }
    }

    private void generateVersionDocx(DocComponent component,
                                     ComponentVersion version,
                                     SourceConfig source,
                                     EffectiveDocxConfig effective,
                                     AsciidoctorRenderer renderer) throws IOException {
        Map<String, Object> sourceRenderAttributes = resolveSourceRenderAttributes(source, version);
        Path versionRoot = outputRoot.resolve(component.id()).resolve(version.version());
        Files.createDirectories(versionRoot);
        Path outputFile = versionRoot.resolve(docxFileName(component, version));

        Path explicitMaster = resolveExplicitMaster(version, source, effective.masterFile());
        Path renderSource = explicitMaster;
        boolean temporary = false;
        if (renderSource == null) {
            if (version.pages().isEmpty()) {
                System.err.println("[warn] No pages found for " + component.id() + "/" + version.version() + ", skipping DOCX generation.");
                return;
            }
            renderSource = createAggregateMaster(component, version);
            temporary = true;
        }

        try {
            validateCrossReferences(version);
            AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                renderSource,
                AsciidoctorRenderer.RenderOptions.split(false, config.site().defaultLanguage()),
                sourceRenderAttributes
            );
            DocxComposer composer = new DocxComposer(
                effective.referenceDoc() != null ? Path.of(effective.referenceDoc()) : null,
                component,
                version,
                effective.features(),
                deriveDocRoot(version)
            );
            composer.write(rendered.html(), rendered.title(), outputFile);
            System.out.println("[docx] " + component.id() + "/" + version.version() + " -> " + outputRoot.relativize(outputFile));
        } finally {
            if (temporary) {
                Files.deleteIfExists(renderSource);
            }
        }
    }

    private void validateCrossReferences(ComponentVersion version) throws IOException {
        Set<String> anchors = new HashSet<>();
        Set<String> refs = new HashSet<>();

        for (DocPage page : version.pages()) {
            Path sourceFile = resolveSourceFile(page);
            if (sourceFile == null || !Files.exists(sourceFile)) {
                continue;
            }
            String content = Files.readString(sourceFile, StandardCharsets.UTF_8);
            Matcher anchorMatcher = ANCHOR_PATTERN.matcher(content);
            while (anchorMatcher.find()) {
                String anchor = anchorMatcher.group(1).trim();
                if (!anchor.isBlank()) {
                    anchors.add(anchor);
                }
            }
            Matcher refMatcher = XREF_PATTERN.matcher(content);
            while (refMatcher.find()) {
                String target = refMatcher.group(1).trim();
                if (!target.isBlank() && isLocalXrefTarget(target)) {
                    refs.add(target);
                }
            }
        }

        List<String> missing = new ArrayList<>();
        for (String ref : refs) {
            String normalized = stripXrefPrefix(ref);
            if (!anchors.contains(normalized)) {
                missing.add(ref);
            }
        }
        if (!missing.isEmpty()) {
            throw new IOException("Unresolved AsciiDoc cross-references for DOCX export: " + String.join(", ", missing));
        }
    }

    private boolean isLocalXrefTarget(String target) {
        return !(target.contains("/") || target.contains(".adoc") || target.contains("#") || target.contains(":"));
    }

    private String stripXrefPrefix(String target) {
        String normalized = target;
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized.trim();
    }

    private Map<String, Object> resolveSourceRenderAttributes(SourceConfig source, ComponentVersion version) {
        Object revnumber = source.revnumber();
        if (revnumber == null || Boolean.FALSE.equals(revnumber)) {
            return Map.of();
        }
        if (Boolean.TRUE.equals(revnumber)) {
            return Map.of("revnumber", version.displayVersion());
        }
        if (revnumber instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isBlank()) {
                return Map.of();
            }
            return Map.of("revnumber", trimmed);
        }
        return Map.of("revnumber", revnumber);
    }

    private Path resolveExplicitMaster(ComponentVersion version, SourceConfig source, String explicitMasterFile) throws IOException {
        String masterFile = explicitMasterFile;
        if (masterFile == null || masterFile.isBlank()) {
            if (source.renderMode().isSinglePage()) {
                masterFile = source.masterFile();
            } else {
                return null;
            }
        }

        Path docRoot = deriveDocRoot(version);
        if (docRoot == null) {
            throw new IOException("Failed to determine documentation root for " + source.id() + "/" + version.version());
        }

        Path masterPath = docRoot.resolve(masterFile).normalize();
        if (!Files.exists(masterPath) || !Files.isRegularFile(masterPath)) {
            throw new IOException(
                "DOCX master file not found for " + source.id() + "/" + version.version() + ": " + masterPath
            );
        }
        return masterPath;
    }

    private Path createAggregateMaster(DocComponent component, ComponentVersion version) throws IOException {
        Path tempFile = Files.createTempFile("thoth-biblios-docx-", ".adoc");
        StringBuilder document = new StringBuilder();
        document.append("= ").append(component.displayName()).append(": ").append(version.displayVersion()).append("\n");
        document.append(":doctype: book\n\n");
        for (DocPage page : version.pages()) {
            Path sourceFile = resolveSourceFile(page);
            document.append("include::").append(toIncludeTarget(sourceFile)).append("[]\n\n");
        }
        Files.writeString(tempFile, document.toString(), StandardCharsets.UTF_8);
        return tempFile;
    }

    private Path deriveDocRoot(ComponentVersion version) {
        if (version.pages().isEmpty()) {
            return null;
        }
        DocPage firstPage = version.pages().get(0);
        Path sourceFile = resolveSourceFile(firstPage);
        if (sourceFile == null) {
            return null;
        }

        Path root = sourceFile.toAbsolutePath().normalize();
        Path relativeSource = Path.of(firstPage.sourcePath()).normalize();
        for (int i = 0; i < relativeSource.getNameCount(); i++) {
            root = root.getParent();
            if (root == null) {
                return null;
            }
        }
        return root;
    }

    private Path resolveSourceFile(DocPage page) {
        if (page.sourceUri() == null || page.sourceUri().isBlank()) {
            return null;
        }
        URI sourceUri = URI.create(page.sourceUri());
        if (!"file".equalsIgnoreCase(sourceUri.getScheme())) {
            return null;
        }
        return Path.of(sourceUri).toAbsolutePath().normalize();
    }

    private String toIncludeTarget(Path sourceFile) {
        String raw = sourceFile.toAbsolutePath().normalize().toString().replace('\\', '/');
        return raw.replace(" ", "\\ ");
    }

    private EffectiveDocxConfig resolveEffectiveDocxConfig(SourceConfig source) {
        DocxSection global = config.docx();
        SourceDocxSection local = source.docx();

        boolean enabled = global != null && global.enabled();
        if (local != null && local.enabled() != null) {
            enabled = local.enabled();
        }

        String masterFile = local != null ? local.masterFile() : null;
        String referenceDoc = global != null ? global.referenceDoc() : null;
        if (local != null && local.referenceDoc() != null) {
            referenceDoc = local.referenceDoc();
        }

        DocxFeaturesSection mergedFeatures = mergeDocxFeatures(
            global != null ? global.features() : DocxFeaturesSection.defaults(),
            local != null ? local.features() : null
        );
        return new EffectiveDocxConfig(enabled, masterFile, referenceDoc, mergedFeatures);
    }

    private DocxFeaturesSection mergeDocxFeatures(DocxFeaturesSection global, SourceDocxFeaturesSection local) {
        if (local == null) {
            return global;
        }
        boolean titlePage = local.titlePage() != null ? local.titlePage() : global.titlePage();
        boolean toc = local.toc() != null ? local.toc() : global.toc();
        boolean changeLog = local.changeLog() != null ? local.changeLog() : global.changeLog();
        return new DocxFeaturesSection(titlePage, toc, changeLog);
    }

    private String docxFileName(DocComponent component, ComponentVersion version) {
        return component.id() + "-" + version.version() + ".docx";
    }

    private Set<String> normalizeFilters(Set<String> selectedVersions) {
        if (selectedVersions == null || selectedVersions.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String candidate : selectedVersions) {
            if (candidate == null) {
                continue;
            }
            String trimmed = candidate.trim();
            if (!trimmed.isBlank()) {
                normalized.add(trimmed);
            }
        }
        return Set.copyOf(normalized);
    }

    private boolean matchesFilter(DocComponent component, ComponentVersion version, Set<String> filters) {
        if (filters.isEmpty()) {
            return true;
        }
        return filters.contains(version.version()) || filters.contains(component.id() + "/" + version.version());
    }

    private void markMatched(DocComponent component, ComponentVersion version, Set<String> unmatched) {
        unmatched.remove(version.version());
        unmatched.remove(component.id() + "/" + version.version());
    }

    private Map<String, SourceConfig> indexSourceConfigs(BibliosConfig cfg) {
        Map<String, SourceConfig> indexed = new LinkedHashMap<>();
        for (SourceConfig source : cfg.content().sources()) {
            indexed.put(source.id(), source);
        }
        return Map.copyOf(indexed);
    }

    private record EffectiveDocxConfig(boolean enabled, String masterFile, String referenceDoc, DocxFeaturesSection features) {
    }

    private static final class DocxComposer {
        private final Path referenceDoc;
        private final DocComponent component;
        private final ComponentVersion version;
        private final DocxFeaturesSection features;
        private final Path docRoot;
        private final DocxFieldSupport.BookmarkRegistry bookmarkRegistry = new DocxFieldSupport.BookmarkRegistry();
        private final Set<String> bookmarkTargets = new LinkedHashSet<>();
        private final Map<String, BigInteger> footnoteIds = new HashMap<>();
        private final Set<String> seenHtmlIds = new LinkedHashSet<>();
        private final Set<String> referencedHtmlIds = new LinkedHashSet<>();
        private BigInteger bookmarkId = BigInteger.ONE;

        private DocxComposer(Path referenceDoc, DocComponent component, ComponentVersion version, DocxFeaturesSection features, Path docRoot) {
            this.referenceDoc = referenceDoc;
            this.component = component;
            this.version = version;
            this.features = features;
            this.docRoot = docRoot;
        }

        private void write(String html, String documentTitle, Path outputFile) throws IOException {
            try (XWPFDocument document = createDocument()) {
                if (features.titlePage()) {
                    writeTitlePage(document, documentTitle);
                }
                if (features.toc()) {
                    writeTocField(document);
                }
                Map<String, String> footnotes = extractFootnotes(html);
                validateHtmlReferenceTargets(html);
                renderHtml(document, html, footnotes);
                if (features.changeLog()) {
                    writeChangeLogPlaceholder(document);
                }
                Files.createDirectories(outputFile.toAbsolutePath().normalize().getParent());
                try (OutputStream out = Files.newOutputStream(outputFile)) {
                    document.write(out);
                }
            }
        }

        private XWPFDocument createDocument() throws IOException {
            if (referenceDoc == null) {
                return new XWPFDocument();
            }
            try (InputStream in = Files.newInputStream(referenceDoc)) {
                XWPFDocument doc = new XWPFDocument(in);
                while (doc.getBodyElements().size() > 0) {
                    doc.removeBodyElement(0);
                }
                return doc;
            }
        }

        private void writeTitlePage(XWPFDocument doc, String title) {
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            p.setStyle("Title");
            XWPFRun run = p.createRun();
            run.setText(title != null && !title.isBlank() ? title : component.displayName() + " - " + version.displayVersion());
            run.setBold(true);
            run.setFontSize(28);

            XWPFParagraph meta = doc.createParagraph();
            meta.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun metaRun = meta.createRun();
            metaRun.setText(component.displayName() + " / " + version.displayVersion());
            metaRun.setFontSize(14);
            metaRun.addBreak(BreakType.PAGE);
        }

        private void writeTocField(XWPFDocument doc) {
            XWPFParagraph heading = doc.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("Inhaltsverzeichnis");

            XWPFParagraph toc = doc.createParagraph();
            CTP ctp = toc.getCTP();
            var begin = ctp.addNewR();
            begin.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
            var instr = ctp.addNewR();
            CTText instrText = instr.addNewInstrText();
            instrText.setStringValue(" TOC \\o \"1-3\" \\h \\z \\u ");
            instrText.setSpace(SpaceAttribute.Space.PRESERVE);
            var sep = ctp.addNewR();
            sep.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
            var placeholder = ctp.addNewR();
            placeholder.addNewT().setStringValue("Inhaltsverzeichnis wird beim Aktualisieren der Felder erzeugt.");
            var end = ctp.addNewR();
            end.addNewFldChar().setFldCharType(STFldCharType.END);
        }

        private Map<String, String> extractFootnotes(String html) {
            Map<String, String> notes = new HashMap<>();
            org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(html);
            for (Element note : parsed.select("div#footnotes div.footnote")) {
                String id = note.id();
                Element cloned = note.clone();
                for (Element anchor : cloned.select("a")) {
                    anchor.remove();
                }
                String text = cloned.text().replaceFirst("^\\d+\\.?\\s*", "").trim();
                if (!id.isBlank() && !text.isBlank()) {
                    notes.put(id, text);
                }
            }
            return notes;
        }

        private void validateHtmlReferenceTargets(String html) throws IOException {
            seenHtmlIds.clear();
            referencedHtmlIds.clear();
            org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(html);

            for (Element withId : parsed.select("[id]")) {
                String id = withId.id().trim();
                if (id.isBlank()) {
                    continue;
                }
                if (!seenHtmlIds.add(id)) {
                    throw new IOException("Duplicate target id in rendered document for DOCX export: " + id);
                }
            }

            for (Element anchor : parsed.select("a[href]")) {
                String href = anchor.attr("href").trim();
                if (!href.startsWith("#") || href.length() < 2) {
                    continue;
                }
                referencedHtmlIds.add(href.substring(1));
            }

            List<String> missing = new ArrayList<>();
            for (String ref : referencedHtmlIds) {
                if (!seenHtmlIds.contains(ref) && !ref.startsWith("_footnote_")) {
                    missing.add(ref);
                }
            }
            if (!missing.isEmpty()) {
                throw new IOException("Unresolved rendered HTML anchor targets for DOCX export: " + String.join(", ", missing));
            }
        }

        private void renderHtml(XWPFDocument doc, String html, Map<String, String> footnotes) throws IOException {
            org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(html);
            for (Element child : parsed.body().children()) {
                if ("div".equals(child.tagName()) && "footnotes".equals(child.id())) {
                    continue;
                }
                appendBlock(doc, child, footnotes);
            }
        }

        private void appendBlock(XWPFDocument doc, Element element, Map<String, String> footnotes) throws IOException {
            String tag = element.tagName().toLowerCase(Locale.ROOT);
            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> appendHeading(doc, element);
                case "p" -> appendParagraph(doc, element, footnotes);
                case "ul" -> appendList(doc, element, "ListBullet", footnotes);
                case "ol" -> appendList(doc, element, "ListNumber", footnotes);
                case "pre" -> appendPreformatted(doc, element);
                case "div" -> appendDiv(doc, element, footnotes);
                default -> {
                    if (element.children().isEmpty()) {
                        if (!element.text().isBlank()) {
                            XWPFParagraph p = doc.createParagraph();
                            p.createRun().setText(element.text());
                        }
                    } else {
                        for (Element child : element.children()) {
                            appendBlock(doc, child, footnotes);
                        }
                    }
                }
            }
        }

        private void appendHeading(XWPFDocument doc, Element heading) throws IOException {
            String tag = heading.tagName().toLowerCase(Locale.ROOT);
            int level = Integer.parseInt(tag.substring(1));
            XWPFParagraph p = doc.createParagraph();
            p.setStyle("Heading" + level);
            String text = heading.text();
            if (text.isBlank()) {
                text = "Section";
            }
            String rawTarget = heading.id().isBlank() ? "heading_" + bookmarkId : heading.id();
            String bookmark = registerBookmarkTarget(rawTarget);
            addBookmark(p, bookmark);
            p.createRun().setText(text);
        }

        private void appendParagraph(XWPFDocument doc, Element paragraph, Map<String, String> footnotes) throws IOException {
            XWPFParagraph p = doc.createParagraph();
            if (paragraph.hasAttr("id")) {
                String bookmark = registerBookmarkTarget(paragraph.id());
                addBookmark(p, bookmark);
            }
            appendInlineNodes(doc, p, paragraph.childNodes(), footnotes);
        }

        private void appendList(XWPFDocument doc, Element list, String style, Map<String, String> footnotes) throws IOException {
            for (Element li : list.select("> li")) {
                XWPFParagraph p = doc.createParagraph();
                p.setStyle(style);
                appendInlineNodes(doc, p, li.childNodes(), footnotes);
            }
        }

        private void appendPreformatted(XWPFDocument doc, Element pre) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.setFontFamily("Courier New");
            run.setText(pre.text());
        }

        private void appendDiv(XWPFDocument doc, Element div, Map<String, String> footnotes) throws IOException {
            if (div.hasClass("imageblock")) {
                appendImageBlock(doc, div);
                return;
            }
            for (Element child : div.children()) {
                appendBlock(doc, child, footnotes);
            }
        }

        private void appendImageBlock(XWPFDocument doc, Element imageBlock) throws IOException {
            Element image = imageBlock.selectFirst("img");
            if (image == null) {
                return;
            }

            XWPFParagraph imageParagraph = doc.createParagraph();
            imageParagraph.setAlignment(ParagraphAlignment.CENTER);
            String src = image.attr("src");
            Path imagePath = resolveImagePath(src);
            if (imagePath != null && Files.exists(imagePath) && Files.isRegularFile(imagePath)) {
                XWPFRun run = imageParagraph.createRun();
                try (InputStream in = Files.newInputStream(imagePath)) {
                    int format = imageFormatFor(imagePath);
                    run.addPicture(in, format, imagePath.getFileName().toString(), Units.toEMU(480), Units.toEMU(320));
                } catch (InvalidFormatException e) {
                    imageParagraph.createRun().setText("[Image: " + src + "]");
                }
            } else {
                imageParagraph.createRun().setText("[Image: " + src + "]");
            }

            XWPFParagraph caption = doc.createParagraph();
            caption.setStyle("Caption");
            String captionText = imageBlock.selectFirst(".title") != null
                ? imageBlock.selectFirst(".title").text()
                : image.attr("alt");
            if (captionText == null || captionText.isBlank()) {
                captionText = "Abbildung";
            }
            String figureId = imageBlock.id().isBlank() ? "figure_" + bookmarkId : imageBlock.id();
            String bookmark = registerBookmarkTarget(figureId);
            addBookmark(caption, bookmark);
            caption.createRun().setText("Figure ");
            addSimpleField(caption, DocxFieldSupport.seqInstruction("Figure"), "1");
            caption.createRun().setText(": " + captionText);
        }

        private Path resolveImagePath(String src) {
            if (src == null || src.isBlank() || docRoot == null) {
                return null;
            }
            if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("data:")) {
                return null;
            }
            Path path = Path.of(src);
            if (!path.isAbsolute()) {
                path = docRoot.resolve(path);
            }
            return path.normalize();
        }

        private int imageFormatFor(Path imagePath) {
            String lower = imagePath.getFileName().toString().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) {
                return XWPFDocument.PICTURE_TYPE_PNG;
            }
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return XWPFDocument.PICTURE_TYPE_JPEG;
            }
            if (lower.endsWith(".gif")) {
                return XWPFDocument.PICTURE_TYPE_GIF;
            }
            if (lower.endsWith(".bmp")) {
                return XWPFDocument.PICTURE_TYPE_BMP;
            }
            return XWPFDocument.PICTURE_TYPE_PNG;
        }

        private void appendInlineNodes(XWPFDocument doc, XWPFParagraph paragraph, List<Node> nodes, Map<String, String> footnotes) throws IOException {
            for (Node node : nodes) {
                appendInlineNode(doc, paragraph, node, footnotes);
            }
        }

        private void appendInlineNode(XWPFDocument doc, XWPFParagraph paragraph, Node node, Map<String, String> footnotes) throws IOException {
            if (node instanceof TextNode textNode) {
                String text = textNode.getWholeText();
                if (!text.isBlank()) {
                    paragraph.createRun().setText(text);
                }
                return;
            }
            if (!(node instanceof Element element)) {
                return;
            }
            String tag = element.tagName().toLowerCase(Locale.ROOT);
            switch (tag) {
                case "a" -> appendAnchor(paragraph, element);
                case "strong", "b" -> {
                    XWPFRun run = paragraph.createRun();
                    run.setBold(true);
                    run.setText(element.text());
                }
                case "em", "i" -> {
                    XWPFRun run = paragraph.createRun();
                    run.setItalic(true);
                    run.setText(element.text());
                }
                case "code" -> {
                    XWPFRun run = paragraph.createRun();
                    run.setFontFamily("Courier New");
                    run.setText(element.text());
                }
                case "br" -> paragraph.createRun().addBreak();
                case "sup" -> appendSup(doc, paragraph, element, footnotes);
                default -> appendInlineNodes(doc, paragraph, element.childNodes(), footnotes);
            }
        }

        private void appendAnchor(XWPFParagraph paragraph, Element anchor) throws IOException {
            String href = anchor.attr("href");
            String text = anchor.text();
            if (text == null || text.isBlank()) {
                text = href;
            }
            if (href.startsWith("#")) {
                String rawTarget = href.substring(1);
                if (!seenHtmlIds.contains(rawTarget) && !rawTarget.startsWith("_footnote_")) {
                    throw new IOException("Unresolved internal DOCX target: " + rawTarget);
                }
                String bookmark = registerBookmarkName(rawTarget);
                addRefField(paragraph, bookmark, text);
                if (shouldAppendPageRef(anchor)) {
                    paragraph.createRun().setText(" (Seite ");
                    addPageRefField(paragraph, bookmark, "?");
                    paragraph.createRun().setText(")");
                }
                return;
            }
            if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("mailto:")) {
                XWPFHyperlinkRun run = paragraph.createHyperlinkRun(href);
                run.setText(text);
                run.setColor("0563C1");
                run.setUnderline(UnderlinePatterns.SINGLE);
                return;
            }
            paragraph.createRun().setText(text);
        }

        private void appendSup(XWPFDocument doc, XWPFParagraph paragraph, Element sup, Map<String, String> footnotes) {
            Element link = sup.selectFirst("a[href]");
            if (link == null) {
                paragraph.createRun().setText(sup.text());
                return;
            }
            String href = link.attr("href");
            if (!href.startsWith("#_footnote_")) {
                paragraph.createRun().setText(sup.text());
                return;
            }
            String footnoteId = href.substring(1);
            String text = footnotes.get(footnoteId);
            if (text == null || text.isBlank()) {
                paragraph.createRun().setText(sup.text());
                return;
            }
            BigInteger id = footnoteIds.computeIfAbsent(footnoteId, key -> createFootnote(doc, text));
            paragraph.getCTP().addNewR().addNewFootnoteReference().setId(id);
        }

        private BigInteger createFootnote(XWPFDocument doc, String text) {
            XWPFFootnote footnote = doc.createFootnote();
            XWPFParagraph p = footnote.createParagraph();
            p.createRun().setText(text);
            return footnote.getId();
        }

        private void addRefField(XWPFParagraph paragraph, String bookmark, String displayText) {
            addSimpleField(paragraph, DocxFieldSupport.refInstruction(bookmark),
                displayText != null && !displayText.isBlank() ? displayText : bookmark);
        }

        private void addPageRefField(XWPFParagraph paragraph, String bookmark, String fallbackText) {
            addSimpleField(paragraph, DocxFieldSupport.pageRefInstruction(bookmark), fallbackText);
        }

        private void addSimpleField(XWPFParagraph paragraph, String instruction, String fallbackText) {
            CTSimpleField field = paragraph.getCTP().addNewFldSimple();
            field.setInstr(instruction);
            var r = field.addNewR();
            r.addNewT().setStringValue(fallbackText != null && !fallbackText.isBlank() ? fallbackText : "");
        }

        private void addBookmark(XWPFParagraph paragraph, String bookmarkName) throws IOException {
            if (bookmarkTargets.contains(bookmarkName)) {
                throw new IOException("Ambiguous bookmark target in DOCX export: " + bookmarkName);
            }
            bookmarkTargets.add(bookmarkName);
            CTP ctp = paragraph.getCTP();
            CTBookmark start = ctp.addNewBookmarkStart();
            start.setName(bookmarkName);
            start.setId(bookmarkId);
            CTMarkupRange end = ctp.addNewBookmarkEnd();
            end.setId(bookmarkId);
            bookmarkId = bookmarkId.add(BigInteger.ONE);
        }

        private String registerBookmarkTarget(String rawHtmlId) {
            return registerBookmarkName(rawHtmlId);
        }

        private String registerBookmarkName(String rawHtmlId) {
            String key = rawHtmlId != null ? rawHtmlId.trim() : "";
            if (key.isBlank()) {
                key = "bookmark_" + bookmarkId;
            }
            return bookmarkRegistry.nameFor(key);
        }

        private boolean shouldAppendPageRef(Element anchor) {
            if (anchor.hasClass("pageref") || anchor.hasClass("page-ref")) {
                return true;
            }
            Node previous = anchor.previousSibling();
            while (previous instanceof TextNode textNode) {
                String text = textNode.getWholeText();
                if (text != null && !text.isBlank()) {
                    String normalized = text.replace('\u00A0', ' ').toLowerCase(Locale.ROOT);
                    if (normalized.matches(".*(auf\\s+seite\\s*|on\\s+page\\s*)$")) {
                        return true;
                    }
                    break;
                }
                previous = previous.previousSibling();
            }
            return false;
        }

        private void writeChangeLogPlaceholder(XWPFDocument doc) {
            XWPFParagraph heading = doc.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("Änderungsverzeichnis");

            XWPFParagraph placeholder = doc.createParagraph();
            placeholder.createRun().setText("Platzhalter: Inhalt des Änderungsverzeichnisses wird in einer späteren Ausbaustufe befüllt.");
        }
    }
}
