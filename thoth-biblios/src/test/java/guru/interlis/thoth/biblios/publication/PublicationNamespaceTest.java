package guru.interlis.thoth.biblios.publication;

import guru.interlis.thoth.biblios.catalog.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PublicationNamespaceTest {
    @TempDir Path root;
    private SiteCatalog catalog(String id) {
        return new SiteCatalog(List.of(new DocComponent(id, "Docs", "main",
            List.of(new ComponentVersion(id, "main", "main", "main", null, null, List.of())))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"site-assets", "SITE-ASSETS", "Site-Assets", "a/b", ".", ".."})
    void writerRejectsUnsafeNamespaceBeforeWriting(String id) {
        assertThrows(IllegalArgumentException.class, () -> new PublicationPackageWriter(
            root.resolve("package"), root.resolve("output"), catalog(id)));
        assertFalse(Files.exists(root.resolve("package")));
    }

    @Test void ordinaryAttachmentsKeepSourceAndThemeStaysShared() throws Exception {
        Path output = root.resolve("output");
        Files.createDirectories(output.resolve("internal-docs/main"));
        Files.createDirectories(output.resolve("site-assets"));
        Files.writeString(output.resolve("internal-docs/main/secret.txt"), "secret");
        Files.writeString(output.resolve("site-assets/styles.css"), "body{}");
        var writer = new PublicationPackageWriter(root.resolve("package"), output, catalog("internal-docs"));
        writer.begin(); writer.finish();
        var attachment = writer.manifest().find("internal-docs/main/secret.txt").orElseThrow();
        assertEquals("internal-docs", attachment.sourceId());
        assertEquals("SOURCE", attachment.scope().name());
        var asset = writer.manifest().find("site-assets/styles.css").orElseThrow();
        assertNull(asset.sourceId());
        assertEquals("SHARED", asset.scope().name());
    }
}
