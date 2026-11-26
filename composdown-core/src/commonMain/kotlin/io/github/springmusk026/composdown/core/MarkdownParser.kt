package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.internal.AstBuilder
import io.github.springmusk026.composdown.core.internal.InlineConverter
import io.github.springmusk026.composdown.core.internal.block.BlockParser
import io.github.springmusk026.composdown.core.internal.inline.FootnoteRegistry
import io.github.springmusk026.composdown.core.internal.inline.InlineParser
import io.github.springmusk026.composdown.core.internal.inline.LinkReferenceMap

/**
 * Parses Markdown text into a [MarkdownDocument], following the
 * [CommonMark specification](https://spec.commonmark.org/0.31.2/) plus the enabled [extensions].
 *
 * A parser holds no state between calls: [parse] can be called any number of times, from any
 * thread, including concurrently.
 *
 * ```
 * val document = MarkdownParser().parse("# Hello\n\nThis is *Markdown* with ~~GFM~~.")
 * val commonMarkOnly = MarkdownParser(extensions = emptySet())
 * ```
 *
 * @property extensions the syntax extensions to recognize. Defaults to GitHub Flavored Markdown.
 */
public class MarkdownParser(
    public val extensions: Set<MarkdownExtension> = MarkdownExtension.GFM,
) {
    /**
     * Parses [markdown] and returns its document tree.
     *
     * Any string is valid Markdown, so parsing never fails. Lines may end with `\n`, `\r\n` or `\r`.
     */
    public fun parse(markdown: String): MarkdownDocument {
        val references = LinkReferenceMap()
        val footnotes = if (MarkdownExtension.FOOTNOTES in extensions) FootnoteRegistry() else null
        val blockParser = BlockParser(references, extensions, footnotes)
        forEachLine(markdown, blockParser::addLine)
        val document = blockParser.finish()
        val inlines = InlineConverter(InlineParser(references, extensions, footnotes))
        return AstBuilder(inlines, extensions, footnotes).document(document)
    }

    private inline fun forEachLine(
        text: String,
        action: (String) -> Unit,
    ) {
        var start = 0
        while (start < text.length) {
            var end = start
            while (end < text.length && text[end] != '\n' && text[end] != '\r') end++
            action(text.substring(start, end))
            start =
                when {
                    end >= text.length -> end
                    text[end] == '\r' && end + 1 < text.length && text[end + 1] == '\n' -> end + 2
                    else -> end + 1
                }
        }
    }
}
