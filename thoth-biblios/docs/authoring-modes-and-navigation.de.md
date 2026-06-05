# Wie Biblios Struktur Auflöst

Diese Anleitung beschreibt das aktuell implementierte Verhalten von `thoth-biblios`, wenn AsciiDoc-Quellen in eine Website umgewandelt werden.

Sie konzentriert sich auf die Fragen beim Schreiben der Doku, die in der Praxis leicht durcheinandergeraten:

- Kapitel- und Abschnittsnummerierung
- `render_mode: split` versus `render_mode: single_page`
- `nav.yml`
- `start_page`
- `master_file`
- `sidebar_toc_numbers`
- `content_section_numbers`
- Dokumenttitelzeilen wie `= Mein Titel`
- AsciiDoc-`doctype`

Das hier ist bewusst eine Verhaltens-Anleitung und keine API-Referenz für Konfigurationsschlüssel. Die Beschreibungen unten entsprechen den aktuellen Codepfaden in `CatalogBuilder`, `AsciidoctorRenderer`, `BibliosPdfGenerator`, `BibliosDocxGenerator` und den zugehörigen Biblios-Tests.

Die englische Originalfassung dieser Seite steht unter [authoring-modes-and-navigation.md](authoring-modes-and-navigation.md).

## Schnelle Entscheidungstabelle

| `render_mode` | `nav.yml` | `start_page` | Nummerierungseinstellungen | Benötigte Quelldateien | Erzeugtes Ergebnis | Wichtige Besonderheiten |
|-----|-----|-----|-----|-----|-----|-----|
| `split` | Gültige `navigation.file`, die erfolgreich geparst wird | Wird als Root-Route der Version verwendet. Wenn die Datei existiert, aber nicht in `nav.yml` steht, setzt Biblios sie an den Anfang der Build-Liste. | `ui.content_section_numbers` steuert die gerenderten Abschnittsnummern innerhalb jeder Seite. | Einzelne `.adoc`-Seiten sowie `nav.yml`, falls konfiguriert. | Eine HTML-Seite pro referenzierter Quellseite. Sidebar, Breadcrumbs und Prev/Next-Reihenfolge kommen aus `nav.yml`. | Bei nicht leerem `nav.yml` baut Biblios aktuell nur die dort referenzierten Seiten sowie `start_page`, falls diese existiert und in nav fehlte. |
| `split` | Datei fehlt, YAML ist ungültig oder gar nicht konfiguriert | Wird weiterhin als bevorzugte Root-Seite der Version verwendet, falls die Datei existiert. | `ui.content_section_numbers` steuert weiterhin die gerenderten Abschnittsnummern. | Einzelne `.adoc`-Seiten. Keine Nav-Datei erforderlich. | Eine HTML-Seite pro gefundener `.adoc`-Datei. Die Suche läuft rekursiv unter `start_path`. | Auf diesem Fallback-Pfad protokolliert Biblios Warnungen und entdeckt `.adoc`-Dateien automatisch, sortiert nach relativem Pfad. Die Sidebar-Reihenfolge folgt dann dieser Fundreihenfolge. |
| `split` | Gültiges `nav.yml`, aber `start_page` ist darin nicht aufgeführt | Wenn die Datei existiert, fügt Biblios sie vorne ein und gibt ihr die Root-Route `/<component>/<version>/`. | Wie bei anderen `split`-Builds. | `start_page` plus die in nav gelisteten Seiten. | Die Startseite wird gebaut, obwohl sie nicht in nav steht, und wird zur Root-Seite der Version. | Die Seite wird gebaut, ihre Nav-Breadcrumbs und Platzierung stammen aber trotzdem nicht aus einem expliziten Nav-Eintrag. |
| `single_page` | Für Seitengenerierung und Sidebar-Struktur ignoriert | Wird nicht zur Auswahl der Root-Seite verwendet. Die Version wird immer aus `master_file` gerendert. | `ui.content_section_numbers` steuert, ob nummerierte Überschriften im Inhalt gerendert werden. `ui.sidebar_toc_depth` steuert, wie viele Überschriftenebenen in den generierten Sidebar-Baum eingehen. | `master_file` ist Pflicht. | Genau eine HTML-Seite für die gesamte Version. Sidebar-Einträge werden aus den Überschriften des gerenderten Master-Dokuments erzeugt. | `navigation.file` steuert die Sidebar in diesem Modus nicht. `start_page` ist hier nicht das maßgebliche Dokument. |
| `single_page` | Ignoriert | Ignoriert | `content_section_numbers: on` plus `sidebar_toc_numbers: on` setzt Kapitelnummern vor die Sidebar-Einträge. | `master_file`. | Eine Seite mit generierter, überschriftenbasierter Sidebar. | `sidebar_toc_numbers` betrifft nur die Labels in der Single-Page-Sidebar. Auf `split` hat es keinen Einfluss. |
| `single_page` | Ignoriert | Ignoriert | `content_section_numbers: off` unterdrückt nummerierte Überschriften im Inhalt und unterdrückt dadurch auch Nummernpräfixe in der Sidebar, selbst wenn `sidebar_toc_numbers: on` gesetzt ist. | `master_file`. | Eine Seite mit unnummerierter, überschriftenbasierter Sidebar. | `sidebar_toc_numbers` ist faktisch von `content_section_numbers` abhängig. |

## Grundregeln

### `split`-Modus

Verwende `split`, wenn jede Quelldatei zu einer eigenen HTML-Seite werden soll.

Aktuelles Verhalten:

- Wenn `navigation.file` konfiguriert ist und die Datei erfolgreich geparst wird, verwendet Biblios diesen Nav-Baum als Quelle für Seitenreihenfolge, Breadcrumbs und Sidebar-Struktur.
- In diesem Fall sammelt Biblios die Seitenpfade aus `nav.yml` und baut genau diese Seiten.
- Wenn die konfigurierte `start_page` existiert, aber in `nav.yml` fehlt, fügt Biblios sie vorne in die Build-Liste ein, damit die Root-Seite der Version weiterhin funktioniert.
- Wenn keine verwendbare Nav-Datei vorhanden ist, entdeckt Biblios `.adoc`-Dateien rekursiv unter `start_path`.
- Die Routen sind seitenbasiert: `guide.adoc` wird zu `/<component>/<version>/guide/`, während `start_page` zu `/<component>/<version>/` wird.

Wichtige Folge:

- `nav.yml` ist im `split`-Modus nicht zwingend erforderlich.
- Aber wenn `nav.yml` vorhanden und gültig ist, ist es nicht bloß Metadaten. Es definiert effektiv, welche Seiten gebaut werden, mit Ausnahme des oben beschriebenen zusätzlichen `start_page`-Einfügens.

### `single_page`-Modus

Verwende `single_page`, wenn ein Master-Dokument die komplette Version rendern soll.

Aktuelles Verhalten:

- `master_file` ist erforderlich.
- Biblios rendert das Master-Dokument genau einmal und erzeugt genau eine HTML-Seite für die Root der Version.
- Sidebar-Einträge werden aus dem gerenderten Überschriftenbaum abgeleitet, nicht aus `nav.yml`.
- `navigation.file` wird in diesem Modus für Sidebar- und Seitengenerierung ignoriert.
- `start_page` wählt in diesem Modus nicht das Quelldokument aus.

Praktische Folge:

- Für `single_page` sollte man in AsciiDoc-Dokumentmontage mit `include::...[]` denken, nicht in Seitenlisten innerhalb von `nav.yml`.

## Verhalten der Nummerierung

### `ui.content_section_numbers`

Diese Einstellung steuert, ob Biblios Asciidoctor anweist, Abschnittsnummern im HTML-Inhalt zu rendern.

- `on` setzt `sectnums` beim HTML-Rendering.
- `off` entfernt `sectnums` beim HTML-Rendering.

Auswirkungen:

- Im `split`-Modus ändert sich die Nummerierung innerhalb jeder gerenderten Seite.
- Im `single_page`-Modus ändert sich die Nummerierung innerhalb des Master-Dokuments und außerdem, ob Biblios Nummern in der generierten Sidebar anzeigen kann.

### `sidebar_toc_numbers`

Dies ist eine source-spezifische Einstellung und ist nur in `single_page` relevant.

- `off` belässt Sidebar-Einträge ohne Präfix.
- `on` setzt Überschriftennummern wie `1.` oder `1.1.` vor generierte Sidebar-Einträge, aber nur wenn `ui.content_section_numbers: on` gesetzt ist.

Diese Einstellung ändert nicht:

- Nav-Labels im `split`-Modus
- die Nummerierung im gerenderten Inhalt selbst

### `[unnumbered]` und `[.appendix]` in `single_page`

Biblios filtert die generierte Single-Page-Sidebar aus dem Überschriftenbaum.

Aktuelles Verhalten:

- `[unnumbered]`-Abschnitte werden standardmäßig aus der generierten Sidebar ausgeblendet.
- Wenn ein unnummerierter Abschnitt zusätzlich die Rolle `[.appendix]` hat, behält Biblios ihn in der Sidebar.

Dadurch ist dieses Muster nützlich für Anhänge, die in der Sidebar sichtbar bleiben sollen, ohne wie nummerierte Kapitel auszusehen:

```adoc
[unnumbered]
[.appendix]
== Anhang A - Referenztabellen
```

## `nav.yml`: Wann Es Relevant Ist

`nav.yml` ist nicht in allen Fällen erforderlich.

### In `split`

- Wenn es vorhanden und gültig ist, definiert es die gebaute Seitenmenge sowie die Reihenfolge in Sidebar und Breadcrumbs.
- Wenn es fehlt oder unbrauchbar ist, fällt Biblios auf rekursive `.adoc`-Erkennung zurück.

### In `single_page`

- Es wird nicht zur Seitengenerierung und nicht für den Sidebar-Baum verwendet.
- Entscheidend ist die Überschriftenstruktur im `master_file`.

Die Kurzfassung lautet also:

- `nav.yml` ist in `split` optional
- `nav.yml` ist in `single_page` operativ irrelevant

## Ist Eine Dokumenttitelzeile (`= ...`) Pflicht?

Kurzantwort: nein, technisch nicht in jedem Fall, praktisch aber oft schon.

### Was ohne `= Titel` passiert

Bei HTML-Builds:

- Biblios fragt zuerst Asciidoctor nach dem Dokumenttitel.
- Wenn dieser im `split`-Modus leer ist, fällt Biblios auf das erste gerenderte `<h1>` zurück, falls eines existiert.
- Wenn auch das fehlschlägt, fällt Biblios auf den Dateinamen ohne `.adoc` zurück.
- In `single_page` fällt Biblios bei leerem gerendertem Dokumenttitel auf `display_name` aus `biblios.yml` zurück.

Ein Dokument ohne oberste `= Titel`-Zeile kann also trotzdem gebaut werden.

### Warum es meistens trotzdem die richtige Wahl ist

Eine saubere Dokumenttitelzeile sollte man verwenden, wenn:

- die Seite einen stabilen, bewusst gewählten Seitentitel haben soll
- man vorhersehbare Seitenlabels außerhalb nav-gesteuerter Kontexte möchte
- das Dokument ein echtes Master-Dokument für `single_page` ist
- dieselbe Quelle auch für PDF oder DOCX wiederverwendet wird

Für kleine Include-Fragmente kann man ohne `= Titel` auskommen, aber für eigenständige Seiten und Master-Dateien sollte man es in der Praxis als erforderlich betrachten.

## Was `doctype` in Biblios Heute Verändert

`doctype` spielt eine Rolle, aber nicht auf dieselbe Weise für HTML, PDF und DOCX.

### HTML-Site-Generierung

Für HTML verzweigt Biblios in seiner eigenen Site-Logik nicht anhand von `doctype`.

Was tatsächlich passiert:

- Biblios lädt und rendert das Dokument über Asciidoctor.
- Asciidoctor sieht den `doctype` des Dokuments und kann dadurch die Dokumentsemantik ändern.
- Biblios verwendet anschließend das gerenderte HTML und die extrahierten Überschriftendaten.

Für HTML beeinflusst `doctype` die Ausgabe also indirekt über Asciidoctor-Semantik, nicht über Biblios-spezifische Routing- oder Navigationsregeln.

### PDF und DOCX mit expliziten Master-Dateien

Wenn man verwendet:

- `render_mode: single_page` mit dessen `master_file`
- oder `pdf.master_file`
- oder `docx.master_file`

dann ist der `doctype` dieses Master-Dokuments das, was Asciidoctor erhält.

### PDF und DOCX für Split-Quellen ohne explizite Artefakt-Master-Dateien

Wenn Biblios viele Split-Seiten zu einem Artefakt zusammenbauen muss, erzeugt es ein temporäres Aggregate-Master-Dokument und erzwingt:

```adoc
:doctype: book
```

Das geschieht im Aggregate-Master-Pfad sowohl für PDF als auch für DOCX.

Praktische Folge:

- HTML-`split`-Seiten behalten das, was ihre Quelldateien deklarieren, oder den Asciidoctor-Standard, wenn nichts deklariert ist.
- Aggregierte PDF-/DOCX-Exporte aus Split-Quellen werden als `book` zusammengesetzt.

### Aktuelle Biblios-spezifische `doctype`-Logik

Heute macht Biblios selbst nur diese `doctype`-bezogenen Dinge:

- es liest den geladenen `doctype` aus Asciidoctor aus
- es bewahrt diesen Wert im DOCX-Normalisierungsmodell
- es erzwingt `book`, wenn es für Split-Quellen Aggregate-Master-Dokumente für PDF/DOCX erzeugt

Es schaltet aktuell nicht HTML-Routing, Sidebar-Generierung oder Seitenauswahl anhand von `doctype` um.

## Praktische Empfehlungen

- Verwende `split`, wenn Autorinnen und Autoren in einzelnen Seiten denken und nav-gesteuerte Reihenfolge möchten.
- Verwende `single_page`, wenn Autorinnen und Autoren in einem zusammengebauten Handbuch mit überschriftenbasierter Sidebar-Navigation denken.
- Behandle `master_file` in `single_page` als verpflichtende Entwurfsentscheidung, nicht als Nachgedanken.
- Behandle `= Titel` nur bei Fragmenten als optional; bei eigenständigen Seiten und Master-Dateien sollte es vorhanden sein.
- Gehe nicht davon aus, dass `nav.yml` in `split` nur harmlose Metadaten enthält; wenn es gültig ist, bestimmt es sehr stark, was gebaut wird.
- Wenn Anhänge in einer Single-Page-Sidebar sichtbar bleiben sollen, kombiniere `[unnumbered]` mit `[.appendix]`.

## Verwandte Referenzen

Die Konfigurationsreferenz bleibt in [README.md](../README.md). Verwende diese Datei für die Syntax der Optionen und diese Anleitung für Verhalten und Erwartungen beim Schreiben der Doku.
