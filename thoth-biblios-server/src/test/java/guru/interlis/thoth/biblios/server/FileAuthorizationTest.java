package guru.interlis.thoth.biblios.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FileAuthorizationTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var fixture = PathTestFixture.create(Files.createTempDirectory("biblios-path-test"));
        registry.add("biblios.package-dir", () -> fixture.packageDir().toString());
        registry.add("biblios.access-config", () -> fixture.accessConfig().toString());
    }
    @Autowired MockMvc mvc;

    static RequestPostProcessor user(boolean member) {
        return oidcLogin().clientRegistration(PackageTestFixture.registration("keycloak"))
            .idToken(t -> t.claim("iss", PackageTestFixture.ISSUER).subject("reader").claim("groups", member ? List.of("agi-betrieb") : List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/public+docs/main/attachment.txt", "/public%2Bdocs/main/attachment.txt"})
    void plusPathsAlwaysCheckTheProtectedFile(String path) throws Exception {
        URI uri = URI.create(path);
        mvc.perform(get(uri)).andExpect(status().isNotFound());
        mvc.perform(get(uri).with(user(false))).andExpect(status().isNotFound());
        mvc.perform(get(uri).with(user(true))).andExpect(status().isOk())
            .andExpect(content().string("PROTECTED PLUS"));
    }

    @Test
    void unicodeAndSpacesRemainReadableWithoutAliasing() throws Exception {
        mvc.perform(get(URI.create("/public%20docs/main/attachment.txt")))
            .andExpect(status().isOk()).andExpect(content().string("PUBLIC SPACE"));
        mvc.perform(get(URI.create("/public-docs/main/%C3%BCber%20space.txt")))
            .andExpect(status().isOk()).andExpect(content().string("UNICODE"));
        mvc.perform(get("/public+docs/main/unlisted.txt")).andExpect(status().isNotFound());
        mvc.perform(get("/public-docs/main/link.txt")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "head", "range", "conditional"})
    void everyHttpVariantAuthorizesBeforeServing(String variant) throws Exception {
        String path = "/public+docs/main/attachment.txt";
        for (int identity = 0; identity < 3; identity++) {
            var request = variant.equals("head") ? head(path) : get(path);
            if (variant.equals("range")) request.header("Range", "bytes=0-8");
            if (variant.equals("conditional")) request.header("If-Modified-Since", "Wed, 01 Jan 2031 00:00:00 GMT");
            if (identity > 0) request.with(user(identity == 2));
            var result = mvc.perform(request);
            if (identity < 2) {
                result.andExpect(status().isNotFound())
                    .andExpect(content().string(not(containsString("PROTECTED PLUS"))));
            } else {
                result.andExpect(header().string("Cache-Control", "private, no-store"));
                switch (variant) {
                    case "range" -> result.andExpect(status().isPartialContent())
                        .andExpect(content().string("PROTECTED"));
                    case "conditional" -> result.andExpect(status().isNotModified());
                    case "head" -> result.andExpect(status().isOk()).andExpect(content().string(""));
                    default -> result.andExpect(status().isOk()).andExpect(content().string("PROTECTED PLUS"));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/public-docs/%FF",
        "/public-docs/%C0%AF", "/public-docs/../catalog.json", "/public-docs/%2e%2e/catalog.json"})
    void malformedAndTraversalRequestsAreRejected(String path) throws Exception {
        mvc.perform(get("/placeholder").with(request -> { request.setRequestURI(path); return request; }))
            .andExpect(status().isBadRequest());
    }
}
