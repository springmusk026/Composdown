package io.github.springmusk026.composdown.core.internal

import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.ast.AlertType
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.FootnoteDefinition
import io.github.springmusk026.composdown.core.ast.ListItem
import io.github.springmusk026.composdown.core.ast.ListKind
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.ast.TableCell
import io.github.springmusk026.composdown.core.ast.TableRow
import io.github.springmusk026.composdown.core.ast.TaskState
import io.github.springmusk026.composdown.core.internal.block.BlockNode
import io.github.springmusk026.composdown.core.internal.block.BlockQuoteNode
import io.github.springmusk026.composdown.core.internal.block.CodeBlockNode
import io.github.springmusk026.composdown.core.internal.block.DocumentNode
import io.github.springmusk026.composdown.core.internal.block.FootnoteDefinitionNode
import io.github.springmusk026.composdown.core.internal.block.HeadingNode
import io.github.springmusk026.composdown.core.internal.block.HtmlBlockNode
import io.github.springmusk026.composdown.core.internal.block.ItemNode
import io.github.springmusk026.composdown.core.internal.block.ListNode
import io.github.springmusk026.composdown.core.internal.block.MathBlockNode
import io.github.springmusk026.composdown.core.internal.block.ParagraphNode
import io.github.springmusk026.composdown.core.internal.block.TableNode
import io.github.springmusk026.composdown.core.internal.block.ThematicBreakNode
import io.github.springmusk026.composdown.core.internal.inline.FootnoteRegistry
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab
import io.github.springmusk026.composdown.core.internal.text.normalizeLinkLabel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts finalized [BlockNode]s into the immutable public model, parsing inline content on the
 * way. Blocks are converted in document order, so footnotes are numbered in the order in which they
 * are first referenced.
 *
 * @param footnotes the document's footnote definitions, or `null` if footnotes are disabled.
 * @param streamingLeaf the block whose text is still being written, if any. Its inline content is
 *   parsed optimistically (see [io.github.springmusk026.composdown.core.internal.inline.InlineParser.parse]).
 */
internal class AstBuilder(
    private val inlines: InlineConverter,
    extensions: Set<MarkdownExtension>,
    private val footnotes: FootnoteRegistry?,
    private val streamingLeaf: BlockNode? = null,
) {
    private val taskLists = MarkdownExtension.TASK_LISTS in extensions
    private val alerts = MarkdownExtension.ALERTS in extensions
    private val math = MarkdownExtension.MATH in extensions

    /** Converted footnote definitions by normalized label; the first definition of a label wins. */
    private val footnoteDefinitions = LinkedHashMap<String, Pair<String, ImmutableList<Block>>>()

    /** Footnote definitions converted so far, by normalized label: the label as written and its blocks. */
    val collectedFootnotes: Map<String, Pair<String, ImmutableList<Block>>> get() = footnoteDefinitions

    /** Converts one top-level block, or returns `null` if it is not part of the block flow. */
    fun topLevelBlock(node: BlockNode): Block? = block(node, topLevel = true)

    fun document(root: DocumentNode): MarkdownDocument {
        val blocks = blocks(root.children, topLevel = true)
        val referenced =
            footnoteDefinitions.values
                .mapNotNull { (label, children) ->
                    footnotes?.numberOf(label)?.let { number -> FootnoteDefinition(label, number, children) }
                }.sortedBy { it.number }
        return MarkdownDocument(blocks, referenced.toImmutableList())
    }

    private fun blocks(
        nodes: List<BlockNode>,
        topLevel: Boolean = false,
    ): ImmutableList<Block> = nodes.mapNotNull { block(it, topLevel) }.toImmutableList()

    /** Converts [node], or returns `null` for blocks that are not part of the block flow. */
    private fun block(
        node: BlockNode,
        topLevel: Boolean,
    ): Block? =
        when (node) {
            is ParagraphNode -> {
                Block.Paragraph(inlines.convert(node.content.toString(), node === streamingLeaf))
            }

            is HeadingNode -> {
                Block.Heading(node.level, inlines.convert(node.content.toString(), node === streamingLeaf))
            }

            is ThematicBreakNode -> {
                Block.ThematicBreak
            }

            is BlockQuoteNode -> {
                blockQuote(node, topLevel)
            }

            is ListNode -> {
                list(node)
            }

            is CodeBlockNode -> {
                codeBlock(node)
            }

            is MathBlockNode -> {
                Block.MathBlock(node.literal)
            }

            is HtmlBlockNode -> {
                Block.HtmlBlock(node.literal)
            }

            is TableNode -> {
                table(node)
            }

            is FootnoteDefinitionNode -> {
                collectFootnote(node)
                null
            }

            is ItemNode, is DocumentNode -> {
                error("${node::class.simpleName} is not a standalone block")
            }
        }

    /** A top-level block quote may be an alert. */
    private fun blockQuote(
        node: BlockQuoteNode,
        topLevel: Boolean,
    ): Block = (if (alerts && topLevel) alert(node) else null) ?: Block.BlockQuote(blocks(node.children))

    /** A ```math code block is a math block, as on GitHub. */
    private fun codeBlock(node: CodeBlockNode): Block =
        if (math && node.isFenced && node.info?.substringBefore(' ') == "math") {
            Block.MathBlock(node.literal.trim())
        } else {
            Block.CodeBlock(node.literal, node.info, node.isFenced)
        }

    private fun list(node: ListNode): Block.ListBlock {
        val data = node.data
        val kind =
            if (data.isOrdered) {
                ListKind.Ordered(
                    data.start,
                    data.delimiter,
                )
            } else {
                ListKind.Bullet(data.bulletChar)
            }
        val items = node.children.map { listItem(it as ItemNode) }.toImmutableList()
        return Block.ListBlock(kind, node.isTight, items)
    }

    /** A task list item starts with `[ ]`, `[x]` or `[X]` followed by a space or tab. */
    private fun listItem(node: ItemNode): ListItem {
        val first = node.children.firstOrNull() as? ParagraphNode
        val content = first?.content
        val taskState = if (taskLists && content != null) taskStateOf(content) else null
        if (taskState == null || content == null) return ListItem(blocks(node.children), null)

        val text = content.substring(TASK_MARKER_LENGTH).trimStart(' ', '\t')
        val children = withLeadingText(text, first, blocks(node.children.drop(1)))
        return ListItem(children, taskState)
    }

    private fun taskStateOf(content: CharSequence): TaskState? {
        if (content.length <= TASK_MARKER_LENGTH || !content[TASK_MARKER_LENGTH].isSpaceOrTab()) return null
        return when (content.substring(0, TASK_MARKER_LENGTH)) {
            "[ ]" -> TaskState.UNCHECKED
            "[x]", "[X]" -> TaskState.CHECKED
            else -> null
        }
    }

    private fun table(node: TableNode): Block.Table {
        val columns = node.alignments.size
        val lastRowIsStreaming = node === streamingLeaf

        fun row(
            cells: List<String>,
            optimistic: Boolean,
        ) = TableRow(
            List(columns) { TableCell(inlines.convert(cells.getOrElse(it) { "" }, optimistic)) }.toImmutableList(),
        )
        val rows =
            node.rows.mapIndexed {
                index,
                cells,
                ->
                row(cells, lastRowIsStreaming && index == node.rows.lastIndex)
            }
        return Block.Table(
            node.alignments.toImmutableList(),
            row(node.headerCells, optimistic = false),
            rows.toImmutableList(),
        )
    }

    /**
     * Converts a top-level block quote whose first line is `[!TYPE]` into an alert, or returns `null`
     * if it is an ordinary block quote.
     */
    private fun alert(node: BlockQuoteNode): Block.Alert? {
        val first = node.children.firstOrNull() as? ParagraphNode ?: return null
        val content = first.content
        val lineEnd = content.indexOf('\n').let { if (it < 0) content.length else it }
        val marker = content.substring(0, lineEnd).trimEnd(' ', '\t')
        if (!marker.startsWith("[!") || !marker.endsWith("]")) return null
        val type =
            AlertType.entries.firstOrNull { it.name.equals(marker.substring(2, marker.length - 1), ignoreCase = true) }
                ?: return null

        val text = content.substring(minOf(lineEnd + 1, content.length))
        return Block.Alert(type, withLeadingText(text, first, blocks(node.children.drop(1))))
    }

    /**
     * [rest] preceded by a paragraph of [text] (what remains of [source] after a marker was removed),
     * unless the text is blank.
     */
    private fun withLeadingText(
        text: String,
        source: ParagraphNode,
        rest: ImmutableList<Block>,
    ): ImmutableList<Block> {
        if (text.isBlank()) return rest
        val paragraph = Block.Paragraph(inlines.convert(text, source === streamingLeaf))
        return (listOf<Block>(paragraph) + rest).toImmutableList()
    }

    private fun collectFootnote(node: FootnoteDefinitionNode) {
        // Every definition is converted, in document order, so references inside it are numbered
        // where they appear; only the first definition of a label is kept.
        val children = blocks(node.children)
        val key = normalizeLinkLabel(node.label)
        if (key !in footnoteDefinitions) footnoteDefinitions[key] = node.label to children
    }

    private companion object {
        /** Length of `[ ]`, `[x]` and `[X]`. */
        const val TASK_MARKER_LENGTH = 3
    }
}
