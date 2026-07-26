# Thoth

Thoth is a family of JVM-based static site generators for AsciiDoc content.

## Product Family

| Product | Purpose | Status |
|---------|---------|--------|
| **`thoth-blog`** | Static site generator for AsciiDoc blogs | Production-ready |
| **`thoth-biblios`** | Multi-repo documentation site generator with versioning | MVP complete |
| **`thoth-core`** | Shared technical infrastructure | Production-ready |

## Quick Start

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test integrationTest e2eTest
```

## Products

### thoth-blog

Plain text. Real websites.

Thoth Blog builds pretty URLs, tag pages, RSS, local assets, Lunr search, and a watch-based dev server.

**Details:** See [thoth-blog/README.md](thoth-blog/README.md)

```bash
./gradlew :thoth-blog:build
java -jar thoth-blog/build/libs/thoth-blog-<version>-all.jar --help
```

### thoth-biblios

Multi-repo documentation site generator with versioning support. Think of it as a lightweight, JVM-native alternative to Antora.

**Details:** See [thoth-biblios/README.md](thoth-biblios/README.md)

```bash
./gradlew :thoth-biblios:build
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar --help
```

### thoth-core

Shared technical infrastructure:
- `DevServer` – HTTP static file server
- `InputWatcher` – Recursive file system watcher
- `ServeHandle` – Serve/watch orchestration helper
- AsciidoctorJ, FreeMarker, jsoup dependencies

## Testing

### Test Strategy

Thoth uses a three-tier test strategy across all modules:

| Category | Purpose | Scope |
|----------|---------|-------|
| **Unit Tests** (`test`) | Isolated component tests | Parsers, config, slugging, templates, routing |
| **Integration Tests** (`integrationTest`) | Full build pipeline tests | Realistic input with local Git repos |
| **E2E Tests** (`e2eTest`) | End-to-end user flows | Build + serve + HTTP verification |

### Running Tests

```bash
# All modules
./gradlew test integrationTest e2eTest

# Specific module
./gradlew :thoth-blog:test :thoth-blog:integrationTest :thoth-blog:e2eTest
./gradlew :thoth-biblios:test :thoth-biblios:integrationTest :thoth-biblios:e2eTest
```

### Coverage

Both `thoth-blog` and `thoth-biblios` use JaCoCo for coverage reporting:

```bash
./gradlew :thoth-blog:jacocoTestReport
./gradlew :thoth-biblios:jacocoTestReport
```

HTML reports: `<module>/build/reports/jacoco/test/html/index.html`

## Input Structures

### INTERLIS Lab

Blog posts and Biblios pages can embed the bundled Interlis Lab web component:

```asciidoc
interlis-lab::labs/simple.json[storage-key=simple-lab,title="Simple Lab"]
```

The macro emits `<interlis-lab>` only for HTML output. Thoth copies the bundled component runtime to
`assets/interlis-lab/` for Blog and `site-assets/interlis-lab/` for Biblios, injects the module
script only on pages that use a lab, and copies referenced lesson JSON files next to the generated
page.

During build, Thoth resolves `@edigonzales/interlis-lab-web-component` from Codeberg Packages and
extracts `dist/interlis-lab.js` plus `dist/ili2c.jar` into both products. Select the package version
with a Gradle property:

```bash
./gradlew build -PinterlisLabVersion=0.1.2
```

Optional: override the tarball URL directly (for npmjs.org, mirrors, or internal proxies). When set,
this URL takes precedence over package metadata lookup:

```bash
./gradlew build \
  -PinterlisLabVersion=0.1.2 \
  -PinterlisLabTarballUrl=https://codeberg.org/api/packages/edigonzales/npm/%40edigonzales%2Finterlis-lab-web-component/-/0.1.2/interlis-lab-web-component-0.1.2.tgz
```

### thoth-blog

Input root with required content directory plus optional theme overrides:

```
input/
  thoth.properties
  blog/        # required
    2026/
      hello.adoc
      image.png
  templates/   # optional template overrides
  assets/      # optional theme asset overrides
```

Breaking change: the old flat input layout is no longer supported.
`blog/2026/hello.adoc` now generates `/2026/hello/` (without `/blog` prefix).

### thoth-biblios

YAML configuration (`biblios.yml`) pointing to Git repositories:

```yaml
site:
  title: My Docs Portal
  url: https://docs.example.org

content:
  sources:
    - id: mydocs
      display_name: My Documentation
      url: https://github.com/example/docs.git
      branches:
        - name: main
          display_version: Latest
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml
```

## Architecture

For detailed architecture documentation, see [ARCHITECTURE.md](ARCHITECTURE.md).

Key decisions:
- **Java 17** baseline (Java 17 or later)
- **AsciidoctorJ** for rendering (not Asciidoctor.js)
- **nav.yml** as navigation standard for Biblios
- **Gradle multi-project** with clear module boundaries
- **DevServer + InputWatcher** centralized in thoth-core

## Project Structure

```
thoth/
├── thoth-core/          Shared technical infrastructure
├── thoth-blog/          Blog-specific product
└── thoth-biblios/       Multi-repo documentation generator
```

### Module Responsibilities

| Module | Contains |
|--------|----------|
| **thoth-core** | DevServer, InputWatcher, ServeHandle, shared dependencies (AsciidoctorJ, FreeMarker, jsoup) |
| **thoth-blog** | Post parsing, tags, RSS, templates, blog CLI |
| **thoth-biblios** | YAML config, Git fetching, catalog building, doc templates, biblios CLI |

## Known MVP Limitations (thoth-biblios)

1. No redirects from `/<component>/` to default version
2. No branch patterns (`release/*`) – use exact branch names
3. No tag-based versions – only branch-based
4. Global search only – no faceting
5. Single theme only
6. No multi-language per component

See [thoth-biblios/README.md](thoth-biblios/README.md) for the full list.

## Troubleshooting

### Java Version

Both products require Java 17 or later:

```bash
java -version
# If Java 17 or later is not active:
sdk use java 17.0.12-tem
```

### Clean Build

```bash
./gradlew clean build
```

### Git Cache (Biblios)

```bash
rm -rf .thoth/cache
```

## Specifications

- Blog: [thoth-blog/README.md](thoth-blog/README.md)
- Biblios spec: [thoth-biblios-spec-v2.md](thoth-biblios-spec-v2.md)
- Architecture: [ARCHITECTURE.md](ARCHITECTURE.md)
