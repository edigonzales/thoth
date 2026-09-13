package guru.interlis.thoth.biblios.access;

import guru.interlis.thoth.core.ThothBuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AccessRulesParserTest {

    @TempDir
    Path tempDir;

    private final AccessRulesParser parser = new AccessRulesParser();

    @Test
    void parsesFullConfiguration() {
        AccessRules rules = parser.parseString("""
            default: deny
            policies:
              public:
                mode: public
              agi-betrieb:
                mode: restricted
                allow:
                  groups:
                    - provider: entra-kanton
                      id: "group-1"
                  users:
                    - provider: Entra-Kanton
                      id: "user-1"
              intern:
                mode: authenticated
            """);

        assertEquals(AccessDefault.DENY, rules.defaultAccess());
        assertEquals(3, rules.policies().size());

        AccessPolicy publicPolicy = rules.policy("public");
        assertNotNull(publicPolicy);
        assertEquals(AccessMode.PUBLIC, publicPolicy.mode());

        AccessPolicy restricted = rules.policy("agi-betrieb");
        assertNotNull(restricted);
        assertEquals(AccessMode.RESTRICTED, restricted.mode());
        assertEquals(1, restricted.groups().size());
        assertEquals(1, restricted.users().size());
        assertTrue(restricted.groups().contains(new SubjectRef("entra-kanton", "group-1")));
        assertTrue(restricted.users().contains(new SubjectRef("entra-kanton", "user-1")));

        assertEquals(AccessMode.AUTHENTICATED, rules.policy("intern").mode());
    }

    @Test
    void defaultsToDenyWhenDefaultMissing() {
        AccessRules rules = parser.parseString("""
            policies:
              public:
                mode: public
            """);
        assertEquals(AccessDefault.DENY, rules.defaultAccess());
    }

    @Test
    void parsesDefaultPublic() {
        AccessRules rules = parser.parseString("default: public\n");
        assertTrue(rules.isDefaultPublic());
        assertTrue(rules.policies().isEmpty());
    }

    @Test
    void rejectsEmptyDocument() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString(""));
        assertTrue(error.getMessage().contains("empty"), error.getMessage());
    }

    @Test
    void rejectsUnknownRootKey() {
        ThothBuildException error = assertThrows(ThothBuildException.class,
            () -> parser.parseString("defaults: deny\n"));
        assertTrue(error.getMessage().contains("unknown key"), error.getMessage());
    }

    @Test
    void rejectsUnknownPolicyKey() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString("""
            policies:
              public:
                mode: public
                allowlist: []
            """));
        assertTrue(error.getMessage().contains("unknown key"), error.getMessage());
    }

    @Test
    void rejectsUnknownMode() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString("""
            policies:
              broken:
                mode: internal
            """));
        assertTrue(error.getMessage().contains("Unknown access mode"), error.getMessage());
    }

    @Test
    void rejectsMissingMode() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString("""
            policies:
              broken: {}
            """));
        assertTrue(error.getMessage().contains("mode"), error.getMessage());
    }

    @Test
    void rejectsAllowEntriesOnPublicPolicy() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString("""
            policies:
              public:
                mode: public
                allow:
                  users:
                    - provider: keycloak
                      id: user-1
            """));
        assertTrue(error.getMessage().contains("restricted"), error.getMessage());
    }

    @Test
    void rejectsSubjectWithoutId() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString("""
            policies:
              protected:
                mode: restricted
                allow:
                  groups:
                    - provider: keycloak
            """));
        assertTrue(error.getMessage().contains("id"), error.getMessage());
    }

    @Test
    void rejectsUnknownAllowKey() {
        ThothBuildException error = assertThrows(ThothBuildException.class, () -> parser.parseString("""
            policies:
              protected:
                mode: restricted
                allow:
                  teams: []
            """));
        assertTrue(error.getMessage().contains("unknown key"), error.getMessage());
    }

    @Test
    void parseOptionalReturnsEmptyForMissingFile() {
        Optional<AccessRules> rules = parser.parseOptional(tempDir.resolve("access.yml"));
        assertTrue(rules.isEmpty());
    }

    @Test
    void parseOptionalReadsExistingFile() throws Exception {
        Path file = tempDir.resolve("access.yml");
        Files.writeString(file, "default: public\n");
        Optional<AccessRules> rules = parser.parseOptional(file);
        assertTrue(rules.isPresent());
        assertTrue(rules.get().isDefaultPublic());
    }

    @Test
    void parseRejectsMissingFile() {
        ThothBuildException error = assertThrows(ThothBuildException.class,
            () -> parser.parse(tempDir.resolve("does-not-exist.yml")));
        assertTrue(error.getMessage().contains("not found"), error.getMessage());
    }
}
