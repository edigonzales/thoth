package guru.interlis.thoth.biblios.config;

import java.util.List;
import java.util.Objects;

/**
 * Root configuration for a biblios site.
 * Maps to biblios.yml structure.
 */
public final class BibliosConfig {
    private final SiteSection site;
    private final OutputSection output;
    private final UiSection ui;
    private final PdfSection pdf;
    private final DocxSection docx;
    private final ContentSection content;

    public BibliosConfig(SiteSection site, OutputSection output, UiSection ui, PdfSection pdf, ContentSection content) {
        this(site, output, ui, pdf, new DocxSection(false, null, DocxFeaturesSection.defaults()), content);
    }

    public BibliosConfig(SiteSection site, OutputSection output, UiSection ui, PdfSection pdf, DocxSection docx, ContentSection content) {
        this.site = Objects.requireNonNull(site, "site section is required");
        this.output = Objects.requireNonNull(output, "output section is required");
        this.ui = Objects.requireNonNull(ui, "ui section is required");
        this.pdf = Objects.requireNonNull(pdf, "pdf section is required");
        this.docx = Objects.requireNonNull(docx, "docx section is required");
        this.content = Objects.requireNonNull(content, "content section is required");
    }

    public SiteSection site() {
        return site;
    }

    public OutputSection output() {
        return output;
    }

    public UiSection ui() {
        return ui;
    }

    public PdfSection pdf() {
        return pdf;
    }

    public DocxSection docx() {
        return docx;
    }

    public ContentSection content() {
        return content;
    }

    @Override
    public String toString() {
        return "BibliosConfig{site=" + site + ", output=" + output + ", pdf=" + pdf + ", docx=" + docx +
            ", content.sources=" + content.sources().size() + "}";
    }
}
