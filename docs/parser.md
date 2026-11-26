# Parser and document model

`composdown-core` is a Markdown parser written in pure Kotlin. It has no UI dependencies and runs on
every Kotlin Multiplatform target the project supports, so it is equally suited to rendering with
another toolkit, to indexing and search, to extracting text for previews or notifications, and to
validating content on a server.

- [Parsing](#parsing)
- [The document model](#the-document-model)
- [Walking a document](#walking-a-document)
- [Extracting text](#extracting-text)
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

The model is a sealed hierarchy, so `when` expressions are checked for exhaustiveness:

```kotlin
fun Block.links(): List<String> = when (this) {
    is Block.Paragraph -> content.links()
    is Block.Heading -> content.links()
    is Block.BlockQuote -> children.flatMap { it.links() }
    is Block.Alert -> children.flatMap { it.links() }
    is Block.ListBlock -> items.flatMap { item -> item.children.flatMap { it.links() } }
    is Block.Table -> (listOf(header) + rows).flatMap { row -> row.cells.flatMap { it.content.links() } }
    is Block.CodeBlock, is Block.HtmlBlock, is Block.MathBlock, Block.ThematicBreak -> emptyList()
}

fun List<Inline>.links(): List<String> = flatMap { inline ->
    when (inline) {
        is Inline.Link -> listOf(inline.destination) + inline.children.links()
        is Inline.Emphasis -> inline.children.links()
        is Inline.Strong -> inline.children.links()
        is Inline.Strikethrough -> inline.children.links()
        else -> emptyList()
    }
}
```

## Extracting text

`Iterable<Inline>.plainText()` returns the visible text of inline content, without markup: useful for
titles, previews, search indexing and accessibility descriptions.

```kotlin
val title = document.blocks
    .filterIsInstance<Block.Heading>()
    .firstOrNull { it.level == 1 }
    ?.content
    ?.plainText()
```

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
