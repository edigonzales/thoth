package guru.interlis.thoth.biblios.config;

import java.util.Objects;

/**
 * Output configuration.
 */
public final class OutputSection {
    private final String dir;
    private final boolean clean;

    public OutputSection(String dir, boolean clean) {
        this.dir = Objects.requireNonNull(dir, "output.dir is required");
        this.clean = clean;
    }

    public String dir() {
        return dir;
    }

    public boolean clean() {
        return clean;
    }

    @Override
    public String toString() {
        return "OutputSection{dir='" + dir + "', clean=" + clean + "}";
    }
}
