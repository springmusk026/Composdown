package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.springmusk026.composdown.compose.ComposdownBlock
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline

private val CodeBlockShape = RoundedCornerShape(8.dp)

/** Renders inline content as text, with links opened through the environment's handler. */
@Composable
internal fun InlineContent(
    inlines: List<Inline>,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val environment = currentEnvironment()
    val uriHandler = LocalUriHandler.current
    val onLinkClick = environment.onLinkClick
    val targets = LocalNavigationTargets.current
    val scope = rememberCoroutineScope()
    val text =
        remember(inlines, environment.theme, onLinkClick, uriHandler, targets, scope) {
            val openExternal: (String) -> Unit = onLinkClick ?: { url -> if (isSafeLink(url)) uriHandler.openUri(url) }
            // Links to a heading of the document scroll to it; any other link goes to the handler.
            val openLink: (String) -> Unit = { url ->
                if (targets?.scrollToHeading(url, scope) !=
                    true
                ) {
                    openExternal(url)
                }
            }
            val openFootnote: ((String) -> Unit)? = targets?.let { { label -> it.scrollToFootnote(label, scope) } }
            buildInlineText(inlines, environment.theme, openLink, openFootnote)
        }
    Text(
        text = text,
        // The text fills the block, so that it is aligned to the start of its own direction: a
        // right-to-left paragraph inside a left-to-right layout ends at the right edge.
        modifier = modifier.fillMaxWidth().then(rememberReferenceTarget(inlines)),
        style = style.merge(color = environment.theme.colors.text),
    )
}

@Composable
internal fun DefaultParagraph(
    block: Block.Paragraph,
    modifier: Modifier = Modifier,
) {
    val environment = currentEnvironment()
    val image = block.standaloneImage()
    val renderer = environment.imageRenderer
    if (image != null && renderer != null) {
        renderer.Image(image, modifier)
    } else {
        InlineContent(block.content, environment.theme.typography.paragraph, modifier)
    }
}

@Composable
internal fun DefaultHeading(
    block: Block.Heading,
    modifier: Modifier = Modifier,
) {
    val typography = currentEnvironment().theme.typography
    InlineContent(block.content, typography.heading(block.level), modifier.semantics { heading() })
}

@Composable
internal fun DefaultCodeBlock(
    block: Block.CodeBlock,
    modifier: Modifier = Modifier,
) {
    val environment = currentEnvironment()
    val code = block.literal.removeSuffix("\n")
    val language = block.language
    val highlighter = environment.codeHighlighter
    val darkTheme =
        environment.theme.colors.codeBlockBackground
            .luminance() < DARK_LUMINANCE
    val highlighted =
        remember(code, language, highlighter, darkTheme) {
            if (highlighter != null && language != null) highlighter.highlight(code, language, darkTheme) else null
        }
    CodeText(highlighted ?: AnnotatedString(code), modifier)
}

/** Backgrounds darker than this count as dark when choosing syntax colors. */
private const val DARK_LUMINANCE = 0.5f

/** TeX source of a math block, centered and in the math style. */
@Composable
internal fun DefaultMathBlock(
    block: Block.MathBlock,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    // Formulas read left to right regardless of the surrounding text.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = block.literal,
                style = theme.typography.mathBlock.merge(color = theme.colors.text),
                softWrap = false,
            )
        }
    }
}

/** Monospace text on the code background, scrolling horizontally instead of wrapping. */
@Composable
private fun CodeText(
    code: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    // Code reads left to right regardless of the surrounding text.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(CodeBlockShape)
                    .background(theme.colors.codeBlockBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(theme.spacing.codeBlockPadding),
        ) {
            Text(
                text = code,
                style = theme.typography.codeBlock.merge(color = theme.colors.code, textDirection = TextDirection.Ltr),
                softWrap = false,
            )
        }
    }
}

@Composable
internal fun DefaultBlockQuote(
    block: Block.BlockQuote,
    modifier: Modifier = Modifier,
) {
    val environment = currentEnvironment()
    val quoted = remember(environment) { environment.withTextColor(environment.theme.colors.blockQuoteText) }
    QuoteLayout(barColor = environment.theme.colors.blockQuoteBar, modifier = modifier) {
        CompositionLocalProvider(LocalComposdownEnvironment provides quoted) {
            BlockColumn(block.children)
        }
    }
}

@Composable
internal fun DefaultAlert(
    block: Block.Alert,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    val color = theme.colors.alerts[block.type] ?: theme.colors.blockQuoteBar
    QuoteLayout(barColor = color, modifier = modifier) {
        // The title belongs to the content below it, so it sits closer than separate blocks.
        Column(verticalArrangement = Arrangement.spacedBy(theme.spacing.tightListItem)) {
            val title = theme.strings.alertTitles[block.type] ?: block.type.name
            Text(text = title, style = theme.typography.alertTitle.merge(color = color))
            BlockColumn(block.children)
        }
    }
}

/** A colored bar along the start edge of [content], as used by block quotes and alerts. */
@Composable
private fun QuoteLayout(
    barColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = currentEnvironment().theme.spacing
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(spacing.blockQuoteBarWidth).fillMaxHeight().background(barColor))
        Spacer(modifier = Modifier.width(spacing.blockQuoteIndent))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
internal fun DefaultList(
    block: Block.ListBlock,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    val depth = LocalListDepth.current
    val itemSpacing = if (block.isTight) theme.spacing.tightListItem else theme.spacing.block
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
        block.items.forEachIndexed { index, item ->
            Row {
                ListMarker(
                    kind = block.kind,
                    index = index,
                    depth = depth,
                    taskState = item.taskState,
                    style = theme.typography.paragraph,
                    color = theme.colors.listMarker,
                    accent = theme.colors.link,
                    width = theme.spacing.listMarkerWidth,
                )
                Spacer(modifier = Modifier.width(theme.spacing.tightListItem * 2))
                CompositionLocalProvider(LocalListDepth provides depth + 1) {
                    BlockColumn(item.children, spacing = itemSpacing, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun DefaultThematicBreak(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = currentEnvironment().theme.colors.divider)
}

/** Raw HTML is never interpreted: its source is shown like code. */
@Composable
internal fun DefaultHtmlBlock(
    block: Block.HtmlBlock,
    modifier: Modifier = Modifier,
) {
    CodeText(AnnotatedString(block.literal), modifier)
}

/** Renders blocks one under another with the theme's block spacing. */
@Composable
internal fun BlockColumn(
    blocks: List<Block>,
    modifier: Modifier = Modifier,
    spacing: Dp = currentEnvironment().theme.spacing.block,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        blocks.forEach { ComposdownBlock(it) }
    }
}
