package guru.interlis.thoth.biblios.config;

/**
 * UI configuration options.
 */
public final class UiSection {
    private final String theme;
    private final boolean showVersionBadge;
    private final boolean showEditLink;
    private final boolean showSourceLink;
    private final String editUrlPattern;
    private final String sourceUrlPattern;
    private final VersionSwitchMode versionSwitchMode;
    private final SearchLanguageMode searchLanguageMode;
    private final int sidebarTocDepth;
    private final ContentTocMode contentToc;

    public UiSection(String theme, boolean showVersionBadge, boolean showEditLink,
                     boolean showSourceLink, String editUrlPattern, String sourceUrlPattern,
                     VersionSwitchMode versionSwitchMode, SearchLanguageMode searchLanguageMode) {
        this(theme, showVersionBadge, showEditLink, showSourceLink, editUrlPattern, sourceUrlPattern,
            versionSwitchMode, searchLanguageMode, 2, ContentTocMode.OFF);
    }

    public UiSection(String theme, boolean showVersionBadge, boolean showEditLink,
                     boolean showSourceLink, String editUrlPattern, String sourceUrlPattern,
                     VersionSwitchMode versionSwitchMode, SearchLanguageMode searchLanguageMode,
                     int sidebarTocDepth, ContentTocMode contentToc) {
        this.theme = theme != null ? theme : "default";
        this.showVersionBadge = showVersionBadge;
        this.showEditLink = showEditLink;
        this.showSourceLink = showSourceLink;
        this.editUrlPattern = editUrlPattern;
        this.sourceUrlPattern = sourceUrlPattern;
        this.versionSwitchMode = versionSwitchMode != null ? versionSwitchMode : VersionSwitchMode.START_PAGE;
        this.searchLanguageMode = searchLanguageMode != null ? searchLanguageMode : SearchLanguageMode.MULTILINGUAL_SAFE;
        this.sidebarTocDepth = Math.max(1, Math.min(6, sidebarTocDepth));
        this.contentToc = contentToc != null ? contentToc : ContentTocMode.OFF;
    }

    public String theme() {
        return theme;
    }

    public boolean showVersionBadge() {
        return showVersionBadge;
    }

    public boolean showEditLink() {
        return showEditLink;
    }

    public boolean showSourceLink() {
        return showSourceLink;
    }

    /**
     * URL pattern for edit links.
     * Placeholders: {repo_url}, {branch}, {path}
     * Example: {repo_url}/edit/{branch}/{path}
     * May be null if not configured.
     */
    public String editUrlPattern() {
        return editUrlPattern;
    }

    /**
     * URL pattern for source links.
     * Placeholders: {repo_url}, {branch}, {path}
     * Example: {repo_url}/blob/{branch}/{path}
     * May be null if not configured.
     */
    public String sourceUrlPattern() {
        return sourceUrlPattern;
    }

    public VersionSwitchMode versionSwitchMode() {
        return versionSwitchMode;
    }

    public SearchLanguageMode searchLanguageMode() {
        return searchLanguageMode;
    }

    public int sidebarTocDepth() {
        return sidebarTocDepth;
    }

    public ContentTocMode contentToc() {
        return contentToc;
    }

    @Override
    public String toString() {
        return "UiSection{theme='" + theme + "', versionSwitchMode=" + versionSwitchMode +
            ", searchLanguageMode=" + searchLanguageMode +
            ", sidebarTocDepth=" + sidebarTocDepth +
            ", contentToc=" + contentToc + "}";
    }
}
