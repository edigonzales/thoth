package guru.interlis.thoth.biblios.server.view;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FrameRendererTest {

    private final FrameRenderer renderer =
        new FrameRenderer(new FreeMarkerServerConfiguration().freeMarkerConfiguration());

    @Test
    void rendersClasspathTemplate() {
        String html = renderer.render("search.ftl", Map.of(
            "siteTitle", "Test Portal",
            "basePath", "",
            "siteRootHref", "/",
            "searchIndexUrl", "/api/search-index"
        ));

        assertTrue(html.contains("<title>Search - Test Portal</title>"), html);
        assertTrue(html.contains("id=\"search-results\""), html);
        assertFalse(html.contains("class=\"login-link\""), "Static models must not render a login element");
    }
}
