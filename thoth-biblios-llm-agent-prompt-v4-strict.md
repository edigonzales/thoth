# Thoth Biblios – ultrastrenger LLM-Agenten-Prompt

Du arbeitest in einem bestehenden Git-Repository namens **`thoth`**.

## Verbindliche Referenzen

Bevor du Änderungen vornimmst, musst du die Spezifikationsdatei vollständig lesen und als maßgebliche Referenz behandeln:

- `thoth-biblios-spec-v2.md`

Diese Spezifikation ist die **fachliche, architektonische und testbezogene Referenz** für die Umsetzung.

### Vorrangregeln

Bei Widersprüchen gilt diese Reihenfolge:

1. `thoth-biblios-spec-v2.md`
2. dieser Prompt
3. implizite Annahmen aus dem bestehenden Code

Wenn du einen Widerspruch findest, musst du:

- ihn kurz benennen,
- die Spezifikation bevorzugen,
- die getroffene Entscheidung in der Dokumentation oder im Code knapp festhalten.

---

## Projektkontext

Das aktuelle Projekt ist ein Java-basierter Static-Site-Generator für AsciiDoc-Blogs. Es nutzt bereits:

- Java
- Gradle
- Picocli
- FreeMarker
- AsciidoctorJ
- jsoup

Das bestehende Produkt soll erhalten bleiben und als **`thoth-blog`** weitergeführt werden.
Zusätzlich soll ein neues Schwesterprodukt entstehen: **`thoth-biblios`**.
Ein gemeinsamer technischer Kern soll in **`thoth-core`** extrahiert werden.

---

## Zielarchitektur

Baue das Projekt zu einem **Gradle-Multi-Project** um mit den Modulen:

1. `thoth-core`
2. `thoth-blog`
3. `thoth-biblios`

### Architekturregeln

- Teile nur **technische Infrastruktur** in `thoth-core`.
- Verschiebe **keine Blog-Fachlogik** in `thoth-core`.
- Verschiebe **keine Biblios-Fachlogik** in `thoth-core`.
- `thoth-blog` soll funktional möglichst unverändert bleiben.
- `thoth-biblios` soll ein neues Produkt für Multi-Repo-Asciidoc-Dokumentation werden.
- Rendering muss mit **AsciidoctorJ** erfolgen, nicht mit Asciidoctor.js.
- Das neue Biblios-Produkt soll sich in wichtigen Punkten **antora-artig** verhalten, Antora aber nicht 1:1 kopieren.

---

## Verbindliche Produktentscheidungen

Diese Entscheidungen sind bereits getroffen und dürfen nicht eigenmächtig geändert werden:

- **Java-Version:** 25
- **Navigation:** `nav.yml`
- **Versionsanzeige:** `display_version`
- **Suche im MVP:** global
- **Redirect von `/<component>/` auf Default-Version:** erst später, nicht im MVP

---

## Zielverhalten von `thoth-biblios`

`thoth-biblios` muss im MVP mindestens Folgendes leisten:

- liest eine YAML-Konfigurationsdatei `biblios.yml`
- unterstützt mehrere Git-Repositories als Content-Quellen
- unterstützt mehrere Branches pro Repository als veröffentlichte Versionen
- unterstützt je Quelle einen konfigurierbaren Startpfad
- baut aus allen Quellen einen Site-Katalog
- erzeugt ein HTML-Dokuportal
- erzeugt eine globale Startseite
- erzeugt pro Dokumentation eine Landing Page
- erzeugt Dokumentationsseiten mit:
  - Doku-Switcher
  - Versions-Switcher
  - Navigation
  - Breadcrumbs
  - optional Prev/Next
- verwendet URLs im Format `/<component>/<version>/...`
- bietet `build` und `serve` als CLI-Kommandos

---

## Konfigurationsrahmen für `biblios.yml`

Verwende das in der Spezifikation definierte Modell. Die folgenden Elemente müssen unterstützt werden:

- `site`
- `output`
- `ui`
- `content.sources`
- `id`
- `display_name`
- `url`
- `branches`
- `start_path`
- `default_version`
- `display_version`
- `navigation.file`
- `start_page`

Navigation erfolgt im MVP über **`nav.yml`**.

---

## Strikte Arbeitsweise

Du musst iterativ arbeiten und nach jeder Phase einen überprüfbaren Zustand herstellen.

### Pflichtablauf

1. Analysiere die bestehende Projektstruktur.
2. Lies die Spezifikation vollständig.
3. Erstelle einen präzisen Migrationsplan.
4. Setze zuerst die Gradle-Multi-Project-Struktur um.
5. Extrahiere anschließend den technischen Kern nach `thoth-core`.
6. Stelle sicher, dass `thoth-blog` danach weiterhin funktioniert.
7. Erzeuge ein minimales, aber lauffähiges `thoth-biblios`.
8. Implementiere danach schrittweise:
   - YAML-Config-Lader
   - Git-Source-Fetching
   - Branch-Resolution
   - Site-Catalog
   - Routing
   - HTML-Templating
   - globale Suche
   - Doku- und Versions-Switcher
9. Ergänze Tests und Dokumentation in jeder Phase.
10. Ändere bestehendes Blog-Verhalten nur dann, wenn es technisch zwingend ist.

### Verboten

- Keine spekulativen Großumbauten ohne Zwischentest.
- Keine Vermischung von Blog- und Biblios-Fachlogik.
- Keine stillschweigenden Abweichungen von der Spezifikation.
- Keine ungetesteten „fertigen“ Phasen.
- Keine Einführung unnötiger Frameworks oder Technologiewechsel.

---

## Verbindliche Testanforderungen

Du musst auf **drei Ebenen** testen:

### 1. Unit-Tests
Mindestens für:

- Parsing von `biblios.yml`
- Mapping auf Konfigurationsobjekte
- Branch-Pattern-Auswertung
- `display_version`-Auflösung
- Routing und URL-Erzeugung
- Katalogaufbau
- Navigationslogik aus `nav.yml`
- Template-Helfer
- Breadcrumb-Erzeugung

### 2. Integrationstests
Mindestens für:

- vollständiger Build des Gradle-Multi-Projects
- `thoth-blog` funktioniert nach der Modularisierung weiterhin
- `thoth-biblios` lädt Beispiel-Repositories über Git/JGit
- mehrere Branches werden korrekt als Versionen erkannt
- `display_version` wird korrekt ins Modell übernommen
- Rendering mit AsciidoctorJ funktioniert im Zusammenspiel mit Templates, Routing und Katalog
- globaler Suchindex wird korrekt erzeugt
- `build` erzeugt die erwartete Site-Struktur

### 3. End-to-End-Tests
Mindestens für:

- kompletter Build einer Beispiel-`biblios.yml`
- Ausgabe einer vollständigen HTML-Site
- globale Startseite vorhanden
- Komponenten-Landingpages vorhanden
- Dokumentationsseiten unter erwarteten URLs vorhanden
- Doku-Switcher sichtbar und korrekt befüllt
- Versions-Switcher sichtbar und korrekt befüllt
- Navigation sichtbar und korrekt aufgebaut
- Suchfunktion verwendet globalen Index
- `serve` startet erfolgreich und liefert Seiten aus

### Testpflicht pro Schritt

Nach **jedem größeren Implementierungsschritt** musst du:

- alle Unit-Tests ausführen
- alle Integrationstests ausführen
- die relevanten E2E-Tests ausführen
- Build-Fehler beheben, bevor du weiterarbeitest
- kurz dokumentieren, welche Tests hinzugefügt oder angepasst wurden

### CI-Anforderung

Die Tests müssen so strukturiert sein, dass sie in CI ausführbar sind.
Mindestens muss es eine klare Ausführung für diese Ebenen geben:

- `./gradlew test`
- `./gradlew integrationTest`
- eigener Task für E2E-Tests, falls sinnvoll, z. B. `./gradlew e2eTest`

Unit-Tests allein reichen **nicht** aus.

---

## Implementierungsphasen

## Phase 1 – Analyse und Modularisierung

### Ziele

- bestehende Projektstruktur analysieren
- Spezifikation gegen Ist-Zustand abgleichen
- Gradle-Multi-Project einführen
- Module `thoth-core`, `thoth-blog`, `thoth-biblios` anlegen
- bestehende Anwendung in `thoth-blog` überführen
- gemeinsames technisches Fundament in `thoth-core` extrahieren
- sicherstellen, dass `thoth-blog` weiterhin baut und läuft

### Deliverables

- neue Gradle-Struktur
- lauffähiges `thoth-blog`
- minimales `thoth-biblios`-CLI
- erste README-Aktualisierung
- Tests für die Modularisierung

### Definition of Done

Diese Phase ist nur abgeschlossen, wenn:

- das Projekt als Multi-Project baut
- `thoth-blog` funktional weiterhin startet
- `thoth-biblios` als minimales CLI vorhanden ist
- kein produktbezogener Biblios-Code in `thoth-blog` liegt
- kein blogbezogener Fachcode in `thoth-core` liegt
- die neuen Modulgrenzen dokumentiert sind
- Tests grün sind

### Pflicht-Checkliste vor Phasenabschluss

- [ ] Spezifikation gelesen und abgeglichen
- [ ] Modulstruktur angelegt
- [ ] Build erfolgreich
- [ ] `thoth-blog` startet
- [ ] `thoth-biblios` startet
- [ ] Tests laufen grün
- [ ] README aktualisiert

---

## Phase 2 – Biblios-MVP-Kern

### Ziele

- `biblios.yml` laden und validieren
- `nav.yml` laden und verarbeiten
- Git-Quellen via JGit auflösen
- Branches als Versionen bestimmen
- `display_version` unterstützen
- Site-Catalog aufbauen
- Routingmodell implementieren
- HTML-Site mit globaler Startseite und Komponenten-Landingpages erzeugen

### Deliverables

- Konfigurationsmodell
- Git-Quellenauflösung
- Branch-Resolution
- Katalogmodell
- Nav-Modell
- Seitenmodell
- HTML-Templates für Biblios
- globale Suche

### Definition of Done

Diese Phase ist nur abgeschlossen, wenn:

- `biblios.yml` aus Beispieldateien zuverlässig geladen wird
- mehrere Repositories verarbeitet werden können
- mehrere Branches je Repo als Versionen erscheinen
- `display_version` im UI-Modell verfügbar ist
- `nav.yml` für die Navigation genutzt wird
- URLs dem Schema `/<component>/<version>/...` folgen
- globale Startseite erzeugt wird
- Komponenten-Landingpages erzeugt werden
- globale Suche erzeugt wird
- Tests grün sind

### Pflicht-Checkliste vor Phasenabschluss

- [ ] Beispiel-`biblios.yml` vorhanden
- [ ] Beispiel-`nav.yml` vorhanden
- [ ] Git-Fetching funktioniert
- [ ] Branch-Resolution getestet
- [ ] `display_version` getestet
- [ ] Routing getestet
- [ ] globale Startseite erzeugt
- [ ] Komponenten-Landingpages erzeugt
- [ ] globale Suche erzeugt
- [ ] alle Tests grün

---

## Phase 3 – UI, Navigation und Nutzerfluss

### Ziele

- Doku-Switcher implementieren
- Versions-Switcher implementieren
- Breadcrumbs implementieren
- optional Prev/Next ergänzen
- Layout verfeinern
- serve-Workflow für Biblios brauchbar machen

### Deliverables

- UI-Modelle für Switcher
- Sidebar-/Navigationsintegration
- Breadcrumb-Logik
- stabile Templates
- Serve-Unterstützung

### Definition of Done

Diese Phase ist nur abgeschlossen, wenn:

- Doku-Switcher sichtbar und korrekt befüllt ist
- Versions-Switcher sichtbar und korrekt befüllt ist
- Navigation korrekt dargestellt wird
- Breadcrumbs auf Inhaltsseiten sichtbar sind
- `serve` eine funktionierende HTML-Site ausliefert
- wichtige E2E-Flows erfolgreich getestet sind

### Pflicht-Checkliste vor Phasenabschluss

- [ ] Doku-Switcher vorhanden
- [ ] Versions-Switcher vorhanden
- [ ] Navigation sichtbar
- [ ] Breadcrumbs sichtbar
- [ ] `serve` funktioniert
- [ ] E2E-Tests für zentrale Flows grün

---

## Phase 4 – Robustheit und Dokumentation

### Ziele

- Fehlermeldungen verbessern
- zusätzliche Randfälle absichern
- Testabdeckung erweitern
- Architektur dokumentieren
- Migrationshinweise dokumentieren

### Deliverables

- verbesserte Validierungen
- robuste Fehlerbehandlung
- Architektur-README
- Beispielprojekte
- Abschlussdokumentation

### Definition of Done

Diese Phase ist nur abgeschlossen, wenn:

- zentrale Fehlerszenarien verständlich behandelt werden
- Test-Suite stabil und CI-fähig ist
- README und Entwicklerdokumentation vollständig sind
- die wichtigsten Architekturentscheidungen dokumentiert sind
- Akzeptanzkriterien der Spezifikation nachweisbar erfüllt sind

### Pflicht-Checkliste vor Phasenabschluss

- [ ] Fehlermeldungen verbessert
- [ ] Randfälle getestet
- [ ] CI-Ausführung dokumentiert
- [ ] README vollständig
- [ ] Architektur dokumentiert
- [ ] Akzeptanzkriterien abgeglichen

---

## Definition of Done für das Gesamtprojekt

Das Gesamtprojekt ist nur dann fertig, wenn **alle** folgenden Punkte erfüllt sind:

- das Repository ist ein funktionierendes Gradle-Multi-Project
- `thoth-core`, `thoth-blog` und `thoth-biblios` sind sauber getrennt
- `thoth-blog` bleibt lauffähig und funktional weitgehend stabil
- `thoth-biblios` kann mindestens zwei Repositories aus `biblios.yml` verarbeiten
- `thoth-biblios` kann Branches als Versionen publizieren
- `nav.yml` wird als Navigation verwendet
- `display_version` wird im Modell und UI unterstützt
- globale Suche ist implementiert
- URL-Schema `/<component>/<version>/...` ist aktiv
- globale Startseite und Komponenten-Landingpages werden erzeugt
- Doku-Switcher und Versions-Switcher funktionieren
- `build` und `serve` funktionieren für Biblios
- Unit-, Integrations- und E2E-Tests sind vorhanden und grün
- README und Architektur-Dokumentation sind aktualisiert
- die Umsetzung ist mit der Spezifikation nachvollziehbar abgeglichen

---

## Konkrete Deliverables

Du musst am Ende mindestens diese Artefakte liefern:

- aktualisierte Gradle-Multi-Project-Struktur
- funktionierende Modulgrenzen
- lauffähiges `thoth-blog`
- lauffähiges `thoth-biblios`
- Beispiel-`biblios.yml`
- Beispiel-`nav.yml`
- Tests auf drei Ebenen
- aktualisierte README(s)
- Architekturhinweise zur Aufteilung in Core/Blog/Biblios

---

## Berichtspflicht des Agenten

Nach jedem größeren Schritt musst du knapp berichten:

1. was du geändert hast
2. welche Architekturentscheidung du getroffen hast
3. welche Tests du ausgeführt hast
4. ob alles grün ist
5. was der nächste Schritt ist

Wenn etwas fehlschlägt, benenne klar:

- was fehlgeschlagen ist
- warum es fehlgeschlagen ist
- wie du es behebst

---

## Beginne jetzt mit dieser Reihenfolge

1. lies `thoth-biblios-spec-v2.md` vollständig
2. analysiere die bestehende Projektstruktur
3. schreibe einen präzisen Migrationsplan
4. setze Phase 1 um
5. teste gründlich
6. dokumentiere den Stand
7. gehe erst dann zu Phase 2 über
