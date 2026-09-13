package guru.interlis.thoth.biblios.nav;

import java.util.List;
import java.util.Objects;

/**
 * Root navigation tree for a documentation version.
 */
public final class NavTree {
    private final List<NavItem> items;

    public NavTree(List<NavItem> items) {
        Objects.requireNonNull(items, "nav items are required");
        this.items = List.copyOf(items);
    }

    public List<NavItem> items() {
        return items;
    }

    /**
     * Find a nav item by its page path (recursive search).
     *
     * @param pagePath relative page path like "index.adoc"
     * @return the matching nav item, or null if not found
     */
    public NavItem findByPage(String pagePath) {
        return findInList(items, pagePath);
    }

    private NavItem findInList(List<NavItem> items, String pagePath) {
        for (NavItem item : items) {
            if (pagePath.equals(item.page())) {
                return item;
            }
            NavItem found = findInList(item.children(), pagePath);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Build breadcrumb path for a given page.
     *
     * @param pagePath relative page path
     * @return list of nav items from root to the page, or empty list if not found
     */
    public List<NavItem> buildBreadcrumbs(String pagePath) {
        return buildBreadcrumbsInList(items, pagePath);
    }

    private List<NavItem> buildBreadcrumbsInList(List<NavItem> items, String pagePath) {
        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            if (pagePath.equals(item.page())) {
                return List.of(item);
            }
            if (item.isGroup()) {
                List<NavItem> childPath = buildBreadcrumbsInList(item.children(), pagePath);
                if (!childPath.isEmpty()) {
                    // Prepend current group to the path
                    java.util.ArrayList<NavItem> result = new java.util.ArrayList<>();
                    result.add(item);
                    result.addAll(childPath);
                    return List.copyOf(result);
                }
            }
        }
        return List.of();
    }

    /**
     * Find the previous nav item for a given page.
     */
    public NavItem findPrev(String pagePath) {
        List<NavItem> flat = flatten(items);
        int index = indexOf(flat, pagePath);
        if (index <= 0) {
            return null;
        }
        return flat.get(index - 1);
    }

    /**
     * Find the next nav item for a given page.
     */
    public NavItem findNext(String pagePath) {
        List<NavItem> flat = flatten(items);
        int index = indexOf(flat, pagePath);
        if (index < 0 || index >= flat.size() - 1) {
            return null;
        }
        return flat.get(index + 1);
    }

    private List<NavItem> flatten(List<NavItem> items) {
        java.util.ArrayList<NavItem> result = new java.util.ArrayList<>();
        for (NavItem item : items) {
            if (!item.isGroup() || item.page() != null) {
                result.add(item);
            }
            result.addAll(flatten(item.children()));
        }
        return result;
    }

    private int indexOf(List<NavItem> flat, String pagePath) {
        for (int i = 0; i < flat.size(); i++) {
            if (pagePath.equals(flat.get(i).page())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "NavTree{items=" + items.size() + "}";
    }
}
