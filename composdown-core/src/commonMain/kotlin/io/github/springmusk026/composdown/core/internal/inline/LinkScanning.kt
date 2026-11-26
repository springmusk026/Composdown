package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isAsciiControl
import io.github.springmusk026.composdown.core.internal.text.isAsciiPunctuation
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab
import io.github.springmusk026.composdown.core.internal.text.unescapeString

/** Maximum length of a link label including its brackets: 999 characters inside, plus `[` and `]`. */
private const val MAX_LINK_LABEL_LENGTH = 1001

/** A scanned link component: its end index (exclusive) and its unescaped value. */
internal class ScannedValue(
    val end: Int,
    val value: String,
)

/**
 * Scans a link label (`[label]`) starting at the `[` at [start]. Returns the index after the closing
 * `]`, or -1 if there is no valid label there. Labels cannot contain unescaped brackets and are at
 * most 999 characters long.
 */
internal fun scanLinkLabel(
    text: CharSequence,
    start: Int,
): Int {
    if (text.charAtOrEnd(start) != '[') return -1
    var i = start + 1
    while (i < text.length && i - start < MAX_LINK_LABEL_LENGTH) {
        when (text[i]) {
            '\\' -> i += if (i + 1 < text.length) 2 else 1
            '[' -> return -1
            ']' -> return i + 1
            else -> i++
        }
    }
    return -1
}

/**
 * Scans a link destination starting at [start]: either `<...>` (no line endings or unescaped `<`
 * and `>`), or a nonempty run without spaces or ASCII control characters in which parentheses are
 * balanced unless escaped.
 */
internal fun scanLinkDestination(
    text: CharSequence,
    start: Int,
): ScannedValue? =
    if (text.charAtOrEnd(start) == '<') {
        scanBracketedDestination(text, start)
    } else {
        scanBareDestination(text, start)
    }

private fun scanBracketedDestination(
    text: CharSequence,
    start: Int,
): ScannedValue? {
    var i = start + 1
    while (i < text.length) {
        val c = text[i]
        if (c == '>') return ScannedValue(i + 1, unescapeString(text.substring(start + 1, i)))
        if (c == '<' || c == '\n') return null
        // A backslash escapes the next character, except a line ending.
        i += if (c == '\\' && i + 1 < text.length && text[i + 1] != '\n') 2 else 1
    }
    return null
}

private fun scanBareDestination(
    text: CharSequence,
    start: Int,
): ScannedValue? {
    var i = start
    var openParens = 0
    while (i < text.length) {
        val c = text[i]
        val step =
            when {
                c == '\\' && text.charAtOrEnd(i + 1).isAsciiPunctuation() -> 2
                c == '(' -> 1.also { openParens++ }
                c == ')' && openParens > 0 -> 1.also { openParens-- }
                c == ')' || c == ' ' || c.isAsciiControl() -> 0
                else -> 1
            }
        if (step == 0) break
        i += step
    }
    val empty = i == start && text.charAtOrEnd(i) != ')'
    return if (empty || openParens != 0) null else ScannedValue(i, unescapeString(text.substring(start, i)))
}

/**
 * Scans a link title starting at [start]: `"..."`, `'...'` or `(...)`. Backslash escapes are
 * allowed; the parenthesized form cannot contain unescaped parentheses.
 */
internal fun scanLinkTitle(
    text: CharSequence,
    start: Int,
): ScannedValue? {
    val closer =
        when (text.charAtOrEnd(start)) {
            '"' -> '"'
            '\'' -> '\''
            '(' -> ')'
            else -> return null
        }
    var i = start + 1
    while (i < text.length) {
        val c = text[i]
        when {
            c == closer -> return ScannedValue(i + 1, unescapeString(text.substring(start + 1, i)))
            c == '\\' -> i += 2
            closer == ')' && c == '(' -> return null
            else -> i++
        }
    }
    return null
}

/** Skips spaces and tabs, at most one line ending, and spaces and tabs after it. */
internal fun skipSpacesTabsAndOneNewline(
    text: CharSequence,
    start: Int,
): Int {
    var i = start
    while (text.charAtOrEnd(i).isSpaceOrTab()) i++
    if (text.charAtOrEnd(i) == '\n') {
        i++
        while (text.charAtOrEnd(i).isSpaceOrTab()) i++
    }
    return i
}
