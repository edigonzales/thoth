package guru.interlis.thoth.biblios.config;

/**
 * UI configuration options.
 */
public final class UiSection {
    private final String theme;
    private final boolean showVersionBadge;
    private final boolean showEditLink;

    public UiSection(String theme, boolean showVersionBadge, boolean showEditLink) {
        this.theme = theme != null ? theme : "default";
        this.showVersionBadge = showVersionBadge;
        this.showEditLink = showEditLink;
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

    @Override
    public String toString() {
        return "UiSection{theme='" + theme + "'}";
    }
}
