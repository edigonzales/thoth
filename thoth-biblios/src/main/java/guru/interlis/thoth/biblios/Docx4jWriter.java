package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.config.DocxFeaturesSection;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FootnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.Body;
import org.docx4j.wml.Br;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTBookmark;
import org.docx4j.wml.CTFtnEdn;
import org.docx4j.wml.CTFtnEdnRef;
import org.docx4j.wml.CTFootnotes;
import org.docx4j.wml.CTLanguage;
import org.docx4j.wml.CTMarkupRange;
import org.docx4j.wml.CTSettings;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.CTSimpleField;
import org.docx4j.wml.CTVerticalAlignRun;
import org.docx4j.wml.Color;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.FldChar;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.Jc;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.Lvl;
import org.docx4j.wml.NumFmt;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.Numbering;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.R;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.STBrType;
import org.docx4j.wml.STFldCharType;
import org.docx4j.wml.STFtnEdn;
import org.docx4j.wml.STHint;
import org.docx4j.wml.STShd;
import org.docx4j.wml.STVerticalAlignRun;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.docx4j.wml.U;
import org.docx4j.wml.UnderlineEnumeration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class Docx4jWriter {
    private static final ObjectFactory WML = Context.getWmlObjectFactory();
    private static final int A4_WIDTH_TWIPS = 11906;
    private static final int A4_HEIGHT_TWIPS = 16838;
    private static final int PAGE_MARGIN_TWIPS = 1134;
    private static final int CONTENT_WIDTH_TWIPS = A4_WIDTH_TWIPS - (2 * PAGE_MARGIN_TWIPS);
    private static final double CONTENT_WIDTH_POINTS = CONTENT_WIDTH_TWIPS / 20.0;
    private static final double DEFAULT_IMAGE_DPI = 96.0;

    private final Path referenceDoc;
    private final DocComponent component;
    private final ComponentVersion version;
    private final DocxFeaturesSection features;
    private final Map<String, BigInteger> footnoteIds = new LinkedHashMap<>();
    private BigInteger bookmarkId = BigInteger.ONE;
    private BigInteger nextFootnoteId = BigInteger.ONE;
    private BigInteger headingNumId;
    private BigInteger bulletNumId;
    private BigInteger decimalNumId;
    private BigInteger sequenceBookmarkFallbackCounter = BigInteger.ONE;
    private String documentLanguage = "en";
    private WordprocessingMLPackage wordPackage;
    private MainDocumentPart mainDocumentPart;
    private FootnotesPart footnotesPart;

    Docx4jWriter(Path referenceDoc, DocComponent component, ComponentVersion version, DocxFeaturesSection features) {
        this.referenceDoc = referenceDoc;
        this.component = component;
        this.version = version;
        this.features = features;
    }

    void write(DocxDocumentModel.DocumentModel model, Path outputFile) throws IOException {
        try {
            documentLanguage = model.language() != null && !model.language().isBlank() ? model.language() : "en";
            wordPackage = referenceDoc != null
                ? WordprocessingMLPackage.load(referenceDoc.toFile())
                : WordprocessingMLPackage.createPackage();
            mainDocumentPart = wordPackage.getMainDocumentPart();
            mainDocumentPart.getContent().clear();
            configureDocumentDefaults(documentLanguage);
            if (features.titlePage()) {
                writeTitlePage(model.title(), documentLanguage);
            }
            if (features.toc()) {
                writeTocField(documentLanguage);
            }
            renderBlocks(mainDocumentPart, model.blocks(), model.anchors(), 0);
            if (features.changeLog()) {
                writeChangeLogPlaceholder(documentLanguage);
            }
            Files.createDirectories(outputFile.toAbsolutePath().normalize().getParent());
            wordPackage.save(outputFile.toFile());
        } catch (Docx4JException e) {
            throw new IOException("Failed to write DOCX for " + component.id() + "/" + version.version(), e);
        } catch (Exception e) {
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to render DOCX for " + component.id() + "/" + version.version(), e);
        }
    }

    private void configureDocumentDefaults(String language) throws Exception {
        ensureSectionProperties();
        ensureStyles(language);
        ensureNumbering();
        ensureUpdateFields();
        ensureFootnotesPart();
    }

    private void ensureSectionProperties() {
        Body body = mainDocumentPart.getJaxbElement().getBody();
        if (body == null) {
            body = WML.createBody();
            mainDocumentPart.getJaxbElement().setBody(body);
        }
        SectPr sectPr = body.getSectPr();
        if (sectPr == null) {
            sectPr = WML.createSectPr();
            body.setSectPr(sectPr);
        }
        SectPr.PgSz pgSz = sectPr.getPgSz();
        if (pgSz == null) {
            pgSz = new SectPr.PgSz();
            sectPr.setPgSz(pgSz);
        }
        pgSz.setW(BigInteger.valueOf(A4_WIDTH_TWIPS));
        pgSz.setH(BigInteger.valueOf(A4_HEIGHT_TWIPS));
        SectPr.PgMar pgMar = sectPr.getPgMar();
        if (pgMar == null) {
            pgMar = new SectPr.PgMar();
            sectPr.setPgMar(pgMar);
        }
        pgMar.setTop(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pgMar.setBottom(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pgMar.setLeft(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pgMar.setRight(BigInteger.valueOf(PAGE_MARGIN_TWIPS));
        pgMar.setHeader(BigInteger.valueOf(720));
        pgMar.setFooter(BigInteger.valueOf(720));
        pgMar.setGutter(BigInteger.ZERO);
    }

    private void ensureStyles(String language) throws Exception {
        StyleDefinitionsPart styleDefinitionsPart = mainDocumentPart.getStyleDefinitionsPart();
        if (styleDefinitionsPart == null) {
            styleDefinitionsPart = new StyleDefinitionsPart();
            styleDefinitionsPart.setJaxbElement(WML.createStyles());
            mainDocumentPart.addTargetPart(styleDefinitionsPart);
        }
        if (styleDefinitionsPart.getJaxbElement() == null) {
            styleDefinitionsPart.setJaxbElement(WML.createStyles());
        }

        ensureParagraphStyle(styleDefinitionsPart, "Title", "Title", "Normal", null, 28, true, false, false);
        for (int level = 1; level <= 6; level++) {
            int size = Math.max(11, 18 - (level * 2));
            ensureParagraphStyle(styleDefinitionsPart, "Heading" + level, "Heading " + level,
                "Normal", level - 1, size, true, false, false);
        }
        ensureParagraphStyle(styleDefinitionsPart, "Caption", "Caption", "Normal", null, 9, false, true, false);
        ensureParagraphStyle(styleDefinitionsPart, "ListBullet", "List Bullet", "Normal", null, 11, false, false, false);
        ensureParagraphStyle(styleDefinitionsPart, "ListNumber", "List Number", "Normal", null, 11, false, false, false);
        ensureParagraphStyle(styleDefinitionsPart, "DefinitionTerm", "Definition Term", "Normal", null, 11, true, false, false);
        ensureParagraphStyle(styleDefinitionsPart, "CodeBlock", "Code Block", "Normal", null, 10, false, false, true);
        ensureParagraphStyle(styleDefinitionsPart, "AdmonitionTitle", "Admonition Title", "Normal", null, 10, true, false, false);
        ensureParagraphStyle(styleDefinitionsPart, "TableHeader", "Table Header", "Normal", null, 10, true, false, false);
        ensureParagraphStyle(styleDefinitionsPart, "TableCaption", "Table Caption", "Caption", null, 9, false, true, false);

        ensureCharacterDefaults(styleDefinitionsPart, language);
    }

    private void ensureCharacterDefaults(StyleDefinitionsPart styleDefinitionsPart, String language) {
        if (styleDefinitionsPart.getJaxbElement().getDocDefaults() == null) {
            styleDefinitionsPart.getJaxbElement().setDocDefaults(WML.createDocDefaults());
        }
        if (styleDefinitionsPart.getJaxbElement().getDocDefaults().getRPrDefault() == null) {
            styleDefinitionsPart.getJaxbElement().getDocDefaults().setRPrDefault(WML.createDocDefaultsRPrDefault());
        }
        RPr rPr = styleDefinitionsPart.getJaxbElement().getDocDefaults().getRPrDefault().getRPr();
        if (rPr == null) {
            rPr = WML.createRPr();
            styleDefinitionsPart.getJaxbElement().getDocDefaults().getRPrDefault().setRPr(rPr);
        }
        if (rPr.getLang() == null) {
            CTLanguage lang = WML.createCTLanguage();
            lang.setVal(language != null && !language.isBlank() ? language : "en");
            rPr.setLang(lang);
        }
    }

    private void ensureParagraphStyle(StyleDefinitionsPart part,
                                      String styleId,
                                      String name,
                                      String basedOn,
                                      Integer outlineLevel,
                                      int fontSize,
                                      boolean bold,
                                      boolean italic,
                                      boolean codeFont) {
        if (part.getStyleById(styleId) != null) {
            return;
        }
        Style style = WML.createStyle();
        style.setStyleId(styleId);
        style.setType("paragraph");
        Style.Name styleName = WML.createStyleName();
        styleName.setVal(name);
        style.setName(styleName);
        if (basedOn != null && !basedOn.isBlank()) {
            Style.BasedOn styleBasedOn = WML.createStyleBasedOn();
            styleBasedOn.setVal(basedOn);
            style.setBasedOn(styleBasedOn);
        }
        style.setQFormat(new BooleanDefaultTrue());

        PPr pPr = WML.createPPr();
        PPrBase.Spacing spacing = WML.createPPrBaseSpacing();
        spacing.setAfter(BigInteger.valueOf(styleId.startsWith("Heading") ? 120 : 100));
        if (styleId.startsWith("Heading")) {
            spacing.setBefore(BigInteger.valueOf(180));
        }
        pPr.setSpacing(spacing);
        if (outlineLevel != null) {
            PPrBase.OutlineLvl lvl = WML.createPPrBaseOutlineLvl();
            lvl.setVal(BigInteger.valueOf(outlineLevel));
            pPr.setOutlineLvl(lvl);
        }
        style.setPPr(pPr);

        RPr rPr = WML.createRPr();
        if (codeFont) {
            RFonts fonts = WML.createRFonts();
            fonts.setAscii("Courier New");
            fonts.setHAnsi("Courier New");
            fonts.setHint(STHint.DEFAULT);
            rPr.setRFonts(fonts);
        }
        HpsMeasure sz = WML.createHpsMeasure();
        sz.setVal(BigInteger.valueOf(fontSize * 2L));
        rPr.setSz(sz);
        HpsMeasure szCs = WML.createHpsMeasure();
        szCs.setVal(BigInteger.valueOf(fontSize * 2L));
        rPr.setSzCs(szCs);
        if (bold) {
            rPr.setB(new BooleanDefaultTrue());
        }
        if (italic) {
            rPr.setI(new BooleanDefaultTrue());
        }
        style.setRPr(rPr);
        part.getJaxbElement().getStyle().add(style);
    }

    private void ensureNumbering() throws Exception {
        NumberingDefinitionsPart numberingDefinitionsPart = mainDocumentPart.getNumberingDefinitionsPart();
        if (numberingDefinitionsPart == null) {
            numberingDefinitionsPart = new NumberingDefinitionsPart();
            numberingDefinitionsPart.setJaxbElement(WML.createNumbering());
            mainDocumentPart.addTargetPart(numberingDefinitionsPart);
        }
        if (numberingDefinitionsPart.getJaxbElement() == null) {
            numberingDefinitionsPart.setJaxbElement(WML.createNumbering());
        }

        Numbering numbering = numberingDefinitionsPart.getJaxbElement();
        if (numbering.getAbstractNum().isEmpty() && numbering.getNum().isEmpty()) {
            headingNumId = addHeadingNumbering(numbering);
            bulletNumId = addListNumbering(numbering, BigInteger.valueOf(20), NumberFormat.BULLET, true);
            decimalNumId = addListNumbering(numbering, BigInteger.valueOf(21), NumberFormat.DECIMAL, false);
        } else {
            headingNumId = BigInteger.ONE;
            bulletNumId = BigInteger.valueOf(2);
            decimalNumId = BigInteger.valueOf(3);
        }
    }

    private BigInteger addHeadingNumbering(Numbering numbering) {
        Numbering.AbstractNum abstractNum = WML.createNumberingAbstractNum();
        abstractNum.setAbstractNumId(BigInteger.TEN);
        for (int level = 0; level < 6; level++) {
            Lvl lvl = WML.createLvl();
            lvl.setIlvl(BigInteger.valueOf(level));
            Lvl.Start start = new Lvl.Start();
            start.setVal(BigInteger.ONE);
            lvl.setStart(start);
            NumFmt numFmt = WML.createNumFmt();
            numFmt.setVal(NumberFormat.DECIMAL);
            lvl.setNumFmt(numFmt);
            Lvl.LvlText lvlText = new Lvl.LvlText();
            lvlText.setVal(headingLevelText(level));
            lvl.setLvlText(lvlText);
            Lvl.PStyle pStyle = new Lvl.PStyle();
            pStyle.setVal("Heading" + (level + 1));
            lvl.setPStyle(pStyle);
            PPr pPr = WML.createPPr();
            PPrBase.Ind ind = WML.createPPrBaseInd();
            ind.setLeft(BigInteger.valueOf(360L * level));
            ind.setHanging(BigInteger.ZERO);
            pPr.setInd(ind);
            lvl.setPPr(pPr);
            abstractNum.getLvl().add(lvl);
        }
        numbering.getAbstractNum().add(abstractNum);

        Numbering.Num num = WML.createNumberingNum();
        Numbering.Num.AbstractNumId abstractNumId = new Numbering.Num.AbstractNumId();
        abstractNumId.setVal(BigInteger.TEN);
        num.setAbstractNumId(abstractNumId);
        num.setNumId(BigInteger.ONE);
        numbering.getNum().add(num);
        return BigInteger.ONE;
    }

    private BigInteger addListNumbering(Numbering numbering,
                                        BigInteger abstractId,
                                        NumberFormat format,
                                        boolean bullet) {
        Numbering.AbstractNum abstractNum = WML.createNumberingAbstractNum();
        abstractNum.setAbstractNumId(abstractId);
        for (int level = 0; level < 8; level++) {
            Lvl lvl = WML.createLvl();
            lvl.setIlvl(BigInteger.valueOf(level));
            Lvl.Start start = new Lvl.Start();
            start.setVal(BigInteger.ONE);
            lvl.setStart(start);
            NumFmt numFmt = WML.createNumFmt();
            numFmt.setVal(format);
            lvl.setNumFmt(numFmt);
            Lvl.LvlText lvlText = new Lvl.LvlText();
            lvlText.setVal(bullet ? bulletTextForLevel(level) : "%" + (level + 1) + ".");
            lvl.setLvlText(lvlText);
            PPr pPr = WML.createPPr();
            PPrBase.Ind ind = WML.createPPrBaseInd();
            ind.setLeft(BigInteger.valueOf(720L * (level + 1)));
            ind.setHanging(BigInteger.valueOf(360));
            pPr.setInd(ind);
            lvl.setPPr(pPr);
            abstractNum.getLvl().add(lvl);
        }
        numbering.getAbstractNum().add(abstractNum);

        BigInteger numIdValue = BigInteger.valueOf(numbering.getNum().size() + 1L);
        Numbering.Num num = WML.createNumberingNum();
        Numbering.Num.AbstractNumId abstractNumId = new Numbering.Num.AbstractNumId();
        abstractNumId.setVal(abstractId);
        num.setAbstractNumId(abstractNumId);
        num.setNumId(numIdValue);
        numbering.getNum().add(num);
        return numIdValue;
    }

    private String bulletTextForLevel(int level) {
        return switch (level % 3) {
            case 1 -> "o";
            case 2 -> "\u25A0";
            default -> "\u2022";
        };
    }

    private void ensureUpdateFields() throws Exception {
        DocumentSettingsPart settingsPart = mainDocumentPart.getDocumentSettingsPart(true);
        CTSettings settings = settingsPart.getJaxbElement();
        if (settings == null) {
            settings = WML.createCTSettings();
            settingsPart.setJaxbElement(settings);
        }
        if (settings.getUpdateFields() == null) {
            settings.setUpdateFields(new BooleanDefaultTrue());
        }
    }

    private void ensureFootnotesPart() throws Exception {
        footnotesPart = mainDocumentPart.getFootnotesPart();
        if (footnotesPart == null) {
            footnotesPart = new FootnotesPart();
            CTFootnotes footnotes = WML.createCTFootnotes();
            footnotesPart.setJaxbElement(footnotes);
            mainDocumentPart.addTargetPart(footnotesPart);
            footnotes.getFootnote().add(separatorFootnote(BigInteger.valueOf(-1), STFtnEdn.SEPARATOR));
            footnotes.getFootnote().add(separatorFootnote(BigInteger.ZERO, STFtnEdn.CONTINUATION_SEPARATOR));
        } else if (footnotesPart.getJaxbElement() == null) {
            CTFootnotes footnotes = WML.createCTFootnotes();
            footnotesPart.setJaxbElement(footnotes);
            footnotes.getFootnote().add(separatorFootnote(BigInteger.valueOf(-1), STFtnEdn.SEPARATOR));
            footnotes.getFootnote().add(separatorFootnote(BigInteger.ZERO, STFtnEdn.CONTINUATION_SEPARATOR));
        }
    }

    private CTFtnEdn separatorFootnote(BigInteger id, STFtnEdn type) {
        CTFtnEdn note = WML.createCTFtnEdn();
        note.setId(id);
        note.setType(type);
        P p = WML.createP();
        R r = WML.createR();
        r.getContent().add(WML.createRSeparator());
        p.getContent().add(r);
        note.getContent().add(p);
        return note;
    }

    private void writeTitlePage(String title, String language) {
        P titleParagraph = createStyledParagraph("Title");
        setParagraphAlignment(titleParagraph, JcEnumeration.CENTER);
        addTextRun(titleParagraph, title != null && !title.isBlank()
            ? title
            : component.displayName() + " - " + version.displayVersion(), boldRun(true, 28, false));
        mainDocumentPart.addObject(titleParagraph);

        P meta = WML.createP();
        setParagraphAlignment(meta, JcEnumeration.CENTER);
        addTextRun(meta, component.displayName() + " / " + version.displayVersion(), runProps(14, false, false, null, null, null));
        addPageBreak(meta);
        mainDocumentPart.addObject(meta);
    }

    private void writeTocField(String language) {
        P heading = createStyledParagraph("Heading1");
        addTextRun(heading, localizedTocTitle(language), null);
        mainDocumentPart.addObject(heading);

        P toc = WML.createP();
        addFieldCharRun(toc, STFldCharType.BEGIN);
        addInstrTextRun(toc, " TOC \\o \"1-3\" \\h \\z \\u ");
        addFieldCharRun(toc, STFldCharType.SEPARATE);
        addTextRun(toc, localizedTocPlaceholder(language), null);
        addFieldCharRun(toc, STFldCharType.END);
        mainDocumentPart.addObject(toc);
    }

    private void writeChangeLogPlaceholder(String language) {
        P heading = createStyledParagraph("Heading1");
        addTextRun(heading, localizedChangeLogTitle(language), null);
        mainDocumentPart.addObject(heading);
        P placeholder = WML.createP();
        addTextRun(placeholder, localizedChangeLogPlaceholder(language), null);
        mainDocumentPart.addObject(placeholder);
    }

    private void renderBlocks(ContentAccessor container,
                              List<DocxDocumentModel.Block> blocks,
                              Map<String, DocxDocumentModel.AnchorTarget> anchors,
                              int listLevel) throws Exception {
        for (DocxDocumentModel.Block block : blocks) {
            renderBlock(container, block, anchors, listLevel);
        }
    }

    private void renderBlock(ContentAccessor container,
                             DocxDocumentModel.Block block,
                             Map<String, DocxDocumentModel.AnchorTarget> anchors,
                             int listLevel) throws Exception {
        if (block instanceof DocxDocumentModel.SectionBlock section) {
            P p = createStyledParagraph("Heading" + Math.max(1, Math.min(6, section.level())));
            if (section.numbered() && !section.special()) {
                applyNumbering(p, headingNumId, Math.max(0, Math.min(5, section.level() - 1)));
            }
            openBookmark(p, section.id(), anchors);
            addTextRun(p, section.title(), null);
            closePendingBookmark(p);
            addTo(container, p);
            renderBlocks(container, section.children(), anchors, listLevel);
            return;
        }
        if (block instanceof DocxDocumentModel.ParagraphBlock paragraph) {
            P p = WML.createP();
            openBookmark(p, paragraph.id(), anchors);
            appendInlines(p, paragraph.inlines(), anchors);
            closePendingBookmark(p);
            addTo(container, p);
            return;
        }
        if (block instanceof DocxDocumentModel.OrderedListBlock ordered) {
            renderList(container, ordered.items(), anchors, true, listLevel, ordered.id());
            return;
        }
        if (block instanceof DocxDocumentModel.UnorderedListBlock unordered) {
            renderList(container, unordered.items(), anchors, false, listLevel, unordered.id());
            return;
        }
        if (block instanceof DocxDocumentModel.DescriptionListBlock descriptionList) {
            renderDescriptionList(container, descriptionList, anchors);
            return;
        }
        if (block instanceof DocxDocumentModel.ListingBlock listing) {
            renderListing(container, listing, anchors);
            return;
        }
        if (block instanceof DocxDocumentModel.LiteralBlock literal) {
            renderLiteral(container, literal, anchors);
            return;
        }
        if (block instanceof DocxDocumentModel.ImageBlock image) {
            renderImage(container, image, anchors);
            return;
        }
        if (block instanceof DocxDocumentModel.TableBlock table) {
            renderTable(container, table, anchors);
            return;
        }
        if (block instanceof DocxDocumentModel.AdmonitionBlock admonition) {
            renderAdmonition(container, admonition, anchors);
            return;
        }
        if (block instanceof DocxDocumentModel.OpenBlock open) {
            renderBlocks(container, open.children(), anchors, listLevel);
            return;
        }
        if (block instanceof DocxDocumentModel.ExampleBlock example) {
            renderExample(container, example, anchors, listLevel);
            return;
        }
        if (block instanceof DocxDocumentModel.ThematicBreakBlock) {
            P p = WML.createP();
            addTextRun(p, "", null);
            addTo(container, p);
            return;
        }
        throw new IllegalArgumentException("Unsupported DOCX block: " + block.getClass().getName());
    }

    private void renderList(ContentAccessor container,
                            List<DocxDocumentModel.ListItemModel> items,
                            Map<String, DocxDocumentModel.AnchorTarget> anchors,
                            boolean ordered,
                            int level,
                            String listId) throws Exception {
        boolean first = true;
        for (DocxDocumentModel.ListItemModel item : items) {
            P p = createStyledParagraph(ordered ? "ListNumber" : "ListBullet");
            if (first && listId != null) {
                openBookmark(p, listId, anchors);
            }
            applyNumbering(p, ordered ? decimalNumId : bulletNumId, Math.max(0, Math.min(7, level)));
            appendInlines(p, item.text(), anchors);
            closePendingBookmark(p);
            addTo(container, p);
            first = false;
            renderBlocks(container, item.children(), anchors, level + 1);
        }
    }

    private void renderDescriptionList(ContentAccessor container,
                                       DocxDocumentModel.DescriptionListBlock dlist,
                                       Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        boolean first = true;
        for (DocxDocumentModel.DescriptionEntry entry : dlist.entries()) {
            for (List<DocxDocumentModel.Inline> term : entry.terms()) {
                P termParagraph = createStyledParagraph("DefinitionTerm");
                if (first && dlist.id() != null) {
                    openBookmark(termParagraph, dlist.id(), anchors);
                    first = false;
                }
                appendInlines(termParagraph, term, anchors);
                closePendingBookmark(termParagraph);
                addTo(container, termParagraph);
            }
            renderBlocks(container, entry.description(), anchors, 1);
        }
    }

    private void renderListing(ContentAccessor container,
                               DocxDocumentModel.ListingBlock listing,
                               Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        if (listing.title() != null && !listing.title().isBlank()) {
            P caption = createStyledParagraph("Caption");
            addTextRun(caption, listing.title(), null);
            addTo(container, caption);
        }
        P p = createStyledParagraph("CodeBlock");
        openBookmark(p, listing.id(), anchors);
        for (int i = 0; i < listing.lines().size(); i++) {
            appendInlines(p, parseListingLine(listing.lines().get(i)), anchors);
            if (i + 1 < listing.lines().size()) {
                addLineBreak(p);
            }
        }
        closePendingBookmark(p);
        addTo(container, p);
        if (!listing.callouts().isEmpty()) {
            renderCallouts(container, listing.callouts(), anchors);
        }
    }

    private List<DocxDocumentModel.Inline> parseListingLine(String line) {
        List<DocxDocumentModel.Inline> inlines = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '<') {
                int end = line.indexOf('>', i);
                if (end > i + 1 && line.substring(i + 1, end).chars().allMatch(Character::isDigit)) {
                    if (!plain.isEmpty()) {
                        inlines.add(new DocxDocumentModel.TextInline(plain.toString()));
                        plain.setLength(0);
                    }
                    inlines.add(new DocxDocumentModel.CalloutInline(line.substring(i + 1, end)));
                    i = end;
                    continue;
                }
            }
            plain.append(ch);
        }
        if (!plain.isEmpty()) {
            inlines.add(new DocxDocumentModel.TextInline(plain.toString()));
        }
        return List.copyOf(inlines);
    }

    private void renderCallouts(ContentAccessor container,
                                List<DocxDocumentModel.CalloutDefinition> callouts,
                                Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        for (DocxDocumentModel.CalloutDefinition callout : callouts) {
            P p = createStyledParagraph("ListNumber");
            applyNumbering(p, decimalNumId, 0);
            appendInlines(p, List.of(new DocxDocumentModel.TextInline(callout.number() + ". ")), anchors);
            appendInlines(p, callout.description(), anchors);
            addTo(container, p);
        }
    }

    private void renderLiteral(ContentAccessor container,
                               DocxDocumentModel.LiteralBlock literal,
                               Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        P p = createStyledParagraph("CodeBlock");
        openBookmark(p, literal.id(), anchors);
        for (int i = 0; i < literal.lines().size(); i++) {
            addTextRun(p, literal.lines().get(i), runProps(10, false, false, "Courier New", null, null));
            if (i + 1 < literal.lines().size()) {
                addLineBreak(p);
            }
        }
        closePendingBookmark(p);
        addTo(container, p);
    }

    private void renderImage(ContentAccessor container,
                             DocxDocumentModel.ImageBlock image,
                             Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        P imageParagraph = WML.createP();
        setParagraphAlignment(imageParagraph, JcEnumeration.CENTER);
        if (image.resolvedPath() != null && Files.exists(image.resolvedPath()) && Files.isRegularFile(image.resolvedPath())) {
            byte[] bytes = Files.readAllBytes(image.resolvedPath());
            BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordPackage, bytes);
            long[] extent = imageExtent(image.resolvedPath(), image.width());
            Inline inline = imagePart.createImageInline(
                image.resolvedPath().getFileName().toString(),
                image.altText() != null && !image.altText().isBlank() ? image.altText() : image.sourceTarget(),
                sequenceBookmarkFallbackCounter.longValue(),
                sequenceBookmarkFallbackCounter.intValue(),
                extent[0],
                extent[1],
                false
            );
            sequenceBookmarkFallbackCounter = sequenceBookmarkFallbackCounter.add(BigInteger.ONE);
            Drawing drawing = WML.createDrawing();
            drawing.getAnchorOrInline().add(inline);
            R run = WML.createR();
            run.getContent().add(WML.createRDrawing(drawing));
            imageParagraph.getContent().add(run);
        } else {
            addTextRun(imageParagraph, "[Image: " + image.sourceTarget() + "]", null);
        }
        addTo(container, imageParagraph);

        P caption = createStyledParagraph("Caption");
        openBookmark(caption, image.id(), anchors);
        addTextRun(caption, localizedFigureLabel(documentLanguage) + " ", null);
        addSimpleField(caption, DocxFieldSupport.seqInstruction("Figure"), "1");
        addTextRun(caption, ": " + (!image.caption().isBlank() ? image.caption() : defaultFigureCaption(documentLanguage)), null);
        closePendingBookmark(caption);
        addTo(container, caption);
    }

    private void renderTable(ContentAccessor container,
                             DocxDocumentModel.TableBlock table,
                             Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        if (table.caption() != null && !table.caption().isBlank()) {
            P caption = createStyledParagraph("TableCaption");
            openBookmark(caption, table.id(), anchors);
            addTextRun(caption, localizedTableLabel(documentLanguage) + ": " + table.caption(), null);
            closePendingBookmark(caption);
            addTo(container, caption);
        }

        Tbl tbl = WML.createTbl();
        TblPr tblPr = new TblPr();
        TblWidth tblWidth = new TblWidth();
        tblWidth.setType(TblWidth.TYPE_DXA);
        tblWidth.setW(BigInteger.valueOf(CONTENT_WIDTH_TWIPS));
        tblPr.setTblW(tblWidth);
        tbl.setTblPr(tblPr);
        TblGrid grid = new TblGrid();
        int maxColumns = Math.max(maxColumns(table.headerRows()), maxColumns(table.bodyRows()));
        if (maxColumns <= 0) {
            maxColumns = 1;
        }
        for (int i = 0; i < maxColumns; i++) {
            TblGridCol col = new TblGridCol();
            col.setW(BigInteger.valueOf(CONTENT_WIDTH_TWIPS / maxColumns));
            grid.getGridCol().add(col);
        }
        tbl.setTblGrid(grid);
        appendRows(tbl, table.headerRows(), anchors, true, maxColumns);
        appendRows(tbl, table.bodyRows(), anchors, false, maxColumns);
        addTo(container, tbl);
    }

    private void appendRows(Tbl tbl,
                            List<DocxDocumentModel.TableRowModel> rows,
                            Map<String, DocxDocumentModel.AnchorTarget> anchors,
                            boolean header,
                            int maxColumns) throws Exception {
        for (DocxDocumentModel.TableRowModel row : rows) {
            Tr tr = WML.createTr();
            for (DocxDocumentModel.TableCellModel cell : row.cells()) {
                Tc tc = WML.createTc();
                TcPr tcPr = WML.createTcPr();
                TblWidth tcWidth = new TblWidth();
                tcWidth.setType(TblWidth.TYPE_DXA);
                tcWidth.setW(BigInteger.valueOf(Math.max(1, (CONTENT_WIDTH_TWIPS / maxColumns) * cell.colspan())));
                tcPr.setTcW(tcWidth);
                if (cell.colspan() > 1) {
                    TcPrInner.GridSpan gridSpan = new TcPrInner.GridSpan();
                    gridSpan.setVal(BigInteger.valueOf(cell.colspan()));
                    tcPr.setGridSpan(gridSpan);
                }
                if (cell.rowspan() > 1) {
                    TcPrInner.VMerge merge = new TcPrInner.VMerge();
                    merge.setVal("restart");
                    tcPr.setVMerge(merge);
                }
                tc.setTcPr(tcPr);
                if (cell.blocks().isEmpty()) {
                    tc.getContent().add(WML.createP());
                } else {
                    renderBlocks(tc, cell.blocks(), anchors, 0);
                }
                if (header && !tc.getContent().isEmpty() && tc.getContent().get(0) instanceof P firstParagraph) {
                    applyParagraphStyle(firstParagraph, "TableHeader");
                }
                applyCellAlignment(tc, cell.horizontalAlignment());
                tr.getContent().add(tc);
            }
            tbl.getContent().add(tr);
        }
    }

    private void applyCellAlignment(Tc tc, String alignment) {
        if (alignment == null || alignment.isBlank() || tc.getContent().isEmpty() || !(tc.getContent().get(0) instanceof P p)) {
            return;
        }
        JcEnumeration value = switch (alignment.trim().toUpperCase(Locale.ROOT)) {
            case "CENTER" -> JcEnumeration.CENTER;
            case "RIGHT" -> JcEnumeration.RIGHT;
            default -> JcEnumeration.LEFT;
        };
        setParagraphAlignment(p, value);
    }

    private void renderAdmonition(ContentAccessor container,
                                  DocxDocumentModel.AdmonitionBlock admonition,
                                  Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        Tbl tbl = WML.createTbl();
        TblPr tblPr = new TblPr();
        TblWidth width = new TblWidth();
        width.setType(TblWidth.TYPE_DXA);
        width.setW(BigInteger.valueOf(CONTENT_WIDTH_TWIPS));
        tblPr.setTblW(width);
        applyTableBorder(tblPr, "9A3412");
        tbl.setTblPr(tblPr);
        TblGrid grid = new TblGrid();
        TblGridCol col = new TblGridCol();
        col.setW(BigInteger.valueOf(CONTENT_WIDTH_TWIPS));
        grid.getGridCol().add(col);
        tbl.setTblGrid(grid);

        Tr tr = WML.createTr();
        Tc tc = WML.createTc();
        TcPr tcPr = WML.createTcPr();
        tc.setTcPr(tcPr);
        applyShading(tcPr, admonitionFill(admonition.style()));
        tr.getContent().add(tc);
        tbl.getContent().add(tr);

        P title = createStyledParagraph("AdmonitionTitle");
        openBookmark(title, admonition.id(), anchors);
        addTextRun(title, localizedAdmonitionLabel(admonition.style(), documentLanguage), null);
        if (admonition.title() != null && !admonition.title().isBlank()) {
            addTextRun(title, ": " + admonition.title(), null);
        }
        closePendingBookmark(title);
        tc.getContent().add(title);
        renderBlocks(tc, admonition.children(), anchors, 0);
        addTo(container, tbl);
    }

    private void renderExample(ContentAccessor container,
                               DocxDocumentModel.ExampleBlock example,
                               Map<String, DocxDocumentModel.AnchorTarget> anchors,
                               int listLevel) throws Exception {
        if (example.title() != null && !example.title().isBlank()) {
            P title = createStyledParagraph("Caption");
            addTextRun(title, example.title(), null);
            addTo(container, title);
        }
        renderBlocks(container, example.children(), anchors, listLevel);
    }

    private void appendInlines(P paragraph,
                               List<DocxDocumentModel.Inline> inlines,
                               Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        for (DocxDocumentModel.Inline inline : inlines) {
            appendInline(paragraph, inline, anchors);
        }
    }

    private void appendInline(P paragraph,
                              DocxDocumentModel.Inline inline,
                              Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        if (inline instanceof DocxDocumentModel.TextInline text) {
            addTextRun(paragraph, text.text(), null);
            return;
        }
        if (inline instanceof DocxDocumentModel.StrongInline strong) {
            appendStyledInlineChildren(paragraph, strong.children(), anchors, runProps(null, true, false, null, null, null));
            return;
        }
        if (inline instanceof DocxDocumentModel.EmphasisInline emphasis) {
            appendStyledInlineChildren(paragraph, emphasis.children(), anchors, runProps(null, false, true, null, null, null));
            return;
        }
        if (inline instanceof DocxDocumentModel.MonospaceInline mono) {
            addTextRun(paragraph, mono.text(), runProps(null, false, false, "Courier New", null, null));
            return;
        }
        if (inline instanceof DocxDocumentModel.LineBreakInline) {
            addLineBreak(paragraph);
            return;
        }
        if (inline instanceof DocxDocumentModel.CalloutInline callout) {
            addTextRun(paragraph, "(" + callout.number() + ")", runProps(null, true, false, null, null, null));
            return;
        }
        if (inline instanceof DocxDocumentModel.LinkInline link) {
            addExternalLink(paragraph, link, anchors);
            return;
        }
        if (inline instanceof DocxDocumentModel.InternalXrefInline xref) {
            addInternalReference(paragraph, xref, anchors);
            return;
        }
        if (inline instanceof DocxDocumentModel.FootnoteInline footnote) {
            addFootnoteReference(paragraph, footnote, anchors);
            return;
        }
        throw new IllegalArgumentException("Unsupported DOCX inline: " + inline.getClass().getName());
    }

    private void appendStyledInlineChildren(P paragraph,
                                            List<DocxDocumentModel.Inline> children,
                                            Map<String, DocxDocumentModel.AnchorTarget> anchors,
                                            RPr style) throws Exception {
        for (DocxDocumentModel.Inline child : children) {
            if (child instanceof DocxDocumentModel.TextInline text) {
                addTextRun(paragraph, text.text(), copyRunProps(style));
            } else if (child instanceof DocxDocumentModel.MonospaceInline mono) {
                RPr props = copyRunProps(style);
                if (props == null) {
                    props = WML.createRPr();
                }
                props.setRFonts(fonts("Courier New"));
                addTextRun(paragraph, mono.text(), props);
            } else {
                appendInline(paragraph, child, anchors);
            }
        }
    }

    private void addExternalLink(P paragraph,
                                 DocxDocumentModel.LinkInline link,
                                 Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        Relationship relationship = new Relationship();
        relationship.setType(Namespaces.HYPERLINK);
        relationship.setTarget(link.href());
        relationship.setTargetMode("External");
        relationship.setId(mainDocumentPart.getRelationshipsPart().getNextId());
        mainDocumentPart.getRelationshipsPart().addRelationship(relationship);

        P.Hyperlink hyperlink = WML.createPHyperlink();
        hyperlink.setId(relationship.getId());
        for (DocxDocumentModel.Inline child : link.label()) {
            if (child instanceof DocxDocumentModel.TextInline text) {
                R run = WML.createR();
                run.setRPr(runProps(null, false, false, null, "0563C1", UnderlineEnumeration.SINGLE));
                run.getContent().add(WML.createRT(text(text.text())));
                hyperlink.getContent().add(run);
            } else {
                appendInline(paragraph, child, anchors);
            }
        }
        paragraph.getContent().add(WML.createPHyperlink(hyperlink));
    }

    private void addInternalReference(P paragraph,
                                      DocxDocumentModel.InternalXrefInline xref,
                                      Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        DocxDocumentModel.AnchorTarget target = anchors.get(xref.targetId());
        if (target == null) {
            throw new IOException("Unresolved internal DOCX target: " + xref.targetId());
        }
        String fallback = plainText(xref.label());
        if (xref.chapterNumber() != null && !xref.chapterNumber().isBlank()) {
            addTextRun(paragraph, localizedChapterLabel(documentLanguage) + " ", null);
            addSimpleField(paragraph, DocxFieldSupport.refNumberInstruction(target.bookmarkName()), xref.chapterNumber());
            String suffix = fallback.replaceFirst("^" + Pattern.quote(localizedChapterLabel(documentLanguage)) + "\\s+" + Pattern.quote(xref.chapterNumber()), "");
            if (!suffix.isBlank()) {
                addTextRun(paragraph, suffix, null);
            }
        } else {
            addSimpleField(paragraph, DocxFieldSupport.refInstruction(target.bookmarkName()), fallback.isBlank() ? target.displayText() : fallback);
        }
        if (xref.pageRef()) {
            addTextRun(paragraph, " (" + localizedPageLabel(documentLanguage) + " ", null);
            addSimpleField(paragraph, DocxFieldSupport.pageRefInstruction(target.bookmarkName()), "?");
            addTextRun(paragraph, ")", null);
        }
    }

    private void addFootnoteReference(P paragraph,
                                      DocxDocumentModel.FootnoteInline footnote,
                                      Map<String, DocxDocumentModel.AnchorTarget> anchors) throws Exception {
        BigInteger id = footnoteIds.get(footnote.id());
        if (id == null) {
            id = nextFootnoteId;
            nextFootnoteId = nextFootnoteId.add(BigInteger.ONE);
            footnoteIds.put(footnote.id(), id);
            CTFtnEdn note = WML.createCTFtnEdn();
            note.setId(id);
            note.setType(STFtnEdn.NORMAL);
            P p = WML.createP();
            appendInlines(p, footnote.content(), anchors);
            note.getContent().add(p);
            footnotesPart.getJaxbElement().getFootnote().add(note);
        }
        R run = WML.createR();
        RPr rPr = WML.createRPr();
        CTVerticalAlignRun verticalAlign = WML.createCTVerticalAlignRun();
        verticalAlign.setVal(STVerticalAlignRun.SUPERSCRIPT);
        rPr.setVertAlign(verticalAlign);
        run.setRPr(rPr);
        CTFtnEdnRef ref = WML.createCTFtnEdnRef();
        ref.setId(id);
        run.getContent().add(WML.createRFootnoteReference(ref));
        paragraph.getContent().add(run);
    }

    private void addSimpleField(P paragraph, String instruction, String fallbackText) {
        CTSimpleField field = WML.createCTSimpleField();
        field.setInstr(instruction);
        R r = WML.createR();
        r.getContent().add(WML.createRT(text(fallbackText)));
        field.getContent().add(r);
        paragraph.getContent().add(WML.createPFldSimple(field));
    }

    private void addFieldCharRun(P paragraph, STFldCharType type) {
        R run = WML.createR();
        FldChar fieldChar = WML.createFldChar();
        fieldChar.setFldCharType(type);
        run.getContent().add(WML.createRFldChar(fieldChar));
        paragraph.getContent().add(run);
    }

    private void addInstrTextRun(P paragraph, String instruction) {
        R run = WML.createR();
        Text text = text(instruction);
        text.setSpace("preserve");
        run.getContent().add(WML.createRInstrText(text));
        paragraph.getContent().add(run);
    }

    private void addTextRun(P paragraph, String value, RPr properties) {
        R run = WML.createR();
        if (properties != null) {
            run.setRPr(properties);
        }
        run.getContent().add(WML.createRT(text(value)));
        paragraph.getContent().add(run);
    }

    private void addLineBreak(P paragraph) {
        R run = WML.createR();
        Br br = WML.createBr();
        run.getContent().add(br);
        paragraph.getContent().add(run);
    }

    private void addPageBreak(P paragraph) {
        R run = WML.createR();
        Br br = WML.createBr();
        br.setType(STBrType.PAGE);
        run.getContent().add(br);
        paragraph.getContent().add(run);
    }

    private void openBookmark(P paragraph,
                              String rawId,
                              Map<String, DocxDocumentModel.AnchorTarget> anchors) {
        if (rawId == null || rawId.isBlank()) {
            return;
        }
        DocxDocumentModel.AnchorTarget target = anchors.get(rawId);
        if (target == null) {
            return;
        }
        CTBookmark bookmark = WML.createCTBookmark();
        bookmark.setName(target.bookmarkName());
        bookmark.setId(bookmarkId);
        paragraph.getContent().add(WML.createPBookmarkStart(bookmark));
    }

    private void closePendingBookmark(P paragraph) {
        if (paragraph.getContent().stream().noneMatch(jaxb -> jaxb instanceof jakarta.xml.bind.JAXBElement<?> element && element.getDeclaredType() == CTBookmark.class)) {
            return;
        }
        CTMarkupRange end = WML.createCTMarkupRange();
        end.setId(bookmarkId);
        paragraph.getContent().add(WML.createPBookmarkEnd(end));
        bookmarkId = bookmarkId.add(BigInteger.ONE);
    }

    private void applyParagraphStyle(P paragraph, String styleId) {
        PPr pPr = paragraph.getPPr();
        if (pPr == null) {
            pPr = WML.createPPr();
            paragraph.setPPr(pPr);
        }
        PPrBase.PStyle style = WML.createPPrBasePStyle();
        style.setVal(styleId);
        pPr.setPStyle(style);
    }

    private P createStyledParagraph(String styleId) {
        P p = WML.createP();
        applyParagraphStyle(p, styleId);
        return p;
    }

    private void setParagraphAlignment(P paragraph, JcEnumeration alignment) {
        PPr pPr = paragraph.getPPr();
        if (pPr == null) {
            pPr = WML.createPPr();
            paragraph.setPPr(pPr);
        }
        Jc jc = WML.createJc();
        jc.setVal(alignment);
        pPr.setJc(jc);
    }

    private void applyNumbering(P paragraph, BigInteger numId, int level) {
        if (numId == null) {
            return;
        }
        PPr pPr = paragraph.getPPr();
        if (pPr == null) {
            pPr = WML.createPPr();
            paragraph.setPPr(pPr);
        }
        PPrBase.NumPr numPr = WML.createPPrBaseNumPr();
        PPrBase.NumPr.NumId numIdValue = WML.createPPrBaseNumPrNumId();
        numIdValue.setVal(numId);
        numPr.setNumId(numIdValue);
        PPrBase.NumPr.Ilvl ilvl = WML.createPPrBaseNumPrIlvl();
        ilvl.setVal(BigInteger.valueOf(level));
        numPr.setIlvl(ilvl);
        pPr.setNumPr(numPr);
    }

    private void applyTableBorder(TblPr tblPr, String color) {
        TblBorders borders = new TblBorders();
        borders.setTop(border(color));
        borders.setBottom(border(color));
        borders.setLeft(border(color));
        borders.setRight(border(color));
        borders.setInsideH(border(color));
        borders.setInsideV(border(color));
        tblPr.setTblBorders(borders);
    }

    private CTBorder border(String color) {
        CTBorder border = WML.createCTBorder();
        border.setVal(STBorder.SINGLE);
        border.setColor(color);
        border.setSz(BigInteger.valueOf(8));
        return border;
    }

    private void applyShading(TcPr tcPr, String fill) {
        CTShd shd = WML.createCTShd();
        shd.setVal(STShd.CLEAR);
        shd.setFill(fill);
        tcPr.setShd(shd);
    }

    private RPr boldRun(boolean bold, Integer size, boolean italic) {
        return runProps(size, bold, italic, null, null, null);
    }

    private RPr runProps(Integer size,
                         boolean bold,
                         boolean italic,
                         String fontFamily,
                         String color,
                         UnderlineEnumeration underline) {
        RPr rPr = WML.createRPr();
        if (fontFamily != null && !fontFamily.isBlank()) {
            rPr.setRFonts(fonts(fontFamily));
        }
        if (size != null) {
            HpsMeasure sz = WML.createHpsMeasure();
            sz.setVal(BigInteger.valueOf(size * 2L));
            rPr.setSz(sz);
            HpsMeasure szCs = WML.createHpsMeasure();
            szCs.setVal(BigInteger.valueOf(size * 2L));
            rPr.setSzCs(szCs);
        }
        if (bold) {
            rPr.setB(new BooleanDefaultTrue());
        }
        if (italic) {
            rPr.setI(new BooleanDefaultTrue());
        }
        if (color != null && !color.isBlank()) {
            Color c = WML.createColor();
            c.setVal(color);
            rPr.setColor(c);
        }
        if (underline != null) {
            U u = WML.createU();
            u.setVal(underline);
            rPr.setU(u);
        }
        return rPr;
    }

    private RFonts fonts(String fontFamily) {
        RFonts fonts = WML.createRFonts();
        fonts.setAscii(fontFamily);
        fonts.setHAnsi(fontFamily);
        fonts.setHint(STHint.DEFAULT);
        return fonts;
    }

    private RPr copyRunProps(RPr source) {
        if (source == null) {
            return null;
        }
        RPr copy = WML.createRPr();
        copy.setRFonts(source.getRFonts());
        copy.setB(source.getB());
        copy.setI(source.getI());
        copy.setColor(source.getColor());
        copy.setU(source.getU());
        copy.setSz(source.getSz());
        copy.setSzCs(source.getSzCs());
        copy.setVertAlign(source.getVertAlign());
        return copy;
    }

    private Text text(String value) {
        Text text = WML.createText();
        text.setValue(value != null ? value : "");
        if (value != null && (value.startsWith(" ") || value.endsWith(" ") || value.contains("  "))) {
            text.setSpace("preserve");
        }
        return text;
    }

    private long[] imageExtent(Path imagePath, String requestedWidth) throws IOException {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            long width = Math.round(CONTENT_WIDTH_POINTS * 12700);
            return new long[]{width, Math.round(width * 0.66)};
        }
        double widthPoints = parseImageWidthPoints(requestedWidth);
        if (widthPoints <= 0) {
            widthPoints = CONTENT_WIDTH_POINTS;
        }
        widthPoints = Math.min(widthPoints, CONTENT_WIDTH_POINTS);
        double heightPoints = widthPoints * image.getHeight() / (double) image.getWidth();
        return new long[]{
            Math.round(widthPoints * 12700),
            Math.round(heightPoints * 12700)
        };
    }

    private double parseImageWidthPoints(String width) {
        if (width == null || width.isBlank()) {
            return -1;
        }
        String normalized = width.trim().toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("%")) {
                double percent = Double.parseDouble(normalized.substring(0, normalized.length() - 1).trim());
                return CONTENT_WIDTH_POINTS * percent / 100.0;
            }
            if (normalized.endsWith("px")) {
                double pixels = Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim());
                return pixels * 72.0 / DEFAULT_IMAGE_DPI;
            }
            double pixels = Double.parseDouble(normalized);
            return pixels * 72.0 / DEFAULT_IMAGE_DPI;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private int maxColumns(List<DocxDocumentModel.TableRowModel> rows) {
        int max = 0;
        for (DocxDocumentModel.TableRowModel row : rows) {
            int count = 0;
            for (DocxDocumentModel.TableCellModel cell : row.cells()) {
                count += Math.max(1, cell.colspan());
            }
            max = Math.max(max, count);
        }
        return max;
    }

    private void addTo(ContentAccessor container, Object value) {
        container.getContent().add(value);
    }

    private String plainText(List<DocxDocumentModel.Inline> inlines) {
        StringBuilder text = new StringBuilder();
        for (DocxDocumentModel.Inline inline : inlines) {
            if (inline instanceof DocxDocumentModel.TextInline t) {
                text.append(t.text());
            } else if (inline instanceof DocxDocumentModel.StrongInline strong) {
                text.append(plainText(strong.children()));
            } else if (inline instanceof DocxDocumentModel.EmphasisInline emphasis) {
                text.append(plainText(emphasis.children()));
            } else if (inline instanceof DocxDocumentModel.MonospaceInline mono) {
                text.append(mono.text());
            } else if (inline instanceof DocxDocumentModel.LinkInline link) {
                text.append(plainText(link.label()));
            } else if (inline instanceof DocxDocumentModel.InternalXrefInline xref) {
                text.append(plainText(xref.label()));
            } else if (inline instanceof DocxDocumentModel.FootnoteInline footnote) {
                text.append(plainText(footnote.content()));
            } else if (inline instanceof DocxDocumentModel.CalloutInline callout) {
                text.append(callout.number());
            } else if (inline instanceof DocxDocumentModel.LineBreakInline) {
                text.append(' ');
            } else {
                throw new IllegalArgumentException("Unsupported DOCX inline: " + inline.getClass().getName());
            }
        }
        return text.toString();
    }

    private String headingLevelText(int zeroBasedLevel) {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= zeroBasedLevel + 1; i++) {
            if (i > 1) {
                text.append('.');
            }
            text.append('%').append(i);
        }
        text.append('.');
        return text.toString();
    }

    private String localizedTocTitle(String language) {
        return isGerman(language) ? "Inhaltsverzeichnis" : "Table of Contents";
    }

    private String localizedTocPlaceholder(String language) {
        return isGerman(language)
            ? "Inhaltsverzeichnis wird beim Aktualisieren der Felder erzeugt."
            : "Table of contents will be generated when fields are updated.";
    }

    private String localizedChangeLogTitle(String language) {
        return isGerman(language) ? "Änderungsverzeichnis" : "Change Log";
    }

    private String localizedChangeLogPlaceholder(String language) {
        return isGerman(language)
            ? "Platzhalter: Inhalt des Änderungsverzeichnisses wird in einer späteren Ausbaustufe befüllt."
            : "Placeholder: the change log content will be added in a later iteration.";
    }

    private String localizedFigureLabel(String language) {
        return isGerman(language) ? "Abbildung" : "Figure";
    }

    private String localizedTableLabel(String language) {
        return isGerman(language) ? "Tabelle" : "Table";
    }

    private String localizedChapterLabel(String language) {
        return isGerman(language) ? "Kapitel" : "Chapter";
    }

    private String localizedPageLabel(String language) {
        return isGerman(language) ? "Seite" : "Page";
    }

    private String localizedAdmonitionLabel(String style, String language) {
        String normalized = style != null ? style.trim().toLowerCase(Locale.ROOT) : "";
        return switch (normalized) {
            case "tip" -> isGerman(language) ? "Tipp" : "Tip";
            case "important" -> isGerman(language) ? "Wichtig" : "Important";
            case "warning" -> isGerman(language) ? "Warnung" : "Warning";
            case "caution" -> isGerman(language) ? "Vorsicht" : "Caution";
            default -> isGerman(language) ? "Hinweis" : "Note";
        };
    }

    private String admonitionFill(String style) {
        String normalized = style != null ? style.trim().toLowerCase(Locale.ROOT) : "";
        return switch (normalized) {
            case "tip" -> "E8F5E9";
            case "important" -> "F5F0FF";
            case "warning" -> "FFF3E0";
            case "caution" -> "FDECEA";
            default -> "EEF4FF";
        };
    }

    private String defaultFigureCaption(String language) {
        return isGerman(language) ? "Abbildung" : "Figure";
    }

    private boolean isGerman(String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("de");
    }
}
