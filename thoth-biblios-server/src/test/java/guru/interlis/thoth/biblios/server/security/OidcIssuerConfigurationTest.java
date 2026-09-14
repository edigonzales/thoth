package guru.interlis.thoth.biblios.server.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import guru.interlis.thoth.biblios.server.PackageTestFixture;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.jwt.JwtException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class OidcIssuerConfigurationTest {
    private static final String ISSUER = "https://trusted.example/tenant";

    @Test void signedTokensRequireExactIssuerAndKeepExistingChecks() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).keyID("test").generate();
        RSAKey wrongKey = new RSAKeyGenerator(2048).keyID("test").generate();
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/keys", exchange -> {
            requests.incrementAndGet();
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var registration = ClientRegistration.withClientRegistration(PackageTestFixture.registration("keycloak"))
                .jwkSetUri("http://127.0.0.1:" + server.getAddress().getPort() + "/keys").build();
            var properties = properties(ISSUER);
            var factory = new OidcIssuerConfiguration().oidcJwtDecoderFactory(properties,
                new InMemoryClientRegistrationRepository(registration));
            var decoder = factory.createDecoder(registration);
            assertEquals(0, requests.get(), "Constructing the factory and decoder must not contact the IdP");
            assertEquals("anna", decoder.decode(token(key, ISSUER, "test-client", 300)).getSubject());
            for (String issuer : new String[] {null, "https://other.example/tenant", ISSUER + "/"}) {
                assertThrows(JwtException.class, () -> decoder.decode(token(key, issuer, "test-client", 300)));
            }
            assertThrows(JwtException.class, () -> decoder.decode(token(key, ISSUER, "wrong-client", 300)));
            assertThrows(JwtException.class, () -> decoder.decode(token(key, ISSUER, "test-client", -300)));
            assertThrows(JwtException.class, () -> decoder.decode(token(wrongKey, ISSUER, "test-client", 300)));
            assertTrue(requests.get() > 0);
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "/relative", "ftp://host/path", "https:///path", "https://user@host/path",
        "https://host/path?query", "https://host/path#fragment"})
    void invalidIssuerPreventsContextStartup(String issuer) {
        context(issuer, PackageTestFixture.registration("keycloak")).run(ctx -> {
            assertNotNull(ctx.getStartupFailure());
            assertTrue(rootCause(ctx.getStartupFailure()).getMessage().contains("biblios.issuer-uri"));
        });
    }

    @Test void conflictingMetadataIssuerPreventsStartup() {
        var registration = ClientRegistration.withClientRegistration(PackageTestFixture.registration("keycloak"))
            .issuerUri(ISSUER + "/").build();
        context(ISSUER, registration).run(ctx -> assertNotNull(ctx.getStartupFailure()));
        context(ISSUER + "/", registration).run(ctx -> assertNull(ctx.getStartupFailure()));
    }

    private ApplicationContextRunner context(String issuer, ClientRegistration registration) {
        return new ApplicationContextRunner().withUserConfiguration(OidcIssuerConfiguration.class)
            .withBean(BibliosServerProperties.class, () -> properties(issuer))
            .withBean(ClientRegistrationRepository.class, () -> new InMemoryClientRegistrationRepository(registration));
    }

    private BibliosServerProperties properties(String issuer) {
        var p = new BibliosServerProperties(); p.setIssuerUri(issuer); return p;
    }

    private Throwable rootCause(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        return error;
    }

    private String token(RSAKey key, String issuer, String audience, int expiresIn) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject("anna").audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(Instant.now().plusSeconds(expiresIn)));
        if (issuer != null) claims.issuer(issuer);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
