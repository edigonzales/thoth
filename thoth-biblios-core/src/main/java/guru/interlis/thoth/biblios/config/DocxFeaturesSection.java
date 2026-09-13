package guru.interlis.thoth.biblios.config;

/**
 * DOCX feature flags.
 */
public final class DocxFeaturesSection {
    private final boolean titlePage;
    private final boolean toc;
    private final boolean changeLog;

    public DocxFeaturesSection(boolean titlePage, boolean toc, boolean changeLog) {
        this.titlePage = titlePage;
        this.toc = toc;
        this.changeLog = changeLog;
    }

    public boolean titlePage() {
        return titlePage;
    }

    public boolean toc() {
        return toc;
    }

    public boolean changeLog() {
        return changeLog;
    }

    public static DocxFeaturesSection defaults() {
        return new DocxFeaturesSection(false, true, false);
    }

    @Override
    public String toString() {
        return "DocxFeaturesSection{titlePage=" + titlePage + ", toc=" + toc + ", changeLog=" + changeLog + "}";
    }
}
