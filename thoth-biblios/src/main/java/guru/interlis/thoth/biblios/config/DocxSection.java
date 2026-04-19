package guru.interlis.thoth.biblios.config;

/**
 * Global DOCX output configuration.
 */
public final class DocxSection {
    private final boolean enabled;
    private final String referenceDoc;
    private final DocxFeaturesSection features;

    public DocxSection(boolean enabled, String referenceDoc, DocxFeaturesSection features) {
        this.enabled = enabled;
        this.referenceDoc = referenceDoc != null && !referenceDoc.isBlank() ? referenceDoc.trim() : null;
        this.features = features != null ? features : DocxFeaturesSection.defaults();
    }

    public boolean enabled() {
        return enabled;
    }

    public String referenceDoc() {
        return referenceDoc;
    }

    public DocxFeaturesSection features() {
        return features;
    }

    @Override
    public String toString() {
        return "DocxSection{enabled=" + enabled + ", referenceDoc='" + referenceDoc + "', features=" + features + "}";
    }
}
