package io.github.springmusk026.composdown.core

/**
 * How an [IncrementalMarkdownParser] presents the end of a document that is still being written.
 *
 * The policy only affects intermediate results. After [IncrementalMarkdownParser.finish], the
 * document is always exactly what [MarkdownParser.parse] produces for the same text.
 */
public enum class StreamingPolicy {
    /**
     * Presents incomplete syntax at the end of the text as its most likely final form, to avoid
     * flicker while text arrives:
     *
     * - Unclosed emphasis, strong emphasis, strikethrough and code spans are shown closed
     *   (`**bold te` as bold text).
     * - A link whose destination is still being written (`[text](https://exa`) is shown as a link.
     * - A trailing run of `*`, `_`, `~` or `` ` `` is hidden until the next character shows what it is.
     * - A line made only of characters that may start a block marker (`` ` ``, `-`, `1.`, `|`, `>`,
     *   ...) is hidden until it is complete, so that it does not briefly appear as text.
     * - A paragraph line that looks like a table header row is hidden until the next line shows
     *   whether a table starts.
     *
     * If the text ends without the syntax being completed (for example, a lone `*` that was meant
     * literally), the final document differs from the last intermediate one in that detail.
     */
    OPTIMISTIC,

    /** Parses the text exactly as written at every step, as [MarkdownParser.parse] would. */
    STRICT,
}
