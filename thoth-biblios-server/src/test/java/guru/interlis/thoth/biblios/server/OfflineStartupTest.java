package guru.interlis.thoth.biblios.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "biblios.issuer-uri=https://offline.example/tenant",
    "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://127.0.0.1:1/auth",
    "spring.security.oauth2.client.provider.keycloak.token-uri=http://127.0.0.1:1/token",
    "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://127.0.0.1:1/keys",
    "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://127.0.0.1:1/userinfo"
})
class OfflineStartupTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        var fixture = PackageTestFixture.create(Files.createTempDirectory("biblios-offline"));
        registry.add("biblios.package-dir", () -> fixture.packageDir().toString());
        registry.add("biblios.access-config", () -> fixture.accessConfig().toString());
    }
    @Autowired JwtDecoderFactory<ClientRegistration> factory;
    @Autowired ClientRegistrationRepository registrations;

    @Test void applicationAndDecoderStartWithoutAnAvailableIdentityProvider() {
        assertNotNull(factory.createDecoder(registrations.findByRegistrationId("keycloak")));
    }
}
