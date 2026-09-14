package guru.interlis.thoth.biblios.server.security;

import guru.interlis.thoth.biblios.server.web.PortalSession;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
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
        // Only explicit login actions may set return targets.
        return new NullRequestCache();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(PortalSession portalSession) {
        return (request, response, authentication) -> {
            portalSession.markAuthenticated(request);
            new HttpSessionRequestCache().removeRequest(request, response);
            response.setStatus(302);
            response.setHeader("Location", LoginReturnTarget.consume(request));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler authenticationSuccessHandler,
                                                   BibliosServerProperties properties,
                                                   ClientRegistrationRepository registrations)
        throws Exception {
        String registrationId = properties.getRegistrationId();
        if (registrationId == null || registrationId.isBlank()
            || registrations.findByRegistrationId(registrationId) == null) {
            throw new IllegalStateException("biblios.registration-id must identify a configured OAuth2 client");
        }
        http
            .addFilterBefore(new LoginRegistrationFilter(registrationId),
                OAuth2AuthorizationRequestRedirectFilter.class)
            .requestCache(cache -> cache.requestCache(requestCache()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/" + registrationId)
                .successHandler(authenticationSuccessHandler))
            // A redirect to the OIDC login page would immediately authenticate again.
            .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }
}
