# Architecture

This document explains how Composdown is built, for contributors and for users who want to understand
its behavior and performance in depth.

- [Modules](#modules)
- [Parsing pipeline](#parsing-pipeline)
- [Incremental parsing](#incremental-parsing)
- [Rendering](#rendering)
- [Verification strategy](#verification-strategy)
- [Design decisions](#design-decisions)

## Modules

```
composdown-highlight ──┐
composdown-coil ───────┼──> composdown-compose ──> composdown-core
                       │         (Compose, Material 3)    (Kotlin stdlib,
  application ─────────┘                                   kotlinx-collections-immutable)
```

| Module | Packages | Responsibility |
|---|---|---|
| `composdown-core` | `core`, `core.ast`, `core.internal.*` | Parsing, the document model, incremental updates |
| `composdown-compose` | `compose`, `compose.internal` | Layout and text rendering, theme, state holder |
| `composdown-coil` | `coil` | `ImageRenderer` backed by Coil 3 |
| `composdown-highlight` | `highlight` | `CodeHighlighter` backed by Highlights |
| `benchmarks` | | JMH benchmarks of the parser (not published) |
| `sample/*` | | Demonstration app for Android, iOS and Desktop (not published) |

Optional capabilities with heavy dependencies (networking, grammars) live in their own modules behind
small interfaces, so the core library stays small and applications choose their own stack.

## Parsing pipeline

Parsing follows the two-phase strategy of the CommonMark reference implementation:

1. **Block phase** (`internal/block`). Lines are consumed one at a time. Each line is matched against
   the open container blocks (block quotes, list items, footnote definitions), then may close them,
   start new blocks, or add text to the innermost open leaf (paragraph, code block, table, math
   block). Link reference definitions are extracted from paragraphs as they close.
2. **Inline phase** (`internal/inline`). The text of each leaf is parsed into inline nodes: code spans,
   autolinks, HTML, entities and escapes, then links and emphasis through the delimiter-stack algorithm
   of the specification.
3. **Tree building** (`AstBuilder`, `InlineConverter`). Mutable parser nodes are converted into the
   immutable public model. Alerts are recognized, footnotes are numbered in order of first reference,
   and unreferenced definitions are dropped.

Supporting data is generated at build time rather than written by hand: the HTML5 entity table
(from `data/html-entities.json`, by a Gradle task in `build-logic`) and Unicode character classes for
punctuation and whitespace (by `scripts/generate_unicode_tables.py`).

### Linear-time guarantees

Markdown has well-known inputs that make naive parsers quadratic or exponential. Composdown bounds them:

- Delimiter processing uses the specification's "openers bottom" optimization, per delimiter kind and
  length class.
- Scans for link destinations, link labels and HTML constructs that can fail are memoized, so a failed
  scan is not repeated from every position.
- Autolink detection never rewinds more than a fixed number of characters.
- Nesting deeper than 100 levels is flattened, so recursion depth is bounded.

`PathologicalInputTest` checks these cases with inputs large enough that quadratic behavior would time
out.

## Incremental parsing

`IncrementalEngine` implements `IncrementalMarkdownParser`, and `MarkdownParser.parse` is the same engine
run once to completion, so the two cannot diverge.

```
 text:  ┌──────────── committed ────────────┬──────── tail ────────┐
        │ # Title                           │ - item one           │
        │                                   │ - item tw            │
        │ First paragraph.                  │                      │
        └───────────────────────────────────┴──────────────────────┘
 blocks: [Heading] [Paragraph]  (reused)       [ListBlock] (re-parsed)
```

1. **Committed prefix.** A live block parser is fed only complete lines. When a top-level block
   closes, it is committed: converted once, and its `Block` instance reused in every later document.
2. **Tail.** On each update, the text from the start of the still-open top-level block to the end is
   parsed again, with registries of link references and footnotes that overlay the committed ones.
   The tail is usually small (one paragraph, list or code block), so update cost is independent of
   document length.
3. **Retroactive resolution.** While converting committed blocks, the engine records which link and
   footnote labels failed to resolve. When a later definition provides one of those labels, only the
   affected committed blocks are converted again. Footnote numbers are recomputed by first reference.
4. **Optimistic presentation.** Under `StreamingPolicy.OPTIMISTIC`, the tail's inline conversion
   closes unclosed emphasis and code spans, shows unfinished links as links, and holds back trailing
   text that would otherwise flicker (delimiter runs, partial block markers, possible table headers).
   `finish()` discards all of this and produces the exact document.
5. **Edits.** `setText` with text that extends the current text is an append. Any other change restarts
   the live parse, and blocks equal to previous ones keep their previous instances.

The engine's correctness criterion is simple and checked exhaustively: for every input and every way of
splitting it, the finished document equals the one-shot parse.

## Rendering

`composdown-compose` maps the document model onto Compose:

- **Configuration** (theme, components, image renderer, highlighter, link handler) is provided once
  through a composition local, so nested blocks do not pass it down explicitly. Theme and component
  classes have value equality, so an equal configuration built during recomposition does not invalidate
  anything.
- **Blocks** are composed in a `Column` (or as lazy items) keyed by position. Each block's composable
  receives an immutable, stable block; unchanged instances from the incremental parser are skipped by
  Compose without comparing their contents.
- **Inline content** of a block becomes a single `AnnotatedString` with span styles and
  `LinkAnnotation`s, laid out by one `Text`. This keeps selection, line breaking and accessibility
  native, and keeps the number of layout nodes proportional to the number of blocks, not of inline
  elements.
- **Lists, quotes and alerts** are drawn with lightweight layouts; bullets and checkboxes are drawn on a
  canvas, centered on the item's first line.
- **Tables** measure each column's intrinsic width so cells align across rows, and scroll horizontally
  when wider than the available space.
- **Direction** is determined per block from its first strongly directional character.
- **`ComposdownState`** sends updates to a single parsing coroutine through a channel; updates queued
  while a parse runs are combined into one. Each parsed document is published in a snapshot, so
  Compose observes it like any other state.

## Verification strategy

| Layer | Technique |
|---|---|
| Parser conformance | CommonMark and GFM example suites, generated into common tests and run on all platforms |
| Incremental engine | Equivalence against one-shot parsing: per character, random chunks, random edits, generated documents |
| Robustness | Pathological inputs with time limits; deep nesting |
| Rendering | Compose UI tests of semantics and behavior; screenshot tests in light and dark themes with bundled fonts for determinism |
| API | Binary-compatibility dumps for JVM and Kotlin/Native |
| Performance | JMH benchmarks with recorded baselines |

## Design decisions

**A purpose-built parser.** General-purpose Markdown parsers re-parse whole documents and produce
mutable trees built for conversion to HTML. Owning the parser lets the incremental engine
reuse its internals, and the specification suites make that ownership safe.

**An immutable public model with internal constructors.** Users can read and compare every node, while
the parser remains free to evolve its internals. Structural equality makes documents usable as Compose
inputs and test expectations.

**Native rendering, no WebView.** Text participates in Compose layout, theming, selection and
accessibility, starts instantly, and behaves identically on every platform.

**Safe by default.** HTML is never interpreted, links are filtered by scheme unless the application
handles them, and remote images are limited to HTTPS. Applications can relax any of these explicitly.

**Small extension points instead of options.** Rather than a growing list of flags, rendering is
customized by replacing a block's composable or plugging in an interface. The defaults are ordinary
implementations of the same extension points.
