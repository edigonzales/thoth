package guru.interlis.thoth.blog;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups translations independently of their titles, filenames and public URLs. */
final class PostCatalog {
    private static final Comparator<Post> ORDER = Comparator.comparing(Post::date).reversed()
        .thenComparing(Post::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Post::url);
    private final Map<String, Map<String, Post>> variants = new LinkedHashMap<>();
    private final String defaultLanguage;

    PostCatalog(Collection<Post> posts, String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        for (Post post : posts.stream().sorted(Comparator.comparing(p -> p.sourceRelativePath().toString())).toList()) {
            Map<String, Post> group = variants.computeIfAbsent(post.contentId(), ignored -> new LinkedHashMap<>());
            Post previous = group.putIfAbsent(post.language(), post);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate content id '" + post.contentId() + "' in language "
                    + post.language() + ": " + previous.sourceRelativePath() + " and " + post.sourceRelativePath());
            }
        }
    }

    Post publishedVariant(String id, String language) {
        Post post = variants.getOrDefault(id, Map.of()).get(language);
        return post != null && post.published() ? post : null;
    }

    List<Post> visiblePosts(String language) {
        return variants.keySet().stream().map(id -> {
            Post selected = publishedVariant(id, language);
            return selected != null ? selected : publishedVariant(id, defaultLanguage);
        }).filter(java.util.Objects::nonNull).sorted(ORDER).toList();
    }

    List<Post> publishedPosts(String language) {
        return variants.keySet().stream().map(id -> publishedVariant(id, language))
            .filter(java.util.Objects::nonNull).sorted(ORDER).toList();
    }
}
