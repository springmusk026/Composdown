package io.github.springmusk026.composdown.compose

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.springmusk026.composdown.compose.internal.Footnotes
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.MarkdownDocument

/**
 * Adds the blocks of [document] to a lazy list as separate items, so that long documents (or many
 * messages in a chat) only compose what is visible. Footnotes, if any, form a last item.
 *
 * Each item's key is `"$keyPrefix:$index"`: a block keeps its key while text is appended, so items
 * above the streaming end of a document neither move nor recompose. Use a distinct [keyPrefix] for
 * each document in the same list, such as a message id.
 *
 * Items have no spacing between them; use the list's `verticalArrangement` (for example
 * `Arrangement.spacedBy(theme.spacing.block)`) or [itemModifier].
 *
 * ```
 * LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
 *     for (message in messages) {
 *         composdownItems(message.state.document, keyPrefix = message.id)
 *     }
 * }
 * ```
 *
 * @param theme colors, text styles and spacing, or `null` for [ComposdownDefaults.theme] from the
 *   current Material theme.
 * See [Composdown] for the other parameters.
 */
public fun LazyListScope.composdownItems(
    document: MarkdownDocument,
    keyPrefix: String,
    theme: ComposdownTheme? = null,
    components: ComposdownComponents = ComposdownDefaults.Components,
    imageRenderer: ImageRenderer? = null,
    codeHighlighter: CodeHighlighter? = null,
    onLinkClick: ((String) -> Unit)? = null,
    itemModifier: Modifier = Modifier,
) {
    val config = ItemConfig(theme, components, imageRenderer, codeHighlighter, onLinkClick)
    val blocks = document.blocks
    items(
        count = blocks.size,
        key = { index -> "$keyPrefix:$index" },
        contentType = { index -> blocks[index].contentType() },
    ) { index ->
        Item(config) { ComposdownBlock(blocks[index], itemModifier) }
    }
    if (document.footnotes.isNotEmpty()) {
        item(key = "$keyPrefix:footnotes", contentType = "footnotes") {
            Item(config) { Footnotes(document.footnotes, itemModifier) }
        }
    }
}

/** The configuration shared by all items of one document. */
private class ItemConfig(
    val theme: ComposdownTheme?,
    val components: ComposdownComponents,
    val imageRenderer: ImageRenderer?,
    val codeHighlighter: CodeHighlighter?,
    val onLinkClick: ((String) -> Unit)?,
)

@Composable
private fun Item(
    config: ItemConfig,
    content: @Composable () -> Unit,
) {
    with(config) {
        ProvideComposdown(
            theme ?: ComposdownDefaults.theme(),
            components,
            imageRenderer,
            codeHighlighter,
            onLinkClick,
            content,
        )
    }
}

/** Items of the same kind of block can reuse each other's compositions when scrolled. */
private fun Block.contentType(): String =
    when (this) {
        is Block.Paragraph -> "paragraph"
        is Block.Heading -> "heading"
        is Block.CodeBlock -> "code"
        is Block.BlockQuote -> "quote"
        is Block.Alert -> "alert"
        is Block.ListBlock -> "list"
        is Block.Table -> "table"
        Block.ThematicBreak -> "break"
        is Block.HtmlBlock -> "html"
        is Block.MathBlock -> "math"
    }
