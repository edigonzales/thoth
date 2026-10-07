# Thoth Architektur-Dokumentation

## Übersicht und Modulgrenzen

Thoth umfasst zwei JVM-basierte Generatoren für AsciiDoc und ein authentifiziertes
Dokumentationsportal. Das Gradle-Multi-Project besteht aus fünf Modulen:

| Modul | Verantwortung | Projektabhängigkeiten |
|-------|---------------|-----------------------|
| `thoth-core` | `DevServer`, `InputWatcher`, `ServeHandle`, `ThothBuildException`, INTERLIS-Lab-Unterstützung und gemeinsame Sprach-/URL-Helfer | Keine |
| `thoth-blog` | Blog-CLI, Posts, Übersetzungen, Tags, RSS, Suche, Templates und Assets | `thoth-core` |
| `thoth-biblios-core` | YAML-Konfiguration, Katalog- und Navigationsmodelle, Zugriffsregeln, Publikationsverträge, View-Modelle und gemeinsame FreeMarker-Templates | `thoth-core` |
| `thoth-biblios` | Biblios-CLI, Git-Auflösung, Katalogaufbau, AsciiDoc-Rendering, HTML/PDF/DOCX und Publikationspakete | `thoth-core`, `thoth-biblios-core` |
| `thoth-biblios-server` | OIDC, Sessions, Paketvalidierung, Zugriffskontrolle und benutzerbezogene Darstellung | `thoth-biblios-core` |

Generatoren und gemeinsame Module verwenden Java 17 als Toolchain und Zielversion.
Der Spring-Boot-Server verwendet Java 25; native Server-Builds benötigen GraalVM 25.
Ein vollständiger Repository-Build benötigt daher beide Java-Toolchains. Der Gradle
Wrapper ist der Einstiegspunkt für Builds und Tests.

Die Generatoren deklarieren AsciidoctorJ, FreeMarker und jsoup direkt. Die ebenfalls
in `thoth-core` deklarierten Bibliotheken sind `implementation`-Abhängigkeiten,
keine exportierte `api`. Biblios verwendet zusätzlich JGit, SnakeYAML Engine,
Asciidoctor PDF und docx4j. Der Server schließt Asciidoctor/JRuby, JGit und docx4j
aus seinem Klassenpfad aus: Er rendert weder AsciiDoc noch PDF/DOCX und greift nicht
auf Content-Repositories zu.

Die produktiven Serve-Einstiegspunkte nutzen die gemeinsame Infrastruktur. Im
Blog-Package liegen zusätzlich ältere `DevServer`-/`InputWatcher`-Klassen; ihre
Existenz bedeutet nicht, dass die CLI diese statt `thoth-core` verwendet.

## Blog: Eingabe und Build

`ThothBlogCli` liest `thoth.properties` im Input-Root. Der erforderliche Unterordner
`blog/` enthält Posts und begleitende Dateien; `templates/` und `assets/` im
Input-Root überschreiben die gebündelten Theme-Dateien selektiv.

`SiteConfig`, `PostParser` und `PostCatalog` bereiten Konfiguration, Front Matter,
gerenderte Inhalte und Übersetzungsbeziehungen auf. `SiteGenerator` und
`TemplateService` erzeugen einzelne Posts sowie Homepage, Archiv, Tags, Suche und
RSS. Nur veröffentlichte Posts werden ausgeliefert; fehlender Status entspricht
`published`.

URLs folgen den Pfaden innerhalb von `blog/`: `blog/2026/hello.adoc` wird zu
`/2026/hello/`. Es gibt keine obligatorische Jahres-/Monatsstruktur. Mit
`site.languages` werden Übersetzungen über `thoth-id` beziehungsweise ihren
sprachrelativen Pfad verknüpft. Die Standardsprache behält unpräfixierte URLs;
weitere Sprachen erhalten eigene Verzeichnisse, Suchindizes und Feeds.

Der Watch-Modus aktualisiert betroffene Posts und Übersetzungen sowie aggregierte
Seiten. Änderungen an Templates oder der Konfiguration benötigen umfassendere
Rebuilds. Details zu Fallback-Sprache, Asset-Kopien, Ignorierregeln und der
Ausgabedatei `.thoth-blog-pages` stehen in [thoth-blog/README.md](thoth-blog/README.md).

## Biblios: Konfiguration und Katalog

`BibliosConfigParser` in `thoth-biblios-core` liest `biblios.yml`. `CatalogBuilder`
im Generator löst Quellen und Versionen auf und erzeugt `SiteCatalog`,
`DocComponent`, `ComponentVersion` und `DocPage`.

1. `GitSourceResolver` klont Quellen in `.thoth/cache/repos/<source-id>/` und
   aktualisiert bestehende Repositories per Fetch. Der Cache-Root ist über die
   JVM-Property `thoth.work.dir` überschreibbar.
2. Konfigurierte Branches werden nacheinander im gecachten Repository ausgecheckt.
   Es entstehen keine isolierten Git-Worktrees je Version. Branch-Namen werden
   exakt ausgewertet; Branch-Patterns und Tag-Versionen werden nicht unterstützt.
3. Mit `--use-local-working-tree` verwendet der Build für den aktuell ausgecheckten,
   konfigurierten Branch einer lokalen Quelle direkt deren Arbeitsverzeichnis,
   einschließlich uncommitteter Dateien. Andere Branches nutzen weiterhin den Cache.
4. Navigation und Rendering bestimmen die Seiten des Katalogs. Anschließend erzeugt
   `BibliosSiteGenerator` die angeforderten Ausgaben.

`output.dir` wird relativ zur Konfigurationsdatei aufgelöst; ein CLI-`--output`
ist relativ zum Arbeitsverzeichnis. `start_path` ist optional und entspricht
standardmäßig dem Repository-Root. Ohne `default_version` wird die erste verfügbare
Version gewählt. Fehlt eine ausdrücklich konfigurierte Default-Version, wird mit
Warnung ebenfalls auf die erste verfügbare Version zurückgefallen.

### Seiten und Navigation

- **`split`:** Enthält `nav.yml` Seitenreferenzen, bestimmen diese die gebauten
  Seiten. Eine vorhandene `start_page` wird zusätzlich aufgenommen und zur
  Versions-Root-Route. Fehlt eine nutzbare Navigation oder enthält sie keine
  Seitenreferenzen, werden `.adoc`-Dateien rekursiv und sortiert entdeckt.
- **Sidebar:** Verwendet den Navigationsbaum. Automatische Dateisuche erzeugt
  keinen neuen Nav-Baum; ohne Navigation bleibt die Sidebar leer. Prev/Next kann
  auf die Reihenfolge der entdeckten Seiten zurückfallen.
- **`single_page`:** Der erforderliche `master_file` wird einmal gerendert;
  Sidebar-Einträge entstehen aus dessen Überschriften. `nav.yml` und `start_page`
  wählen hier nicht das gerenderte Dokument.

HTML verwendet AsciidoctorJ. Der Generator erzeugt Homepage, Komponenten-Landingpages,
Versionsseiten, Suche und Assets. Komponenten-Landingpages sind keine Redirects zur
Default-Version. Der Versionswechsel kann zur Startseite oder zur gleichen Quellseite
in der Zielversion führen. Suche unterstützt globale Suche und die Einschränkung auf
eine Komponente/Version; sie indexiert auch Kapitelanker.

Die gemeinsamen Templates liegen unter `thoth-biblios-core/src/main/resources/templates/`.
Konfigurationsrelative `templates/` und `assets/` ermöglichen Anpassungen des
statischen HTML-Outputs. Der Server verwendet die gemeinsamen gebündelten Templates;
Template-Overrides werden nicht als ausführbare Templates ins Publikationspaket kopiert.

### PDF, DOCX und Entwicklungsserver

PDF und DOCX sind implementiert. Ihre Erzeugung benötigt sowohl die entsprechende
Konfigurationsfreigabe als auch das CLI-Format. DOCX benötigt zusätzlich explizite
`--docx-version`-Filter. Exporte verwenden einen eigenen Master, den Single-Page-Master
oder einen generierten Sammelmaster für Split-Seiten. Letzterer verwendet `doctype: book`.

`serve` ist HTML-only. Es überwacht Konfiguration, Theme-Overrides und mit
`--use-local-working-tree` die lokalen Content-Quellen. Inhaltsänderungen bauen die
betroffene Quelle neu; Konfigurations- und Theme-Änderungen führen zu vollständigen
Rebuilds. Der Cache selbst wird nicht überwacht. Fetch läuft nur beim initialen
Serve-Build; Remote-Änderungen erfordern einen Neustart. Geschützte Quellen werden
vom statischen Entwicklungsserver nicht ausgeliefert.

## Publikationspaket und Zugriffsschutz

Eine Quelle kann mit `access_policy` eine Richtlinie aus `access.yml` referenzieren.
Schutzeinheit ist die gesamte Dokumentation einschließlich aller Versionen, Seiten
und Dateien. Die Regeln unterstützen `public`, `authenticated` und `restricted`
sowie Freigaben für Gruppen oder Benutzer über Provider und stabile IDs.

Es gibt drei getrennte Build-Wege:

| Aufruf | Ergebnis |
|--------|----------|
| `build` | Statische Ausgabe; bricht bei nicht öffentlichen Quellen ab |
| `build --public-export` | Statische Ausgabe ausschließlich öffentlicher Quellen |
| `build --package <dir>` | Privates Portalpaket mit allen Quellen, ohne zusätzliche statische Site |

Im Paketmodus erzeugt der bestehende Generator HTML und optionale PDF/DOCX-Dateien
in einem frischen temporären Verzeichnis. Dieses wird auch bei Fehlern bereinigt.
Das konfigurierte Site-Ausgabeverzeichnis bleibt unberührt. `--package` ist nicht
mit `--output` oder `--public-export` kombinierbar und benötigt HTML im Formatset.
Paket- und Site-Verzeichnis dürfen sich nicht überlappen. Das Paket wird bei jedem
Build neu erstellt; `--clean` ist dafür nicht nötig. Bei statischen Builds setzt
`--clean` die YAML-Einstellung `output.clean` für diesen Aufruf auf `true`.

`PublicationPackageWriter` erzeugt:

| Bestandteil | Inhalt |
|-------------|--------|
| `manifest.json` | Verbindliche Zuordnung von Pfaden zu Dokumentationen bzw. gemeinsamen oder internen Ressourcen |
| `catalog.json` | Katalog, Richtlinienreferenzen und Daten für die Darstellung |
| `pages/` | Vorgerenderte HTML-Fragmente ohne Portalrahmen |
| `files/` | Auslieferbare Assets, Bilder, Anhänge und optionale PDF/DOCX-Dateien |
| `search-index.json`, `search-index.js` | Interne Suchdaten, nicht direkt auszuliefern |

Der Server liest das Paket beim Start; neue Inhalte benötigen einen neuen Build und
Server-Neustart. Er rendert Rahmen, Navigation, Dokumentations-/Versionsauswahl und
Suche aus der für den Besucher erlaubten Teilmenge. Dateien werden ausschließlich
über das Manifest und nach Zugriffskontrolle ausgeliefert, auch bei HEAD-, Range-
und bedingten Requests. Unbekannte und unberechtigte Dokumentationsziele liefern
jeweils eine generische 404-Antwort.

OIDC-Login erfolgt ausdrücklich über einen Anmeldelink. Ein validiertes lokales
Rücksprungziel wird beim Login gespeichert und anschließend erneut auf Berechtigung
geprüft. Die ausgewählte Registrierung und der exakte Issuer müssen stimmen.
Identitäten werden über Provider plus Subject verglichen (für Entra typischerweise
`oid`), nie über E-Mail. Nach `biblios.max-identity-age` ist eine neue Anmeldung
nötig. Logout verwendet POST mit CSRF-Token.

`access.yml` ist für den Server auch bei öffentlichen Portalen erforderlich.
Jede Zugriffsentscheidung liest den Dateiinhalt; Änderungen gelten unabhängig von
Timestamp und Dateigröße. Fehlende, unlesbare oder ungültige Regeln sperren alle
Dokumentationen bis zur Wiederherstellung einer vollständig gültigen Datei. Es gibt
keinen Rückfall auf frühere Freigaben. Gemeinsame Theme-Assets bleiben verfügbar.
Beim Start führen ungültige Regeln oder unbekannte Richtlinien zum Abbruch.

Details: [Server-Betrieb](thoth-biblios-server/README.md) und
[lokales Keycloak](dev/keycloak/README.md).

## Fehlerbehandlung und Tests

`ThothBuildException` transportiert Severity (`WARNING`, `ERROR`, `FATAL`),
Komponente und Fehlermeldung. Konfiguration, Git-Zugriff, Navigation, Rendering
und Paketvalidierung prüfen ihre jeweiligen Eingaben. Ein fehlender Branch wird
übersprungen; ohne verfügbare Version schlägt die Quelle fehl. Fehlerhafte Navigation
kann zur Dateisuche führen. Split-Rendering kann auf escaped Rohtext zurückfallen;
dies ist keine allgemeine Erfolgsgarantie für Single-Page- oder Artefakt-Rendering.

Alle Module besitzen Gradle-Tasks für Unit-, Integrations- und E2E-Tests; nicht jedes
Modul befüllt alle Source Sets. Core testet unter anderem HTTP-Auslieferung und
Sprachhelfer, Biblios-Core Konfiguration, Navigation und Zugriffsauswertung.
Generator-Integrationstests verwenden lokale Git-Repositories; Server-Tests prüfen
Paketvalidierung, Zugriffsschutz, OIDC und Regeländerungen. Die Keycloak-E2E-Tests
benötigen Docker und werden ohne verfügbares Docker übersprungen.

```bash
./gradlew test integrationTest e2eTest
./gradlew :thoth-blog:jacocoTestReport :thoth-biblios:jacocoTestReport
```

`build` führt nicht automatisch alle Integrations- und E2E-Tests aus. Die CI auf
`main` führt die drei Testkategorien aus, veröffentlicht die Generator-Fat-JARs bei
Snapshot-Versionen und baut/prüft einen nativen Linux-Server.

## Weiterentwicklung und Grenzen

Gemeinsame technische Helfer gehören in `thoth-core`, gemeinsam genutzte
Biblios-Verträge in `thoth-biblios-core`, Rendering/Git in den Generator und
Authentifizierung/HTTP-Zugriffsschutz in den Server. Änderungen an gemeinsamen
Templates und Publikationsverträgen müssen beide Biblios-Ausgabewege berücksichtigen.

Aktuelle Grenzen sind Branch-Versionen ohne Patterns oder Tags, keine
Biblios-Sprachvarianten pro Komponente, keine kapitel- oder versionsspezifischen
Rechte und kein Hintergrund-Refresh von OIDC-Gruppen. HTML-Anpassungen erfolgen
über Overrides des gebündelten Themes. Weitere Details und Beispiele stehen in
[der Biblios-Referenz](thoth-biblios/README.md).
