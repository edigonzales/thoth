package guru.interlis.thoth.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InterlisLabMacroProcessor {
    public static final String DEFAULT_RUNNER = "cheerpj";
    public static final String ILI2C_JAR_URL_PLACEHOLDER = "__THOTH_INTERLIS_LAB_ILI2C_JAR_URL__";

    private static final Pattern MACRO_PATTERN = Pattern.compile("^\\s*interlis-lab::([^\\[]+)\\[(.*)]\\s*$");
    private static final List<String> DELIMITERS = List.of("----", "....", "====", "____", "****", "++++");

    private InterlisLabMacroProcessor() {
    }

    public static Result processHtml(String asciidoc, HtmlOptions options) {
        return process(asciidoc, options != null ? options : HtmlOptions.defaults(), Mode.HTML);
    }

    public static Result processFallback(String asciidoc) {
        return process(asciidoc, HtmlOptions.defaults(), Mode.FALLBACK);
    }

    private static Result process(String asciidoc, HtmlOptions options, Mode mode) {
        if (asciidoc == null || asciidoc.isBlank()) {
            return new Result(asciidoc != null ? asciidoc : "", List.of());
        }

        String[] lines = asciidoc.split("\\R", -1);
        StringBuilder output = new StringBuilder(asciidoc.length());
        List<LabReference> labs = new ArrayList<>();
        String activeDelimiter = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (activeDelimiter != null) {
                output.append(line);
                if (i < lines.length - 1) {
                    output.append('\n');
                }
                if (activeDelimiter.equals(trimmed)) {
                    activeDelimiter = null;
                }
                continue;
            }

            if (DELIMITERS.contains(trimmed)) {
                activeDelimiter = trimmed;
                output.append(line);
                if (i < lines.length - 1) {
                    output.append('\n');
                }
                continue;
            }

            Matcher matcher = MACRO_PATTERN.matcher(line);
            if (matcher.matches()) {
                String target = matcher.group(1).trim();
                Map<String, String> attributes = parseAttributeList(matcher.group(2));
                labs.add(new LabReference(target, attributes));
                output.append(mode == Mode.HTML ? renderHtml(target, attributes, options) : renderFallback(target, attributes));
            } else {
                output.append(line);
            }

            if (i < lines.length - 1) {
                output.append('\n');
            }
        }

        return new Result(output.toString(), List.copyOf(labs));
    }

    private static String renderHtml(String target, Map<String, String> attributes, HtmlOptions options) {
        String runner = valueOrDefault(attributes.get("runner"), options.defaultRunner());
        String title = valueOrDefault(attributes.get("title"), "INTERLIS Lab");
        String ili2cJarUrl = valueOrDefault(attributes.get("ili2c-jar-url"), options.ili2cJarUrl());
        String cheerpjLoaderUrl = valueOrDefault(attributes.get("cheerpj-loader-url"), options.cheerpjLoaderUrl());

        StringBuilder html = new StringBuilder();
        html.append("++++\n");
        html.append("<interlis-lab");
        appendAttribute(html, "src", target);
        appendAttribute(html, "runner", runner);
        appendAttribute(html, "ili2c-jar-url", ili2cJarUrl);
        appendAttribute(html, "cheerpj-loader-url", cheerpjLoaderUrl);
        appendAttribute(html, "storage-key", attributes.get("storage-key"));
        appendAttribute(html, "theme", attributes.get("theme"));
        if (isTruthy(attributes.get("readonly"))) {
            html.append(" readonly");
        }
        if (isTruthy(attributes.get("show-solution")) || isTruthy(attributes.get("showSolution"))) {
            html.append(" show-solution");
        }
        html.append(">");
        html.append("<a href=\"").append(escapeHtml(target)).append("\">").append(escapeHtml(title)).append("</a>");
        html.append("</interlis-lab>\n");
        html.append("++++");
        return html.toString();
    }

    private static String renderFallback(String target, Map<String, String> attributes) {
        String title = valueOrDefault(attributes.get("title"), "INTERLIS Lab");
        return "[NOTE]\n" +
            "====\n" +
            "Interactive INTERLIS Lab available in the HTML version: link:" +
            escapeLinkTarget(target) + "[" + escapeLinkText(title) + "].\n" +
            "====";
    }

    private static Map<String, String> parseAttributeList(String input) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (input == null || input.isBlank()) {
            return attributes;
        }

        for (String token : splitAttributeTokens(input)) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int separator = trimmed.indexOf('=');
            if (separator < 0) {
                attributes.put(normalizeKey(trimmed), "true");
                continue;
            }

            String key = normalizeKey(trimmed.substring(0, separator));
            String value = unquote(trimmed.substring(separator + 1).trim());
            if (!key.isBlank()) {
                attributes.put(key, value);
            }
        }
        return attributes;
    }

    private static List<String> splitAttributeTokens(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                current.append(ch);
                escaped = true;
                continue;
            }
            if ((ch == '"' || ch == '\'') && quote == 0) {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == quote) {
                quote = 0;
                current.append(ch);
                continue;
            }
            if (ch == ',' && quote == 0) {
                tokens.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        tokens.add(current.toString());
        return tokens;
    }

    private static String normalizeKey(String key) {
        return key != null ? key.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value != null ? value : "";
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void appendAttribute(StringBuilder html, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        html.append(' ').append(name).append("=\"").append(escapeHtml(value)).append('"');
    }

    private static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || "true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '"' -> escaped.append("&quot;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private static String escapeLinkTarget(String value) {
        return value != null ? value.replace("[", "%5B").replace("]", "%5D") : "";
    }

    private static String escapeLinkText(String value) {
        return value != null ? value.replace("]", "\\]") : "";
    }

    private enum Mode {
        HTML,
        FALLBACK
    }

    public record HtmlOptions(String ili2cJarUrl, String cheerpjLoaderUrl, String defaultRunner) {
        public static HtmlOptions defaults() {
            return new HtmlOptions("", "", DEFAULT_RUNNER);
        }
    }

    public record LabReference(String target, Map<String, String> attributes) {
        public LabReference {
            attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
        }
    }

    public record Result(String content, List<LabReference> labs) {
        public Result {
            content = content != null ? content : "";
            labs = labs != null ? List.copyOf(labs) : List.of();
        }

        public boolean usesInterlisLab() {
            return !labs.isEmpty();
        }
    }
}
