# Rendering

`composdown-compose` renders a `MarkdownDocument` with native Compose layouts and text. This guide
covers the rendering entry points and how each kind of content is presented.

- [Entry points](#entry-points)
- [Long documents and lists](#long-documents-and-lists)
- [Text selection](#text-selection)
- [Links](#links)
- [Images](#images)
- [Code](#code)
- [Math](#math)
- [Tables](#tables)
- [Footnotes](#footnotes)
- [Raw HTML](#raw-html)
- [Choosing extensions](#choosing-extensions)

## Entry points

| Composable | Input | Parsing |
|---|---|---|
| `Composdown(markdown: String, ...)` | Markdown text | On the composition thread, when the text or extensions change |
| `Composdown(document: MarkdownDocument, ...)` | A parsed document | None; you parse where you like |
| `Composdown(state: ComposdownState, ...)` | Text that changes over time | On a background dispatcher, incrementally ([Live content](streaming.md)) |
| `LazyListScope.composdownItems(document, keyPrefix, ...)` | A parsed document | None |
| `ComposdownBlock(block, modifier)` | One block | None; used inside custom components |

All entry points share these parameters:

| Parameter | Default | Purpose |
|---|---|---|
| `theme` | `ComposdownDefaults.theme()` | Colors, text styles and spacing ([Customization](customization.md)) |
| `components` | No replacements | Custom composables per block type |
| `imageRenderer` | `null`: images show their description | Loads and draws images |
| `codeHighlighter` | `null`: plain code | Syntax colors for code blocks |
| `onLinkClick` | Opens safe links with the platform handler | Handles link clicks |
| `selectable` | `true` | Text selection across blocks |

The `String` overload parses during composition. That is fast for typical content (a 73 KB document
parses in about 2 ms on a desktop JVM), but for very large documents, or when the same text is
rendered repeatedly, parse once in a view model and pass the document.

## Long documents and lists

`Composdown` lays out all blocks in a `Column`. For long documents, or a feed or chat that shows many
documents, use `composdownItems` so that only visible blocks are composed:

```kotlin
LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    item { Header() }
    for (message in messages) {
        composdownItems(message.document, keyPrefix = message.id)
    }
}
```

- Each top-level block is one item, with the key `"$keyPrefix:$index"` and a content type per block
  kind, so compositions are reused efficiently while scrolling. Footnotes form a final item.
- `keyPrefix` must be unique per document within the list.
- Items have no spacing of their own: use `verticalArrangement` or `itemModifier`.
- Text selection does not span lazy items. Wrap the `LazyColumn` in a `SelectionContainer` if needed.

## Text selection

With `selectable = true` (the default), all text of a `Composdown` can be selected as one range, across
paragraphs, lists, tables and code blocks. Set `selectable = false` when the content sits inside a
clickable container, or when you provide your own `SelectionContainer` around several documents.

## Links

Links are rendered with the theme's link color and style, are focusable, and are announced as links by
screen readers.

By default, a clicked link is opened with the platform's URI handler **only** if its scheme is `http`,
`https`, `mailto` or `tel`. Any other link (`javascript:`, `file:`, `intent:`, custom schemes and
relative URLs) is ignored. Supply `onLinkClick` to take full control:

```kotlin
Composdown(
    markdown = markdown,
    onLinkClick = { url ->
        when {
            url.startsWith("app://") -> navigator.navigate(url)
            url.startsWith("https://") -> browser.open(url)
            else -> Unit // ignore everything else
        }
    },
)
```

When `onLinkClick` is set, Composdown performs no filtering of its own; validate destinations before
acting on them.

Links to a heading of the same document, such as `[Installation](#installation)`, scroll that heading
into view instead of calling the handler, as on GitHub. Anchors are computed like GitHub's (see
[headings](parser.md#headings-and-anchors)). This works when the `Composdown` is inside a scrollable
container such as `verticalScroll`. In a lazy list (`composdownItems`), headings outside the visible
items are not composed, so anchor links go to `onLinkClick`; resolve them with `document.headings()`
and scroll the `LazyListState` to the heading's item.

## Images

Composdown has no image loader of its own, so the core library carries no networking dependency.
Without an `imageRenderer`, images are shown as their description in italics.

`composdown-coil` provides a renderer built on Coil 3:

```kotlin
val imageRenderer = remember { CoilImageRenderer() }
Composdown(markdown = markdown, imageRenderer = imageRenderer)
```

`CoilImageRenderer`:

- reserves space at `placeholderAspectRatio` (16:9 by default) while an image loads, then draws it at
  its own aspect ratio, never wider than its natural size;
- remembers loaded sizes by URL, so an image does not resize again when it scrolls back into view (share
  one instance across documents);
- loads only URLs accepted by `canLoad`, which by default allows HTTPS only, because loading a remote
  image reveals the reader's IP address to the image's server;
- shows the description when an image is not allowed or fails to load, and exposes it as the content
  description.

An image that is alone in its paragraph is drawn as a block with the renderer. An image inside running
text is shown as its description. To use another loader, implement `ImageRenderer`
([Customization](customization.md#image-renderers)).

## Code

Code blocks are drawn on a tinted surface in a monospace font and scroll horizontally, so long lines
are never wrapped or cut off. Code spans inside text use a slightly smaller monospace font on a subtle
background.

For syntax colors, pass a `CodeHighlighter`. `composdown-highlight` provides one based on the pure
Kotlin [Highlights](https://github.com/SnipMeDev/Highlights) library:

```kotlin
val highlighter = remember { HighlightsCodeHighlighter() }
Composdown(markdown = markdown, codeHighlighter = highlighter)
```

The language is taken from the first word of the code block's info string (```` ```kotlin ````), and
common aliases such as `kt`, `js`, `ts`, `py`, `sh` and `c++` are recognized. Code in unsupported or
unspecified languages is shown without colors. Colors are chosen for the luminance of the code block
background, so they stay legible in light and dark themes.

## Math

With `MarkdownExtension.MATH` enabled, inline math (`$...$`, `\(...\)`) and display math (`$$...$$`,
`\[...\]`, ```` ```math ````) are recognized. By default they are shown as TeX source in a monospace
font, which keeps every symbol legible. To typeset formulas, replace the `mathBlock` component with a
renderer of your choice. Math is off by default, because ordinary prose often contains dollar signs.

```kotlin
Composdown(
    markdown = markdown,
    extensions = ComposdownDefaults.Extensions + MarkdownExtension.MATH,
)
```

## Tables

Tables size each column to its content, honor column alignment, and scroll horizontally when they are
wider than the screen. Header cells use the header style and background. Tables expose collection
semantics (row and column counts and positions) to assistive technologies.

## Footnotes

Footnote references are rendered as superscript numbers, numbered in order of first reference. The
definitions are collected into a section at the end of the document, below a divider, regardless of
where they appear in the source.

## Raw HTML

Composdown never interprets HTML. HTML blocks are shown as source code, and inline HTML as literal
text, so untrusted content cannot inject markup, scripts or tracking pixels. To render specific HTML
yourself, replace the `htmlBlock` component.

## Choosing extensions

The `String` overload and `ComposdownState` take an `extensions` set. The default,
`ComposdownDefaults.Extensions`, is GitHub Flavored Markdown plus alerts. Typical alternatives:

```kotlin
// Strict CommonMark, nothing else
extensions = emptySet()

// GFM, alerts and math (for scientific or LLM content)
extensions = ComposdownDefaults.Extensions + MarkdownExtension.MATH

// GFM without footnotes
extensions = ComposdownDefaults.Extensions - MarkdownExtension.FOOTNOTES
```

See [Compatibility](compatibility.md) for what each extension recognizes.
