# Parser and document model

`composdown-core` is a Markdown parser written in pure Kotlin. It has no UI dependencies and runs on
every Kotlin Multiplatform target the project supports, so it is equally suited to rendering with
another toolkit, to indexing and search, to extracting text for previews or notifications, and to
validating content on a server.

- [Parsing](#parsing)
- [The document model](#the-document-model)
- [Walking a document](#walking-a-document)
- [Headings and anchors](#headings-and-anchors)
- [Extracting text](#extracting-text)
- [Rendering HTML](#rendering-html)
- [Incremental parsing](#incremental-parsing)
- [Threading](#threading)
- [Robustness](#robustness)

## Parsing

```kotlin
val parser = MarkdownParser() // GitHub Flavored Markdown
val document: MarkdownDocument = parser.parse(markdown)

val commonMark = MarkdownParser(extensions = emptySet())
val everything = MarkdownParser(MarkdownExtension.entries.toSet())
```

`MarkdownExtension` values: `TABLES`, `TASK_LISTS`, `STRIKETHROUGH`, `AUTOLINKS`, `FOOTNOTES`
(together `MarkdownExtension.GFM`), plus the opt-in `ALERTS` and `MATH`.

## The document model

A `MarkdownDocument` has `blocks` and `footnotes`. All node types are immutable, use
`ImmutableList` for children, and implement structural `equals` and `hashCode`, so documents can be
compared, used as keys, and passed to Compose as stable values. Nodes are created only by the parser.

**Blocks** (`Block`):

| Type | Properties |
|---|---|
| `Paragraph` | `content: List<Inline>` |
| `Heading` | `level` (1-6), `content` |
| `ThematicBreak` | |
| `BlockQuote` | `children: List<Block>` |
| `ListBlock` | `kind` (`ListKind.Bullet(marker)` or `ListKind.Ordered(startNumber, delimiter)`), `isTight`, `items: List<ListItem>` |
| `CodeBlock` | `literal`, `info`, `language`, `isFenced` |
| `Table` | `columns: List<TableAlignment>`, `header: TableRow`, `rows` |
| `Alert` | `type: AlertType`, `children` |
| `MathBlock` | `literal` (TeX) |
| `HtmlBlock` | `literal` |

`ListItem` has `children` and `taskState` (`CHECKED`, `UNCHECKED`, or `null` for ordinary items).
`TableRow` has `cells`, each with `content`.

**Inlines** (`Inline`):

| Type | Properties |
|---|---|
| `Text` | `literal` (entities and escapes already resolved) |
| `Code` | `literal` |
| `Emphasis`, `Strong`, `Strikethrough` | `children` |
| `Link` | `destination`, `title`, `children` |
| `Image` | `destination`, `title`, `children` (the description) |
| `FootnoteReference` | `label`, `number` |
| `Math` | `literal`, `isDisplay` |
| `HtmlInline` | `literal` |
| `SoftBreak`, `HardBreak` | |

Reference links are resolved during parsing: a `Link` always carries its final destination, whether
written inline or by reference.

**Footnotes** (`FootnoteDefinition`) have `label`, `number` and `children`. Only referenced definitions
are included, numbered in order of first reference, like GitHub.

## Walking a document

`allBlocks()` and `allInlines()` return every node of a document as a lazy sequence, in document order
and depth first, including the content of quotes, lists, table cells and footnotes:

```kotlin
val links = document.allInlines().filterIsInstance<Inline.Link>().map { it.destination }.toList()
val languages = document.allBlocks().filterIsInstance<Block.CodeBlock>().mapNotNull { it.language }.toSet()
val hasTables = document.allBlocks().any { it is Block.Table }
```

For transformations that need the tree structure, the model is a sealed hierarchy, so `when`
expressions over `Block` and `Inline` are checked for exhaustiveness.

## Headings and anchors

`headings()` returns every heading with its level, plain text and an anchor computed exactly as GitHub
computes heading ids (lower case, punctuation removed, spaces to hyphens, `-1`, `-2` for repeats):

```kotlin
val toc = document.headings().joinToString("\n") { heading ->
    "  ".repeat(heading.level - 1) + "- [${heading.text}](#${heading.anchor})"
}
```

The same anchors are used by `toHtml(HtmlOptions(headingIds = true))` and by the Compose renderer, where
a link such as `[Installation](#installation)` scrolls to its heading.

## Extracting text

`MarkdownDocument.plainText()` returns the text of a whole document without markup, for previews,
notifications, search indexing or copying. Blocks are separated by blank lines, list items and table
rows by line breaks, and table cells by tabs.

`Iterable<Inline>.plainText()` does the same for inline content, such as a heading or a link's text:

```kotlin
val title = document.headings().firstOrNull { it.level == 1 }?.text
val preview = document.plainText().take(200)
```

## Rendering HTML

`toHtml()` renders a document as an HTML fragment, for example for a web view, an email, or
server-side rendering. The markup follows the CommonMark reference implementations and, for extensions,
GitHub (task list checkboxes, footnote sections, alert `div`s); math is written as TeX source in
`math inline` and `math display` elements for a client-side renderer such as KaTeX.

```kotlin
val html = document.toHtml()
val forDocsSite = document.toHtml(HtmlOptions(headingIds = true, softBreak = "<br />"))
```

| Option | Default | Effect |
|---|---|---|
| `rawHtml` | `RawHtml.ESCAPE` | Raw HTML in the Markdown is escaped (`ESCAPE`), left out (`OMIT`), or passed through (`ALLOW`) |
| `safeLinks` | `true` | Removes `javascript:`, `vbscript:`, `file:` and non-image `data:` destinations |
| `headingIds` | `false` | Adds GitHub-compatible `id` attributes to headings |
| `headingIdPrefix` | `""` | Text before each heading id, such as `"user-content-"` as GitHub uses. Set it when embedding untrusted Markdown in a page, so that headings cannot take over the page's own ids (DOM clobbering) |
| `softBreak` | `"\n"` | The HTML written for a soft line break |

The defaults are safe for untrusted Markdown. `HtmlOptions.CommonMark` reproduces the reference
implementations exactly, including raw HTML, and is what the specification test suites run with: the
public renderer is verified against all 702 specification examples. Use it only for trusted input.

## Incremental parsing

For text that arrives in pieces, `IncrementalMarkdownParser` re-parses only the end of the document on
each update. See [Live content](streaming.md#without-compose).

## Threading

`MarkdownParser` holds no state between calls: one instance may be shared and called concurrently from
any number of threads. `IncrementalMarkdownParser` holds the text being built and must be used from one
thread at a time.

## Robustness

The parser is designed for untrusted input:

- It never throws on any input; every string is valid Markdown.
- It runs in linear time on known pathological inputs (long runs of delimiters, deeply nested brackets,
  many unclosed constructs), tested against the cases of the reference implementation's suite.
- Block quotes, lists and inline elements nested more than 100 levels deep are flattened rather than
  exhausting the stack.
- Character references are decoded against the complete HTML5 entity table; invalid and null code
  points are replaced with U+FFFD, as the specification requires.
