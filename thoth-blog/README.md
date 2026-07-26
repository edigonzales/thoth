# Thoth Blog

Static site generator for AsciiDoc blogs.

## Overview

**Thoth Blog** turns a directory of `.adoc` files with front matter into a fully-featured blog website with tags, RSS feed, archive, search, and a watch-based dev server.

Plain text. Real websites.

## Quick Start

> [!WARNING]
> Breaking change: old input layouts are no longer supported. `thoth-blog` now requires `blog/` in the input root.

### Prerequisites

- Java 17 or later

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
  - Changed `blog/**/*.adoc`: re-renders that post + regenerates aggregate pages
  - Changed `blog/**` non-`.adoc`: copies only that file
  - Changed `templates/**`: re-renders all pages
  - Changed `assets/**`: updates only `output/assets/**`
  - Deleted `blog/**/*.adoc`: removes generated post + regenerates aggregate pages

## Configuration: `thoth.properties`

Required keys:
| Key | Description |
|-----|-------------|
| `site.title` | Blog title (homepage + feed) |
| `site.description` | Feed description |
| `site.baseUrl` | Absolute base URL for feed links |
| `site.language` | Feed language (e.g. `en-gb`) |
| `site.dateFormat` | Date format in HTML pages (e.g. `dd.MM.yyyy`) |

Optional keys:
| Key | Description |
|-----|-------------|
| `dev.port` | Default `serve` port |
| `site.indexThumbnails.enabled` | Enable thumbnail generation on index/archive pages. Default: `false` |

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

## Output Structure

- Per post: `path/to/post/index.html` (pretty URLs)
- `index.html` – Homepage
- `archive.html` – Archive
- `search.html` – Search page
- `feed.xml` – RSS 2.0 feed
- `tags/<tag-slug>/index.html` – Tag pages
- `assets/` – Bundled assets (CSS, JS, search, syntax highlighting)

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
# If Java 17 or later is not active, use SDKMAN:
sdk use java 17.0.12-tem
```

### Build Failures

```bash
./gradlew clean build
./gradlew :thoth-blog:test --info
```
