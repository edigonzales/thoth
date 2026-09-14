package guru.interlis.thoth.biblios.server;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RuleLossTest {
    static Path access;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        var f = PackageTestFixture.create(Files.createTempDirectory("biblios-rule-loss"));
        access = f.accessConfig();
        registry.add("biblios.package-dir", () -> f.packageDir().toString());
        registry.add("biblios.access-config", () -> f.accessConfig().toString());
    }
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"missing", "syntax", "policy", "removed"})
    void ruleFailureHidesAllDocumentationButKeepsThemeAvailable(String failure) throws Exception {
        var valid = Files.readString(access);
        var time = Files.getLastModifiedTime(access);
        mvc.perform(get("/public-docs/main/")).andExpect(status().isOk());
        switch (failure) {
            case "missing" -> Files.delete(access);
            case "syntax" -> Files.writeString(access, "policies: [broken");
            case "policy" -> Files.writeString(access, valid.replace("mode: restricted", "mode: unsupported"));
            default -> Files.writeString(access, "default: public\n");
        }
        if (Files.exists(access)) Files.setLastModifiedTime(access, time);
        try {
            mvc.perform(get("/").with(FileAuthorizationTest.user(true)))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("Public Docs"))))
                .andExpect(content().string(not(containsString("Internal Docs"))));
            mvc.perform(get("/api/search-index").with(FileAuthorizationTest.user(true)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
            for (String path : new String[] {"/public-docs/", "/public-docs/main/",
                "/public-docs/main/attachment.txt", "/internal-docs/main/", "/internal-docs/main/secret.txt"}) {
                mvc.perform(get(path)).andExpect(status().isNotFound());
                mvc.perform(get(path).with(FileAuthorizationTest.user(true))).andExpect(status().isNotFound());
            }
            mvc.perform(get("/site-assets/styles.css")).andExpect(status().isOk())
                .andExpect(content().string(containsString("color: black")));
        } finally {
            Files.writeString(access, valid);
            Files.setLastModifiedTime(access, time);
        }
        mvc.perform(get("/public-docs/main/")).andExpect(status().isOk());
        mvc.perform(get("/internal-docs/main/").with(FileAuthorizationTest.user(true)))
            .andExpect(status().isOk());
    }
}
