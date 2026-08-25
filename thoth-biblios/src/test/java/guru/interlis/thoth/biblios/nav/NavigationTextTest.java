package guru.interlis.thoth.biblios.nav;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NavigationTextTest {

    @Test
    void extractsVisibleTextFromInlineMarkup() {
        assertEquals(
            "Gemeinsames shared/Jenkinsfile fett kursiv Link",
            NavigationText.plainText(
                "Gemeinsames <code>shared/Jenkinsfile</code> " +
                    "<strong>fett</strong> <em>kursiv</em> " +
                    "<a href=\"https://example.org\">Link</a>"
            )
        );
    }

    @Test
    void extractsVisibleTextFromNestedInlineMarkup() {
        assertEquals(
            "Äußerer innerer Text",
            NavigationText.plainText("<strong>Äußerer <code>innerer</code></strong> Text")
        );
    }

    @Test
    void keepsPlainTextUnchanged() {
        assertEquals("Plain title", NavigationText.plainText("Plain title"));
    }
}
