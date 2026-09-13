package guru.interlis.thoth.biblios.nav;

import org.jsoup.Jsoup;

/**
 * Utilities for converting rendered navigation titles to visible text.
 */
public final class NavigationText {

    private NavigationText() {
    }

    /**
     * Extract the visible text from a title that may contain rendered HTML
     * inline markup, such as {@code <code>} or {@code <strong>} elements.
     */
    public static String plainText(String renderedTitle) {
        if (renderedTitle == null || renderedTitle.isBlank()) {
            return renderedTitle != null ? renderedTitle : "";
        }
        return Jsoup.parseBodyFragment(renderedTitle).text();
    }
}
