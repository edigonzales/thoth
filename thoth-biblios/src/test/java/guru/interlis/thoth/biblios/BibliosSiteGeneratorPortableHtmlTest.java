package guru.interlis.thoth.biblios;

import guru.interlis.thoth.biblios.catalog.CatalogBuilder;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.fixture.BibliosConfigBuilder;
import guru.interlis.thoth.biblios.fixture.SiteAssertions;
import guru.interlis.thoth.biblios.fixture.TestRepoBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibliosSiteGeneratorPortableHtmlTest {

    @Test
    void generatesPortableRelativeSiteChromeAndSearchArtifacts(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        new TestRepoBuilder(repoDir).withBasicDocs();

        Path outputDir = tempDir.resolve("site");
        generateSite(tempDir, repoDir, outputDir);

        SiteAssertions site = new SiteAssertions(outputDir);
        site.assertFileExists("search-index.json");
        site.assertFileExists("search-index.js");
        site.assertSiteAssets();

        Document globalIndex = Jsoup.parse(Files.readString(outputDir.resolve("index.html")));
        assertEquals("./site-assets/styles.css", attr(globalIndex, "link[href$=styles.css]", "href"));
        assertEquals("./", attr(globalIndex, "a.site-title", "href"));
        assertEquals("./search/", attr(globalIndex, "form.search-form", "action"));

        Document guidePage = Jsoup.parse(Files.readString(outputDir.resolve("docs/main/guide/index.html")));
        assertEquals("../../../site-assets/styles.css", attr(guidePage, "link[href$=styles.css]", "href"));
        assertEquals("../../../search/", attr(guidePage, "form.search-form", "action"));
        assertEquals("../../../docs/main/", attr(guidePage, "#doc-switch option[value][selected]", "value"));

        Document searchPage = Jsoup.parse(Files.readString(outputDir.resolve("search/index.html")));
        assertEquals("../search-index.js", attr(searchPage, "script[src$='search-index.js']", "src"));
        assertEquals("../search-index.json", searchPage.body().attr("data-search-index-url"));

        String searchJs = Files.readString(outputDir.resolve("site-assets/search.js"));
        assertFalse(searchJs.contains("\"/search-index.json\""));

        String searchIndexJs = Files.readString(outputDir.resolve("search-index.js"));
        assertTrue(searchIndexJs.contains("window.__BIBLIOS_SEARCH_INDEX__"));
    }

    @Test
    void rewritesInternalContentLinksToPortableBibliosRoutes(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        new TestRepoBuilder(repoDir).withBasicDocsAndCrossReferences();

        Path outputDir = tempDir.resolve("site");
        generateSite(tempDir, repoDir, outputDir);

        Document indexPage = Jsoup.parse(Files.readString(outputDir.resolve("docs/main/index.html")));
        Map<String, String> indexLinks = linksByText(indexPage.select(".doc-content a[href]"));
        assertEquals("guide/", indexLinks.get("Guide via xref"));
        assertEquals("guide/", indexLinks.get("Guide via html"));
        assertEquals("guide/", indexLinks.get("Guide via absolute route"));
        assertEquals("guide/#configuration", indexLinks.get("Guide section"));

        String indexHtml = Files.readString(outputDir.resolve("docs/main/index.html"));
        assertFalse(indexHtml.contains("guide.html"));
        assertFalse(indexHtml.contains("guide.adoc"));
        assertFalse(indexHtml.contains("href=\"/docs/main/guide/\""));

        Document guidePage = Jsoup.parse(Files.readString(outputDir.resolve("docs/main/guide/index.html")));
        Map<String, String> guideLinks = linksByText(guidePage.select(".doc-content a[href]"));
        assertEquals("../config/", guideLinks.get("Config sibling"));
    }

    private void generateSite(Path tempDir, Path repoDir, Path outputDir) throws Exception {
        Path configFile = tempDir.resolve("biblios.yml");
        BibliosConfig config = new BibliosConfigBuilder()
            .withOutputDir(outputDir)
            .withSingleSourceGitRepo(repoDir, "docs", "Documentation", "docs", "main", "main")
            .writeTo(configFile);

        SiteCatalog catalog;
        try (CatalogBuilder builder = new CatalogBuilder(config, tempDir.resolve("work"), false)) {
            catalog = builder.build();
        }

        try (BibliosSiteGenerator generator = new BibliosSiteGenerator(config, catalog, outputDir)) {
            generator.generate();
        }
    }

    private String attr(Document document, String cssQuery, String attribute) {
        Element element = document.selectFirst(cssQuery);
        return element != null ? element.attr(attribute) : "";
    }

    private Map<String, String> linksByText(Iterable<Element> links) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Element link : links) {
            result.put(link.text().trim(), link.attr("href"));
        }
        return result;
    }
}
