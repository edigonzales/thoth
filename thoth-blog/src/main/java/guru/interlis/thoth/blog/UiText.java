package guru.interlis.thoth.blog;

import java.util.Locale;
import java.util.Map;

/** Built-in UI translations. Content and site metadata are supplied by authors. */
public final class UiText {
    private UiText() { }

    public static Map<String, String> forLanguage(String language) {
        boolean german = "de".equals(Locale.forLanguageTag(language).getLanguage());
        return Map.ofEntries(
            Map.entry("home", german ? "Startseite" : "Home"),
            Map.entry("archive", german ? "Archiv" : "Archive"),
            Map.entry("archiveTitle", german ? "Blog-Archiv" : "Blog Archive"),
            Map.entry("subscribe", german ? "Abonnieren" : "Subscribe"),
            Map.entry("search", german ? "Suche" : "Search"),
            Map.entry("searchPosts", german ? "Beiträge durchsuchen" : "Search posts"),
            Map.entry("language", german ? "Sprache" : "Language"),
            Map.entry("languageHome", german ? "Startseite; keine Übersetzung" : "home; no translation"),
            Map.entry("readMore", german ? "weiterlesen" : "read more"),
            Map.entry("read", german ? "Lesen:" : "Read"),
            Map.entry("postedOn", german ? "Veröffentlicht am" : "Posted on"),
            Map.entry("words", german ? "Wörter" : "words"),
            Map.entry("writtenBy", german ? "Geschrieben von" : "Written by"),
            Map.entry("tags", german ? "Schlagwörter" : "Tags"),
            Map.entry("tag", german ? "Schlagwort" : "Tag"),
            Map.entry("onlyAvailable", german ? "Nur verfügbar auf" : "Only available in"),
            Map.entry("resultsFor", german ? "Ergebnisse für \"{query}\"" : "Results for \"{query}\""),
            Map.entry("noResults", german ? "Keine Ergebnisse für \"{query}\"." : "No results for \"{query}\"."),
            Map.entry("enterQuery", german ? "Suchbegriff in das Suchfeld eingeben." : "Enter a query in the search field."),
            Map.entry("enterSearchTerm", german ? "Oben einen Suchbegriff eingeben." : "Enter a search term above."),
            Map.entry("searchFailed", german ? "Suchindex konnte nicht geladen werden." : "Search index could not be loaded."),
            Map.entry("expandedImage", german ? "Vergrößertes Beitragsbild" : "Expanded post image"),
            Map.entry("closeImage", german ? "Bild schließen" : "Close image"),
            Map.entry("copyCode", german ? "Codeblock kopieren" : "Copy code block content"),
            Map.entry("copiedCode", german ? "Codeblock kopiert" : "Copied code block content"),
            Map.entry("copyFailed", german ? "Kopieren fehlgeschlagen" : "Copy failed"),
            Map.entry("theme", german ? "Zwischen hellem, dunklem und Systemdesign wechseln" : "Switch between dark, light and system mode")
        );
    }
}
