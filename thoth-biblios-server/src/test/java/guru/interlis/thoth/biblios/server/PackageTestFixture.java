package guru.interlis.thoth.biblios.server;

import guru.interlis.thoth.biblios.catalog.ComponentVersion;
import guru.interlis.thoth.biblios.catalog.DocComponent;
import guru.interlis.thoth.biblios.catalog.DocPage;
import guru.interlis.thoth.biblios.catalog.SiteCatalog;
import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.BibliosConfigParser;
import guru.interlis.thoth.biblios.publication.JsonSupport;
import guru.interlis.thoth.biblios.publication.ManifestEntry;
import guru.interlis.thoth.biblios.publication.PublicationCatalogWriter;
import guru.interlis.thoth.biblios.publication.PublicationManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Creates a small publication package for server tests: one public and one
 * policy-protected documentation, shared assets, protected files and search data.
 */
public final class PackageTestFixture {

    public static final String ISSUER = "http://localhost:8090/realms/biblios-dev";

    private PackageTestFixture() {
    }

    public static org.springframework.security.oauth2.client.registration.ClientRegistration registration(String id) {
        return org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId(id)
            .clientId("test-client").authorizationGrantType(
                org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/login/oauth2/code/" + id).scope("openid")
            .authorizationUri("http://localhost/auth").tokenUri("http://localhost/token")
            .jwkSetUri("http://localhost/keys").build();
    }

    public record Fixture(Path packageDir, Path accessConfig) {
    }

    public static Fixture create(Path root) throws IOException {
        Path packageDir = root.resolve("package");
        Path accessConfig = root.resolve("access.yml");
        Files.createDirectories(packageDir);

        Path configFile = root.resolve("biblios.yml");
        Files.writeString(configFile, """
            site:
              title: Access Test Portal
              url: https://docs.example.org
              default_language: en
            output:
              dir: site
            content:
              sources:
                - id: public-docs
                  display_name: Public Docs
                  url: https://example.org/public.git
                  branches:
                    - name: main
                  access_policy: public
                - id: internal-docs
                  display_name: Internal Docs
                  url: https://example.org/internal.git
                  branches:
                    - name: main
                  access_policy: internal
              groups:
                - title: All docs
                  sources: [public-docs, internal-docs]
            """);
        BibliosConfig config = new BibliosConfigParser().parse(configFile);

        DocPage publicPage = page("public-docs", "Public Home");
        DocPage internalPage = page("internal-docs", "Internal Home");
        SiteCatalog catalog = new SiteCatalog(List.of(
            new DocComponent("public-docs", "Public Docs", "main", List.of(
                new ComponentVersion("public-docs", "main", "Aktuell", "main", "index.adoc", null, List.of(publicPage)))),
            new DocComponent("internal-docs", "Internal Docs", "main", List.of(
                new ComponentVersion("internal-docs", "main", "Aktuell", "main", "index.adoc", null, List.of(internalPage))))
        ));

        write(packageDir.resolve("pages/public-docs/main/index.html"), "<p>Public content</p>");
        write(packageDir.resolve("pages/internal-docs/main/index.html"), "<p>Internal content</p>");
        write(packageDir.resolve("files/site-assets/styles.css"), "body { color: black; }");
        write(packageDir.resolve("files/public-docs/main/attachment.txt"), "public attachment");
        write(packageDir.resolve("files/internal-docs/main/secret.txt"), "internal secret");

        write(packageDir.resolve("catalog.json"),
            PublicationCatalogWriter.write(config, catalog, "/site-assets/site-logo.svg"));

        PublicationManifest manifest = new PublicationManifest();
        manifest.add(ManifestEntry.sourcePage("/public-docs/main/", "public-docs"));
        manifest.add(ManifestEntry.sourcePage("/internal-docs/main/", "internal-docs"));
        manifest.add(ManifestEntry.sharedFile("site-assets/styles.css"));
        manifest.add(ManifestEntry.sourceFile("public-docs/main/attachment.txt", "public-docs"));
        manifest.add(ManifestEntry.sourceFile("internal-docs/main/secret.txt", "internal-docs"));
        manifest.add(ManifestEntry.internal("search-index.json"));
        manifest.add(ManifestEntry.internal("catalog.json"));
        write(packageDir.resolve("manifest.json"), manifest.toJson());

        List<Map<String, Object>> searchIndex = List.of(
            Map.of("component", "public-docs", "version", "main", "displayVersion", "Aktuell",
                "kind", "page", "title", "Public Home", "pageTitle", "Public Home",
                "sectionPath", "Public Home", "sectionLevel", 0, "route", "/public-docs/main/",
                "content", "public searchable content"),
            Map.of("component", "internal-docs", "version", "main", "displayVersion", "Aktuell",
                "kind", "page", "title", "Internal Home", "pageTitle", "Internal Home",
                "sectionPath", "Internal Home", "sectionLevel", 0, "route", "/internal-docs/main/",
                "content", "internal searchable content")
        );
        write(packageDir.resolve("search-index.json"), JsonSupport.write(searchIndex));

        Files.writeString(accessConfig, """
            default: deny
            policies:
              public:
                mode: public
              internal:
                mode: restricted
                allow:
                  groups:
                    - provider: keycloak-local
                      id: agi-betrieb
            """);
        return new Fixture(packageDir, accessConfig);
    }

    private static DocPage page(String componentId, String title) {
        return new DocPage(
            componentId, "main", "index.adoc", "file:///index.adoc", "index",
            title, title, "/" + componentId + "/main/",
            "<p>" + title + " content</p>",
            List.of(new DocPage.Breadcrumb(title, null)),
            null, null, null, null
        );
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
