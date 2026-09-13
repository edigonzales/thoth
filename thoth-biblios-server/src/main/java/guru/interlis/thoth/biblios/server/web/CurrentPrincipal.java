package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.access.IdentityMapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    public CurrentPrincipal(IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    public PrincipalIdentity resolve(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return identityMapper.from(oidcUser);
        }
        return null;
    }
}
