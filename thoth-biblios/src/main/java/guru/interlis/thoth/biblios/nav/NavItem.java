package guru.interlis.thoth.biblios.nav;

import java.util.List;
import java.util.Objects;

/**
 * A single navigation entry. May have child entries.
 */
public final class NavItem {
    private final String title;
    private final String rawTitle;
    private final String page;
    private final List<NavItem> children;

    public NavItem(String title, String page, List<NavItem> children) {
        this(title, page, children, title);
    }

    public NavItem(String title, String page, List<NavItem> children, String rawTitle) {
        this.title = Objects.requireNonNull(title, "nav item title is required");
        this.rawTitle = rawTitle != null && !rawTitle.isBlank() ? rawTitle : title;
        this.page = page;
        this.children = children != null ? List.copyOf(children) : List.of();
    }

    /**
     * Display title for this navigation entry.
     */
    public String title() {
        return title;
    }

    /**
     * Raw title without presentation-time TOC number prefixes.
     */
    public String rawTitle() {
        return rawTitle;
    }

    /**
     * Relative page path (e.g. "index.adoc"). Null for group entries with children.
     */
    public String page() {
        return page;
    }

    /**
     * Child navigation entries. Empty list if leaf node.
     */
    public List<NavItem> children() {
        return children;
    }

    /**
     * Whether this is a group entry (has children).
     */
    public boolean isGroup() {
        return !children.isEmpty();
    }

    @Override
    public String toString() {
        if (isGroup()) {
            return "NavItem{title='" + title + "', children=" + children.size() + "}";
        }
        return "NavItem{title='" + title + "', page='" + page + "'}";
    }
}
