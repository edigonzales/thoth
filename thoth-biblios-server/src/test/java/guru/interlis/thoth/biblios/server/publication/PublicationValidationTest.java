package guru.interlis.thoth.biblios.server.publication;

import guru.interlis.thoth.biblios.server.PackageTestFixture;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class PublicationValidationTest {
    @TempDir Path root;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @ParameterizedTest
    @ValueSource(strings = {"site-assets", "SITE-ASSETS", "Site-Assets"})
    void reservedSourcesAndComponentsAreRejected(String id) throws Exception {
        var f = PackageTestFixture.create(root);
        Path catalog = f.packageDir().resolve("catalog.json");
        String original = Files.readString(catalog);
        // Check each list independently, including a stale/malformed legacy package.
        var tree = mapper.readTree(original);
        ((tools.jackson.databind.node.ObjectNode) tree.get("sources").get(0)).put("id", id);
        Files.writeString(catalog, mapper.writeValueAsString(tree));
        assertThrows(IllegalArgumentException.class, () -> load(f));
        tree = mapper.readTree(original);
        ((tools.jackson.databind.node.ObjectNode) tree.get("components").get(0)).put("id", id);
        Files.writeString(catalog, mapper.writeValueAsString(tree));
        assertThrows(IllegalArgumentException.class, () -> load(f));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-directory", "source-assigned", "traversal"})
    void contradictorySharedEntriesAreRejected(String variant) throws Exception {
        var f = PackageTestFixture.create(root);
        Path path = f.packageDir().resolve("manifest.json");
        var manifest = mapper.readValue(path.toFile(), ManifestDocument.class);
        var entries = new java.util.ArrayList<>(manifest.entries());
        entries.add(new ManifestDocument.Entry(
            variant.equals("wrong-directory") ? "internal-docs/secret.txt"
                : variant.equals("traversal") ? "site-assets/../internal-docs/secret.txt" : "site-assets/secret.txt",
            "shared", variant.equals("source-assigned") ? "internal-docs" : null, "file"));
        Files.writeString(path, mapper.writeValueAsString(new ManifestDocument(1, entries)));
        assertThrows(IllegalArgumentException.class, () -> load(f));
    }

    private PublicationPackage load(PackageTestFixture.Fixture f) {
        var p = new BibliosServerProperties(); p.setPackageDir(f.packageDir());
        return new PublicationPackage(p, mapper);
    }
}
