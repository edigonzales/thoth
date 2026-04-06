# Thoth

Thoth is a family of JVM-based static site generators for AsciiDoc content.

## Product Family

- **`thoth-blog`** – Static site generator for AsciiDoc blogs (the original product)
- **`thoth-biblios`** – Multi-repo documentation site generator with versioning (new, in development)
- **`thoth-core`** – Shared technical infrastructure (AsciidoctorJ, FreeMarker, DevServer, FileWatcher)

## thoth-blog

Plain text. Real websites.

Thoth Blog builds pretty URLs, tag pages, RSS, local assets, Lunr search, and a watch-based dev server.

### Build
```bash
./gradlew test
./gradlew build
```

The executable fat JAR is generated as:
- `thoth-blog/build/libs/thoth-blog-<version>-all.jar`

Run it with:
```bash
java -jar thoth-blog/build/libs/thoth-blog-<version>-all.jar --help
```

### CLI
```bash
java -jar thoth-blog/build/libs/thoth-blog-<version>-all.jar <command> [options]
```

Commands:
1. `build`
2. `serve`

### `build`
```bash
java -jar build/libs/thoth-<version>-all.jar build \
  --input /path/to/input \
  --output /path/to/output \
  --clean
```

Options:
- `--input <dir>`: input root
- `--output <dir>`: output root
- `--clean`: delete output before generating

### `serve`
```bash
java -jar build/libs/thoth-<version>-all.jar serve \
  --input /path/to/input \
  --output /path/to/output \
  --port 8080
```

Options:
- `--input <dir>`: input root
- `--output <dir>`: output root
- `--port <port>`: dev server port (default from `thoth.properties` or `8080`)

`serve` behavior:
- performs an initial build
- serves output via a local HTTP server
- watches input recursively
- incremental changes:
  - changed `.adoc`: re-render only that post + regenerate aggregate pages
  - changed non-`.adoc`: copy only that file
  - deleted `.adoc`: remove generated post + regenerate aggregate pages

## Input Structure
Input root contains:
- `.adoc` blog posts
- arbitrary assets (images, CSS, JS, fonts, etc.)
- `thoth.properties`

Example:
```text
input/
  thoth.properties
  blog/
    2026/
      hello.adoc
      image.png
      custom.js
```

## AsciiDoc Header Block (Front Matter)
Each post must begin with a header block between the first and second `---` lines.

```adoc
---
= My Post Title
Author Name
2026-01-12
:thoth-status: published
:thoth-tags: Java,AI,Thoth
:thoth-teaser: Optional teaser override
:thoth-cover-image: Optional cover override
---
AsciiDoc body starts here.
```

Parsing rules implemented:
1. Header block is text between first and second `---`
2. Title is line 1 (`= ...`)
3. Author is line 2
4. Date is line 3 (`YYYY-MM-DD`)
5. Supported attributes:
   - `:thoth-status:` `published|draft` (available in model)
   - `:thoth-tags:` comma-separated, trimmed, empty entries removed
   - `:thoth-teaser:` optional homepage teaser override
   - `:thoth-cover-image:` optional homepage cover override

Timezone used for publication/feed logic:
- `Europe/Zurich`

## Configuration (`thoth.properties`)
Required keys:
1. `site.title`: blog title (homepage + feed)
2. `site.description`: feed description
3. `site.baseUrl`: absolute base URL (used for feed links)
4. `site.language`: feed language (for example `en-gb`)
5. `site.dateFormat`: date format used in HTML pages (for example `dd.MM.yyyy`)

Optional keys:
1. `dev.port`: default `serve` port

Example:
```properties
site.title=Thoth Blog
site.description=My notes and projects
site.baseUrl=https://example.com
site.language=en-gb
site.dateFormat=yyyy-MM-dd
dev.port=8080
```

## Output Structure
Generated output includes:
- per post: `path/to/post/index.html`
- `index.html`
- `archive.html`
- `search.html`
- `feed.xml`
- tag pages: `tags/<tag-slug>/index.html`

All non-`.adoc` files from input are copied 1:1 recursively to output.

## Assets
Thoth writes bundled assets to `assets/`:
- `assets/styles-light.css`
- `assets/styles-dark.css`
- `assets/theme.js`
- `assets/search.js`
- `assets/search-index.json`
- `assets/lunr.min.js`
- `assets/prism/prism.css`
- `assets/prism/prism.js`
- `assets/prism/components/*.js` (markup, clike, javascript, css, ini, java, typescript, json, bash, sql, python, yaml, kotlin, go, c, cpp)
- `assets/prism/plugins/line-highlight/*`
- `assets/prism/plugins/line-numbers/*`

Enable line numbers per code block with:
```adoc
[source,ini,linenums]
----
[ch.ehi.ili2db]
defaultSrsCode=2056
----
```

### Navbar and Theming
Every page uses the same sticky navbar:
- left: Home, Archive, Subscribe
- right: search field (`#search-input`) + dark mode toggle (`#theme-toggle`)

Theme behavior (`theme.js`):
1. respects `prefers-color-scheme` by default
2. allows manual toggle
3. persists choice in `localStorage`

## Search (Lunr)
- Build generates `assets/search-index.json`
- Each document contains:
  - `title`
  - `date`
  - `tags`
  - `url`
  - `body` (plain text)
  - `teaser`
- `search.html?q=...` performs client-side search via `lunr.min.js`

To customize search UI, edit:
- `src/main/resources/site-assets/search.js`
- `src/main/resources/templates/search.ftl`

## RSS Feed
`feed.xml` is generated as RSS 2.0 with Atom self-link.

Characteristics:
1. items sorted by date descending
2. item `link` points to pretty URL
3. `guid` is relative path with `isPermaLink="false"`
4. description stored in CDATA

## Tag Pages and Slugs
For each tag, Thoth generates:
- `tags/<slug>/index.html`

Slug normalization:
1. lower-case
2. spaces/commas => `-`
3. remove special characters
4. normalize umlauts (`ä->ae`, `ö->oe`, `ü->ue`, `ß->ss`)

## Templates and Layout
FreeMarker templates are packaged in:
- `src/main/resources/templates`

Main templates:
- `layout.ftl`
- `post.ftl`
- `index.ftl`
- `archive.ftl`
- `tag.ftl`
- `search.ftl`
- `feed.ftl`

## Tests
Implemented tests cover:
1. header parsing (title, author, date, tags, overrides)
2. tag slugging
3. pretty output paths (`post/index.html`)
4. generation of homepage/archive/tag/feed/search index
5. asset copy for non-`.adoc` files

Run:
```bash
./gradlew test
```

## Notes on Dependencies
Build dependencies are resolved from Maven Central via Gradle.
The generator uses:
- FreeMarker
- AsciidoctorJ
- Lunr (client-side)
- Prism.js (client-side syntax highlighting)

All are packaged into `thoth-blog-<version>-all.jar` by the `fatJar` task.

## Project Structure

This is a Gradle multi-project with three modules:

```
thoth/
├── thoth-core/          Shared technical infrastructure (AsciidoctorJ, FreeMarker, DevServer, FileWatcher)
├── thoth-blog/          Blog-specific product (posts, tags, RSS, archive, search)
└── thoth-biblios/       Multi-repo documentation generator (in development)
```

### Module Responsibilities

**thoth-core** contains only technical infrastructure shared by both products:
- AsciidoctorJ integration
- FreeMarker template engine setup
- Dev server (HTTP static file server)
- File system watcher
- Common utilities

**thoth-blog** contains all blog-specific logic:
- Post parsing with front matter
- Tag pages, RSS feed, archive
- Blog-specific templates and assets
- Blog-specific CLI (`build` / `serve`)

**thoth-biblios** (in development) will contain:
- YAML configuration loading (`biblios.yml`)
- Git repository fetching and branch resolution
- Multi-version documentation catalog
- Documentation-specific templates
- Doku- and version switchers

## thoth-biblios (MVP Complete)

Multi-repo documentation site generator with versioning support.

### Build
```bash
./gradlew :thoth-biblios:build
```

The executable fat JAR is generated as:
- `thoth-biblios/build/libs/thoth-biblios-<version>-all.jar`

Run it with:
```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar --help
```

### CLI
```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar <command> [options]
```

Commands:
1. `build` – Builds the documentation site from `biblios.yml`
2. `serve` – Runs dev server for documentation site

### Configuration

`thoth-biblios` uses a YAML configuration file (`biblios.yml`) that defines:
- Multiple Git repositories as content sources
- Branches per repository as published versions
- `display_version` for human-readable version labels
- `nav.yml` for navigation structure

### MVP Features
- ✅ YAML configuration loading (`biblios.yml`)
- ✅ Git repository fetching via JGit (with local cache)
- ✅ Branch resolution as documentation versions
- ✅ `display_version` support in UI
- ✅ `nav.yml` navigation parsing
- ✅ Site catalog building
- ✅ Routing with `/<component>/<version>/...` URL schema
- ✅ Global start page listing all documentations
- ✅ Component landing pages
- ✅ Documentation content pages with breadcrumbs and prev/next
- ✅ Doku-Switcher and Versions-Switcher in UI
- ✅ Global search index (`search-index.json`)
- ✅ `build` and `serve` CLI commands

### Example biblios.yml
```yaml
site:
  title: My Docs Portal
  url: https://docs.example.org

output:
  dir: build/site
  clean: true

content:
  sources:
    - id: mydocs
      display_name: My Documentation
      url: https://github.com/example/docs.git
      branches:
        - name: main
          display_version: Latest
        - name: v1.x
          display_version: Version 1.x
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml
```

### Example nav.yml
```yaml
items:
  - title: Introduction
    page: index.adoc
  - title: User Guide
    children:
      - title: Installation
        page: installation.adoc
      - title: Configuration
        page: config.adoc
```

## Running Tests

### Unit Tests (all modules)
```bash
./gradlew test
```

### Module-specific Tests
```bash
./gradlew :thoth-blog:test        # Blog tests
./gradlew :thoth-biblios:test     # Biblios tests (unit + integration)
./gradlew :thoth-core:test        # Core tests (none yet)
```

### Test Coverage
- **thoth-blog:** 14 tests (PostParser, TagSlugger, SiteGenerator integration)
- **thoth-biblios:** 30 tests (Config parsing, Navigation, Routing, Breadcrumbs, Integration)
- **thoth-core:** Technical infrastructure only (DevServer, InputWatcher)

## Architecture Decisions

- **Java 25** as target platform
- **AsciidoctorJ** for rendering (not Asciidoctor.js)
- **nav.yml** as MVP navigation standard
- **display_version** for human-readable version labels in UI
- **Global search** across all documentation in MVP
- **No redirects** from `/<component>/` to default version in MVP

For detailed specifications, see `thoth-biblios-spec-v2.md`.
For architecture details, see `ARCHITECTURE.md`.

## Troubleshooting

### Java Version Issues

If you see `UnsupportedClassVersionError`, ensure you're using Java 25:

```bash
# Check Java version
java -version

# Use SDKMAN to switch to Java 25
sdk use java 25.0.1-tem
```

### Build Failures

```bash
# Clean and rebuild
./gradlew clean build

# Check specific module
./gradlew :thoth-biblios:test --info
```

### Git Repository Errors

- Check that repository URLs are correct and accessible
- For local repos, use `file:///path/to/repo` format
- Clear cache if needed: `rm -rf .thoth/cache`

### Navigation Issues

- Ensure `nav.yml` is in the correct `start_path`
- Check YAML syntax
- Every nav item must have either `page` or `children`

### Asciidoctor Rendering Problems

- Check AsciiDoc syntax
- Ensure `.adoc` files are in the configured `start_path`
- Check logs for `[warn]` messages during build
