package guru.interlis.thoth.biblios.config;

/**
 * Source-specific DOCX output configuration.
 */
public final class SourceDocxSection {
    private final Boolean enabled;
    private final String masterFile;
    private final String referenceDoc;
    private final SourceDocxFeaturesSection features;

    public SourceDocxSection(Boolean enabled, String masterFile, String referenceDoc, SourceDocxFeaturesSection features) {
        this.enabled = enabled;
        this.masterFile = masterFile != null && !masterFile.isBlank() ? masterFile.trim() : null;
        this.referenceDoc = referenceDoc != null && !referenceDoc.isBlank() ? referenceDoc.trim() : null;
        this.features = features;
    }

    public Boolean enabled() {
        return enabled;
    }

    public String masterFile() {
        return masterFile;
    }

    public String referenceDoc() {
        return referenceDoc;
    }

    public SourceDocxFeaturesSection features() {
        return features;
    }

    @Override
    public String toString() {
        return "SourceDocxSection{enabled=" + enabled + ", masterFile='" + masterFile + "', referenceDoc='" +
            referenceDoc + "', features=" + features + "}";
    }
}
