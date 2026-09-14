package guru.interlis.thoth.biblios.server.security;

import guru.interlis.thoth.biblios.server.web.PortalSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginReturnTargetTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://evil.example", "//evil.example/x", "/%2f%2fevil.example",
        "/\\evil.example", "/%5cevil.example", "/x%0d%0aLocation:evil", "/x?y=%0a", "/x#fragment",
        "/login", "/a/../login", "/a/../../login", "/%2e%2e/login", "/%6cogin", "/logout/x", "/oauth2/authorization/keycloak",
        "/login;session=x", "relative", "javascript:alert(1)"})
    void invalidTargetsFallBackToTheLocalPortal(String target) {
        assertEquals("/", LoginReturnTarget.validate(target, ""));
    }

    @Test void preservesLocalPathAndQueryAndRequiresTheContext() {
        assertEquals("/public+docs/%C3%BCber%20space/?q=a%2Bb&x=1", LoginReturnTarget.validate(
            "/public+docs/%C3%BCber%20space/?q=a%2Bb&x=1", ""));
        assertEquals("/portal/", LoginReturnTarget.validate("/outside", "/portal"));
        assertEquals("/portal/", LoginReturnTarget.validate("/portal/../login", "/portal"));
    }

    @Test void successUsesExplicitTargetOnceAndDiscardsSavedRequests() throws Exception {
        var request = new MockHttpServletRequest("GET", "/login");
        request.setParameter("returnTo", "/unknown/main/?q=a%2Bb");
        request.setParameter("loginIntent", LoginReturnTarget.prepare(request));
        LoginReturnTarget.beginAuthorization(request);
        request.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", "https://evil.example");
        var response = new MockHttpServletResponse();
        var handler = new SecurityConfig().authenticationSuccessHandler(mock(PortalSession.class));
        handler.onAuthenticationSuccess(request, response, null);
        assertEquals("/unknown/main/?q=a%2Bb", response.getHeader("Location"));
        assertNull(request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST"));
        response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, null);
        assertEquals("/", response.getHeader("Location"));
    }

    @Test void abandoningTheLoginLinkBeforeAuthorizationCannotAffectALaterDirectLogin() {
        var request = new MockHttpServletRequest("GET", "/login");
        request.setParameter("returnTo", "/abandoned/");
        LoginReturnTarget.prepare(request);
        LoginReturnTarget.beginAuthorization(request); // No matching loginIntent on a direct login.
        assertEquals("/", LoginReturnTarget.consume(request));
    }

    @Test void newOrDirectLoginCannotReuseAnAbandonedTarget() {
        var request = new MockHttpServletRequest("GET", "/login");
        request.setParameter("returnTo", "/old-target/");
        request.setParameter("loginIntent", LoginReturnTarget.prepare(request));
        LoginReturnTarget.beginAuthorization(request);
        LoginReturnTarget.beginAuthorization(request); // Direct login without a new /login action.
        assertEquals("/", LoginReturnTarget.consume(request));
        request.setParameter("returnTo", "/old-target/");
        request.setParameter("loginIntent", LoginReturnTarget.prepare(request));
        request.removeParameter("returnTo");
        request.setParameter("loginIntent", LoginReturnTarget.prepare(request));
        LoginReturnTarget.beginAuthorization(request);
        assertEquals("/", LoginReturnTarget.consume(request));
    }
}
