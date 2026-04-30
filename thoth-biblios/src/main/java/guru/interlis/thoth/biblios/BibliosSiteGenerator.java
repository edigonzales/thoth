package guru.interlis.thoth.biblios;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import guru.interlis.thoth.biblios.catalog.*;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.RenderMode;
import guru.interlis.thoth.biblios.config.VersionSwitchMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Generates the HTML site from a SiteCatalog.
 */
public final class BibliosSiteGenerator implements AutoCloseable {
    private static final int SEARCH_MAX_SECTION_LEVEL = 4;
    private static final java.util.regex.Pattern IMG_SRC_PATTERN =
        java.util.regex.Pattern.compile("<img\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final List<String> PRISM_BUNDLED_ASSETS = List.of(
        "prism-conum-bridge.js",
        "prism/prism.css",
        "prism/prism.js",
        "prism/components/prism-markup.min.js",
        "prism/components/prism-clike.min.js",
        "prism/components/prism-javascript.min.js",
        "prism/components/prism-css.min.js",
        "prism/components/prism-ini.min.js",
        "prism/components/prism-interlis.js",
        "prism/components/prism-java.min.js",
        "prism/components/prism-typescript.min.js",
        "prism/components/prism-json.min.js",
        "prism/components/prism-bash.min.js",
        "prism/components/prism-sql.min.js",
        "prism/components/prism-python.min.js",
        "prism/components/prism-yaml.min.js",
        "prism/components/prism-kotlin.min.js",
        "prism/components/prism-go.min.js",
        "prism/components/prism-c.min.js",
        "prism/components/prism-cpp.min.js",
        "prism/plugins/line-highlight/prism-line-highlight.min.css",
        "prism/plugins/line-highlight/prism-line-highlight.min.js",
        "prism/plugins/line-numbers/prism-line-numbers.min.css",
        "prism/plugins/line-numbers/prism-line-numbers.min.js",
        "prism/plugins/toolbar/prism-toolbar.min.css",
        "prism/plugins/toolbar/prism-toolbar.min.js",
        "prism/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js",
        "icons/bootstrap-copy.svg",
        "icons/bootstrap-check.svg"
    );

    private final BibliosConfig config;
    private final SiteCatalog catalog;
    private final Path outputRoot;
    private final Configuration freemarker;
    private String siteLogo;

    public BibliosSiteGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot) {
        this.config = config;
        this.catalog = catalog;
        this.outputRoot = outputRoot;
        this.siteLogo = resolveConfiguredLogoReference();

        // Initialize FreeMarker
        freemarker = new Configuration(Configuration.VERSION_2_3_34);
        freemarker.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarker.setLogTemplateExceptions(false);
        freemarker.setWrapUncheckedExceptions(true);
    }

    /**
     * Generate the complete site.
     */
    public void generate() throws IOException {
        generate(true, Set.of(), false, Set.of());
    }

    /**
     * Generate the complete site with optional PDF generation control.
     */
    public void generate(boolean generatePdf, Set<String> selectedPdfVersions) throws IOException {
        generate(generatePdf, selectedPdfVersions, false, Set.of());
    }

    /**
     * Generate the complete site with optional PDF and DOCX generation control.
     */
    public void generate(boolean generatePdf, Set<String> selectedPdfVersions,
                         boolean generateDocx, Set<String> selectedDocxVersions) throws IOException {
        System.out.println("[info] Generating site to: " + outputRoot);

        // Clean output if configured
        if (config.output().clean() && Files.exists(outputRoot)) {
            deleteDirectory(outputRoot);
        }
        Files.createDirectories(outputRoot);

        // Copy site assets
        copyAssets();

        // Generate global start page
        generateHomePage();

        // Generate search page
        generateSearchPage();

        // Generate component pages
        for (DocComponent component : catalog.components()) {
            generateComponentPages(component);
        }

        // Generate search index
        generateSearchIndex();

        if (generatePdf && (isGlobalPdfEnabled() || hasSourceLevelPdfEnablement())) {
            new BibliosPdfGenerator(config, catalog, outputRoot).generate(selectedPdfVersions);
        }
        if (generateDocx && (isGlobalDocxEnabled() || hasSourceLevelDocxEnablement())) {
            new BibliosDocxGenerator(config, catalog, outputRoot).generate(selectedDocxVersions);
        }

        System.out.println("[info] Site generation complete.");
    }

    /**
     * Regenerate a single component tree and keep other component outputs untouched.
     */
    public void regenerateComponent(DocComponent component) throws IOException {
        Objects.requireNonNull(component, "component is required");
        Files.createDirectories(outputRoot);
        Path componentRoot = outputRoot.resolve(component.id());
        if (Files.exists(componentRoot)) {
            deleteDirectory(componentRoot);
        }
        generateComponentPages(component);
    }

    /**
     * Regenerate global artifacts that aggregate all components.
     */
    public void regenerateGlobalArtifacts() throws IOException {
        Files.createDirectories(outputRoot);
        generateHomePage();
        generateSearchPage();
        generateSearchIndex();
    }

    private boolean isGlobalPdfEnabled() {
        return config.pdf() != null && config.pdf().enabled();
    }

    private boolean hasSourceLevelPdfEnablement() {
        for (var source : config.content().sources()) {
            if (source.pdf() != null && Boolean.TRUE.equals(source.pdf().enabled())) {
                return true;
            }
        }
        return false;
    }

    private boolean isGlobalDocxEnabled() {
        return config.docx() != null && config.docx().enabled();
    }

    private boolean hasSourceLevelDocxEnablement() {
        for (var source : config.content().sources()) {
            if (source.docx() != null && Boolean.TRUE.equals(source.docx().enabled())) {
                return true;
            }
        }
        return false;
    }

    private void generateHomePage() throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("siteLogo", siteLogo);
        model.put("siteDescription", config.site().url());
        model.put("basePath", "");
        model.put("catalog", catalogToModel());
        model.put("locale", config.site().defaultLanguage());
        model.put("searchLanguageMode", uiSearchLanguageMode());
        model.put("syntaxHighlightingEnabled", uiSyntaxHighlightingEnabled());
        model.put("prismCustomComponentUrls", uiPrismCustomComponentUrls());

        String html = renderTemplate("index.ftl", model);
        writeOutput(Path.of("index.html"), html);
    }

    private void generateSearchPage() throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("siteLogo", siteLogo);
        model.put("basePath", "");
        model.put("locale", config.site().defaultLanguage());
        model.put("docSwitcher", buildDocSwitcher());
        model.put("searchLanguageMode", uiSearchLanguageMode());
        model.put("syntaxHighlightingEnabled", uiSyntaxHighlightingEnabled());
        model.put("prismCustomComponentUrls", uiPrismCustomComponentUrls());

        String html = renderTemplate("search.ftl", model);
        writeOutput(Path.of("search/index.html"), html);
    }

    private void generateComponentPages(DocComponent component) throws IOException {
        // Generate component-level landing page (<component>/index.html)
        generateComponentLandingPage(component);

        // Generate version-specific pages (<component>/<version>/...)
        for (ComponentVersion version : component.versions()) {
            generateVersionPages(component, version);
        }
    }

    private void generateComponentLandingPage(DocComponent component) throws IOException {
        Path componentRoot = outputRoot.resolve(component.id());
        Files.createDirectories(componentRoot);
        ComponentVersion defaultVersion = component.getVersion(component.defaultVersion());
        if (defaultVersion == null && !component.versions().isEmpty()) {
            defaultVersion = component.versions().get(0);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("siteLogo", siteLogo);
        model.put("basePath", "");
        model.put("locale", config.site().defaultLanguage());
        model.put("component", componentToModel(component));
        model.put("currentVersion", defaultVersion != null ? versionToModel(defaultVersion) : Map.of());
        model.put("currentComponentId", component.id());
        model.put("currentVersionStr", defaultVersion != null ? defaultVersion.version() : component.defaultVersion());
        model.put("navigation", null);
        model.put("docSwitcher", buildDocSwitcher());
        model.put("versionSwitcher", buildVersionSwitcher(component));
        model.put("searchLanguageMode", uiSearchLanguageMode());
        model.put("syntaxHighlightingEnabled", uiSyntaxHighlightingEnabled());
        model.put("prismCustomComponentUrls", uiPrismCustomComponentUrls());

        String html = renderTemplate("component.ftl", model);
        writeOutput(componentRoot.resolve("index.html"), html);
    }

    private void generateVersionPages(DocComponent component, ComponentVersion version) throws IOException {
        // Create output directory for this version
        Path versionRoot = outputRoot.resolve(component.id()).resolve(version.version());
        Files.createDirectories(versionRoot);

        // Generate content pages
        for (DocPage page : version.pages()) {
            generateContentPage(component, version, page, versionRoot);
        }

        // Copy local image assets referenced by generated page HTML.
        copyReferencedContentAssets(component, version, versionRoot);
    }

    private void generateContentPage(DocComponent component, ComponentVersion version, DocPage page, Path versionRoot) throws IOException {
        Path pageDir = resolvePageOutputDir(component, version, page, versionRoot);
        Files.createDirectories(pageDir);

        boolean singlePageMode = version.renderMode() == RenderMode.SINGLE_PAGE;

        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.site().title());
        model.put("siteLogo", siteLogo);
        model.put("basePath", "");
        model.put("locale", config.site().defaultLanguage());
        model.put("page", pageToModel(page));
        model.put("currentComponentId", component.id());
        model.put("currentVersionStr", version.version());
        model.put("currentVersion", versionToModel(version));
        model.put("currentPagePath", page.sourcePath());
        model.put(
            "navigation",
            version.navigation() != null
                ? (singlePageMode ? singlePageNavToModel(page.route(), version) : navToModel(component.id(), version))
                : null
        );
        model.put("docSwitcher", buildDocSwitcher());
        model.put("versionSwitcher", buildVersionSwitcher(component, page.sourcePath()));
        model.put("breadcrumbs", breadcrumbsToModel(page.breadcrumbs()));
        model.put("singlePageMode", singlePageMode);
        model.put("chapterBreadcrumbEnabled", singlePageMode);
        model.put("initialChapterId", singlePageMode ? singlePageInitialChapterId(version.navigation()) : "");
        model.put("editUrl", page.editUrl());
        model.put("sourceUrl", page.sourceUrl());
        model.put("showEditLink", config.ui() != null && config.ui().showEditLink());
        model.put("showSourceLink", config.ui() != null && config.ui().showSourceLink());
        model.put("searchLanguageMode", uiSearchLanguageMode());
        model.put("syntaxHighlightingEnabled", uiSyntaxHighlightingEnabled());
        model.put("prismCustomComponentUrls", uiPrismCustomComponentUrls());

        if (page.prev() != null) {
            model.put("prevPage", pageToModel(page.prev()));
        }
        if (page.next() != null) {
            model.put("nextPage", pageToModel(page.next()));
        }

        String html = renderTemplate("page.ftl", model);
        writeOutput(pageDir.resolve("index.html"), html);
    }

    private void copyReferencedContentAssets(DocComponent component, ComponentVersion version, Path versionRoot) throws IOException {
        Set<String> copiedTargets = new HashSet<>();

        for (DocPage page : version.pages()) {
            Path pageDir = resolvePageOutputDir(component, version, page, versionRoot);
            for (String src : extractImgSources(page.html())) {
                if (!isLocalRelativeAssetReference(src)) {
                    continue;
                }

                if (isSuspiciousDuplicateImagesPath(src)) {
                    System.err.println("[warn] Suspicious duplicate image path '" + src + "' in " + page.sourcePath());
                }

                Path sourceAsset = resolveSourceAssetPath(page, src);
                if (sourceAsset == null || !Files.exists(sourceAsset) || !Files.isRegularFile(sourceAsset)) {
                    System.err.println("[warn] Referenced image not found: " + src + " (source page: " + page.sourcePath() + ")");
                    continue;
                }

                Path targetAsset = pageDir.resolve(src).normalize();
                if (!targetAsset.startsWith(versionRoot)) {
                    System.err.println("[warn] Skipping unsafe image target path: " + src + " (source page: " + page.sourcePath() + ")");
                    continue;
                }

                String dedupeKey = sourceAsset.toAbsolutePath().normalize() + "->" + targetAsset.toAbsolutePath().normalize();
                if (!copiedTargets.add(dedupeKey)) {
                    continue;
                }

                Files.createDirectories(targetAsset.getParent());
                Files.copy(sourceAsset, targetAsset, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path resolvePageOutputDir(DocComponent component, ComponentVersion version, DocPage page, Path versionRoot) {
        // Build route path: /<component>/<version>/<page>/
        String prefix = "/" + component.id() + "/" + version.version();
        String routePath = page.route();
        if (routePath.startsWith(prefix)) {
            routePath = routePath.substring(prefix.length());
        }
        if (routePath.startsWith("/")) {
            routePath = routePath.substring(1);
        }
        if (routePath.endsWith("/")) {
            routePath = routePath.substring(0, routePath.length() - 1);
        }
        return versionRoot.resolve(routePath);
    }

    private static List<String> extractImgSources(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<String> sources = new ArrayList<>();
        Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        while (matcher.find()) {
            String src = matcher.group(2);
            if (src != null) {
                String normalized = src.trim();
                if (!normalized.isEmpty()) {
                    sources.add(normalized);
                }
            }
        }
        return sources;
    }

    private static boolean isLocalRelativeAssetReference(String src) {
        String normalized = src.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith("http://")
            || normalized.startsWith("https://")
            || normalized.startsWith("data:")
            || normalized.startsWith("mailto:")
            || normalized.startsWith("#")
            || normalized.startsWith("/")) {
            return false;
        }
        return true;
    }

    private static boolean isSuspiciousDuplicateImagesPath(String src) {
        String normalized = src.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.startsWith("images/images/") || normalized.contains("/images/images/");
    }

    private static Path resolveSourceAssetPath(DocPage page, String src) {
        Path baseDir = resolvePageBaseDir(page);
        if (baseDir == null) {
            return null;
        }

        Path srcPath = Path.of(src).normalize();
        if (srcPath.isAbsolute()) {
            return null;
        }

        // Primary strategy: preserve rendered src as-is relative to Asciidoctor baseDir.
        Path direct = baseDir.resolve(srcPath).normalize();
        if (Files.exists(direct) && Files.isRegularFile(direct)) {
            return direct;
        }

        // Fallback: combine imagesdir + src when src wasn't already prefixed by imagesdir.
        String imagesDir = page.imagesDir();
        if (imagesDir != null && !imagesDir.isBlank()) {
            Path imagesPath = Path.of(imagesDir).normalize();
            if (!imagesPath.isAbsolute() && !startsWithPathPrefix(srcPath, imagesPath)) {
                Path viaImagesDir = baseDir.resolve(imagesPath).resolve(srcPath).normalize();
                if (Files.exists(viaImagesDir) && Files.isRegularFile(viaImagesDir)) {
                    return viaImagesDir;
                }
            }
        }
        return direct;
    }

    private static Path resolvePageBaseDir(DocPage page) {
        if (page.sourceBaseDir() != null && !page.sourceBaseDir().isBlank()) {
            return Path.of(page.sourceBaseDir()).toAbsolutePath().normalize();
        }
        if (page.sourceUri() == null || page.sourceUri().isBlank()) {
            return null;
        }
        try {
            URI sourceUri = new URI(page.sourceUri());
            if (!"file".equalsIgnoreCase(sourceUri.getScheme())) {
                return null;
            }
            Path sourceFile = Path.of(sourceUri);
            return sourceFile.getParent() != null ? sourceFile.getParent().toAbsolutePath().normalize() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean startsWithPathPrefix(Path candidate, Path prefix) {
        if (candidate == null || prefix == null || prefix.getNameCount() == 0 || candidate.getNameCount() < prefix.getNameCount()) {
            return false;
        }
        for (int i = 0; i < prefix.getNameCount(); i++) {
            if (!candidate.getName(i).toString().equals(prefix.getName(i).toString())) {
                return false;
            }
        }
        return true;
    }

    private void generateSearchIndex() throws IOException {
        List<SearchIndexEntry> entries = new ArrayList<>();
        for (DocComponent component : catalog.components()) {
            for (ComponentVersion version : component.versions()) {
                for (DocPage page : version.pages()) {
                    entries.addAll(buildSearchIndexEntries(component, version, page));
                }
            }
        }

        StringBuilder json = new StringBuilder("[\n");
        boolean first = true;
        for (SearchIndexEntry entry : entries) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("  {")
                .append("\"component\":\"").append(escapeJson(entry.component())).append("\",")
                .append("\"version\":\"").append(escapeJson(entry.version())).append("\",")
                .append("\"displayVersion\":\"").append(escapeJson(entry.displayVersion())).append("\",")
                .append("\"kind\":\"").append(escapeJson(entry.kind())).append("\",")
                .append("\"title\":\"").append(escapeJson(entry.title())).append("\",")
                .append("\"pageTitle\":\"").append(escapeJson(entry.pageTitle())).append("\",")
                .append("\"sectionPath\":\"").append(escapeJson(entry.sectionPath())).append("\",")
                .append("\"sectionLevel\":").append(entry.sectionLevel()).append(",")
                .append("\"route\":\"").append(escapeJson(entry.route())).append("\",")
                .append("\"content\":\"").append(escapeJson(entry.content())).append("\"")
                .append("}");
        }

        json.append("\n]");

        writeOutput(Path.of("search-index.json"), json.toString());
    }

    private List<SearchIndexEntry> buildSearchIndexEntries(DocComponent component, ComponentVersion version, DocPage page) {
        List<SearchIndexEntry> entries = new ArrayList<>();
        String pageTitle = normalizeWhitespace(page.title());
        if (pageTitle.isBlank()) {
            pageTitle = normalizeWhitespace(page.navTitle());
        }
        if (pageTitle.isBlank()) {
            pageTitle = normalizeWhitespace(page.route());
        }

        Document document = Jsoup.parseBodyFragment(page.html() != null ? page.html() : "");
        Element body = document.body();
        for (Element chapter : searchableSections(body)) {
            int level = sectionLevel(chapter);
            Element heading = sectionHeading(chapter);
            String chapterId = normalizeChapterId(chapter.id());
            if (chapterId.isBlank() && heading != null) {
                chapterId = normalizeChapterId(heading.id());
            }
            if (chapterId.isBlank()) {
                continue;
            }
            String chapterTitle = normalizeWhitespace(heading != null ? heading.text() : "");
            if (chapterTitle.isBlank()) {
                chapterTitle = pageTitle;
            }
            String chapterPath = sectionPath(chapter, pageTitle);
            String chapterContent = sectionOwnContent(chapter, level);
            if (chapterContent.isBlank()) {
                continue;
            }
            entries.add(new SearchIndexEntry(
                component.id(),
                version.version(),
                version.displayVersion(),
                "chapter",
                chapterTitle,
                pageTitle,
                chapterPath,
                level,
                page.route() + "#" + chapterId,
                chapterContent
            ));
        }

        if (!entries.isEmpty()) {
            return entries;
        }

        String pageContent = normalizeWhitespace(body != null ? body.text() : "");
        entries.add(new SearchIndexEntry(
            component.id(),
            version.version(),
            version.displayVersion(),
            "page",
            pageTitle,
            pageTitle,
            pageTitle,
            0,
            page.route(),
            pageContent
        ));
        return entries;
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private List<Element> searchableSections(Element body) {
        if (body == null) {
            return List.of();
        }
        List<Element> result = new ArrayList<>();
        for (Element section : body.select("div[class~=\\bsect[1-6]\\b]")) {
            int level = sectionLevel(section);
            if (level > 0 && level <= SEARCH_MAX_SECTION_LEVEL) {
                result.add(section);
            }
        }
        return result;
    }

    private Element sectionHeading(Element section) {
        if (section == null) {
            return null;
        }
        Element direct = section.selectFirst("> h1, > h2, > h3, > h4, > h5, > h6");
        if (direct != null) {
            return direct;
        }
        return section.selectFirst("h1, h2, h3, h4, h5, h6");
    }

    private String sectionPath(Element section, String pageTitle) {
        if (section == null) {
            return pageTitle;
        }
        List<String> path = new ArrayList<>();
        List<Element> chain = new ArrayList<>();
        for (Element current = section; current != null; current = current.parent()) {
            int level = sectionLevel(current);
            if (level > 0 && level <= SEARCH_MAX_SECTION_LEVEL) {
                chain.add(current);
            }
        }
        Collections.reverse(chain);
        for (Element current : chain) {
            Element heading = sectionHeading(current);
            String headingTitle = normalizeWhitespace(heading != null ? heading.text() : "");
            if (!headingTitle.isBlank()) {
                path.add(headingTitle);
            }
        }
        if (path.isEmpty()) {
            return pageTitle;
        }
        return String.join(" > ", path);
    }

    private String sectionOwnContent(Element section, int sectionLevel) {
        if (section == null) {
            return "";
        }
        Element clone = section.clone();
        for (Element nested : clone.select("div[class~=\\bsect[1-6]\\b]")) {
            int nestedLevel = sectionLevel(nested);
            if (nestedLevel > sectionLevel) {
                nested.remove();
            }
        }
        return normalizeWhitespace(clone.text());
    }

    private int sectionLevel(Element element) {
        if (element == null) {
            return -1;
        }
        for (String className : element.classNames()) {
            if (className != null && className.matches("sect[1-6]")) {
                return className.charAt(4) - '0';
            }
        }
        return -1;
    }

    private void copyAssets() throws IOException {
        // Copy site-assets from resources to output
        Path assetsDest = outputRoot.resolve("site-assets");
        Files.createDirectories(assetsDest);

        copyAsset(assetsDest, "jetbrainsmono.css");
        copyAsset(assetsDest, "merriweather.css");
        copyAsset(assetsDest, "source-sans-3.css");
        copyAsset(assetsDest, "notoserif.css");
        copyAsset(assetsDest, "open-sans.css");
        copyAsset(assetsDest, "literata.css");
        copyAsset(assetsDest, "atkinson-hyperlegible-next.css");
        copyAsset(assetsDest, "ibm-plex-sans.css");
        copyAsset(assetsDest, "fonts/JetBrainsMono/JetBrainsMono-Regular.woff2");
        copyAsset(assetsDest, "fonts/JetBrainsMono/JetBrainsMono-Bold.woff2");
        copyAsset(assetsDest, "fonts/JetBrainsMono/JetBrainsMono-Italic.woff2");
        copyAsset(assetsDest, "fonts/Merriweather/Merriweather-Variable.woff2");
        copyAsset(assetsDest, "fonts/Merriweather/Merriweather-Italic-Variable.woff2");
        copyAsset(assetsDest, "fonts/SourceSans3/SourceSans3-Variable.woff2");
        copyAsset(assetsDest, "fonts/SourceSans3/SourceSans3-Italic-Variable.woff2");
        copyAsset(assetsDest, "fonts/NotoSerif/NotoSerif-Variable.woff2");
        copyAsset(assetsDest, "fonts/NotoSerif/NotoSerif-Italic-Variable.woff2");
        copyAsset(assetsDest, "fonts/OpenSans/OpenSans-Variable.woff2");
        copyAsset(assetsDest, "fonts/OpenSans/OpenSans-Italic-Variable.woff2");
        copyAsset(assetsDest, "fonts/Literata/Literata-Variable.woff2");
        copyAsset(assetsDest, "fonts/Literata/Literata-Italic-Variable.woff2");
        copyAsset(assetsDest, "fonts/AtkinsonHyperlegibleNext/AtkinsonHyperlegibleNext-wght.woff2");
        copyAsset(assetsDest, "fonts/AtkinsonHyperlegibleNext/AtkinsonHyperlegibleNext-Italic-wght.woff2");
        copyAsset(assetsDest, "fonts/IBMPlexSansVariable/IBM Plex Sans Var-Roman.woff2");
        copyAsset(assetsDest, "fonts/IBMPlexSansVariable/IBM Plex Sans Var-Italic.woff2");
        copyAsset(assetsDest, "styles.css");
        copyAsset(assetsDest, "lunr.min.js");
        copyAsset(assetsDest, "search.js");
        if (uiSyntaxHighlightingEnabled()) {
            copyAsset(assetsDest, "prism-overrides.css");
            copyPrismAssets(assetsDest);
            copyPrismCustomComponents(assetsDest);
        }
        siteLogo = copyConfiguredLogoAsset(assetsDest);
    }

    private void copyAsset(Path assetsDest, String relativePath) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/site-assets/" + relativePath)) {
            if (stream == null) {
                throw new IOException("Missing bundled site asset: " + relativePath);
            }
            Path target = assetsDest.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyPrismAssets(Path assetsDest) throws IOException {
        for (String relativePath : PRISM_BUNDLED_ASSETS) {
            copyAsset(assetsDest, relativePath);
        }
    }

    private void copyPrismCustomComponents(Path assetsDest) throws IOException {
        if (config.ui() == null || config.ui().prismCustomComponents().isEmpty()) {
            return;
        }
        Path customDest = assetsDest.resolve("prism").resolve("custom");
        Files.createDirectories(customDest);
        for (String rawPath : config.ui().prismCustomComponents()) {
            Path source = Path.of(rawPath).toAbsolutePath().normalize();
            String fileName = source.getFileName().toString();
            Files.copy(source, customDest.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String copyConfiguredLogoAsset(Path assetsDest) throws IOException {
        String logo = config.site().logo();
        if (logo == null || logo.isBlank()) {
            return null;
        }
        if (isExternalLogoReference(logo)) {
            return logo;
        }

        Path sourceLogoPath = resolveLocalLogoPath(logo);
        String extension = fileExtension(sourceLogoPath.getFileName().toString());
        String fileName = "site-logo" + extension;
        Path targetLogoPath = assetsDest.resolve(fileName);
        Files.copy(sourceLogoPath, targetLogoPath, StandardCopyOption.REPLACE_EXISTING);
        return "/site-assets/" + fileName;
    }

    private String resolveConfiguredLogoReference() {
        String logo = config.site().logo();
        if (logo == null || logo.isBlank()) {
            return null;
        }
        if (isExternalLogoReference(logo)) {
            return logo;
        }
        Path sourceLogoPath = resolveLocalLogoPath(logo);
        String extension = fileExtension(sourceLogoPath.getFileName().toString());
        return "/site-assets/site-logo" + extension;
    }

    private boolean isExternalLogoReference(String logo) {
        try {
            URI uri = new URI(logo);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String normalized = scheme.toLowerCase();
            return normalized.equals("http") || normalized.equals("https") || normalized.equals("data");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private Path resolveLocalLogoPath(String logo) {
        try {
            URI uri = new URI(logo);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Path.of(uri).toAbsolutePath().normalize();
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            // Fall through and treat as regular path string.
        }
        return Path.of(logo).toAbsolutePath().normalize();
    }

    private String fileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot);
    }

    private record SearchIndexEntry(
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

    private String uiSearchLanguageMode() {
        if (config.ui() == null || config.ui().searchLanguageMode() == null) {
            return "multilingual_safe";
        }
        return config.ui().searchLanguageMode().configValue();
    }

    private boolean uiSyntaxHighlightingEnabled() {
        if (config.ui() == null || config.ui().syntaxHighlightingMode() == null) {
            return true;
        }
        return config.ui().syntaxHighlightingMode().isEnabled();
    }

    private List<String> uiPrismCustomComponentUrls() {
        if (!uiSyntaxHighlightingEnabled() || config.ui() == null || config.ui().prismCustomComponents().isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (String rawPath : config.ui().prismCustomComponents()) {
            Path source = Path.of(rawPath);
            String fileName = source.getFileName().toString();
            urls.add("/site-assets/prism/custom/" + fileName);
        }
        return List.copyOf(urls);
    }

    // Template rendering

    private String renderTemplate(String name, Map<String, Object> model) {
        try {
            Template template = freemarker.getTemplate(name);
            try (StringWriter writer = new StringWriter()) {
                template.process(model, writer);
                return writer.toString();
            }
        } catch (IOException | TemplateException e) {
            throw new RuntimeException("Failed to render template: " + name, e);
        }
    }

    private void writeOutput(Path relativePath, String content) throws IOException {
        Path target = outputRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        writeIfChanged(target, content);
    }

    private void writeIfChanged(Path target, String content) throws IOException {
        if (Files.exists(target) && Files.isRegularFile(target)) {
            String current = Files.readString(target, StandardCharsets.UTF_8);
            if (current.equals(content)) {
                return;
            }
        }
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    // Model converters

    private Map<String, Object> catalogToModel() {
        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> components = new ArrayList<>();
        for (DocComponent c : catalog.components()) {
            components.add(componentToModel(c));
        }
        model.put("components", components);
        return model;
    }

    private Map<String, Object> componentToModel(DocComponent component) {
        Map<String, Object> model = new HashMap<>();
        model.put("id", component.id());
        model.put("displayName", component.displayName());
        model.put("defaultVersion", component.defaultVersion());

        List<Map<String, Object>> versions = new ArrayList<>();
        for (ComponentVersion v : component.versions()) {
            versions.add(versionToModel(v));
        }
        model.put("versions", versions);
        return model;
    }

    private Map<String, Object> versionToModel(ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        model.put("version", version.version());
        model.put("displayVersion", version.displayVersion());
        model.put("branchName", version.branchName());
        return model;
    }

    private Map<String, Object> pageToModel(DocPage page) {
        Map<String, Object> model = new HashMap<>();
        model.put("componentId", page.componentId());
        model.put("version", page.version());
        model.put("sourcePath", page.sourcePath());
        model.put("pageId", page.pageId());
        model.put("title", page.title());
        model.put("navTitle", page.navTitle());
        model.put("route", page.route());
        model.put("html", page.html());
        return model;
    }

    private Map<String, Object> navToModel(String componentId, ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        Map<String, String> routeBySourcePath = new HashMap<>();
        for (DocPage page : version.pages()) {
            routeBySourcePath.put(page.sourcePath(), page.route());
        }
        model.put("items", navItemsToModel(componentId, version, version.navigation().items(), routeBySourcePath));
        return model;
    }

    private Map<String, Object> singlePageNavToModel(String baseRoute, ComponentVersion version) {
        Map<String, Object> model = new HashMap<>();
        model.put("items", singlePageNavItemsToModel(baseRoute, version.navigation().items()));
        model.put("singlePage", true);
        return model;
    }

    private List<Map<String, Object>> navItemsToModel(String componentId, ComponentVersion version,
                                                      List<guru.interlis.thoth.biblios.nav.NavItem> items,
                                                      Map<String, String> routeBySourcePath) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (guru.interlis.thoth.biblios.nav.NavItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", item.title());
            m.put("page", item.page());
            if (item.page() != null) {
                String route = routeBySourcePath.get(item.page());
                if (route == null) {
                    route = navFallbackRoute(componentId, version, item.page());
                }
                m.put("route", route);
            }
            if (item.children() != null && !item.children().isEmpty()) {
                m.put("children", navItemsToModel(componentId, version, item.children(), routeBySourcePath));
            }
            m.put("group", item.isGroup());
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> singlePageNavItemsToModel(String baseRoute, List<guru.interlis.thoth.biblios.nav.NavItem> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (guru.interlis.thoth.biblios.nav.NavItem item : items) {
            Map<String, Object> m = new HashMap<>();
            String displayTitle = item.title();
            String chapterTitle = item.rawTitle() != null && !item.rawTitle().isBlank()
                ? item.rawTitle()
                : displayTitle;
            m.put("title", displayTitle);
            m.put("group", item.isGroup());

            if (item.page() != null && !item.page().isBlank()) {
                String chapterId = normalizeChapterId(item.page());
                m.put("page", chapterId);
                m.put("chapter", true);
                m.put("chapterId", chapterId);
                m.put("chapterTitle", chapterTitle);
                m.put("route", baseRoute + "#" + chapterId);
            } else {
                m.put("chapter", false);
            }

            if (item.children() != null && !item.children().isEmpty()) {
                m.put("children", singlePageNavItemsToModel(baseRoute, item.children()));
            }
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> breadcrumbsToModel(List<DocPage.Breadcrumb> breadcrumbs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocPage.Breadcrumb crumb : breadcrumbs) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", crumb.title());
            m.put("route", crumb.route());
            result.add(m);
        }
        return result;
    }

    private String singlePageInitialChapterId(guru.interlis.thoth.biblios.nav.NavTree nav) {
        if (nav == null || nav.items() == null) {
            return "";
        }
        return firstChapterId(nav.items());
    }

    private String firstChapterId(List<guru.interlis.thoth.biblios.nav.NavItem> items) {
        for (guru.interlis.thoth.biblios.nav.NavItem item : items) {
            if (item.page() != null && !item.page().isBlank()) {
                return normalizeChapterId(item.page());
            }
            if (item.children() != null && !item.children().isEmpty()) {
                String nested = firstChapterId(item.children());
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }
        return "";
    }

    private String normalizeChapterId(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private List<Map<String, Object>> buildDocSwitcher() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocComponent c : catalog.components()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.id());
            m.put("displayName", c.displayName());
            m.put("defaultVersion", c.defaultVersion());
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> buildVersionSwitcher(DocComponent component) {
        return buildVersionSwitcher(component, null);
    }

    private List<Map<String, Object>> buildVersionSwitcher(DocComponent component, String currentPageSourcePath) {
        List<Map<String, Object>> result = new ArrayList<>();
        VersionSwitchMode mode = config.ui() != null ? config.ui().versionSwitchMode() : VersionSwitchMode.START_PAGE;
        for (ComponentVersion v : component.versions()) {
            Map<String, Object> m = new HashMap<>();
            m.put("version", v.version());
            m.put("displayVersion", v.displayVersion());

            // Equivalent page mode: map source-path across versions, fallback to version root.
            if (mode == VersionSwitchMode.EQUIVALENT_PAGE && currentPageSourcePath != null) {
                var targetPage = v.findPageBySourcePath(currentPageSourcePath);
                if (targetPage != null) {
                    m.put("route", targetPage.route());
                } else {
                    m.put("route", versionRootRoute(component.id(), v.version()));
                }
            } else {
                m.put("route", versionRootRoute(component.id(), v.version()));
            }

            result.add(m);
        }
        return result;
    }

    private String versionRootRoute(String componentId, String version) {
        return "/" + componentId + "/" + version + "/";
    }

    private String navFallbackRoute(String componentId, ComponentVersion version, String sourcePath) {
        String normalizedPath = sourcePath.replace('\\', '/');
        if (normalizedPath.endsWith(".adoc")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 5);
        }
        if (normalizedPath.equals(version.startPage().replace('\\', '/').replaceFirst("\\.adoc$", ""))) {
            return versionRootRoute(componentId, version.version());
        }
        return versionRootRoute(componentId, version.version()) + normalizedPath + "/";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void deleteDirectory(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                  .forEach(path -> {
                      try {
                          Files.delete(path);
                      } catch (IOException e) {
                          System.err.println("[warn] Failed to delete: " + path);
                      }
                  });
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
