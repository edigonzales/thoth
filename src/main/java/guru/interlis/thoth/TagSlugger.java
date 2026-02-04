package guru.interlis.thoth;

import java.text.Normalizer;
import java.util.Locale;

public final class TagSlugger {
    private TagSlugger() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "tag";
        }

        String value = input.trim();
        if (value.isEmpty()) {
            return "tag";
        }

        value = value
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss");

        value = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[\\s,]+", "-")
            .replaceAll("[^a-z0-9-]", "")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        return value.isEmpty() ? "tag" : value;
    }
}
