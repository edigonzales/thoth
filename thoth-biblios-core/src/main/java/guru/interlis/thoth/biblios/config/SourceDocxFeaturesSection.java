package guru.interlis.thoth.biblios.config;

/**
 * Source-specific DOCX feature overrides.
 */
public final class SourceDocxFeaturesSection {
    private final Boolean titlePage;
    private final Boolean toc;
    private final Boolean changeLog;

    public SourceDocxFeaturesSection(Boolean titlePage, Boolean toc, Boolean changeLog) {
        this.titlePage = titlePage;
        this.toc = toc;
        this.changeLog = changeLog;
    }

    public Boolean titlePage() {
        return titlePage;
    }

    public Boolean toc() {
        return toc;
    }

    public Boolean changeLog() {
        return changeLog;
    }

    @Override
    public String toString() {
        return "SourceDocxFeaturesSection{titlePage=" + titlePage + ", toc=" + toc + ", changeLog=" + changeLog + "}";
    }
}
