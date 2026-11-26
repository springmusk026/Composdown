package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd

/** Inline math found in text: where it ends (exclusive), its TeX source, and whether it is display math. */
internal class MathSpan(
    val end: Int,
    val source: String,
    val isDisplay: Boolean,
)

/**
 * Scanners for inline math in the forms language models commonly produce: `$...$` and `$$...$$`
 * (with the rules of GitHub and pandoc, so that prices such as "$5 and $10" stay text), `\(...\)` and
 * `\[...\]`. Searches for closing delimiters are memoized, so many unmatched openers in one paragraph
 * are still scanned in linear time.
 */
internal class MathScanner {
    private val absentFrom = HashMap<String, Int>()

    /** Math opened by `$` or `$$` at [start], or `null` if it is not closed according to the rules. */
    fun scanDollar(
        text: CharSequence,
        start: Int,
    ): MathSpan? {
        var count = 0
        while (text.charAtOrEnd(start + count) == '$') count++
        return when (count) {
            1 -> scanSingleDollar(text, start)
            2 -> find(text, start + 2, "$$")?.let { close -> span(text, start + 2, close, close + 2, isDisplay = true) }
            else -> null
        }
    }

    /**
     * `$...$`: the opening `$` must be followed by a non-space character, and the closing `$` must
     * follow a non-space character, must not be escaped, and must not be followed by a digit.
     */
    private fun scanSingleDollar(
        text: CharSequence,
        start: Int,
    ): MathSpan? {
        val first = text.charAtOrEnd(start + 1)
        if (first.isWhitespace() || first == '$' || start + 1 >= text.length) return null
        val close = nextValidSingleCloser(text, start + 1) ?: return null
        return span(text, start + 1, close, close + 1, isDisplay = false)
    }

    /** Where the last search for a closing `$` started, and what it found (`-1` for nothing). */
    private var lastCloserSearchFrom = Int.MAX_VALUE
    private var lastCloser = -1

    /**
     * The first `$` at or after [from] that can close `$...$`. Whether a `$` can close does not depend
     * on the opener (the opener is never directly before it, since `$` cannot follow the opener), so
     * results are reused across openers: a later search from inside an already searched range has
     * the same answer, which keeps many unmatched openers (such as prices) linear.
     */
    private fun nextValidSingleCloser(
        text: CharSequence,
        from: Int,
    ): Int? {
        if (from >= lastCloserSearchFrom && (lastCloser < 0 || from <= lastCloser)) {
            return lastCloser.takeIf { it >= 0 }
        }
        var position = from
        var result = -1
        while (result < 0) {
            val close = find(text, position, "$") ?: break
            val before = text[close - 1]
            if (!before.isWhitespace() && before != '\\' && !text.charAtOrEnd(close + 1).isDigit()) {
                result = close
            } else {
                position = close + 1
            }
        }
        lastCloserSearchFrom = from
        lastCloser = result
        return result.takeIf { it >= 0 }
    }

    /** `\(...\)` or `\[...\]` starting at the backslash at [start], or `null` if not closed. */
    fun scanBracketed(
        text: CharSequence,
        start: Int,
    ): MathSpan? {
        val isDisplay =
            when (text.charAtOrEnd(start + 1)) {
                '(' -> false
                '[' -> true
                else -> return null
            }
        val closer = if (isDisplay) "\\]" else "\\)"
        val close = find(text, start + 2, closer) ?: return null
        return span(text, start + 2, close, close + 2, isDisplay)
    }

    private fun span(
        text: CharSequence,
        contentStart: Int,
        contentEnd: Int,
        end: Int,
        isDisplay: Boolean,
    ): MathSpan? {
        val source = text.substring(contentStart, contentEnd).trim()
        return if (source.isEmpty()) null else MathSpan(end, source, isDisplay)
    }

    /** Index of [closer] at or after [from], remembering positions from which it is known to be absent. */
    private fun find(
        text: CharSequence,
        from: Int,
        closer: String,
    ): Int? {
        if (from >= (absentFrom[closer] ?: Int.MAX_VALUE)) return null
        val index = text.indexOf(closer, from)
        if (index < 0) {
            absentFrom[closer] = from
            return null
        }
        return index
    }
}
