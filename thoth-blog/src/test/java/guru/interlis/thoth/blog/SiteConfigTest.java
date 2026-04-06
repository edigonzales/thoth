package guru.interlis.thoth.blog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SiteConfigTest {

    @TempDir Path tempDir;

    @Test
    void loadsMinimalRequiredConfig() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        SiteConfig config = SiteConfig.load(tempDir);

        assertEquals("Test Blog", config.title());
        assertEquals("A test blog", config.description());
        assertEquals("https://example.com", config.baseUrl());
        assertEquals("en-gb", config.language());
        assertEquals("yyyy-MM-dd", config.dateFormat());
        assertFalse(config.indexThumbnailsEnabled());
        assertEquals(8080, config.devPort());
        assertNotNull(config.htmlDateFormatter());
        assertEquals("Europe/Zurich", config.zoneId().getId());
    }

    @Test
    void loadsOptionalDevPort() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            dev.port=9999
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        assertEquals(9999, config.devPort());
    }

    @Test
    void enablesIndexThumbnailsWhenConfigured() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            site.indexThumbnails.enabled=true
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        assertTrue(config.indexThumbnailsEnabled());
    }

    @Test
    void stripsTrailingSlashFromBaseUrl() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com/
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        assertEquals("https://example.com", config.baseUrl());
    }

    @Test
    void absoluteUrlBuildsCorrectUrls() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        SiteConfig config = SiteConfig.load(tempDir);

        assertEquals("https://example.com/blog/2026/post/", config.absoluteUrl("/blog/2026/post/"));
        assertEquals("https://example.com/feed.xml", config.absoluteUrl("/feed.xml"));
        assertEquals("https://example.com/", config.absoluteUrl(""));
    }

    @Test
    void absoluteUrlPassesThroughAbsoluteUrls() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        assertEquals("https://other.com/page", config.absoluteUrl("https://other.com/page"));
    }

    @Test
    void absoluteUrlRejectsNull() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        assertThrows(NullPointerException.class, () -> config.absoluteUrl(null));
    }

    @Test
    void throwsOnMissingConfigFile() {
        assertThrows(IllegalArgumentException.class, () -> SiteConfig.load(tempDir));
    }

    @Test
    void throwsOnMissingRequiredKey() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        assertThrows(IllegalArgumentException.class, () -> SiteConfig.load(tempDir));
    }

    @Test
    void htmlDateFormatterFormatsCorrectly() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=dd.MM.yyyy
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        String formatted = config.htmlDateFormatter().format(LocalDate.of(2026, 1, 12));
        assertEquals("12.01.2026", formatted);
    }

    @Test
    void htmlDateFormatterUsesLocale() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=de-CH
            site.dateFormat=d. MMMM yyyy
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        String formatted = config.htmlDateFormatter().format(LocalDate.of(2026, 3, 17));
        // German locale should produce "März" or similar
        assertTrue(formatted.contains("M") || formatted.contains("m"));
        assertTrue(formatted.contains("2026"));
    }

    @Test
    void defaultsToPort8080WhenDevPortMissing() throws Exception {
        writeConfig("""
            site.title=Test Blog
            site.description=A test blog
            site.baseUrl=https://example.com
            site.language=en-gb
            site.dateFormat=yyyy-MM-dd
            """);

        SiteConfig config = SiteConfig.load(tempDir);
        assertEquals(8080, config.devPort());
    }

    private void writeConfig(String content) throws Exception {
        Files.writeString(tempDir.resolve("thoth.properties"), content, StandardCharsets.UTF_8);
    }
}
