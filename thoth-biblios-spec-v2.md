# Thoth Biblios – Produktspezifikation

## 1. Zielbild

Es soll aus dem heutigen Projekt eine **Produktfamilie** entstehen:

- **`thoth-blog`**: das bestehende Blog-Produkt, funktional weitgehend unverändert
- **`thoth-biblios`**: ein neues Produkt für **mehrere Dokumentationen aus mehreren Git-Repositories**, mit **versionsbasierter Veröffentlichung über Branches**, **Doku-Switcher**, **Versions-Switcher** und **AsciidoctorJ** als Render-Engine
- **`thoth-core`**: ein gemeinsamer technischer Kern, den beide Produkte nutzen

Wichtig: **Thoth soll nicht verbogen werden**, sondern als Blog-Werkzeug bestehen bleiben. `thoth-biblios` wird als Schwesterprodukt entwickelt.

## 2. Warum diese Aufteilung sinnvoll ist

Die heutige Thoth-Struktur ist fachlich klar blogorientiert: Eingabe ist ein einzelnes Input-Root; Inhalte sind `.adoc`-Posts mit Autor, Datum, Status, Tags, Teaser und Cover; Ausgabe enthält `index.html`, `archive.html`, `search.html`, `feed.xml` und Tag-Seiten. Das passt sehr gut zu einem Blog, aber schlecht zu einem Multi-Repo-Dokumentationssystem mit Versionen und Navigationslogik.

Technisch ist Thoth dagegen bereits eine gute Basis für beide Produkte, weil die zentralen Bausteine bereits da sind: CLI mit Picocli, Rendering mit AsciidoctorJ, Templating mit FreeMarker und HTML-Nachbearbeitung mit jsoup.

## 3. Produktziele für `thoth-biblios`

`thoth-biblios` soll ein JVM-basiertes, Git-zentriertes Dokumentationswerkzeug werden, das sich in wichtigen Punkten **antora-artig verhält**, ohne Antora 1:1 zu kopieren.

### 3.1 Muss-Ziele

`thoth-biblios` muss:

- mehrere Git-Repositories als Content-Quellen unterstützen
- Branches als veröffentlichte Dokumentationsversionen unterstützen
- je Quelle einen konfigurierbaren Startpfad unterstützen
- aus allen Quellen einen **zentralen Site-Katalog** aufbauen
- eine **globale Startseite** erzeugen
- pro Doku eine **Landing Page** erzeugen
- einen **Doku-Switcher** und einen **Versions-Switcher** bereitstellen
- **AsciidoctorJ** nutzen, nicht Asciidoctor.js
- HTML ausgeben
- den bestehenden JVM-Stack so weit wie sinnvoll wiederverwenden
- **Java 25** als Zielplattform nutzen
- `nav.yml` als offiziellen MVP-Navigationsstandard unterstützen
- ein konfigurierbares `display_version`-Feld im UI nutzen statt Branch-Namen direkt anzuzeigen
- im MVP eine **globale Suche** über alle Dokumentationen anbieten

### 3.2 Soll-Ziele

`thoth-biblios` soll:

- gute Defaults haben, aber viele Dinge per YAML konfigurierbar machen
- mit Git-Cache arbeiten, statt bei jedem Build alles neu zu klonen
- stabile URLs erzeugen
- Seitenhierarchie und Navigation explizit definieren können
- später gut um PDF-Pipelines erweitert werden können
- später internationale Dokumentationen unterstützen können

### 3.3 Nicht-Ziele für die erste Version

Version 1 von `thoth-biblios` soll **nicht** sofort alles können:

- keine 1:1-Kompatibilität mit Antora
- keine vollständige Antora-Adressierungssemantik
- keine Tag-basierten Versionen im ersten Schritt
- keine perfekte Cross-Version-Seitenauflösung im ersten Schritt
- kein komplexes Rechte- oder Publishing-Workflow-System
- kein CCMS
- keine Word- oder PDF-Ausgabe im MVP
- keine Asciidoctor.js- oder Node-Abhängigkeit
- **keine Redirects von `/<component>/` auf Default-Version im MVP**

## 4. Bestehender Zustand (`thoth` heute)

### 4.1 Laufzeit- und Build-Grundlage

Das aktuelle Repo ist ein einzelnes Gradle-Java-Anwendungsprojekt mit:

- Java-Toolchain: 25
- Main-Class: `guru.interlis.thoth.ThothCli`
- Picocli
- FreeMarker
- AsciidoctorJ
- jsoup
- eigenem `fatJar`-Task

### 4.2 CLI und Verhalten

Heute gibt es zwei Kommandos:

- `build`
- `serve`

`serve` führt einen Initial-Build aus, startet einen lokalen HTTP-Server, beobachtet das Input-Verzeichnis rekursiv und reagiert inkrementell auf Änderungen an `.adoc`- und Nicht-`.adoc`-Dateien.

### 4.3 Eingabemodell

Das heutige Eingabemodell ist:

- ein einzelnes Input-Root
- `.adoc`-Blogposts
- beliebige Assets
- `thoth.properties`

Posts müssen mit einem Headerblock zwischen zwei `---`-Zeilen beginnen. Daraus werden Titel, Autor, Datum sowie Attribute wie `:thoth-status:`, `:thoth-tags:`, `:thoth-teaser:` und `:thoth-cover-image:` gelesen.

### 4.4 Ausgabemodell

Die aktuelle Ausgabe umfasst:

- Pretty URLs pro Post
- `index.html`
- `archive.html`
- `search.html`
- `feed.xml`
- Tag-Seiten unter `tags/<slug>/index.html`
- kopierte Nicht-`.adoc`-Assets
- gebündelte Assets wie Stylesheets, Theme-JS, Search-JS, Suchindex, Lunr und Prism

### 4.5 Konsequenz für die Weiterentwicklung

Der aktuelle Code ist **technisch gut wiederverwendbar**, aber **fachlich zu eng für Doku-Portale**. Deshalb braucht es eine saubere Trennung zwischen gemeinsamem technischen Kern und produktspezifischer Fachlogik.

## 5. Zielarchitektur

### 5.1 Modulstruktur

Ziel ist ein Monorepo mit drei Gradle-Modulen:

```text
thoth/
  settings.gradle
  build.gradle
  gradle.properties

  thoth-core/
  thoth-blog/
  thoth-biblios/
```

### `thoth-core`
Gemeinsame technische Infrastruktur.

### `thoth-blog`
Das heutige Produkt in modularisierter Form.

### `thoth-biblios`
Das neue Dokumentationsprodukt.

### 5.2 Verantwortung von `thoth-core`

In `thoth-core` gehört nur, was beide Produkte wirklich teilen:

- AsciidoctorJ-Integration
- Template-Engine-Integration mit FreeMarker
- Routing und Pretty-URL-Grundlogik
- Output-Schreiben
- Asset-Kopieren
- HTML-Nachbearbeitung
- allgemeiner Build-Kontext
- Watch-/Serve-Grundlagen
- Logging, Fehlerbehandlung, gemeinsame Utilities

Nicht in `thoth-core` gehören:

- Blog-Tags
- Feed-Erzeugung
- Archive-Seiten
- Git-Quellenverwaltung
- Branch-Versionierung
- Komponenten- und Versionskatalog
- Doku-Switcher-Logik

### 5.3 Verantwortung von `thoth-blog`

`thoth-blog` bleibt das heutige Produkt mit:

- `thoth.properties`
- Blogpost-Modell
- Front-Matter-Parsing
- Tag-Seiten
- Feed
- Archiv
- Suche
- blogbezogenen Templates
- bestehender `build`/`serve`-CLI

Ziel ist **Verhaltensstabilität**: Die Umstellung auf Module soll das bestehende Produkt möglichst nicht verändern.

### 5.4 Verantwortung von `thoth-biblios`

`thoth-biblios` bekommt ein neues Domänenmodell:

- YAML-basierte Site-Konfiguration
- mehrere Content Sources
- Git-Repository-Fetching und lokaler Cache
- Branch-Resolution zu veröffentlichten Versionen
- Komponenten-/Doku-Katalog
- Seitenmodell für Dokumentation statt Blogpost
- explizite Navigation mit `nav.yml`
- Doku-Landingpages
- Doku-Switcher
- Versions-Switcher mit `display_version`
- HTML-Site-Erzeugung
- globale Suche

## 6. Konfigurationsmodell für `thoth-biblios`

### 6.1 Dateiname

Empfohlener Dateiname: `biblios.yml`

Alternative: `site.yml`

Empfehlung: **`biblios.yml`**, um Verwechslungen mit anderen Site-Tools zu vermeiden.

### 6.2 Vorschlag für das Schema

```yaml
site:
  title: Interlis Docs
  url: https://docs.example.org
  default_language: de
  default_component: cadastral
  default_version: latest

output:
  dir: build/site
  clean: true

ui:
  theme: default
  show_version_badge: true
  show_edit_link: true

content:
  sources:
    - id: cadastral
      display_name: Kataster
      url: https://git.example.org/docs/cadastral.git
      branches:
        - name: main
          display_version: Aktuell
        - name: v1.*
          display_version: 1.x
        - name: v2.*
          display_version: 2.x
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml
      start_page: index.adoc

    - id: api
      display_name: API
      url: https://git.example.org/docs/api.git
      branches:
        - name: main
          display_version: Aktuell
        - name: release/*
          display_version: Release
      start_path: handbook
      default_version: main
      navigation:
        file: nav.yml
      start_page: index.adoc
```

### 6.3 Semantik

- `site`: globale Site-Metadaten
- `output`: Ausgabeverzeichnis und Bereinigungsoptionen
- `ui`: UI-Optionen
- `content.sources`: Liste aller Dokumentationsquellen
- `id`: technische Kennung der Doku
- `display_name`: Anzeigename im UI
- `url`: Git-URL
- `branches`: zu publizierende Branches oder Branch-Patterns inklusive `display_version`
- `start_path`: relativer Pfad im Repo, unter dem die Doku liegt
- `default_version`: technische Standardversion dieser Doku
- `navigation.file`: Navigationsquelle, im MVP `nav.yml`
- `start_page`: Startseite relativ zum Doku-Root

## 7. Inhaltsmodell für `thoth-biblios`

### 7.1 Kernobjekte

#### SiteCatalog
Zentrale Sammlung aller Dokumentationen und Versionen.

#### DocComponent
Eine Dokumentation, z. B. „Kataster“ oder „API“.

#### ComponentVersion
Eine veröffentlichte Version einer Dokumentation, typischerweise aus einem Branch abgeleitet.

#### DocPage
Eine einzelne gerenderte Dokumentationsseite.

#### NavigationTree
Hierarchische Navigationsstruktur einer Doku-Version.

### 7.2 Felder

#### DocComponent
- `id`
- `displayName`
- `defaultVersion`
- `versions`

#### ComponentVersion
- `componentId`
- `version`
- `displayVersion`
- `branchName`
- `startPage`
- `pages`
- `navigation`

#### DocPage
- `componentId`
- `version`
- `sourcePath`
- `sourceUri`
- `pageId`
- `title`
- `navTitle`
- `route`
- `html`
- `breadcrumbs`
- `prev`
- `next`

## 8. Navigationsmodell

Für Version 1 soll die Navigation **explizit** sein, nicht automatisch aus der Ordnerstruktur erraten werden.

Offizieller MVP-Standard ist eine Datei **`nav.yml`** pro Doku-Version.

### Anforderungen
- definierte Reihenfolge
- verschachtelte Einträge
- Seitenverweise relativ zum Doku-Root
- Generierung von Sidebar, Breadcrumbs und Prev/Next
- klare Validierungsfehler bei ungültigen oder fehlenden Einträgen

### Beispiel `nav.yml`

```yaml
items:
  - title: Einführung
    page: index.adoc
  - title: Installation
    page: installation.adoc
  - title: Bedienung
    children:
      - title: CLI
        page: cli.adoc
      - title: Konfiguration
        page: config.adoc
```

## 9. URL-Schema

Empfohlenes Standardschema:

```text
/<component>/<version>/...
```

Beispiele:

```text
/kataster/main/
/kataster/v2.1/installation/
/api/main/authentication/
```

### Gründe
- Version ist immer sichtbar
- URLs sind stabil und cache-freundlich
- Switcher-Logik ist einfacher
- Deep Links sind eindeutig

Nicht im MVP:
- kein Redirect von `/<component>/` auf Default-Version
- kein `latest`-Alias ohne explizite Version in der URL

Im MVP soll die Version **immer explizit in der URL** stehen.

## 10. Build-Pipeline von `thoth-biblios`

### 10.1 Schritte

1. `biblios.yml` laden und validieren
2. Content Sources auflösen
3. Repositories lokal klonen oder aktualisieren
4. veröffentlichte Branches bestimmen
5. pro Branch und Quelle Arbeitskontext aufbauen
6. `nav.yml` laden und validieren
7. SiteCatalog erzeugen
8. Seiten mit AsciidoctorJ rendern
9. Template-Layer anwenden
10. globale Startseite und Komponenten-Landingpages erzeugen
11. Assets schreiben
12. globalen Suchindex erzeugen
13. HTML-Site ausgeben

### 10.2 Git-Strategie

- lokaler Cache unter `.thoth/cache` oder ähnlich
- `clone` beim ersten Build
- `fetch` bei weiteren Builds
- Branch-Checkouts in isolierten Arbeitsverzeichnissen oder per JGit-Worktree-Strategie
- robuste Fehlerbehandlung bei fehlenden Branches oder unerreichbaren Repos

## 11. UI-Anforderungen für `thoth-biblios`

### 11.1 Globale Startseite
Die Startseite listet alle Dokumentationen mit Namen, Kurzbeschreibung und Standardversion.

### 11.2 Dokumentationsseite
Jede Seite hat:

- Header
- Doku-Switcher
- Versions-Switcher
- linke Navigation
- Hauptinhalt
- Breadcrumbs
- Prev/Next
- optional Edit-Link
- optional Source-Link

### 11.3 Verhalten des Doku-Switchers
Wechsel auf die Startseite der gewählten Doku in ihrer Standardversion.

### 11.4 Verhalten des Versions-Switchers
Im MVP:
- Wechsel auf die Startseite der Zielversion
- Anzeige erfolgt über `display_version`

Später:
- falls äquivalente Seite existiert, Wechsel auf diese Seite
- sonst Fallback auf Startseite der Version

### 11.5 Suche
Im MVP ist die Suche **global** über alle Dokumentationen und Versionen.

## 12. CLI für `thoth-biblios`

### 12.1 Kommandos

#### `build`
Beispiel:

```bash
java -jar thoth-biblios-<version>-all.jar build \
  --config biblios.yml \
  --output build/site \
  --clean
```

#### `serve`
Beispiel:

```bash
java -jar thoth-biblios-<version>-all.jar serve \
  --config biblios.yml \
  --output build/site \
  --port 8080
```

### 12.2 Verhalten von `serve`
- initialer Build
- lokaler HTTP-Server
- Watch auf lokale Konfigurationsdatei und auf bereits ausgecheckte Arbeitsverzeichnisse
- inkrementeller Rebuild, soweit sinnvoll
- bei Git-Änderungen zunächst konservativ: kompletter Rebuild der betroffenen Komponente

## 13. Nicht-funktionale Anforderungen

### 13.1 Technologievorgaben
- JVM-basiert
- **Java 25**
- AsciidoctorJ, nicht Asciidoctor.js
- Gradle-Multi-Project
- Monorepo als Startpunkt
- möglichst geringe neue Abhängigkeiten
- Git-Unterstützung bevorzugt über JGit

### 13.2 Wartbarkeit
- klar getrennte Modulgrenzen
- kein Leaken der Blogdomäne in Biblios
- kein Leaken der Bibliosdomäne in Blog
- Tests für Parsing, Katalogaufbau, Routing und Rendering
- gute Fehlermeldungen

### 13.3 Rückwärtskompatibilität
`thoth-blog` soll funktional möglichst nah am bestehenden Verhalten bleiben, insbesondere bei:

- CLI-Bedienung
- `thoth.properties`
- Header-Parsing
- Output-Struktur
- Build-/Serve-Grundverhalten

## 14. Implementierungsphasen

### Phase 1: Modularisierung
Ziel:
- `thoth-core`, `thoth-blog`, `thoth-biblios` anlegen
- aktuelles `thoth` in `thoth-blog` überführen
- gemeinsames Rendering/Templating/Build-Grundlagen in `thoth-core` extrahieren
- bestehendes Blog-Verhalten erhalten

Ergebnis:
- zwei lauffähige Anwendungen
- `thoth-biblios` zunächst nur minimales CLI

### Phase 2: Biblios-MVP
Ziel:
- `biblios.yml`
- Git-Fetching
- Branch-Resolution
- `display_version`
- SiteCatalog
- `nav.yml`
- HTML-Rendering
- globale Startseite
- Komponenten-Landingpages
- Doku- und Versions-Switcher
- globale Search-Integration

Ergebnis:
- erste nutzbare Multi-Repo-Doku-Site

### Phase 3: Robustheit und UX
Ziel:
- bessere Fehlermeldungen
- inkrementellere Rebuilds
- Edit-Link/Source-Link
- bessere Switcher-Logik
- UI-Verfeinerung
- Testabdeckung erhöhen

### Phase 4: Erweiterungen
Mögliche spätere Themen:
- PDF-Pipeline
- mehrsprachige Dokumentationen
- Tag-basierte Versionen
- Cross-Version-Page-Mapping
- Theming-API
- Redirects und Aliasse

## 15. Akzeptanzkriterien

Die Arbeit ist erfolgreich, wenn mindestens Folgendes erfüllt ist:

1. Das Repo ist ein Gradle-Multi-Project mit `thoth-core`, `thoth-blog`, `thoth-biblios`.
2. `thoth-blog` baut und verhält sich funktional weitgehend wie das heutige Produkt.
3. `thoth-biblios` kann mindestens zwei Git-Repositories aus einer YAML-Datei lesen.
4. `thoth-biblios` kann pro Repo mehrere Branches als Versionen publizieren.
5. Es entsteht ein HTML-Portal mit globaler Startseite, Komponenten-Landingpages, Doku-Switcher und Versions-Switcher.
6. Rendering erfolgt über AsciidoctorJ.
7. Das URL-Schema enthält Komponente und Version.
8. Die Build- und Serve-CLI sind nutzbar.
9. Es gibt automatisierte Tests für Konfigurationsladen, Git-Quellenauflösung, Routing und Katalogaufbau.
10. Die README-Dokumentation erklärt Architektur, Konfiguration und Nutzung.
11. Navigation wird im MVP über `nav.yml` gesteuert.
12. Der Versions-Switcher zeigt `display_version` statt roher Branch-Namen.
13. Die Suche ist im MVP global.

## 16. Risiken

- Git-Arbeitsbaum-Management kann komplizierter werden als zunächst gedacht.
- Die Watch-/Serve-Logik für Multi-Repo-Builds ist schwerer als im Blog-Fall.
- Navigation und Seitenidentität über Versionen hinweg brauchen klare Konventionen.
- Ein zu früher Versuch, Antora exakt nachzubauen, würde den Scope sprengen.
- Wenn `thoth-core` zu fachlich wird, koppeln sich die Produkte ungesund.

## 17. Getroffene Entscheidungen

Die folgenden Architekturentscheidungen sind getroffen und gelten für die Implementierung:

1. **Java-Version**: Java 25
2. **MVP-Navigation**: `nav.yml`
3. **Versionsanzeige**: `display_version` statt direkter Branch-Namen
4. **Suche im MVP**: global
5. **Redirect von `/<component>/` auf Default-Version**: später, nicht im MVP

## 18. Verbindliche Teststrategie

Testing ist kein optionaler Nachtrag, sondern ein verpflichtender Bestandteil der Implementierung. Die neue Architektur darf nur schrittweise eingeführt werden, wenn auf jeder Ebene ausreichend automatisiert getestet wird.

### 18.1 Testebenen

Es müssen **drei Testebenen** eingeführt und dauerhaft gepflegt werden:

#### Unit-Tests
Unit-Tests müssen mindestens folgende Bereiche abdecken:

- Laden und Validieren von `biblios.yml`
- Parsen von `nav.yml`
- Branch- und Versionsauflösung
- Routing und URL-Erzeugung
- Site-Katalog-Aufbau
- Navigation, Breadcrumbs und Prev/Next-Logik
- Template-Helfer und zentrale Formatter
- AsciidoctorJ-Adapter auf isolierter Ebene
- zentrale Utilities in `thoth-core`

#### Integrationstests
Integrationstests müssen mindestens folgende Integrationen absichern:

- vollständiger Gradle-Multi-Project-Build
- korrekte Zusammenarbeit von `thoth-core` und `thoth-blog`
- korrekte Zusammenarbeit von `thoth-core` und `thoth-biblios`
- YAML-Konfiguration + Git-Quellenauflösung + Katalogaufbau
- Git-/JGit-basierte Verarbeitung mehrerer Repositories
- Erkennung mehrerer Branches als veröffentlichte Versionen
- Rendering mit AsciidoctorJ in Kombination mit Routing, Templates und Output-Schreiben
- Erzeugung des globalen Suchindex
- `serve`-Pfad mit lokalem Build-Kontext, soweit automatisiert testbar

#### End-to-End-Tests
E2E-Tests müssen mindestens folgende Benutzerflüsse absichern:

- kompletter Build einer Beispiel-`biblios.yml`
- Build einer Site aus mindestens zwei Beispiel-Repositories
- Erzeugung einer globalen Startseite
- Erzeugung von Komponenten-/Dokumentations-Landingpages
- Erzeugung von Versionsseiten unter dem erwarteten URL-Schema
- Sichtbarkeit und Befüllung von Doku-Switcher und Versions-Switcher
- Vorhandensein zentraler Zielseiten an den erwarteten Ausgabepfaden
- erfolgreicher Start von `serve` und Auslieferung der generierten Seiten
- Weiterfunktionieren von `thoth-blog` nach der Modularisierung

### 18.2 Testpflicht pro Implementierungsphase

Nach jedem größeren Implementierungsschritt müssen Tests aktiv ergänzt, ausgeführt und ausgewertet werden.

Pflicht je größerem Schritt:

- relevante Unit-Tests ergänzen oder aktualisieren
- relevante Integrationstests ergänzen oder aktualisieren
- relevante E2E-Tests ergänzen oder aktualisieren
- Build grün bekommen, bevor der nächste größere Schritt begonnen wird
- kurz dokumentieren, welche Tests hinzugefügt oder angepasst wurden

### 18.3 Testanforderungen an neue Module

Für alle neu eingeführten Kernmodule in `thoth-core` und `thoth-biblios` gilt:

- kein neues Kernmodul ohne automatisierte Tests
- keine kritische Logik ausschließlich manuell prüfen
- Konfigurations-, Routing-, Katalog- und Git-Logik immer automatisiert absichern
- Fehlerfälle und Randfälle ausdrücklich mittesten

### 18.4 CI-Fähigkeit

Die Test-Suite muss so aufgebaut werden, dass sie in CI zuverlässig und reproduzierbar ausgeführt werden kann.

Empfohlene Aufgabenstruktur:

- `./gradlew test`
- `./gradlew integrationTest`
- eigener Task für E2E-Tests, falls sinnvoll, z. B. `./gradlew e2eTest`

Wenn kein separater Task eingeführt wird, muss trotzdem klar dokumentiert und technisch sichergestellt werden, wie E2E-Tests automatisiert ausgeführt werden.

### 18.5 Akzeptanzkriterium für Testing

Die Implementierung ist nicht vollständig, wenn nur Unit-Tests vorhanden sind. Erfolgreich ist sie erst, wenn für die kritischen Benutzerflüsse von `thoth-blog` und `thoth-biblios` sowohl Unit-Tests als auch Integrationstests und mindestens eine belastbare E2E-Teststrecke vorhanden sind.
