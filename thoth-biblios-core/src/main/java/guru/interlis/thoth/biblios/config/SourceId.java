package guru.interlis.thoth.biblios.config;

/** Shared namespace validation for configuration, package writers and readers. */
public final class SourceId {
    private SourceId() { }

    public static String validate(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\")
            || id.equals(".") || id.equals("..") || id.equalsIgnoreCase("site-assets")) {
            throw new IllegalArgumentException("Invalid source/component ID '" + id
                + "': expected one nonempty path segment other than '.', '..' or reserved 'site-assets'");
        }
        return id;
    }
}
