package guru.interlis.thoth.biblios.server.security;

import guru.interlis.thoth.biblios.server.web.PortalSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

/**
 * Security setup: OIDC login with server-side sessions.
 *
 * <p>Request authorization is intentionally not expressed in the filter chain:
 * whether a documentation may be read depends on the access policies of the
 * publication package and is enforced by the portal controllers. The filter
 * chain only establishes authentication, records the identity age and provides
 * logout.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public RequestCache requestCache() {
        // Remembers protected deep links so that login returns to the requested page.
        return new HttpSessionRequestCache();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(PortalSession portalSession) {
        SavedRequestAwareAuthenticationSuccessHandler delegate =
            new SavedRequestAwareAuthenticationSuccessHandler();
        return (request, response, authentication) -> {
            portalSession.markAuthenticated(request);
            delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler authenticationSuccessHandler)
        throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth.successHandler(authenticationSuccessHandler))
            .logout(Customizer.withDefaults());
        return http.build();
    }
}
