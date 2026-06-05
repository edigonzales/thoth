# How Biblios Resolves Structure

This guide explains the current, implemented behavior of `thoth-biblios` when it turns AsciiDoc sources into a site.

It focuses on the authoring questions that are easy to confuse in practice:

- chapter and section numbering
- `render_mode: split` versus `render_mode: single_page`
- `nav.yml`
- `start_page`
- `master_file`
- `sidebar_toc_numbers`
- `content_section_numbers`
- document title lines such as `= My Title`
- AsciiDoc `doctype`

This is intentionally a behavior guide, not a config API reference. The descriptions below match the current code paths in `CatalogBuilder`, `AsciidoctorRenderer`, `BibliosPdfGenerator`, `BibliosDocxGenerator`, and the related Biblios tests.

A German translation of this page is available at [authoring-modes-and-navigation.de.md](authoring-modes-and-navigation.de.md).

## Quick Decision Table

| `render_mode` | `nav.yml` | `start_page` | Numbering settings | Required source files | Generated result | Important caveats |
|-----|-----|-----|-----|-----|-----|-----|
| `split` | Valid `navigation.file` that parses successfully | Used as version root route. If the file exists but is not listed in `nav.yml`, Biblios prepends it to the build list. | `ui.content_section_numbers` controls rendered section numbers inside each page. | Individual `.adoc` pages, plus `nav.yml` if configured. | One HTML page per referenced source page. Sidebar, breadcrumbs, and prev/next order come from `nav.yml`. | With a non-empty `nav.yml`, Biblios currently builds only pages referenced there, plus `start_page` if it exists and was missing from nav. |
| `split` | Missing file, invalid YAML, or not configured | Still used as the preferred version root if that file exists. | `ui.content_section_numbers` still controls rendered section numbers. | Individual `.adoc` pages. No nav file is required. | One HTML page per discovered `.adoc` file. Discovery is recursive under `start_path`. | On this fallback path, Biblios warns and auto-discovers `.adoc` files sorted by relative path. Sidebar order then follows that discovered order. |
| `split` | Valid `nav.yml`, but `start_page` is not listed in it | If the file exists, Biblios inserts it at the front and gives it the root route `/<component>/<version>/`. | Same as other `split` builds. | `start_page` file plus nav-listed pages. | Start page is built even though it is absent from nav, and it becomes the version root. | The page is built, but nav breadcrumbs and nav placement still do not come from an explicit nav entry. |
| `single_page` | Ignored for page generation and sidebar structure | Not used to choose the root page. The version always renders from `master_file`. | `ui.content_section_numbers` controls whether numbered headings are rendered in content. `ui.sidebar_toc_depth` controls how many heading levels enter the generated sidebar tree. | `master_file` is mandatory. | Exactly one HTML page for the whole version. Sidebar entries are generated from headings in the rendered master document. | `navigation.file` does not drive the sidebar in this mode. `start_page` is not the controlling document. |
| `single_page` | Ignored | Ignored | `content_section_numbers: on` plus `sidebar_toc_numbers: on` prefixes sidebar chapter labels with heading numbers. | `master_file`. | One page with a generated heading-based sidebar. | `sidebar_toc_numbers` only affects the single-page sidebar labels. It does not affect split mode. |
| `single_page` | Ignored | Ignored | `content_section_numbers: off` suppresses numbered headings in content and also suppresses sidebar number prefixes, even if `sidebar_toc_numbers: on`. | `master_file`. | One page with an unnumbered heading-based sidebar. | `sidebar_toc_numbers` is effectively gated by `content_section_numbers`. |

## Core Rules

### `split` mode

Use `split` when each source file should become its own HTML page.

Current behavior:

- If `navigation.file` is configured and the file parses successfully, Biblios uses that nav tree as the source of page order, breadcrumbs, and sidebar structure.
- In that case, Biblios collects page paths from `nav.yml` and builds those pages.
- If the configured `start_page` exists but is missing from `nav.yml`, Biblios adds it to the front of the build list so that the version root still works.
- If no usable nav file is available, Biblios recursively discovers `.adoc` files under `start_path`.
- Routes are page-based: `guide.adoc` becomes `/<component>/<version>/guide/`, while the `start_page` becomes `/<component>/<version>/`.

Important implication:

- `nav.yml` is not strictly required in `split` mode.
- But when `nav.yml` is present and valid, it is not just decoration. It effectively defines which pages are built, except for the extra `start_page` insertion described above.

### `single_page` mode

Use `single_page` when one master document should render the whole version.

Current behavior:

- `master_file` is required.
- Biblios renders the master document once and emits exactly one HTML page for the version root.
- Sidebar entries are derived from the rendered heading tree, not from `nav.yml`.
- `navigation.file` is ignored for sidebar/page generation in this mode.
- `start_page` does not select the source document in this mode.

Practical implication:

- For `single_page`, think in terms of AsciiDoc document assembly with `include::...[]`, not in terms of page lists in `nav.yml`.

## Numbering Behavior

### `ui.content_section_numbers`

This setting controls whether Biblios asks Asciidoctor to render section numbers in HTML content.

- `on` adds `sectnums` during HTML rendering.
- `off` unsets `sectnums` during HTML rendering.

Effects:

- In `split` mode, it changes numbering inside each rendered page.
- In `single_page` mode, it changes numbering inside the master document and also affects whether Biblios can show numbering in the generated sidebar.

### `sidebar_toc_numbers`

This is a source-level setting and only matters in `single_page`.

- `off` keeps sidebar entries unprefixed.
- `on` prefixes generated sidebar entries with heading numbers such as `1.` or `1.1.`, but only if `ui.content_section_numbers: on`.

This setting does not change:

- split-mode navigation labels
- the numbering inside the rendered content itself

### `[unnumbered]` and `[.appendix]` in `single_page`

Biblios filters the generated single-page sidebar from the heading tree.

Current behavior:

- `[unnumbered]` sections are hidden from the generated sidebar by default.
- If an unnumbered section also has role `[.appendix]`, Biblios keeps it in the sidebar.

That makes this pattern useful for appendices that should stay visible in the sidebar without looking like numbered chapters:

```adoc
[unnumbered]
[.appendix]
== Appendix A - Reference Tables
```

## `nav.yml`: When It Matters

`nav.yml` is not universally required.

### In `split`

- If present and valid, it defines the built page set and the sidebar/breadcrumb order.
- If absent or unusable, Biblios falls back to recursive `.adoc` discovery.

### In `single_page`

- It is not used to generate the page or the sidebar tree.
- The heading structure in `master_file` is what matters.

So the short answer is:

- `nav.yml` is optional in `split`
- `nav.yml` is operationally irrelevant in `single_page`

## Cross-Project Links

Biblios distinguishes between:

- links that target another page inside the same component/version
- links that target a route in another component

### What works reliably

For cross-project links inside the same Biblios site, prefer explicit Biblios routes with `link:`.

Examples for `split` targets:

```adoc
link:/target-project/main/guide/[Guide]
link:/target-project/main/guide/#configuration[Configuration]
```

Example for a `single_page` target:

```adoc
link:/target-project/main/#configuration[Configuration]
```

In these examples:

- `target-project` is the target `source.id` from `biblios.yml`
- `main` is the target version/branch in Biblios routing
- `#configuration` is an explicit section anchor in the target document

For stable section links, prefer explicit anchors in the target content:

```adoc
[#configuration]
== Configuration
```

### What does not resolve across projects

Relative AsciiDoc cross references are only resolved within the current component/version.

These are therefore the wrong tool for cross-project navigation:

```adoc
xref:../target-project/guide.adoc[]
xref:guide.adoc[]
```

The second example is valid only when `guide.adoc` belongs to the current component/version.

## Is a Document Title Line (`= ...`) Mandatory?

Short answer: no, not technically in every case, but often yes in practice.

### What happens without `= Title`

In HTML builds:

- Biblios first asks Asciidoctor for the document title.
- If that is blank in `split` mode, Biblios falls back to the first rendered `<h1>` if one exists.
- If that also fails, Biblios falls back to the filename without `.adoc`.
- In `single_page`, if the rendered document title is blank, Biblios falls back to `display_name` from `biblios.yml`.

So a document without a top-level `= Title` can still build.

### Why it is still usually the right choice

Use a proper document title line when:

- the page should have a stable, intentional page title
- you want predictable page labels outside nav-driven contexts
- the document is a real master document for `single_page`
- the same source is reused for PDF or DOCX

You can get away without `= Title` for small included fragments, but for standalone pages and master files it is best treated as required authoring hygiene.

## What `doctype` Changes in Biblios Today

`doctype` matters, but not in the same way across HTML, PDF, and DOCX.

### HTML site generation

For HTML, Biblios does not branch on `doctype` in its own site-generation logic.

What actually happens:

- Biblios loads and renders the document through Asciidoctor.
- Asciidoctor sees the document's `doctype` and may change document semantics accordingly.
- Biblios then uses the rendered HTML and extracted heading data.

So for HTML, `doctype` influences output indirectly through Asciidoctor semantics, not through Biblios-specific routing or navigation rules.

### PDF and DOCX with explicit master files

If you use:

- `render_mode: single_page` with its `master_file`
- or `pdf.master_file`
- or `docx.master_file`

then that master document's own `doctype` is what Asciidoctor receives.

### PDF and DOCX for split sources without explicit artifact master files

When Biblios has to assemble many split pages into one artifact, it creates a temporary aggregate master and forces:

```adoc
:doctype: book
```

This happens in the aggregate-master path for both PDF and DOCX.

Practical consequence:

- HTML `split` pages keep whatever each source page declares, or Asciidoctor's default if nothing is declared.
- Aggregate PDF/DOCX exports from split sources are assembled as `book`.

### Current Biblios-specific doctype logic

Today Biblios itself only does these doctype-related things:

- it reads the loaded `doctype` from Asciidoctor
- it preserves that value in the DOCX normalization model
- it forces `book` when it creates aggregate PDF/DOCX master documents for split sources

It does not currently switch HTML routing, sidebar generation, or page selection based on `doctype`.

## Practical Recommendations

- Use `split` when your authors think in discrete pages and want nav-driven ordering.
- Use `single_page` when your authors think in one assembled manual with heading-driven sidebar navigation.
- Treat `master_file` as mandatory design input for `single_page`, not as an afterthought.
- Treat `= Title` as optional only for fragments; for standalone pages and masters, include it.
- Do not assume `nav.yml` is harmless metadata in `split`; when valid, it strongly shapes what gets built.
- If you need appendices visible in a single-page sidebar, combine `[unnumbered]` with `[.appendix]`.

## Related Reference

The configuration reference remains in [README.md](../README.md). Use that file for option syntax and this guide for behavior and authoring expectations.
