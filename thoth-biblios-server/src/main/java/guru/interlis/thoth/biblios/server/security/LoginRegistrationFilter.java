package guru.interlis.thoth.biblios.server.security;

import guru.interlis.thoth.biblios.server.web.PortalRequestPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Reject other configured registrations before OAuth2 login and callback processing. */
final class LoginRegistrationFilter extends OncePerRequestFilter {
    private final String registrationId;

    LoginRegistrationFilter(String registrationId) {
        this.registrationId = registrationId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        final String path;
        try {
            path = PortalRequestPath.from(request);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        for (String prefix : new String[] {"/oauth2/authorization/", "/login/oauth2/code/"}) {
            if ((path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1)))
                && !path.equals(prefix + registrationId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        if (path.equals("/login") && request.getMethod().equals("GET")) {
            String intent = LoginReturnTarget.prepare(request);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", request.getContextPath() + "/oauth2/authorization/" + registrationId
                + "?loginIntent=" + intent);
            return;
        }
        if (path.equals("/oauth2/authorization/" + registrationId)) {
            LoginReturnTarget.beginAuthorization(request);
        }
        chain.doFilter(request, response);
    }
}
