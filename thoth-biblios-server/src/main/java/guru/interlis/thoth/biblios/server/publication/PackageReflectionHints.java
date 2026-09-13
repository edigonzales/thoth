package guru.interlis.thoth.biblios.server.publication;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.List;

/**
 * Reflection hints for the publication package DTOs.
 *
 * <p>Jackson deserializes these records dynamically, which Spring AOT cannot
 * detect. Without the hints, reading {@code catalog.json} fails in native images.</p>
 */
public class PackageReflectionHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        List<Class<?>> recordTypes = List.of(
            PackageCatalog.class,
            PackageCatalog.Site.class,
            PackageCatalog.Ui.class,
            PackageCatalog.SourceEntry.class,
            PackageCatalog.Home.class,
            PackageCatalog.ComponentEntry.class,
            PackageCatalog.VersionEntry.class,
            PackageCatalog.PageEntry.class,
            ManifestDocument.class,
            ManifestDocument.Entry.class,
            SearchIndexEntry.class
        );
        for (Class<?> type : recordTypes) {
            hints.reflection().registerType(type,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.ACCESS_DECLARED_FIELDS);
        }
    }
}
