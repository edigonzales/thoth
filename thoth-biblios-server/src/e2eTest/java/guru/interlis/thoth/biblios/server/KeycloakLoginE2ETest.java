package guru.interlis.thoth.biblios.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end OIDC login against a real Keycloak container: authorization code
 * flow, session, group claim from the Keycloak group membership mapper and the
 * resulting access decisions.
 *
 * <p>Skipped automatically when Docker is not available.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KeycloakLoginE2ETest {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.7.3";

    private static final boolean DOCKER_AVAILABLE = dockerAvailable();
    private static final int APP_PORT = freePort();

    private static Path packageDir;
    private static Path accessConfig;
    private static GenericContainer<?> keycloak;
    private static String issuer;

    static {
        if (DOCKER_AVAILABLE) {
            try {
                Path root = Files.createTempDirectory("biblios-keycloak-e2e");
                PackageTestFixture.Fixture fixture = PackageTestFixture.create(root);
                packageDir = fixture.packageDir();
                accessConfig = root.resolve("access.yml");
                Files.writeString(accessConfig, """
                    default: deny
                    policies:
                      public:
                        mode: public
                      internal:
                        mode: restricted
                        allow:
                          groups:
                            - provider: keycloak-local
                              id: agi-betrieb
                    """);

                Path sourceRealm = findRealmFile();
                Path realmFile = root.resolve("realm-e2e.json");
                Files.writeString(realmFile, Files.readString(sourceRealm)
                    .replace("http://localhost:8080", "http://localhost:" + APP_PORT));

                keycloak = new GenericContainer<>(KEYCLOAK_IMAGE)
                    .withExposedPorts(8080)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "local-dev-only")
                    .withCommand("start-dev", "--import-realm", "--http-port=8080")
                    .withCopyFileToContainer(MountableFile.forHostPath(realmFile),
                        "/opt/keycloak/data/import/realm.json")
                    .waitingFor(Wait.forHttp("/realms/biblios-dev/.well-known/openid-configuration")
                        .forPort(8080)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
                keycloak.start();
                issuer = "http://localhost:" + keycloak.getMappedPort(8080) + "/realms/biblios-dev";
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start Keycloak test container", e);
            }
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        if (!DOCKER_AVAILABLE) {
            return;
        }
        registry.add("server.port", () -> APP_PORT);
        registry.add("biblios.package-dir", () -> packageDir.toString());
        registry.add("biblios.access-config", () -> accessConfig.toString());
        registry.add("biblios.provider", () -> "keycloak-local");
        registry.add("biblios.registration-id", () -> "keycloak");
        registry.add("spring.security.oauth2.client.provider.keycloak.authorization-uri",
            () -> issuer + "/protocol/openid-connect/auth");
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
            () -> issuer + "/protocol/openid-connect/token");
        registry.add("spring.security.oauth2.client.provider.keycloak.jwk-set-uri",
            () -> issuer + "/protocol/openid-connect/certs");
        registry.add("spring.security.oauth2.client.provider.keycloak.user-info-uri",
            () -> issuer + "/protocol/openid-connect/userinfo");
    }

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker is not available; skipping Keycloak E2E test");
    }

    @AfterAll
    static void stopContainer() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    @Test
    void groupMemberLogsInAndReadsProtectedPage() throws Exception {
        OidcTestClient client = new OidcTestClient(baseUrl());

        HttpResponse<String> page = client.loginAndGet("anna", "anna", "/internal-docs/main/");

        assertEquals(200, page.statusCode(), page.body());
        assertTrue(page.body().contains("Internal content"), page.body());

        HttpResponse<String> home = client.getPath("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("Internal Docs"), "group member should see protected documentation");
    }

    @Test
    void authenticatedUserWithoutGroupIsDenied() throws Exception {
        OidcTestClient client = new OidcTestClient(baseUrl());

        HttpResponse<String> page = client.loginAndGet("ben", "ben", "/internal-docs/main/");

        assertEquals(403, page.statusCode(), page.body());

        HttpResponse<String> home = client.getPath("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("Public Docs"));
        assertTrue(!home.body().contains("Internal Docs"), "protected documentation must stay hidden");
    }

    private static String baseUrl() {
        return "http://localhost:" + APP_PORT;
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No free port available", e);
        }
    }

    private static Path findRealmFile() {
        for (String candidate : new String[] {
            "../dev/keycloak/import/realm-biblios-dev.json",
            "dev/keycloak/import/realm-biblios-dev.json"
        }) {
            Path path = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                return path;
            }
        }
        throw new IllegalStateException("realm-biblios-dev.json not found");
    }
}
