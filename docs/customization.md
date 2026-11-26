# Customization

Composdown is styled through three layers, from least to most effort:

1. **Theme**: colors, text styles and spacing, derived from Material 3 by default.
2. **Components**: replace the composable for any kind of block.
3. **Plug-ins**: image loading and syntax highlighting.

- [Theme](#theme)
- [Components](#components)
- [Image renderers](#image-renderers)
- [Code highlighters](#code-highlighters)
- [Sharing a configuration across an app](#sharing-a-configuration-across-an-app)

## Theme

`ComposdownDefaults.theme()` builds a `ComposdownTheme` from the current `MaterialTheme`. Each part can
be replaced, and each part's builder takes the individual values as parameters with Material defaults,
so you override only what differs:

```kotlin
val theme = ComposdownDefaults.theme(
    colors = ComposdownDefaults.colors(
        link = BrandColors.Link,
        codeBlockBackground = BrandColors.SurfaceSunken,
    ),
    typography = ComposdownDefaults.typography(
        paragraph = BrandType.Body,
        codeBlock = BrandType.Code,
    ),
    spacing = ComposdownDefaults.spacing(block = 16.dp, codeBlockPadding = 16.dp),
)

Composdown(markdown = markdown, theme = theme)
```

### Colors (`ComposdownDefaults.colors`)

| Value | Material default | Used for |
|---|---|---|
| `text` | `onSurface` | Body text |
| `link` | `primary` | Link text |
| `code` | `onSurfaceVariant` | Code spans and blocks |
| `codeBlockBackground` | `surfaceContainerHighest` | Code blocks |
| `inlineCodeBackground` | `surfaceContainerHigh` | Code spans |
| `blockQuoteBar` | `outlineVariant` | Bar beside block quotes |
| `blockQuoteText` | `onSurfaceVariant` | Text inside block quotes |
| `divider` | `outlineVariant` | Thematic breaks, table borders, footnote separator |
| `tableHeaderBackground` | `surfaceContainer` | Table header row |
| `listMarker` | `onSurfaceVariant` | Bullets, numbers, checkboxes |
| `alerts` | `primary`, `tertiary`, `secondary`, amber, `error` | Accent of NOTE, TIP, IMPORTANT, WARNING, CAUTION |

### Typography (`ComposdownDefaults.typography`)

| Value | Material default |
|---|---|
| `paragraph` | `bodyLarge` |
| `headings` (6 styles) | `headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`, `titleSmall`, `labelLarge`, all semibold |
| `codeBlock` | `bodyMedium`, monospace |
| `inlineCode` | Monospace at 0.9 em |
| `link` | Underlined |
| `tableHeader`, `tableCell` | `bodyMedium` (header semibold) |
| `footnote` | `bodySmall` |
| `alertTitle` | `labelLarge`, semibold |
| `math`, `mathBlock` | Monospace (TeX source) |

Text styles for prose use `TextDirection.Content`, so right-to-left paragraphs align correctly. Keep
that when you supply your own styles: `myStyle.copy(textDirection = TextDirection.Content)`.

### Strings (`ComposdownDefaults.strings`)

Text that Composdown adds to documents, in English by default:

```kotlin
val theme = ComposdownDefaults.theme(
    strings = ComposdownDefaults.strings(
        alertTitles = mapOf(
            AlertType.NOTE to "Hinweis",
            AlertType.TIP to "Tipp",
            AlertType.IMPORTANT to "Wichtig",
            AlertType.WARNING to "Warnung",
            AlertType.CAUTION to "Vorsicht",
        ),
        backToReference = "Zurück zum Verweis",
    ),
)
```

| Value | Default | Used for |
|---|---|---|
| `alertTitles` | Note, Tip, Important, Warning, Caution | The title of each kind of alert |
| `backToReference` | Back to reference | The accessibility label of a footnote's back link, followed by its number |

### Spacing (`ComposdownDefaults.spacing`)

| Value | Default |
|---|---|
| `block` (between blocks) | 12 dp |
| `tightListItem` | 4 dp |
| `listMarkerWidth` | 24 dp |
| `codeBlockPadding` | 12 dp |
| `blockQuoteIndent` | 12 dp |
| `blockQuoteBarWidth` | 4 dp |
| `tableCellPadding` | 8 dp |

Theme classes compare by value. A theme built in composition with the same values is equal to the
previous one, so it does not cause blocks to recompose.

## Components

`ComposdownComponents` holds an optional replacement for each kind of block: `paragraph`, `heading`,
`codeBlock`, `blockQuote`, `alert`, `list`, `table`, `thematicBreak`, `htmlBlock` and `mathBlock`. A
replacement is a `BlockComponent<T>`, a functional interface receiving the block and the modifier to
apply to its outermost layout.

A code block with a copy button:

```kotlin
val components = ComposdownComponents(
    codeBlock = BlockComponent { block, modifier ->
        val clipboard = LocalClipboardManager.current
        Column(modifier) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(block.language.orEmpty(), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { clipboard.setText(AnnotatedString(block.literal)) }) {
                    Text("Copy")
                }
            }
            Text(block.literal, fontFamily = FontFamily.Monospace)
        }
    },
)
```

Container blocks (block quotes, alerts, list items) contain other blocks. Render those with
`ComposdownBlock(child)`, which applies the current theme, components and plug-ins:

```kotlin
val components = ComposdownComponents(
    alert = BlockComponent { alert, modifier ->
        Card(modifier) {
            Column(Modifier.padding(12.dp)) {
                Text(alert.type.name, style = MaterialTheme.typography.labelLarge)
                alert.children.forEach { child -> ComposdownBlock(child) }
            }
        }
    },
)
```

Create `ComposdownComponents` once (at the top level, or with `remember`) rather than on every
composition. Like the theme, it compares by value; a lambda created in composition is a new value each
time and would cause recomposition.

## Image renderers

Implement `ImageRenderer` to use any image loader:

```kotlin
class MyImageRenderer(private val loader: MyLoader) : ImageRenderer {
    @Composable
    override fun Image(image: Inline.Image, modifier: Modifier) {
        val description = image.children.plainText()
        MyAsyncImage(
            url = image.destination,
            contentDescription = description,
            // Reserve a stable size while loading, so content below does not move.
            modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
    }
}
```

Guidelines for implementations:

- Set the content description from `image.children.plainText()`.
- Reserve the image's space before it loads, to avoid layout jumps.
- Decide which URLs may be loaded. Markdown from untrusted sources can reference any address.

## Code highlighters

Implement `CodeHighlighter` to color code with your own engine:

```kotlin
object MyHighlighter : CodeHighlighter {
    override fun highlight(code: String, language: String, darkTheme: Boolean): AnnotatedString? {
        val grammar = grammars[language.lowercase()] ?: return null
        return grammar.colorize(code, if (darkTheme) DarkPalette else LightPalette)
    }
}
```

Return `null` for unsupported languages, and the code is shown without colors. `highlight` runs during
composition whenever a code block's text changes, including on every update of a streamed code block,
so it should be fast; results are remembered per text, language and theme.

## Sharing a configuration across an app

Wrap the configuration in your own composable, so every screen renders Markdown the same way:

```kotlin
private val AppComponents = ComposdownComponents(codeBlock = CopyableCodeBlock)

@Composable
fun AppMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val imageRenderer = remember { CoilImageRenderer() }
    val highlighter = remember { HighlightsCodeHighlighter() }
    val navigator = LocalNavigator.current
    Composdown(
        markdown = markdown,
        modifier = modifier,
        theme = ComposdownDefaults.theme(spacing = ComposdownDefaults.spacing(block = 16.dp)),
        components = AppComponents,
        imageRenderer = imageRenderer,
        codeHighlighter = highlighter,
        onLinkClick = navigator::openLink,
    )
}
```
