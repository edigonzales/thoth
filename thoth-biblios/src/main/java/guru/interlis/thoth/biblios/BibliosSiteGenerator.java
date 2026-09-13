package guru.interlis.thoth.biblios;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import guru.interlis.thoth.core.InterlisLabHtmlSupport;
import guru.interlis.thoth.core.InterlisLabMacroProcessor;
import guru.interlis.thoth.biblios.catalog.*;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.RenderMode;
import guru.interlis.thoth.biblios.config.VersionSwitchMode;
import guru.interlis.thoth.biblios.nav.NavigationText;
import guru.interlis.thoth.biblios.publication.PublicationCatalogWriter;
import guru.interlis.thoth.biblios.publication.PublicationPackageWriter;
import guru.interlis.thoth.biblios.view.SiteViewModelFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Generates the HTML site from a SiteCatalog.
 */
public final class BibliosSiteGenerator implements AutoCloseable {
    private static final String TEMPLATE_OVERRIDES_DIR_NAME = "templates";
    private static final String ASSET_OVERRIDES_DIR_NAME = "assets";
    private static final int SEARCH_MAX_SECTION_LEVEL = 4;
    private static final java.util.regex.Pattern IMG_SRC_PATTERN =
        java.util.regex.Pattern.compile("<img\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final Set<String> DEFAULT_EXCLUDED_DIRECTORIES = Set.of(
        ".git",
        ".hg",
        ".svn",
        ".idea",
        ".vscode",
        "node_modules",
        "build",
        "target",
        ".gradle"
    );
    private static final List<String> PRISM_BUNDLED_ASSETS = List.of(
        "prism-conum-bridge.js",
        "prism/prism.css",
        "prism/prism.js",
        "prism/components/prism-markup.min.js",
        "prism/components/prism-clike.min.js",
        "prism/components/prism-groovy.min.js",
        "prism/components/prism-gradle.min.js",
        "prism/components/prism-javascript.min.js",
        "prism/components/prism-css.min.js",
        "prism/components/prism-ini.min.js",
        "prism/components/prism-interlis.js",
        "prism/components/prism-ilimap.js",
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
    private final Path configDirectory;
    private final Path templateOverrideRoot;
    private final Path assetOverrideRoot;
    private final Configuration freemarker;
    private final SiteViewModelFactory view;
    private final PublicationPackageWriter packageWriter;
    private String siteLogo;

    public BibliosSiteGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot) {
        this(config, catalog, outputRoot, null);
    }

    public BibliosSiteGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot, Path configPath) {
        this(config, catalog, outputRoot, configPath, null);
    }

    public BibliosSiteGenerator(BibliosConfig config, SiteCatalog catalog, Path outputRoot, Path configPath,
                                PublicationPackageWriter packageWriter) {
        this.config = config;
        this.catalog = catalog;
        this.outputRoot = outputRoot;
        this.packageWriter = packageWriter;
        this.configDirectory = resolveConfigDirectory(configPath);
        this.templateOverrideRoot = configDirectory != null ? configDirectory.resolve(TEMPLATE_OVERRIDES_DIR_NAME) : null;
        this.assetOverrideRoot = configDirectory != null ? configDirectory.resolve(ASSET_OVERRIDES_DIR_NAME) : null;
        this.siteLogo = resolveConfiguredLogoReference();
        this.view = new SiteViewModelFactory(config, catalog);

        // Initialize FreeMarker
        freemarker = new Configuration(Configuration.VERSION_2_3_34);
        freemarker.setTemplateLoader(createTemplateLoader());
        freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarker.setLogTemplateExceptions(false);
        freemarker.setWrapUncheckedExceptions(true);
    }

    private Path resolveConfigDirectory(Path configPath) {
        if (configPath == null) {
            return null;
        }
        Path normalized = configPath.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) {
            return normalized;
        }
        Path parent = normalized.getParent();
        return parent != null ? parent : Path.of(".").toAbsolutePath().normalize();
    }

    private TemplateLoader createTemplateLoader() {
        TemplateLoader bundled = new ClassTemplateLoader(getClass(), "/templates");
        if (templateOverrideRoot == null || !Files.isDirectory(templateOverrideRoot)) {
            return bundled;
        }
        try {
            TemplateLoader overrides = new FileTemplateLoader(templateOverrideRoot.toFile());
            return new MultiTemplateLoader(new TemplateLoader[] {overrides, bundled});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize template override directory " + templateOverrideRoot, e);
        }
    }

    /**
     * Generate the complete site.
     */
    public void generate() throws IOException {
        generate(true, true, Set.of(), false, Set.of());
    }

    /**
     * Generate the complete site with optional PDF generation control.
     */
    public void generate(boolean generatePdf, Set<String> selectedPdfVersions) throws IOException {
        generate(true, generatePdf, selectedPdfVersions, false, Set.of());
    }

    /**
     * Generate the complete site with optional PDF and DOCX generation control.
     */
    public void generate(boolean generatePdf, Set<String> selectedPdfVersions,
                         boolean generateDocx, Set<String> selectedDocxVersions) throws IOException {
        generate(true, generatePdf, selectedPdfVersions, generateDocx, selectedDocxVersions);
    }

    /**
     * Generate selected outputs with independent HTML/PDF/DOCX toggles.
     */
    public void generate(boolean generateHtml,
                         boolean generatePdf,
                         Set<String> selectedPdfVersions,
                         boolean generateDocx,
                         Set<String> selectedDocxVersions) throws IOException {
        System.out.println("[info] Generating outputs to: " + outputRoot);

        // Clean output if configured
        if (config.output().clean() && Files.exists(outputRoot)) {
            deleteDirectory(outputRoot);
        }
        Files.createDirectories(outputRoot);

        if (generateHtml) {
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
        }

        if (generatePdf && (isGlobalPdfEnabled() || hasSourceLevelPdfEnablement())) {
            new BibliosPdfGenerator(config, catalog, outputRoot).generate(selectedPdfVersions);
        }
        if (generateDocx && (isGlobalDocxEnabled() || hasSourceLevelDocxEnablement())) {
            new BibliosDocxGenerator(config, catalog, outputRoot).generate(selectedDocxVersions);
        }

        System.out.println("[info] Site generation complete.");

        if (packageWriter != null) {
            packageWriter.writeCatalog(PublicationCatalogWriter.write(config, catalog, siteLogo, view));
        }
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
        Path outputFile = Path.of("index.html");
        Map<String, Object> model = createCommonModel(outputFile);
        model.put("siteDescription", config.site().url());
        model.put("catalog", view.catalogModel());
        model.put("docSwitcher", view.docSwitcher());

        String html = renderTemplate("index.ftl", model);
        writeOutput(outputFile, html);
    }

    private void generateSearchPage() throws IOException {
        Path outputFile = Path.of("search/index.html");
        Map<String, Object> model = createCommonModel(outputFile);
        model.put("docSwitcher", view.docSwitcher());
        model.put("searchIndexScriptHref",
            SiteViewModelFactory.routeHref(SiteViewModelFactory.basePathForOutput(outputFile), "/search-index.js"));

        String html = renderTemplate("search.ftl", model);
        writeOutput(outputFile, html);
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
        Path outputFile = Path.of(component.id(), "index.html");
        ComponentVersion defaultVersion = component.getVersion(component.defaultVersion());
        if (defaultVersion == null && !component.versions().isEmpty()) {
            defaultVersion = component.versions().get(0);
        }

        Map<String, Object> model = createCommonModel(outputFile);
        model.put("component", view.componentModel(component));
        model.put("currentVersion", defaultVersion != null ? view.versionModel(defaultVersion) : Map.of());
        model.put("currentComponentId", component.id());
        model.put("currentVersionStr", defaultVersion != null ? defaultVersion.version() : component.defaultVersion());
        model.put("navigation", null);
        model.put("docSwitcher", view.docSwitcher());
        model.put("versionSwitcher", view.versionSwitcher(component));

        String html = renderTemplate("component.ftl", model);
        writeOutput(outputFile, html);
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
        Path outputFile = outputRoot.relativize(pageDir.resolve("index.html"));

        boolean singlePageMode = version.renderMode() == RenderMode.SINGLE_PAGE;

        String contentHtml = rewriteContentLinks(page, version, outputFile);
        contentHtml = prepareInterlisLabMarkup(contentHtml, outputFile);
        if (packageWriter != null) {
            packageWriter.recordPage(page.route(), component.id(), contentHtml);
        }

        Map<String, Object> model = createCommonModel(outputFile);
        model.put("page", view.pageModel(page, contentHtml));
        model.put("currentComponentId", component.id());
        model.put("currentVersionStr", version.version());
        model.put("currentVersion", view.versionModel(version));
        model.put("currentPagePath", page.sourcePath());
        model.put(
            "navigation",
            version.navigation() != null
                ? (singlePageMode
                    ? view.singlePageNavigationModel(page.route(), version)
                    : view.navigationModel(component.id(), version))
                : null
        );
        model.put("docSwitcher", view.docSwitcher());
        model.put("versionSwitcher", view.versionSwitcher(component, page.sourcePath()));
        model.put("breadcrumbs", view.breadcrumbsModel(page.breadcrumbs()));
        model.put("singlePageMode", singlePageMode);
        model.put("chapterBreadcrumbEnabled", singlePageMode);
        model.put("initialChapterId", singlePageMode ? view.singlePageInitialChapterId(version.navigation()) : "");
        model.put("editUrl", page.editUrl());
        model.put("sourceUrl", page.sourceUrl());
        model.put("showEditLink", config.ui() != null && config.ui().showEditLink());
        model.put("showSourceLink", config.ui() != null && config.ui().showSourceLink());
        model.put("interlisLabEnabled", page.usesInterlisLab() || InterlisLabHtmlSupport.hasInterlisLab(contentHtml));

        if (page.prev() != null) {
            model.put("prevPage", view.pageModel(page.prev()));
        }
        if (page.next() != null) {
            model.put("nextPage", view.pageModel(page.next()));
        }

        String html = renderTemplate("page.ftl", model);
        writeOutput(outputFile, html);
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
            for (String src : extractInterlisLabSources(page.html())) {
                if (!isLocalRelativeAssetReference(src)) {
                    continue;
                }

                Path sourceAsset = resolveSourceAssetPath(page, src);
                if (sourceAsset == null || !Files.exists(sourceAsset) || !Files.isRegularFile(sourceAsset)) {
                    System.err.println("[warn] Referenced INTERLIS Lab lesson not found: " + src + " (source page: " + page.sourcePath() + ")");
                    continue;
                }

                Path targetAsset = pageDir.resolve(src).normalize();
                if (!targetAsset.startsWith(versionRoot)) {
                    System.err.println("[warn] Skipping unsafe INTERLIS Lab lesson target path: " + src + " (source page: " + page.sourcePath() + ")");
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

    private static List<String> extractInterlisLabSources(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<String> sources = new ArrayList<>();
        Document document = Jsoup.parseBodyFragment(html);
        for (Element lab : document.select("interlis-lab[src]")) {
            String src = lab.attr("src").trim();
            if (!src.isEmpty()) {
                sources.add(src);
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

    private String rewriteContentLinks(DocPage page, ComponentVersion version, Path currentOutputFile) {
        if (page == null || page.html() == null || page.html().isBlank()) {
            return page != null ? page.html() : "";
        }

        Document document = Jsoup.parseBodyFragment(page.html());
        Map<String, DocPage> pagesBySourcePath = new HashMap<>();
        for (DocPage candidate : version.pages()) {
            pagesBySourcePath.put(candidate.sourcePath(), candidate);
        }

        boolean changed = false;
        for (Element anchor : document.select("a[href]")) {
            String href = anchor.attr("href").trim();
            String rewritten = rewriteContentHref(href, page, version, pagesBySourcePath, currentOutputFile);
            if (!href.equals(rewritten)) {
                anchor.attr("href", rewritten);
                changed = true;
            }
        }

        return changed ? document.body().html() : page.html();
    }

    private String prepareInterlisLabMarkup(String html, Path outputFile) {
        if (html == null || html.isBlank() || !InterlisLabHtmlSupport.hasInterlisLab(html)) {
            return html != null ? html : "";
        }

        String ili2cJarUrl = SiteViewModelFactory.assetHref(SiteViewModelFactory.basePathForOutput(outputFile), "site-assets/interlis-lab/ili2c.jar");
        Document document = Jsoup.parseBodyFragment(html);
        boolean changed = false;
        for (Element lab : document.select("interlis-lab")) {
            String current = lab.attr("ili2c-jar-url").trim();
            if (current.isEmpty() || InterlisLabMacroProcessor.ILI2C_JAR_URL_PLACEHOLDER.equals(current)) {
                lab.attr("ili2c-jar-url", ili2cJarUrl);
                changed = true;
            }
        }

        return changed ? document.body().html() : html;
    }

    private String rewriteContentHref(String href, DocPage currentPage, ComponentVersion version,
                                      Map<String, DocPage> pagesBySourcePath, Path currentOutputFile) {
        if (href == null || href.isBlank() || href.startsWith("#") || SiteViewModelFactory.isExternalHref(href)) {
            return href;
        }

        HrefParts parts = splitHref(href);
        if (parts.path().isBlank()) {
            return href;
        }

        if (parts.path().startsWith("/")) {
            if (!isManagedAbsolutePath(parts.path())) {
                return href;
            }
            return relativeManagedHref(currentOutputFile, parts.path()) + parts.suffix();
        }

        String targetRoute = resolveRelativeContentRoute(currentPage, version, pagesBySourcePath, parts.path());
        if (targetRoute == null) {
            return href;
        }
        return relativeManagedHref(currentOutputFile, targetRoute) + parts.suffix();
    }

    private String resolveRelativeContentRoute(DocPage currentPage, ComponentVersion version,
                                               Map<String, DocPage> pagesBySourcePath, String rawPath) {
        String normalizedRawPath = rawPath.replace('\\', '/');
        boolean directoryLike = normalizedRawPath.endsWith("/");

        final Path relativeTargetPath;
        try {
            relativeTargetPath = Path.of(normalizedRawPath).normalize();
        } catch (Exception ignored) {
            return null;
        }
        if (relativeTargetPath.isAbsolute()) {
            return null;
        }

        Path currentSourcePath = Path.of(currentPage.sourcePath().replace('\\', '/'));
        Path currentSourceDir = currentSourcePath.getParent() != null ? currentSourcePath.getParent() : Path.of("");
        Path resolvedTargetPath = currentSourceDir.resolve(relativeTargetPath).normalize();
        if (resolvedTargetPath.toString().startsWith("..")) {
            return null;
        }

        for (String candidateSourcePath : candidateSourcePaths(resolvedTargetPath, directoryLike)) {
            DocPage targetPage = pagesBySourcePath.get(candidateSourcePath);
            if (targetPage != null) {
                return targetPage.route();
            }
        }

        if (directoryLike || rawPath.indexOf('.') < 0) {
            String startPageFileName = Path.of(version.startPage()).getFileName() != null
                ? Path.of(version.startPage()).getFileName().toString()
                : version.startPage();
            String directoryPath = normalizeSourcePath(resolvedTargetPath);
            String candidateStartPage = directoryPath.isBlank()
                ? startPageFileName
                : directoryPath + "/" + startPageFileName;
            DocPage startPage = pagesBySourcePath.get(candidateStartPage);
            if (startPage != null) {
                return startPage.route();
            }
        }

        return null;
    }

    private List<String> candidateSourcePaths(Path resolvedTargetPath, boolean directoryLike) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalizedPath = normalizeSourcePath(resolvedTargetPath);
        if (normalizedPath.isBlank()) {
            candidates.add("index.adoc");
            return List.copyOf(candidates);
        }

        if (directoryLike) {
            candidates.add(normalizedPath + "/index.adoc");
            return List.copyOf(candidates);
        }

        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);
        if (lowerPath.endsWith(".adoc")) {
            candidates.add(normalizedPath);
        } else if (lowerPath.endsWith(".html")) {
            candidates.add(normalizedPath.substring(0, normalizedPath.length() - 5) + ".adoc");
        } else if (lowerPath.endsWith(".htm")) {
            candidates.add(normalizedPath.substring(0, normalizedPath.length() - 4) + ".adoc");
        } else if (!resolvedTargetPath.getFileName().toString().contains(".")) {
            candidates.add(normalizedPath + ".adoc");
            candidates.add(normalizedPath + "/index.adoc");
        }
        return List.copyOf(candidates);
    }

    private String normalizeSourcePath(Path path) {
        if (path == null) {
            return "";
        }
        String normalized = path.normalize().toString().replace('\\', '/');
        return ".".equals(normalized) ? "" : normalized;
    }

    private String relativeManagedHref(Path currentOutputFile, String managedTargetPath) {
        Path currentDir = currentOutputFile != null && currentOutputFile.getParent() != null
            ? currentOutputFile.getParent()
            : Path.of("");
        Path targetPath = outputPathForManagedTarget(managedTargetPath);
        Path relativePath = currentDir.relativize(targetPath);
        String normalizedRelativePath = normalizeSourcePath(relativePath);
        if (isManagedFilePath(managedTargetPath)) {
            return normalizedRelativePath.isBlank() ? "." : normalizedRelativePath;
        }
        return normalizedRelativePath.isBlank() ? "./" : normalizedRelativePath + "/";
    }

    private Path outputPathForManagedTarget(String managedTargetPath) {
        if (managedTargetPath == null || managedTargetPath.isBlank() || "/".equals(managedTargetPath)) {
            return Path.of("");
        }
        if ("/search-index.json".equals(managedTargetPath)) {
            return Path.of("search-index.json");
        }
        if ("/search-index.js".equals(managedTargetPath)) {
            return Path.of("search-index.js");
        }
        String normalized = managedTargetPath.startsWith("/") ? managedTargetPath.substring(1) : managedTargetPath;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            return Path.of("");
        }
        return Path.of(normalized);
    }

    private boolean isManagedFilePath(String managedTargetPath) {
        return "/search-index.json".equals(managedTargetPath) || "/search-index.js".equals(managedTargetPath);
    }

    private boolean isManagedAbsolutePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if ("/".equals(path) || "/search/".equals(path) || "/search-index.json".equals(path) || "/search-index.js".equals(path)) {
            return true;
        }
        for (DocComponent component : catalog.components()) {
            String prefix = "/" + component.id() + "/";
            if (path.equals("/" + component.id() + "/") || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private HrefParts splitHref(String href) {
        int queryIndex = href.indexOf('?');
        int hashIndex = href.indexOf('#');
        int suffixIndex = -1;
        if (queryIndex >= 0 && hashIndex >= 0) {
            suffixIndex = Math.min(queryIndex, hashIndex);
        } else if (queryIndex >= 0) {
            suffixIndex = queryIndex;
        } else if (hashIndex >= 0) {
            suffixIndex = hashIndex;
        }
        if (suffixIndex < 0) {
            return new HrefParts(href, "");
        }
        return new HrefParts(href.substring(0, suffixIndex), href.substring(suffixIndex));
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

        String json = buildSearchIndexJson(entries);
        writeOutput(Path.of("search-index.json"), json);
        writeOutput(Path.of("search-index.js"), "window.__BIBLIOS_SEARCH_INDEX__ = " + json + ";\n");
    }

    private String buildSearchIndexJson(List<SearchIndexEntry> entries) {
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
        return json.toString();
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
            String chapterId = view.normalizeChapterId(chapter.id());
            if (chapterId.isBlank() && heading != null) {
                chapterId = view.normalizeChapterId(heading.id());
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
        copyAsset(assetsDest, "interlis-lab/interlis-lab.js");
        copyAsset(assetsDest, "interlis-lab/ili2c.jar");
        if (view.syntaxHighlightingEnabled()) {
            copyAsset(assetsDest, "prism-overrides.css");
            copyPrismAssets(assetsDest);
            copyPrismCustomComponents(assetsDest);
        }
        copyAssetOverrides(assetsDest);
        siteLogo = copyConfiguredLogoAsset(assetsDest);
    }

    private Map<String, Object> createCommonModel(Path outputFile) {
        return view.commonModel(SiteViewModelFactory.basePathForOutput(outputFile), siteLogo);
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

    private void copyAssetOverrides(Path assetsDest) throws IOException {
        if (assetOverrideRoot == null || !Files.isDirectory(assetOverrideRoot)) {
            return;
        }
        Path normalizedAssetsDest = assetsDest.toAbsolutePath().normalize();
        Files.walkFileTree(assetOverrideRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (assetOverrideRoot.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                if (shouldSkipAssetOverrideDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink()) {
                    return FileVisitResult.CONTINUE;
                }
                Path relativePath = assetOverrideRoot.relativize(file).normalize();
                if (!isSafeRelativePath(relativePath) || shouldSkipAssetOverrideFile(relativePath)) {
                    return FileVisitResult.CONTINUE;
                }
                Path target = assetsDest.resolve(relativePath).toAbsolutePath().normalize();
                if (!target.startsWith(normalizedAssetsDest)) {
                    throw new IOException("Asset override target escapes site-assets: " + relativePath);
                }
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean shouldSkipAssetOverrideDirectory(Path dir) {
        Path name = dir.getFileName();
        return name != null && DEFAULT_EXCLUDED_DIRECTORIES.contains(name.toString());
    }

    private boolean shouldSkipAssetOverrideFile(Path relativePath) {
        Path fileName = relativePath.getFileName();
        return fileName != null && ".DS_Store".equals(fileName.toString());
    }

    private boolean isSafeRelativePath(Path relativePath) {
        if (relativePath == null || relativePath.isAbsolute()) {
            return false;
        }
        for (Path part : relativePath) {
            if ("..".equals(part.toString())) {
                return false;
            }
        }
        return true;
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

    private record HrefParts(String path, String suffix) {
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
