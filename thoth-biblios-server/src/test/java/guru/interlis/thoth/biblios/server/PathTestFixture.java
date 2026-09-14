package guru.interlis.thoth.biblios.server;

import guru.interlis.thoth.biblios.server.publication.ManifestDocument;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

final class PathTestFixture {
    static PackageTestFixture.Fixture create(Path root) throws IOException {
        var fixture = PackageTestFixture.create(root);
        var mapper = JsonMapper.builder().build();
        Path manifestPath = fixture.packageDir().resolve("manifest.json");
        var manifest = mapper.readValue(manifestPath.toFile(), ManifestDocument.class);
        var entries = new ArrayList<>(manifest.entries());
        add(fixture, entries, "public docs/main/attachment.txt", "public-docs", "PUBLIC SPACE");
        add(fixture, entries, "public+docs/main/attachment.txt", "internal-docs", "PROTECTED PLUS");
        add(fixture, entries, "public-docs/main/über space.txt", "public-docs", "UNICODE");
        add(fixture, entries, "public docs/main/unlisted.txt", "public-docs", "PUBLIC DECOY");
        Files.writeString(fixture.packageDir().resolve("files/public+docs/main/unlisted.txt"), "UNLISTED SECRET");
        // An explicitly manifested symlink must not escape the servable directory.
        Path outside = root.resolve("outside-secret.txt");
        Files.writeString(outside, "OUTSIDE SECRET");
        Files.createSymbolicLink(fixture.packageDir().resolve("files/public-docs/main/link.txt"), outside);
        entries.add(new ManifestDocument.Entry("public-docs/main/link.txt", "source", "public-docs", "file"));
        Files.writeString(manifestPath, mapper.writeValueAsString(new ManifestDocument(1, entries)));
        return fixture;
    }

    private static void add(PackageTestFixture.Fixture fixture, ArrayList<ManifestDocument.Entry> entries,
                            String path, String source, String content) throws IOException {
        Path file = fixture.packageDir().resolve("files").resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        entries.add(new ManifestDocument.Entry(path, "source", source, "file"));
    }
}
