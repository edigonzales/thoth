package guru.interlis.thoth.blog;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public record Post(
    Path sourceRelativePath,
    String title,
    String author,
    LocalDate date,
    String status,
    List<TagRef> tags,
    String teaser,
    String coverImage,
    String htmlContent,
    String plainText,
    boolean usesInterlisLab,
    String url,
    String guid,
    Path outputRelativePath,
    String contentId,
    String language
) {
    public boolean published() { return "published".equals(status); }

    public String tagsAsText() {
        return tags.stream().map(TagRef::name).reduce((left, right) -> left + ", " + right).orElse("");
    }
}
