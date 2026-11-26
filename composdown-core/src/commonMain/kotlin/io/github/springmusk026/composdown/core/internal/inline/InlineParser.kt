package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.codePointAt
import io.github.springmusk026.composdown.core.internal.text.codePointBefore
import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter
import io.github.springmusk026.composdown.core.internal.text.isAsciiPunctuation
import io.github.springmusk026.composdown.core.internal.text.matchCharacterReference
import io.github.springmusk026.composdown.core.internal.text.normalizeLinkLabel

/** Maximum length of a link label including its brackets (999 characters inside). */
private const val MAX_REFERENCE_LABEL_LENGTH = 1001

/**
 * Parses the inline content of a paragraph, heading or table cell into an [InlineNode] tree,
 * following the CommonMark algorithm: a single left-to-right pass that records emphasis delimiters
 * and link openers on stacks, resolving emphasis when a link closes and at the end of the content.
 *
 * An instance can be reused for any number of [parse] calls, but not concurrently.
 *
 * @param footnotes the footnote definitions of the document, or `null` if footnotes are disabled.
 */
internal class InlineParser(
    private val references: LinkReferenceMap,
    extensions: Set<MarkdownExtension>,
    private val footnotes: FootnoteRegistry?,
) {
    private val strikethrough = MarkdownExtension.STRIKETHROUGH in extensions
    private val autolinks = MarkdownExtension.AUTOLINKS in extensions

    private var subject: String = ""
    private var pos = 0
    private var delimiters: Delimiter? = null
    private var brackets: Bracket? = null
    private var backticks: BacktickIndex? = null
    private var htmlMemo = HtmlScanMemo()
    private var optimistic = false

    /** Where to record unresolved labels and footnote references, if anywhere. */
    var trace: ConversionTrace? = null

    /**
     * Parses [content] (raw paragraph or heading text) and returns a root node holding the result.
     *
     * With [optimistic] set, [content] is treated as text that is still being written: constructs
     * that are open at the end are rendered as if they were closed (`**bold` as strong emphasis,
     * `` `code`` as a code span, `[text](partial` as a link), and a trailing run of delimiter
     * characters, which may still become an opener or closer, is held back. This avoids flicker while
     * streaming; the final parse of the complete text is never optimistic.
     */
    fun parse(
        content: String,
        optimistic: Boolean = false,
    ): InlineNode {
        val trimmed = content.trim { it == ' ' || it == '\t' || it == '\n' }
        subject = if (optimistic) withoutTrailingDelimiters(trimmed) else trimmed
        this.optimistic = optimistic
        pos = 0
        delimiters = null
        brackets = null
        backticks = null
        htmlMemo = HtmlScanMemo()
        val root = InlineNode(InlineNode.Kind.ROOT)
        while (pos < subject.length) {
            parseInline(root)
        }
        if (optimistic) closeUnterminatedDelimiters(root) else processEmphasis(null)
        return root
    }

    /**
     * Matches every opener left on the delimiter stack with a synthetic closer at the end of the
     * content, innermost first, then removes any synthetic closer that still did not match.
     */
    private fun closeUnterminatedDelimiters(root: InlineNode) {
        processEmphasis(null, clearRemaining = false)
        val synthetic = ArrayList<InlineNode>()
        var delimiter = delimiters
        while (delimiter != null) {
            val opener = delimiter
            delimiter = opener.previous
            if (!opener.canOpen) continue
            val node = text(opener.char.toString().repeat(opener.count))
            root.appendChild(node)
            synthetic.add(node)
            val closer =
                Delimiter(opener.char, opener.count, node, canOpen = false, canClose = true, previous = delimiters)
            delimiters?.next = closer
            delimiters = closer
        }
        processEmphasis(null)
        synthetic.forEach { it.unlink() }
    }

    private fun parseInline(block: InlineNode) {
        val c = subject[pos]
        val handled =
            when {
                c == '\n' -> parseNewline(block)

                c == '\\' -> parseBackslash(block)

                c == '`' -> parseBackticks(block)

                c == '*' || c == '_' || (c == '~' && strikethrough) -> handleDelimiter(c, block)

                c == '[' -> parseOpenBracket(block)

                c == '!' -> parseBang(block)

                c == ']' -> parseCloseBracket(block)

                c == '<' -> parseAutolink(block) || parseHtmlTag(block)

                c == '&' -> parseEntity(block)

                // Extended autolinks are not recognized inside link text.
                autolinks && brackets == null && c == ':' -> consumeAutolink(parseUrlAutolink(block, subject, pos))

                autolinks && brackets == null && c == '@' -> consumeAutolink(parseEmailAutolink(block, subject, pos))

                autolinks && brackets == null && c == 'w' -> consumeAutolink(parseWwwAutolink(block, subject, pos))

                else -> parseString(block)
            }
        if (!handled) {
            block.appendChild(text(c.toString()))
            pos++
        }
    }

    private fun parseString(block: InlineNode): Boolean {
        val start = pos
        while (pos < subject.length && !isSpecial(pos)) pos++
        if (pos == start) return false
        block.appendChild(text(subject.substring(start, pos)))
        return true
    }

    /** Whether the character at [index] may start an inline construct, so plain text stops there. */
    private fun isSpecial(index: Int): Boolean =
        when (subject[index]) {
            '\n', '`', '[', ']', '\\', '!', '<', '&', '*', '_' -> true
            '~' -> strikethrough
            ':', '@' -> autolinks
            'w' -> autolinks && ExtendedAutolinks.mayStartWwwAt(subject, index)
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
        if (closer < 0 && optimistic) {
            // A code span still being written extends to the end of the content.
            block.appendChild(InlineNode(InlineNode.Kind.CODE, subject.substring(pos).replace('\n', ' ')))
            pos = subject.length
            return true
        }
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
            node.isActiveDelimiter = true
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
        if (opener == null || !opener.active) {
            block.appendChild(text("]"))
            if (opener != null) brackets = opener.previous
            return true
        }

        val target = parseInlineLinkTarget() ?: parseReferenceLinkTarget(opener, afterBracket)
        if (target != null) {
            closeLink(block, opener, target)
        } else if (parseFootnoteReference(block, opener, afterBracket)) {
            // Handled as a footnote reference.
        } else if (optimistic && isUnterminatedInlineLink(subject, afterBracket)) {
            // `[text](partial` while the destination is still being written renders as a link.
            closeLink(block, opener, LinkReference("", null))
            pos = subject.length
        } else {
            brackets = opener.previous
            pos = afterBracket
            block.appendChild(text("]"))
        }
        return true
    }

    private fun closeLink(
        block: InlineNode,
        opener: Bracket,
        target: LinkReference,
    ) {
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
        val normalized = label?.let(::normalizeLinkLabel) ?: return null
        val reference = references[normalized]
        if (reference == null) {
            trace?.linkMisses?.add(normalized)
            return null
        }
        pos = if (labelLength > 0) labelEnd else afterBracket
        return reference
    }

    /**
     * Turns `[^label]` into a footnote reference, as cmark-gfm does when no link matched. A reference
     * to an undefined footnote becomes the literal text `[^label]`. Returns false if the brackets do
     * not hold a footnote label or footnotes are disabled.
     */
    private fun parseFootnoteReference(
        block: InlineNode,
        opener: Bracket,
        afterBracket: Int,
    ): Boolean {
        val footnotes = footnotes ?: return false
        val first = opener.node.next ?: return false
        val isLabel =
            first.kind == InlineNode.Kind.TEXT && first.literal.startsWith('^') &&
                (first.literal.length > 1 || first.next != null)
        if (!isLabel) return false

        val rawLabel = subject.substring(opener.index + 2, afterBracket - 1)
        processEmphasis(opener.previousDelimiter)
        brackets = opener.previous
        while (true) {
            val node = opener.node.next ?: break
            node.unlink()
        }
        opener.node.unlink()
        // For `![^label]`, the `!` is plain text.
        if (opener.image) block.appendChild(text("!"))
        val reference = footnotes.reference(rawLabel)
        val key = normalizeLinkLabel(rawLabel)
        if (reference == null) {
            trace?.footnoteMisses?.add(key)
        } else {
            trace?.footnoteKeys?.add(key)
            trace?.footnoteNumbers?.add(reference.second)
        }
        if (reference != null) {
            block.appendChild(
                InlineNode(InlineNode.Kind.FOOTNOTE_REFERENCE, reference.first).also {
                    it.number =
                        reference.second
                },
            )
        } else {
            block.appendChild(text("[^$rawLabel]"))
        }
        pos = afterBracket
        return true
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
        block.appendLink(if (isEmail) "mailto:$address" else address, address)
        pos = linkEnd
        return true
    }

    /** Advances past an extended autolink that ends at [end], if one was found (`end >= 0`). */
    private fun consumeAutolink(end: Int): Boolean {
        if (end < 0) return false
        pos = end
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
     * Resolves emphasis and strikethrough between the delimiters above [stackBottom], then removes
     * them from the stack. This is the "process emphasis" procedure from the CommonMark specification.
     */
    private fun processEmphasis(
        stackBottom: Delimiter?,
        clearRemaining: Boolean = true,
    ) {
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
            val opener = if (bottomIndex < 0) null else findOpener(closer, stackBottom, openersBottom[bottomIndex])

            val currentCloser: Delimiter = closer
            if (opener != null) {
                closer =
                    if (currentCloser.char ==
                        '~'
                    ) {
                        applyStrikethrough(opener, currentCloser)
                    } else {
                        applyEmphasis(opener, currentCloser)
                    }
            } else {
                closer = currentCloser.next
                if (bottomIndex >= 0) openersBottom[bottomIndex] = currentCloser.previous
                // A closer that cannot open has no further use once no opener matched it.
                if (!currentCloser.canOpen) removeDelimiter(currentCloser)
            }
        }

        if (clearRemaining) {
            while (delimiters != null && delimiters !== stackBottom) removeDelimiter(checkNotNull(delimiters))
        }
    }

    /**
     * Searches back from [closer] for the nearest delimiter that can open with it, stopping at
     * [stackBottom] or [searchBottom]. Emphasis follows the "multiple of 3" rule for runs that can
     * both open and close; strikethrough needs runs of equal length.
     */
    private fun findOpener(
        closer: Delimiter,
        stackBottom: Delimiter?,
        searchBottom: Delimiter?,
    ): Delimiter? {
        var opener = closer.previous
        while (opener != null && opener !== stackBottom && opener !== searchBottom) {
            if (opener.char == closer.char && opener.canOpen && canPair(opener, closer)) return opener
            opener = opener.previous
        }
        return null
    }

    private fun canPair(
        opener: Delimiter,
        closer: Delimiter,
    ): Boolean {
        if (closer.char == '~') return opener.count == closer.count
        val oddMatch =
            (closer.canOpen || opener.canClose) &&
                closer.originalCount % RUN_LENGTH_MODULUS != 0 &&
                (opener.originalCount + closer.originalCount) % RUN_LENGTH_MODULUS == 0
        return !oddMatch
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

        wrapBetween(opener, closer, InlineNode(if (used == 1) InlineNode.Kind.EMPHASIS else InlineNode.Kind.STRONG))
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

    /** Wraps the nodes between [opener] and [closer] in strikethrough; both delimiters are consumed. */
    private fun applyStrikethrough(
        opener: Delimiter,
        closer: Delimiter,
    ): Delimiter? {
        wrapBetween(opener, closer, InlineNode(InlineNode.Kind.STRIKETHROUGH))
        opener.node.unlink()
        removeDelimiter(opener)
        closer.node.unlink()
        val next = closer.next
        removeDelimiter(closer)
        return next
    }

    /** Moves the nodes between the delimiters into [container], placed after the opener's node. */
    private fun wrapBetween(
        opener: Delimiter,
        closer: Delimiter,
        container: InlineNode,
    ) {
        var child = opener.node.next
        while (child != null && child !== closer.node) {
            val next = child.next
            container.appendChild(child)
            child = next
        }
        opener.node.insertAfter(container)

        // Delimiters between the opener and closer can no longer match anything.
        if (opener.next !== closer) {
            var between = opener.next
            while (between != null && between !== closer) {
                between.node.isActiveDelimiter = false
                between = between.next
            }
            opener.next = closer
            closer.previous = opener
        }
    }

    /**
     * The slot in the openers-bottom table for this kind of closer, or -1 if the closer can never
     * match (a strikethrough run longer than two tildes).
     */
    private fun openersBottomIndex(closer: Delimiter): Int =
        when (closer.char) {
            '~' -> {
                if (closer.count <= 2) TILDE_SLOTS_START + closer.count - 1 else -1
            }

            else -> {
                val base = if (closer.char == '_') UNDERSCORE_SLOTS_START else ASTERISK_SLOTS_START
                base + (if (closer.canOpen) RUN_LENGTH_MODULUS else 0) + closer.originalCount % RUN_LENGTH_MODULUS
            }
        }

    private fun removeDelimiter(delimiter: Delimiter) {
        delimiter.node.isActiveDelimiter = false
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

        /**
         * `_` and `*` closers each use 6 slots: (can open: 2) x (length mod 3: 3). `~` closers use one
         * slot per run length (1 or 2), since only runs of equal length pair.
         */
        const val OPENERS_BOTTOM_SLOTS = 14
        const val UNDERSCORE_SLOTS_START = 0
        const val ASTERISK_SLOTS_START = 6
        const val TILDE_SLOTS_START = 12
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

private fun text(literal: String) = InlineNode(InlineNode.Kind.TEXT, literal)

/** Characters held back at the end of optimistically parsed content. */
private const val TRAILING_DELIMITERS = "*_~`"

/**
 * Drops a trailing run of delimiter characters (and the whitespace before it), unless the run starts
 * with a backslash-escaped character.
 */
private fun withoutTrailingDelimiters(text: String): String {
    val runStart = text.dropLastWhile { it in TRAILING_DELIMITERS }.length
    if (runStart == text.length || (runStart > 0 && text[runStart - 1] == '\\')) return text
    return text.substring(0, runStart).trimEnd(' ', '\t')
}

/** Whether the text after `]` at [afterBracket] is `(` with no closing `)` anywhere after it. */
private fun isUnterminatedInlineLink(
    subject: String,
    afterBracket: Int,
): Boolean = subject.charAtOrEnd(afterBracket) == '(' && subject.indexOf(')', afterBracket) < 0
