package guru.interlis.thoth.biblios.server.publication;

import java.util.List;
import java.util.Map;

/**
 * Catalog metadata and prebuilt view models from {@code catalog.json}.
 * Written by {@code PublicationCatalogWriter} and read with Jackson.
 */
public record PackageCatalog(
    int version,
    Site site,
    Ui ui,
    List<SourceEntry> sources,
    Home home,
    List<ComponentEntry> components
) {

    public record Site(String title, String url, String logo, String defaultLanguage) {
    }

    public record Ui(
        boolean showEditLink,
        boolean showSourceLink,
        String searchLanguageMode,
        boolean syntaxHighlightingEnabled,
        List<String> prismCustomComponentHrefs,
        String interlisLabScriptHref
    ) {
    }

    public record SourceEntry(String id, String accessPolicy) {
    }

    public record Home(Map<String, Object> catalog, List<Map<String, Object>> docSwitcher) {
    }

    public record ComponentEntry(
        String id,
        Map<String, Object> component,
        List<Map<String, Object>> versionSwitcher,
        List<VersionEntry> versions
    ) {
    }

    public record VersionEntry(
        String version,
        String displayVersion,
        String route,
        boolean singlePage,
        String startPage,
        Map<String, Object> navigation,
        Map<String, Object> singlePageNavigation,
        String defaultPageRoute,
        String initialChapterId,
        List<PageEntry> pages
    ) {
    }

    public record PageEntry(
        String route,
        String fragment,
        boolean usesInterlisLab,
        String editUrl,
        String sourceUrl,
        Map<String, Object> model,
        List<Map<String, Object>> breadcrumbs,
        String prevRoute,
        String nextRoute,
        String prevTitle,
        String nextTitle
    ) {
    }
}
