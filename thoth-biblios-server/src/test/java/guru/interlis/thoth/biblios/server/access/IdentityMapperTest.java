package guru.interlis.thoth.biblios.server.access;

import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.web.CurrentPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class IdentityMapperTest {
    @Test void configuredSubjectIsMandatoryAndTyped() {
        var properties = new BibliosServerProperties();
        properties.setSubjectClaim("oid");
        var mapper = new IdentityMapper(properties);
        for (Map<String, Object> claims : java.util.List.<Map<String,Object>>of(
            Map.of("sub", "fallback"), Map.of("sub", "fallback", "oid", " "),
            Map.of("sub", "fallback", "oid", 42))) {
            assertNull(mapper.from(user(claims)));
        }
        var identity = mapper.from(user(Map.of("sub", "fallback", "oid", "stable-id")));
        assertEquals("stable-id", identity.subject());
        assertTrue(identity.groups().isEmpty());
    }

    @Test void registrationIsCheckedBeforeMapping() {
        var p = new BibliosServerProperties();
        p.setIssuerUri("https://trusted.example");
        var resolver = new CurrentPrincipal(new IdentityMapper(p), p);
        var user = user(Map.of("sub", "anna", "iss", "https://trusted.example"));
        assertNull(resolver.resolve(new OAuth2AuthenticationToken(user, user.getAuthorities(), "other")));
        assertNotNull(resolver.resolve(new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak")));
    }

    private DefaultOidcUser user(Map<String, Object> claims) {
        return new DefaultOidcUser(Set.of(), new OidcIdToken("token", Instant.now(),
            Instant.now().plusSeconds(300), claims));
    }
}
