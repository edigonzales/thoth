package guru.interlis.thoth.blog;

import org.asciidoctor.Asciidoctor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SiteGenerator implements AutoCloseable {
    private static final String CONTENT_DIR_NAME = "blog";
    private static final String TEMPLATES_DIR_NAME = "templates";
    private static final String ASSET_OVERRIDES_DIR_NAME = "assets";
    private static final String THOTH_IGNORE_FILE_NAME = ".thothignore";
    private static final DateTimeFormatter FEED_DATE_FORMATTER =
        DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);
    private static final int INDEX_THUMBNAIL_MAX_WIDTH = 360;
    private static final int INDEX_THUMBNAIL_MAX_HEIGHT = 240;
    private static final Set<String> THUMBNAIL_EXTENSIONS = Set.of("png", "jpg", "jpeg");
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

    private static final List<String> BUNDLED_ASSETS = List.of(
        "site-assets/zurich.css::assets/zurich.css",
        "site-assets/styles-light.css::assets/styles-light.css",
        "site-assets/styles-dark.css::assets/styles-dark.css",
        "site-assets/theme.js::assets/theme.js",
        "site-assets/code-copy.js::assets/code-copy.js",
        "site-assets/image-lightbox.js::assets/image-lightbox.js",
        "site-assets/home-hero.jpg::assets/home-hero.jpg",
        "site-assets/search.js::assets/search.js",
        "site-assets/lunr.min.js::assets/lunr.min.js",
        "site-assets/prism/prism.css::assets/prism/prism.css",
        "site-assets/prism/prism.js::assets/prism/prism.js",
        "site-assets/prism/components/prism-markup.min.js::assets/prism/components/prism-markup.min.js",
        "site-assets/prism/components/prism-clike.min.js::assets/prism/components/prism-clike.min.js",
        "site-assets/prism/components/prism-javascript.min.js::assets/prism/components/prism-javascript.min.js",
        "site-assets/prism/components/prism-css.min.js::assets/prism/components/prism-css.min.js",
        "site-assets/prism/components/prism-ini.min.js::assets/prism/components/prism-ini.min.js",
        "site-assets/prism/components/prism-interlis.js::assets/prism/components/prism-interlis.js",
        "site-assets/prism/components/prism-java.min.js::assets/prism/components/prism-java.min.js",
        "site-assets/prism/components/prism-typescript.min.js::assets/prism/components/prism-typescript.min.js",
        "site-assets/prism/components/prism-json.min.js::assets/prism/components/prism-json.min.js",
        "site-assets/prism/components/prism-bash.min.js::assets/prism/components/prism-bash.min.js",
        "site-assets/prism/components/prism-sql.min.js::assets/prism/components/prism-sql.min.js",
        "site-assets/prism/components/prism-python.min.js::assets/prism/components/prism-python.min.js",
        "site-assets/prism/components/prism-yaml.min.js::assets/prism/components/prism-yaml.min.js",
        "site-assets/prism/components/prism-kotlin.min.js::assets/prism/components/prism-kotlin.min.js",
        "site-assets/prism/components/prism-go.min.js::assets/prism/components/prism-go.min.js",
        "site-assets/prism/components/prism-c.min.js::assets/prism/components/prism-c.min.js",
        "site-assets/prism/components/prism-cpp.min.js::assets/prism/components/prism-cpp.min.js",
        "site-assets/prism/plugins/line-highlight/prism-line-highlight.min.css::assets/prism/plugins/line-highlight/prism-line-highlight.min.css",
        "site-assets/prism/plugins/line-highlight/prism-line-highlight.min.js::assets/prism/plugins/line-highlight/prism-line-highlight.min.js",
        "site-assets/prism/plugins/line-numbers/prism-line-numbers.min.css::assets/prism/plugins/line-numbers/prism-line-numbers.min.css",
        "site-assets/prism/plugins/line-numbers/prism-line-numbers.min.js::assets/prism/plugins/line-numbers/prism-line-numbers.min.js",
        "site-assets/fonts/JetBrainsMono/JetBrainsMono-Regular.woff2::assets/fonts/JetBrainsMono/JetBrainsMono-Regular.woff2",
        "site-assets/fonts/JetBrainsMono/JetBrainsMono-Bold.woff2::assets/fonts/JetBrainsMono/JetBrainsMono-Bold.woff2",
        "site-assets/fonts/JetBrainsMono/JetBrainsMono-Italic.woff2::assets/fonts/JetBrainsMono/JetBrainsMono-Italic.woff2"
    );
    private static final Map<String, String> BUNDLED_ASSET_RESOURCE_BY_OUTPUT = BUNDLED_ASSETS.stream()
        .map(descriptor -> descriptor.split("::", 2))
        .collect(Collectors.toUnmodifiableMap(parts -> parts[1], parts -> parts[0]));

    private final Path inputRoot;
    private final Path contentRoot;
    private final Path templateOverrideRoot;
    private final Path assetOverrideRoot;
    private final Path outputRoot;
    private TemplateService templateService;
    private final Asciidoctor asciidoctor;
    private final PostParser postParser;
    private final Map<Path, Post> posts;
    private final Set<String> generatedTagSlugs;
    private final List<PathMatcher> ignoreMatchers;

    private SiteConfig config;

    public SiteGenerator(Path inputRoot, Path outputRoot) throws IOException {
        this.inputRoot = inputRoot.toAbsolutePath().normalize();
        this.contentRoot = this.inputRoot.resolve(CONTENT_DIR_NAME);
        this.templateOverrideRoot = this.inputRoot.resolve(TEMPLATES_DIR_NAME);
        this.assetOverrideRoot = this.inputRoot.resolve(ASSET_OVERRIDES_DIR_NAME);
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
        requireContentRoot();
        this.config = SiteConfig.load(this.inputRoot);
        this.templateService = new TemplateService(templateOverrideRoot);
        this.asciidoctor = Asciidoctor.Factory.create();
        this.postParser = new PostParser(asciidoctor);
        this.posts = new ConcurrentHashMap<>();
        this.generatedTagSlugs = new HashSet<>();
        this.ignoreMatchers = loadIgnoreMatchers();
    }

    public SiteConfig config() {
        return config;
    }

    public void buildAll(boolean cleanOutput) throws IOException {
        if (cleanOutput && Files.exists(outputRoot)) {
            deleteRecursively(outputRoot);
        }

        Files.createDirectories(outputRoot);
        reloadConfig();
        reloadTemplateService();
        loadAllPosts();
        copyAllNonAdocAssets();
        writeBundledAssets();
        copyAllAssetOverrides();
        renderAllPosts();
        renderAggregatedPages();
    }

    public void handleInputEvent(Path changedFile, String eventType) {
        try {
            Path absolutePath = changedFile.toAbsolutePath().normalize();
            if (!absolutePath.startsWith(inputRoot)) {
                return;
            }

            Path relativePath = inputRoot.relativize(absolutePath);
            if (Files.exists(absolutePath) && Files.isDirectory(absolutePath)) {
                return;
            }

            if (SiteConfig.FILE_NAME.equals(relativePath.toString())) {
                if (!"DELETE".equals(eventType)) {
                    reloadConfig();
                    renderAllPosts();
                    renderAggregatedPages();
                }
                return;
            }

            Path contentRelativePath = toNestedRelativePath(relativePath, CONTENT_DIR_NAME);
            if (contentRelativePath != null) {
                if (shouldIgnoreAsset(toInputContentRelativePath(contentRelativePath))) {
                    return;
                }

                boolean isAdoc = contentRelativePath.toString().endsWith(".adoc");
                if ("DELETE".equals(eventType)) {
                    handleContentDelete(contentRelativePath, isAdoc);
                    return;
                }

                if (isAdoc) {
                    updateSinglePost(contentRelativePath);
                    renderAggregatedPages();
                } else {
                    copySingleContentAsset(contentRelativePath);
                }
                return;
            }

            Path templateRelativePath = toNestedRelativePath(relativePath, TEMPLATES_DIR_NAME);
            if (templateRelativePath != null) {
                if (!"DELETE".equals(eventType) && shouldIgnoreAsset(relativePath)) {
                    return;
                }
                reloadTemplateService();
                renderAllPosts();
                renderAggregatedPages();
                return;
            }

            Path assetOverrideRelativePath = toNestedRelativePath(relativePath, ASSET_OVERRIDES_DIR_NAME);
            if (assetOverrideRelativePath != null) {
                if (!"DELETE".equals(eventType) && shouldIgnoreAsset(relativePath)) {
                    return;
                }

                if ("DELETE".equals(eventType)) {
                    deleteSingleAssetOverride(assetOverrideRelativePath);
                } else {
                    copySingleAssetOverride(assetOverrideRelativePath);
                }
            }
        } catch (Exception ex) {
            System.err.println("[warn] Failed handling file event for " + changedFile + ": " + ex.getMessage());
        }
    }

    public int resolveServePort(Integer commandLinePort) {
        return commandLinePort != null ? commandLinePort : config.devPort();
    }

    private void handleContentDelete(Path relativePath, boolean isAdoc) throws IOException {
        if (isAdoc) {
            posts.remove(relativePath);
            deletePostOutput(relativePath);
            System.out.println("[remove] " + toUnixPath(relativePath));
            renderAggregatedPages();
            return;
        }

        Path target = outputRoot.resolve(relativePath);
        Files.deleteIfExists(target);
        System.out.println("[delete] " + toUnixPath(relativePath));
    }

    private void requireContentRoot() {
        if (!Files.isDirectory(contentRoot)) {
            throw new IllegalArgumentException("Missing required content directory: " + contentRoot);
        }
    }

    private void reloadConfig() throws IOException {
        this.config = SiteConfig.load(inputRoot);
    }

    private void reloadTemplateService() {
        this.templateService = new TemplateService(templateOverrideRoot);
    }

    private void loadAllPosts() throws IOException {
        posts.clear();

        try (var stream = Files.walk(contentRoot)) {
            stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".adoc"))
                .forEach(path -> {
                    try {
                        Post post = postParser.parse(path, contentRoot);
                        posts.put(post.sourceRelativePath(), post);
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to parse post " + path, ex);
                    }
                });
        }
    }

    private void copyAllNonAdocAssets() throws IOException {
        Files.walkFileTree(contentRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (contentRoot.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = contentRoot.relativize(dir);
                if (shouldIgnoreDirectory(toInputContentRelativePath(relativePath))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = contentRoot.relativize(file);
                if (relativePath.toString().endsWith(".adoc")
                    || shouldIgnoreAsset(toInputContentRelativePath(relativePath))) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    copyFile(file, outputRoot.resolve(relativePath));
                    System.out.println("[copy] " + toUnixPath(relativePath));
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed copying asset " + file, ex);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copySingleContentAsset(Path relativePath) throws IOException {
        Path source = contentRoot.resolve(relativePath);
        if (!Files.exists(source)) {
            return;
        }

        if (shouldIgnoreAsset(toInputContentRelativePath(relativePath))) {
            return;
        }

        copyFile(source, outputRoot.resolve(relativePath));
        System.out.println("[copy] " + toUnixPath(relativePath));
    }

    private void copyAllAssetOverrides() throws IOException {
        if (!Files.isDirectory(assetOverrideRoot)) {
            return;
        }

        Files.walkFileTree(assetOverrideRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (assetOverrideRoot.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = assetOverrideRoot.relativize(dir);
                if (shouldIgnoreDirectory(Path.of(ASSET_OVERRIDES_DIR_NAME).resolve(relativePath))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = assetOverrideRoot.relativize(file);
                if (shouldIgnoreAsset(Path.of(ASSET_OVERRIDES_DIR_NAME).resolve(relativePath))) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    copySingleAssetOverride(relativePath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed copying asset override " + file, ex);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copySingleAssetOverride(Path relativePath) throws IOException {
        Path source = assetOverrideRoot.resolve(relativePath);
        if (!Files.exists(source)) {
            return;
        }

        Path outputRelativePath = Path.of("assets").resolve(relativePath);
        copyFile(source, outputRoot.resolve(outputRelativePath));
        System.out.println("[copy] " + toUnixPath(outputRelativePath));
    }

    private void deleteSingleAssetOverride(Path relativePath) throws IOException {
        Path outputRelativePath = Path.of("assets").resolve(relativePath);
        String bundledResource = BUNDLED_ASSET_RESOURCE_BY_OUTPUT.get(toUnixPath(outputRelativePath));
        if (bundledResource != null) {
            writeBundledAsset(bundledResource, outputRoot.resolve(outputRelativePath));
            System.out.println("[copy] " + toUnixPath(outputRelativePath));
            return;
        }

        Files.deleteIfExists(outputRoot.resolve(outputRelativePath));
        System.out.println("[delete] " + toUnixPath(outputRelativePath));
    }

    private boolean shouldIgnoreAsset(Path relativePath) {
        Path fileName = relativePath.getFileName();
        if (fileName == null) {
            return false;
        }

        String fileNameValue = fileName.toString();
        if (".DS_Store".equals(fileNameValue) || THOTH_IGNORE_FILE_NAME.equals(fileNameValue)) {
            return true;
        }

        if (isInDefaultExcludedDirectory(relativePath)) {
            return true;
        }

        return matchesIgnorePatterns(relativePath);
    }

    private boolean shouldIgnoreDirectory(Path relativePath) {
        if (isInDefaultExcludedDirectory(relativePath)) {
            return true;
        }

        if (matchesIgnorePatterns(relativePath)) {
            return true;
        }

        Path directoryProbe = relativePath.resolve("probe");
        return matchesIgnorePatterns(directoryProbe);
    }

    private boolean isInDefaultExcludedDirectory(Path relativePath) {
        for (Path part : relativePath) {
            if (DEFAULT_EXCLUDED_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIgnorePatterns(Path relativePath) {
        if (ignoreMatchers.isEmpty()) {
            return false;
        }

        Path normalizedPath = Path.of(toUnixPath(relativePath));
        for (PathMatcher matcher : ignoreMatchers) {
            if (matcher.matches(normalizedPath)) {
                return true;
            }
        }

        return false;
    }

    private List<PathMatcher> loadIgnoreMatchers() {
        Path ignoreFile = inputRoot.resolve(THOTH_IGNORE_FILE_NAME);
        if (!Files.exists(ignoreFile) || Files.isDirectory(ignoreFile)) {
            return List.of();
        }

        List<PathMatcher> matchers = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(ignoreFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                String pattern = line.trim();
                if (pattern.isEmpty() || pattern.startsWith("#")) {
                    continue;
                }

                String normalizedPattern = pattern.startsWith("/") ? pattern.substring(1) : pattern;
                if (normalizedPattern.endsWith("/")) {
                    normalizedPattern = normalizedPattern + "**";
                }

                try {
                    matchers.add(Path.of(".").getFileSystem().getPathMatcher("glob:" + normalizedPattern));
                } catch (IllegalArgumentException ex) {
                    System.err.println("[warn] Ignoring invalid .thothignore pattern '" + pattern + "': " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("[warn] Failed reading " + THOTH_IGNORE_FILE_NAME + ": " + ex.getMessage());
            return List.of();
        }

        return List.copyOf(matchers);
    }

    private void writeBundledAssets() throws IOException {
        for (String descriptor : BUNDLED_ASSETS) {
            String[] parts = descriptor.split("::", 2);
            writeBundledAsset(parts[0], outputRoot.resolve(parts[1]));
        }
    }

    private void writeBundledAsset(String resourcePath, Path targetPath) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled asset: " + resourcePath);
            }
            Files.createDirectories(targetPath.getParent());
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void renderAllPosts() throws IOException {
        for (Post post : posts.values()) {
            renderPost(post);
        }
    }

    private void updateSinglePost(Path relativePath) throws IOException {
        Path source = contentRoot.resolve(relativePath);
        if (!Files.exists(source)) {
            return;
        }

        Post post = postParser.parse(source, contentRoot);
        posts.put(relativePath, post);
        renderPost(post);
    }

    private Path toNestedRelativePath(Path inputRelativePath, String topLevelDirectory) {
        if (inputRelativePath.getNameCount() < 2) {
            return null;
        }

        if (!topLevelDirectory.equals(inputRelativePath.getName(0).toString())) {
            return null;
        }

        return inputRelativePath.subpath(1, inputRelativePath.getNameCount());
    }

    private Path toInputContentRelativePath(Path contentRelativePath) {
        return Path.of(CONTENT_DIR_NAME).resolve(contentRelativePath);
    }

    private void renderPost(Post post) throws IOException {
        Map<String, Object> model = baseModel(post.title(), "");

        Map<String, Object> postModel = new LinkedHashMap<>();
        postModel.put("title", post.title());
        postModel.put("author", post.author());
        postModel.put("date", formatDate(post.date()));
        postModel.put("status", post.status());
        postModel.put("html", post.htmlContent());
        postModel.put("tags", tagsForTemplate(post.tags()));
        postModel.put("url", post.url());

        model.put("post", postModel);
        templateService.renderToFile("post.ftl", model, outputRoot.resolve(post.outputRelativePath()));

        System.out.println("[render] " + toUnixPath(post.sourceRelativePath()) + " -> " + toUnixPath(post.outputRelativePath()));
    }

    private void renderAggregatedPages() throws IOException {
        List<Post> sortedPosts = sortedPosts();

        renderIndexPage(sortedPosts);
        renderArchivePage(sortedPosts);
        renderSearchPage();
        renderTagPages(sortedPosts);
        renderFeed(sortedPosts);
        writeSearchIndex(sortedPosts);
    }

    private void renderIndexPage(List<Post> sortedPosts) throws IOException {
        if (!config.indexThumbnailsEnabled()) {
            cleanupIndexThumbnails();
        }
        Map<String, Object> model = baseModel(config.title(), "");
        model.put("posts", summariesForTemplate(sortedPosts, true));
        templateService.renderToFile("index.ftl", model, outputRoot.resolve("index.html"));
    }

    private void renderArchivePage(List<Post> sortedPosts) throws IOException {
        Map<String, Object> model = baseModel("Blog Archive", "");
        model.put("groups", archiveGroupsForTemplate(sortedPosts));
        templateService.renderToFile("archive.ftl", model, outputRoot.resolve("archive.html"));
    }

    private void renderSearchPage() throws IOException {
        Map<String, Object> model = baseModel("Search", "");
        templateService.renderToFile("search.ftl", model, outputRoot.resolve("search.html"));
    }

    private void renderTagPages(List<Post> sortedPosts) throws IOException {
        cleanupOldTagPages();

        Map<String, String> displayNameBySlug = new LinkedHashMap<>();
        Map<String, List<Post>> postsBySlug = new LinkedHashMap<>();

        for (Post post : sortedPosts) {
            for (TagRef tag : post.tags()) {
                displayNameBySlug.putIfAbsent(tag.slug(), tag.name());
                postsBySlug.computeIfAbsent(tag.slug(), ignored -> new ArrayList<>()).add(post);
            }
        }

        generatedTagSlugs.clear();
        generatedTagSlugs.addAll(postsBySlug.keySet());

        for (Map.Entry<String, List<Post>> entry : postsBySlug.entrySet()) {
            String slug = entry.getKey();
            String displayName = displayNameBySlug.getOrDefault(slug, slug);

            Map<String, Object> model = baseModel("Tag: " + displayName, "");
            model.put("tagName", displayName);
            model.put("posts", summariesForTemplate(entry.getValue(), false));

            Path tagFile = outputRoot.resolve("tags").resolve(slug).resolve("index.html");
            templateService.renderToFile("tag.ftl", model, tagFile);
        }
    }

    private void renderFeed(List<Post> sortedPosts) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", config.title());
        model.put("siteDescription", config.description());
        model.put("siteLanguage", config.language());
        model.put("siteLink", config.baseUrl());
        model.put("feedSelf", config.absoluteUrl("/feed.xml"));

        ZonedDateTime now = ZonedDateTime.now(config.zoneId());
        String nowFormatted = FEED_DATE_FORMATTER.format(now);
        model.put("pubDate", nowFormatted);
        model.put("lastBuildDate", nowFormatted);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Post post : sortedPosts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", post.title());
            item.put("link", config.absoluteUrl(post.url()));
            item.put("pubDate", FEED_DATE_FORMATTER.format(post.date().atStartOfDay(config.zoneId())));
            item.put("guid", post.guid());
            item.put("description", cdataSafe(feedDescription(post)));
            items.add(item);
        }
        model.put("items", items);

        templateService.renderToFile("feed.ftl", model, outputRoot.resolve("feed.xml"));
    }

    private void writeSearchIndex(List<Post> sortedPosts) throws IOException {
        Path searchIndexPath = outputRoot.resolve("assets/search-index.json");
        Files.createDirectories(searchIndexPath.getParent());

        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < sortedPosts.size(); i++) {
            Post post = sortedPosts.get(i);
            if (i > 0) {
                json.append(",\n");
            }

            json.append("  {")
                .append(jsonField("title", post.title())).append(",")
                .append(jsonField("date", post.date().toString())).append(",")
                .append(jsonField("tags", post.tagsAsText())).append(",")
                .append(jsonField("url", post.url())).append(",")
                .append(jsonField("body", post.plainText())).append(",")
                .append(jsonField("teaser", post.teaser()))
                .append("}");
        }

        json.append("\n]\n");
        Files.writeString(searchIndexPath, json.toString(), StandardCharsets.UTF_8);
    }

    private void cleanupOldTagPages() throws IOException {
        Path tagsRoot = outputRoot.resolve("tags");
        if (Files.exists(tagsRoot)) {
            deleteRecursively(tagsRoot);
        }
        Files.createDirectories(tagsRoot);
    }

    private List<Post> sortedPosts() {
        return posts.values().stream()
            .sorted(Comparator
                .comparing(Post::date, Comparator.reverseOrder())
                .thenComparing(Post::title, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<Map<String, Object>> summariesForTemplate(List<Post> postsToConvert, boolean includeTeaserAndCover) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Post post : postsToConvert) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("title", post.title());
            summary.put("date", formatDate(post.date()));
            summary.put("author", post.author());
            summary.put("wordCount", wordCount(post.plainText()));
            summary.put("url", post.url());
            summary.put("tags", tagsForTemplate(post.tags()));
            if (includeTeaserAndCover) {
                summary.put("teaser", post.teaser());
                if (config.indexThumbnailsEnabled()) {
                    summary.put("coverImage", resolveIndexCoverImage(post.coverImage()));
                }
            }
            summaries.add(summary);
        }
        return summaries;
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private List<Map<String, Object>> archiveGroupsForTemplate(List<Post> sortedPosts) {
        DateTimeFormatter headingFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", config.locale());
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd", config.locale());
        Map<YearMonth, List<Map<String, String>>> postsByMonth = new LinkedHashMap<>();

        for (Post post : sortedPosts) {
            List<Map<String, String>> entries = postsByMonth.computeIfAbsent(YearMonth.from(post.date()), ignored -> new ArrayList<>());

            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("day", dayFormatter.format(post.date()));
            entry.put("title", post.title());
            entry.put("url", post.url());
            entries.add(entry);
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<YearMonth, List<Map<String, String>>> monthEntry : postsByMonth.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("heading", headingFormatter.format(monthEntry.getKey().atDay(1)));
            group.put("posts", monthEntry.getValue());
            groups.add(group);
        }
        return groups;
    }

    private void cleanupIndexThumbnails() throws IOException {
        Path thumbnailsRoot = outputRoot.resolve("assets").resolve("thumbnails");
        if (Files.exists(thumbnailsRoot)) {
            deleteRecursively(thumbnailsRoot);
        }
    }

    private String resolveIndexCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isBlank()) {
            return coverImage;
        }

        String normalizedCover = coverImage.trim();
        if (!normalizedCover.startsWith("/") || normalizedCover.startsWith("//")) {
            return normalizedCover;
        }

        String relativeCoverPath = normalizedCover.substring(1);
        Path source = outputRoot.resolve(relativeCoverPath).normalize();
        if (!source.startsWith(outputRoot) || !Files.exists(source) || Files.isDirectory(source)) {
            return normalizedCover;
        }

        String extension = extensionOf(source.getFileName().toString());
        if (!THUMBNAIL_EXTENSIONS.contains(extension)) {
            return normalizedCover;
        }

        Path sourceRelativePath = outputRoot.relativize(source);
        String fileName = source.getFileName().toString();
        String fileNameWithoutExtension = fileName.substring(0, fileName.length() - extension.length() - 1);
        String outputExtension = "jpeg".equals(extension) ? "jpg" : extension;
        String thumbnailFileName = fileNameWithoutExtension + "-thumb." + outputExtension;

        Path thumbnailsRoot = Path.of("assets", "thumbnails");
        Path thumbnailRelativePath = thumbnailsRoot.resolve(sourceRelativePath).getParent().resolve(thumbnailFileName);
        Path thumbnailAbsolutePath = outputRoot.resolve(thumbnailRelativePath).normalize();
        if (!thumbnailAbsolutePath.startsWith(outputRoot)) {
            return normalizedCover;
        }

        try {
            boolean created = createThumbnail(source, thumbnailAbsolutePath, outputExtension);
            if (created) {
                return "/" + toUnixPath(thumbnailRelativePath);
            }
            return normalizedCover;
        } catch (IOException ex) {
            System.err.println("[warn] Failed creating thumbnail for " + normalizedCover + ": " + ex.getMessage());
            return normalizedCover;
        }
    }

    private boolean createThumbnail(Path source, Path target, String format) throws IOException {
        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) {
            return false;
        }

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        if (originalWidth <= 0 || originalHeight <= 0) {
            return false;
        }

        double scale = Math.min(
            1.0d,
            Math.min((double) INDEX_THUMBNAIL_MAX_WIDTH / originalWidth, (double) INDEX_THUMBNAIL_MAX_HEIGHT / originalHeight)
        );

        int thumbnailWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int thumbnailHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        int imageType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage thumbnail = resizeProgressively(original, thumbnailWidth, thumbnailHeight, imageType);

        Files.createDirectories(target.getParent());
        if (!ImageIO.write(thumbnail, format, target.toFile())) {
            throw new IOException("Image format not supported for thumbnail generation: " + format);
        }
        return true;
    }

    private BufferedImage resizeProgressively(BufferedImage source, int targetWidth, int targetHeight, int imageType) {
        BufferedImage current = source;
        int currentWidth = source.getWidth();
        int currentHeight = source.getHeight();

        while (currentWidth > targetWidth || currentHeight > targetHeight) {
            int nextWidth = currentWidth;
            int nextHeight = currentHeight;

            if (nextWidth > targetWidth) {
                nextWidth = Math.max(targetWidth, currentWidth / 2);
            }
            if (nextHeight > targetHeight) {
                nextHeight = Math.max(targetHeight, currentHeight / 2);
            }

            BufferedImage next = resizeTo(current, nextWidth, nextHeight, imageType);
            if (current != source) {
                current.flush();
            }

            current = next;
            currentWidth = nextWidth;
            currentHeight = nextHeight;
        }

        if (currentWidth != targetWidth || currentHeight != targetHeight || current.getType() != imageType) {
            BufferedImage finalImage = resizeTo(current, targetWidth, targetHeight, imageType);
            if (current != source) {
                current.flush();
            }
            current = finalImage;
        }

        return current;
    }

    private BufferedImage resizeTo(BufferedImage source, int width, int height, int imageType) {
        BufferedImage resized = new BufferedImage(width, height, imageType);
        Graphics2D graphics = resized.createGraphics();
        applyHighQualityHints(graphics);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private void applyHighQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private List<Map<String, String>> tagsForTemplate(Collection<TagRef> tags) {
        List<Map<String, String>> result = new ArrayList<>();
        for (TagRef tag : tags) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", tag.name());
            entry.put("slug", tag.slug());
            result.add(entry);
        }
        return result;
    }

    private Map<String, Object> baseModel(String pageTitle, String searchQuery) {
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> site = new HashMap<>();
        String[] brandParts = splitBrandTitle(config.title());

        site.put("title", config.title());
        site.put("description", config.description());
        site.put("baseUrl", config.baseUrl());
        site.put("language", config.language());
        site.put("brandMain", brandParts[0]);
        site.put("brandDomain", brandParts[1]);

        model.put("site", site);
        model.put("pageTitle", pageTitle);
        model.put("searchQuery", searchQuery == null ? "" : searchQuery);
        return model;
    }

    private String[] splitBrandTitle(String title) {
        int firstDot = title.indexOf('.');
        if (firstDot <= 0 || firstDot >= title.length() - 1) {
            return new String[] {title, ""};
        }
        return new String[] {
            title.substring(0, firstDot),
            title.substring(firstDot)
        };
    }

    private String formatDate(LocalDate date) {
        return config.htmlDateFormatter().format(date);
    }

    private String feedDescription(Post post) {
        String html = post.htmlContent();
        if (html != null && !html.isBlank()) {
            return absolutizeFeedHtml(html);
        }

        String content = post.plainText();
        if (content == null || content.isBlank()) {
            content = post.teaser();
        }
        return content == null ? "" : content;
    }

    private String absolutizeFeedHtml(String html) {
        Document document = Jsoup.parseBodyFragment(html);

        for (Element element : document.select("[href]")) {
            element.attr("href", absolutizeFeedUrl(element.attr("href")));
        }

        for (Element element : document.select("[src]")) {
            element.attr("src", absolutizeFeedUrl(element.attr("src")));
        }

        return document.body().html();
    }

    private String absolutizeFeedUrl(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String normalized = value.trim();
        if (normalized.startsWith("#") || normalized.startsWith("?")) {
            return normalized;
        }

        if (isAbsoluteOrSpecialUrl(normalized)) {
            return normalized;
        }

        if (normalized.startsWith("/")) {
            return config.absoluteUrl(normalized);
        }

        return config.absoluteUrl("/" + normalized);
    }

    private boolean isAbsoluteOrSpecialUrl(String value) {
        return value.startsWith("http://")
            || value.startsWith("https://")
            || value.startsWith("//")
            || value.startsWith("mailto:")
            || value.startsWith("tel:")
            || value.startsWith("data:");
    }

    private String cdataSafe(String text) {
        return text.replace("]]>", "]]]]><![CDATA[>");
    }

    private String jsonField(String key, String value) {
        return "\"" + escapeJson(key) + "\":\"" + escapeJson(value == null ? "" : value) + "\"";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private void deletePostOutput(Path relativePath) throws IOException {
        String base = removeAdocExtension(toUnixPath(relativePath));
        Path directory = outputRoot.resolve(base);
        if (Files.exists(directory)) {
            deleteRecursively(directory);
        }
    }

    private void copyFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String removeAdocExtension(String value) {
        if (value.endsWith(".adoc")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    private String toUnixPath(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    @Override
    public void close() {
        asciidoctor.shutdown();
    }
}
