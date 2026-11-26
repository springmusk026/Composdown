package io.github.springmusk026.composdown.core.internal

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.ListItem
import io.github.springmusk026.composdown.core.ast.ListKind
import io.github.springmusk026.composdown.core.internal.block.BlockNode
import io.github.springmusk026.composdown.core.internal.block.BlockQuoteNode
import io.github.springmusk026.composdown.core.internal.block.CodeBlockNode
import io.github.springmusk026.composdown.core.internal.block.DocumentNode
import io.github.springmusk026.composdown.core.internal.block.HeadingNode
import io.github.springmusk026.composdown.core.internal.block.HtmlBlockNode
import io.github.springmusk026.composdown.core.internal.block.ItemNode
import io.github.springmusk026.composdown.core.internal.block.ListNode
import io.github.springmusk026.composdown.core.internal.block.MAX_NESTING_DEPTH
import io.github.springmusk026.composdown.core.internal.block.ParagraphNode
import io.github.springmusk026.composdown.core.internal.block.ThematicBreakNode
import io.github.springmusk026.composdown.core.internal.inline.InlineNode
import io.github.springmusk026.composdown.core.internal.inline.InlineParser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts finalized [BlockNode]s into the immutable public model, parsing inline content on the way.
 */
internal class AstBuilder(
    private val inlineParser: InlineParser,
) {
    fun blocks(nodes: List<BlockNode>): ImmutableList<Block> = nodes.map(::block).toImmutableList()

    fun block(node: BlockNode): Block =
        when (node) {
            is ParagraphNode -> Block.Paragraph(inlines(node.content.toString()))
            is HeadingNode -> Block.Heading(node.level, inlines(node.content.toString()))
            is ThematicBreakNode -> Block.ThematicBreak
            is BlockQuoteNode -> Block.BlockQuote(blocks(node.children))
            is ListNode -> list(node)
            is CodeBlockNode -> Block.CodeBlock(node.literal, node.info, node.isFenced)
            is HtmlBlockNode -> Block.HtmlBlock(node.literal)
            is ItemNode, is DocumentNode -> error("${node::class.simpleName} is not a standalone block")
        }

    private fun list(node: ListNode): Block.ListBlock {
        val data = node.data
        val kind =
            if (data.isOrdered) {
                ListKind.Ordered(data.start, data.delimiter)
            } else {
                ListKind.Bullet(data.bulletChar)
            }
        val items = node.children.map { ListItem(blocks(it.children)) }.toImmutableList()
        return Block.ListBlock(kind, node.isTight, items)
    }

    private fun inlines(content: String): ImmutableList<Inline> = inlines(inlineParser.parse(content), depth = 0)

    /**
     * Converts the children of [parent]. At [MAX_NESTING_DEPTH], nested elements are flattened to
     * their text, so that hostile input cannot produce trees deep enough to exhaust the stack.
     */
    private fun inlines(
        parent: InlineNode,
        depth: Int,
    ): ImmutableList<Inline> {
        if (parent.firstChild == null) return persistentListOf()
        if (depth >= MAX_NESTING_DEPTH) return persistentListOf(Inline.Text(plainTextOf(parent)))
        val result = ArrayList<Inline>()
        val pendingText = StringBuilder()
        var child = parent.firstChild
        while (child != null) {
            if (child.kind == InlineNode.Kind.TEXT) {
                pendingText.append(child.literal)
            } else {
                flushText(pendingText, result)
                result.add(inline(child, depth))
            }
            child = child.next
        }
        flushText(pendingText, result)
        return result.toImmutableList()
    }

    /** Adjacent text nodes are merged, and empty ones (fully consumed delimiter runs) dropped. */
    private fun flushText(
        pendingText: StringBuilder,
        result: MutableList<Inline>,
    ) {
        if (pendingText.isNotEmpty()) {
            result.add(Inline.Text(pendingText.toString()))
            pendingText.clear()
        }
    }

    private fun inline(
        node: InlineNode,
        depth: Int,
    ): Inline =
        when (node.kind) {
            InlineNode.Kind.CODE -> Inline.Code(node.literal)
            InlineNode.Kind.EMPHASIS -> Inline.Emphasis(inlines(node, depth + 1))
            InlineNode.Kind.STRONG -> Inline.Strong(inlines(node, depth + 1))
            InlineNode.Kind.LINK -> Inline.Link(node.destination, node.title.nullIfEmpty(), inlines(node, depth + 1))
            InlineNode.Kind.IMAGE -> Inline.Image(node.destination, node.title.nullIfEmpty(), inlines(node, depth + 1))
            InlineNode.Kind.HTML -> Inline.HtmlInline(node.literal)
            InlineNode.Kind.SOFT_BREAK -> Inline.SoftBreak
            InlineNode.Kind.HARD_BREAK -> Inline.HardBreak
            InlineNode.Kind.TEXT -> Inline.Text(node.literal)
            InlineNode.Kind.ROOT -> error("The root node is not an inline element")
        }

    /** The text of all descendants of [root], collected without recursion. */
    private fun plainTextOf(root: InlineNode): String {
        val text = StringBuilder()
        var node = root.firstChild
        while (node != null && node !== root) {
            when (node.kind) {
                InlineNode.Kind.TEXT, InlineNode.Kind.CODE -> text.append(node.literal)
                InlineNode.Kind.SOFT_BREAK, InlineNode.Kind.HARD_BREAK -> text.append('\n')
                else -> Unit
            }
            node = node.firstChild ?: nextInPreOrder(node, root)
        }
        return text.toString()
    }

    private fun nextInPreOrder(
        node: InlineNode,
        root: InlineNode,
    ): InlineNode? {
        var current: InlineNode? = node
        while (current != null && current !== root) {
            current.next?.let { return it }
            current = current.parent
        }
        return null
    }

    private fun String?.nullIfEmpty(): String? = this?.takeIf { it.isNotEmpty() }
}
