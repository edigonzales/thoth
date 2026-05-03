package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.render.AsciidoctorRenderer;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.DescriptionList;
import org.asciidoctor.ast.DescriptionListEntry;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DocxAstNormalizer {
    private static final Pattern CHAPTER_LABEL_PATTERN = Pattern.compile("^Kapitel\\s+([0-9A-Za-z.]+)(.*)$");
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("^(?:#)?([A-Za-z0-9_:\\-.]+)$");
    private static final Pattern INLINE_LINK_MACRO = Pattern.compile("(?s)^(link|xref):([^\\[]+)\\[(.*)]$");
    private static final Pattern DOUBLE_ANGLE_XREF = Pattern.compile("(?s)^<<([^,>]+)(?:,([^>]*))?>>$");
    private static final Pattern FOOTNOTE_MACRO = Pattern.compile("(?s)^footnote:(?:\\[([^]]*)]|([^\\[]+)\\[([^]]*)])$");
    private static final Pattern INLINE_HTML_TAG_PATTERN = Pattern.compile("(?i)<\\s*/?\\s*(strong|b|em|i|a)\\b");

    private final Path docRoot;
    private final String imagesDir;
    private final DocxFieldSupport.BookmarkRegistry bookmarkRegistry = new DocxFieldSupport.BookmarkRegistry();
    private final Map<String, DocxDocumentModel.AnchorTarget> anchors = new LinkedHashMap<>();
    private final Set<String> footnoteIds = new LinkedHashSet<>();
    private int generatedFootnoteCounter = 1;

    private DocxAstNormalizer(Path docRoot, String imagesDir) {
        this.docRoot = docRoot;
        this.imagesDir = imagesDir != null ? imagesDir.trim() : "";
    }

    static DocxDocumentModel.DocumentModel normalize(AsciidoctorRenderer.LoadedDocument loaded, Path docRootOverride) throws IOException {
        Objects.requireNonNull(loaded, "loaded");
        Path docRoot = docRootOverride != null ? docRootOverride : loaded.sourcePath().getParent();
        DocxAstNormalizer normalizer = new DocxAstNormalizer(docRoot, loaded.imagesDir());
        List<DocxDocumentModel.Block> blocks = normalizer.normalizeBlocks(loaded.document().getBlocks());
        return new DocxDocumentModel.DocumentModel(
            loaded.title(),
            loaded.language(),
            loaded.doctype(),
            loaded.sourcePath(),
            List.copyOf(blocks),
            Map.copyOf(normalizer.anchors)
        );
    }

    private List<DocxDocumentModel.Block> normalizeBlocks(List<StructuralNode> nodes) throws IOException {
        List<DocxDocumentModel.Block> blocks = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            StructuralNode node = nodes.get(i);
            if (node == null) {
                continue;
            }
            String context = safeContext(node);
            if ("colist".equals(context)) {
                continue;
            }

            StructuralNode next = i + 1 < nodes.size() ? nodes.get(i + 1) : null;
            if (("listing".equals(context) || "literal".equals(context)) && next != null && "colist".equals(safeContext(next))) {
                DocxDocumentModel.Block block = normalizeListingLike(node, next);
                blocks.add(block);
                i++;
                continue;
            }

            blocks.add(normalizeBlock(node));
        }
        return blocks;
    }

    private DocxDocumentModel.Block normalizeBlock(StructuralNode node) throws IOException {
        if (node instanceof Section section) {
            String id = registerAnchor(section.getId(), DocxDocumentModel.AnchorKind.SECTION,
                normalizeNumeral(section), section.getTitle());
            return new DocxDocumentModel.SectionBlock(
                id,
                textOrEmpty(section.getTitle()),
                Math.max(1, section.getLevel()),
                section.isNumbered(),
                section.isSpecial(),
                normalizeNumeral(section),
                List.copyOf(normalizeBlocks(section.getBlocks()))
            );
        }

        if (node instanceof Table table) {
            return normalizeTable(table);
        }

        if (node instanceof DescriptionList dlist) {
            String id = registerAnchor(dlist.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, dlist.getTitle());
            List<DocxDocumentModel.DescriptionEntry> entries = new ArrayList<>();
            for (DescriptionListEntry entry : dlist.getItems()) {
                List<List<DocxDocumentModel.Inline>> terms = new ArrayList<>();
                for (ListItem term : entry.getTerms()) {
                    terms.add(List.copyOf(parseInline(term.getText())));
                }
                entries.add(new DocxDocumentModel.DescriptionEntry(
                    List.copyOf(terms),
                    List.copyOf(normalizeDescription(entry.getDescription()))
                ));
            }
            return new DocxDocumentModel.DescriptionListBlock(id, List.copyOf(entries));
        }

        if (node instanceof org.asciidoctor.ast.List list) {
            return normalizeList(node, list);
        }

        String context = safeContext(node);
        return switch (context) {
            case "paragraph" -> normalizeParagraph(node);
            case "listing", "literal" -> normalizeListingLike(node, null);
            case "image" -> normalizeImage(node);
            case "admonition" -> normalizeAdmonition(node);
            case "example" -> normalizeExample(node);
            case "open", "sidebar", "quote", "verse", "preamble" -> normalizeOpenLike(node);
            case "thematic_break", "page_break" -> new DocxDocumentModel.ThematicBreakBlock(
                registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, null)
            );
            default -> {
                if (!node.getBlocks().isEmpty()) {
                    yield normalizeOpenLike(node);
                }
                throw unsupported(node, "Unsupported DOCX AST context: " + context);
            }
        };
    }

    private DocxDocumentModel.Block normalizeList(StructuralNode node, org.asciidoctor.ast.List list) throws IOException {
        String context = safeContext(node);
        if (!"ulist".equals(context) && !"olist".equals(context) && !"colist".equals(context)) {
            throw unsupported(node, "Unsupported DOCX list context: " + context);
        }
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, node.getTitle());
        List<DocxDocumentModel.ListItemModel> items = new ArrayList<>();
        for (StructuralNode itemNode : list.getItems()) {
            if (!(itemNode instanceof ListItem item)) {
                throw unsupported(node, "Unexpected list item type in DOCX export: " + itemNode.getClass().getName());
            }
            items.add(new DocxDocumentModel.ListItemModel(
                List.copyOf(parseInline(resolveListItemInlineSource(item))),
                List.copyOf(normalizeBlocks(item.getBlocks()))
            ));
        }
        if ("olist".equals(context) || "colist".equals(context)) {
            return new DocxDocumentModel.OrderedListBlock(id, List.copyOf(items));
        }
        return new DocxDocumentModel.UnorderedListBlock(id, List.copyOf(items));
    }

    private DocxDocumentModel.Block normalizeListingLike(StructuralNode node, StructuralNode calloutList) throws IOException {
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, node.getTitle());
        String context = safeContext(node);
        List<String> lines = node instanceof Block block ? List.copyOf(block.getLines()) : List.of(textOrEmpty(node.getContent()));
        List<DocxDocumentModel.CalloutDefinition> callouts = calloutList != null ? normalizeCallouts(calloutList) : List.of();
        if ("listing".equals(context)) {
            String language = attributeAsString(node.getAttribute("language"));
            if (language.isBlank()) {
                language = attributeAsString(node.getAttribute("lang"));
            }
            return new DocxDocumentModel.ListingBlock(id, textOrEmpty(node.getTitle()), language, lines, callouts);
        }
        return new DocxDocumentModel.LiteralBlock(id, lines);
    }

    private List<DocxDocumentModel.CalloutDefinition> normalizeCallouts(StructuralNode calloutList) throws IOException {
        if (!(calloutList instanceof org.asciidoctor.ast.List list)) {
            throw unsupported(calloutList, "Callout list is not an Asciidoctor list.");
        }
        List<DocxDocumentModel.CalloutDefinition> definitions = new ArrayList<>();
        for (StructuralNode itemNode : list.getItems()) {
            if (!(itemNode instanceof ListItem item)) {
                throw unsupported(calloutList, "Unexpected callout list item type: " + itemNode.getClass().getName());
            }
            String marker = item.getMarker() != null && !item.getMarker().isBlank() ? item.getMarker().trim() : Integer.toString(definitions.size() + 1);
            definitions.add(new DocxDocumentModel.CalloutDefinition(marker, List.copyOf(parseInline(resolveListItemInlineSource(item)))));
        }
        return List.copyOf(definitions);
    }

    private DocxDocumentModel.ParagraphBlock normalizeParagraph(StructuralNode node) {
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, node.getTitle());
        String source = node instanceof Block block ? block.getSource() : textOrEmpty(node.getContent());
        if (source == null || source.isBlank()) {
            source = textOrEmpty(node.getContent());
        }
        return new DocxDocumentModel.ParagraphBlock(id, List.copyOf(parseInline(source)));
    }

    private DocxDocumentModel.ImageBlock normalizeImage(StructuralNode node) {
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.FIGURE, null, node.getTitle());
        String target = attributeAsString(node.getAttribute("target"));
        Path resolvedPath = resolveImagePath(target);
        return new DocxDocumentModel.ImageBlock(
            id,
            textOrEmpty(node.getTitle()),
            resolvedPath,
            target,
            attributeAsString(node.getAttribute("alt")),
            attributeAsString(node.getAttribute("width"))
        );
    }

    private DocxDocumentModel.TableBlock normalizeTable(Table table) throws IOException {
        String id = registerAnchor(table.getId(), DocxDocumentModel.AnchorKind.TABLE, null, table.getTitle());
        List<DocxDocumentModel.TableRowModel> headerRows = normalizeRows(table.getHeader());
        List<DocxDocumentModel.TableRowModel> bodyRows = normalizeRows(table.getBody());
        return new DocxDocumentModel.TableBlock(id, textOrEmpty(table.getTitle()), headerRows, bodyRows);
    }

    private List<DocxDocumentModel.TableRowModel> normalizeRows(List<Row> rows) throws IOException {
        List<DocxDocumentModel.TableRowModel> result = new ArrayList<>();
        for (Row row : rows) {
            List<DocxDocumentModel.TableCellModel> cells = new ArrayList<>();
            for (Cell cell : row.getCells()) {
                List<DocxDocumentModel.Block> blocks;
                if (cell.getInnerDocument() != null) {
                    blocks = normalizeBlocks(cell.getInnerDocument().getBlocks());
                } else {
                    String cellSource = cell.getSource();
                    if (cellSource == null || cellSource.isBlank()) {
                        cellSource = cell.getText();
                    }
                    blocks = List.of(new DocxDocumentModel.ParagraphBlock(null, List.copyOf(parseInline(cellSource))));
                }
                cells.add(new DocxDocumentModel.TableCellModel(
                    Math.max(1, cell.getColspan()),
                    Math.max(1, cell.getRowspan()),
                    List.copyOf(blocks),
                    cell.getHorizontalAlignment() != null ? cell.getHorizontalAlignment().name() : ""
                ));
            }
            result.add(new DocxDocumentModel.TableRowModel(List.copyOf(cells)));
        }
        return List.copyOf(result);
    }

    private DocxDocumentModel.AdmonitionBlock normalizeAdmonition(StructuralNode node) throws IOException {
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, node.getTitle());
        return new DocxDocumentModel.AdmonitionBlock(
            id,
            safeStyle(node),
            textOrEmpty(node.getTitle()),
            List.copyOf(normalizeBlocks(node.getBlocks()))
        );
    }

    private DocxDocumentModel.ExampleBlock normalizeExample(StructuralNode node) throws IOException {
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, node.getTitle());
        return new DocxDocumentModel.ExampleBlock(id, textOrEmpty(node.getTitle()), List.copyOf(normalizeBlocks(node.getBlocks())));
    }

    private DocxDocumentModel.OpenBlock normalizeOpenLike(StructuralNode node) throws IOException {
        String id = registerAnchor(node.getId(), DocxDocumentModel.AnchorKind.BLOCK, null, node.getTitle());
        return new DocxDocumentModel.OpenBlock(id, List.copyOf(normalizeBlocks(node.getBlocks())));
    }

    private List<DocxDocumentModel.Block> normalizeDescription(ListItem description) throws IOException {
        List<DocxDocumentModel.Block> blocks = normalizeBlocks(description.getBlocks());
        if (!blocks.isEmpty()) {
            return blocks;
        }
        if (description.hasText()) {
            return List.of(new DocxDocumentModel.ParagraphBlock(null, List.copyOf(parseInline(description.getText()))));
        }
        return List.of();
    }

    private String registerAnchor(String rawId, DocxDocumentModel.AnchorKind kind, String numberText, String displayText) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String normalized = rawId.trim();
        if (anchors.containsKey(normalized)) {
            throw new IllegalStateException("Duplicate DOCX bookmark target: " + normalized);
        }
        String bookmark = bookmarkRegistry.nameFor(normalized);
        anchors.put(normalized, new DocxDocumentModel.AnchorTarget(normalized, bookmark, kind, numberText, textOrEmpty(displayText)));
        return normalized;
    }

    private List<DocxDocumentModel.Inline> parseInline(String source) {
        String raw = source != null ? source : "";
        if (containsInlineHtml(raw)) {
            return parseInlineHtml(raw);
        }
        String normalized = raw.replace("\r\n", "\n");
        normalized = normalized.replaceAll(" \\+\n", "\n__THOTH_LINE_BREAK__\n");
        normalized = normalized.replace('\n', ' ');
        normalized = normalized.replace("__THOTH_LINE_BREAK__", "\n");
        return parseInlineRange(normalized);
    }

    private List<DocxDocumentModel.Inline> parseInlineRange(String text) {
        List<DocxDocumentModel.Inline> result = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                flushPlain(result, plain);
                result.add(new DocxDocumentModel.LineBreakInline());
                continue;
            }

            MatchResult macro = tryParseMacro(text, i);
            if (macro != null) {
                flushPlain(result, plain);
                result.add(macro.inline());
                i = macro.endIndex();
                continue;
            }

            MatchResult delimited = tryParseDelimited(text, i, '*', children -> new DocxDocumentModel.StrongInline(children));
            if (delimited == null) {
                delimited = tryParseDoubleDelimited(text, i, '*', children -> new DocxDocumentModel.StrongInline(children));
            }
            if (delimited == null) {
                delimited = tryParseDelimited(text, i, '_', children -> new DocxDocumentModel.EmphasisInline(children));
            }
            if (delimited == null) {
                delimited = tryParseDoubleDelimited(text, i, '_', children -> new DocxDocumentModel.EmphasisInline(children));
            }
            if (delimited == null) {
                delimited = tryParseMonospace(text, i);
            }
            if (delimited != null) {
                flushPlain(result, plain);
                result.add(delimited.inline());
                i = delimited.endIndex();
                continue;
            }

            plain.append(ch);
        }
        flushPlain(result, plain);
        return mergeAdjacentText(result);
    }

    private MatchResult tryParseMacro(String text, int start) {
        if (text.startsWith("link:", start) || text.startsWith("xref:", start) || text.startsWith("footnote:", start)) {
            int end = findClosingBracket(text, text.indexOf('[', start));
            if (end > start) {
                String candidate = text.substring(start, end + 1);
                Matcher footnote = FOOTNOTE_MACRO.matcher(candidate);
                if (footnote.matches()) {
                    String explicitId = footnote.group(1) != null ? "" : footnote.group(2);
                    String noteText = footnote.group(1) != null ? footnote.group(1) : footnote.group(3);
                    String id = explicitId != null && !explicitId.isBlank() ? explicitId.trim() : generatedFootnoteId();
                    footnoteIds.add(id);
                    return new MatchResult(new DocxDocumentModel.FootnoteInline(id, List.copyOf(parseInlineRange(noteText))), end);
                }
                Matcher macro = INLINE_LINK_MACRO.matcher(candidate);
                if (macro.matches()) {
                    String type = macro.group(1);
                    String target = macro.group(2).trim();
                    String label = macro.group(3);
                    List<DocxDocumentModel.Inline> labelInlines = List.copyOf(parseInlineRange(label));
                    if (isInternalTarget(target)) {
                        return new MatchResult(buildInternalXref(target, labelInlines), end);
                    }
                    return new MatchResult(new DocxDocumentModel.LinkInline(target, labelInlines), end);
                }
            }
        }
        if (text.startsWith("<<", start)) {
            int end = text.indexOf(">>", start);
            if (end > start) {
                String candidate = text.substring(start, end + 2);
                Matcher matcher = DOUBLE_ANGLE_XREF.matcher(candidate);
                if (matcher.matches()) {
                    String target = matcher.group(1).trim();
                    String label = matcher.group(2) != null ? matcher.group(2).trim() : target;
                    return new MatchResult(buildInternalXref(target, List.copyOf(parseInlineRange(label))), end + 1);
                }
            }
        }
        return null;
    }

    private MatchResult tryParseDelimited(String text, int start, char marker, InlineFactory factory) {
        if (text.charAt(start) != marker) {
            return null;
        }
        int end = text.indexOf(marker, start + 1);
        if (end <= start + 1) {
            return null;
        }
        String inner = text.substring(start + 1, end);
        if (inner.contains("\n")) {
            return null;
        }
        return new MatchResult(factory.create(List.copyOf(parseInlineRange(inner))), end);
    }

    private MatchResult tryParseDoubleDelimited(String text, int start, char marker, InlineFactory factory) {
        if (start + 1 >= text.length() || text.charAt(start) != marker || text.charAt(start + 1) != marker) {
            return null;
        }
        String sequence = "" + marker + marker;
        int end = text.indexOf(sequence, start + 2);
        if (end <= start + 2) {
            return null;
        }
        String inner = text.substring(start + 2, end);
        if (inner.contains("\n")) {
            return null;
        }
        return new MatchResult(factory.create(List.copyOf(parseInlineRange(inner))), end + 1);
    }

    private MatchResult tryParseMonospace(String text, int start) {
        char marker = text.charAt(start);
        if (marker != '`' && marker != '+') {
            return null;
        }
        int end = text.indexOf(marker, start + 1);
        if (end <= start + 1) {
            return null;
        }
        return new MatchResult(new DocxDocumentModel.MonospaceInline(text.substring(start + 1, end)), end);
    }

    private int findClosingBracket(String text, int open) {
        if (open < 0) {
            return -1;
        }
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private DocxDocumentModel.InternalXrefInline buildInternalXref(String target, List<DocxDocumentModel.Inline> label) {
        String normalizedTarget = normalizeInternalTarget(target);
        boolean pageRef = false;
        String chapterNumber = null;
        String plainLabel = plainText(label).trim();
        Matcher matcher = CHAPTER_LABEL_PATTERN.matcher(plainLabel);
        if (matcher.matches()) {
            chapterNumber = matcher.group(1);
        }
        return new DocxDocumentModel.InternalXrefInline(normalizedTarget, label, pageRef, chapterNumber);
    }

    private boolean isInternalTarget(String target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        String trimmed = target.trim();
        if (trimmed.contains("://") || trimmed.startsWith("mailto:")) {
            return false;
        }
        if (trimmed.startsWith("#")) {
            return INTERNAL_LINK_PATTERN.matcher(trimmed.substring(1)).matches();
        }
        return !trimmed.contains("/") && !trimmed.contains(".adoc") && !trimmed.contains(":") && INTERNAL_LINK_PATTERN.matcher(trimmed).matches();
    }

    private String normalizeInternalTarget(String target) {
        String normalized = target.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void flushPlain(List<DocxDocumentModel.Inline> result, StringBuilder plain) {
        if (plain.isEmpty()) {
            return;
        }
        result.add(new DocxDocumentModel.TextInline(plain.toString()));
        plain.setLength(0);
    }

    private List<DocxDocumentModel.Inline> mergeAdjacentText(List<DocxDocumentModel.Inline> inlines) {
        List<DocxDocumentModel.Inline> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (DocxDocumentModel.Inline inline : inlines) {
            if (inline instanceof DocxDocumentModel.TextInline textInline) {
                buffer.append(textInline.text());
                continue;
            }
            flushPlain(merged, buffer);
            if (inline instanceof DocxDocumentModel.InternalXrefInline xref) {
                boolean pageRef = false;
                if (!merged.isEmpty() && merged.get(merged.size() - 1) instanceof DocxDocumentModel.TextInline previous) {
                    String prevText = previous.text().replace('\u00A0', ' ').toLowerCase(Locale.ROOT);
                    if (prevText.matches(".*(auf\\s+seite\\s*|on\\s+page\\s*)$")) {
                        pageRef = true;
                    }
                }
                merged.add(new DocxDocumentModel.InternalXrefInline(xref.targetId(), xref.label(), pageRef, xref.chapterNumber()));
            } else {
                merged.add(inline);
            }
        }
        flushPlain(merged, buffer);
        return List.copyOf(merged);
    }

    private String plainText(List<DocxDocumentModel.Inline> inlines) {
        StringBuilder text = new StringBuilder();
        for (DocxDocumentModel.Inline inline : inlines) {
            if (inline instanceof DocxDocumentModel.TextInline t) {
                text.append(t.text());
            } else if (inline instanceof DocxDocumentModel.StrongInline s) {
                text.append(plainText(s.children()));
            } else if (inline instanceof DocxDocumentModel.EmphasisInline e) {
                text.append(plainText(e.children()));
            } else if (inline instanceof DocxDocumentModel.MonospaceInline m) {
                text.append(m.text());
            } else if (inline instanceof DocxDocumentModel.LinkInline l) {
                text.append(plainText(l.label()));
            } else if (inline instanceof DocxDocumentModel.InternalXrefInline x) {
                text.append(plainText(x.label()));
            } else if (inline instanceof DocxDocumentModel.FootnoteInline f) {
                text.append(plainText(f.content()));
            } else if (inline instanceof DocxDocumentModel.CalloutInline c) {
                text.append(c.number());
            }
        }
        return text.toString();
    }

    private String generatedFootnoteId() {
        return "_footnote_auto_" + generatedFootnoteCounter++;
    }

    private String resolveListItemInlineSource(ListItem item) {
        String source = invokeOptionalStringMethod(item, "getSource");
        if (source != null && !source.isBlank()) {
            return source;
        }
        source = item.getText();
        if (source != null && !source.isBlank()) {
            return source;
        }
        return textOrEmpty(item.getContent());
    }

    private String invokeOptionalStringMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private boolean containsInlineHtml(String text) {
        return text != null && INLINE_HTML_TAG_PATTERN.matcher(text).find();
    }

    private List<DocxDocumentModel.Inline> parseInlineHtml(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Element body = Jsoup.parseBodyFragment(html).body();
        List<DocxDocumentModel.Inline> inlines = parseHtmlNodes(body.childNodes());
        return mergeAdjacentText(inlines);
    }

    private List<DocxDocumentModel.Inline> parseHtmlNodes(List<Node> nodes) {
        List<DocxDocumentModel.Inline> result = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof TextNode textNode) {
                result.addAll(parseInlineRange(textNode.getWholeText()));
                continue;
            }
            if (!(node instanceof Element element)) {
                continue;
            }
            String tag = element.tagName().toLowerCase(Locale.ROOT);
            List<DocxDocumentModel.Inline> children = parseHtmlNodes(element.childNodes());
            switch (tag) {
                case "strong", "b" -> result.add(new DocxDocumentModel.StrongInline(List.copyOf(children)));
                case "em", "i" -> result.add(new DocxDocumentModel.EmphasisInline(List.copyOf(children)));
                case "a" -> {
                    String href = attributeAsString(element.attr("href"));
                    if (href.isBlank()) {
                        result.addAll(children);
                    } else if (isInternalTarget(href)) {
                        result.add(buildInternalXref(href, List.copyOf(children)));
                    } else {
                        result.add(new DocxDocumentModel.LinkInline(href, List.copyOf(children)));
                    }
                }
                case "br" -> result.add(new DocxDocumentModel.LineBreakInline());
                default -> result.addAll(children);
            }
        }
        return result;
    }

    private IOException unsupported(ContentNode node, String message) {
        return new IOException(message + " at " + location(node));
    }

    private String location(ContentNode node) {
        if (node instanceof StructuralNode structuralNode && structuralNode.getSourceLocation() != null) {
            var cursor = structuralNode.getSourceLocation();
            String path = cursor.getPath() != null ? cursor.getPath() : cursor.getFile();
            if (path != null && cursor.getLineNumber() > 0) {
                return path + ":" + cursor.getLineNumber();
            }
            if (path != null) {
                return path;
            }
        }
        return "unknown location";
    }

    private String normalizeNumeral(Section section) {
        String numeral = section.getSectnum();
        if (numeral == null || numeral.isBlank()) {
            numeral = section.getNumeral();
        }
        if (numeral == null) {
            return "";
        }
        return numeral.trim().replaceAll("\\.$", "");
    }

    private Path resolveImagePath(String target) {
        if (target == null || target.isBlank() || docRoot == null) {
            return null;
        }
        if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("data:")) {
            return null;
        }
        Path path = Path.of(target);
        if (!path.isAbsolute()) {
            if (!imagesDir.isBlank()) {
                path = docRoot.resolve(imagesDir).resolve(target);
            } else {
                path = docRoot.resolve(target);
            }
        }
        return path.normalize();
    }

    private String safeContext(ContentNode node) {
        return node.getContext() != null ? node.getContext().trim().toLowerCase(Locale.ROOT) : "";
    }

    private String safeStyle(ContentNode node) {
        return node instanceof StructuralNode structuralNode && structuralNode.getStyle() != null
            ? structuralNode.getStyle().trim().toLowerCase(Locale.ROOT)
            : "";
    }

    private String textOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private String attributeAsString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private interface InlineFactory {
        DocxDocumentModel.Inline create(List<DocxDocumentModel.Inline> children);
    }

    private record MatchResult(
        DocxDocumentModel.Inline inline,
        int endIndex
    ) {
    }
}
