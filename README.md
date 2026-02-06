# Thoth
Plain text. Real websites.

Thoth is a Java-based static site generator for AsciiDoc blogs.
It builds pretty URLs, tag pages, RSS, local assets, Lunr search, and a watch-based dev server.

## Requirements
- Java: 25 (toolchain configured in Gradle)
- Gradle: wrapper included (`./gradlew`)

## Build
```bash
./gradlew test
./gradlew build
```

The executable fat JAR is generated as:
- `build/libs/thoth-<version>-all.jar` (for example `build/libs/thoth-0.0.1-all.jar`)

Run it with:
```bash
java -jar build/libs/thoth-<version>-all.jar --help
```

## CLI
```bash
java -jar build/libs/thoth-<version>-all.jar <command> [options]
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
- `assets/fonts/Inter/Inter-Regular.woff2`
- `assets/fonts/Inter/Inter-SemiBold.woff2`

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

All are packaged into `thoth-<version>-all.jar` by the `fatJar` task.
