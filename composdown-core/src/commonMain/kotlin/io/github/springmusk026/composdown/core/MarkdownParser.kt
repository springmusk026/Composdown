package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.internal.IncrementalEngine

/**
 * Parses Markdown text into a [MarkdownDocument], following the
 * [CommonMark specification](https://spec.commonmark.org/0.31.2/) plus the enabled [extensions].
 *
 * A parser holds no state between calls: [parse] can be called any number of times, from any
 * thread, including concurrently. To parse text that arrives in pieces, use
 * [IncrementalMarkdownParser].
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
    public fun parse(markdown: String): MarkdownDocument =
        IncrementalEngine(extensions, optimistic = false).parseComplete(markdown)
}
