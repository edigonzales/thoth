package guru.interlis.thoth.biblios.config;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for a single documentation source (Git repository).
 */
public final class SourceConfig {
    private final String id;
    private final String displayName;
    private final String url;
    private final List<BranchConfig> branches;
    private final String startPath;
    private final String defaultVersion;
    private final NavigationConfig navigation;
    private final String startPage;
    private final RenderMode renderMode;
    private final String masterFile;
    private final Object revnumber;
    private final SidebarTocNumbersMode sidebarTocNumbers;
    private final SourcePdfSection pdf;
    private final SourceDocxSection docx;

    public SourceConfig(String id, String displayName, String url, List<BranchConfig> branches,
                        String startPath, String defaultVersion, NavigationConfig navigation, String startPage) {
        this(id, displayName, url, branches, startPath, defaultVersion, navigation, startPage,
            RenderMode.SPLIT, null, null, SidebarTocNumbersMode.OFF);
    }

    public SourceConfig(String id, String displayName, String url, List<BranchConfig> branches,
                        String startPath, String defaultVersion, NavigationConfig navigation, String startPage,
                        RenderMode renderMode, String masterFile) {
        this(id, displayName, url, branches, startPath, defaultVersion, navigation, startPage,
            renderMode, masterFile, null, SidebarTocNumbersMode.OFF);
    }

    public SourceConfig(String id, String displayName, String url, List<BranchConfig> branches,
                        String startPath, String defaultVersion, NavigationConfig navigation, String startPage,
                        RenderMode renderMode, String masterFile, Object revnumber, SidebarTocNumbersMode sidebarTocNumbers) {
        this(id, displayName, url, branches, startPath, defaultVersion, navigation, startPage,
            renderMode, masterFile, revnumber, sidebarTocNumbers, null);
    }

    public SourceConfig(String id, String displayName, String url, List<BranchConfig> branches,
                        String startPath, String defaultVersion, NavigationConfig navigation, String startPage,
                        RenderMode renderMode, String masterFile, Object revnumber, SidebarTocNumbersMode sidebarTocNumbers,
                        SourcePdfSection pdf) {
        this(id, displayName, url, branches, startPath, defaultVersion, navigation, startPage,
            renderMode, masterFile, revnumber, sidebarTocNumbers, pdf, null);
    }

    public SourceConfig(String id, String displayName, String url, List<BranchConfig> branches,
                        String startPath, String defaultVersion, NavigationConfig navigation, String startPage,
                        RenderMode renderMode, String masterFile, Object revnumber, SidebarTocNumbersMode sidebarTocNumbers,
                        SourcePdfSection pdf, SourceDocxSection docx) {
        this.id = Objects.requireNonNull(id, "source.id is required");
        this.displayName = Objects.requireNonNull(displayName, "source.display_name is required");
        this.url = Objects.requireNonNull(url, "source.url is required");
        Objects.requireNonNull(branches, "source.branches is required");
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("source.branches must not be empty for: " + id);
        }
        this.branches = List.copyOf(branches);
        this.startPath = startPath != null ? startPath : ".";
        this.defaultVersion = defaultVersion;
        this.navigation = navigation;
        this.startPage = startPage != null ? startPage : "index.adoc";
        this.renderMode = renderMode != null ? renderMode : RenderMode.SPLIT;
        this.masterFile = masterFile != null && !masterFile.isBlank() ? masterFile.trim() : null;
        this.revnumber = revnumber;
        this.sidebarTocNumbers = sidebarTocNumbers != null ? sidebarTocNumbers : SidebarTocNumbersMode.OFF;
        this.pdf = pdf;
        this.docx = docx;

        if (this.renderMode == RenderMode.SINGLE_PAGE && this.masterFile == null) {
            throw new IllegalArgumentException("source.master_file is required when source.render_mode is single_page");
        }
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String url() {
        return url;
    }

    public List<BranchConfig> branches() {
        return branches;
    }

    public String startPath() {
        return startPath;
    }

    public String defaultVersion() {
        return defaultVersion;
    }

    public NavigationConfig navigation() {
        return navigation;
    }

    public String startPage() {
        return startPage;
    }

    public RenderMode renderMode() {
        return renderMode;
    }

    public String masterFile() {
        return masterFile;
    }

    public Object revnumber() {
        return revnumber;
    }

    public SidebarTocNumbersMode sidebarTocNumbers() {
        return sidebarTocNumbers;
    }

    public SourcePdfSection pdf() {
        return pdf;
    }

    public SourceDocxSection docx() {
        return docx;
    }

    @Override
    public String toString() {
        return "SourceConfig{id='" + id + "', displayName='" + displayName + "', branches=" + branches.size() + "}";
    }
}
