package guru.interlis.thoth.biblios.publication;

/**
 * Visibility scope of a file or page inside a publication package.
 */
public enum PublicationScope {
    /** Belongs to a documentation source and inherits its access policy. */
    SOURCE,
    /** Shared portal assets (theme, fonts, scripts) that are always public. */
    SHARED,
    /** Internal build data (search index, catalog, manifest) never served directly. */
    INTERNAL
}
