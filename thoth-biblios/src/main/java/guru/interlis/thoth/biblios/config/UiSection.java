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

    public UiSection(String theme, boolean showVersionBadge, boolean showEditLink,
                     boolean showSourceLink, String editUrlPattern, String sourceUrlPattern) {
        this.theme = theme != null ? theme : "default";
        this.showVersionBadge = showVersionBadge;
        this.showEditLink = showEditLink;
        this.showSourceLink = showSourceLink;
        this.editUrlPattern = editUrlPattern;
        this.sourceUrlPattern = sourceUrlPattern;
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

    @Override
    public String toString() {
        return "UiSection{theme='" + theme + "'}";
    }
}
