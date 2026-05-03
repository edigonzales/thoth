package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.config.DocxFeaturesSection;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class Docx4jWriterListStyleTest {

    @Test
    void listStylesDoNotForceSansSerifFonts(@TempDir Path tempDir) throws Exception {
        ComponentVersion version = new ComponentVersion("comp", "v1", "v1", "main", "index.adoc", null, List.of());
        DocComponent component = new DocComponent("comp", "Component", "v1", List.of(version));

        DocxDocumentModel.DocumentModel model = new DocxDocumentModel.DocumentModel(
            "Doc",
            "de",
            "book",
            tempDir.resolve("index.adoc"),
            List.of(new DocxDocumentModel.UnorderedListBlock(
                "list-1",
                List.of(new DocxDocumentModel.ListItemModel(
                    List.of(new DocxDocumentModel.TextInline("Eintrag")),
                    List.of()
                ))
            )),
            Map.of()
        );

        Path output = tempDir.resolve("out.docx");
        new Docx4jWriter(null, component, version, new DocxFeaturesSection(false, false, false)).write(model, output);

        WordprocessingMLPackage loaded = WordprocessingMLPackage.load(output.toFile());
        StyleDefinitionsPart styles = loaded.getMainDocumentPart().getStyleDefinitionsPart();
        assertNotNull(styles);

        Style listBullet = styles.getStyleById("ListBullet");
        Style listNumber = styles.getStyleById("ListNumber");
        assertNotNull(listBullet);
        assertNotNull(listNumber);
        assertNotNull(listBullet.getRPr());
        assertNotNull(listNumber.getRPr());

        assertNull(listBullet.getRPr().getRFonts());
        assertNull(listNumber.getRPr().getRFonts());
    }
}
