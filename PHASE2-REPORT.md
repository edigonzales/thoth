# Phase 2 Completion Report – Biblios MVP Core

## Zusammenfassung

Phase 2 (Biblios-MVP-Kern) wurde erfolgreich abgeschlossen. `thoth-biblios` kann jetzt:
- `biblios.yml` laden und validieren
- `nav.yml` laden und verarbeiten
- Git-Quellen via JGit klonen/fetchen und Branches auschecken
- `display_version` korrekt unterstützen
- Site-Catalog aufbauen
- URLs im Schema `/<component>/<version>/...` erzeugen
- HTML-Site mit globaler Startseite, Komponenten-Landingpages und Dokumentationsseiten erzeugen
- Globalen Suchindex erzeugen
- Doku-Switcher und Versions-Switcher in Templates vorbereiten
- `build` und `serve` CLI-Kommandos vollständig implementieren

## Implementierte Module

### Konfiguration (`guru.interlis.thoth.biblios.config`)

| Klasse | Zweck |
|--------|-------|
| `BibliosConfig` | Root-Konfigurationsobjekt |
| `BibliosConfigParser` | YAML-Parser mit SnakeYAML Engine |
| `SiteSection` | Site-Metadaten (Title, URL, Language) |
| `OutputSection` | Ausgabekonfiguration (dir, clean) |
| `UiSection` | UI-Optionen (theme, badges, links) |
| `ContentSection` | Content-Quellen-Liste |
| `SourceConfig` | Einzelne Git-Quelle (id, url, branches, start_path, etc.) |
| `BranchConfig` | Branch-Konfiguration (name, display_version) |
| `NavigationConfig` | Navigationsdatei-Referenz |

### Navigation (`guru.interlis.thoth.biblios.nav`)

| Klasse | Zweck |
|--------|-------|
| `NavItem` | Einzelner Navigationseintrag (mit optionalen Children) |
| `NavTree` | Navigationsbaum mit Such-, Breadcrumb- und Prev/Next-Logik |
| `NavParser` | YAML-Parser für nav.yml |

### Git (`guru.interlis.thoth.biblios.git`)

| Klasse | Zweck |
|--------|-------|
| `GitSourceResolver` | JGit-basiertes Repository-Management (clone, fetch, checkout) |

### Katalog (`guru.interlis.thoth.biblios.catalog`)

| Klasse | Zweck |
|--------|-------|
| `SiteCatalog` | Zentrale Sammlung aller Komponenten und Versionen |
| `DocComponent` | Dokumentationseinheit (z.B. "Kataster", "API") |
| `ComponentVersion` | Spezifische Version einer Dokumentation |
| `DocPage` | Gerenderte Dokumentationsseite mit Route, Breadcrumbs, Prev/Next |
| `CatalogBuilder` | Orchestriert Git-Fetching, Nav-Parsing und Seitenaufbau |

### Site-Generator (`guru.interlis.thoth.biblios`)

| Klasse | Zweck |
|--------|-------|
| `BibliosSiteGenerator` | Erzeugt HTML-Site aus Katalog (FreeMarker, Asset-Kopie, Suchindex) |
| `ThothBibliosCli` | CLI mit `build` und `serve` Kommandos |

### Templates

| Template | Zweck |
|----------|-------|
| `layout.ftl` | Basis-Layout mit Header, Sidebar, Breadcrumbs, Prev/Next |
| `index.ftl` | Globale Startseite (listet alle Dokumentationen) |
| `component.ftl` | Komponenten-Landingpage |
| `page.ftl` | Dokumentations-Inhaltsseite |
| `sidebar-nav.ftl` | Rekursive Seitennavigation |
| `styles.css` | Basis-CSS für das Biblios-UI |

## Tests

### Unit-Tests (42 Tests insgesamt)

**thoth-biblios:**
- `BibliosConfigParserTest` – Parsing, Validierung, Defaults (7 Tests)
- `BranchPatternTest` – Branch-Pattern-Matching, display_version (5 Tests)
- `NavParserTest` – Nav-Parsing, Breadcrumbs, Prev/Next (7 Tests)
- `RoutingTest` – URL-Erzeugung, Katalog-Suche (4 Tests)
- `BreadcrumbTest` – Breadcrumb-Generierung (5 Tests)

**thoth-blog:**
- `PostParserTest` – Front-Matter-Parsing (5 Tests)
- `TagSluggerTest` – Slug-Normalisierung (2 Tests)
- `SiteGeneratorIntegrationTest` – Vollständiger Blog-Build (7 Tests)

### Integrationstests

- `BibliosIntegrationTest` – Vollständiger Build-Pipeline-Test mit lokalem Git-Repo (2 Tests)
  - Single-Build mit Navigation und Suchindex
  - Multi-Branch-Build mit zwei Versionen

### Testausführung

```bash
# Alle Unit-Tests
./gradlew test

# Alle Tests (Unit + Integration)
./gradlew build
```

### Testergebnisse

- **thoth-blog:** Alle Tests grün ✅
- **thoth-biblios:** Alle Tests grün ✅
- **thoth-core:** Keine eigenen Tests (nur technische Infrastruktur)
- **Gesamt-Build:** Erfolgreich ✅

## Deliverables

- ✅ Beispiel-`biblios.yml` (`src/test/resources/test-biblios.yml`)
- ✅ Beispiel-`nav.yml` (`src/test/resources/test-nav.yml`)
- ✅ Voll funktionsfähige `build`-Pipeline
- ✅ Voll funktionsfähige `serve`-Pipeline (mit DevServer aus thoth-core)
- ✅ Globale Startseite erzeugt
- ✅ Komponenten-Landingpages erzeugt
- ✅ Dokumentationsseiten unter korrekten URLs
- ✅ Doku-Switcher und Versions-Switcher in Templates
- ✅ Navigation aus nav.yml
- ✅ Breadcrumbs auf jeder Seite
- ✅ Prev/Next-Navigation
- ✅ Globaler Suchindex (`search-index.json`)

## Definition of Done – Checkliste

- [x] Beispiel-`biblios.yml` vorhanden
- [x] Beispiel-`nav.yml` vorhanden
- [x] Git-Fetching funktioniert
- [x] Branch-Resolution getestet
- [x] `display_version` getestet
- [x] Routing getestet
- [x] Globale Startseite erzeugt
- [x] Komponenten-Landingpages erzeugt
- [x] Globale Suche erzeugt
- [x] Alle Tests grün

## Architekturentscheidungen

1. **SnakeYAML Engine statt SnakeYAML:** Verwendet `org.snakeyaml:snakeyaml-engine:2.9` für sichereres Parsing ohne Reflection.
2. **Lokaler Cache unter `.thoth/cache/repos/<source-id>/`:** Ermöglicht wiederverwendbare Git-Caches über Builds hinweg.
3. **FreeMarker Macros statt Includes für Layout:** Bessere Parameterisierung und Wiederverwendbarkeit.
4. **Suchindex als JSON-Array:** Einfach zu konsumieren für clientseitige JavaScript-Suche.
5. **Content-Seiten mit `index.html` unter Routen-Pfad:** Pretty URLs wie `/<component>/<version>/guide/` durch `guide/index.html`.
6. **MVP: Raw-Content als `<pre><code>`:** Vollständiges AsciidoctorJ-Rendering wird in Phase 3 nachgereicht.

## Bekannte Einschränkungen

- **AsciidoctorJ-Rendering:** Aktuell wird der AsciiDoc-Rohcontent als `<pre><code>` ausgegeben. Vollständiges HTML-Rendering mit AsciidoctorJ ist für Phase 3 geplant.
- **Watch/Incremental bei Serve:** `serve` macht initialen Build und startet Server. Inkrementelle Updates bei Git-Änderungen sind noch nicht implementiert.
- **PDF/Export:** Nicht im MVP enthalten.

## Nächster Schritt

**Phase 3 – UI, Navigation und Nutzerfluss:**

1. Doku-Switcher und Versions-Switcher vollständig implementieren (Routing zwischen Docs)
2. AsciidoctorJ-Rendering für Content-Seiten integrieren
3. Breadcrumbs visuell verfeinern
4. Prev/Next-Navigation vollständig implementieren
5. Layout und CSS verfeinern
6. Serve-Workflow mit inkrementellen Rebuilds verbessern
7. E2E-Tests für zentrale Benutzerflüsse
