# IMPLEMENTATION_PLAN

> Dieses Dokument wird während der Umsetzung **fortlaufend aktualisiert**.
> Es dient als verbindliches Arbeits- und Nachweis-Dokument für die Implementierung von `thoth-core`, `thoth-blog` und `thoth-biblios`.
>
> Maßgebliche Referenzen:
> 1. `thoth-biblios-spec-v2.md`
> 2. `thoth-biblios-llm-agent-prompt-v4-strict.md`
>
> Bei Widersprüchen gilt die Spezifikation vor dem Prompt. Dieses Dokument darf diese Referenzen **konkretisieren**, aber nicht stillschweigend abschwächen.

---

## 1. Projektüberblick

### 1.1 Ziel

Kurze Zusammenfassung des Vorhabens in 3–8 Sätzen:

- Was soll erreicht werden?
- Welche Produkte entstehen?
- Was bleibt erhalten?
- Was ist der MVP?
- Welche Architekturprinzipien sind verbindlich?

**Aktueller Stand:**

- Status: `not started | in progress | blocked | done`
- Letzte Aktualisierung: `YYYY-MM-DD`
- Verantwortliche Instanz: `LLM-Agent / Mensch / Team`

### 1.2 Referenzdokumente

- Spezifikation: `thoth-biblios-spec-v2.md`
- Agenten-Prompt: `thoth-biblios-llm-agent-prompt-v4-strict.md`
- Weitere relevante Dokumente:
  - `...`
  - `...`

### 1.3 Nicht-Ziele

Hier die aktuell gültigen Nicht-Ziele eintragen, damit Scope Creep sichtbar bleibt.

- `...`
- `...`
- `...`

---

## 2. Architektur-Leitplanken

Diese Regeln dürfen während der Umsetzung nicht verletzt werden.

- `thoth-core` enthält nur gemeinsamen technischen Kern.
- `thoth-blog` bleibt funktional möglichst nah am bestehenden Produkt.
- `thoth-biblios` enthält die neue Doku-Domäne.
- Rendering erfolgt mit **AsciidoctorJ**.
- Build-System ist **Gradle Multi-Project**.
- Ziel-Java-Version ist **Java 25**.
- Navigation im MVP erfolgt über **`nav.yml`**.
- Versionsanzeige im UI erfolgt über **`display_version`**.
- Suche im MVP ist **global**.
- Redirects wie `/<component>/ -> default version` sind **nicht Teil des MVP**.

Zusätzliche projektspezifische Regeln:

- `...`
- `...`

---

## 3. Arbeitsmodus

### 3.1 Verpflichtende Vorgehensweise

Für jede Phase und jeden größeren Arbeitsschritt gilt:

1. Bestehenden Zustand analysieren.
2. Konkreten Umsetzungsplan für den Schritt formulieren.
3. Änderungen implementieren.
4. Unit-Tests ausführen.
5. Integrationstests ausführen.
6. Relevante E2E-Tests ausführen.
7. Ergebnisse dokumentieren.
8. Offene Punkte und Risiken festhalten.

### 3.2 Dokumentationspflicht

Nach jedem größeren Schritt muss dieses Dokument aktualisiert werden mit:

- erledigten Arbeiten
- betroffenen Dateien/Module
- Teststatus
- offenen Problemen
- Architekturentscheidungen
- Abweichungen von der ursprünglichen Planung

---

## 4. Gesamtplan nach Phasen

## Phase 1 — Modularisierung

### Ziel

- Gradle Multi-Project einführen
- `thoth-core`, `thoth-blog`, `thoth-biblios` anlegen
- vorhandene gemeinsame technische Bausteine extrahieren
- `thoth-blog` weiterhin lauffähig halten
- `thoth-biblios` als minimales, startbares Produkt anlegen

### Geplante Arbeitspakete

- [ ] Projektstruktur analysieren
- [ ] `settings.gradle` und Root-Build anpassen
- [ ] Modul `thoth-core` anlegen
- [ ] Modul `thoth-blog` anlegen
- [ ] Modul `thoth-biblios` anlegen
- [ ] Main-Klassen aufteilen
- [ ] bestehende Build-/Serve-Logik für Blog migrieren
- [ ] gemeinsame Rendering-/Templating-Helfer extrahieren
- [ ] Build erfolgreich machen
- [ ] Regressionen im Blog-Verhalten prüfen

### Definition of Done

Phase 1 ist abgeschlossen, wenn:

- [ ] das Repo ein Gradle-Multi-Project ist
- [ ] `thoth-blog` baut und startet
- [ ] `thoth-biblios` baut und startet
- [ ] `thoth-core` enthält gemeinsame technische Infrastruktur
- [ ] bestehende Blog-Funktionen sind nicht ungewollt regressiert
- [ ] Unit- und Integrationstests für die neue Struktur existieren

### Status

- Status: `not started | in progress | blocked | done`
- Startdatum:
- Enddatum:
- Kurzfazit:

### Durchgeführte Arbeiten

- `...`
- `...`

### Testnachweis

#### Unit-Tests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Testklassen:
  - `...`

#### Integrationstests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Tests:
  - `...`

#### E2E-Tests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Tests:
  - `...`

### Offene Punkte

- `...`

### Risiken / Blocker

- `...`

### Entscheidungen in dieser Phase

- Entscheidung:
- Begründung:
- Auswirkung:

---

## Phase 2 — Biblios-MVP

### Ziel

- YAML-Konfiguration `biblios.yml`
- mehrere Git-Repositories als Quellen
- Branches als Versionen
- Site-Katalog
- `nav.yml`-gestützte Navigation
- HTML-Portal mit globaler Startseite
- Komponenten-Landingpages
- Doku-Switcher
- Versions-Switcher
- globale Suche

### Geplante Arbeitspakete

- [ ] YAML-Modell definieren
- [ ] Konfigurationsparser implementieren
- [ ] Validierung der Konfiguration implementieren
- [ ] Git-Source-Fetcher implementieren
- [ ] Repo-Cache-Konzept umsetzen
- [ ] Branch-Resolution implementieren
- [ ] `display_version` unterstützen
- [ ] Site-Katalog aufbauen
- [ ] `nav.yml` einlesen und Navigation generieren
- [ ] Routing für `/<component>/<version>/...` implementieren
- [ ] Seiten mit AsciidoctorJ rendern
- [ ] globale Startseite generieren
- [ ] Komponenten-Landingpages generieren
- [ ] Doku-Switcher implementieren
- [ ] Versions-Switcher implementieren
- [ ] globale Suche integrieren

### Definition of Done

Phase 2 ist abgeschlossen, wenn:

- [ ] `biblios.yml` gelesen und validiert wird
- [ ] mindestens zwei Beispiel-Repositories verarbeitet werden können
- [ ] mehrere Branches pro Repo als Versionen publiziert werden können
- [ ] `display_version` im UI sichtbar ist
- [ ] `nav.yml` die Navigation steuert
- [ ] globale Startseite vorhanden ist
- [ ] Komponenten-Landingpages vorhanden sind
- [ ] Doku-Switcher funktioniert
- [ ] Versions-Switcher funktioniert
- [ ] globale Suche funktioniert
- [ ] Unit-, Integrations- und E2E-Tests vorhanden und grün sind

### Status

- Status: `not started | in progress | blocked | done`
- Startdatum:
- Enddatum:
- Kurzfazit:

### Durchgeführte Arbeiten

- `...`
- `...`

### Testnachweis

#### Unit-Tests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Testklassen:
  - `...`

#### Integrationstests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Tests:
  - `...`

#### E2E-Tests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Tests:
  - `...`

### Offene Punkte

- `...`

### Risiken / Blocker

- `...`

### Entscheidungen in dieser Phase

- Entscheidung:
- Begründung:
- Auswirkung:

---

## Phase 3 — Robustheit, DX und UX

### Ziel

- Robustere Fehlerbehandlung
- Bessere Build-Meldungen
- Stabilere Watch-/Serve-Logik
- Verbesserte UX für Navigation, Links und Darstellung
- Erweiterte Testabdeckung

### Geplante Arbeitspakete

- [ ] Fehlermeldungen verbessern
- [ ] Validierungsfehler nutzerfreundlich machen
- [ ] Watch-/Serve-Verhalten für Multi-Repo verbessern
- [ ] Rebuild-Strategie optimieren
- [ ] UI verfeinern
- [ ] Edit-/Source-Link vorbereiten oder implementieren
- [ ] Testabdeckung erhöhen
- [ ] README und Entwicklerdokumentation ausbauen

### Definition of Done

Phase 3 ist abgeschlossen, wenn:

- [ ] zentrale Fehlerszenarien verständlich behandelt werden
- [ ] `serve` für den MVP stabil nutzbar ist
- [ ] die wichtigsten Nutzerflüsse per E2E abgedeckt sind
- [ ] Dokumentation für Entwicklung und Nutzung vorhanden ist

### Status

- Status: `not started | in progress | blocked | done`
- Startdatum:
- Enddatum:
- Kurzfazit:

### Durchgeführte Arbeiten

- `...`
- `...`

### Testnachweis

#### Unit-Tests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Testklassen:
  - `...`

#### Integrationstests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Tests:
  - `...`

#### E2E-Tests
- Ausgeführt: `ja/nein`
- Ergebnis:
- Relevante Tests:
  - `...`

### Offene Punkte

- `...`

### Risiken / Blocker

- `...`

### Entscheidungen in dieser Phase

- Entscheidung:
- Begründung:
- Auswirkung:

---

## Phase 4 — Optionale Erweiterungen

### Mögliche Themen

- PDF-Pipeline
- Mehrsprachigkeit
- Tag-basierte Versionen
- besseres Seiten-Mapping zwischen Versionen
- Theming-API
- Komponenten- oder globale Suchmodi konfigurierbar machen

### Status

- Status: `not started | in progress | blocked | done`
- Kurzfazit:

### Kandidatenliste

- [ ] `...`
- [ ] `...`
- [ ] `...`

---

## 5. Detaillierter Arbeitslog

> Hier jeden relevanten Implementierungsschritt chronologisch dokumentieren.

### Eintrag 001

- Datum:
- Phase:
- Ziel des Schritts:
- Umgesetzte Änderungen:
- Betroffene Dateien/Module:
- Tests ausgeführt:
- Ergebnis:
- Offene Punkte:
- Nächster Schritt:

### Eintrag 002

- Datum:
- Phase:
- Ziel des Schritts:
- Umgesetzte Änderungen:
- Betroffene Dateien/Module:
- Tests ausgeführt:
- Ergebnis:
- Offene Punkte:
- Nächster Schritt:

### Eintrag 003

- Datum:
- Phase:
- Ziel des Schritts:
- Umgesetzte Änderungen:
- Betroffene Dateien/Module:
- Tests ausgeführt:
- Ergebnis:
- Offene Punkte:
- Nächster Schritt:

---

## 6. Architekturentscheidungen (ADR-light)

> Jede relevante Architekturentscheidung kurz festhalten.

### ADR-001 — Monorepo mit drei Gradle-Modulen

- Status: `proposed | accepted | superseded`
- Kontext:
- Entscheidung:
- Begründung:
- Konsequenzen:

### ADR-002 — Java 25

- Status: `accepted`
- Kontext:
- Entscheidung: Ziel-Java-Version ist 25.
- Begründung:
- Konsequenzen:

### ADR-003 — Navigation per `nav.yml`

- Status: `accepted`
- Kontext:
- Entscheidung: Der MVP nutzt `nav.yml`.
- Begründung:
- Konsequenzen:

### ADR-004 — `display_version` statt Branchname im UI

- Status: `accepted`
- Kontext:
- Entscheidung: UI zeigt `display_version`.
- Begründung:
- Konsequenzen:

### ADR-005 — globale Suche im MVP

- Status: `accepted`
- Kontext:
- Entscheidung: Der MVP bietet globale Suche.
- Begründung:
- Konsequenzen:

### ADR-006 — Redirects nicht im MVP

- Status: `accepted`
- Kontext:
- Entscheidung: Redirects wie `/<component>/` auf Default-Version werden später behandelt.
- Begründung:
- Konsequenzen:

---

## 7. Teststrategie

## 7.1 Testebenen

### Unit-Tests

Pflicht für:
- Konfigurationsparser
- Routing
- Kataloglogik
- Navigationslogik
- Template-Helfer
- zentrale Core-Utilities

### Integrationstests

Pflicht für:
- Gradle-Multi-Project-Build
- Zusammenspiel von `thoth-core` und `thoth-blog`
- Zusammenspiel von `thoth-core` und `thoth-biblios`
- Git-Quellenauflösung
- AsciidoctorJ-Rendering im Biblios-Flow
- globale Suchindex-Erzeugung

### E2E-Tests

Pflicht für:
- vollständiger Build eines Beispiel-Biblios-Projekts
- Generierung einer HTML-Site
- Vorhandensein der globalen Startseite
- Vorhandensein von Komponenten-Landingpages
- Sichtbarkeit und Funktion von Doku-Switcher und Versions-Switcher
- erwartete Zielseiten unter den erwarteten URLs
- erfolgreicher Start von `serve`

## 7.2 Test-Tasks / Kommandos

Einzutragen bzw. aktuell zu halten:

```bash
./gradlew test
./gradlew integrationTest
./gradlew e2eTest
```

Falls abweichend, hier dokumentieren:

- `...`

## 7.3 Aktueller Teststatus

- Unit-Tests: `not started | partial | green | failing`
- Integrationstests: `not started | partial | green | failing`
- E2E-Tests: `not started | partial | green | failing`

---

## 8. Risiken, Annahmen und offene Fragen

## 8.1 Risiken

- Git-Worktree-/Checkout-Strategie könnte komplex werden.
- Watch-/Serve-Verhalten für Multi-Repo könnte mehr Aufwand verursachen als geplant.
- Navigation und Seitenidentität über Versionen hinweg brauchen klare Konventionen.
- Gefahr von Scope Creep in Richtung vollständige Antora-Nachbildung.

Weitere Risiken:

- `...`
- `...`

## 8.2 Annahmen

- Das bestehende Blog-Produkt kann mit vertretbarem Aufwand modularisiert werden.
- Die bestehende Rendering- und Template-Logik ist teilweise wiederverwendbar.
- JGit ist für den MVP ausreichend.

Weitere Annahmen:

- `...`
- `...`

## 8.3 Offene Fragen

- `...`
- `...`
- `...`

---

## 9. Akzeptanz- und Abnahmestatus

### Gesamtprojekt

- [ ] Gradle-Multi-Project vorhanden
- [ ] `thoth-blog` weiterhin nutzbar
- [ ] `thoth-biblios` MVP vorhanden
- [ ] YAML-Konfiguration implementiert
- [ ] Git-Repositories als Quellen funktionieren
- [ ] Branch-Versionierung funktioniert
- [ ] `display_version` funktioniert
- [ ] `nav.yml` funktioniert
- [ ] globale Suche funktioniert
- [ ] Doku-Switcher funktioniert
- [ ] Versions-Switcher funktioniert
- [ ] Tests auf allen drei Ebenen vorhanden
- [ ] README / Doku aktualisiert

### Freigabe

- Fachlich freigegeben: `ja/nein`
- Technisch freigegeben: `ja/nein`
- Testseitig freigegeben: `ja/nein`
- Datum:
- Bemerkungen:

---

## 10. Abschlussbericht

> Erst am Ende vollständig ausfüllen.

### Zusammenfassung

- Was wurde erreicht?
- Was wurde bewusst nicht umgesetzt?
- Welche technischen Schulden bleiben?
- Welche nächsten sinnvollen Schritte gibt es?

### Wichtigste Ergebnisse

- `...`
- `...`
- `...`

### Bekannte Einschränkungen

- `...`
- `...`

### Empfohlene nächste Schritte

1. `...`
2. `...`
3. `...`

