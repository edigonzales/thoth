package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.server.publication.ManifestDocument;
import org.springframework.core.io.FileSystemResource;

/** Request-local result of manifest lookup, path resolution and authorization. */
record AuthorizedFile(ManifestDocument.Entry entry, FileSystemResource resource) {
    static final String ATTRIBUTE = AuthorizedFile.class.getName();
}
