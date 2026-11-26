# Compatibility

This page states precisely which Markdown Composdown understands, how it is verified, and where its
behavior differs from other renderers.

- [Specifications](#specifications)
- [Extensions](#extensions)
- [Rendering differences](#rendering-differences)
- [Platform support](#platform-support)
- [Versioning and API stability](#versioning-and-api-stability)

## Specifications

| Specification | Version | Coverage | Verification |
|---|---|---|---|
| [CommonMark](https://spec.commonmark.org/0.31.2/) | 0.31.2 | Complete | All 652 examples, on every platform |
| [GitHub Flavored Markdown](https://github.github.com/gfm/) | 0.29-gfm | Tables, task lists, strikethrough, extended autolinks | All extension examples of the specification |
| GitHub footnotes, as implemented by cmark-gfm | | Complete | cmark-gfm's extension tests |
| [GitHub alerts](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax#alerts) | | Complete | Unit tests |

Conformance is checked by rendering the parsed document to HTML with the public `toHtml()` renderer and comparing it with
each example's expected output after normalization, as the reference implementation's test runner does.
The examples are generated into common tests from the specification files in
[`composdown-core/spec`](../composdown-core/spec), so the suite runs on the JVM, Android and
Kotlin/Native.

## Extensions

| Extension | Constant | Default in parser | Default in `composdown-compose` |
|---|---|---|---|
| Tables | `TABLES` | On | On |
| Task lists | `TASK_LISTS` | On | On |
| Strikethrough | `STRIKETHROUGH` | On | On |
| Extended autolinks | `AUTOLINKS` | On | On |
| Footnotes | `FOOTNOTES` | On | On |
| Alerts | `ALERTS` | Off | On |
| Math | `MATH` | Off | Off |

`MarkdownExtension.GFM` is the set of the first five. `ComposdownDefaults.Extensions` adds `ALERTS`.

### Tables

GFM pipe tables with `:---`, `:---:` and `---:` alignment. Cells contain inline content only. A row
with fewer cells than the header is padded; extra cells are ignored.

### Task lists

`[ ]` and `[x]` (or `[X]`) at the start of a list item, followed by whitespace.

### Strikethrough

`~text~` and `~~text~~`. Runs of three or more tildes are not strikethrough.

### Extended autolinks

URLs starting with `www.`, `http://`, `https://` or `ftp://`, and email addresses (including
`mailto:` and `xmpp:` forms), recognized without angle brackets. Trailing punctuation and unbalanced
closing parentheses are excluded, as on GitHub. `www.` links are given an `http://` destination.

### Footnotes

`[^label]` references and `[^label]: text` definitions, which may contain multiple blocks. Footnotes
are numbered in order of first reference; unreferenced definitions are omitted; only the first
definition of a label is used.

### Alerts

A top-level block quote whose first line is exactly `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`
or `[!CAUTION]` (case-insensitive). Any other block quote is an ordinary quote.

### Math

| Syntax | Result |
|---|---|
| `$x$`, `\(x\)` | Inline math |
| `$$x$$`, `\[x\]` within text | Inline math displayed as a block-style formula (`isDisplay = true`) |
| A line of only `$$` or `\[`, up to a closing `$$` or `\]` line | Math block |
| `$$x$$` alone on a line | Math block |
| ```` ```math ```` code block | Math block |

To avoid misreading prices, `$...$` is math only when the opening `$` is not followed by whitespace,
the closing `$` is not preceded by whitespace, and the closing `$` is not followed by a digit. "It costs
$5 and $10" remains text. Escaped dollars (`\$`) and code spans are never math.

## Rendering differences

Composdown renders natively rather than producing HTML. Differences from a browser rendering of the
same Markdown:

| Construct | Composdown |
|---|---|
| Raw HTML blocks | Shown as source code; never interpreted |
| Inline HTML | Shown as literal text |
| Images inside running text | Shown as their description; images alone in a paragraph are drawn by the `ImageRenderer` |
| Math | TeX source in a monospace font, unless a `mathBlock` component is supplied |
| Link titles | Not shown (no hover tooltips) |
| Links with schemes other than `http`, `https`, `mailto`, `tel` | Not opened by default |

## Platform support

| Module | Android | iOS arm64 | iOS simulator arm64 | iOS x64 | Desktop JVM | wasmJs |
|---|---|---|---|---|---|---|
| `composdown-core` | Yes | Yes | Yes | Yes | Yes | Planned |
| `composdown-compose` | Yes | Yes | Yes | No | Yes | Planned |
| `composdown-coil` | Yes | Yes | Yes | No | Yes | Planned |
| `composdown-highlight` | Yes | Yes | Yes | No | Yes | Planned |

Compose Multiplatform no longer publishes `iosX64` artifacts, so the Compose-based modules cannot
target Intel-Mac simulators.

## Versioning and API stability

Composdown follows [Semantic Versioning](https://semver.org/). Before 1.0, minor versions may contain
breaking changes, always listed in the [changelog](../CHANGELOG.md). The public API of every module is
recorded in binary-compatibility dumps (JVM and Kotlin/Native), and any change to it is reviewed
explicitly.

Changes to the document tree produced for a given input (for example, fixing a parsing bug) are
treated as behavior changes and noted in the changelog.
