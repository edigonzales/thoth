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

### thoth-blog

Single directory with `.adoc` files and `thoth.properties`:

```
input/
  thoth.properties
  blog/
    2026/
      hello.adoc
      image.png
```

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
- **Java 25** target platform
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
6. No PDF output – HTML only
7. No multi-language per component

See [thoth-biblios/README.md](thoth-biblios/README.md) for the full list.

## Troubleshooting

### Java Version

Both products require Java 25:

```bash
java -version
# If not Java 25:
sdk use java 25.0.1-tem
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
