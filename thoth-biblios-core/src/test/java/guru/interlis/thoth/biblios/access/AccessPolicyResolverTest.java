package guru.interlis.thoth.biblios.access;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.core.ThothBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccessPolicyResolverTest {

    @TempDir
    Path tempDir;

    private final BibliosConfigParser configParser = new BibliosConfigParser();
    private final AccessRulesParser rulesParser = new AccessRulesParser();

    @Test
    void validatesKnownPolicies() throws Exception {
        BibliosConfig config = parseConfig(TWO_SOURCES);
        AccessPolicyResolver resolver = resolver(ALL_POLICIES);

        assertDoesNotThrow(() -> resolver.validate(config.content().sources()));
        assertTrue(resolver.hasNonPublicSources(config.content().sources()));
        assertEquals(List.of("internal-docs"), resolver.nonPublicSourceIds(config.content().sources()));
    }

    @Test
    void rejectsUnknownPolicyReference() throws Exception {
        BibliosConfig config = parseConfig(TWO_SOURCES);
        AccessPolicyResolver resolver = resolver("""
            default: deny
            policies:
              public:
                mode: public
            """);

        ThothBuildException error = assertThrows(ThothBuildException.class,
            () -> resolver.validate(config.content().sources()));
        assertTrue(error.getMessage().contains("internal-docs"), error.getMessage());
        assertTrue(error.getMessage().contains("internal"), error.getMessage());
    }

    @Test
    void defaultDenyWithoutPolicyIsNotPublic() throws Exception {
        BibliosConfig config = parseConfig("""
            site:
              title: Test
            output:
              dir: out
            content:
              sources:
                - id: unassigned
                  display_name: Unassigned
                  url: file:///tmp/unassigned
                  branches:
                    - name: main
            """);
        AccessPolicyResolver resolver = resolver("default: deny\n");

        assertTrue(resolver.hasNonPublicSources(config.content().sources()));
        assertEquals(List.of("unassigned"), resolver.nonPublicSourceIds(config.content().sources()));
    }

    @Test
    void noAccessConfigurationKeepsSourcesPublic() throws Exception {
        BibliosConfig config = parseConfig("""
            site:
              title: Test
            output:
              dir: out
            content:
              sources:
                - id: docs
                  display_name: Docs
                  url: file:///tmp/docs
                  branches:
                    - name: main
            """);
        AccessPolicyResolver resolver = new AccessPolicyResolver(AccessRules.defaultPublic());

        assertFalse(resolver.hasNonPublicSources(config.content().sources()));
        BibliosConfig publicOnly = resolver.publicOnly(config);
        assertEquals(1, publicOnly.content().sources().size());
    }

    @Test
    void publicOnlyFiltersSourcesAndGroups() throws Exception {
        BibliosConfig config = parseConfig(TWO_SOURCES_WITH_GROUP);
        AccessPolicyResolver resolver = resolver(ALL_POLICIES);

        BibliosConfig publicOnly = resolver.publicOnly(config);

        assertEquals(1, publicOnly.content().sources().size());
        assertEquals("public-docs", publicOnly.content().sources().get(0).id());
        assertEquals(1, publicOnly.content().groups().size());
        assertEquals(List.of("public-docs"), publicOnly.content().groups().get(0).sources());
    }

    @Test
    void publicOnlyDropsEmptyGroups() throws Exception {
        BibliosConfig config = parseConfig("""
            site:
              title: Test
            output:
              dir: out
            content:
              sources:
                - id: public-docs
                  display_name: Public
                  url: file:///tmp/public
                  branches:
                    - name: main
                  access_policy: public
                - id: internal-docs
                  display_name: Internal
                  url: file:///tmp/internal
                  branches:
                    - name: main
                  access_policy: internal
              groups:
                - title: Public Group
                  sources: [public-docs]
                - title: Internal Group
                  sources: [internal-docs]
            """);
        AccessPolicyResolver resolver = resolver(ALL_POLICIES);

        BibliosConfig publicOnly = resolver.publicOnly(config);

        assertEquals(1, publicOnly.content().groups().size());
        assertEquals("Public Group", publicOnly.content().groups().get(0).title());
    }

    @Test
    void publicOnlyFailsWhenNothingIsPublic() throws Exception {
        BibliosConfig config = parseConfig("""
            site:
              title: Test
            output:
              dir: out
            content:
              sources:
                - id: internal-docs
                  display_name: Internal
                  url: file:///tmp/internal
                  branches:
                    - name: main
                  access_policy: internal
            """);
        AccessPolicyResolver resolver = resolver(ALL_POLICIES);

        assertThrows(ThothBuildException.class, () -> resolver.publicOnly(config));
    }

    private AccessPolicyResolver resolver(String yaml) {
        return new AccessPolicyResolver(rulesParser.parseString(yaml));
    }

    private BibliosConfig parseConfig(String yaml) throws Exception {
        Path file = tempDir.resolve("biblios.yml");
        Files.writeString(file, yaml);
        return configParser.parse(file);
    }

    private static final String TWO_SOURCES = """
        site:
          title: Test
        output:
          dir: out
        content:
          sources:
            - id: public-docs
              display_name: Public
              url: file:///tmp/public
              branches:
                - name: main
              access_policy: public
            - id: internal-docs
              display_name: Internal
              url: file:///tmp/internal
              branches:
                - name: main
              access_policy: internal
        """;

    private static final String TWO_SOURCES_WITH_GROUP = TWO_SOURCES + """
          groups:
            - title: Everything
              sources: [public-docs, internal-docs]
        """;

    private static final String ALL_POLICIES = """
        default: deny
        policies:
          public:
            mode: public
          internal:
            mode: restricted
            allow:
              groups:
                - provider: entra-kanton
                  id: group-internal
        """;
}
