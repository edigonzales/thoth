package guru.interlis.thoth.blog;

import guru.interlis.thoth.core.LanguageSupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public final class SiteConfig {
    public static final String FILE_NAME = "thoth.properties";
    public static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

    private final String title;
    private final String description;
    private final String baseUrl;
    private final String language;
    private final Locale locale;
    private final String dateFormat;
    private final boolean indexThumbnailsEnabled;
    private final int devPort;
    private final DateTimeFormatter htmlDateFormatter;
    private final LanguageSupport languageSupport;
    private final Properties properties;

    private SiteConfig(
        String title,
        String description,
        String baseUrl,
        String language,
        Locale locale,
        String dateFormat,
        boolean indexThumbnailsEnabled,
        int devPort,
        DateTimeFormatter htmlDateFormatter,
        LanguageSupport languageSupport,
        Properties properties
    ) {
        this.title = title;
        this.description = description;
        this.baseUrl = baseUrl;
        this.language = language;
        this.locale = locale;
        this.dateFormat = dateFormat;
        this.indexThumbnailsEnabled = indexThumbnailsEnabled;
        this.devPort = devPort;
        this.htmlDateFormatter = htmlDateFormatter;
        this.languageSupport = languageSupport;
        this.properties = properties;
    }

    public static SiteConfig load(Path inputRoot) throws IOException {
        Path configPath = inputRoot.resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            throw new IllegalArgumentException("Missing required config file: " + configPath);
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        }

        String title = required(properties, "site.title");
        String description = required(properties, "site.description");
        String baseUrl = stripTrailingSlash(required(properties, "site.baseUrl"));
        String language = required(properties, "site.language");
        String dateFormat = required(properties, "site.dateFormat");
        boolean indexThumbnailsEnabled = Boolean.parseBoolean(
            properties.getProperty("site.indexThumbnails.enabled", "false").trim()
        );

        int devPort = Integer.parseInt(properties.getProperty("dev.port", "8080").trim());
        Locale locale = Locale.forLanguageTag(language);
        if (locale.getLanguage().isBlank()) {
            locale = Locale.ENGLISH;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat, locale);
        List<String> languages = Arrays.asList(properties.getProperty("site.languages", language).split(",", -1));
        LanguageSupport support = new LanguageSupport(language, languages, properties.containsKey("site.languages"));
        return new SiteConfig(title, description, baseUrl, language, locale, dateFormat, indexThumbnailsEnabled, devPort,
            formatter, support, properties);
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String language() {
        return language;
    }

    public Locale locale() {
        return locale;
    }

    public LanguageSupport languageSupport() { return languageSupport; }
    public List<String> languages() { return languageSupport.languages(); }
    public String defaultLanguage() { return languageSupport.defaultLanguage(); }
    public String route(String language, String path) { return languageSupport.route(language, path); }
    public String title(String language) { return localizedProperty("site.title", language, title); }
    public String description(String language) { return localizedProperty("site.description", language, description); }
    public DateTimeFormatter htmlDateFormatter(String language) {
        return DateTimeFormatter.ofPattern(dateFormat, Locale.forLanguageTag(language));
    }

    private String localizedProperty(String key, String language, String fallback) {
        String value = properties.getProperty(key + "." + language);
        if (value == null) {
            value = properties.getProperty(key + "." + language.toLowerCase(Locale.ROOT));
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String dateFormat() {
        return dateFormat;
    }

    public boolean indexThumbnailsEnabled() {
        return indexThumbnailsEnabled;
    }

    public int devPort() {
        return devPort;
    }

    public DateTimeFormatter htmlDateFormatter() {
        return htmlDateFormatter;
    }

    public ZoneId zoneId() {
        return ZURICH;
    }

    public String absoluteUrl(String path) {
        Objects.requireNonNull(path, "path must not be null");
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return baseUrl + normalized;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value.trim();
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
