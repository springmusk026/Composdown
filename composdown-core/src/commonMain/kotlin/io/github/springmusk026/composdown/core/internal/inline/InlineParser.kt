package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.codePointAt
import io.github.springmusk026.composdown.core.internal.text.codePointBefore
import io.github.springmusk026.composdown.core.internal.text.isAsciiAlphanumeric
import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter
import io.github.springmusk026.composdown.core.internal.text.isAsciiPunctuation
import io.github.springmusk026.composdown.core.internal.text.isUnicodePunctuation
import io.github.springmusk026.composdown.core.internal.text.isUnicodeWhitespace
import io.github.springmusk026.composdown.core.internal.text.matchCharacterReference
import io.github.springmusk026.composdown.core.internal.text.normalizeLinkLabel

/** Maximum length of a link label including its brackets (999 characters inside). */
private const val MAX_REFERENCE_LABEL_LENGTH = 1001

/**
 * Parses the inline content of a paragraph or heading into an [InlineNode] tree, following the
 * CommonMark algorithm: a single left-to-right pass that records emphasis delimiters and link
 * openers on stacks, resolving emphasis when a link closes and at the end of the content.
 *
 * An instance can be reused for any number of [parse] calls, but not concurrently.
 */
internal class InlineParser(
    private val references: LinkReferenceMap,
) {
    private var subject: String = ""
    private var pos = 0
    private var delimiters: Delimiter? = null
    private var brackets: Bracket? = null
    private var backticks: BacktickIndex? = null
    private var htmlMemo = HtmlScanMemo()

    /** Parses [content] (raw paragraph or heading text) and returns a root node holding the result. */
    fun parse(content: String): InlineNode {
        subject = content.trim { it == ' ' || it == '\t' || it == '\n' }
        pos = 0
        delimiters = null
        brackets = null
        backticks = null
        htmlMemo = HtmlScanMemo()
        val root = InlineNode(InlineNode.Kind.ROOT)
        while (pos < subject.length) {
            parseInline(root)
        }
        processEmphasis(null)
        return root
    }

    private fun parseInline(block: InlineNode) {
        val handled =
            when (subject[pos]) {
                '\n' -> parseNewline(block)
                '\\' -> parseBackslash(block)
                '`' -> parseBackticks(block)
                '*', '_' -> handleDelimiter(subject[pos], block)
                '[' -> parseOpenBracket(block)
                '!' -> parseBang(block)
                ']' -> parseCloseBracket(block)
                '<' -> parseAutolink(block) || parseHtmlTag(block)
                '&' -> parseEntity(block)
                else -> parseString(block)
            }
        if (!handled) {
            block.appendChild(text(subject[pos].toString()))
            pos++
        }
    }

    private fun text(literal: String) = InlineNode(InlineNode.Kind.TEXT, literal)

    private fun parseString(block: InlineNode): Boolean {
        val start = pos
        while (pos < subject.length && !subject[pos].isSpecial()) pos++
        if (pos == start) return false
        block.appendChild(text(subject.substring(start, pos)))
        return true
    }

    private fun Char.isSpecial(): Boolean =
        when (this) {
            '\n', '`', '[', ']', '\\', '!', '<', '&', '*', '_' -> true
            else -> false
        }

    private fun parseNewline(block: InlineNode): Boolean {
        pos++
        val last = block.lastChild
        if (last != null && last.kind == InlineNode.Kind.TEXT && last.literal.endsWith(' ')) {
            val hardBreak = last.literal.endsWith("  ")
            last.literal = last.literal.trimEnd(' ')
            block.appendChild(InlineNode(if (hardBreak) InlineNode.Kind.HARD_BREAK else InlineNode.Kind.SOFT_BREAK))
        } else {
            block.appendChild(InlineNode(InlineNode.Kind.SOFT_BREAK))
        }
        while (subject.charAtOrEnd(pos) == ' ') pos++
        return true
    }

    private fun parseBackslash(block: InlineNode): Boolean {
        pos++
        val next = subject.charAtOrEnd(pos)
        when {
            next == '\n' -> {
                pos++
                block.appendChild(InlineNode(InlineNode.Kind.HARD_BREAK))
            }

            next.isAsciiPunctuation() -> {
                block.appendChild(text(next.toString()))
                pos++
            }

            else -> {
                block.appendChild(text("\\"))
            }
        }
        return true
    }

    private fun parseBackticks(block: InlineNode): Boolean {
        val start = pos
        while (subject.charAtOrEnd(pos) == '`') pos++
        val length = pos - start
        val index = backticks ?: BacktickIndex(subject).also { backticks = it }
        val closer = index.findRun(length, pos)
        if (closer < 0) {
            block.appendChild(text(subject.substring(start, pos)))
            return true
        }
        val contents = subject.substring(pos, closer).replace('\n', ' ')
        block.appendChild(InlineNode(InlineNode.Kind.CODE, stripCodeSpanSpace(contents)))
        pos = closer + length
        return true
    }

    private fun handleDelimiter(
        delimiterChar: Char,
        block: InlineNode,
    ): Boolean {
        val start = pos
        while (subject.charAtOrEnd(pos) == delimiterChar) pos++
        val count = pos - start

        val before = subject.codePointBefore(start).let { if (it < 0) '\n'.code else it }
        val after = subject.codePointAt(pos).let { if (it < 0) '\n'.code else it }
        val run = DelimiterRun.classify(delimiterChar, before, after)

        val node = text(subject.substring(start, pos))
        block.appendChild(node)
        if (run.canOpen || run.canClose) {
            val delimiter = Delimiter(delimiterChar, count, node, run.canOpen, run.canClose, previous = delimiters)
            delimiters?.next = delimiter
            delimiters = delimiter
        }
        return true
    }

    private fun parseOpenBracket(block: InlineNode): Boolean {
        val node = text("[")
        block.appendChild(node)
        addBracket(node, pos, image = false)
        pos++
        return true
    }

    private fun parseBang(block: InlineNode): Boolean {
        if (subject.charAtOrEnd(pos + 1) == '[') {
            val node = text("![")
            block.appendChild(node)
            addBracket(node, pos + 1, image = true)
            pos += 2
        } else {
            block.appendChild(text("!"))
            pos++
        }
        return true
    }

    private fun addBracket(
        node: InlineNode,
        index: Int,
        image: Boolean,
    ) {
        brackets?.bracketAfter = true
        brackets = Bracket(node, brackets, delimiters, index, image)
    }

    private fun parseCloseBracket(block: InlineNode): Boolean {
        pos++
        val afterBracket = pos
        val opener = brackets
        if (opener == null) {
            block.appendChild(text("]"))
            return true
        }
        if (!opener.active) {
            block.appendChild(text("]"))
            brackets = opener.previous
            return true
        }

        val target = parseInlineLinkTarget() ?: parseReferenceLinkTarget(opener, afterBracket)
        if (target == null) {
            brackets = opener.previous
            pos = afterBracket
            block.appendChild(text("]"))
            return true
        }

        val link = InlineNode(if (opener.image) InlineNode.Kind.IMAGE else InlineNode.Kind.LINK)
        link.destination = target.destination
        link.title = target.title
        var child = opener.node.next
        while (child != null) {
            val next = child.next
            link.appendChild(child)
            child = next
        }
        block.appendChild(link)
        processEmphasis(opener.previousDelimiter)
        brackets = opener.previous
        opener.node.unlink()

        // Links cannot contain other links, so earlier link openers become inactive.
        if (!opener.image) {
            var earlier = brackets
            while (earlier != null) {
                if (!earlier.image) earlier.active = false
                earlier = earlier.previous
            }
        }
        return true
    }

    /** Parses `(destination "title")` at [pos]. On success advances [pos] past `)`. */
    private fun parseInlineLinkTarget(): LinkReference? {
        if (subject.charAtOrEnd(pos) != '(') return null
        var i = skipSpacesTabsAndOneNewline(subject, pos + 1)
        val destination = scanLinkDestination(subject, i) ?: return null
        i = destination.end
        val beforeTitle = i
        i = skipSpacesTabsAndOneNewline(subject, i)
        var title: String? = null
        if (i != beforeTitle) {
            val scannedTitle = scanLinkTitle(subject, i)
            if (scannedTitle != null) {
                title = scannedTitle.value
                i = skipSpacesTabsAndOneNewline(subject, scannedTitle.end)
            }
        }
        if (subject.charAtOrEnd(i) != ')') return null
        pos = i + 1
        return LinkReference(destination.value, title)
    }

    /**
     * Resolves a full (`[text][label]`), collapsed (`[text][]`) or shortcut (`[text]`) reference link.
     * On success advances [pos] past the second label, if any.
     */
    private fun parseReferenceLinkTarget(
        opener: Bracket,
        afterBracket: Int,
    ): LinkReference? {
        val labelEnd = scanLinkLabel(subject, afterBracket)
        val labelLength = if (labelEnd < 0) 0 else labelEnd - afterBracket
        val label: CharSequence? =
            when {
                labelLength > 2 -> {
                    subject.subSequence(afterBracket + 1, labelEnd - 1)
                }

                // A collapsed or shortcut reference uses the link text as the label. It cannot
                // contain brackets; if one was seen, there is no need to look it up.
                !opener.bracketAfter && afterBracket - opener.index <= MAX_REFERENCE_LABEL_LENGTH -> {
                    subject.subSequence(opener.index + 1, afterBracket - 1)
                }

                else -> {
                    null
                }
            }
        val reference = label?.let { references[normalizeLinkLabel(it)] } ?: return null
        pos = if (labelLength > 0) labelEnd else afterBracket
        return reference
    }

    private fun parseAutolink(block: InlineNode): Boolean {
        val end = scanUriAutolink(subject, pos)
        val isEmail: Boolean
        val linkEnd: Int
        if (end >= 0) {
            isEmail = false
            linkEnd = end
        } else {
            linkEnd = scanEmailAutolink(subject, pos)
            if (linkEnd < 0) return false
            isEmail = true
        }
        val address = subject.substring(pos + 1, linkEnd - 1)
        val link = InlineNode(InlineNode.Kind.LINK)
        link.destination = if (isEmail) "mailto:$address" else address
        link.appendChild(text(address))
        block.appendChild(link)
        pos = linkEnd
        return true
    }

    private fun parseHtmlTag(block: InlineNode): Boolean {
        val end =
            when (subject.charAtOrEnd(pos + 1)) {
                '/' -> scanClosingTag(subject, pos)
                '!', '?' -> scanHtmlSpecial(subject, pos, htmlMemo)
                else -> scanOpenTag(subject, pos)
            }
        if (end < 0) return false
        block.appendChild(InlineNode(InlineNode.Kind.HTML, subject.substring(pos, end)))
        pos = end
        return true
    }

    private fun parseEntity(block: InlineNode): Boolean {
        val reference = matchCharacterReference(subject, pos) ?: return false
        block.appendChild(text(reference.replacement))
        pos = reference.end
        return true
    }

    /**
     * Resolves emphasis between the delimiters above [stackBottom], then removes them from the stack.
     * This is the "process emphasis" procedure from the CommonMark specification.
     */
    private fun processEmphasis(stackBottom: Delimiter?) {
        // Lower bounds for opener searches, per closer type, so failed searches are not repeated.
        val openersBottom = arrayOfNulls<Delimiter>(OPENERS_BOTTOM_SLOTS)
        openersBottom.fill(stackBottom)

        var closer = delimiters
        while (closer != null && closer.previous !== stackBottom) closer = closer.previous

        while (closer != null) {
            if (!closer.canClose) {
                closer = closer.next
                continue
            }
            val bottomIndex = openersBottomIndex(closer)
            val opener = findOpener(closer, stackBottom, openersBottom[bottomIndex])

            val currentCloser: Delimiter = closer
            if (opener != null) {
                closer = applyEmphasis(opener, currentCloser)
            } else {
                closer = currentCloser.next
                openersBottom[bottomIndex] = currentCloser.previous
                // A closer that cannot open has no further use once no opener matched it.
                if (!currentCloser.canOpen) removeDelimiter(currentCloser)
            }
        }

        while (delimiters != null && delimiters !== stackBottom) removeDelimiter(checkNotNull(delimiters))
    }

    /**
     * Searches back from [closer] for the nearest delimiter that can open emphasis with it, stopping at
     * [stackBottom] or [searchBottom]. Implements the "multiple of 3" rule for runs that can both open
     * and close.
     */
    private fun findOpener(
        closer: Delimiter,
        stackBottom: Delimiter?,
        searchBottom: Delimiter?,
    ): Delimiter? {
        var opener = closer.previous
        while (opener != null && opener !== stackBottom && opener !== searchBottom) {
            val oddMatch =
                (closer.canOpen || opener.canClose) &&
                    closer.originalCount % RUN_LENGTH_MODULUS != 0 &&
                    (opener.originalCount + closer.originalCount) % RUN_LENGTH_MODULUS == 0
            if (opener.char == closer.char && opener.canOpen && !oddMatch) return opener
            opener = opener.previous
        }
        return null
    }

    /** Wraps the nodes between [opener] and [closer] in emphasis; returns the next closer to process. */
    private fun applyEmphasis(
        opener: Delimiter,
        closer: Delimiter,
    ): Delimiter? {
        val used = if (closer.count >= 2 && opener.count >= 2) 2 else 1
        opener.count -= used
        closer.count -= used
        opener.node.literal = opener.node.literal.dropLast(used)
        closer.node.literal = closer.node.literal.dropLast(used)

        val emphasis = InlineNode(if (used == 1) InlineNode.Kind.EMPHASIS else InlineNode.Kind.STRONG)
        var child = opener.node.next
        while (child != null && child !== closer.node) {
            val next = child.next
            emphasis.appendChild(child)
            child = next
        }
        opener.node.insertAfter(emphasis)

        // Delimiters between the opener and closer can no longer match anything.
        if (opener.next !== closer) {
            opener.next = closer
            closer.previous = opener
        }
        if (opener.count == 0) {
            opener.node.unlink()
            removeDelimiter(opener)
        }
        if (closer.count == 0) {
            closer.node.unlink()
            val next = closer.next
            removeDelimiter(closer)
            return next
        }
        return closer
    }

    private fun openersBottomIndex(closer: Delimiter): Int {
        val base = if (closer.char == '_') UNDERSCORE_SLOTS_START else ASTERISK_SLOTS_START
        return base + (if (closer.canOpen) RUN_LENGTH_MODULUS else 0) + closer.originalCount % RUN_LENGTH_MODULUS
    }

    private fun removeDelimiter(delimiter: Delimiter) {
        delimiter.previous?.next = delimiter.next
        val next = delimiter.next
        if (next == null) {
            delimiters = delimiter.previous
        } else {
            next.previous = delimiter.previous
        }
    }

    private class Delimiter(
        val char: Char,
        var count: Int,
        val node: InlineNode,
        val canOpen: Boolean,
        val canClose: Boolean,
        var previous: Delimiter?,
    ) {
        val originalCount: Int = count
        var next: Delimiter? = null
    }

    private class Bracket(
        val node: InlineNode,
        val previous: Bracket?,
        val previousDelimiter: Delimiter?,
        /** Index of the `[` in the subject. */
        val index: Int,
        val image: Boolean,
    ) {
        var active = true

        /** Whether another bracket opener follows this one, so it cannot be a shortcut reference label. */
        var bracketAfter = false
    }

    private companion object {
        /** The "multiple of 3" rule: run lengths are compared modulo 3 when matching emphasis. */
        const val RUN_LENGTH_MODULUS = 3

        /** Two closer characters, each with (can open: 2) x (length mod 3: 3) combinations. */
        const val OPENERS_BOTTOM_SLOTS = 12
        const val UNDERSCORE_SLOTS_START = 0
        const val ASTERISK_SLOTS_START = 6
    }
}

/**
 * Positions of every backtick run in a text, grouped by run length, so that finding the closing run
 * of a code span is fast and the whole paragraph is scanned only once.
 */
private class BacktickIndex(
    text: String,
) {
    private val runsByLength = HashMap<Int, MutableList<Int>>()
    private val cursors = HashMap<Int, Int>()

    init {
        var i = 0
        while (i < text.length) {
            if (text[i] == '`') {
                val start = i
                while (i < text.length && text[i] == '`') i++
                runsByLength.getOrPut(i - start) { ArrayList() }.add(start)
            } else {
                i++
            }
        }
    }

    /** Returns the start of the first run of exactly [length] backticks at or after [from], or -1. */
    fun findRun(
        length: Int,
        from: Int,
    ): Int {
        val runs = runsByLength[length] ?: return -1
        // Searches for a given length only ever move forward, so a cursor keeps this linear overall.
        var cursor = cursors[length] ?: 0
        while (cursor < runs.size && runs[cursor] < from) cursor++
        cursors[length] = cursor
        return if (cursor < runs.size) runs[cursor] else -1
    }
}

/**
 * Removes one space from both ends of code span content, if it both begins and ends with a space and
 * is not made of spaces only.
 */
private fun stripCodeSpanSpace(contents: String): String {
    val surroundedBySpaces = contents.length >= 2 && contents.first() == ' ' && contents.last() == ' '
    return if (surroundedBySpaces &&
        contents.any { it != ' ' }
    ) {
        contents.substring(1, contents.length - 1)
    } else {
        contents
    }
}
