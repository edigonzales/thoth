package guru.interlis.thoth.core;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class InterlisLabHtmlSupport {
    private InterlisLabHtmlSupport() {
    }

    public static boolean hasInterlisLab(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        return org.jsoup.Jsoup.parseBodyFragment(html).selectFirst("interlis-lab") != null;
    }

    public static boolean applyDefaultAttributes(Document document, String ili2cJarUrl, String cheerpjLoaderUrl) {
        if (document == null) {
            return false;
        }

        boolean changed = false;
        for (Element lab : document.select("interlis-lab")) {
            if (ili2cJarUrl != null && !ili2cJarUrl.isBlank() && !lab.hasAttr("ili2c-jar-url")) {
                lab.attr("ili2c-jar-url", ili2cJarUrl);
                changed = true;
            }
            if (cheerpjLoaderUrl != null && !cheerpjLoaderUrl.isBlank() && !lab.hasAttr("cheerpj-loader-url")) {
                lab.attr("cheerpj-loader-url", cheerpjLoaderUrl);
                changed = true;
            }
        }
        return changed;
    }
}
