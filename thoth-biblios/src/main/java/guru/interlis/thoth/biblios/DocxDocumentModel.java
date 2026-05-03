package guru.interlis.thoth.biblios;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class DocxDocumentModel {

    private DocxDocumentModel() {
    }

    record DocumentModel(
        String title,
        String language,
        String doctype,
        Path sourcePath,
        List<Block> blocks,
        Map<String, AnchorTarget> anchors
    ) {
    }

    record AnchorTarget(
        String rawId,
        String bookmarkName,
        AnchorKind kind,
        String numberText,
        String displayText
    ) {
    }

    enum AnchorKind {
        SECTION,
        BLOCK,
        FIGURE,
        TABLE
    }

    sealed interface Block permits SectionBlock, ParagraphBlock, OrderedListBlock, UnorderedListBlock,
        DescriptionListBlock, ListingBlock, LiteralBlock, ImageBlock, TableBlock, AdmonitionBlock,
        OpenBlock, ExampleBlock, ThematicBreakBlock {
    }

    record SectionBlock(
        String id,
        String title,
        int level,
        boolean numbered,
        boolean special,
        String numeral,
        List<Block> children
    ) implements Block {
    }

    record ParagraphBlock(
        String id,
        List<Inline> inlines
    ) implements Block {
    }

    record OrderedListBlock(
        String id,
        List<ListItemModel> items
    ) implements Block {
    }

    record UnorderedListBlock(
        String id,
        List<ListItemModel> items
    ) implements Block {
    }

    record DescriptionListBlock(
        String id,
        List<DescriptionEntry> entries
    ) implements Block {
    }

    record ListingBlock(
        String id,
        String title,
        String language,
        List<String> lines,
        List<CalloutDefinition> callouts
    ) implements Block {
    }

    record LiteralBlock(
        String id,
        List<String> lines
    ) implements Block {
    }

    record ImageBlock(
        String id,
        String caption,
        Path resolvedPath,
        String sourceTarget,
        String altText,
        String width
    ) implements Block {
    }

    record TableBlock(
        String id,
        String caption,
        List<TableRowModel> headerRows,
        List<TableRowModel> bodyRows
    ) implements Block {
    }

    record AdmonitionBlock(
        String id,
        String style,
        String title,
        List<Block> children
    ) implements Block {
    }

    record OpenBlock(
        String id,
        List<Block> children
    ) implements Block {
    }

    record ExampleBlock(
        String id,
        String title,
        List<Block> children
    ) implements Block {
    }

    record ThematicBreakBlock(
        String id
    ) implements Block {
    }

    record ListItemModel(
        List<Inline> text,
        List<Block> children
    ) {
    }

    record DescriptionEntry(
        List<List<Inline>> terms,
        List<Block> description
    ) {
    }

    record CalloutDefinition(
        String number,
        List<Inline> description
    ) {
    }

    record TableRowModel(
        List<TableCellModel> cells
    ) {
    }

    record TableCellModel(
        int colspan,
        int rowspan,
        List<Block> blocks,
        String horizontalAlignment
    ) {
    }

    sealed interface Inline permits TextInline, StrongInline, EmphasisInline, MonospaceInline, LinkInline,
        InternalXrefInline, LineBreakInline, FootnoteInline, CalloutInline {
    }

    record TextInline(
        String text
    ) implements Inline {
    }

    record StrongInline(
        List<Inline> children
    ) implements Inline {
    }

    record EmphasisInline(
        List<Inline> children
    ) implements Inline {
    }

    record MonospaceInline(
        String text
    ) implements Inline {
    }

    record LinkInline(
        String href,
        List<Inline> label
    ) implements Inline {
    }

    record InternalXrefInline(
        String targetId,
        List<Inline> label,
        boolean pageRef,
        String chapterNumber
    ) implements Inline {
    }

    record LineBreakInline() implements Inline {
    }

    record FootnoteInline(
        String id,
        List<Inline> content
    ) implements Inline {
    }

    record CalloutInline(
        String number
    ) implements Inline {
    }
}
