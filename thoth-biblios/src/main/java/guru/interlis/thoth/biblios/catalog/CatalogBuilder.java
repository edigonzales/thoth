package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BranchConfig;
import guru.interlis.thoth.biblios.config.NavigationConfig;
import guru.interlis.thoth.biblios.config.RenderMode;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.biblios.config.UiSection;
import guru.interlis.thoth.biblios.git.GitSourceResolver;
import guru.interlis.thoth.biblios.nav.NavParser;
import guru.interlis.thoth.biblios.nav.NavItem;
import guru.interlis.thoth.biblios.nav.NavTree;
import guru.interlis.thoth.biblios.render.AsciidoctorRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a SiteCatalog from configuration.
 * Orchestrates Git fetching, navigation parsing, and page discovery.
 */
public final class CatalogBuilder implements AutoCloseable {

    private final BibliosConfig config;
    private final Path workRoot;
    private final NavParser navParser = new NavParser();
    private final List<GitSourceResolver> resolvers = new ArrayList<>();

    public CatalogBuilder(BibliosConfig config, Path workRoot) {
        this.config = config;
        this.workRoot = workRoot;
    }

    /**
     * Build the complete site catalog.
     */
    public SiteCatalog build() throws IOException {
        List<DocComponent> components = new ArrayList<>();

        for (SourceConfig source : config.content().sources()) {
            System.out.println("[info] Resolving source: " + source.id() + " (" + source.url() + ")");
            DocComponent component = buildComponent(source, config.ui());
            components.add(component);
        }

        return new SiteCatalog(components);
    }

    private DocComponent buildComponent(SourceConfig source, UiSection ui) throws IOException {
        GitSourceResolver resolver = new GitSourceResolver(workRoot);
        resolvers.add(resolver);

        resolver.resolve(source.url(), source.id());

        List<ComponentVersion> versions = new ArrayList<>();

        for (BranchConfig branch : source.branches()) {
            if (!resolver.branchExists(branch.name())) {
                System.err.println("[warn] Branch '" + branch.name() + "' not found in " + source.id() + ", skipping");
                continue;
            }

            System.out.println("[info] Checking out " + source.id() + "/" + branch.name());
            resolver.checkout(branch.name());

            ComponentVersion version = buildVersion(source, branch, resolver.workTree(), ui);
            versions.add(version);
        }

        if (versions.isEmpty()) {
            throw new IOException("No valid versions found for source: " + source.id());
        }

        String configuredDefaultVersion = source.defaultVersion() != null
            ? source.defaultVersion()
            : versions.get(0).version();
        final String effectiveDefaultVersion;

        // Validate that defaultVersion references an existing version
        boolean defaultVersionExists = versions.stream().anyMatch(v -> v.version().equals(configuredDefaultVersion));
        if (!defaultVersionExists) {
            System.err.println("[warn] Default version '" + configuredDefaultVersion + "' not found for source: " + source.id());
            System.err.println("[warn] Available versions: " + versions.stream().map(ComponentVersion::version).collect(java.util.stream.Collectors.joining(", ")));
            System.err.println("[warn] Falling back to first available version: " + versions.get(0).version());
            effectiveDefaultVersion = versions.get(0).version();
        } else {
            effectiveDefaultVersion = configuredDefaultVersion;
        }

        return new DocComponent(source.id(), source.displayName(), effectiveDefaultVersion, versions);
    }

    private ComponentVersion buildVersion(SourceConfig source, BranchConfig branch, Path workTree, UiSection ui) throws IOException {
        Path docRoot = workTree.resolve(source.startPath());
        if (!Files.exists(docRoot)) {
            throw new IOException(
                "Documentation root not found: " + docRoot + "\n" +
                "Check the 'start_path' configuration for source: " + source.id()
            );
        }

        if (source.renderMode().isSinglePage()) {
            return buildSinglePageVersion(source, branch, docRoot, ui);
        }

        // Load navigation
        NavTree navigation = null;
        NavigationConfig navConfig = source.navigation();
        if (navConfig != null) {
            Path navFile = docRoot.resolve(navConfig.file());
            if (Files.exists(navFile)) {
                try {
                    navigation = navParser.parse(navFile);
                    System.out.println("[info] Loaded navigation from " + navFile);
                } catch (Exception e) {
                    System.err.println("[warn] Failed to load navigation from " + navFile + ": " + e.getMessage());
                    System.err.println("[warn] Continuing without navigation for this version.");
                }
            } else {
                System.err.println("[warn] Navigation file not found: " + navFile);
                System.err.println("[warn] Continuing without navigation for this version.");
            }
        }

        if (navigation == null) {
            navigation = new NavTree(List.of());
        }

        // Discover and build pages
        List<DocPage> pages = discoverAndBuildPages(source, branch, docRoot, navigation, ui);

        if (pages.isEmpty()) {
            System.err.println("[warn] No pages found for " + source.id() + "/" + branch.name());
            System.err.println("[warn] Check that .adoc files exist in: " + docRoot);
        }

        // Validate startPage exists
        String configuredStartPage = source.startPage() != null ? source.startPage() : "index.adoc";
        boolean startPageExists = pages.stream().anyMatch(p -> p.sourcePath().equals(configuredStartPage));
        if (!startPageExists) {
            System.err.println("[warn] Start page '" + configuredStartPage + "' not found for " + source.id() + "/" + branch.name());
            System.err.println("[warn] Available pages: " + pages.stream().map(DocPage::sourcePath).limit(10).collect(java.util.stream.Collectors.joining(", ")));
            if (!pages.isEmpty()) {
                System.err.println("[warn] Falling back to first available page: " + pages.get(0).sourcePath());
            }
        }

        return new ComponentVersion(
            source.id(),
            branch.name(),
            branch.displayVersion(),
            branch.name(),
            configuredStartPage,
            navigation,
            pages,
            RenderMode.SPLIT
        );
    }

    private ComponentVersion buildSinglePageVersion(SourceConfig source, BranchConfig branch, Path docRoot, UiSection ui) throws IOException {
        Path masterPath = docRoot.resolve(source.masterFile());
        if (!Files.exists(masterPath) || !Files.isRegularFile(masterPath)) {
            throw new IOException(
                "Master file not found for single_page source '" + source.id() + "': " + masterPath + "\n" +
                "Check the 'master_file' configuration and ensure the file exists under start_path."
            );
        }

        boolean contentToc = ui != null && ui.contentToc().isEnabled();
        int headingDepth = ui != null ? ui.sidebarTocDepth() : 2;
        String renderLanguage = config.site().defaultLanguage();

        AsciidoctorRenderer.RenderedDocument rendered;
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            rendered = renderer.renderDocument(
                masterPath,
                AsciidoctorRenderer.RenderOptions.singlePage(contentToc, headingDepth, renderLanguage)
            );
        }

        String route = "/" + source.id() + "/" + branch.name() + "/";
        String sourcePath = source.masterFile();
        String pageId = pagePathToId(sourcePath);
        String html = rendered.html() != null ? rendered.html() : "";
        String title = rendered.title() != null && !rendered.title().isBlank()
            ? rendered.title().trim()
            : source.displayName();

        NavTree navigation = new NavTree(
            mapHeadingsToNavItems(rendered.headings(), source.sidebarTocNumbers().isEnabled())
        );
        String initialChapterTitle = firstHeadingTitle(rendered.headings(), title);
        List<DocPage.Breadcrumb> breadcrumbs = List.of(
            new DocPage.Breadcrumb(source.displayName(), route),
            new DocPage.Breadcrumb(initialChapterTitle, null)
        );

        String editUrl = buildEditUrl(ui, source, branch, sourcePath);
        String sourceUrl = buildSourceUrl(ui, source, branch, sourcePath);

        DocPage page = new DocPage(
            source.id(),
            branch.name(),
            sourcePath,
            masterPath.toUri().toString(),
            pageId,
            title,
            title,
            route,
            html,
            breadcrumbs,
            null,
            null,
            editUrl,
            sourceUrl
        );

        return new ComponentVersion(
            source.id(),
            branch.name(),
            branch.displayVersion(),
            branch.name(),
            source.masterFile(),
            navigation,
            List.of(page),
            RenderMode.SINGLE_PAGE
        );
    }

    private List<DocPage> discoverAndBuildPages(SourceConfig source, BranchConfig branch,
                                                  Path docRoot, NavTree navigation, UiSection ui) throws IOException {
        List<DocPage> pages = new ArrayList<>();

        // Collect all .adoc files from the navigation tree
        List<String> pagePaths = collectPagePathsFromNav(navigation);

        // If no navigation, discover .adoc files recursively
        if (pagePaths.isEmpty()) {
            pagePaths = discoverAdocFiles(docRoot);
        }

        // Create renderer for this version
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            boolean contentToc = ui != null && ui.contentToc().isEnabled();
            String renderLanguage = config.site().defaultLanguage();
            for (String pagePath : pagePaths) {
                Path filePath = docRoot.resolve(pagePath);
                if (!Files.exists(filePath)) {
                    System.err.println("[warn] Page not found: " + filePath);
                    continue;
                }

                String pageId = pagePathToId(pagePath);
                String route = buildRoute(source.id(), branch.name(), pagePath, source.startPage());

                // Render AsciiDoc to HTML
                String html;
                String title;
                try {
                    AsciidoctorRenderer.RenderedDocument rendered = renderer.renderDocument(
                        filePath,
                        AsciidoctorRenderer.RenderOptions.split(contentToc, renderLanguage)
                    );
                    html = rendered.html();
                    if (html == null || html.isEmpty()) {
                        // Fallback to raw content if renderer returns empty
                        String content = Files.readString(filePath);
                        html = "<pre><code>" + escapeHtml(content) + "</code></pre>";
                        title = extractTitleFallback(pagePath, content);
                    } else {
                        if (rendered.title() != null && !rendered.title().isBlank()) {
                            title = rendered.title().trim();
                        } else {
                            title = extractTitle(filePath, html);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[warn] Failed to render " + filePath + ": " + e.getMessage());
                    // Fallback to raw content
                    String content = Files.readString(filePath);
                    html = "<pre><code>" + escapeHtml(content) + "</code></pre>";
                    title = extractTitleFallback(pagePath, content);
                }

                // Build edit/source URLs
                String editUrl = buildEditUrl(ui, source, branch, pagePath);
                String sourceUrl = buildSourceUrl(ui, source, branch, pagePath);

                DocPage page = new DocPage(
                    source.id(),
                    branch.name(),
                    pagePath,
                    filePath.toUri().toString(),
                    pageId,
                    title,
                    title,
                    route,
                    html,
                    buildBreadcrumbs(navigation, source.id(), branch.name(), source.startPage(), pagePath, title),
                    null, // prev/next will be set after all pages are collected
                    null,
                    editUrl,
                    sourceUrl
                );
                pages.add(page);
            }
        }

        // Set prev/next based on navigation order
        linkPages(pages, navigation);

        return pages;
    }

    private List<NavItem> mapHeadingsToNavItems(List<AsciidoctorRenderer.Heading> headings, boolean showNumbers) {
        List<NavItem> items = new ArrayList<>();
        if (headings == null) {
            return items;
        }
        for (AsciidoctorRenderer.Heading heading : headings) {
            if (heading == null || heading.title() == null || heading.title().isBlank()) {
                continue;
            }
            String chapterId = heading.id() != null ? heading.id().trim() : "";
            if (chapterId.isBlank()) {
                continue;
            }
            String rawTitle = heading.title().trim();
            String displayTitle = rawTitle;
            if (showNumbers && heading.sectionNumber() != null && !heading.sectionNumber().isBlank()) {
                displayTitle = heading.sectionNumber().trim() + " " + rawTitle;
            }
            items.add(new NavItem(
                displayTitle,
                chapterId,
                mapHeadingsToNavItems(heading.children(), showNumbers),
                rawTitle
            ));
        }
        return items;
    }

    private String firstHeadingTitle(List<AsciidoctorRenderer.Heading> headings, String fallback) {
        if (headings == null || headings.isEmpty()) {
            return fallback;
        }
        AsciidoctorRenderer.Heading first = headings.get(0);
        if (first == null || first.title() == null || first.title().isBlank()) {
            return fallback;
        }
        return first.title().trim();
    }

    private List<String> collectPagePathsFromNav(NavTree nav) {
        List<String> paths = new ArrayList<>();
        collectPagePathsFromItems(nav.items(), paths);
        return paths;
    }

    private void collectPagePathsFromItems(List<NavItem> items, List<String> paths) {
        for (NavItem item : items) {
            if (item.page() != null) {
                paths.add(item.page());
            }
            collectPagePathsFromItems(item.children(), paths);
        }
    }

    private List<String> discoverAdocFiles(Path root) throws IOException {
        List<String> paths = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".adoc"))
                  .forEach(p -> {
                      String relative = root.relativize(p).toString();
                      paths.add(relative);
                  });
        }
        paths.sort(String::compareTo);
        return paths;
    }

    private String buildRoute(String componentId, String version, String pagePath, String startPage) {
        // /<component>/<version>/<page>
        String base = "/" + componentId + "/" + version + "/";
        String normalizedStartPage = normalizePagePath(startPage != null ? startPage : "index.adoc");
        String normalizedStartPageWithoutExtension = normalizedStartPage.endsWith(".adoc")
            ? normalizedStartPage.substring(0, normalizedStartPage.length() - 5)
            : normalizedStartPage;
        String normalizedPagePath = normalizePagePath(pagePath);
        String pageWithoutExtension = normalizedPagePath.endsWith(".adoc")
            ? normalizedPagePath.substring(0, normalizedPagePath.length() - 5)
            : normalizedPagePath;

        if (pageWithoutExtension.equals(normalizedStartPageWithoutExtension)) {
            return base;
        }
        return base + pageWithoutExtension + "/";
    }

    private String normalizePagePath(String pagePath) {
        if (pagePath == null || pagePath.isBlank()) {
            return "index.adoc";
        }
        String normalized = pagePath.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String pagePathToId(String pagePath) {
        return pagePath.replace('/', '-').replace(".adoc", "");
    }

    private String extractTitle(Path filePath, String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return extractTitleFallback(filePath.getFileName().toString(), "");
        }
        // Try to extract from rendered HTML <h1> tag
        int h1Start = htmlContent.indexOf("<h1");
        if (h1Start >= 0) {
            int contentStart = htmlContent.indexOf('>', h1Start);
            if (contentStart >= 0) {
                int contentEnd = htmlContent.indexOf("</h1>", contentStart);
                if (contentEnd >= 0) {
                    String title = htmlContent.substring(contentStart + 1, contentEnd).trim();
                    // Remove any nested HTML tags
                    return title.replaceAll("<[^>]+>", "").trim();
                }
            }
        }
        // Fallback to raw content
        try {
            String content = Files.readString(filePath);
            return extractTitleFallback(filePath.getFileName().toString(), content);
        } catch (Exception e) {
            return filePath.getFileName().toString();
        }
    }

    private String extractTitleFallback(String pagePath, String content) {
        // Try to extract from AsciiDoc header
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("= ")) {
                return line.substring(2).trim();
            }
            // Stop looking after header section
            if (line.isBlank() && lines.length > 1) {
                break;
            }
        }
        // Fallback to filename
        String fileName = Path.of(pagePath).getFileName().toString();
        if (fileName.endsWith(".adoc")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        return fileName.replace('-', ' ').replace('_', ' ');
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * Build an edit URL for a page based on the configured pattern.
     */
    private String buildEditUrl(UiSection ui, SourceConfig source, BranchConfig branch, String pagePath) {
        if (ui == null || ui.editUrlPattern() == null || ui.editUrlPattern().isBlank()) {
            return null;
        }
        return ui.editUrlPattern()
            .replace("{repo_url}", source.url().replaceFirst("\\.git$", ""))
            .replace("{branch}", branch.name())
            .replace("{path}", source.startPath() + "/" + pagePath);
    }

    /**
     * Build a source view URL for a page based on the configured pattern.
     */
    private String buildSourceUrl(UiSection ui, SourceConfig source, BranchConfig branch, String pagePath) {
        if (ui == null || ui.sourceUrlPattern() == null || ui.sourceUrlPattern().isBlank()) {
            return null;
        }
        return ui.sourceUrlPattern()
            .replace("{repo_url}", source.url().replaceFirst("\\.git$", ""))
            .replace("{branch}", branch.name())
            .replace("{path}", source.startPath() + "/" + pagePath);
    }

    private List<DocPage.Breadcrumb> buildBreadcrumbs(NavTree nav, String componentId, String version,
                                                      String startPage, String pagePath, String title) {
        List<DocPage.Breadcrumb> crumbs = new ArrayList<>();
        if (nav != null) {
            List<NavItem> path = nav.buildBreadcrumbs(pagePath);
            for (NavItem item : path) {
                if (item.page() != null) {
                    String itemRoute = buildRoute(componentId, version, item.page(), startPage);
                    crumbs.add(new DocPage.Breadcrumb(item.title(), itemRoute));
                } else {
                    crumbs.add(new DocPage.Breadcrumb(item.title(), null));
                }
            }
        }
        // Add current page
        crumbs.add(new DocPage.Breadcrumb(title, null));
        return crumbs;
    }

    private void linkPages(List<DocPage> pages, NavTree nav) {
        // Build ordered list from navigation
        List<String> orderedPaths = new ArrayList<>();
        if (nav != null) {
            collectPagePathsFromItems(nav.items(), orderedPaths);
        } else {
            for (DocPage p : pages) {
                orderedPaths.add(p.sourcePath());
            }
        }

        // Create page map
        var pageMap = new java.util.HashMap<String, DocPage>();
        for (DocPage p : pages) {
            pageMap.put(p.sourcePath(), p);
        }

        // Link prev/next based on navigation order
        for (int i = 0; i < orderedPaths.size(); i++) {
            DocPage page = pageMap.get(orderedPaths.get(i));
            if (page == null) continue;

            DocPage prev = i > 0 ? pageMap.get(orderedPaths.get(i - 1)) : null;
            DocPage next = i < orderedPaths.size() - 1 ? pageMap.get(orderedPaths.get(i + 1)) : null;

            // Rebuild with prev/next, preserving edit/source URLs
            DocPage updated = new DocPage(
                page.componentId(), page.version(), page.sourcePath(), page.sourceUri(),
                page.pageId(), page.title(), page.navTitle(), page.route(), page.html(),
                page.breadcrumbs(), prev, next,
                page.editUrl(), page.sourceUrl()
            );
            pages.set(i, updated);
            pageMap.put(orderedPaths.get(i), updated);
        }
    }

    @Override
    public void close() {
        for (GitSourceResolver resolver : resolvers) {
            resolver.close();
        }
        resolvers.clear();
    }
}
