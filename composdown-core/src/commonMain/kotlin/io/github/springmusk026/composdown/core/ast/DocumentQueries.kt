package io.github.springmusk026.composdown.core.ast

import io.github.springmusk026.composdown.core.internal.Poko

/**
 * Every block of this document in document order, depth first: each block is followed by the
 * blocks it contains (in block quotes, alerts and list items), and the blocks of the
 * [footnotes][MarkdownDocument.footnotes] come last.
 *
 * ```
 * val codeLanguages = document.allBlocks().filterIsInstance<Block.CodeBlock>().mapNotNull { it.language }
 * ```
 */
public fun MarkdownDocument.allBlocks(): Sequence<Block> =
    sequence {
        for (block in blocks) yieldBlock(block)
        for (footnote in footnotes) footnote.children.forEach { yieldBlock(it) }
    }

/**
 * Every inline element of this document in document order, depth first: each element is followed
 * by the elements it contains (the text of emphasis, links and image descriptions), across
 * paragraphs, headings, table cells and footnotes.
 *
 * ```
 * val links = document.allInlines().filterIsInstance<Inline.Link>().map { it.destination }.toList()
 * ```
 */
public fun MarkdownDocument.allInlines(): Sequence<Inline> =
    sequence {
        for (block in allBlocks()) {
            for (content in block.inlineContent()) content.forEach { yieldInline(it) }
        }
    }

/**
 * A heading of a document, with the anchor that identifies it. See [headings].
 *
 * @property level the heading level, 1 to 6.
 * @property text the heading's text without formatting.
 * @property anchor an identifier for links to the heading (`#anchor`), computed as GitHub does: the
 *   text in lower case (image descriptions excluded), without punctuation and emoji, with spaces
 *   replaced by hyphens, and with `-1`, `-2`, ... appended to repeated anchors.
 * @property block the heading block.
 */
@Poko
public class DocumentHeading internal constructor(
    public val level: Int,
    public val text: String,
    public val anchor: String,
    public val block: Block.Heading,
)

/**
 * The headings of this document in document order, including those inside block quotes, alerts and
 * lists, followed by those inside [footnotes][MarkdownDocument.footnotes], with GitHub-compatible
 * anchors. Use it to build a table of contents, or to resolve links
 * such as `[Installation](#installation)`.
 *
 * ```
 * for (heading in document.headings()) {
 *     println("  ".repeat(heading.level - 1) + "- [${heading.text}](#${heading.anchor})")
 * }
 * ```
 */
public fun MarkdownDocument.headings(): List<DocumentHeading> {
    val anchors = AnchorGenerator()
    return allBlocks()
        .filterIsInstance<Block.Heading>()
        .map { heading ->
            DocumentHeading(
                heading.level,
                heading.content.plainText(),
                anchors.next(heading.content.anchorText()),
                heading,
            )
        }.toList()
}

/**
 * The text of this document without formatting, for example for previews, notifications, search
 * indexes or copying.
 *
 * Blocks are separated by a blank line; list items, table rows and lines of code by a line break;
 * table cells by a tab. Thematic breaks and raw HTML are omitted, footnotes follow the content, and
 * math is kept as its TeX source.
 */
public fun MarkdownDocument.plainText(): String =
    buildString {
        val paragraphs = blocks.mapNotNull { it.plainText() } + footnotes.mapNotNull { it.children.plainText() }
        paragraphs.filter { it.isNotEmpty() }.joinTo(this, separator = "\n\n")
    }

private suspend fun SequenceScope<Block>.yieldBlock(block: Block) {
    yield(block)
    when (block) {
        is Block.BlockQuote -> block.children.forEach { yieldBlock(it) }

        is Block.Alert -> block.children.forEach { yieldBlock(it) }

        is Block.ListBlock -> block.items.forEach { item -> item.children.forEach { yieldBlock(it) } }

        is Block.Paragraph, is Block.Heading, is Block.CodeBlock, is Block.Table, is Block.MathBlock,
        is Block.HtmlBlock, Block.ThematicBreak,
        -> Unit
    }
}

private suspend fun SequenceScope<Inline>.yieldInline(inline: Inline) {
    yield(inline)
    when (inline) {
        is Inline.Emphasis -> inline.children.forEach { yieldInline(it) }

        is Inline.Strong -> inline.children.forEach { yieldInline(it) }

        is Inline.Strikethrough -> inline.children.forEach { yieldInline(it) }

        is Inline.Link -> inline.children.forEach { yieldInline(it) }

        is Inline.Image -> inline.children.forEach { yieldInline(it) }

        is Inline.Text, is Inline.Code, is Inline.FootnoteReference, is Inline.Math, is Inline.HtmlInline,
        Inline.SoftBreak, Inline.HardBreak,
        -> Unit
    }
}

/** The text a heading's anchor is made from: its plain text without image descriptions. */
private fun List<Inline>.anchorText(): String =
    buildString {
        fun visit(inlines: List<Inline>) {
            for (inline in inlines) {
                when (inline) {
                    is Inline.Image -> Unit
                    is Inline.Emphasis -> visit(inline.children)
                    is Inline.Strong -> visit(inline.children)
                    is Inline.Strikethrough -> visit(inline.children)
                    is Inline.Link -> visit(inline.children)
                    else -> append(listOf(inline).plainText())
                }
            }
        }
        visit(this@anchorText)
    }

/** The inline content held directly by this block: one list per paragraph, heading or table cell. */
private fun Block.inlineContent(): List<List<Inline>> =
    when (this) {
        is Block.Paragraph -> listOf(content)
        is Block.Heading -> listOf(content)
        is Block.Table -> (listOf(header) + rows).flatMap { row -> row.cells.map { it.content } }
        else -> emptyList()
    }

/** The plain text of a block, or `null` if it has none (thematic breaks and raw HTML). */
private fun Block.plainText(): String? =
    when (this) {
        is Block.Paragraph -> {
            content.plainText()
        }

        is Block.Heading -> {
            content.plainText()
        }

        is Block.CodeBlock -> {
            literal.removeSuffix("\n")
        }

        is Block.MathBlock -> {
            literal.removeSuffix("\n")
        }

        is Block.BlockQuote -> {
            children.plainText()
        }

        is Block.Alert -> {
            children.plainText()
        }

        is Block.ListBlock -> {
            items
                .map { item ->
                    item.children.plainText(separator = "\n")
                }.filter { it.isNotEmpty() }
                .joinToString("\n")
        }

        is Block.Table -> {
            (
                listOf(
                    header,
                ) + rows
            ).joinToString("\n") { row -> row.cells.joinToString("\t") { it.content.plainText() } }
        }

        Block.ThematicBreak, is Block.HtmlBlock -> {
            null
        }
    }

private fun List<Block>.plainText(separator: String = "\n\n"): String =
    mapNotNull { it.plainText() }.filter { it.isNotEmpty() }.joinToString(separator)

/**
 * Generates unique heading anchors the way GitHub does (github-slugger): lower case, letters, digits,
 * marks, `_` and `-` kept, spaces turned into `-`, everything else removed; a repeated anchor gets
 * the next free `-1`, `-2`, ... suffix.
 */
private class AnchorGenerator {
    private val used = HashSet<String>()
    private val occurrences = HashMap<String, Int>()

    fun next(text: String): String {
        val base = slug(text)
        var anchor = base
        if (anchor in used) {
            var count = occurrences[base] ?: 0
            do {
                count++
                anchor = "$base-$count"
            } while (anchor in used)
            occurrences[base] = count
        }
        used += anchor
        return anchor
    }

    private fun slug(text: String): String =
        buildString {
            val lower = text.lowercase()
            var i = 0
            while (i < lower.length) {
                val c = lower[i]
                if (c.isHighSurrogate() && i + 1 < lower.length && lower[i + 1].isLowSurrogate()) {
                    // Letters and digits outside the Basic Multilingual Plane are kept; emoji and other
                    // symbols there are removed, as GitHub does.
                    if (!isSupplementarySymbol(c, lower[i + 1])) append(lower, i, i + 2)
                    i += 2
                    continue
                }
                when {
                    c == ' ' -> append('-')
                    c == '-' || c.isLetterOrDigit() || c.category in KEPT_CATEGORIES -> append(c)
                }
                i++
            }
        }

    private fun isSupplementarySymbol(
        high: Char,
        low: Char,
    ): Boolean {
        val codePoint =
            SUPPLEMENTARY_BASE + ((high.code - HIGH_SURROGATE_START) shl SURROGATE_BITS) +
                (low.code - LOW_SURROGATE_START)
        return SUPPLEMENTARY_SYMBOLS.any { codePoint in it }
    }

    private companion object {
        /** Categories kept besides letters and digits: marks, connector punctuation and letter numbers. */
        val KEPT_CATEGORIES =
            setOf(
                CharCategory.NON_SPACING_MARK,
                CharCategory.COMBINING_SPACING_MARK,
                CharCategory.ENCLOSING_MARK,
                CharCategory.CONNECTOR_PUNCTUATION,
                CharCategory.LETTER_NUMBER,
            )

        /** Emoji, pictographs, playing cards, mahjong and domino tiles, and tag characters. */
        val SUPPLEMENTARY_SYMBOLS = listOf(0x1F000..0x1FAFF, 0xE0000..0xE007F)

        const val SUPPLEMENTARY_BASE = 0x10000
        const val HIGH_SURROGATE_START = 0xD800
        const val LOW_SURROGATE_START = 0xDC00
        const val SURROGATE_BITS = 10
    }
}
