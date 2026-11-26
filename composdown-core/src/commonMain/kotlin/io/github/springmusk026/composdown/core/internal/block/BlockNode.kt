package io.github.springmusk026.composdown.core.internal.block

import io.github.springmusk026.composdown.core.ast.TableAlignment
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab
import io.github.springmusk026.composdown.core.internal.text.unescapeString

/** Columns of indentation for footnote content after the first line. */
private const val FOOTNOTE_CONTENT_INDENT = 4

/** How a line relates to an open block. */
internal enum class ContinueResult {
    /** The line continues the block; the cursor has consumed the block's markers or indentation. */
    MATCHED,

    /** The line does not continue the block, which will be closed. */
    NOT_MATCHED,

    /** The line ends the block and nothing else on it needs parsing (a closing code fence). */
    LINE_CONSUMED,
}

/**
 * Mutable block tree used while parsing. Blocks are open while lines can still be added to them and
 * closed (finalized) once a line cannot continue them; closed blocks never reopen.
 */
internal sealed class BlockNode {
    var parent: BlockNode? = null
        private set
    val children: MutableList<BlockNode> = ArrayList(0)
    var isOpen: Boolean = true

    /** Nesting depth: 0 for the document, 1 for top-level blocks, and so on. */
    var depth: Int = 0
        private set

    /** 1-based number of the first source line of this block. */
    var startLine: Int = 0

    /** 1-based number of the last source line of this block, set when it is finalized. */
    var endLine: Int = 0

    /** Whether this block holds lines of text (rather than child blocks). */
    open val acceptsLines: Boolean get() = false

    abstract fun canContain(child: BlockNode): Boolean

    /**
     * Checks whether the cursor's line continues this open block and, if so, consumes the block's
     * markers or indentation. The cursor's next non-space position has been computed.
     */
    abstract fun continueWith(cursor: LineCursor): ContinueResult

    fun appendChild(child: BlockNode) {
        child.parent = this
        child.depth = depth + 1
        children.add(child)
    }

    /** Replaces this block, which must be its parent's last child, with [replacement]. */
    fun replaceWith(replacement: BlockNode) {
        val parent = checkNotNull(parent) { "Cannot replace a detached block" }
        check(parent.children.last() === this) { "Only the last child of a block can be replaced" }
        parent.children[parent.children.lastIndex] = replacement
        replacement.parent = parent
        replacement.depth = depth
        this.parent = null
    }

    /** Removes this block, which must be its parent's last child. */
    fun unlink() {
        val parent = checkNotNull(parent) { "Cannot unlink a detached block" }
        check(parent.children.last() === this) { "Only the last child of a block can be unlinked" }
        parent.children.removeAt(parent.children.lastIndex)
        this.parent = null
    }
}

/** A block that collects raw lines of text, which become its content when it is finalized. */
internal sealed class LeafBlockNode : BlockNode() {
    val content: StringBuilder = StringBuilder()

    override fun canContain(child: BlockNode): Boolean = false
}

internal class DocumentNode : BlockNode() {
    override fun canContain(child: BlockNode): Boolean = child !is ItemNode

    override fun continueWith(cursor: LineCursor): ContinueResult = ContinueResult.MATCHED
}

internal class BlockQuoteNode : BlockNode() {
    override fun canContain(child: BlockNode): Boolean = child !is ItemNode

    override fun continueWith(cursor: LineCursor): ContinueResult =
        if (cursor.consumeBlockQuoteMarker()) ContinueResult.MATCHED else ContinueResult.NOT_MATCHED
}

/** Details of a list marker, shared by a list and its items. */
internal class ListData(
    val isOrdered: Boolean,
    /** `-`, `+` or `*` for bullet lists. */
    val bulletChar: Char,
    /** The start number for ordered lists. */
    val start: Int,
    /** `.` or `)` for ordered lists. */
    val delimiter: Char,
    /** Column of the marker relative to the container's content. */
    val markerOffset: Int,
    /** Number of characters in the marker, such as 1 for `-` or 3 for `10.`. */
    val markerWidth: Int,
) {
    /** Width of the marker plus the spaces after it: content columns of the item. */
    var padding: Int = 0

    fun matches(other: ListData): Boolean =
        isOrdered == other.isOrdered && delimiter == other.delimiter && bulletChar == other.bulletChar
}

internal class ListNode(
    val data: ListData,
) : BlockNode() {
    var isTight: Boolean = true

    override fun canContain(child: BlockNode): Boolean = child is ItemNode

    /**
     * Computes [isTight] and [endLine] from the finalized items. A list is loose if any of its items
     * are separated by blank lines, or if any item directly contains two blocks with a blank line
     * between them.
     */
    fun finalizeList() {
        endLine = children.last().endLine
        isTight =
            children.zipWithNext().none { (item, next) -> hasBlankLineBetween(item, next) } &&
            children.none { item ->
                item.children.zipWithNext().any { (first, second) -> hasBlankLineBetween(first, second) }
            }
    }

    private fun hasBlankLineBetween(
        first: BlockNode,
        second: BlockNode,
    ): Boolean = first.endLine != second.startLine - 1

    override fun continueWith(cursor: LineCursor): ContinueResult = ContinueResult.MATCHED
}

internal class ItemNode(
    val data: ListData,
) : BlockNode() {
    override fun canContain(child: BlockNode): Boolean = child !is ItemNode

    /** An item ends with its last child, not with any blank lines after it. */
    fun finalizeItem() {
        endLine = children.lastOrNull()?.endLine ?: startLine
    }

    override fun continueWith(cursor: LineCursor): ContinueResult {
        val contentIndent = data.markerOffset + data.padding
        return when {
            // An item can begin with at most one blank line.
            cursor.blank && children.isEmpty() -> ContinueResult.NOT_MATCHED

            cursor.blank -> ContinueResult.MATCHED.also { cursor.advanceNextNonspace() }

            cursor.indent >= contentIndent -> ContinueResult.MATCHED.also { cursor.advanceColumns(contentIndent) }

            else -> ContinueResult.NOT_MATCHED
        }
    }
}

internal class ParagraphNode : LeafBlockNode() {
    override val acceptsLines: Boolean get() = true

    override fun continueWith(cursor: LineCursor): ContinueResult =
        if (cursor.blank) ContinueResult.NOT_MATCHED else ContinueResult.MATCHED
}

internal class HeadingNode(
    val level: Int,
) : LeafBlockNode() {
    /** Headings are always a single line. */
    override fun continueWith(cursor: LineCursor): ContinueResult = ContinueResult.NOT_MATCHED
}

internal class ThematicBreakNode : LeafBlockNode() {
    override fun continueWith(cursor: LineCursor): ContinueResult = ContinueResult.NOT_MATCHED
}

internal class CodeBlockNode(
    val isFenced: Boolean,
    val fenceChar: Char = ' ',
    val fenceLength: Int = 0,
    /** Indentation of the opening fence, removed from content lines where present. */
    val fenceOffset: Int = 0,
) : LeafBlockNode() {
    override val acceptsLines: Boolean get() = true

    var info: String? = null
    var literal: String = ""

    /** Splits the collected lines into the info string and the literal code. */
    fun finalizeCode() {
        if (isFenced) {
            // The first line holds the info string.
            val firstLineEnd = content.indexOf('\n')
            val rawInfo = content.substring(0, firstLineEnd).trim(' ', '\t')
            info = if (rawInfo.isEmpty()) null else unescapeString(rawInfo)
            literal = content.substring(firstLineEnd + 1)
        } else {
            // Trailing blank lines are not part of an indented code block.
            var end = content.length
            while (end > 0) {
                val lineStart = content.lastIndexOf('\n', end - 2) + 1
                if ((lineStart until end).any {
                        content[it] != ' ' && content[it] != '\t' && content[it] != '\n'
                    }
                ) {
                    break
                }
                end = lineStart
            }
            literal = content.substring(0, end)
            // The block ends at its last non-blank line, so that a list containing it can tell that a
            // blank line follows (which makes the list loose), as for any other block.
            endLine -= (end until content.length).count { content[it] == '\n' }
        }
    }

    override fun continueWith(cursor: LineCursor): ContinueResult {
        if (!isFenced) {
            return when {
                cursor.indent >= CODE_INDENT -> ContinueResult.MATCHED.also { cursor.advanceColumns(CODE_INDENT) }
                cursor.blank -> ContinueResult.MATCHED.also { cursor.advanceNextNonspace() }
                else -> ContinueResult.NOT_MATCHED
            }
        }
        if (isClosingFence(cursor)) return ContinueResult.LINE_CONSUMED
        // Remove up to the opening fence's indentation from content lines.
        var remaining = fenceOffset
        while (remaining > 0 && cursor.charAt(cursor.offset).isSpaceOrTab()) {
            cursor.advanceColumns(1)
            remaining--
        }
        return ContinueResult.MATCHED
    }

    /** A closing fence uses the opening fence character, is at least as long, and has nothing after it. */
    private fun isClosingFence(cursor: LineCursor): Boolean {
        if (cursor.indented || cursor.charAt(cursor.nextNonspace) != fenceChar) return false
        var end = cursor.nextNonspace
        while (cursor.charAt(end) == fenceChar) end++
        return end - cursor.nextNonspace >= fenceLength && cursor.isBlankFrom(end)
    }
}

internal class HtmlBlockNode(
    val type: HtmlBlockType,
) : LeafBlockNode() {
    override val acceptsLines: Boolean get() = true

    var literal: String = ""

    override fun continueWith(cursor: LineCursor): ContinueResult =
        if (cursor.blank && type.endsAtBlankLine) ContinueResult.NOT_MATCHED else ContinueResult.MATCHED
}

/**
 * A GFM table. The header and alignments come from the line that started it; body rows are collected
 * as lines and split into cells when the table is finalized.
 */
internal class TableNode(
    val alignments: List<TableAlignment>,
    val headerCells: List<String>,
) : LeafBlockNode() {
    override val acceptsLines: Boolean get() = true

    var rows: List<List<String>> = emptyList()

    /**
     * If the header row was split off a longer paragraph, the first line of that paragraph; else 0.
     * Re-parsing from the header line alone could read it differently (it was a continuation line of
     * the paragraph), so incremental parsing re-parses from here instead.
     */
    var splitFromLine: Int = 0

    fun finalizeTable() {
        rows =
            content
                .split('\n')
                .filter { it.isNotEmpty() }
                .map { splitTableRow(it, 0) }
    }

    /** A row continues the table if it has at least one cell (so a blank line ends it). */
    override fun continueWith(cursor: LineCursor): ContinueResult =
        if (splitTableRow(
                cursor.line,
                cursor.nextNonspace,
            ).isEmpty()
        ) {
            ContinueResult.NOT_MATCHED
        } else {
            ContinueResult.MATCHED
        }
}

/**
 * A footnote definition (`[^label]: content`). Like a list item, it contains blocks; continuation
 * lines are indented by four columns.
 */
internal class FootnoteDefinitionNode(
    val label: String,
) : BlockNode() {
    override fun canContain(child: BlockNode): Boolean = child !is ItemNode

    /** Footnote content is indented by four columns; blank lines continue the definition. */
    override fun continueWith(cursor: LineCursor): ContinueResult =
        when {
            cursor.blank -> {
                ContinueResult.MATCHED.also { cursor.advanceNextNonspace() }
            }

            cursor.indent >= FOOTNOTE_CONTENT_INDENT -> {
                ContinueResult.MATCHED.also {
                    cursor.advanceColumns(FOOTNOTE_CONTENT_INDENT)
                }
            }

            else -> {
                ContinueResult.NOT_MATCHED
            }
        }
}

/**
 * A block of TeX math delimited by lines starting with `$$` and ending with [closer] (`$$` or `\]`).
 * Like a fenced code block, it ends at its closing delimiter or at the end of its container.
 */
internal class MathBlockNode(
    private val closer: String,
) : LeafBlockNode() {
    override val acceptsLines: Boolean get() = true

    var literal: String = ""

    override fun continueWith(cursor: LineCursor): ContinueResult {
        val rest = cursor.line.substring(cursor.offset).trimEnd(' ', '\t')
        if (!rest.endsWith(closer)) return ContinueResult.MATCHED
        // The closing line ends the block; text before the delimiter belongs to the math.
        content.append(rest, 0, rest.length - closer.length).append('\n')
        return ContinueResult.LINE_CONSUMED
    }

    fun finalizeMath() {
        literal = content.toString().trim()
    }
}
