# Phase 1 Completion Report – Modularisierung

## Zusammenfassung

Phase 1 (Analyse und Modularisierung) wurde erfolgreich abgeschlossen. Das Projekt wurde von einem Single-Module-Gradle-Build zu einem Multi-Project mit drei Modulen umgebaut.

## Änderungen

### 1. Gradle-Multi-Project-Struktur eingeführt

- **Root `settings.gradle`**: Module `thoth-core`, `thoth-blog`, `thoth-biblios` aufgenommen
- **Root `build.gradle`**: Gemeinsame Konfiguration für Subprojects (Java 25, JUnit, Encoding)
- **Modulspezifische `build.gradle`-Dateien** für jedes Modul angelegt

### 2. thoth-core erstellt

**Zweck**: Gemeinsame technische Infrastruktur

**Enthält**:
- `DevServer` – HTTP-Static-File-Server
- `InputWatcher` – Rekursiver Filesystem-Watcher

**Package**: `guru.interlis.thoth.core`

**Begründung**: Diese Komponenten sind produktagnostisch und werden sowohl von Blog als auch Biblios benötigt.

### 3. thoth-blog erstellt

**Zweck**: Bestehendes Blog-Produkt (funktional unverändert)

**Enthält**:
- `ThothBlogCli` – CLI Entry Point (umbenannt von `ThothCli`)
- `SiteGenerator` – Core Generation Engine
- `PostParser` – AsciiDoc Front Matter Parsing
- `Post`, `TagRef`, `TagSlugger` – Blog-Domänenmodelle
- `SiteConfig` – Blog-Konfigurationsloader
- `TemplateService` – FreeMarker-Rendering
- Templates und Site-Assets

**Package**: `guru.interlis.thoth.blog`

**Abhängigkeiten**: 
- `project(':thoth-core')` für DevServer/InputWatcher
- `picocli` für CLI

### 4. thoth-biblios erstellt

**Zweck**: Neues Produkt für Multi-Repo-Dokumentation (MVP in Entwicklung)

**Enthält** (aktuell):
- `ThothBibliosCli` – Minimaler CLI-Stub mit `build` und `serve` Commands
- Akzeptiert `--config`, `--output`, `--clean`, `--port` Parameter

**Package**: `guru.interlis.thoth.biblios`

**Abhängigkeiten**:
- `project(':thoth-core')` für technische Infrastruktur
- `picocli` für CLI

### 5. Alte Struktur bereinigt

- Root `src/`-Verzeichnis entfernt
- Alle Sources in die entsprechenden Module verschoben

### 6. README aktualisiert

- Neue Produktfamilie dokumentiert
- Modulstruktur und -verantwortlichkeiten erklärt
- Build- und Testanweisungen aktualisiert

## Architekturentscheidungen

1. **DevServer und InputWatcher in thoth-core**: Beide Komponenten sind produktunabhängig und werden von Blog und Biblios gleichermaßen benötigt.

2. **TemplateService bleibt in thoth-blog**: Die Template-Konfiguration ist aktuell blog-spezifisch (ClassTemplateLoader für blog-spezifische Templates). Biblios wird eine eigene Template-Konfiguration benötigen.

3. **SiteConfig bleibt in thoth-blog**: Lädt `thoth.properties` – das ist ein blog-spezifisches Konzept. Biblios wird `biblios.yml` verwenden.

4. **Keine fachliche Logik in thoth-core**: Nur technische Infrastruktur wurde extrahiert. Blog-Domänenlogik (Posts, Tags, Feed) bleibt vollständig in thoth-blog.

## Tests

### Ausgeführte Tests

- `./gradlew clean build` – **ERFOLGREICH** (22 Tasks, alle grün)
- `./gradlew :thoth-blog:test` – **ERFOLGREICH** (alle bestehenden Tests bestanden)
- `./gradlew :thoth-blog:run --args="--help"` – **ERFOLGREICH** (CLI startet korrekt)
- `./gradlew :thoth-biblios:run --args="--help"` – **ERFOLGREICH** (CLI-Stub startet korrekt)

### Getroffene Testannahmen

- Bestehende Blog-Tests (`PostParserTest`, `SiteGeneratorIntegrationTest`, `TagSluggerTest`) laufen unverändert
- Keine neuen Tests für thoth-core (DevServer/InputWatcher haben keine eigenen Unit-Tests im ursprünglichen Code)
- Keine neuen Tests für thoth-biblios (MVP-Stub hat noch keine eigene Logik)

## Build-Artefakte

Folgende JARs werden erzeugt:

```
thoth-core/build/libs/thoth-core-0.0.1.jar
thoth-blog/build/libs/thoth-blog-0.0.1.jar
thoth-blog/build/libs/thoth-blog-0.0.1-all.jar (fat JAR, executable)
thoth-biblios/build/libs/thoth-biblios-0.0.1.jar
thoth-biblios/build/libs/thoth-biblios-0.0.1-all.jar (fat JAR, executable)
```

## Definition of Done – Checkliste

- [x] Spezifikation gelesen und abgeglichen
- [x] Modulstruktur angelegt
- [x] Build erfolgreich (`./gradlew clean build`)
- [x] `thoth-blog` startet (`--help` funktioniert)
- [x] `thoth-biblios` startet (`--help` funktioniert)
- [x] Tests laufen grün
- [x] README aktualisiert

## Nächster Schritt

**Phase 2 – Biblios-MVP-Kern**:

1. YAML-Config-Lader für `biblios.yml` implementieren
2. Git-Source-Fetching mit JGit aufbauen
3. Branch-Resolution und `display_version` unterstützen
4. Site-Catalog und Routing implementieren
5. `nav.yml` Parser erstellen
6. HTML-Templates für Biblios anlegen
7. Globale Suchindex-Erzeugung implementieren

## Bekannte Einschränkungen

- **Java-Version im Runtime-Environment**: Das Ausführen der fat JARs direkt (`java -jar`) kann auf Systemen mit älterer Java-Version (< 25) fehlschen. Über Gradle (`./gradlew :module:run`) funktioniert es stets korrekt.
- **thoth-core Tests**: DevServer und InputWatcher haben currently keine eigenen Unit-Tests. Dies sollte in Phase 2 nachgeholt werden.
- **TemplateService**: Ist aktuell noch blog-spezifisch implementiert. Für Biblios wird eine flexiblere Template-Konfiguration benötigt.
