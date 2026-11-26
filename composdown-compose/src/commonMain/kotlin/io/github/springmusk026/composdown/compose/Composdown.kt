package io.github.springmusk026.composdown.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import io.github.springmusk026.composdown.compose.internal.ComposdownEnvironment
import io.github.springmusk026.composdown.compose.internal.DefaultAlert
import io.github.springmusk026.composdown.compose.internal.DefaultBlockQuote
import io.github.springmusk026.composdown.compose.internal.DefaultCodeBlock
import io.github.springmusk026.composdown.compose.internal.DefaultHeading
import io.github.springmusk026.composdown.compose.internal.DefaultHtmlBlock
import io.github.springmusk026.composdown.compose.internal.DefaultList
import io.github.springmusk026.composdown.compose.internal.DefaultMathBlock
import io.github.springmusk026.composdown.compose.internal.DefaultParagraph
import io.github.springmusk026.composdown.compose.internal.DefaultTable
import io.github.springmusk026.composdown.compose.internal.DefaultThematicBreak
import io.github.springmusk026.composdown.compose.internal.Footnotes
import io.github.springmusk026.composdown.compose.internal.LocalComposdownEnvironment
import io.github.springmusk026.composdown.compose.internal.contentDirection
import io.github.springmusk026.composdown.compose.internal.currentEnvironment
import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.MarkdownDocument

/**
 * Renders static Markdown [markdown]. The text is parsed when it changes; for text that grows over
 * time, such as a streamed response, use the overload taking a [ComposdownState] instead.
 *
 * @param extensions the syntax extensions to recognize. Defaults to GitHub Flavored Markdown with alerts.
 * @param theme colors, text styles and spacing.
 * @param components replacements for how specific kinds of blocks are drawn.
 * @param imageRenderer loads and draws images, or `null` to show images as their description.
 * @param codeHighlighter adds syntax colors to code blocks, or `null` for plain code.
 * @param onLinkClick called with the destination of a clicked link. If `null`, web, email and phone
 *   links are opened with the platform's URI handler and other links are ignored.
 * @param selectable whether the text can be selected (across blocks).
 */
@Composable
public fun Composdown(
    markdown: String,
    modifier: Modifier = Modifier,
    extensions: Set<MarkdownExtension> = ComposdownDefaults.Extensions,
    theme: ComposdownTheme = ComposdownDefaults.theme(),
    components: ComposdownComponents = ComposdownDefaults.Components,
    imageRenderer: ImageRenderer? = null,
    codeHighlighter: CodeHighlighter? = null,
    onLinkClick: ((String) -> Unit)? = null,
    selectable: Boolean = true,
) {
    val document = remember(markdown, extensions) { MarkdownParser(extensions).parse(markdown) }
    Composdown(document, modifier, theme, components, imageRenderer, codeHighlighter, onLinkClick, selectable)
}

/**
 * Renders the Markdown held by [state], which can grow while it is shown (see [ComposdownState]).
 * Completed blocks are not recomposed as text is appended.
 *
 * See the overload taking a `String` for the other parameters.
 */
@Composable
public fun Composdown(
    state: ComposdownState,
    modifier: Modifier = Modifier,
    theme: ComposdownTheme = ComposdownDefaults.theme(),
    components: ComposdownComponents = ComposdownDefaults.Components,
    imageRenderer: ImageRenderer? = null,
    codeHighlighter: CodeHighlighter? = null,
    onLinkClick: ((String) -> Unit)? = null,
    selectable: Boolean = true,
) {
    Composdown(state.document, modifier, theme, components, imageRenderer, codeHighlighter, onLinkClick, selectable)
}

/**
 * Renders a parsed [document]: its blocks one under another, followed by its footnotes.
 *
 * See the overload taking a `String` for the other parameters.
 */
@Composable
public fun Composdown(
    document: MarkdownDocument,
    modifier: Modifier = Modifier,
    theme: ComposdownTheme = ComposdownDefaults.theme(),
    components: ComposdownComponents = ComposdownDefaults.Components,
    imageRenderer: ImageRenderer? = null,
    codeHighlighter: CodeHighlighter? = null,
    onLinkClick: ((String) -> Unit)? = null,
    selectable: Boolean = true,
) {
    ProvideComposdown(theme, components, imageRenderer, codeHighlighter, onLinkClick) {
        val content: @Composable () -> Unit = {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(theme.spacing.block)) {
                document.blocks.forEachIndexed { index, block ->
                    // Keys by position: a block keeps its position while text is appended.
                    key(index) { ComposdownBlock(block) }
                }
                if (document.footnotes.isNotEmpty()) Footnotes(document.footnotes)
            }
        }
        if (selectable) SelectionContainer { content() } else content()
    }
}

/**
 * Renders a single [block] with the configuration of the enclosing [Composdown] (or of
 * [composdownItems]), using the replacement from [ComposdownComponents] if there is one.
 *
 * Use it inside custom [BlockComponent]s to render nested blocks.
 */
@Composable
public fun ComposdownBlock(
    block: Block,
    modifier: Modifier = Modifier,
) {
    val components = currentEnvironment().components
    // Each block is laid out in the direction of its own text, so right-to-left paragraphs, lists and
    // quotes put their markers on the correct side.
    val direction = remember(block) { block.contentDirection() } ?: LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        when (block) {
            is Block.Paragraph -> {
                BlockOrDefault(
                    block,
                    components.paragraph,
                    modifier,
                ) { b, m -> DefaultParagraph(b, m) }
            }

            is Block.Heading -> {
                BlockOrDefault(block, components.heading, modifier) { b, m -> DefaultHeading(b, m) }
            }

            is Block.CodeBlock -> {
                BlockOrDefault(
                    block,
                    components.codeBlock,
                    modifier,
                ) { b, m -> DefaultCodeBlock(b, m) }
            }

            is Block.BlockQuote -> {
                BlockOrDefault(
                    block,
                    components.blockQuote,
                    modifier,
                ) { b, m -> DefaultBlockQuote(b, m) }
            }

            is Block.Alert -> {
                BlockOrDefault(block, components.alert, modifier) { b, m -> DefaultAlert(b, m) }
            }

            is Block.ListBlock -> {
                BlockOrDefault(block, components.list, modifier) { b, m -> DefaultList(b, m) }
            }

            is Block.Table -> {
                BlockOrDefault(block, components.table, modifier) { b, m -> DefaultTable(b, m) }
            }

            Block.ThematicBreak -> {
                BlockOrDefault(
                    Block.ThematicBreak,
                    components.thematicBreak,
                    modifier,
                ) { _, m -> DefaultThematicBreak(m) }
            }

            is Block.HtmlBlock -> {
                BlockOrDefault(
                    block,
                    components.htmlBlock,
                    modifier,
                ) { b, m -> DefaultHtmlBlock(b, m) }
            }

            is Block.MathBlock -> {
                BlockOrDefault(block, components.mathBlock, modifier) { b, m -> DefaultMathBlock(b, m) }
            }
        }
    }
}

/** Draws [block] with its replacement [component] if there is one, else with [default]. */
@Composable
private fun <T : Block> BlockOrDefault(
    block: T,
    component: BlockComponent<T>?,
    modifier: Modifier = Modifier,
    default: @Composable (T, Modifier) -> Unit,
) {
    if (component != null) component.Content(block, modifier) else default(block, modifier)
}

/** Provides the rendering configuration to [content], for the entry points of this library. */
@Composable
internal fun ProvideComposdown(
    theme: ComposdownTheme,
    components: ComposdownComponents,
    imageRenderer: ImageRenderer?,
    codeHighlighter: CodeHighlighter?,
    onLinkClick: ((String) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val environment =
        remember(theme, components, imageRenderer, codeHighlighter, onLinkClick) {
            ComposdownEnvironment(theme, components, imageRenderer, codeHighlighter, onLinkClick)
        }
    CompositionLocalProvider(LocalComposdownEnvironment provides environment, content = content)
}
