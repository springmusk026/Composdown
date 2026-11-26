package io.github.springmusk026.composdown.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import io.github.springmusk026.composdown.compose.internal.Poko
import io.github.springmusk026.composdown.core.ast.Block

/**
 * Renders one kind of [Block]. Implement it to replace how that kind of block is drawn, for example a
 * code block with a copy button. Use [ComposdownBlock] inside it to render nested blocks with the
 * current configuration.
 */
public fun interface BlockComponent<in T : Block> {
    /** Draws [block]; apply [modifier] to the outermost layout. */
    @Composable
    public fun Content(
        block: T,
        modifier: Modifier,
    )
}

/**
 * Replacements for the default rendering of each kind of block. Every component that is `null` uses
 * the default rendering.
 *
 * ```
 * ComposdownComponents(
 *     codeBlock = BlockComponent { block, modifier -> CodeBlockWithCopyButton(block, modifier) },
 * )
 * ```
 */
@Immutable
@Poko
public class ComposdownComponents(
    /** Paragraphs. */
    public val paragraph: BlockComponent<Block.Paragraph>? = null,
    /** Headings. */
    public val heading: BlockComponent<Block.Heading>? = null,
    /** Fenced and indented code blocks. */
    public val codeBlock: BlockComponent<Block.CodeBlock>? = null,
    /** Block quotes. */
    public val blockQuote: BlockComponent<Block.BlockQuote>? = null,
    /** GitHub alerts. */
    public val alert: BlockComponent<Block.Alert>? = null,
    /** Bullet and ordered lists. */
    public val list: BlockComponent<Block.ListBlock>? = null,
    /** Tables. */
    public val table: BlockComponent<Block.Table>? = null,
    /** Thematic breaks. */
    public val thematicBreak: BlockComponent<Block.ThematicBreak>? = null,
    /** Raw HTML blocks. By default their source is shown as code; Composdown never interprets HTML. */
    public val htmlBlock: BlockComponent<Block.HtmlBlock>? = null,
)
