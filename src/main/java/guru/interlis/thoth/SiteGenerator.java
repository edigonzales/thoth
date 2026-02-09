package guru.interlis.thoth;

import org.asciidoctor.Asciidoctor;

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
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
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

public final class SiteGenerator implements AutoCloseable {
    private static final DateTimeFormatter FEED_DATE_FORMATTER =
        DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);
    private static final int INDEX_THUMBNAIL_MAX_WIDTH = 360;
    private static final int INDEX_THUMBNAIL_MAX_HEIGHT = 240;
    private static final Set<String> THUMBNAIL_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private static final List<String> BUNDLED_ASSETS = List.of(
        "site-assets/zurich.css::assets/zurich.css",
        "site-assets/fonts/Zurich/306E22_0_0.eot::assets/fonts/Zurich/306E22_0_0.eot",
        "site-assets/fonts/Zurich/306E22_0_0.ttf::assets/fonts/Zurich/306E22_0_0.ttf",
        "site-assets/fonts/Zurich/306E22_0_0.woff::assets/fonts/Zurich/306E22_0_0.woff",
        "site-assets/fonts/Zurich/306E22_0_0.woff2::assets/fonts/Zurich/306E22_0_0.woff2",
        "site-assets/fonts/Zurich/306E22_1_0.eot::assets/fonts/Zurich/306E22_1_0.eot",
        "site-assets/fonts/Zurich/306E22_1_0.ttf::assets/fonts/Zurich/306E22_1_0.ttf",
        "site-assets/fonts/Zurich/306E22_1_0.woff::assets/fonts/Zurich/306E22_1_0.woff",
        "site-assets/fonts/Zurich/306E22_1_0.woff2::assets/fonts/Zurich/306E22_1_0.woff2",
        "site-assets/styles-light.css::assets/styles-light.css",
        "site-assets/styles-dark.css::assets/styles-dark.css",
        "site-assets/theme.js::assets/theme.js",
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

    private final Path inputRoot;
    private final Path outputRoot;
    private final TemplateService templateService;
    private final Asciidoctor asciidoctor;
    private final PostParser postParser;
    private final Map<Path, Post> posts;
    private final Set<String> generatedTagSlugs;

    private SiteConfig config;

    public SiteGenerator(Path inputRoot, Path outputRoot) throws IOException {
        this.inputRoot = inputRoot.toAbsolutePath().normalize();
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
        this.config = SiteConfig.load(this.inputRoot);
        this.templateService = new TemplateService();
        this.asciidoctor = Asciidoctor.Factory.create();
        this.postParser = new PostParser(asciidoctor);
        this.posts = new ConcurrentHashMap<>();
        this.generatedTagSlugs = new HashSet<>();
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
        loadAllPosts();
        copyAllNonAdocAssets();
        writeBundledAssets();
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

            boolean isAdoc = relativePath.toString().endsWith(".adoc");
            if ("DELETE".equals(eventType)) {
                handleDelete(relativePath, isAdoc);
                return;
            }

            if (isAdoc) {
                updateSinglePost(relativePath);
                renderAggregatedPages();
            } else {
                copySingleAsset(relativePath);
                if (SiteConfig.FILE_NAME.equals(relativePath.toString())) {
                    reloadConfig();
                    renderAllPosts();
                    renderAggregatedPages();
                }
            }
        } catch (Exception ex) {
            System.err.println("[warn] Failed handling file event for " + changedFile + ": " + ex.getMessage());
        }
    }

    public int resolveServePort(Integer commandLinePort) {
        return commandLinePort != null ? commandLinePort : config.devPort();
    }

    private void handleDelete(Path relativePath, boolean isAdoc) throws IOException {
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

    private void reloadConfig() throws IOException {
        this.config = SiteConfig.load(inputRoot);
    }

    private void loadAllPosts() throws IOException {
        posts.clear();

        try (var stream = Files.walk(inputRoot)) {
            stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".adoc"))
                .forEach(path -> {
                    try {
                        Post post = postParser.parse(path, inputRoot);
                        posts.put(post.sourceRelativePath(), post);
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to parse post " + path, ex);
                    }
                });
        }
    }

    private void copyAllNonAdocAssets() throws IOException {
        try (var stream = Files.walk(inputRoot)) {
            stream
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().endsWith(".adoc"))
                .forEach(path -> {
                    Path relativePath = inputRoot.relativize(path);
                    try {
                        copyFile(path, outputRoot.resolve(relativePath));
                        System.out.println("[copy] " + toUnixPath(relativePath));
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed copying asset " + path, ex);
                    }
                });
        }
    }

    private void copySingleAsset(Path relativePath) throws IOException {
        Path source = inputRoot.resolve(relativePath);
        if (!Files.exists(source)) {
            return;
        }

        copyFile(source, outputRoot.resolve(relativePath));
        System.out.println("[copy] " + toUnixPath(relativePath));
    }

    private void writeBundledAssets() throws IOException {
        for (String descriptor : BUNDLED_ASSETS) {
            String[] parts = descriptor.split("::");
            String resourcePath = parts[0];
            Path targetPath = outputRoot.resolve(parts[1]);

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalStateException("Missing bundled asset: " + resourcePath);
                }
                Files.createDirectories(targetPath.getParent());
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void renderAllPosts() throws IOException {
        for (Post post : posts.values()) {
            renderPost(post);
        }
    }

    private void updateSinglePost(Path relativePath) throws IOException {
        Path source = inputRoot.resolve(relativePath);
        if (!Files.exists(source)) {
            return;
        }

        Post post = postParser.parse(source, inputRoot);
        posts.put(relativePath, post);
        renderPost(post);
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
        Map<String, Object> model = baseModel(config.title(), "");
        model.put("posts", summariesForTemplate(sortedPosts, true));
        templateService.renderToFile("index.ftl", model, outputRoot.resolve("index.html"));
    }

    private void renderArchivePage(List<Post> sortedPosts) throws IOException {
        Map<String, Object> model = baseModel("Archive", "");
        model.put("posts", summariesForTemplate(sortedPosts, false));
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
            summary.put("url", post.url());
            summary.put("tags", tagsForTemplate(post.tags()));
            if (includeTeaserAndCover) {
                summary.put("teaser", post.teaser());
                summary.put("coverImage", resolveIndexCoverImage(post.coverImage()));
            }
            summaries.add(summary);
        }
        return summaries;
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

        site.put("title", config.title());
        site.put("description", config.description());
        site.put("baseUrl", config.baseUrl());
        site.put("language", config.language());

        model.put("site", site);
        model.put("pageTitle", pageTitle);
        model.put("searchQuery", searchQuery == null ? "" : searchQuery);
        return model;
    }

    private String formatDate(LocalDate date) {
        return config.htmlDateFormatter().format(date);
    }

    private String feedDescription(Post post) {
        String content = post.teaser();
        if (content == null || content.isBlank()) {
            content = post.plainText();
        }

        if (content.length() > 400) {
            return content.substring(0, 400).trim();
        }
        return content;
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
