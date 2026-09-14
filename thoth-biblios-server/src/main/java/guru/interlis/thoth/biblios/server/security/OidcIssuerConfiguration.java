package guru.interlis.thoth.biblios.server.security;

import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import java.net.URI;

/** Adds an exact issuer check without discovery or weakening the default OIDC validators. */
@Configuration
public class OidcIssuerConfiguration {
    @Bean
    public JwtDecoderFactory<ClientRegistration> oidcJwtDecoderFactory(
            BibliosServerProperties properties, ClientRegistrationRepository registrations) {
        String issuer = validateIssuer(properties.getIssuerUri());
        ClientRegistration registration = registrations.findByRegistrationId(properties.getRegistrationId());
        if (registration == null) {
            throw new IllegalStateException("biblios.registration-id must identify a configured OAuth2 client");
        }
        String metadataIssuer = registration.getProviderDetails().getIssuerUri();
        if (metadataIssuer != null && !issuer.equals(metadataIssuer)) {
            throw new IllegalStateException("biblios.issuer-uri differs from the selected registration's issuer");
        }
        var factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(client -> JwtValidators.createDefaultWithValidators(
            new OidcIdTokenValidator(client), new JwtIssuerValidator(issuer)));
        return factory;
    }

    static String validateIssuer(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && uri.getRawUserInfo() == null
                && uri.getRawQuery() == null && uri.getRawFragment() == null) {
                return value;
            }
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // Report an actionable startup error without modifying the supplied value.
        }
        throw new IllegalStateException("biblios.issuer-uri must be an absolute HTTP(S) issuer with a host"
            + " and without user information, query or fragment");
    }
}
