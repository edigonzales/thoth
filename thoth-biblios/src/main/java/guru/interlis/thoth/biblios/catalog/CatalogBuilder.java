package guru.interlis.thoth.biblios.catalog;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BranchConfig;
import guru.interlis.thoth.biblios.config.NavigationConfig;
import guru.interlis.thoth.biblios.config.SourceConfig;
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
            DocComponent component = buildComponent(source);
            components.add(component);
        }

        return new SiteCatalog(components);
    }

    private DocComponent buildComponent(SourceConfig source) throws IOException {
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

            ComponentVersion version = buildVersion(source, branch, resolver.workTree());
            versions.add(version);
        }

        if (versions.isEmpty()) {
            throw new IOException("No valid versions found for source: " + source.id());
        }

        String defaultVersion = source.defaultVersion() != null
            ? source.defaultVersion()
            : versions.get(0).version();

        return new DocComponent(source.id(), source.displayName(), defaultVersion, versions);
    }

    private ComponentVersion buildVersion(SourceConfig source, BranchConfig branch, Path workTree) throws IOException {
        Path docRoot = workTree.resolve(source.startPath());
        if (!Files.exists(docRoot)) {
            throw new IOException(
                "Documentation root not found: " + docRoot + "\n" +
                "Check the 'start_path' configuration for source: " + source.id()
            );
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
        List<DocPage> pages = discoverAndBuildPages(source, branch, docRoot, navigation);

        if (pages.isEmpty()) {
            System.err.println("[warn] No pages found for " + source.id() + "/" + branch.name());
            System.err.println("[warn] Check that .adoc files exist in: " + docRoot);
        }

        return new ComponentVersion(
            source.id(),
            branch.name(),
            branch.displayVersion(),
            branch.name(),
            source.startPage(),
            navigation,
            pages
        );
    }

    private List<DocPage> discoverAndBuildPages(SourceConfig source, BranchConfig branch,
                                                  Path docRoot, NavTree navigation) throws IOException {
        List<DocPage> pages = new ArrayList<>();

        // Collect all .adoc files from the navigation tree
        List<String> pagePaths = collectPagePathsFromNav(navigation);

        // If no navigation, discover .adoc files recursively
        if (pagePaths.isEmpty()) {
            pagePaths = discoverAdocFiles(docRoot);
        }

        // Create renderer for this version
        try (AsciidoctorRenderer renderer = new AsciidoctorRenderer()) {
            for (String pagePath : pagePaths) {
                Path filePath = docRoot.resolve(pagePath);
                if (!Files.exists(filePath)) {
                    System.err.println("[warn] Page not found: " + filePath);
                    continue;
                }

                String pageId = pagePathToId(pagePath);
                String route = buildRoute(source.id(), branch.name(), pagePath);

                // Render AsciiDoc to HTML
                String html;
                String title;
                try {
                    html = renderer.renderFile(filePath);
                    title = extractTitle(filePath, html);
                } catch (Exception e) {
                    System.err.println("[warn] Failed to render " + filePath + ": " + e.getMessage());
                    // Fallback to raw content
                    String content = Files.readString(filePath);
                    html = "<pre><code>" + escapeHtml(content) + "</code></pre>";
                    title = extractTitleFallback(pagePath, content);
                }

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
                    buildBreadcrumbs(navigation, pagePath, title, route),
                    null, // prev/next will be set after all pages are collected
                    null
                );
                pages.add(page);
            }
        }

        // Set prev/next based on navigation order
        linkPages(pages, navigation);

        return pages;
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

    private String buildRoute(String componentId, String version, String pagePath) {
        // /<component>/<version>/<page>
        String base = "/" + componentId + "/" + version + "/";
        String pageWithoutExtension = pagePath.endsWith(".adoc")
            ? pagePath.substring(0, pagePath.length() - 5)
            : pagePath;

        if ("index".equals(pageWithoutExtension)) {
            return base;
        }
        return base + pageWithoutExtension + "/";
    }

    private String pagePathToId(String pagePath) {
        return pagePath.replace('/', '-').replace(".adoc", "");
    }

    private String extractTitle(Path filePath, String htmlContent) {
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

    private List<DocPage.Breadcrumb> buildBreadcrumbs(NavTree nav, String pagePath, String title, String route) {
        List<DocPage.Breadcrumb> crumbs = new ArrayList<>();
        if (nav != null) {
            List<NavItem> path = nav.buildBreadcrumbs(pagePath);
            for (NavItem item : path) {
                if (item.page() != null) {
                    String itemRoute = buildRoute("", "", item.page());
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

            // Rebuild with prev/next
            DocPage updated = new DocPage(
                page.componentId(), page.version(), page.sourcePath(), page.sourceUri(),
                page.pageId(), page.title(), page.navTitle(), page.route(), page.html(),
                page.breadcrumbs(), prev, next
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
