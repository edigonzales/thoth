package guru.interlis.thoth.blog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiteGeneratorIntegrationTest {

    @Test
    public void buildsSiteWithoutIndexThumbnailsByDefault() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertCommonSiteArtifacts(output);
        assertFalse(Files.exists(output.resolve("assets/thumbnails/2026/images/cover-thumb.png")));

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("First Post"));
        assertTrue(index.contains("Second Post"));
        assertTrue(index.contains("Third Post"));
        assertTrue(index.contains("Fourth Post"));
        assertTrue(index.contains("Manual teaser override"));
        assertTrue(index.contains("class=\"teaser-more\""));
        assertTrue(index.contains("bi bi-calendar3"));
        assertTrue(index.contains("bi bi-file-text"));
        assertTrue(index.contains("bi bi-person-fill"));
        assertTrue(index.contains("M14 0H2a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12"));
        assertTrue(index.contains("Posted on "));
        assertTrue(index.contains(" words"));
        assertTrue(index.contains("Alice Author"));
        assertTrue(index.contains(" | "));
        assertTrue(index.contains("5 words"));
        assertFalse(index.contains("/assets/thumbnails/2026/images/cover-thumb.png"));
        assertFalse(index.contains("class=\"post-card-body post-card-body--with-cover\""));
        assertIndexTeasers(output);
        assertHomeHero(output, "Thoth Blog", "");

        assertCommonCss(output);
        assertGroupedArchive(output);
        assertCommonFeed(output);
        assertCommonSearchIndex(output);
        assertPostFooter(output);
    }

    @Test
    public void buildsSiteWithIndexThumbnailsWhenEnabled() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, true);

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertCommonSiteArtifacts(output);
        assertTrue(Files.exists(output.resolve("assets/thumbnails/2026/images/cover-thumb.png")));

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("/assets/thumbnails/2026/images/cover-thumb.png"));
        assertTrue(index.contains("class=\"post-card-body post-card-body--with-cover\""));
        assertTrue(index.contains("bi bi-calendar3"));
        assertTrue(index.contains("bi bi-file-text"));
        assertTrue(index.contains("bi bi-person-fill"));
        assertTrue(index.contains("M14 0H2a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12"));
        assertTrue(index.contains("Posted on "));
        assertTrue(index.contains(" words"));
        assertTrue(index.contains("Alice Author"));
        assertTrue(index.contains(" | "));
        assertTrue(index.contains("5 words"));
        assertIndexTeasers(output);
        assertHomeHero(output, "Thoth Blog", "");

        assertCommonCss(output);
        assertGroupedArchive(output);
        assertCommonFeed(output);
        assertCommonSearchIndex(output);
        assertPostFooter(output);
    }

    @Test
    public void buildsHomeHeroBrandFromDotSeparatedSiteTitle() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false, "interlis.guru");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertCommonSiteArtifacts(output);
        assertHomeHero(output, "interlis", ".guru");
        assertPostFooter(output);
    }

    @Test
    public void appliesThothIgnorePatternsDuringBuild() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false);

        write(input.resolve(".thothignore"), """
            # Ignore generated scratch files
            blog/tmp/**
            blog/**/*.map
            blog/secrets/*
            """);
        write(input.resolve("blog/tmp/keep.txt"), "ignored");
        write(input.resolve("blog/2026/app.map"), "ignored");
        write(input.resolve("blog/secrets/token.txt"), "ignored");
        write(input.resolve("blog/custom/allowed.txt"), "allowed");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertFalse(Files.exists(output.resolve("tmp/keep.txt")));
        assertFalse(Files.exists(output.resolve("2026/app.map")));
        assertFalse(Files.exists(output.resolve("secrets/token.txt")));
        assertFalse(Files.exists(output.resolve(".thothignore")));
        assertTrue(Files.exists(output.resolve("custom/allowed.txt")));
    }

    @Test
    public void ignoresExcludedAssetsForWatchEvents() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false);
        write(input.resolve(".thothignore"), "blog/tmp/**\n");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);

            Path ignoredGitAsset = input.resolve(".git/objects/aa/new-object");
            write(ignoredGitAsset, "ignored");
            generator.handleInputEvent(ignoredGitAsset, "MODIFY");

            Path ignoredTmpAsset = input.resolve("blog/tmp/ignored.txt");
            write(ignoredTmpAsset, "ignored");
            generator.handleInputEvent(ignoredTmpAsset, "MODIFY");

            Path copiedAsset = input.resolve("blog/2026/new.js");
            write(copiedAsset, "console.log('new');");
            generator.handleInputEvent(copiedAsset, "MODIFY");
        }

        assertFalse(Files.exists(output.resolve(".git/objects/aa/new-object")));
        assertFalse(Files.exists(output.resolve("tmp/ignored.txt")));
        assertTrue(Files.exists(output.resolve("2026/new.js")));
    }

    @Test
    public void requiresBlogContentDirectory() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        write(input.resolve("thoth.properties"), """
            site.title=Thoth Blog
            site.description=Demo feed
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SiteGenerator(input, output));
        assertTrue(ex.getMessage().contains("Missing required content directory"));
    }

    @Test
    public void appliesTemplateAndAssetOverridesFromInputRoot() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false);

        write(
            input.resolve("templates/index.ftl"),
            "<!doctype html><html><body><main id=\"override-marker\">Template Override</main></body></html>"
        );
        write(input.resolve("assets/styles-light.css"), "/* custom styles */ body { color: #123456; }");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("Template Override"));
        assertTrue(index.contains("override-marker"));

        String css = Files.readString(output.resolve("assets/styles-light.css"), StandardCharsets.UTF_8);
        assertTrue(css.contains("custom styles"));
        assertTrue(css.contains("#123456"));
    }

    @Test
    public void updatesTemplatesAndAssetOverridesDuringWatchEvents() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");
        writeSampleSite(input, false);

        Path templateOverride = input.resolve("templates/index.ftl");
        Path assetOverride = input.resolve("assets/theme.js");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);

            write(
                templateOverride,
                "<!doctype html><html><body><main id=\"watch-template\">Watch Template</main></body></html>"
            );
            generator.handleInputEvent(templateOverride, "MODIFY");

            write(assetOverride, "// watch override marker\nconsole.log('theme override');\n");
            generator.handleInputEvent(assetOverride, "MODIFY");

            Files.delete(assetOverride);
            generator.handleInputEvent(assetOverride, "DELETE");
        }

        String index = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        assertTrue(index.contains("Watch Template"));
        assertTrue(index.contains("watch-template"));

        String themeJs = Files.readString(output.resolve("assets/theme.js"), StandardCharsets.UTF_8);
        assertFalse(themeJs.contains("watch override marker"));
    }

    @Test
    public void rendersInterlisLabMacroAndLoadsScriptOnlyForLabPosts() throws Exception {
        Path input = Files.createTempDirectory("thoth-input");
        Path output = Files.createTempDirectory("thoth-output");

        write(input.resolve("thoth.properties"), """
            site.title=Lab Blog
            site.description=Demo feed
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);
        write(input.resolve("blog/2026/lab-post.adoc"), """
            ---
            = Lab Post
            Alice Author
            2026-05-12
            ---
            interlis-lab::labs/simple.json[storage-key=blog-simple,title="Simple Lab"]
            """);
        write(input.resolve("blog/2026/plain-post.adoc"), """
            ---
            = Plain Post
            Alice Author
            2026-05-13
            ---
            Plain content.
            """);
        write(input.resolve("blog/2026/labs/simple.json"), "{\"id\":\"simple\"}");

        try (SiteGenerator generator = new SiteGenerator(input, output)) {
            generator.buildAll(true);
        }

        assertTrue(Files.exists(output.resolve("assets/interlis-lab/interlis-lab.js")));
        assertTrue(Files.exists(output.resolve("assets/interlis-lab/ili2c.jar")));
        assertTrue(Files.exists(output.resolve("2026/labs/simple.json")));

        String labHtml = Files.readString(output.resolve("2026/lab-post/index.html"), StandardCharsets.UTF_8);
        Document labDoc = Jsoup.parse(labHtml);
        Element lab = labDoc.selectFirst("interlis-lab");
        assertNotNull(lab);
        assertEquals("/2026/labs/simple.json", lab.attr("src"));
        assertEquals("cheerpj", lab.attr("runner"));
        assertEquals("/assets/interlis-lab/ili2c.jar", lab.attr("ili2c-jar-url"));
        assertEquals("/assets/interlis-lab/interlis-lab.js", labDoc.selectFirst("script[type=module]").attr("src"));

        String plainHtml = Files.readString(output.resolve("2026/plain-post/index.html"), StandardCharsets.UTF_8);
        assertFalse(plainHtml.contains("assets/interlis-lab/interlis-lab.js"));
    }

    private void assertCommonSiteArtifacts(Path output) throws Exception {
        assertTrue(Files.exists(output.resolve("2026/post-one/index.html")));
        assertTrue(Files.exists(output.resolve("2026/post-two/index.html")));
        assertTrue(Files.exists(output.resolve("2026/post-three/index.html")));
        assertTrue(Files.exists(output.resolve("2026/post-four/index.html")));
        assertTrue(Files.exists(output.resolve("index.html")));
        assertTrue(Files.exists(output.resolve("archive.html")));
        assertTrue(Files.exists(output.resolve("search.html")));
        assertTrue(Files.exists(output.resolve("feed.xml")));
        assertTrue(Files.exists(output.resolve("tags/ai/index.html")));
        assertTrue(Files.exists(output.resolve("tags/java/index.html")));

        assertTrue(Files.exists(output.resolve("assets/styles-light.css")));
        assertTrue(Files.exists(output.resolve("assets/styles-dark.css")));
        assertTrue(Files.exists(output.resolve("assets/zurich.css")));
        assertFalse(Files.exists(output.resolve("assets/fonts/Zurich")));
        assertFalse(Files.exists(output.resolve("assets/fonts/Inter")));
        assertTrue(Files.exists(output.resolve("assets/theme.js")));
        assertTrue(Files.exists(output.resolve("assets/code-copy.js")));
        assertTrue(Files.exists(output.resolve("assets/image-lightbox.js")));
        assertTrue(Files.exists(output.resolve("assets/interlis-lab/interlis-lab.js")));
        assertTrue(Files.exists(output.resolve("assets/interlis-lab/ili2c.jar")));
        assertTrue(Files.exists(output.resolve("assets/home-hero.jpg")));
        assertFalse(Files.exists(output.resolve("assets/home-hero.png")));
        assertTrue(Files.exists(output.resolve("assets/search.js")));
        assertTrue(Files.exists(output.resolve("assets/lunr.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/prism.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/prism.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-ini.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-interlis.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-javascript.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-css.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/components/prism-java.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-highlight/prism-line-highlight.min.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-highlight/prism-line-highlight.min.js")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-numbers/prism-line-numbers.min.css")));
        assertTrue(Files.exists(output.resolve("assets/prism/plugins/line-numbers/prism-line-numbers.min.js")));
        assertTrue(Files.exists(output.resolve("assets/search-index.json")));
        assertTrue(Files.exists(output.resolve("assets/fonts/JetBrainsMono/JetBrainsMono-Regular.woff2")));
        assertTrue(Files.exists(output.resolve("assets/fonts/JetBrainsMono/JetBrainsMono-Bold.woff2")));
        assertTrue(Files.exists(output.resolve("assets/fonts/JetBrainsMono/JetBrainsMono-Italic.woff2")));

        assertTrue(Files.exists(output.resolve("2026/images/cover.png")));
        assertTrue(Files.exists(output.resolve("2026/site.js")));
        assertFalse(Files.exists(output.resolve(".DS_Store")));
        assertFalse(Files.exists(output.resolve(".DS_Store")));
        assertFalse(Files.exists(output.resolve(".git/objects/aa/object")));
        assertFalse(Files.exists(output.resolve(".idea/workspace.xml")));
        assertFalse(Files.exists(output.resolve(".vscode/settings.json")));
        assertFalse(Files.exists(output.resolve("node_modules/some-package/index.js")));
        assertFalse(Files.exists(output.resolve("build/generated/asset.txt")));
        assertFalse(Files.exists(output.resolve("target/generated/asset.txt")));
        assertFalse(Files.exists(output.resolve(".gradle/metadata.bin")));
    }

    private void assertCommonCss(Path output) throws Exception {
        String lightCss = Files.readString(output.resolve("assets/styles-light.css"), StandardCharsets.UTF_8);
        assertTrue(lightCss.contains(".post-grid {\n  display: grid;\n  grid-template-columns: 1fr;"));
        assertTrue(lightCss.contains("overflow-wrap: anywhere;"));
        assertTrue(lightCss.contains(".archive-group-posts {"));
        assertTrue(lightCss.contains(".post-footer-separator {"));
        assertTrue(lightCss.contains(".post-content :not(pre) > code {"));
        assertTrue(lightCss.contains("font-size: 0.95em;"));
        assertTrue(lightCss.contains("background-color: #f2f2f2;"));
        assertTrue(lightCss.contains("--code-bg: #f2f2f2;"));
        assertTrue(lightCss.contains(".code-copy-button {"));
        assertTrue(lightCss.contains(".teaser {"));
        assertTrue(lightCss.contains("line-height: 1.6;"));
        assertTrue(lightCss.contains(".archive-list > h1 {"));
        assertTrue(lightCss.contains("font-size: 2em;"));
        assertTrue(lightCss.contains(".archive-group-heading {"));
        assertTrue(lightCss.contains("font-size: 1.5em;"));
        assertTrue(lightCss.contains(".code-copy-button svg {"));
        assertTrue(lightCss.contains(".post-meta-item {"));
        assertTrue(lightCss.contains(".post-meta-separator {"));
        assertTrue(lightCss.contains(".post-meta-icon {"));
        assertTrue(lightCss.contains(".teaser-more {"));
        assertTrue(lightCss.contains("font-weight: 700;"));
        assertTrue(lightCss.contains(".teaser-tags {"));
        assertTrue(lightCss.contains("font-size: 0.9rem;"));
        assertTrue(lightCss.contains(".post-card .teaser-tags a,"));
        assertTrue(lightCss.contains(".post-tags a {"));
        assertTrue(lightCss.contains(".post-card .teaser-tags a:hover,"));
        assertTrue(lightCss.contains(".post-tags a:hover,"));
        assertTrue(lightCss.contains(".post-card .teaser-tags a:focus-visible,"));
        assertTrue(lightCss.contains(".post-tags a:focus-visible {"));
        assertTrue(lightCss.contains("font-size: 0.9em;"));
        assertTrue(lightCss.contains(".post-content img.lightbox-trigger {"));
        assertTrue(lightCss.contains("#image-lightbox {"));
        assertTrue(lightCss.contains("#image-lightbox::backdrop {"));
        assertTrue(lightCss.contains("#image-lightbox-image {"));
        assertTrue(lightCss.contains("#image-lightbox-close {"));
        assertTrue(lightCss.contains(".post-content p a {"));
        assertTrue(lightCss.contains(".post-content pre {\n  overflow: auto;\n  padding: 0.75rem;\n  border-radius: 0.55rem;\n  background: var(--code-bg);\n}"));
        assertTrue(lightCss.contains(".post-content ul li p,\n.post-content ol li p {\n  margin: 9px 0;\n}"));
        assertTrue(lightCss.contains("--nav-link: rgb(36, 38, 43);"));
        assertTrue(lightCss.contains("--nav-link-hover: rgb(36, 38, 43);"));
        assertTrue(lightCss.contains("--link: #c9533b;"));
        assertTrue(lightCss.contains("--link-hover: #b24731;"));
        assertTrue(lightCss.contains("--focus-ring: #e4a293;"));
        assertTrue(lightCss.contains(".archive-group-posts a {\n  color: var(--link);\n}"));
        assertTrue(lightCss.contains(".post-card a {"));
        assertTrue(lightCss.contains("color: var(--text);"));
        assertTrue(lightCss.contains(".post-card a:hover,"));
        assertTrue(lightCss.contains(".post-card a:focus-visible {"));
        assertTrue(lightCss.contains("color: var(--link-hover);"));
        assertTrue(lightCss.contains("#navbar:not(.navbar--hero) .nav-left a {\n  color: var(--nav-link);\n}"));
        assertTrue(lightCss.contains("#navbar:not(.navbar--hero) .nav-left a:hover,\n#navbar:not(.navbar--hero) .nav-left a:focus-visible {\n  color: var(--nav-link-hover);\n}"));
        assertTrue(lightCss.contains(".post-card .teaser-tags a,\n.post-tags a {\n  color: var(--link);\n}"));
        assertTrue(lightCss.contains(".post-content p a {\n  color: var(--link);\n}"));
        assertTrue(lightCss.contains("outline: 2px solid var(--focus-ring);"));
        assertFalse(lightCss.contains("rgb(0, 133, 161)"));
        assertFalse(lightCss.contains("#2a6496"));
        assertTrue(lightCss.contains("#content.page-home {\n  margin-top: 0;\n}"));
        assertFalse(lightCss.contains("#content.page-home {\n  width: min(1200px, calc(100% - 2rem));"));
        assertTrue(lightCss.contains(".home-hero {"));
        assertTrue(lightCss.contains("width: 100%;"));
        assertTrue(lightCss.contains("background-position: center center;"));
        assertTrue(lightCss.contains(".home-hero__overlay {\n  display: flex;\n  flex-direction: column;\n  justify-content: flex-start;"));
        assertTrue(lightCss.contains(".home-hero__copy {\n  display: flex;\n  flex: 1 1 auto;\n  align-items: center;\n  justify-content: center;\n  min-height: 0;\n  padding: 1rem;\n}"));
        assertTrue(lightCss.contains(".brand {"));
        assertTrue(lightCss.contains("font-size: clamp(2.5rem, 5.25vw, 5.6rem);"));
        assertTrue(lightCss.contains(".brand-main {"));
        assertTrue(lightCss.contains(".brand-domain {"));
        assertTrue(lightCss.contains(".brand:hover,\n.brand:focus-visible {"));
        assertTrue(lightCss.contains("#navbar.navbar--hero .nav-left a {"));
        assertTrue(lightCss.contains("text-decoration: underline;"));
        assertTrue(lightCss.contains("#navbar.navbar--hero #search-input::placeholder {"));
        assertTrue(lightCss.contains("#navbar.navbar--hero .theme-toggle {"));
        assertTrue(lightCss.contains("#navbar.navbar--hero #search-form {"));
        assertFalse(lightCss.contains("border-radius: 1.3rem;"));
        assertFalse(lightCss.contains("box-shadow: 0 1.2rem 3rem rgba(65, 80, 102, 0.18);"));
        assertFalse(lightCss.contains("#navbar.navbar--hero .nav-left a:hover,\n#navbar.navbar--hero .nav-left a:focus-visible {\n  background:"));
        assertTrue(lightCss.contains("color: var(--muted);"));
        assertTrue(lightCss.contains("text-decoration: none;"));
        assertTrue(lightCss.contains("color: currentColor;"));
        assertTrue(lightCss.contains("white-space: nowrap;"));
        assertTrue(lightCss.contains("display: inline-block;"));
        assertTrue(lightCss.contains("vertical-align: -0.125em;"));
        assertFalse(lightCss.contains(".post-meta-item {\n  display: inline-flex;"));
        assertFalse(lightCss.contains(".post-meta-item {\n  align-items: baseline;"));

        String darkCss = Files.readString(output.resolve("assets/styles-dark.css"), StandardCharsets.UTF_8);
        assertTrue(darkCss.contains("background: var(--bg);\n  text-rendering: optimizeLegibility;"));
        assertTrue(darkCss.contains(".post-card,\n.post,\n.archive-list {\n  background: var(--bg);\n  padding: 1rem 1.1rem;\n  box-shadow: none;\n}"));
        assertFalse(darkCss.contains(".post-card,\n.post,\n.archive-list {\n  background: linear-gradient(180deg, rgba(24, 32, 45, 0.98) 0%, rgba(22, 30, 42, 0.98) 100%);"));
        assertTrue(darkCss.contains("#navbar {\n  position: sticky;\n  top: 0;\n  z-index: 100;\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  gap: 1rem;\n  padding: 0.9rem 1.2rem;\n  border-bottom: 1px solid rgba(49, 65, 86, 0.82);\n  background: rgba(16, 23, 34, 0.93);\n"));
        assertTrue(darkCss.contains("#search-results li {\n  margin: 0.65rem 0;\n  padding: 0.7rem;\n  background: linear-gradient(180deg, rgba(24, 32, 45, 0.98) 0%, rgba(22, 30, 42, 0.98) 100%);\n  border: 1px solid rgba(49, 65, 86, 0.76);\n  border-radius: 0.6rem;\n}"));
        assertTrue(darkCss.contains(".post-grid {\n  display: grid;\n  grid-template-columns: 1fr;"));
        assertTrue(darkCss.contains("overflow-wrap: anywhere;"));
        assertTrue(darkCss.contains(".archive-group-posts {"));
        assertTrue(darkCss.contains(".post-footer-separator {"));
        assertTrue(darkCss.contains(".post-content :not(pre) > code {"));
        assertTrue(darkCss.contains("font-size: 0.95em;"));
        assertTrue(darkCss.contains("background-color: var(--code-inline-bg);"));
        assertTrue(darkCss.contains(".code-copy-button {"));
        assertTrue(darkCss.contains(".teaser {"));
        assertTrue(darkCss.contains("line-height: 1.68;"));
        assertTrue(darkCss.contains(".archive-list > h1 {"));
        assertTrue(darkCss.contains("font-size: 2em;"));
        assertTrue(darkCss.contains(".archive-group-heading {"));
        assertTrue(darkCss.contains("font-size: 1.5em;"));
        assertTrue(darkCss.contains(".code-copy-button svg {"));
        assertTrue(darkCss.contains(".post-meta-item {"));
        assertTrue(darkCss.contains(".post-meta-separator {"));
        assertTrue(darkCss.contains(".post-meta-icon {"));
        assertTrue(darkCss.contains(".teaser-more {"));
        assertTrue(darkCss.contains("font-weight: 700;"));
        assertTrue(darkCss.contains(".teaser-tags {"));
        assertTrue(darkCss.contains("font-size: 0.9rem;"));
        assertTrue(darkCss.contains(".post-card .teaser-tags a,"));
        assertTrue(darkCss.contains(".post-tags a {"));
        assertTrue(darkCss.contains(".post-card .teaser-tags a:hover,"));
        assertTrue(darkCss.contains(".post-tags a:hover,"));
        assertTrue(darkCss.contains(".post-card .teaser-tags a:focus-visible,"));
        assertTrue(darkCss.contains(".post-tags a:focus-visible {"));
        assertTrue(darkCss.contains("font-size: 0.9em;"));
        assertTrue(darkCss.contains(".post-content img.lightbox-trigger {"));
        assertTrue(darkCss.contains("#image-lightbox {"));
        assertTrue(darkCss.contains("#image-lightbox::backdrop {"));
        assertTrue(darkCss.contains("#image-lightbox-image {"));
        assertTrue(darkCss.contains("#image-lightbox-close {"));
        assertTrue(darkCss.contains(".post-content p a {"));
        assertTrue(darkCss.contains(".post-content ul li p,\n.post-content ol li p {\n  margin: 9px 0;\n}"));
        assertTrue(darkCss.contains("--nav-link: #e5ebf3;"));
        assertTrue(darkCss.contains("--nav-link-hover: #ffffff;"));
        assertTrue(darkCss.contains("--link: #df7f6d;"));
        assertTrue(darkCss.contains("--link-hover: #ecab9f;"));
        assertTrue(darkCss.contains("--focus-ring: #c9533b;"));
        assertTrue(darkCss.contains(".archive-group-posts a {\n  color: var(--link);\n}"));
        assertTrue(darkCss.contains(".post-card a {"));
        assertTrue(darkCss.contains("color: var(--text);"));
        assertTrue(darkCss.contains(".post-card a:hover,"));
        assertTrue(darkCss.contains(".post-card a:focus-visible {"));
        assertTrue(darkCss.contains("color: var(--link-hover);"));
        assertTrue(darkCss.contains("#navbar:not(.navbar--hero) .nav-left a {\n  color: var(--nav-link);\n}"));
        assertTrue(darkCss.contains("#navbar:not(.navbar--hero) .nav-left a:hover,\n#navbar:not(.navbar--hero) .nav-left a:focus-visible {\n  color: var(--nav-link-hover);\n}"));
        assertTrue(darkCss.contains(".post-card .teaser-tags a,\n.post-tags a {\n  color: var(--link);\n}"));
        assertTrue(darkCss.contains(".post-content p a {\n  color: var(--link);\n}"));
        assertTrue(darkCss.contains("outline: 2px solid var(--focus-ring);"));
        assertFalse(darkCss.contains("rgb(0, 133, 161)"));
        assertFalse(darkCss.contains("#2a6496"));
        assertTrue(darkCss.contains("#content.page-home {\n  margin-top: 0;\n}"));
        assertFalse(darkCss.contains("#content.page-home {\n  width: min(1200px, calc(100% - 2rem));"));
        assertTrue(darkCss.contains(".home-hero {"));
        assertTrue(darkCss.contains("width: 100%;"));
        assertTrue(darkCss.contains("background-position: center center;"));
        assertTrue(darkCss.contains(".home-hero__overlay {\n  display: flex;\n  flex-direction: column;\n  justify-content: flex-start;"));
        assertTrue(darkCss.contains(".home-hero__copy {\n  display: flex;\n  flex: 1 1 auto;\n  align-items: center;\n  justify-content: center;\n  min-height: 0;\n  padding: 1rem;\n}"));
        assertTrue(darkCss.contains(".brand {"));
        assertTrue(darkCss.contains("font-size: clamp(2.5rem, 5.25vw, 5.6rem);"));
        assertTrue(darkCss.contains(".brand-main {"));
        assertTrue(darkCss.contains(".brand-domain {"));
        assertTrue(darkCss.contains(".brand:hover,\n.brand:focus-visible {"));
        assertTrue(darkCss.contains("#navbar.navbar--hero .nav-left a {"));
        assertTrue(darkCss.contains("text-decoration: underline;"));
        assertTrue(darkCss.contains("#navbar.navbar--hero #search-input::placeholder {"));
        assertTrue(darkCss.contains("#navbar.navbar--hero .theme-toggle {"));
        assertTrue(darkCss.contains("#navbar.navbar--hero #search-form {"));
        assertFalse(darkCss.contains("border-radius: 1.3rem;"));
        assertFalse(darkCss.contains("box-shadow: 0 1.2rem 3rem rgba(0, 0, 0, 0.42);"));
        assertFalse(darkCss.contains("#navbar.navbar--hero .nav-left a:hover,\n#navbar.navbar--hero .nav-left a:focus-visible {\n  background:"));
        assertTrue(darkCss.contains("color: var(--muted);"));
        assertTrue(darkCss.contains("text-decoration: none;"));
        assertTrue(darkCss.contains("color: currentColor;"));
        assertTrue(darkCss.contains("white-space: nowrap;"));
        assertTrue(darkCss.contains("display: inline-block;"));
        assertTrue(darkCss.contains("vertical-align: -0.125em;"));
        assertFalse(darkCss.contains(".post-meta-item {\n  display: inline-flex;"));
        assertFalse(darkCss.contains(".post-meta-item {\n  align-items: baseline;"));
    }

    private void assertGroupedArchive(Path output) throws Exception {
        String archive = Files.readString(output.resolve("archive.html"), StandardCharsets.UTF_8);
        Document archiveDoc = Jsoup.parse(archive);

        Element archiveHeading = archiveDoc.selectFirst("h1");
        assertNotNull(archiveHeading);
        assertEquals("Blog Archive", archiveHeading.text());
        assertEquals(List.of("March 2026", "January 2026"), archiveDoc.select(".archive-group-heading").eachText());
        assertEquals(
            List.of("17 - Third Post", "16 - Fourth Post"),
            archiveDoc.select(".archive-group").get(0).select("li").eachText()
        );
        assertEquals(
            List.of("13 - Second Post", "12 - First Post"),
            archiveDoc.select(".archive-group").get(1).select("li").eachText()
        );
        assertEquals(
            "/2026/post-three/",
            archiveDoc.select(".archive-group").get(0).selectFirst("a").attr("href")
        );
        assertEquals(
            List.of("Third Post", "Fourth Post", "Second Post", "First Post"),
            archiveDoc.select(".archive-group-posts a.post-title").eachText()
        );
    }

    private void assertCommonFeed(Path output) throws Exception {
        String feed = Files.readString(output.resolve("feed.xml"), StandardCharsets.UTF_8);
        assertTrue(feed.contains("<rss version=\"2.0\""));
        assertTrue(feed.contains("atom:link"));
        assertTrue(feed.contains("https://example.com/feed.xml"));
        assertTrue(feed.contains("<guid isPermaLink=\"false\">2026/post-one/</guid>"));
        assertTrue(feed.contains("<link>https://example.com/2026/post-two/</link>"));
        assertTrue(feed.contains("<img src=\"https://example.com/2026/images/cover.png\""));
        assertTrue(feed.contains("defaultSrsCode=2056"));
    }

    private void assertCommonSearchIndex(Path output) throws Exception {
        String searchIndex = Files.readString(output.resolve("assets/search-index.json"), StandardCharsets.UTF_8);
        assertTrue(searchIndex.contains("\"title\":\"Second Post\""));
        assertTrue(searchIndex.contains("\"url\":\"/2026/post-one/\""));
    }

    private void assertIndexTeasers(Path output) throws Exception {
        String indexHtml = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        Document indexDoc = Jsoup.parse(indexHtml);

        assertEquals(List.of("[read more]", "[read more]", "[read more]", "[read more]"), indexDoc.select(".teaser-more").eachText());
        assertFalse(indexHtml.contains(">...</a>"));

        Element firstPostCard = findPostCard(indexDoc, "First Post");
        assertNotNull(firstPostCard);
        assertEquals("Tags: #Java #AI", firstPostCard.selectFirst(".teaser-tags").text());
        assertEquals(List.of("#Java", "#AI"), firstPostCard.select(".teaser-tags a").eachText());
        assertEquals("/tags/java/index.html", firstPostCard.select(".teaser-tags a").get(0).attr("href"));
        assertEquals("/tags/ai/index.html", firstPostCard.select(".teaser-tags a").get(1).attr("href"));

        Element secondPostCard = findPostCard(indexDoc, "Second Post");
        assertNotNull(secondPostCard);
        assertEquals("Tags: #AI", secondPostCard.selectFirst(".teaser-tags").text());
        assertEquals(List.of("#AI"), secondPostCard.select(".teaser-tags a").eachText());
        assertEquals("/tags/ai/index.html", secondPostCard.select(".teaser-tags a").get(0).attr("href"));

        Element thirdPostCard = findPostCard(indexDoc, "Third Post");
        assertNotNull(thirdPostCard);
        assertTrue(thirdPostCard.select(".teaser-tags").isEmpty());

        Element fourthPostCard = findPostCard(indexDoc, "Fourth Post");
        assertNotNull(fourthPostCard);
        assertTrue(fourthPostCard.select(".teaser-tags").isEmpty());
    }

    private void assertHomeHero(Path output, String expectedBrandMain, String expectedBrandDomain) throws Exception {
        String indexHtml = Files.readString(output.resolve("index.html"), StandardCharsets.UTF_8);
        Document indexDoc = Jsoup.parse(indexHtml);

        Element hero = indexDoc.selectFirst(".home-hero");
        assertNotNull(hero);
        assertTrue(hero.attr("style").contains("/assets/home-hero.jpg"));
        assertNotNull(indexDoc.selectFirst(".home-hero__overlay"));
        assertEquals(1, indexDoc.select("body > header.home-hero + main#content.page-home").size());
        assertEquals(1, indexDoc.select("body > main#content.page-home > .post-grid").size());
        assertTrue(indexDoc.select("main#content .home-hero").isEmpty());

        Element heroNav = indexDoc.selectFirst("#navbar.navbar--hero");
        assertNotNull(heroNav);
        assertEquals(List.of("Home", "Archive", "Subscribe"), heroNav.select(".nav-left a").eachText());
        assertEquals("/index.html", heroNav.select(".nav-left a").get(0).attr("href"));
        assertEquals("/archive.html", heroNav.select(".nav-left a").get(1).attr("href"));
        assertEquals("/feed.xml", heroNav.select(".nav-left a").get(2).attr("href"));
        assertEquals(1, heroNav.select("#search-form").size());
        assertEquals(1, heroNav.select("#search-input").size());
        assertEquals("Search posts", heroNav.selectFirst("#search-input").attr("placeholder"));
        assertEquals(1, heroNav.select("#theme-toggle").size());

        Element heroCopy = indexDoc.selectFirst(".home-hero__copy");
        assertNotNull(heroCopy);
        Element brand = heroCopy.selectFirst("a.brand");
        assertNotNull(brand);
        assertEquals("/index.html", brand.attr("href"));
        assertEquals(expectedBrandMain + expectedBrandDomain, brand.text());
        assertEquals(expectedBrandMain, brand.selectFirst(".brand-main").text());
        if (expectedBrandDomain.isBlank()) {
            assertTrue(brand.select(".brand-domain").isEmpty());
        } else {
            Element brandDomain = brand.selectFirst(".brand-domain");
            assertNotNull(brandDomain);
            assertEquals(expectedBrandDomain, brandDomain.text());
        }
        assertTrue(indexDoc.select(".home-hero__title").isEmpty());
    }

    private Element findPostCard(Document document, String title) {
        for (Element postCard : document.select(".post-card")) {
            Element titleLink = postCard.selectFirst(".post-title a");
            if (titleLink != null && title.equals(titleLink.text())) {
                return postCard;
            }
        }
        return null;
    }

    private void assertPostFooter(Path output) throws Exception {
        String postHtml = Files.readString(output.resolve("2026/post-one/index.html"), StandardCharsets.UTF_8);
        assertTrue(postHtml.contains("id=\"navbar\""));
        assertTrue(postHtml.contains("id=\"search-input\""));
        assertTrue(postHtml.contains("id=\"theme-toggle\""));
        assertFalse(postHtml.contains("fonts.googleapis.com"));
        assertFalse(postHtml.contains("fonts.gstatic.com"));
        assertFalse(postHtml.contains("navbar--hero"));
        assertFalse(postHtml.contains("class=\"home-hero\""));
        assertTrue(postHtml.contains("/assets/prism/prism.css"));
        assertTrue(postHtml.contains("/assets/prism/prism.js"));
        assertTrue(postHtml.contains("/assets/prism/components/prism-interlis.js"));
        assertTrue(postHtml.contains("/assets/prism/plugins/line-highlight/prism-line-highlight.min.js"));
        assertTrue(postHtml.contains("/assets/prism/plugins/line-numbers/prism-line-numbers.min.js"));
        assertTrue(postHtml.contains("/assets/code-copy.js"));
        assertTrue(postHtml.contains("/assets/image-lightbox.js"));
        assertTrue(postHtml.contains("/assets/zurich.css"));
        assertTrue(postHtml.contains("Written by Alice Author"));

        Document postDoc = Jsoup.parse(postHtml);
        assertEquals(1, postDoc.select("pre.language-ini.line-numbers > code.language-ini").size());
        assertEquals(1, postDoc.select("#image-lightbox").size());
        assertEquals(1, postDoc.select("#image-lightbox-image").size());
        assertEquals(1, postDoc.select("#image-lightbox-close").size());
        assertEquals(List.of("Example Link"), postDoc.select(".post-content p a").eachText());
        assertEquals("https://example.com", postDoc.selectFirst(".post-content p a").attr("href"));
        assertEquals(List.of("First bullet item.", "Second bullet item."), postDoc.select(".post-content ul li p").eachText());

        Element footer = postDoc.selectFirst(".post-footer");
        assertNotNull(footer);
        assertEquals(List.of("#Java", "#AI"), footer.select(".post-tags a").eachText());
        assertEquals("/tags/java/index.html", footer.select(".post-tags a").get(0).attr("href"));
        assertTrue(footer.text().contains("Written by Alice Author | #Java #AI"));
        assertTrue(footer.select("a.tag").isEmpty());

        String searchJs = Files.readString(output.resolve("assets/search.js"), StandardCharsets.UTF_8);
        assertTrue(searchJs.contains("lunrSearch"));

        String codeCopyJs = Files.readString(output.resolve("assets/code-copy.js"), StandardCharsets.UTF_8);
        assertTrue(codeCopyJs.contains("bi bi-copy"));
        assertTrue(codeCopyJs.contains("bi bi-check"));
        assertTrue(codeCopyJs.contains("setTemporarySuccessState"));
        assertTrue(codeCopyJs.contains("window.setTimeout"));

        String imageLightboxJs = Files.readString(output.resolve("assets/image-lightbox.js"), StandardCharsets.UTF_8);
        assertTrue(imageLightboxJs.contains("aria-haspopup"));
        assertTrue(imageLightboxJs.contains("showModal"));
        assertTrue(imageLightboxJs.contains("closest(\"a\")"));
        assertTrue(imageLightboxJs.contains("dialog.addEventListener(\"close\""));
    }

    private void writeSampleSite(Path input, boolean enableIndexThumbnails) throws Exception {
        writeSampleSite(input, enableIndexThumbnails, "Thoth Blog");
    }

    private void writeSampleSite(Path input, boolean enableIndexThumbnails, String siteTitle) throws Exception {
        String thumbnailConfig = enableIndexThumbnails ? "site.indexThumbnails.enabled=true\n" : "";

        write(input.resolve("thoth.properties"), """
            site.title=%s
            site.description=Demo feed
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9090
            """.formatted(siteTitle) + thumbnailConfig);

        write(input.resolve("blog/2026/post-one.adoc"), """
            ---
            = First Post
            Alice Author
            2026-01-12
            :thoth-status: published
            :thoth-tags: Java,AI
            ---
            image::images/cover.png[Cover]

            [source,ini,linenums]
            ----
            [ch.ehi.ili2db]
            defaultSrsCode=2056
            createEnumTabs=true
            ----

            First post paragraph with enough content to form a teaser and an https://example.com[Example Link].

            * First bullet item.
            * Second bullet item.
            """);

        write(input.resolve("blog/2026/post-two.adoc"), """
            ---
            = Second Post
            Bob Builder
            2026-01-13
            :thoth-status: published
            :thoth-tags: AI
            :thoth-teaser: Manual teaser override
            ---
            Some second post body text.
            """);

        write(input.resolve("blog/2026/post-three.adoc"), """
            ---
            = Third Post
            Carla Creator
            2026-03-17
            :thoth-status: published
            ---
            Third post body text.
            """);

        write(input.resolve("blog/2026/post-four.adoc"), """
            ---
            = Fourth Post
            Dana Developer
            2026-03-16
            :thoth-status: published
            ---
            Fourth post body text.
            """);

        writeBinary(
            input.resolve("blog/2026/images/cover.png"),
            Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5l5lEAAAAASUVORK5CYII=")
        );
        write(input.resolve("blog/2026/site.js"), "console.log('ok');");
        write(input.resolve(".DS_Store"), "ignored");
        write(input.resolve("blog/.DS_Store"), "ignored");
        write(input.resolve(".git/objects/aa/object"), "ignored");
        write(input.resolve(".idea/workspace.xml"), "ignored");
        write(input.resolve(".vscode/settings.json"), "ignored");
        write(input.resolve("node_modules/some-package/index.js"), "ignored");
        write(input.resolve("build/generated/asset.txt"), "ignored");
        write(input.resolve("target/generated/asset.txt"), "ignored");
        write(input.resolve(".gradle/metadata.bin"), "ignored");
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeBinary(Path path, byte[] content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }
}
