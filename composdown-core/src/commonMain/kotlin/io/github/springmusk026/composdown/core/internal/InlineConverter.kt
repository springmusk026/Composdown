package io.github.springmusk026.composdown.core.internal

import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.internal.block.MAX_NESTING_DEPTH
import io.github.springmusk026.composdown.core.internal.inline.InlineNode
import io.github.springmusk026.composdown.core.internal.inline.InlineParser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Parses raw inline content and converts the resulting [InlineNode] tree into the immutable public
 * model, merging adjacent text.
 */
internal class InlineConverter(
    private val inlineParser: InlineParser,
) {
    fun convert(content: String): ImmutableList<Inline> = inlines(inlineParser.parse(content), depth = 0)

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
            InlineNode.Kind.STRIKETHROUGH -> Inline.Strikethrough(inlines(node, depth + 1))
            InlineNode.Kind.LINK -> Inline.Link(node.destination, node.title.nullIfEmpty(), inlines(node, depth + 1))
            InlineNode.Kind.IMAGE -> Inline.Image(node.destination, node.title.nullIfEmpty(), inlines(node, depth + 1))
            InlineNode.Kind.HTML -> Inline.HtmlInline(node.literal)
            InlineNode.Kind.SOFT_BREAK -> Inline.SoftBreak
            InlineNode.Kind.HARD_BREAK -> Inline.HardBreak
            InlineNode.Kind.FOOTNOTE_REFERENCE -> Inline.FootnoteReference(node.literal, node.number)
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
                InlineNode.Kind.FOOTNOTE_REFERENCE -> text.append(node.number)
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
