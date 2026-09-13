package guru.interlis.thoth.biblios.server.access;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.access.SubjectRef;
import guru.interlis.thoth.biblios.server.PackageTestFixture;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Access rules are re-read when {@code access.yml} changes; invalid updates are
 * ignored while the last valid rules stay active.
 */
class AccessServiceReloadTest {

    @TempDir
    Path tempDir;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void reloadsChangedAccessConfiguration() throws Exception {
        PackageTestFixture.Fixture fixture = PackageTestFixture.create(tempDir);
        BibliosServerProperties properties = propertiesFor(fixture);
        PublicationPackage publicationPackage = new PublicationPackage(properties, objectMapper);
        AccessService service = new AccessService(publicationPackage, properties);
        PrincipalIdentity member = PrincipalIdentity.of("keycloak-local", "anna", "Anna",
            Set.of(new SubjectRef("keycloak-local", "group-internal")));

        assertFalse(service.canAccessSource("internal-docs", null));
        assertTrue(service.canAccessSource("internal-docs", member));

        writeAccessConfig(fixture.accessConfig(), """
            default: deny
            policies:
              public:
                mode: public
              internal:
                mode: public
            """);
        assertTrue(service.canAccessSource("internal-docs", null),
            "changed rules must apply without restart");

        writeAccessConfig(fixture.accessConfig(), "policies: [broken");
        assertTrue(service.canAccessSource("internal-docs", null),
            "invalid rules must not change the effective permissions");

        writeAccessConfig(fixture.accessConfig(), """
            default: deny
            policies:
              public:
                mode: public
              internal:
                mode: restricted
                allow:
                  groups:
                    - provider: keycloak-local
                      id: group-internal
            """);
        assertFalse(service.canAccessSource("internal-docs", null));
        assertTrue(service.canAccessSource("internal-docs", member));
    }

    @Test
    void failsAtStartupWhenRequiredPolicyIsMissing() throws Exception {
        PackageTestFixture.Fixture fixture = PackageTestFixture.create(tempDir);
        writeAccessConfig(fixture.accessConfig(), """
            default: deny
            policies:
              public:
                mode: public
            """);
        BibliosServerProperties properties = propertiesFor(fixture);
        PublicationPackage publicationPackage = new PublicationPackage(properties, objectMapper);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> new AccessService(publicationPackage, properties));
        assertTrue(error.getMessage().contains("internal"), error.getMessage());
    }

    private BibliosServerProperties propertiesFor(PackageTestFixture.Fixture fixture) {
        BibliosServerProperties properties = new BibliosServerProperties();
        properties.setPackageDir(fixture.packageDir());
        properties.setAccessConfig(fixture.accessConfig());
        properties.setProvider("keycloak-local");
        return properties;
    }

    private void writeAccessConfig(Path path, String content) throws Exception {
        Files.writeString(path, content);
        Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() + 1000));
    }
}
