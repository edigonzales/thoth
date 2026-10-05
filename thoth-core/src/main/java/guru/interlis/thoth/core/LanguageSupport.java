package guru.interlis.thoth.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;

/** Language tags and public paths shared by the static generators and portals. */
public final class LanguageSupport {
    private final String defaultLanguage;
    private final List<String> languages;
    private final boolean languageDirectories;

    public LanguageSupport(String defaultLanguage, List<String> languages, boolean languageDirectories) {
        this.defaultLanguage = normalize(defaultLanguage);
        List<String> normalized = new ArrayList<>();
        for (String language : languages) {
            String tag = normalize(language);
            if (normalized.contains(tag)) {
                throw new IllegalArgumentException("Duplicate language: " + language);
            }
            normalized.add(tag);
        }
        if (!normalized.contains(this.defaultLanguage)) {
            throw new IllegalArgumentException("Languages must include the default language: " + defaultLanguage);
        }
        this.languages = List.copyOf(normalized);
        this.languageDirectories = languageDirectories;
    }

    public static String normalize(String language) {
        try {
            Locale locale = new Locale.Builder().setLanguageTag(language.trim()).build();
            if (locale.getLanguage().isEmpty() || "und".equals(locale.getLanguage())) {
                throw new IllegalArgumentException("Invalid language tag: " + language);
            }
            return locale.toLanguageTag();
        } catch (IllformedLocaleException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid language tag: " + language, e);
        }
    }

    public String defaultLanguage() { return defaultLanguage; }
    public List<String> languages() { return languages; }

    /** Unprefixed sources belong to the default language, including during migration. */
    public SourcePath sourcePath(Path source) {
        Path path = source.normalize();
        if (path.isAbsolute() || path.startsWith("..")) {
            throw new IllegalArgumentException("Source path must be relative: " + source);
        }
        if (languageDirectories && path.getNameCount() > 1) {
            String first = path.getName(0).toString();
            for (String language : languages) {
                if (language.equalsIgnoreCase(first)) {
                    return new SourcePath(language, path.subpath(1, path.getNameCount()));
                }
            }
        }
        return new SourcePath(defaultLanguage, path);
    }

    public String route(String language, String path) {
        String tag = normalize(language);
        if (!languages.contains(tag)) {
            throw new IllegalArgumentException("Unknown language: " + language);
        }
        String relative = path.startsWith("/") ? path.substring(1) : path;
        Path normalized = Path.of(relative).normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..")) {
            throw new IllegalArgumentException("Route must stay inside the site: " + path);
        }
        return (tag.equals(defaultLanguage) ? "/" : "/" + tag + "/") + relative;
    }

    public Path outputPath(Path source) {
        SourcePath resolved = sourcePath(source);
        return Path.of(route(resolved.language(), resolved.path().toString().replace('\\', '/')).substring(1));
    }

    public static String displayName(String language) {
        Locale locale = Locale.forLanguageTag(language);
        return switch (locale.getLanguage()) {
            case "de" -> "Deutsch";
            case "en" -> "English";
            default -> locale.getDisplayName(locale);
        };
    }

    public record SourcePath(String language, Path path) { }
}
