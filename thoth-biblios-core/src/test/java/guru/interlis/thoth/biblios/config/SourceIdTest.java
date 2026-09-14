package guru.interlis.thoth.biblios.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SourceIdTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;

    @ParameterizedTest
    @ValueSource(strings = {"site-assets", "SITE-ASSETS", "Site-Assets"})
    void yamlLoadingRejectsReservedSourceIds(String id) throws Exception {
        var file = root.resolve("biblios.yml");
        java.nio.file.Files.writeString(file, """
            site:
              title: Test
              url: https://docs.example.org
            output:
              dir: site
            content:
              sources:
                - id: %s
                  display_name: Docs
                  url: https://example.org/docs.git
                  branches:
                    - name: main
            """.formatted(id));
        var error = assertThrows(RuntimeException.class, () -> new BibliosConfigParser().parse(file));
        assertTrue(error.getMessage().contains("site-assets"), error.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "site-assets", "SITE-ASSETS", "Site-Assets", ".", "..", "a/b", "a\\b"})
    void configRejectsReservedAndNonSegmentIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> source(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"docs", "public+docs", "public docs", "über", "docs.v1"})
    void ordinaryIdsAreNotRestrictedToAlphanumerics(String id) {
        assertEquals(id, source(id).id());
    }

    private SourceConfig source(String id) {
        return new SourceConfig(id, "Docs", "https://example.org/docs.git",
            List.of(new BranchConfig("main", null)), null, null, null, null);
    }
}
