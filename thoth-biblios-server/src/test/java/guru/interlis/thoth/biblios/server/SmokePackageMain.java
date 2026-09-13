package guru.interlis.thoth.biblios.server;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a small publication package for the native-image smoke test
 * (see the {@code generateSmokePackage} Gradle task).
 */
public final class SmokePackageMain {

    private SmokePackageMain() {
    }

    public static void main(String[] args) throws Exception {
        Path target = Path.of(args.length > 0 ? args[0] : "build/smoke-package");
        Files.createDirectories(target);
        PackageTestFixture.create(target);
        System.out.println("[smoke] package written to " + target.toAbsolutePath());
    }
}
