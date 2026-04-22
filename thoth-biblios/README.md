# Thoth Biblios

Multi-repo documentation site generator with versioning support.

## Overview

**Thoth Biblios** builds a complete documentation portal from multiple Git repositories. Each repository can publish multiple versions (based on Git branches), and Biblios combines them into a unified, searchable site with navigation, breadcrumbs, and version switching.

Think of it as a lightweight, JVM-native alternative to Antora: you point it at Git repos, define your navigation in `nav.yml`, and Biblios generates a full HTML documentation portal plus optional PDF and DOCX artifacts per component version.

## Quick Start

### Prerequisites

- Java 25 or later
- Git (for accessing content repositories)

### Build

```bash
./gradlew :thoth-biblios:build
```

The executable JAR is generated as:
```
thoth-biblios/build/libs/thoth-biblios-<version>-all.jar
```

### Your First Build

1. Create a `biblios.yml` configuration file:

```yaml
site:
  title: My Docs Portal
  url: https://docs.example.org
  logo: ./assets/logo.svg
  default_language: en

output:
  dir: build/site
  clean: true

content:
  sources:
    - id: mydocs
      display_name: My Documentation
      url: file:///path/to/your/docs-repo
      branches:
        - name: main
          display_version: Latest
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml

pdf:
  enabled: true
  attributes:
    pdf-theme: ./themes/default-theme.yml
```

2. Run the build (HTML only by default):

```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar build \
  --config biblios.yml
```

Optional: enable PDF output explicitly.

```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar build \
  --config biblios.yml \
  --pdf
```

Optional: restrict PDF generation to selected versions.

```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar build \
  --config biblios.yml \
  --pdf \
  --pdf-version main \
  --pdf-version mydocs/v1.x
```

Optional: enable DOCX output (requires explicit `--docx-version` filters).

```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar build \
  --config biblios.yml \
  --docx \
  --docx-version main \
  --docx-version mydocs/v1.x
```

3. The site is generated in `build/site/`. Open `build/site/index.html` in your browser.

### Development Server

For local preview with auto-rebuild on file changes:

```bash
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar serve \
  --config biblios.yml \
  --port 8080
```

The `serve` command:
- Performs an initial build
- Starts a local HTTP server on the specified port
- Watches the `biblios.yml` config file for changes
- Watches the cached Git repositories for changes
- Rebuilds automatically when content changes

## Configuration: `biblios.yml`

The `biblios.yml` file is the central configuration for Biblios. It defines your site metadata, output settings, and content sources.

### Full Example

```yaml
site:
  title: Interlis Docs
  url: https://docs.example.org
  logo: https://example.org/assets/interlis-logo.svg
  default_language: de

output:
  dir: build/site
  clean: true

ui:
  theme: default
  show_version_badge: false
  show_edit_link: false
  show_source_link: false
  syntax_highlighting: prism
  prism_custom_components:
    - ./highlighting/prism-customdsl.js
  version_switch_mode: start_page
  search_language_mode: multilingual_safe
  sidebar_toc_depth: 2
  content_toc: off
  edit_url_pattern: "https://github.com/org/repo/edit/{branch}/{path}"
  source_url_pattern: "https://github.com/org/repo/blob/{branch}/{path}"

content:
  sources:
    - id: cadastral
      display_name: Kataster
      url: https://github.com/example/cadastral-docs.git
      branches:
        - name: main
          display_version: Latest
        - name: v1.x
          display_version: Version 1.x
        - name: v2.x
          display_version: Version 2.x
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml
      start_page: index.adoc

    - id: api
      display_name: API Handbook
      url: https://github.com/example/api-docs.git
      branches:
        - name: main
          display_version: Current
        - name: v3.x
          display_version: 3.x
      start_path: handbook
      default_version: main
      navigation:
        file: nav.yml
      start_page: index.adoc
```

### Configuration Reference

#### `site` – Global Site Metadata

| Key | Required | Description |
|-----|----------|-------------|
| `title` | Yes | Title of the documentation portal (shown on start page and in headers) |
| `url` | No | Base URL of the published site (for reference, not yet used in routing) |
| `logo` | No | Logo shown before the site title in the header (`36x36`). Supports URL or local file path. Relative local paths are resolved relative to `biblios.yml`. |
| `default_language` | No | Language code (e.g., `en`, `de`). Default: `en` |

If `site.logo` points to a local file that does not exist, the build fails with a configuration error.

#### `output` – Output Settings

| Key | Required | Description |
|-----|----------|-------------|
| `dir` | Yes | Output directory for the generated HTML site |
| `clean` | No | Delete output directory before building. Default: `false` |

#### `ui` – User Interface Settings (MVP)

| Key | Required | Description |
|-----|----------|-------------|
| `theme` | No | Theme name. Currently only `default` is supported. |
| `show_version_badge` | No | Show version badges on pages. Default: `false` |
| `show_edit_link` | No | Show edit links on pages. Default: `false` |
| `show_source_link` | No | Show source links on pages. Default: `false` |
| `syntax_highlighting` | No | Syntax highlighting mode for code blocks. Allowed: `prism`, `off`. Default: `prism` |
| `prism_custom_components` | No | List of local `.js` files with custom Prism languages. Relative paths resolve from `biblios.yml`. |
| `edit_url_pattern` | No | URL pattern for edit links. Supports `{repo_url}`, `{branch}`, `{path}` placeholders. |
| `source_url_pattern` | No | URL pattern for source links. Supports `{repo_url}`, `{branch}`, `{path}` placeholders. |
| `version_switch_mode` | No | Version switch behavior. `start_page` (default) always jumps to version root. `equivalent_page` tries same source page in target version, otherwise version root. |
| `search_language_mode` | No | Lunr language/ranking mode. `multilingual_safe` (default) removes stemmer/stop-word filters for mixed-language docs. `english_default` keeps Lunr defaults. |
| `sidebar_toc_depth` | No | Sidebar heading depth for generated TOC entries. Allowed: `1..6`. Default: `2` |
| `content_toc` | No | Render AsciiDoc in-content TOC. Allowed values: `off`, `on`. Default: `off` |

#### `pdf` – PDF Output Settings

When `pdf.enabled: true`, Biblios can write one PDF per published component version into the corresponding version output directory.  
PDF generation is only executed when the CLI build is started with `--pdf`.

| Key | Required | Description |
|-----|----------|-------------|
| `enabled` | No | Generate PDF artifacts in addition to HTML. Default: `false` |
| `attributes` | No | Mapping of PDF-relevant Asciidoctor attributes passed to the PDF backend. Supports PDF theme/font options such as `pdf-theme`, `pdf-themesdir`, `pdf-fontsdir`, plus other PDF attributes like `optimize`, `compress`, `scripts`, `pdf-page-size`, etc. |

Path handling inside `pdf.attributes`:

- `pdf-theme`: accepts built-in theme names such as `default` or a local `.yml`/`.yaml` file path. Local paths are resolved relative to `biblios.yml`.
- `pdf-themesdir`: accepts a local directory path, resolved relative to `biblios.yml`.
- `pdf-fontsdir`: accepts either a string or a YAML list. Local directories are resolved relative to `biblios.yml`. `GEM_FONTS_DIR` and `uri:classloader:` entries are preserved.

#### `docx` – DOCX Output Settings

When `docx.enabled: true`, Biblios can write one DOCX per published component version into the corresponding version output directory.  
DOCX generation is only executed when the CLI build is started with `--docx` and one or more `--docx-version` filters.

| Key | Required | Description |
|-----|----------|-------------|
| `enabled` | No | Enable DOCX support. Default: `false` |
| `reference_doc` | No | Path to a `.docx` template with Word styles/page layout. Resolved relative to `biblios.yml`. |
| `features.title_page` | No | Add generated title page. Default: `false` |
| `features.toc` | No | Add Word TOC field. Default: `true` |
| `features.change_log` | No | Add placeholder section for changelog. Default: `false` |

#### `content.sources` – Content Sources

Each entry defines a Git repository as a documentation source.

| Key | Required | Description |
|-----|----------|-------------|
| `id` | Yes | Technical identifier for this documentation (used in URLs: `/<id>/<version>/`) |
| `display_name` | Yes | Human-readable name shown in the UI and switchers |
| `url` | Yes | Git repository URL. Supports `https://`, `ssh://`, and `file://` protocols |
| `branches` | Yes | List of branches to publish as versions (see below) |
| `start_path` | Yes | Relative path inside the repo where documentation root is located |
| `default_version` | Yes | Which version to show when navigating to `/<id>/` without explicit version |
| `navigation.file` | Yes | Navigation file name relative to `start_path`. Must be `nav.yml` in MVP |
| `start_page` | No | Start page filename relative to `start_path`. Default: `index.adoc` |
| `render_mode` | No | Rendering mode. Allowed values: `split`, `single_page`. Default: `split` |
| `master_file` | Conditionally | Required when `render_mode: single_page`. Path to the AsciiDoc master file under `start_path` |
| `sidebar_toc_numbers` | No | Show chapter numbers in the sidebar TOC. Allowed values: `off`, `on`. Default: `off` |
| `pdf.enabled` | No | Override global PDF enablement for this source only. |
| `pdf.master_file` | No | Optional PDF-specific master file under `start_path`. Useful for `split` sources that need a dedicated PDF assembly document. |
| `pdf.attributes` | No | Source-specific PDF attribute overrides merged on top of global `pdf.attributes`. |
| `docx.enabled` | No | Override global DOCX enablement for this source only. |
| `docx.master_file` | No | Optional DOCX-specific master file under `start_path`. |
| `docx.reference_doc` | No | Source-specific `.docx` template override. |
| `docx.features.*` | No | Source-specific feature overrides (`title_page`, `toc`, `change_log`). |

For `render_mode: single_page`, chapter entries with `[unnumbered]` are hidden from the sidebar TOC by default.  
If you want an unnumbered chapter to be shown (for example appendices like `Anhang A - ...`), add role `[.appendix]` to that section:

```adoc
[unnumbered]
[.appendix]
== Anhang A - foo bar
```

#### `branches` – Published Versions

Each branch entry maps a Git branch to a published documentation version.

| Key | Required | Description |
|-----|----------|-------------|
| `name` | Yes | Git branch name (exact name only in MVP). Examples: `main`, `v1.x`, `v2.0` |
| `display_version` | No | Human-readable label shown in the version switcher. Defaults to the branch name |

## Syntax Highlighting

Biblios uses Prism by default (`ui.syntax_highlighting: prism`) and supports the bundled languages:

- `interlis`, `xml`/`html`/`svg` (via Prism `markup`), `java`, `javascript`, `typescript`, `css`, `ini`, `json`, `bash`, `sql`, `python`, `yaml`, `kotlin`, `go`, `c`, `cpp`

Example in AsciiDoc:

```adoc
[source,interlis]
----
MODEL Demo;
END Demo.
----
```

Add your own Prism language component:

```yaml
ui:
  syntax_highlighting: prism
  prism_custom_components:
    - ./highlighting/prism-customdsl.js
```

Custom component files must exist and end with `.js`.

## Navigation: `nav.yml`

Each documentation version must have a `nav.yml` file in its `start_path`. This file defines the sidebar navigation, breadcrumbs, and page ordering.

### Example

```yaml
items:
  - title: Introduction
    page: index.adoc
  - title: Getting Started
    page: installation.adoc
  - title: User Guide
    children:
      - title: Basic Usage
        page: usage.adoc
      - title: Configuration
        page: config.adoc
      - title: Advanced Topics
        children:
          - title: API Reference
            page: api.adoc
          - title: Internals
            page: internals.adoc
  - title: FAQ
    page: faq.adoc
```

### Structure

| Key | Required | Description |
|-----|----------|-------------|
| `items` | Yes | Top-level list of navigation entries |
| `title` | Yes | Display text for this navigation entry |
| `page` | Conditionally | Path to the `.adoc` file (relative to `start_path`). Required if no `children` |
| `children` | Conditionally | Nested sub-entries. Required if no `page` |

### Rules

- Each entry must have **either** `page` **or** `children` (or both)
- `page` paths are relative to the `start_path` in `biblios.yml`
- Nesting depth is unlimited
- Only `.adoc` files referenced in `nav.yml` are included in the navigation sidebar
- Pages that exist as `.adoc` files but are NOT in `nav.yml` are still rendered but not linked from the sidebar

## URL Schema

Biblios uses a consistent URL schema:

```
/<component>/<version>/<page-path>/
```

### Examples

```
/mydocs/main/                     # Start page of mydocs, main version
/mydocs/main/guide/               # guide.adoc in mydocs main
/mydocs/v1.x/guide/               # guide.adoc in mydocs v1.x
/api/current/authentication/      # authentication.adoc in api, current version
```

### Component Landing Pages

Each component also has a landing page at `/<component>/index.html` that shows an overview of the documentation with all available versions.

### URL Characteristics

- URLs always end with `/` (directory-style, serves `index.html`)
- The version is always visible in the URL
- No redirects from `/<component>/` to `/<component>/<default-version>/` in the MVP
- URLs are stable and cache-friendly

## Git Sources and Branch Versioning

### How It Works

1. On the first build, Biblios **clones** each configured Git repository into a local cache (`.thoth/cache/repos/<source-id>/`)
2. On subsequent builds, Biblios **fetches** updates instead of re-cloning
3. For each configured branch, Biblios **checks out** the branch into an isolated work directory
4. The documentation is then read from the checked-out work directory

### Local Git Cache

The cache is stored at:
```
.thoth/cache/repos/<source-id>/
```

Benefits of caching:
- Subsequent builds are faster (fetch instead of clone)
- Multiple builds can share the same cached repository
- The `serve` command uses the cache as its source-of-truth working copy

To clear the cache:
```bash
rm -rf .thoth/cache
```

### Branch Resolution

- Exact branch names are matched first (e.g., `main`, `v1.x`)
- If a branch does not exist, a warning is logged and the version is skipped
- Branch patterns (e.g., `release/*`) are NOT yet supported in the MVP — use exact names

### Local Repositories

For testing or local documentation, you can use `file://` URLs:

```yaml
url: file:///home/user/my-docs-repo
```

The local repository must be a valid Git repository with at least one commit.

## `display_version`

The `display_version` field allows you to show a human-readable label instead of the raw branch name in the version switcher.

### Example

```yaml
branches:
  - name: main
    display_version: Latest
  - name: v1.x
    display_version: Version 1.x (Legacy)
  - name: v2.0
    display_version: 2.0 Stable
```

In the UI, users see "Latest", "Version 1.x (Legacy)", and "2.0 Stable" instead of `main`, `v1.x`, and `v2.0`.

If `display_version` is not specified, the branch name is used as-is.

## Global Search

Biblios provides a client-side search experience with:
- Header search field on all pages
- Scope toggle in the header (`global` or `active docs version`)
- Dedicated search page at `/search/`
- Global `search-index.json` covering all sources and versions
- Lunr-based ranking in the browser

### Search Index Contents

Each entry in the search index contains:
- `component` – Source ID
- `version` – Branch/version name
- `displayVersion` – Human-readable version label
- `kind` – Entry type (`chapter` or `page`)
- `title` – Chapter title (or page title for fallback entries)
- `pageTitle` – Parent page title
- `sectionPath` – Hierarchical section path (for example `Einleitung > Status`)
- `sectionLevel` – Section depth (`1..4`, or `0` for page fallback entries)
- `route` – URL path to the chapter/page (chapter entries include `#anchor`)
- `content` – Indexed plain text for that chapter/page

Section entries are indexed up to `sect4`. Deeper sections are ignored for search indexing.

### Search Scope Parameters

The search UI uses query parameters:
- `q` – Search query
- `scope` – `global` (default) or `active`
- `component` – Required when `scope=active`
- `version` – Required when `scope=active`

When `scope=active`, search is restricted to the exact `component + version` combination.
If `scope=active` is provided without both `component` and `version`, Biblios falls back to `scope=global`.

### Search Index Location

```
<output-dir>/search-index.json
```

### Search Language Mode

Configure Lunr behavior in `biblios.yml`:

```yaml
ui:
  search_language_mode: multilingual_safe
```

Available modes:
- `multilingual_safe` (default): removes stemmer and stop-word filters for more robust DE/EN (or mixed-language) matching.
- `english_default`: uses Lunr's default English-oriented pipeline.

## CLI Commands

### `build`

Builds the documentation site from `biblios.yml`.

```bash
java -jar thoth-biblios-<version>-all.jar build \
  --config biblios.yml \
  --output build/site \
  --clean
```

| Option | Required | Description |
|--------|----------|-------------|
| `--config` | Yes | Path to `biblios.yml` configuration file |
| `--output` | No | Output directory (overrides `output.dir` in config) |
| `--clean` | No | Delete output directory before building |
| `--pdf` | No | Enable PDF generation in addition to HTML (requires PDF to be enabled in config globally or per source) |
| `--pdf-version` | No | Limit PDF generation to selected versions (`main`, `v1.x`, or `<component>/<version>`). Requires `--pdf`. |

### `serve`

Runs the development server with auto-rebuild.

```bash
java -jar thoth-biblios-<version>-all.jar serve \
  --config biblios.yml \
  --output build/site \
  --port 8080
```

| Option | Required | Description |
|--------|----------|-------------|
| `--config` | Yes | Path to `biblios.yml` configuration file |
| `--output` | No | Output directory (overrides `output.dir` in config) |
| `--port` | No | HTTP server port. Default: `8080` |
| `--use-local-working-tree` | No | For local sources (`file://` or local paths), render the currently checked-out branch directly from the local working tree |

**Serve behavior:**
1. Performs an initial build
2. Starts an HTTP server serving the output directory
3. Watches `biblios.yml` for config changes → triggers full clean rebuild
4. Watches local content source paths only with `--use-local-working-tree`
5. Without `--use-local-working-tree`, local content watching is disabled
6. Local content changes trigger an incremental rebuild for the affected source only
7. Config changes still trigger a full rebuild to avoid stale output artifacts
8. Ignores `.git` metadata changes in watched local sources (prevents rebuild loops caused by internal Git updates)
9. Does not watch `.thoth/cache` directly
10. Runs `git fetch` only during the initial `serve` build; watch-triggered rebuilds use the cached repository without fetching
11. With `--use-local-working-tree`, local sources read the currently checked-out branch directly from the local repository path (uncommitted changes become visible immediately)
12. With `--use-local-working-tree`, additional configured branches of the same source still use the cached repository checkout
13. Remote updates are picked up after restarting `serve`
14. Why: avoids self-triggered rebuild loops where the build process modifies the same path that is being watched
15. Press `Ctrl+C` to stop

## Multi-Source Example

Here's a complete example with two content sources and multiple versions:

```yaml
site:
  title: Project Documentation
  url: https://docs.example.org
  default_language: en

output:
  dir: build/site
  clean: true

content:
  sources:
    - id: user-guide
      display_name: User Guide
      url: https://github.com/example/user-guide.git
      branches:
        - name: main
          display_version: Current
        - name: v2.x
          display_version: 2.x
        - name: v1.x
          display_version: 1.x (Legacy)
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml

    - id: api-reference
      display_name: API Reference
      url: https://github.com/example/api-reference.git
      branches:
        - name: main
          display_version: Current
        - name: v3.x
          display_version: 3.x
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml
```

This produces a portal with:
- Global start page listing "User Guide" and "API Reference"
- `/user-guide/main/`, `/user-guide/v2.x/`, `/user-guide/v1.x/`
- `/api-reference/main/`, `/api-reference/v3.x/`
- Doc switcher to jump between User Guide and API Reference
- Version switcher on each doc's pages

## Multi-Version Example

For a single repository with multiple branches:

```yaml
content:
  sources:
    - id: mylib
      display_name: My Library
      url: https://github.com/example/mylib-docs.git
      branches:
        - name: main
          display_version: Latest
        - name: stable
          display_version: Stable
        - name: v1.x
          display_version: 1.x Maintenance
      start_path: documentation
      default_version: main
      navigation:
        file: nav.yml
```

Each branch (`main`, `stable`, `v1.x`) is checked out independently and rendered as a separate version.

## Testing

### Run All Tests

```bash
# Unit tests
./gradlew :thoth-biblios:test

# Integration tests (full build pipeline with local Git repos)
./gradlew :thoth-biblios:integrationTest

# End-to-End tests (realistic user flows: build, HTML output, search, serve)
./gradlew :thoth-biblios:e2eTest

# All tests
./gradlew :thoth-biblios:test :thoth-biblios:integrationTest :thoth-biblios:e2eTest
```

### Test Categories

| Category | Purpose | Examples |
|----------|---------|----------|
| **Unit Tests** | Config parsing, navigation parsing, routing, breadcrumbs, rendering | `BibliosConfigParserTest`, `NavParserTest`, `RoutingTest` |
| **Integration Tests** | Full build pipeline: Git fetching → catalog building → site generation | `BibliosIntegrationTest` |
| **E2E Tests** | Realistic end-to-end flows: multi-source, multi-version, HTML output verification, DevServer | `BibliosE2ETest` |

## Known MVP Limitations

1. **No Redirects**: Navigating to `/<component>/` does NOT redirect to `/<component>/<default-version>/`. Component landing pages show an overview instead.
2. **Version Switch Mode**: By default (`ui.version_switch_mode: start_page`) the switcher always jumps to the target version start page. Set `ui.version_switch_mode: equivalent_page` to prefer same-source-page mapping with fallback to the target version start page.
3. **No Branch Patterns**: Patterns like `release/*` are listed in the spec but not yet implemented in the MVP. Use exact branch names.
4. **No Tag-Based Versions**: Only branch-based versions are supported.
5. **Global Search Only**: Search covers all documentation and versions without faceting or filtering.
6. **Single Theme Only**: Only the default theme is available. No theming API yet.
7. **Single Theme for HTML**: HTML theming is still limited to the bundled default theme.
8. **No Multi-Language**: Each component has a single language.
9. **Edit/Source Links**: Configurable via `ui.show_edit_link`, `ui.show_source_link`, `ui.edit_url_pattern`, and `ui.source_url_pattern` in `biblios.yml`. Patterns support `{repo_url}`, `{branch}`, and `{path}` placeholders. Example: `edit_url_pattern: "https://github.com/org/repo/edit/{branch}/{path}"`.

## thoth-blog vs. thoth-biblios: Which Should I Use?

| Feature | thoth-blog | thoth-biblios |
|---------|-----------|---------------|
| **Purpose** | Blog / journal / news site | Documentation portal |
| **Content model** | Posts with author, date, tags, teaser | Documentation pages with navigation |
| **Input** | Single directory with `.adoc` files | Multiple Git repositories |
| **Versioning** | None (chronological) | Branch-based versions |
| **Navigation** | Archive, tags, homepage | `nav.yml` sidebar, breadcrumbs, prev/next |
| **Switchers** | None | Doc switcher + version switcher |
| **Search** | Client-side Lunr search | Header search + `/search/` page (Lunr + global index) |
| **Output** | Blog homepage, archive, tag pages, RSS | Global start page, component pages, doc pages |
| **Config** | `thoth.properties` | `biblios.yml` |

**Use `thoth-blog` if:**
- You write blog posts, articles, or news
- You want chronological listing with tags and RSS feed
- You have a single content source (a directory)

**Use `thoth-biblios` if:**
- You write documentation for software projects
- You need multiple versions (e.g., v1.x, v2.x, latest)
- You have content spread across multiple Git repositories
- You want a portal that combines multiple documentations

## Troubleshooting

### "Branch not found" Warning

```
[warn] Branch 'v1.x' not found in mydocs, skipping
```

The configured branch does not exist in the Git repository. Check:
- The branch name is spelled correctly
- The branch has been pushed to the remote
- For local repos, the branch exists in the local repository

### "Navigation file not found" Warning

```
[warn] Navigation file not found: /path/to/docs/nav.yml
```

The `nav.yml` file is missing at the expected location. Check:
- The file is named correctly (matches `navigation.file` in `biblios.yml`)
- The file is in the `start_path` directory
- The `start_path` in `biblios.yml` is correct

### "Documentation root not found" Error

```
Documentation root not found: /path/to/work/docs
Check the 'start_path' configuration for source: mydocs
```

The `start_path` points to a directory that does not exist in the checked-out repository. Check:
- The path is relative to the repository root
- The directory exists in the checked-out branch

### "Failed to parse YAML" Error

```
Failed to parse YAML: while parsing a block mapping
```

The `biblios.yml` file has a YAML syntax error. Check:
- Indentation is consistent (use spaces, not tabs)
- Required fields are present (`site.title`, `content.sources`, etc.)
- List items under `sources:` are properly indented

### Git Repository Unreachable

```
Cloning repository: https://github.com/example/docs.git
```

If the clone fails:
- Check the URL is correct and accessible
- For private repos, ensure authentication is configured (SSH keys, credentials)
- For local repos, use `file:///absolute/path` format

### Clear Cache

If you suspect stale Git data:

```bash
rm -rf .thoth/cache
```

Then run the build again.

## Build the Project

```bash
# Build all modules
./gradlew build

# Build Biblios only
./gradlew :thoth-biblios:build

# Run all tests
./gradlew test :thoth-biblios:integrationTest :thoth-biblios:e2eTest

# Create executable JAR
./gradlew :thoth-biblios:fatJar
```

The JAR is available at:
```
thoth-biblios/build/libs/thoth-biblios-<version>-all.jar
```
