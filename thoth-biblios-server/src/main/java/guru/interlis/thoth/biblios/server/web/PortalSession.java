package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Session handling for captured identities.
 *
 * <p>Group memberships are only as fresh as the token they were read from. The
 * principal is therefore accepted for at most {@code biblios.max-identity-age};
 * afterwards the session is invalidated and the user has to log in again.</p>
 */
@Component
public class PortalSession {

    public static final String AUTHENTICATED_AT_ATTRIBUTE = "biblios.authenticated-at";

    private final CurrentPrincipal currentPrincipal;
    private final Duration maxIdentityAge;

    public PortalSession(CurrentPrincipal currentPrincipal, BibliosServerProperties properties) {
        this.currentPrincipal = currentPrincipal;
        this.maxIdentityAge = properties.getMaxIdentityAge();
    }

    /**
     * Mark the current session as freshly authenticated. Called after login.
     */
    public void markAuthenticated(HttpServletRequest request) {
        request.getSession(true).setAttribute(AUTHENTICATED_AT_ATTRIBUTE, System.currentTimeMillis());
    }

    /**
     * Resolve the principal of the current request, or {@code null} when the
     * identity is anonymous or older than the configured maximum age.
     */
    public PrincipalIdentity principal(HttpServletRequest request, Authentication authentication) {
        PrincipalIdentity principal = currentPrincipal.resolve(authentication);
        if (principal == null) {
            return null;
        }
        HttpSession session = request.getSession(true);
        Object capturedAt = session.getAttribute(AUTHENTICATED_AT_ATTRIBUTE);
        long now = System.currentTimeMillis();
        if (capturedAt instanceof Long timestamp) {
            if (now - timestamp > maxIdentityAge.toMillis()) {
                session.invalidate();
                return null;
            }
        } else {
            // Sessions created before the attribute existed (or by tests) start now.
            session.setAttribute(AUTHENTICATED_AT_ATTRIBUTE, now);
        }
        return principal;
    }

    /**
     * Build the frame session (principal plus logout/CSRF data) for a request.
     */
    public PortalFrames.FrameSession frameSession(HttpServletRequest request, Authentication authentication) {
        PrincipalIdentity principal = principal(request, authentication);
        Object attribute = request.getAttribute(CsrfToken.class.getName());
        CsrfToken csrf = attribute instanceof CsrfToken token ? token : null;
        return new PortalFrames.FrameSession(
            principal,
            csrf != null ? csrf.getParameterName() : null,
            csrf != null ? csrf.getToken() : null
        );
    }

    public Duration maxIdentityAge() {
        return maxIdentityAge;
    }
}
