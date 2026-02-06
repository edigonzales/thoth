package guru.interlis.thoth;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostParser {
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("^:([^:]+):\\s*(.*)$");

    private final Asciidoctor asciidoctor;

    public PostParser(Asciidoctor asciidoctor) {
        this.asciidoctor = asciidoctor;
    }

    public Post parse(Path sourceFile, Path inputRoot) throws IOException {
        Path sourceRelativePath = inputRoot.relativize(sourceFile);
        List<String> lines = java.nio.file.Files.readAllLines(sourceFile, StandardCharsets.UTF_8);

        if (lines.isEmpty() || !"---".equals(lines.get(0).trim())) {
            throw new IllegalArgumentException("Post is missing front matter delimiter at top: " + sourceRelativePath);
        }

        int secondDelimiter = findSecondDelimiter(lines);
        if (secondDelimiter < 0) {
            throw new IllegalArgumentException("Post is missing closing front matter delimiter: " + sourceRelativePath);
        }

        List<String> headerLines = lines.subList(1, secondDelimiter);
        if (headerLines.size() < 3) {
            throw new IllegalArgumentException("Front matter must contain title, author and date: " + sourceRelativePath);
        }

        String titleLine = headerLines.get(0).trim();
        if (!titleLine.startsWith("= ")) {
            throw new IllegalArgumentException("Title line must start with '= ': " + sourceRelativePath);
        }

        String title = titleLine.substring(2).trim();
        String author = headerLines.get(1).trim();
        LocalDate date = LocalDate.parse(headerLines.get(2).trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        Map<String, String> attributes = parseAttributes(headerLines.subList(3, headerLines.size()));

        String body = String.join("\n", lines.subList(secondDelimiter + 1, lines.size()));
        List<Boolean> sourceBlockLineNumbers = detectSourceBlockLineNumbers(body);
        String renderedHtml = renderAsciiDoc(body, sourceFile);
        String normalizedHtml = rewriteRelativeLinks(renderedHtml, sourceRelativePath.getParent(), sourceBlockLineNumbers);

        Document document = Jsoup.parseBodyFragment(normalizedHtml);
        String plainText = collapseWhitespace(document.text());

        String teaser = resolveTeaser(attributes.get("thoth-teaser"), plainText);
        String coverImage = resolveCover(attributes.get("thoth-cover-image"), document, sourceRelativePath.getParent());

        String status = attributes.getOrDefault("thoth-status", "published").trim();
        List<TagRef> tags = parseTags(attributes.get("thoth-tags"));

        String relativeWithoutExtension = removeExtension(toUnixPath(sourceRelativePath));
        String url = "/" + relativeWithoutExtension + "/";
        String guid = relativeWithoutExtension + "/";
        Path outputRelativePath = Path.of(relativeWithoutExtension).resolve("index.html");

        return new Post(
            sourceRelativePath,
            title,
            author,
            date,
            status,
            tags,
            teaser,
            coverImage,
            normalizedHtml,
            plainText,
            url,
            guid,
            outputRelativePath
        );
    }

    private int findSecondDelimiter(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if ("---".equals(lines.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, String> parseAttributes(List<String> attributeLines) {
        Map<String, String> attributes = new HashMap<>();
        for (String line : attributeLines) {
            String trimmed = line.trim();
            Matcher matcher = ATTRIBUTE_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                attributes.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }
        return attributes;
    }

    private String renderAsciiDoc(String body, Path sourceFile) {
        AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
        attributes.attribute("source-highlighter", "null");

        OptionsBuilder options = org.asciidoctor.Options.builder()
            .backend("html5")
            .safe(SafeMode.UNSAFE)
            .standalone(false)
            .baseDir(sourceFile.getParent().toFile())
            .attributes(attributes.build());

        return asciidoctor.convert(body, options.build());
    }

    private String rewriteRelativeLinks(String html, Path sourceDirectory, List<Boolean> sourceBlockLineNumbers) {
        Document document = Jsoup.parseBodyFragment(html);

        for (Element element : document.select("[src]")) {
            String rewritten = resolveSiteUrl(element.attr("src"), sourceDirectory, false);
            element.attr("src", rewritten);
        }

        for (Element element : document.select("[href]")) {
            String rewritten = resolveSiteUrl(element.attr("href"), sourceDirectory, true);
            element.attr("href", rewritten);
        }

        normalizeCodeBlocksForPrism(document, sourceBlockLineNumbers);

        return document.body().html();
    }

    private void normalizeCodeBlocksForPrism(Document document, List<Boolean> sourceBlockLineNumbers) {
        int sourceBlockIndex = 0;
        for (Element code : document.select("pre > code")) {
            String language = detectLanguage(code);
            if (language == null) {
                continue;
            }

            String normalizedLanguage = normalizeLanguageAlias(language);
            code.addClass("language-" + normalizedLanguage);
            code.removeAttr("data-lang");

            Element pre = code.parent();
            if (pre != null && "pre".equals(pre.tagName())) {
                pre.addClass("language-" + normalizedLanguage);
                if (sourceBlockIndex < sourceBlockLineNumbers.size() && sourceBlockLineNumbers.get(sourceBlockIndex)) {
                    pre.addClass("line-numbers");
                }
            }

            sourceBlockIndex++;
        }
    }

    private String detectLanguage(Element code) {
        String dataLang = code.attr("data-lang").trim();
        if (!dataLang.isEmpty()) {
            return dataLang;
        }

        for (String className : code.classNames()) {
            if (className.startsWith("language-")) {
                return className.substring("language-".length());
            }
        }
        return null;
    }

    private String normalizeLanguageAlias(String language) {
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "yml" -> "yaml";
            case "sh", "shell", "shell-session", "zsh", "bash" -> "bash";
            case "html", "xml", "svg", "mathml" -> "markup";
            default -> normalized;
        };
    }

    private List<Boolean> detectSourceBlockLineNumbers(String body) {
        List<Boolean> lineNumbersByBlock = new ArrayList<>();
        String[] lines = body.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (!isBlockAttributeLine(trimmed)) {
                continue;
            }

            String attributeList = trimmed.substring(1, trimmed.length() - 1);
            if (!isSourceBlockAttributeList(attributeList)) {
                continue;
            }

            int next = i + 1;
            while (next < lines.length) {
                String candidate = lines[next].trim();
                if (candidate.isEmpty() || candidate.startsWith(".") || candidate.startsWith("//")) {
                    next++;
                    continue;
                }

                if (isListingDelimiter(candidate)) {
                    lineNumbersByBlock.add(hasLineNumbersOption(attributeList));
                }
                break;
            }
        }

        return lineNumbersByBlock;
    }

    private boolean isBlockAttributeLine(String line) {
        return line.startsWith("[") && line.endsWith("]") && line.length() >= 2;
    }

    private boolean isSourceBlockAttributeList(String attributeList) {
        for (String token : attributeList.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("source") || normalized.startsWith("source%")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLineNumbersOption(String attributeList) {
        for (String token : attributeList.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("linenums")
                || normalized.startsWith("linenums=")
                || normalized.contains("%linenums")
                || (normalized.startsWith("opts=") && normalized.contains("linenums"))
                || (normalized.startsWith("options=") && normalized.contains("linenums"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isListingDelimiter(String line) {
        return "----".equals(line) || "....".equals(line);
    }

    private String resolveCover(String overrideValue, Document document, Path sourceDirectory) {
        if (overrideValue != null && !overrideValue.isBlank()) {
            return resolveSiteUrl(overrideValue.trim(), sourceDirectory, false);
        }

        Element firstImage = document.selectFirst("img[src]");
        if (firstImage == null) {
            return null;
        }

        return firstImage.attr("src");
    }

    private String resolveTeaser(String overrideValue, String plainText) {
        if (overrideValue != null && !overrideValue.isBlank()) {
            return overrideValue.trim();
        }

        if (plainText.length() <= 200) {
            return plainText;
        }

        return plainText.substring(0, 200).trim();
    }

    private List<TagRef> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }

        List<TagRef> tags = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String rawTag : rawTags.split(",")) {
            String tag = rawTag.trim();
            if (tag.isEmpty()) {
                continue;
            }
            String key = tag.toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                tags.add(new TagRef(tag, TagSlugger.slugify(tag)));
            }
        }
        return tags;
    }

    private String resolveSiteUrl(String rawValue, Path sourceDirectory, boolean convertAdocLinks) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }

        String value = rawValue.trim();
        if (isExternalOrAbsolute(value)) {
            return value;
        }

        String fragment = "";
        int hashIndex = value.indexOf('#');
        if (hashIndex >= 0) {
            fragment = value.substring(hashIndex);
            value = value.substring(0, hashIndex);
        }

        String query = "";
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            query = value.substring(queryIndex);
            value = value.substring(0, queryIndex);
        }

        if (value.isBlank()) {
            return rawValue;
        }

        Path base = sourceDirectory == null ? Path.of("") : sourceDirectory;
        Path resolved = base.resolve(value).normalize();

        if (resolved.startsWith("..")) {
            return rawValue;
        }

        String normalized = toUnixPath(resolved);
        if (convertAdocLinks && normalized.endsWith(".adoc")) {
            normalized = removeExtension(normalized) + "/";
        }

        return "/" + normalized + query + fragment;
    }

    private boolean isExternalOrAbsolute(String value) {
        return value.startsWith("http://")
            || value.startsWith("https://")
            || value.startsWith("//")
            || value.startsWith("mailto:")
            || value.startsWith("tel:")
            || value.startsWith("data:")
            || value.startsWith("/")
            || value.startsWith("#");
    }

    private String collapseWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private String removeExtension(String value) {
        if (value.endsWith(".adoc")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    private String toUnixPath(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }
}
