package guru.interlis.thoth.biblios.server.publication;

import java.util.List;

/**
 * Parsed {@code manifest.json} of a publication package.
 */
public record ManifestDocument(int version, List<Entry> entries) {

    public record Entry(String path, String scope, String source, String kind) {
    }
}
