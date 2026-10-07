# Thoth

Thoth provides JVM-based static site generators for AsciiDoc content and an authenticated documentation portal.

## Product Family

| Module | Purpose |
|--------|---------|
| **`thoth-blog`** | Static site generator for AsciiDoc blogs |
| **`thoth-biblios`** | Multi-repo documentation site generator with versioning |
| **`thoth-biblios-core`** | Biblios configuration, catalog, access/publication models and shared templates |
| **`thoth-biblios-server`** | Authenticated Biblios portal (OIDC, per-documentation access policies) |
| **`thoth-core`** | Shared technical infrastructure |

Commands below assume the repository root as the working directory. Replace
`<version>` in JAR filenames with the version produced by your Gradle build.

## Quick Start

```bash
# Build all modules (Java 17 and Java 25 toolchains required)
./gradlew build

# Run tests
./gradlew test integrationTest e2eTest
```

## Products

### thoth-blog

Plain text. Real websites.

Thoth Blog builds pretty URLs, tag pages, RSS, local assets, Lunr search, multilingual blogs with translation switching, and a watch-based dev server.

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

### thoth-biblios-core

Biblios configuration (`biblios.yml`, `access.yml`), catalog model, access model and the shared
FreeMarker view contracts and templates used by both the static generator and the server.

### thoth-biblios-server

Authenticated documentation portal. The build produces a private publication package
(`--package`), the server renders navigation, switchers and search per user and enforces
per-documentation access policies (OIDC, e.g. Entra ID or Keycloak).

```bash
# Build only a private publication package, including protected sources
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar build \
  --config biblios.yml --package build/package

# Run the server (JVM, local development profile on port 8091)
./gradlew :thoth-biblios-server:bootJar
java -jar thoth-biblios-server/build/libs/thoth-biblios-server-<version>.jar \
  --spring.profiles.active=dev \
  --biblios.package-dir=build/package --biblios.access-config=access.yml

# Or build a native binary
GRAALVM_HOME=/path/to/graalvm-25 ./gradlew :thoth-biblios-server:nativeCompile
```

**Server reference:** [thoth-biblios-server/README.md](thoth-biblios-server/README.md).
The server requires Java 25 (GraalVM 25 for native builds). Build a public static
site separately with `build --public-export`; `--package` cannot be combined with
`--public-export` or `--output`.

Local development: Keycloak runs as a container (port 8090), the server as a
Java application (port 8091). The complete walkthrough is in
[dev/keycloak/README.md](dev/keycloak/README.md).

### thoth-core

Shared technical infrastructure:
- `DevServer` – HTTP static file server
- `InputWatcher` – Recursive file system watcher
- `ServeHandle` – Serve/watch orchestration helper
- Shared language/URL helpers and INTERLIS Lab support
- Internal AsciidoctorJ, FreeMarker and jsoup dependencies; products declare their own usage

## Testing

### Test Strategy

`build` runs unit tests but does not automatically run `integrationTest` or `e2eTest`.
The server Keycloak E2E test requires Docker and is skipped when Docker is unavailable.

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
with a Gradle property (the current bundled default is `0.1.10`):

```bash
./gradlew build -PinterlisLabVersion=0.1.10
```

Optional: override the tarball URL directly (for npmjs.org, mirrors, or internal proxies). When set,
this URL takes precedence over package metadata lookup:

```bash
./gradlew build \
  -PinterlisLabVersion=0.1.10 \
  -PinterlisLabTarballUrl=https://codeberg.org/api/packages/edigonzales/npm/%40edigonzales%2Finterlis-lab-web-component/-/0.1.10/interlis-lab-web-component-0.1.10.tgz
```

The metadata endpoint can also be overridden with `-PinterlisLabPackageMetadataUrl=<url>`.
The first asset resolution requires network access; subsequent builds reuse Gradle outputs.

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
      start_path: docs
      default_version: main
      navigation:
        file: nav.yml
```

## Architecture

For detailed architecture documentation, see [ARCHITECTURE.md](ARCHITECTURE.md).

Key decisions:
- **Java 17** for generators/shared modules; **Java 25** for the server
- **AsciidoctorJ** for rendering (not Asciidoctor.js)
- **nav.yml** for explicit split-page navigation; automatic discovery fallback and heading-based single-page navigation
- **Gradle multi-project** with clear module boundaries
- **DevServer + InputWatcher** centralized in thoth-core

## Project Structure

```
thoth/
├── thoth-core/           Shared technical infrastructure
├── thoth-blog/           Blog generator
├── thoth-biblios-core/   Shared Biblios models, policies and templates
├── thoth-biblios/        Documentation generator and publication packages
└── thoth-biblios-server/ Authenticated documentation portal
```

### Module Responsibilities

| Module | Contains |
|--------|----------|
| **thoth-core** | DevServer, InputWatcher, ServeHandle, language/URL helpers, INTERLIS Lab support |
| **thoth-blog** | Post parsing, tags, RSS, templates, blog CLI |
| **thoth-biblios-core** | YAML config, catalog/navigation/access/publication models, view factory and templates |
| **thoth-biblios** | Git fetching, catalog building, HTML/PDF/DOCX, package writer, CLI |
| **thoth-biblios-server** | OIDC, package loading, authorization and per-user portal rendering |

## Current Limitations (thoth-biblios)

1. No redirects from `/<component>/` to default version
2. No branch patterns (`release/*`) – use exact branch names
3. No tag-based versions – only branch-based
4. Search supports global and active-version scopes; no arbitrary faceting
5. One bundled theme, customizable through template and asset overrides
6. No multi-language per component

See [thoth-biblios/README.md](thoth-biblios/README.md) for the full list.

## Troubleshooting

### Java Version

The generator JARs require Java 17 or later. The server requires Java 25.
Building all modules requires Java 17 and Java 25 toolchains discoverable by Gradle:

```bash
java -version
# Inspect the JDKs Gradle can use:
./gradlew -q javaToolchains
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
- Biblios: [thoth-biblios/README.md](thoth-biblios/README.md)
- Portal: [thoth-biblios-server/README.md](thoth-biblios-server/README.md)
- Architecture: [ARCHITECTURE.md](ARCHITECTURE.md)
