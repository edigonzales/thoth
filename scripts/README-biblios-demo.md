# Biblios Demo Setup (2 lokale Git-Repos)

Dieses README beschreibt ein lokales Demo-Setup fuer `thoth-biblios` mit:
- zwei lokalen Git-Repositories (`docs-a`, `docs-b`)
- zwei Versionen pro Repo (`main`, `v1.x`)
- einer `biblios.yml`, die beide Repos als `file://`-Sources einbindet
- Such-relevanten Inhalten fuer `search-index.json`

Das Setup wird durch folgendes Skript erstellt:
- `/Users/stefan/sources/thoth/scripts/create_biblios_demo.py`

## Voraussetzungen

- `git` in `PATH`
- Python 3
- Java 25+
- Projekt liegt unter `/Users/stefan/sources/thoth`

## 1) Demo-Setup erstellen

Im Projekt-Root ausfuehren:

```bash
python3 /Users/stefan/sources/thoth/scripts/create_biblios_demo.py --force
```

Optional anderes Zielverzeichnis:

```bash
python3 /Users/stefan/sources/thoth/scripts/create_biblios_demo.py \
  --base-dir /Users/stefan/sources/thoth/.demo/biblios-custom \
  --force
```

Standard-Ausgabeordner des Skripts:
- `/Users/stefan/sources/thoth/.demo/biblios`

## 2) Repos/Branches pruefen

```bash
git -C /Users/stefan/sources/thoth/.demo/biblios/repos/docs-a branch --list
git -C /Users/stefan/sources/thoth/.demo/biblios/repos/docs-b branch --list
```

Erwartet: `main` und `v1.x` in beiden Repos.

## 3) Biblios bauen

Zuerst Jar bauen:

```bash
cd /Users/stefan/sources/thoth
./gradlew :thoth-biblios:build
```

Dann Site generieren:

```bash
java -jar /Users/stefan/sources/thoth/thoth-biblios/build/libs/thoth-biblios-0.0.1-all.jar build \
  --config /Users/stefan/sources/thoth/.demo/biblios/biblios.yml
```

## 4) Suche pruefen (`search-index.json`)

Nach dem Build sollte existieren:
- `/Users/stefan/sources/thoth/.demo/biblios/site/search-index.json`

Schnellcheck auf Such-Tokens aus beiden Repos/Versionen:

```bash
rg "TOKEN_DOCS_A_MAIN|TOKEN_DOCS_A_V1X|TOKEN_DOCS_B_MAIN|TOKEN_DOCS_B_V1X" \
  /Users/stefan/sources/thoth/.demo/biblios/site/search-index.json
```

## 5) Biblios im Server-Modus starten

Exakter Startbefehl:

```bash
java -jar /Users/stefan/sources/thoth/thoth-biblios/build/libs/thoth-biblios-0.0.1-all.jar serve \
  --config /Users/stefan/sources/thoth/.demo/biblios/biblios.yml \
  --port 8080
```

## 6) Test-URLs

- Home: [http://localhost:8080/](http://localhost:8080/)
- Search-Index: [http://localhost:8080/search-index.json](http://localhost:8080/search-index.json)
- Docs A main: [http://localhost:8080/docs-a/main/](http://localhost:8080/docs-a/main/)
- Docs B v1.x: [http://localhost:8080/docs-b/v1.x/](http://localhost:8080/docs-b/v1.x/)
