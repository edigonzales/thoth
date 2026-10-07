# Thoth Blog

Static site generator for AsciiDoc blogs.

## Overview

**Thoth Blog** turns a directory of `.adoc` files with front matter into a fully-featured blog website with tags, RSS feed, archive, search, and a watch-based dev server.

Plain text. Real websites.

Commands below assume the repository root as the working directory. Replace
`<version>` in JAR filenames with the version produced by your Gradle build.

## Quick Start

> [!WARNING]
> Breaking change: old input layouts are no longer supported. `thoth-blog` now requires `blog/` in the input root.

### Prerequisites

- Java 17 or later to run the generator
- Java 17 JDK toolchain for this module
- Full repository builds also require a Java 25 JDK toolchain for the Biblios server

### Build

```bash
./gradlew :thoth-blog:build
```

The executable fat JAR is generated as:
```
thoth-blog/build/libs/thoth-blog-<version>-all.jar
```

### Build a Blog

```bash
java -jar thoth-blog/build/libs/thoth-blog-<version>-all.jar build \
  --input /path/to/input \
  --output /path/to/output \
  --clean
```

### Development Server

```bash
java -jar thoth-blog/build/libs/thoth-blog-<version>-all.jar serve \
  --input /path/to/input \
  --output /path/to/output \
  --port 8080
```

The `serve` command:
- Performs an initial build
- Starts a local HTTP server
- Watches input directory recursively
- Incrementally rebuilds on changes:
  - Changed `blog/**/*.adoc`: re-renders the changed post and its translation siblings, then refreshes aggregate pages
  - Changed `blog/**` non-`.adoc`: copies only that file
  - Changed `templates/**`: re-renders all pages
  - Changed `assets/**`: updates only `output/assets/**`
  - Deleted `blog/**/*.adoc`: removes generated post and refreshes translation links and aggregate pages
  - Changed `thoth.properties`: reloads content and rebuilds with the new language configuration

## Configuration: `thoth.properties`

Required keys:
| Key | Description |
|-----|-------------|
| `site.title` | Blog title (homepage + feed) |
| `site.description` | Feed description |
| `site.baseUrl` | Absolute base URL for feed links |
| `site.language` | Default content and feed language (e.g. `de`, `en-gb`) |
| `site.dateFormat` | Date format in HTML pages (e.g. `dd.MM.yyyy`) |

Optional keys:
| Key | Description |
|-----|-------------|
| `dev.port` | Default `serve` port |
| `site.indexThumbnails.enabled` | Enable thumbnail generation on index/archive pages. Default: `false` |
| `site.languages` | Comma-separated language tags, including `site.language`, e.g. `de,en`. Enables language directories. Omit for the legacy single-language layout. |
| `site.title.<language>` | Optional translated site title, e.g. `site.title.en`. Falls back to `site.title`. |
| `site.description.<language>` | Optional translated description, e.g. `site.description.en`. Falls back to `site.description`. |

Example:
```properties
site.title=Thoth Blog
site.description=My notes and projects
site.baseUrl=https://example.com
site.language=en-gb
site.dateFormat=yyyy-MM-dd
dev.port=8080
site.indexThumbnails.enabled=false
```

## Input Structure (Required)

`--input` must follow this structure:

```
input/
  thoth.properties
  blog/                 # required: .adoc + content-adjacent files
    2026/
      hello.adoc
      images/
        cover.png
  templates/            # optional: FreeMarker overrides
    index.ftl
  assets/               # optional: theme overrides (CSS/JS/images/fonts)
    styles-light.css
    home-hero.jpg
```

Behavior:
- Public URLs are derived from paths inside `blog/`, but without the `blog/` prefix.
- Example: `blog/2026/hello.adoc` -> `/2026/hello/`.
- `templates/` overrides bundled templates selectively (missing files fall back to defaults).
- `assets/` overrides bundled files in `output/assets/` selectively.

## Ignored Files/Directories

`thoth-blog` copies all non-`.adoc` assets from `input/blog/` (and override assets from `input/assets/`) except ignored paths.

Always ignored:
- `.DS_Store`
- `.thothignore`
- `thoth.properties`
- Directories named `.git`, `.hg`, `.svn`, `.idea`, `.vscode`, `node_modules`, `build`, `target`, `.gradle` (and everything inside them)

Optional project-specific ignores can be configured via `.thothignore` in the input root:

```gitignore
# Comments and empty lines are ignored
blog/tmp/**
blog/**/*.map
assets/private/**
blog/cache/
```

Rules:
- `.thothignore` is only loaded from the `--input` root.
- Patterns are glob-style and matched against paths relative to `--input`.
- Patterns starting with `/` are resolved from the input root (example: `/assets/private/**`).
- Patterns ending with `/` are treated as recursive directory patterns (`cache/` behaves like `cache/**`).
- Invalid patterns are skipped with a warning; the build continues.

## Post Front Matter

Each `.adoc` post must begin with a header block between the first and second `---` lines:

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

Only posts with `thoth-status: published` are generated. Missing status defaults to
`published`; every other status, including `draft`, is excluded from HTML, lists,
language switching, search and RSS. This also applies to single-language blogs.
Switching a previously published post to `draft` removes its generated page.

## Multilingual Blogs

Configure the default language and the languages to publish:

```properties
site.language=de
site.languages=de,en
site.title.en=My Blog
site.description.en=Notes and projects
```

Keep one AsciiDoc file per language:

```text
input/blog/
  de/2026/mein-beitrag.adoc
  en/2026/my-post.adoc
  de/2026/images/cover.png
  en/2026/images/cover.png
```

Both posts identify the same logical content using a front-matter attribute:

```asciidoc
:thoth-id: my-shared-post-id
```

When omitted, the relative path inside the language directory, without `.adoc`,
is the content ID. Thus `de/2026/hello.adoc` and `en/2026/hello.adoc` are linked
automatically. An explicit shared ID is required when translated filenames differ.
Titles, teasers, tags and status are independent for each translation.

The default language keeps unprefixed URLs:

| Source | Public URL |
|--------|------------|
| `blog/de/2026/mein-beitrag.adoc` | `/2026/mein-beitrag/` |
| `blog/en/2026/my-post.adoc` | `/en/2026/my-post/` |
| `blog/2026/legacy.adoc` | `/2026/legacy/` (default language) |

Existing unprefixed source files may coexist with language directories during
migration. Moving a default-language post into its language directory preserves
its URL and RSS GUID. Duplicate IDs in one language and output path collisions
fail the build with the conflicting source paths. Language tags are normalized
using BCP 47 conventions, for example `en-gb` becomes `en-GB` in language URLs.

Each language has its own homepage, archive, tag pages, search page, search index
and feed. The default artifacts retain their existing paths; English artifacts
are prefixed with `/en/`, including `/en/feed.xml` and
`/en/assets/search-index.json`. Shared CSS, JavaScript and fonts stay under `/assets/`.
Relative content links and assets follow the source language; root-relative URLs
remain explicit. Standard UI labels are available in German and English; other
configured languages currently use English UI labels.

The language selector opens the published translation with the same content ID.
If none exists, its option explicitly leads to the target language's homepage.
Search-page language switching preserves the query. Direct URLs determine the
language; there are no automatic browser-language redirects.

Lists, archives, tag pages and search prefer the selected language, falling back
to the published default-language original with a language notice. Links point to
the actual original URL. Feeds contain only published posts in the feed's language,
without fallback entries. Published translations have reciprocal `hreflang` links
and self-referencing canonical URLs.

Non-clean builds persist generated page ownership in `output/.thoth-blog-pages`
to remove obsolete pages, tag pages and search indexes across rebuilds. On the
first upgrade from an older Thoth version, use `--clean` to remove legacy outputs
that have not been recorded in this manifest, including obsolete tag pages.

`thoth-core` owns reusable language-tag and URL helpers. Biblios language variants
are a subsequent expansion; its generator, publication format and access policies
are not changed by this feature. The intended extension keeps the existing Source
ID and access policy across all versions, languages and downloads.

## Output Structure

- Per post: `path/to/post/index.html` (pretty URLs)
- `index.html` – Homepage
- `archive.html` – Archive
- `search.html` – Search page
- `feed.xml` – RSS 2.0 feed
- `tags/<tag-slug>/index.html` – Tag pages
- `assets/` – Bundled assets (CSS, JS, search, syntax highlighting)
- `<language>/…` – Corresponding artifacts for non-default languages when enabled

## Testing

```bash
# Unit tests
./gradlew :thoth-blog:test

# Integration tests (full build pipeline)
./gradlew :thoth-blog:integrationTest

# E2E tests (build + serve + HTTP)
./gradlew :thoth-blog:e2eTest

# All tests
./gradlew :thoth-blog:test :thoth-blog:integrationTest :thoth-blog:e2eTest

# JaCoCo coverage report
./gradlew :thoth-blog:jacocoTestReport
```

HTML coverage reports are generated at:
```
thoth-blog/build/reports/jacoco/test/html/index.html
```

## CLI Reference

```bash
java -jar thoth-blog-<version>-all.jar <command> [options]
```

### `build`

| Option | Required | Description |
|--------|----------|-------------|
| `--input` | Yes | Input root with `thoth.properties` and required `blog/` directory |
| `--output` | Yes | Output directory for generated HTML |
| `--clean` | No | Delete output directory before building |

### `serve`

| Option | Required | Description |
|--------|----------|-------------|
| `--input` | Yes | Input root with `thoth.properties` and required `blog/` directory |
| `--output` | Yes | Output directory |
| `--port` | No | HTTP server port (default from `thoth.properties` or `8080`) |

## Features

- Multilingual posts, translation switching, per-language search and feeds

- Pretty URLs (`/2026/01/hello/`)
- Tag pages with slug normalization (including umlauts)
- RSS 2.0 feed with Atom self-link
- Client-side Lunr search
- Dark mode toggle with `prefers-color-scheme` support
- Prism.js syntax highlighting with line numbers support, including Groovy and Gradle
- Responsive layout with sticky navbar

## Project Structure

```
thoth-blog/
├── src/main/java/.../blog/
│   ├── ThothBlogCli.java       CLI entry point
│   ├── SiteGenerator.java      Build orchestration
│   ├── PostParser.java         Front matter + AsciiDoc parsing
│   ├── Post.java               Blog post model
│   ├── SiteConfig.java         thoth.properties parser
│   └── TagSlugger.java         Tag slug normalization
├── src/main/resources/
│   ├── templates/              FreeMarker templates
│   └── site-assets/            CSS, JS, fonts
└── build.gradle
```

## Troubleshooting

### Java Version

Thoth Blog requires Java 17 or later. If you see `UnsupportedClassVersionError`:

```bash
java -version
# Inspect installed JDK toolchains for source builds:
./gradlew -q javaToolchains
```

### Build Failures

```bash
./gradlew clean build
./gradlew :thoth-blog:test --info
```
