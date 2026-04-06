# Thoth Architektur-Dokumentation

## Übersicht

Thoth ist eine Produktfamilie von JVM-basierten Static-Site-Generatoren für AsciiDoc-Inhalte. Das Projekt ist als Gradle-Multi-Project aufgebaut und besteht aus drei Modulen:

```
thoth/
├── thoth-core/      Gemeinsame technische Infrastruktur
├── thoth-blog/      Blog-spezifisches Produkt
└── thoth-biblios/   Multi-Repo-Dokumentationsgenerator
```

## Modulstruktur

### thoth-core

**Zweck**: Technische Infrastruktur, die von beiden Produkten genutzt wird.

**Enthält**:
- `DevServer` – HTTP-Server für statische Dateien (serve-Modus)
- `InputWatcher` – Filesystem-Watcher für inkrementelle Rebuilds
- `ServeHandle` – Orchestrierungshilfe für Serve/Watcher-Lifecycle
- `ThothBuildException` – Zentrale Exception-Klasse für strukturierte Fehlerbehandlung

**Abhängigkeiten**:
- Java Standard Library
- Keine produkt-spezifischen Abhängigkeiten

**Nicht enthalten**:
- ❌ Blog-spezifische Logik (Posts, Tags, Feed)
- ❌ Biblios-spezifische Logik (Git, Catalog, Navigation)
- ❌ Template-Konfiguration (produktspezifisch)

**Design-Entscheidungen**:
- Nur Komponenten, die **beide** Produkte benötigen
- Keine Fachlogik, nur technische Infrastruktur
- Watcher und Server sind produktunabhängig nutzbar

### thoth-blog

**Zweck**: Static-Site-Generator für AsciiDoc-Blogs (bestehendes Produkt).

**Enthält**:
- `ThothBlogCli` – CLI Entry Point
- `SiteGenerator` – Build-Orchestrierung
- `PostParser` – Front-Matter-Parsing und AsciiDoc-Rendering
- `Post`, `TagRef`, `TagSlugger` – Blog-Domänenmodelle
- `SiteConfig` – `thoth.properties` Parser
- Blog-Templates (FreeMarker)
- Blog-Site-Assets (CSS, JS, Fonts)

**Abhängigkeiten**:
- `thoth-core` (DevServer, InputWatcher)
- `picocli` (CLI)
- `asciidoctorj` (via thoth-core)
- `freemarker` (via thoth-core)
- `jsoup` (via thoth-core)

**CLI**:
```bash
java -jar thoth-blog-<version>-all.jar build --input <dir> --output <dir>
java -jar thoth-blog-<version>-all.jar serve --input <dir> --output <dir> [--port <port>]
```

**Eingabe**:
- `thoth.properties` im Input-Root
- `.adoc`-Dateien mit Front-Matter (Titel, Autor, Datum, Tags)
- Beliebige Assets (Bilder, CSS, JS)

**Ausgabe**:
- `index.html` (Homepage)
- `archive.html` (Archiv)
- `search.html` (Suche)
- `feed.xml` (RSS)
- `tags/<slug>/index.html` (Tag-Seiten)
- Gerenderte Posts unter `/<jahr>/<monat>/<titel>/`

### thoth-biblios

**Zweck**: Multi-Repo-Dokumentationsgenerator mit Versionsunterstützung.

**Enthält**:
- `ThothBibliosCli` – CLI Entry Point
- `BibliosSiteGenerator` – Site-Erzeugung
- `BibliosConfigParser` – YAML-Parsing für `biblios.yml`
- `CatalogBuilder` – Orchestriert Git-Fetching → Nav-Parsing → Seitenaufbau
- `AsciidoctorRenderer` – AsciiDoc-zu-HTML-Rendering
- `GitSourceResolver` – JGit-basiertes Repository-Management
- `NavParser`, `NavItem`, `NavTree` – Navigation aus `nav.yml`
- Katalog-Modelle: `SiteCatalog`, `DocComponent`, `ComponentVersion`, `DocPage`
- Biblios-Templates (FreeMarker)
- Biblios-Site-Assets (CSS)

**Abhängigkeiten**:
- `thoth-core` (DevServer, InputWatcher, ThothBuildException)
- `picocli` (CLI)
- `snakeyaml-engine` (YAML-Parsing)
- `org.eclipse.jgit` (Git-Operationen)
- `asciidoctorj` (via thoth-core)
- `freemarker` (via thoth-core)

**CLI**:
```bash
java -jar thoth-biblios-<version>-all.jar build --config <biblios.yml> [--output <dir>]
java -jar thoth-biblios-<version>-all.jar serve --config <biblios.yml> [--output <dir>] [--port <port>]
```

**Eingabe**:
- `biblios.yml` im Projekt-Root
- Mehrere Git-Repositories als Content-Quellen
- `nav.yml` pro Doku-Version im Git-Repo

**Ausgabe**:
- `index.html` (globale Startseite)
- `/<component>/index.html` (Komponenten-Landingpages)
- `/<component>/<version>/...` (Dokumentationsseiten)
- `search-index.json` (globaler Suchindex)
- `site-assets/` (CSS, JS)

## Datenflüsse

### thoth-blog Build-Pipeline

```
Input-Root/
├── thoth.properties ──────┐
├── post1.adoc ────────────┤
├── post2.adoc ────────────┤
└── assets/ ───────────────┤
                           ▼
                    ┌──────────────┐
                    │ SiteConfig   │───> Konfiguration
                    └──────────────┘
                           │
                    ┌──────────────┐
                    │ PostParser   │───> Front-Matter + AsciiDoc-Rendering
                    └──────────────┘
                           │
                    ┌──────────────┐
                    │SiteGenerator │───> Templates + Assets → HTML
                    └──────────────┘
                           │
                           ▼
                    Output-Root/
```

### thoth-biblios Build-Pipeline

```
biblios.yml ──────────────────────┐
                                  ▼
                           ┌───────────────┐
                           │ Config-Parser │───> BibliosConfig
                           └───────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
             ┌──────────────┐          ┌──────────────┐
             │GitSourceRes. │          │GitSourceRes. │ (pro Quelle)
             └──────────────┘          └──────────────┘
                    │                           │
                    └─────────────┬─────────────┘
                                  ▼
                           ┌──────────────┐
                           │CatalogBuilder│───> SiteCatalog
                           └──────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
             ┌──────────────┐          ┌──────────────┐
             │ NavParser    │          │Asciidoctor   │
             │ (nav.yml)    │          │ Renderer     │
             └──────────────┘          └──────────────┘
                                  │
                                  ▼
                           ┌──────────────┐
                           │SiteGenerator │───> Templates → HTML-Site
                           └──────────────┘
                                  │
                                  ▼
                           Output-Root/
```

## Design-Entscheidungen

### 1. Multi-Project statt Single-Module

**Entscheidung**: Gradle-Multi-Project mit drei Modulen.

**Begründung**:
- Klare Trennung von Fachlogik (Blog vs. Biblios)
- Wiederverwendung technischer Infrastruktur (Core)
- Unabhängige Weiterentwicklung beider Produkte
- Vermeidet ungesunde Kopplung

### 2. AsciidoctorJ statt Asciidoctor.js

**Entscheidung**: Rendering über JVM-native AsciidoctorJ-Bibliothek.

**Begründung**:
- Keine Node.js-Abhängigkeit
- Bessere JVM-Integration
- Einheitlicher Tech-Stack (nur Java)
- Einfachere Deployment-Pipeline

### 3. nav.yml als MVP-Navigationsstandard

**Entscheidung**: Explizite Navigation über YAML-Datei statt automatischer Ordnerstruktur-Erkennung.

**Begründung**:
- Definierte Reihenfolge der Seiten
- Verschachtelte Strukturen möglich
- Klare Validierungsfehler bei ungültigen Einträgen
- Einfacher zu verstehen und zu warten

### 4. display_version statt Branch-Namen

**Entscheidung**: Menschlich lesbbare Versionsanzeige im UI.

**Begründung**:
- Branch-Namen sind oft technisch (main, v1.x, release/2.0)
- display_version ermöglicht benutzerfreundliche Labels (Aktuell, 1.x Legacy)
- Trennung von interner Version und UI-Anzeige

### 5. Globaler Suchindex im MVP

**Entscheidung**: Eine Suche über alle Dokumentationen und Versionen.

**Begründung**:
- Einfachere Implementierung für MVP
- Benutzer finden Inhalte über alle Docs hinweg
- Später um facettierte Suche erweiterbar

### 6. Lokaler Git-Cache

**Entscheidung**: Repositories unter `.thoth/cache/repos/<source-id>/` cachen.

**Begründung**:
- Vermeidet erneutes Klonen bei jedem Build
- Fetch ist schneller als Clone
- Mehrere Builds mit gleichem Cache möglich
- Watcher kann auf Cache-Verzeichnis hören

## Fehlerbehandlung

### ThothBuildException

Zentrale Exception-Klasse mit:
- `ErrorSeverity`: WARNING, ERROR, FATAL
- `component`: Ursprung des Fehlers (config, navigation, git, rendering)
- Verständliche Fehlermeldungen mit Handlungsempfehlungen

### Validierungsebenen

1. **Config-Validierung** (BibliosConfigParser)
   - YAML-Syntax
   - Erforderliche Felder
   - Datentypen

2. **Git-Validierung** (GitSourceResolver)
   - Repository-Zugriff
   - Branch-Existenz
   - Checkout-Probleme

3. **Navigation-Validierung** (NavParser)
   - YAML-Syntax
   - Erforderliche Felder (title + page/children)
   - Zirkuläre Referenzen

4. **Rendering-Validierung** (AsciidoctorRenderer)
   - Datei-Existenz
   - AsciiDoc-Syntax (teilweise)
   - Fallback auf Rohcontent bei Fehlern

## Teststrategie

### Ebenen

1. **Unit-Tests**
   - Config-Parsing
   - Nav-Parsing
   - Routing
   - Breadcrumbs
   - Asciidoctor-Rendering
   - Branch-Pattern-Matching
   - Front-Matter-Parsing
   - Tag-Slug-Normalisierung
   - Template-Rendering

2. **Integrationstests**
   - Vollständige Build-Pipeline mit lokalen Git-Repos (Biblios)
   - Vollständige Build-Pipeline mit realistischem Input (Blog)
   - Multi-Branch-Verarbeitung
   - Site-Generierung

3. **E2E-Tests** (implementiert)
   - Vollständige Benutzerflüsse mit lokalen Git-Repos
   - Multi-Source, Multi-Version Builds
   - HTML-Output-Verifikation
   - Globaler Suchindex
   - Doku-Switcher und Versions-Switcher
   - DevServer (serve) HTTP-Auslieferung

### Testanzahl

- **thoth-blog:** 77 Tests (unit + integration + E2E)
- **thoth-biblios:** 54 Tests (unit + integration + E2E)
- **thoth-core:** Keine eigenen Tests (reine technische Infrastruktur)

### Ausführung

```bash
# Alle Unit-Tests
./gradlew test

# Integrationstests
./gradlew integrationTest

# E2E-Tests
./gradlew e2eTest

# Alle Tests
./gradlew test integrationTest e2eTest

# Spezifisches Modul
./gradlew :thoth-blog:test
./gradlew :thoth-biblios:test
./gradlew :thoth-biblios:integrationTest
./gradlew :thoth-biblios:e2eTest
```

## Entwicklungsrichtlinien

### Neue Features

1. **Modulzuordnung prüfen**:
   - Technische Infrastruktur → thoth-core
   - Blog-spezifisch → thoth-blog
   - Biblios-spezifisch → thoth-biblios

2. **Tests schreiben**:
   - Mindestens Unit-Tests
   - Integrationstests für kritische Pfade
   - E2E-Tests für Benutzerflüsse (wenn sinnvoll)

3. **Fehlerbehandlung**:
   - ThothBuildException verwenden
   - Verständliche Meldungen
   - Severity und Component setzen

4. **Dokumentation**:
   - README aktualisieren
   - Architektur-Doc erweitern
   - CHANGELOG pflegen

### Code-Stil

- Java 25 Features nutzen
- Records für immutable Datenmodelle
- Sealed Classes wo sinnvoll
- Pattern Matching
- Text Blocks für Templates/Config

## Migration von Legacy-Thoth

### Schritte (bereits durchgeführt)

1. Bestehenden Code nach `thoth-blog` verschoben
2. Package-Namen angepasst (`guru.interlis.thoth` → `guru.interlis.thoth.blog`)
3. Shared Infrastructure nach `thoth-core` extrahiert
4. `thoth-biblios` als neues Modul angelegt
5. Gradle-Multi-Project konfiguriert

### Verhaltensgarantien

- `thoth-blog` verhält sich funktional wie vorher
- Gleiche CLI-Optionen
- Gleiches `thoth.properties`-Format
- Gleiche Output-Struktur

## Zukünftige Erweiterungen

### Geplant (Phase 4+)

- [ ] Inkrementelle Rebuilds für Biblios
- [ ] Tag-basierte Versionen
- [ ] Cross-Version-Page-Mapping (beyond exact source-path match)
- [ ] PDF-Pipeline
- [ ] Mehrsprachige Dokumentationen
- [ ] Branch-Pattern-Unterstützung (z. B. `release/*`)
- [ ] CI/CD-Pipeline

### Implementiert (Phase 1–3)

- [x] Gradle-Multi-Project mit thoth-core, thoth-blog, thoth-biblios
- [x] biblios.yml Parsing und Validierung
- [x] Git-Fetching mit lokalem Cache
- [x] Branch-Versionierung mit display_version
- [x] nav.yml Navigation
- [x] Site-Katalog, Routing, HTML-Rendering
- [x] Globale Startseite, Komponenten-Landingpages
- [x] Doku-Switcher und Versions-Switcher
- [x] Globaler Suchindex
- [x] Build- und Serve-CLI
- [x] Unit-Tests, Integrationstests, E2E-Tests
- [x] Edit-Links / Source-Links (konfigurierbar über URL-Pattern)
- [x] Versions-Switcher mit Page-Mapping (exakter Source-Pfad-Vergleich)

### Nicht-Ziele (aktuell)

- ❌ 1:1 Antora-Kompatibilität
- ❌ Asciidoctor.js-Unterstützung
- ❌ Word/PDF-Ausgabe im MVP
- ❌ CCMS-Integration
- ❌ Redirects von `/<component>/` auf Default-Version
