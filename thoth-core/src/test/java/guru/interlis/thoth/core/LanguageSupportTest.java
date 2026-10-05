package guru.interlis.thoth.core;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LanguageSupportTest {
    @Test
    void resolvesLanguageDirectoriesAndLegacyPaths() {
        LanguageSupport languages = new LanguageSupport("de", List.of("de", "en"), true);
        assertEquals("/2026/post/", languages.route("de", "2026/post/"));
        assertEquals("/en/2026/post/", languages.route("en", "2026/post/"));
        assertEquals(Path.of("2026/post.adoc"), languages.outputPath(Path.of("de/2026/post.adoc")));
        assertEquals(Path.of("en/2026/post.adoc"), languages.outputPath(Path.of("en/2026/post.adoc")));
        assertEquals("de", languages.sourcePath(Path.of("2026/post.adoc")).language());
    }

    @Test
    void preservesLegacyLanguageLikeDirectoriesUnlessOptedIn() {
        LanguageSupport languages = new LanguageSupport("en-gb", List.of("en-GB"), false);
        assertEquals("en-GB", languages.defaultLanguage());
        assertEquals(Path.of("en-GB/post.adoc"), languages.outputPath(Path.of("en-GB/post.adoc")));
        assertEquals("/", languages.route("en-gb", ""));
    }

    @Test
    void rejectsInvalidDuplicateAndMissingDefaultLanguages() {
        assertThrows(IllegalArgumentException.class, () -> LanguageSupport.normalize("de_bad"));
        assertThrows(IllegalArgumentException.class, () -> LanguageSupport.normalize(""));
        assertThrows(IllegalArgumentException.class, () -> new LanguageSupport("de", List.of("en"), true));
        assertThrows(IllegalArgumentException.class, () -> new LanguageSupport("de", List.of("de", "DE"), true));
        assertThrows(IllegalArgumentException.class, () -> new LanguageSupport("de", List.of("de", ""), true));
    }

    @Test
    void rejectsPathsOutsideTheSiteAndUnknownLanguages() {
        LanguageSupport languages = new LanguageSupport("de", List.of("de", "en"), true);
        assertThrows(IllegalArgumentException.class, () -> languages.outputPath(Path.of("../secret")));
        assertThrows(IllegalArgumentException.class, () -> languages.route("en", "../../secret"));
        assertThrows(IllegalArgumentException.class, () -> languages.route("fr", "post/"));
    }
}
