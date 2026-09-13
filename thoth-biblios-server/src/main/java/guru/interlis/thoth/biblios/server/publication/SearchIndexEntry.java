package guru.interlis.thoth.biblios.server.publication;

/**
 * One entry of the generated search index ({@code search-index.json}).
 */
public record SearchIndexEntry(
    String component,
    String version,
    String displayVersion,
    String kind,
    String title,
    String pageTitle,
    String sectionPath,
    int sectionLevel,
    String route,
    String content
) {
}
