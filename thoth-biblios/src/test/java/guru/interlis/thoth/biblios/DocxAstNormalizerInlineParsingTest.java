package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.render.AsciidoctorRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxAstNormalizerInlineParsingTest {

    @Test
    void parsesHtmlAndDelimitedInlineMarkupInsideListItems(@TempDir Path tempDir) throws Exception {
        Path adoc = tempDir.resolve("list-inline.adoc");
        Files.writeString(adoc, """
            = Inline Test

            * <strong>Überblick</strong> – siehe <a href="#ueberblickambeispielilistal">Kapitel 2</a>.
            * <b>Modellierungsmethoden</b> – mehr unter <a href="https://example.com/modellierung">diesem Link</a>.
            * **Doppelt fett** und _kursiv_
            """);

        DocxDocumentModel.DocumentModel model;
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer(true)) {
            AsciidoctorRenderer.LoadedDocument loaded = renderer.loadDocument(
                adoc,
                AsciidoctorRenderer.RenderOptions.split(false, "de"),
                java.util.Map.of()
            );
            model = DocxAstNormalizer.normalize(loaded, adoc.getParent());
        }

        DocxDocumentModel.UnorderedListBlock list = firstUnorderedList(model.blocks());
        assertNotNull(list);
        assertEquals(3, list.items().size());

        List<DocxDocumentModel.Inline> item1 = list.items().get(0).text();
        assertTrue(item1.stream().anyMatch(i -> i instanceof DocxDocumentModel.StrongInline s
            && plainText(s.children()).equals("Überblick")));
        assertTrue(item1.stream().anyMatch(i -> i instanceof DocxDocumentModel.InternalXrefInline x
            && x.targetId().equals("ueberblickambeispielilistal")
            && plainText(x.label()).equals("Kapitel 2")));
        assertFalse(plainText(item1).contains("<strong>"));
        assertFalse(plainText(item1).contains("<a href="));

        List<DocxDocumentModel.Inline> item2 = list.items().get(1).text();
        assertTrue(item2.stream().anyMatch(i -> i instanceof DocxDocumentModel.LinkInline l
            && l.href().equals("https://example.com/modellierung")
            && plainText(l.label()).equals("diesem Link")));

        List<DocxDocumentModel.Inline> item3 = list.items().get(2).text();
        assertTrue(item3.stream().anyMatch(i -> i instanceof DocxDocumentModel.StrongInline s
            && plainText(s.children()).equals("Doppelt fett")));
        assertTrue(item3.stream().anyMatch(i -> i instanceof DocxDocumentModel.EmphasisInline e
            && plainText(e.children()).equals("kursiv")));
    }

    private DocxDocumentModel.UnorderedListBlock firstUnorderedList(List<DocxDocumentModel.Block> blocks) {
        for (DocxDocumentModel.Block block : blocks) {
            if (block instanceof DocxDocumentModel.UnorderedListBlock unordered) {
                return unordered;
            }
            if (block instanceof DocxDocumentModel.SectionBlock section) {
                DocxDocumentModel.UnorderedListBlock nested = firstUnorderedList(section.children());
                if (nested != null) {
                    return nested;
                }
            }
            if (block instanceof DocxDocumentModel.OpenBlock open) {
                DocxDocumentModel.UnorderedListBlock nested = firstUnorderedList(open.children());
                if (nested != null) {
                    return nested;
                }
            }
            if (block instanceof DocxDocumentModel.ExampleBlock example) {
                DocxDocumentModel.UnorderedListBlock nested = firstUnorderedList(example.children());
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
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
            } else {
                assertInstanceOf(DocxDocumentModel.LineBreakInline.class, inline);
                text.append(' ');
            }
        }
        return text.toString();
    }
}
