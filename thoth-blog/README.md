# Thoth Blog

Static site generator for AsciiDoc blogs.

## Overview

**Thoth Blog** turns a directory of `.adoc` files with front matter into a fully-featured blog website with tags, RSS feed, archive, search, and a watch-based dev server.

Plain text. Real websites.

## Quick Start

### Prerequisites

- Java 25 or later

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
  - Changed `.adoc`: re-renders that post + regenerates aggregate pages
  - Changed non-`.adoc`: copies only that file
  - Deleted `.adoc`: removes generated post + regenerates aggregate pages

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

Example:
```properties
site.title=Thoth Blog
site.description=My notes and projects
site.baseUrl=https://example.com
site.language=en-gb
site.dateFormat=yyyy-MM-dd
dev.port=8080
```

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
| `--input` | Yes | Input directory with `.adoc` files and `thoth.properties` |
| `--output` | Yes | Output directory for generated HTML |
| `--clean` | No | Delete output directory before building |

### `serve`

| Option | Required | Description |
|--------|----------|-------------|
| `--input` | Yes | Input directory |
| `--output` | Yes | Output directory |
| `--port` | No | HTTP server port (default from `thoth.properties` or `8080`) |

## Features

- Pretty URLs (`/2026/01/hello/`)
- Tag pages with slug normalization (including umlauts)
- RSS 2.0 feed with Atom self-link
- Client-side Lunr search
- Dark mode toggle with `prefers-color-scheme` support
- Prism.js syntax highlighting with line numbers support
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

Thoth Blog requires Java 25. If you see `UnsupportedClassVersionError`:

```bash
java -version
# If not Java 25, use SDKMAN:
sdk use java 25.0.1-tem
```

### Build Failures

```bash
./gradlew clean build
./gradlew :thoth-blog:test --info
```
