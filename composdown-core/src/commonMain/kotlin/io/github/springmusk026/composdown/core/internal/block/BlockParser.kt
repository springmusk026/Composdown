package io.github.springmusk026.composdown.core.internal.block

import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.internal.inline.FootnoteRegistry
import io.github.springmusk026.composdown.core.internal.inline.LinkReferenceMap
import io.github.springmusk026.composdown.core.internal.inline.parseLinkReferenceDefinition
import io.github.springmusk026.composdown.core.internal.text.isAsciiDigit
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab

/** Maximum depth of nested block quotes and lists. Matches the default limit of markdown-it. */
internal const val MAX_NESTING_DEPTH = 100

private const val MAX_HEADING_LEVEL = 6

/** Characters that cannot appear in a footnote definition label. */
private const val FOOTNOTE_LABEL_TERMINATORS = "] \t\r\n"
private const val MIN_FENCE_LENGTH = 3
private const val MIN_THEMATIC_BREAK_MARKERS = 3

/**
 * Parses the block structure of a Markdown document one line at a time, following the CommonMark
 * algorithm: each line first continues the chain of open blocks as far as it can, then may start
 * new blocks, and the rest of the line is added as text to the innermost open block.
 *
 * Feed lines with [addLine] (without line endings) and call [finish] once at the end. Link
 * reference definitions are recorded in [references] as paragraphs are finalized, and footnote
 * definitions in [footnotes] as they start.
 *
 * @param footnotes where to record footnote definitions, or `null` if footnotes are disabled.
 */
internal class BlockParser(
    private val references: LinkReferenceMap,
    extensions: Set<MarkdownExtension>,
    private val footnotes: FootnoteRegistry?,
) {
    private val tables = MarkdownExtension.TABLES in extensions

    val document: DocumentNode = DocumentNode().also { it.startLine = 1 }

    private val cursor = LineCursor()
    private var lineNumber = 0

    /** The innermost open block. */
    private var tip: BlockNode = document

    /** The tip before the current line was processed. */
    private var oldTip: BlockNode = document

    /** The deepest open block continued by the current line. */
    private var lastMatchedContainer: BlockNode = document

    /** Whether all blocks below [lastMatchedContainer] have been closed for the current line. */
    private var allClosed = true

    private enum class StartResult { NONE, CONTAINER, LEAF }

    /** Adds one line of input, without its line ending. */
    fun addLine(input: String) {
        cursor.reset(if (input.indexOf('\u0000') >= 0) input.replace('\u0000', '�') else input)
        lineNumber++
        oldTip = tip

        var container = matchOpenBlocks() ?: return
        allClosed = container === oldTip
        lastMatchedContainer = container

        // Open new blocks until a leaf block starts or nothing more can start on this line.
        // Paragraphs and tables accept lines but can still be interrupted by other blocks.
        val isInterruptibleLeaf = container is ParagraphNode || container is TableNode
        var result = if (!isInterruptibleLeaf && container.acceptsLines) StartResult.LEAF else StartResult.CONTAINER
        while (result == StartResult.CONTAINER) {
            cursor.findNextNonspace()
            val mayStart = cursor.indented || cursor.charAt(cursor.nextNonspace).mayStartBlock()
            result = if (mayStart) startBlock(container) else StartResult.NONE
            when (result) {
                StartResult.NONE -> cursor.advanceNextNonspace()
                StartResult.CONTAINER, StartResult.LEAF -> container = tip
            }
        }
        addRemainingText(container)
    }

    /**
     * The innermost open block if it holds text that is still being written (a paragraph, heading or
     * table), or `null`. Only meaningful before [finish].
     */
    val openTextBlock: BlockNode?
        get() = tip.takeIf { it is ParagraphNode || it is HeadingNode || it is TableNode }

    /** Number of lines added so far. */
    val lineCount: Int get() = lineNumber

    /** Closes all open blocks. Call once, after the last line. */
    fun finish(): DocumentNode {
        while (tip !== document) finalize(tip, lineNumber)
        finalize(document, lineNumber)
        return document
    }

    /**
     * Continues the chain of open blocks with the current line. Returns the deepest block the line
     * continues, or `null` if a block consumed the whole line (a closing code fence).
     */
    private fun matchOpenBlocks(): BlockNode? {
        var container: BlockNode = document
        while (true) {
            val last = container.children.lastOrNull()
            if (last == null || !last.isOpen) return container
            cursor.findNextNonspace()
            when (last.continueWith(cursor)) {
                ContinueResult.MATCHED -> {
                    container = last
                }

                ContinueResult.NOT_MATCHED -> {
                    return container
                }

                ContinueResult.LINE_CONSUMED -> {
                    finalize(last, lineNumber)
                    return null
                }
            }
        }
    }

    private fun addRemainingText(container: BlockNode) {
        if (!allClosed && !cursor.blank && tip is ParagraphNode) {
            // Lazy continuation line: text that continues a paragraph inside unmatched containers.
            addLineToTip()
            return
        }
        closeUnmatchedBlocks()
        if (container.acceptsLines) {
            addLineToTip()
            if (container is HtmlBlockNode && container.type.endsOn(cursor.line.substring(cursor.offset))) {
                finalize(container, lineNumber)
            }
        } else if (cursor.offset < cursor.line.length && !cursor.blank) {
            addChild(ParagraphNode())
            cursor.advanceNextNonspace()
            addLineToTip()
        }
    }

    /** Tries each kind of block start in the order the specification gives them precedence. */
    private fun startBlock(container: BlockNode): StartResult =
        when {
            startBlockQuote(container) -> StartResult.CONTAINER
            startAtxHeading() -> StartResult.LEAF
            startFencedCodeBlock() -> StartResult.LEAF
            startHtmlBlock(container) -> StartResult.LEAF
            startSetextHeading(container) -> StartResult.LEAF
            startTable(container) -> StartResult.LEAF
            startThematicBreak() -> StartResult.LEAF
            startFootnoteDefinition(container) -> StartResult.CONTAINER
            startListItem(container) -> StartResult.CONTAINER
            startIndentedCodeBlock() -> StartResult.LEAF
            else -> StartResult.NONE
        }

    private fun startBlockQuote(container: BlockNode): Boolean {
        if (!canNestContainerIn(container) || !cursor.consumeBlockQuoteMarker()) return false
        closeUnmatchedBlocks()
        addChild(BlockQuoteNode())
        return true
    }

    private fun startAtxHeading(): Boolean {
        if (cursor.indented) return false
        var end = cursor.nextNonspace
        while (cursor.charAt(end) == '#') end++
        val level = end - cursor.nextNonspace
        if (level !in 1..MAX_HEADING_LEVEL ||
            (end < cursor.line.length && !cursor.charAt(end).isSpaceOrTab())
        ) {
            return false
        }
        while (cursor.charAt(end).isSpaceOrTab()) end++
        val markerLength = end - cursor.nextNonspace
        cursor.advanceNextNonspace()
        cursor.advanceChars(markerLength)
        closeUnmatchedBlocks()
        val heading = HeadingNode(level)
        addChild(heading)
        heading.content.append(stripClosingSequence(cursor.line.substring(cursor.offset)))
        cursor.advanceToEnd()
        return true
    }

    private fun startFencedCodeBlock(): Boolean {
        if (cursor.indented) return false
        val fenceChar = cursor.charAt(cursor.nextNonspace)
        if (fenceChar != '`' && fenceChar != '~') return false
        var end = cursor.nextNonspace
        while (cursor.charAt(end) == fenceChar) end++
        val fenceLength = end - cursor.nextNonspace
        // The info string of a backtick fence cannot contain backticks.
        if (fenceLength < MIN_FENCE_LENGTH || (fenceChar == '`' && cursor.line.indexOf('`', end) >= 0)) return false
        closeUnmatchedBlocks()
        addChild(
            CodeBlockNode(
                isFenced = true,
                fenceChar = fenceChar,
                fenceLength = fenceLength,
                fenceOffset = cursor.indent,
            ),
        )
        cursor.advanceNextNonspace()
        cursor.advanceChars(fenceLength)
        return true
    }

    private fun startHtmlBlock(container: BlockNode): Boolean {
        if (cursor.indented || cursor.charAt(cursor.nextNonspace) != '<') return false
        val canInterruptParagraph = container !is ParagraphNode && !isLazyParagraphContinuation()
        val type = HtmlBlockType.startingAt(cursor.line, cursor.nextNonspace, canInterruptParagraph) ?: return false
        closeUnmatchedBlocks()
        // Leading spaces are part of the HTML block, so the offset is not advanced.
        addChild(HtmlBlockNode(type))
        return true
    }

    private fun startSetextHeading(container: BlockNode): Boolean {
        if (cursor.indented || container !is ParagraphNode) return false
        val marker = cursor.charAt(cursor.nextNonspace)
        if (marker != '=' && marker != '-') return false
        var end = cursor.nextNonspace
        while (cursor.charAt(end) == marker) end++
        if (!cursor.isBlankFrom(end)) return false

        closeUnmatchedBlocks()
        // Link reference definitions at the start of the paragraph are not part of the heading.
        consumeLinkReferenceDefinitions(container)
        if (container.content.isEmpty()) return false

        val heading = HeadingNode(if (marker == '=') 1 else 2)
        heading.startLine = container.startLine
        heading.content.append(container.content)
        container.replaceWith(heading)
        tip = heading
        cursor.advanceToEnd()
        return true
    }

    /**
     * A GFM table starts at a delimiter row (`| --- | :-: |`) whose number of cells matches the header
     * row: the last line of the paragraph being continued. Earlier lines remain a paragraph.
     */
    private fun startTable(container: BlockNode): Boolean {
        if (!tables || cursor.indented || container !is ParagraphNode) return false
        val alignments = parseDelimiterRow(splitTableRow(cursor.line, cursor.nextNonspace)) ?: return false
        val content = container.content
        val headerStart = content.lastIndexOf('\n', content.length - 2) + 1
        val headerCells = splitTableRow(content.substring(headerStart, content.length - 1), 0)
        if (headerCells.size != alignments.size) return false

        closeUnmatchedBlocks()
        val table = TableNode(alignments, headerCells)
        if (headerStart == 0) {
            table.startLine = container.startLine
            container.replaceWith(table)
            tip = table
        } else {
            table.splitFromLine = container.startLine
            content.setLength(headerStart)
            finalize(container, lineNumber - 2)
            addChild(table)
            table.startLine = lineNumber - 1
        }
        cursor.advanceToEnd()
        return true
    }

    /** `[^label]:` starts a footnote definition; the rest of the line is its first content. */
    private fun startFootnoteDefinition(container: BlockNode): Boolean {
        val footnotes = footnotes ?: return false
        val start = cursor.nextNonspace
        if (cursor.indented || !canNestContainerIn(container) || !cursor.line.startsWith("[^", start)) return false
        var labelEnd = start + 2
        while (labelEnd < cursor.line.length && cursor.line[labelEnd] !in FOOTNOTE_LABEL_TERMINATORS) labelEnd++
        if (labelEnd == start + 2 || !cursor.line.startsWith("]:", labelEnd)) return false
        var contentStart = labelEnd + 2
        while (cursor.charAt(contentStart).isSpaceOrTab()) contentStart++

        closeUnmatchedBlocks()
        val label = cursor.line.substring(start + 2, labelEnd)
        cursor.advanceNextNonspace()
        cursor.advanceChars(contentStart - start)
        addChild(FootnoteDefinitionNode(label))
        footnotes.define(label)
        return true
    }

    private fun startThematicBreak(): Boolean {
        if (cursor.indented) return false
        val marker = cursor.charAt(cursor.nextNonspace)
        if (marker != '*' && marker != '-' && marker != '_') return false
        var count = 0
        for (i in cursor.nextNonspace until cursor.line.length) {
            val c = cursor.line[i]
            if (c == marker) {
                count++
            } else if (!c.isSpaceOrTab()) {
                return false
            }
        }
        if (count < MIN_THEMATIC_BREAK_MARKERS) return false
        closeUnmatchedBlocks()
        addChild(ThematicBreakNode())
        cursor.advanceToEnd()
        return true
    }

    private fun startListItem(container: BlockNode): Boolean {
        if (!canNestContainerIn(container) || (cursor.indented && container !is ListNode)) return false
        val data = parseListMarker(cursor, interruptsParagraph = container is ParagraphNode) ?: return false
        closeUnmatchedBlocks()
        val currentTip = tip
        if (currentTip !is ListNode || !currentTip.data.matches(data)) addChild(ListNode(data))
        addChild(ItemNode(data))
        return true
    }

    private fun startIndentedCodeBlock(): Boolean {
        if (!cursor.indented || tip is ParagraphNode || cursor.blank) return false
        cursor.advanceColumns(CODE_INDENT)
        closeUnmatchedBlocks()
        addChild(CodeBlockNode(isFenced = false))
        return true
    }

    /**
     * Whether a new block quote or list may open inside [container]. Nesting is capped at
     * [MAX_NESTING_DEPTH] so that hostile input cannot build trees deep enough to exhaust the stack
     * of code that walks them; beyond the cap, markers are treated as text.
     */
    private fun canNestContainerIn(container: BlockNode): Boolean = container.depth < MAX_NESTING_DEPTH

    private fun isLazyParagraphContinuation(): Boolean = !allClosed && !cursor.blank && tip is ParagraphNode

    /** Adds [child] to the tip, first closing blocks that cannot contain it. */
    private fun addChild(child: BlockNode) {
        while (!tip.canContain(child)) finalize(tip, lineNumber - 1)
        child.startLine = lineNumber
        tip.appendChild(child)
        tip = child
    }

    private fun addLineToTip() {
        val leaf = tip as LeafBlockNode
        // The rest of a partially consumed tab becomes the spaces it stands for.
        repeat(cursor.consumePartialTab()) { leaf.content.append(' ') }
        leaf.content.append(cursor.line, cursor.offset, cursor.line.length).append('\n')
    }

    /** Finalizes the blocks that were open but not matched by the current line. */
    private fun closeUnmatchedBlocks() {
        if (allClosed) return
        while (oldTip !== lastMatchedContainer) {
            val parent = checkNotNull(oldTip.parent)
            finalize(oldTip, lineNumber - 1)
            oldTip = parent
        }
        allClosed = true
    }

    private fun finalize(
        block: BlockNode,
        endLine: Int,
    ) {
        val parent = block.parent
        block.isOpen = false
        block.endLine = endLine
        when (block) {
            is ParagraphNode -> {
                consumeLinkReferenceDefinitions(block)
                if (block.content.all { it == ' ' || it == '\t' || it == '\n' }) block.unlink()
            }

            is CodeBlockNode -> {
                block.finalizeCode()
            }

            is HtmlBlockNode -> {
                block.literal = block.content.removeSuffix("\n").toString()
            }

            is ItemNode -> {
                block.finalizeItem()
            }

            is ListNode -> {
                block.finalizeList()
            }

            is TableNode -> {
                block.finalizeTable()
            }

            is DocumentNode, is BlockQuoteNode, is HeadingNode, is ThematicBreakNode, is FootnoteDefinitionNode -> {
                // These blocks have no content to post-process.
            }
        }
        if (parent != null) tip = parent
    }

    /** Parses link reference definitions at the start of [paragraph] and removes them from its content. */
    private fun consumeLinkReferenceDefinitions(paragraph: ParagraphNode) {
        while (paragraph.content.startsWith('[')) {
            val consumed = parseLinkReferenceDefinition(paragraph.content.toString(), references)
            if (consumed == 0) break
            paragraph.content.deleteRange(0, consumed)
        }
    }

    /** Fast check: only these characters can start a block when the line is not indented. */
    private fun Char.mayStartBlock(): Boolean =
        when (this) {
            // `|`, `:` and `[` can start table delimiter rows and footnote definitions.
            '#', '`', '~', '*', '+', '_', '=', '<', '>', '-', '|', ':', '[' -> true

            else -> isAsciiDigit()
        }
}

/** Removes an ATX heading's optional closing sequence of `#` characters. */
private fun stripClosingSequence(text: String): String {
    var end = text.length
    while (end > 0 && text[end - 1].isSpaceOrTab()) end--
    var hashStart = end
    while (hashStart > 0 && text[hashStart - 1] == '#') hashStart--
    return when {
        hashStart == end -> text
        hashStart == 0 -> ""
        !text[hashStart - 1].isSpaceOrTab() -> text
        else -> text.substring(0, hashStart).trimEnd(' ', '\t')
    }
}
