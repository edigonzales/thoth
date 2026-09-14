package guru.interlis.thoth.biblios.server.access;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.access.SubjectRef;
import guru.interlis.thoth.biblios.server.PackageTestFixture;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class AccessContentReloadTest {
    @TempDir Path root;
    private final PrincipalIdentity member = PrincipalIdentity.of("keycloak-local", "anna", null,
        Set.of(new SubjectRef("keycloak-local", "agi-betrieb")));

    @Test void equalSizeEqualTimestampAndRapidUpdatesStillRevokeAccess() throws Exception {
        var f = PackageTestFixture.create(root);
        var time = Files.getLastModifiedTime(f.accessConfig());
        var allowed = Files.readString(f.accessConfig());
        var denied = allowed.replace("agi-betrieb", "agi-externx");
        var service = service(f);
        long size = Files.size(f.accessConfig());
        for (int i = 0; i < 3; i++) {
            assertTrue(service.canAccessSource("internal-docs", member));
            write(f.accessConfig(), denied, time);
            assertEquals(size, Files.size(f.accessConfig()));
            assertFalse(service.canAccessSource("internal-docs", member));
            write(f.accessConfig(), allowed, time);
        }
        assertTrue(service.canAccessSource("internal-docs", member));
    }

    @Test void invalidUpdatesDenyAllWithoutRepeatedDiagnosticsAndRecoverAtSameTimestamp() throws Exception {
        var f = PackageTestFixture.create(root);
        var time = Files.getLastModifiedTime(f.accessConfig());
        var valid = Files.readString(f.accessConfig());
        var service = service(f);
        Logger logger = (Logger) LoggerFactory.getLogger(AccessService.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            for (String invalid : new String[] {"policies: [broken", valid.replace("mode: restricted", "mode: unsupported"),
                "default: public\n"}) {
                write(f.accessConfig(), invalid, time);
                assertFalse(service.canAccessSource("internal-docs", member));
                int logged = appender.list.size();
                for (int i = 0; i < 3; i++) {
                    assertFalse(service.isSourcePublic("public-docs"));
                    assertTrue(service.allowedSourceIds(member).isEmpty());
                }
                assertEquals(logged, appender.list.size(), "Unchanged invalid content must not log repeatedly");
                write(f.accessConfig(), valid, time);
                assertTrue(service.canAccessSource("internal-docs", member));
                assertTrue(service.isSourcePublic("public-docs"));
            }
            assertEquals(3, appender.list.stream().filter(e -> e.getFormattedMessage().contains("denying all")).count());
            assertEquals(3, appender.list.stream().filter(e -> e.getFormattedMessage().contains("restored")).count());
        } finally {
            logger.detachAppender(appender); appender.stop();
        }
    }

    private AccessService service(PackageTestFixture.Fixture f) {
        var p = new BibliosServerProperties(); p.setPackageDir(f.packageDir()); p.setAccessConfig(f.accessConfig());
        return new AccessService(new PublicationPackage(p, JsonMapper.builder().build()), p);
    }

    private void write(Path path, String content, FileTime time) throws Exception {
        Files.writeString(path, content); Files.setLastModifiedTime(path, time);
    }
}
