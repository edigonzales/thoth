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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConcealmentTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        var fixture = PackageTestFixture.create(Files.createTempDirectory("biblios-concealment"));
        var mapper = JsonMapper.builder().build();
        var catalogPath = fixture.packageDir().resolve("catalog.json");
        var catalog = mapper.readTree(Files.readString(catalogPath));
        var version = (ObjectNode) catalog.get("components").get(1).get("versions").get(0);
        version.put("defaultPageRoute", "/internal-docs/main/start/");
        ((ObjectNode) version.get("pages").get(0)).put("route", "/internal-docs/main/start/");
        Files.writeString(catalogPath, mapper.writeValueAsString(catalog));
        registry.add("biblios.package-dir", () -> fixture.packageDir().toString());
        registry.add("biblios.access-config", () -> fixture.accessConfig().toString());
    }
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/main", "/main/", "/main/start", "/main/start/", "/main/secret.txt"})
    void missingAndForbiddenAreIndistinguishableExceptForTheRequestedReturnTarget(String suffix) throws Exception {
        for (boolean authenticated : new boolean[] {false, true}) {
            String[] bodies = new String[2];
            int i = 0;
            for (String component : new String[] {"internal-docs", "unknown-docs"}) {
                var request = get("/" + component + suffix);
                if (authenticated) request.with(FileAuthorizationTest.user(false));
                var result = mvc.perform(request).andExpect(status().isNotFound())
                    .andExpect(header().string("Cache-Control", "private, no-store"))
                    .andExpect(header().string("Vary", "Cookie"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(header().doesNotExist("Location")).andReturn();
                assertNull(result.getResponse().getHeader("Set-Cookie"));
                if (!authenticated) assertNull(result.getRequest().getSession(false), "404 must not create a saved-request session");
                bodies[i++] = normalizeLoginLink(result.getResponse().getContentAsString());
            }
            assertEquals(bodies[0], bodies[1]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"head", "range", "conditional"})
    void fileMetadataIsHiddenBeforeHttpResourceProcessing(String variant) throws Exception {
        for (String path : new String[] {"/internal-docs/main/secret.txt", "/unknown-docs/main/secret.txt"}) {
            var request = variant.equals("head") ? head(path) : get(path);
            if (variant.equals("range")) request.header("Range", "bytes=0-3");
            if (variant.equals("conditional")) request.header("If-Modified-Since", "Wed, 01 Jan 2031 00:00:00 GMT");
            var result = mvc.perform(request.with(FileAuthorizationTest.user(false))).andExpect(status().isNotFound());
            for (String header : new String[] {"Location", "ETag", "Last-Modified", "Content-Range", "Accept-Ranges"}) {
                result.andExpect(header().doesNotExist(header));
            }
            if (variant.equals("head")) result.andExpect(content().string(""));
        }
    }

    @Test void authorizedVisitorsStillReceiveComponentAndVersionRedirects() throws Exception {
        mvc.perform(get("/internal-docs").with(FileAuthorizationTest.user(true))).andExpect(status().isFound())
            .andExpect(header().string("Location", "/internal-docs/"));
        mvc.perform(get("/internal-docs/main/").with(FileAuthorizationTest.user(true))).andExpect(status().isFound())
            .andExpect(header().string("Location", "/internal-docs/main/start/"));
        mvc.perform(get("/internal-docs/main/start").with(FileAuthorizationTest.user(true))).andExpect(status().isFound())
            .andExpect(header().string("Location", "/internal-docs/main/start/"));
        mvc.perform(get("/internal-docs/main/start/").with(FileAuthorizationTest.user(true))).andExpect(status().isOk());
    }

    private static String normalizeLoginLink(String body) {
        return body.replaceAll("href=\"/login\\?returnTo=[^\"]*\"", "href=\"LOGIN\"");
    }
}
