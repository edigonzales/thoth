package guru.interlis.thoth.biblios.config;

import java.util.Objects;

/**
 * Global site metadata.
 */
public final class SiteSection {
    private final String title;
    private final String url;
    private final String defaultLanguage;
    private final String defaultComponent;
    private final String defaultVersion;

    public SiteSection(String title, String url, String defaultLanguage, String defaultComponent, String defaultVersion) {
        this.title = Objects.requireNonNull(title, "site.title is required");
        this.url = url;
        this.defaultLanguage = defaultLanguage != null ? defaultLanguage : "en";
        this.defaultComponent = defaultComponent;
        this.defaultVersion = defaultVersion;
    }

    public String title() {
        return title;
    }

    public String url() {
        return url;
    }

    public String defaultLanguage() {
        return defaultLanguage;
    }

    public String defaultComponent() {
        return defaultComponent;
    }

    public String defaultVersion() {
        return defaultVersion;
    }

    @Override
    public String toString() {
        return "SiteSection{title='" + title + "', url='" + url + "'}";
    }
}
