package guru.interlis.thoth.biblios.nav;

import java.util.List;
import java.util.Objects;

/**
 * A single navigation entry. May have child entries.
 */
public final class NavItem {
    private final String title;
    private final String page;
    private final List<NavItem> children;

    public NavItem(String title, String page, List<NavItem> children) {
        this.title = Objects.requireNonNull(title, "nav item title is required");
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
