package guru.interlis.thoth.biblios.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties = {
    "biblios.subject-claim=oid",
    "spring.security.oauth2.client.registration.other.provider=keycloak",
    "spring.security.oauth2.client.registration.other.client-id=other",
    "spring.security.oauth2.client.registration.other.client-secret=secret",
    "spring.security.oauth2.client.registration.other.authorization-grant-type=authorization_code",
    "spring.security.oauth2.client.registration.other.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
    "spring.security.oauth2.client.registration.other.scope=openid"
})
@AutoConfigureMockMvc
class ProviderIsolationTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var f = PackageTestFixture.create(Files.createTempDirectory("biblios-provider"));
        registry.add("biblios.package-dir", () -> f.packageDir().toString());
        registry.add("biblios.access-config", () -> f.accessConfig().toString());
    }
    @Autowired MockMvc mvc;

    @Test void onlySelectedLoginAndCallbackAreAccepted() throws Exception {
        mvc.perform(get("/oauth2/authorization/other")).andExpect(status().isNotFound());
        mvc.perform(get("/login/oauth2/code/other?code=unused&state=unused")).andExpect(status().isNotFound());
        mvc.perform(get("/oauth2/authorization/keycloak")).andExpect(status().isFound())
            .andExpect(header().string("Location", containsString("/protocol/openid-connect/auth")));
        mvc.perform(get("/login")).andExpect(status().isFound())
            .andExpect(header().string("Location", startsWith("/oauth2/authorization/keycloak?loginIntent=")));
    }

    @Test void missingConfiguredSubjectCannotUseSubEvenWithTheRightGroup() throws Exception {
        var missing = oidcLogin().clientRegistration(PackageTestFixture.registration("keycloak"))
            .idToken(t -> t.claim("iss", PackageTestFixture.ISSUER).subject("anna").claim("groups", List.of("agi-betrieb")));
        mvc.perform(get("/internal-docs/main/").with(missing)).andExpect(status().isNotFound());
        var valid = oidcLogin().clientRegistration(PackageTestFixture.registration("keycloak"))
            .idToken(t -> t.claim("iss", PackageTestFixture.ISSUER).subject("anna").claim("oid", "anna").claim("groups", List.of("agi-betrieb")));
        mvc.perform(get("/internal-docs/main/").with(valid)).andExpect(status().isOk());
    }

    @Test void sessionsNeedTheExactIssuerEvenWithMatchingRegistrationAndGroups() throws Exception {
        for (String issuer : new String[] {null, "https://other.example", PackageTestFixture.ISSUER + "/"}) {
            var login = oidcLogin().clientRegistration(PackageTestFixture.registration("keycloak"))
                .idToken(t -> {
                    t.subject("anna").claim("oid", "anna").claim("groups", List.of("agi-betrieb"));
                    t.claims(claims -> {
                        if (issuer == null) claims.remove("iss"); else claims.put("iss", issuer);
                    });
                });
            mvc.perform(get("/internal-docs/main/").with(login)).andExpect(status().isNotFound());
            mvc.perform(get("/api/search-index").with(login)).andExpect(content().string(not(containsString("internal searchable content"))));
        }
    }

    @Test void existingForeignSessionCannotReuseMatchingClaims() throws Exception {
        var foreign = oidcLogin().clientRegistration(PackageTestFixture.registration("other"))
            .idToken(t -> t.claim("iss", PackageTestFixture.ISSUER).subject("anna").claim("oid", "anna").claim("groups", List.of("agi-betrieb")));
        mvc.perform(get("/internal-docs/main/").with(foreign)).andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString("Internal content"))));
        mvc.perform(get("/api/search-index").with(foreign)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("internal searchable content"))));
    }
}
