package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.access.IdentityMapper;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Resolves the normalized principal of the current request (or {@code null} for
 * anonymous visitors).
 */
@Component
public class CurrentPrincipal {

    private final IdentityMapper identityMapper;

    private final String registrationId;
    private final String issuerUri;

    public CurrentPrincipal(IdentityMapper identityMapper, BibliosServerProperties properties) {
        this.registrationId = properties.getRegistrationId();
        this.issuerUri = properties.getIssuerUri();
        this.identityMapper = identityMapper;
    }

    public PrincipalIdentity resolve(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)
            || !oauth.isAuthenticated()
            || !registrationId.equals(oauth.getAuthorizedClientRegistrationId())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser
            && issuerUri != null
            && issuerUri.equals(oidcUser.getIdToken().getClaimAsString("iss"))) {
            return identityMapper.from(oidcUser);
        }
        return null;
    }
}
