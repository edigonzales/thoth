package guru.interlis.thoth.biblios.publication;

import java.util.Collection;
import java.util.Map;

/**
 * Minimal JSON encoder for the publication package. Supports maps, collections,
 * strings, numbers, booleans and {@code null}. Keeping this in core avoids adding
 * a JSON library to the build CLI while the server uses Jackson for reading.
 */
public final class JsonSupport {

    private JsonSupport() {
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            appendString(out, text);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Number number) {
            out.append(number);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(out, map);
        } else if (value instanceof Collection<?> collection) {
            appendCollection(out, collection);
        } else {
            appendString(out, value.toString());
        }
    }

    private static void appendMap(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            appendString(out, entry.getKey().toString());
            out.append(':');
            append(out, entry.getValue());
        }
        out.append('}');
    }

    private static void appendCollection(StringBuilder out, Collection<?> collection) {
        out.append('[');
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                out.append(',');
            }
            first = false;
            append(out, item);
        }
        out.append(']');
    }

    private static void appendString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
