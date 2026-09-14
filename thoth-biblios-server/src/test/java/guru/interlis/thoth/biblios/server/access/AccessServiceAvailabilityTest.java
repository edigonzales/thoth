package guru.interlis.thoth.biblios.server.access;

import guru.interlis.thoth.biblios.server.PackageTestFixture;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessServiceAvailabilityTest {
    @TempDir Path root;

    private AccessService service(PackageTestFixture.Fixture f) {
        var p = new BibliosServerProperties();
        p.setPackageDir(f.packageDir()); p.setAccessConfig(f.accessConfig());
        return new AccessService(new PublicationPackage(p, JsonMapper.builder().build()), p);
    }

    @Test void missingFileNeverDefaultsToPublicAtStartupOrRuntime() throws Exception {
        var f = PackageTestFixture.create(root);
        Path catalog = f.packageDir().resolve("catalog.json");
        Files.writeString(catalog, Files.readString(catalog)
            .replace("\"accessPolicy\":\"public\"", "\"accessPolicy\":null")
            .replace("\"accessPolicy\":\"internal\"", "\"accessPolicy\":null"));
        Files.writeString(f.accessConfig(), "default: public\n");
        var service = service(f);
        assertTrue(service.canAccessSource("internal-docs", null));
        Files.delete(f.accessConfig());
        assertFalse(service.canAccessSource("internal-docs", null));
        assertTrue(service.allowedSourceIds(null).isEmpty());
        assertThrows(IllegalStateException.class, () -> service(f));
    }

    @Test void lossAndInvalidRestorationStayDeniedUntilValidEvenAtSameMtime() throws Exception {
        var f = PackageTestFixture.create(root);
        var time = Files.getLastModifiedTime(f.accessConfig());
        var valid = Files.readString(f.accessConfig());
        var service = service(f);
        assertTrue(service.isSourcePublic("public-docs"));
        Files.delete(f.accessConfig());
        assertFalse(service.isSourcePublic("public-docs"));
        for (String invalid : new String[] {"policies: [broken", "default: public\n"}) {
            Files.writeString(f.accessConfig(), invalid);
            Files.setLastModifiedTime(f.accessConfig(), time);
            assertTrue(service.allowedSourceIds(null).isEmpty());
        }
        Files.writeString(f.accessConfig(), valid);
        Files.setLastModifiedTime(f.accessConfig(), time);
        assertTrue(service.isSourcePublic("public-docs"));
        assertFalse(service.canAccessSource("internal-docs", null));
    }

    @Test void readFailuresDenyAndMetadataIsNotRequired() throws Exception {
        var f = PackageTestFixture.create(root);
        var service = service(f);
        assertTrue(service.isSourcePublic("public-docs"));
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.readString(f.accessConfig())).thenThrow(new AccessDeniedException("simulated"));
            assertTrue(service.allowedSourceIds(null).isEmpty());
            assertThrows(IllegalStateException.class, () -> service(f));
        }
        assertTrue(service.isSourcePublic("public-docs"));
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.getLastModifiedTime(f.accessConfig())).thenThrow(new AccessDeniedException("simulated stat"));
            assertTrue(service.isSourcePublic("public-docs"));
        }
        assertTrue(service.isSourcePublic("public-docs"));
    }
}
