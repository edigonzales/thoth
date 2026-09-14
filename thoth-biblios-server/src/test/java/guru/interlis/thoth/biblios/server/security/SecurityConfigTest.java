package guru.interlis.thoth.biblios.server.security;

import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    @Test void unknownSelectedRegistrationFailsSecurityInitialization() {
        var properties = new BibliosServerProperties();
        properties.setRegistrationId("missing");
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().securityFilterChain(
            mock(HttpSecurity.class), mock(AuthenticationSuccessHandler.class), properties,
            mock(ClientRegistrationRepository.class)));
    }
}
