package guru.interlis.thoth.biblios.server;

import guru.interlis.thoth.biblios.server.web.PortalSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access control tests for the portal: anonymous, authenticated without groups and
 * group members, including navigation filtering, search filtering and file access.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PortalAccessTest {

    @DynamicPropertySource
    static void packageProperties(DynamicPropertyRegistry registry) {
        try {
            Path root = Files.createTempDirectory("biblios-server-test");
            PackageTestFixture.Fixture fixture = PackageTestFixture.create(root);
            registry.add("biblios.package-dir", () -> fixture.packageDir().toString());
            registry.add("biblios.access-config", () -> fixture.accessConfig().toString());
            registry.add("biblios.provider", () -> "keycloak-local");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired
    MockMvc mockMvc;

    private static RequestPostProcessor groupMember() {
        return oidcLogin().clientRegistration(PackageTestFixture.registration("keycloak")).idToken(token -> token
            .claim("iss", PackageTestFixture.ISSUER).subject("anna")
            .claim("groups", List.of("agi-betrieb")));
    }

    private static RequestPostProcessor authenticatedWithoutGroup() {
        return oidcLogin().clientRegistration(PackageTestFixture.registration("keycloak")).idToken(token -> token.claim("iss", PackageTestFixture.ISSUER).subject("ben"));
    }

    @Test
    void anonymousHomeHidesProtectedDocumentation() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public Docs")))
            .andExpect(content().string(not(containsString("Internal Docs"))));
    }

    @Test
    void groupMemberSeesProtectedDocumentationOnHome() throws Exception {
        mockMvc.perform(get("/").with(groupMember()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public Docs")))
            .andExpect(content().string(containsString("Internal Docs")));
    }

    @Test
    void anonymousPublicPageIsReadable() throws Exception {
        mockMvc.perform(get("/public-docs/main/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public content")))
            .andExpect(content().string(not(containsString("Internal Docs"))));
    }

    @Test
    void anonymousProtectedPageIsHidden() throws Exception {
        mockMvc.perform(get("/internal-docs/main/"))
            .andExpect(status().isNotFound())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void authenticatedWithoutGroupIsForbidden() throws Exception {
        mockMvc.perform(get("/internal-docs/main/").with(authenticatedWithoutGroup()))
            .andExpect(status().isNotFound());
    }

    @Test
    void groupMemberReadsProtectedPage() throws Exception {
        mockMvc.perform(get("/internal-docs/main/").with(groupMember()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Internal content")));
    }

    @Test
    void anonymousProtectedComponentLandingIsHidden() throws Exception {
        mockMvc.perform(get("/internal-docs/"))
            .andExpect(status().isNotFound())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void searchIndexIsFilteredForAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/api/search-index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(content().string(containsString("public searchable content")))
            .andExpect(content().string(not(containsString("internal searchable content"))));
    }

    @Test
    void searchIndexContainsProtectedEntriesForGroupMembers() throws Exception {
        mockMvc.perform(get("/api/search-index").with(groupMember()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(content().string(containsString("internal searchable content")));
    }

    @Test
    void sharedAndPublicFilesAreReadableAnonymously() throws Exception {
        mockMvc.perform(get("/site-assets/styles.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("color: black")));
        mockMvc.perform(get("/public-docs/main/attachment.txt"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("public attachment")));
    }

    @Test
    void protectedFileRequiresTheGroup() throws Exception {
        mockMvc.perform(get("/internal-docs/main/secret.txt"))
            .andExpect(status().isNotFound())
            .andExpect(header().doesNotExist("Location"));

        mockMvc.perform(get("/internal-docs/main/secret.txt").with(authenticatedWithoutGroup()))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/internal-docs/main/secret.txt").with(groupMember()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("internal secret")))
            .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    void rangeRequestsAreServedForAllowedFiles() throws Exception {
        mockMvc.perform(get("/internal-docs/main/secret.txt")
                .header("Range", "bytes=0-7")
                .with(groupMember()))
            .andExpect(status().isPartialContent())
            .andExpect(header().string("Content-Range", "bytes 0-7/15"))
            .andExpect(content().string("internal"));
    }

    @Test
    void headRequestsAreCheckedAsWell() throws Exception {
        mockMvc.perform(head("/internal-docs/main/secret.txt"))
            .andExpect(status().isNotFound());
        mockMvc.perform(head("/internal-docs/main/secret.txt").with(groupMember()))
            .andExpect(status().isOk())
            .andExpect(header().string("Accept-Ranges", "bytes"));
    }

    @Test
    void authenticatedHomeShowsLogoutForm() throws Exception {
        mockMvc.perform(get("/").with(groupMember()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("action=\"/logout\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void logoutReturnsToPublicPortalWithoutImmediatelyStartingAnotherLogin() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        mockMvc.perform(post("/logout").session(session).with(groupMember()).with(csrf()))
            .andExpect(status().isFound()).andExpect(header().string("Location", "/"));
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }

    @Test
    void anonymousHomeHasNoLogoutForm() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("action=\"/logout\""))));
    }

    @Test
    void expiredIdentityHidesProtectedContent() throws Exception {
        long expiredTimestamp = System.currentTimeMillis() - 7_200_000L;
        mockMvc.perform(get("/internal-docs/main/")
                .with(groupMember())
                .sessionAttr(PortalSession.AUTHENTICATED_AT_ATTRIBUTE, expiredTimestamp))
            .andExpect(status().isNotFound())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void expiredIdentityOnPublicPageDoesNotGetRecreatedDuringRendering() throws Exception {
        mockMvc.perform(get("/public-docs/main/").with(groupMember())
                .sessionAttr(PortalSession.AUTHENTICATED_AT_ATTRIBUTE, System.currentTimeMillis() - 7_200_000L))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Internal Docs"))))
            .andExpect(content().string(containsString("class=\"login-link\"")));
    }

    @Test
    void anonymousPortalOffersExplicitLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"login-link\" href=\"/login\"")));
    }

    @Test
    void unknownPathsAreNotFound() throws Exception {
        mockMvc.perform(get("/does/not/exist"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/catalog.json"))
            .andExpect(status().isNotFound());
    }
}
