package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isAsciiAlphanumeric
import io.github.springmusk026.composdown.core.internal.text.isAsciiControl
import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter

/** Length of the longest URI scheme an autolink may have. */
private const val MAX_SCHEME_LENGTH = 32

/** Maximum length of one label of an email autolink's domain. */
private const val MAX_DOMAIN_LABEL_LENGTH = 63

/**
 * Scans a URI autolink (`<scheme:rest>`, with a scheme of 2-32 characters and no spaces, `<` or
 * control characters) at the `<` at [start]. Returns the index after `>` or -1.
 */
internal fun scanUriAutolink(
    text: CharSequence,
    start: Int,
): Int {
    var i = start + 1
    if (!text.charAtOrEnd(i).isAsciiLetter()) return -1
    i++
    while (text.charAtOrEnd(i).isSchemeChar()) i++
    val schemeLength = i - start - 1
    if (schemeLength !in 2..MAX_SCHEME_LENGTH || text.charAtOrEnd(i) != ':') return -1
    i++
    while (i < text.length) {
        val c = text[i]
        if (c == '>') return i + 1
        if (c == '<' || c == ' ' || c.isAsciiControl()) return -1
        i++
    }
    return -1
}

private fun Char.isSchemeChar(): Boolean = isAsciiAlphanumeric() || this == '+' || this == '.' || this == '-'

/**
 * Scans an email autolink (`<local@domain>`) at the `<` at [start], using the address syntax from the
 * CommonMark specification. Returns the index after `>` or -1.
 */
internal fun scanEmailAutolink(
    text: CharSequence,
    start: Int,
): Int {
    var i = start + 1
    while (text.charAtOrEnd(i).isEmailLocalChar()) i++
    if (i == start + 1 || text.charAtOrEnd(i) != '@') return -1
    i++
    while (true) {
        val labelEnd = scanDomainLabel(text, i)
        if (labelEnd < 0) return -1
        i = labelEnd
        when (text.charAtOrEnd(i)) {
            '>' -> return i + 1
            '.' -> i++
            else -> return -1
        }
    }
}

/** A domain label: 1-63 letters, digits or hyphens, not starting or ending with a hyphen. */
private fun scanDomainLabel(
    text: CharSequence,
    start: Int,
): Int {
    var end = start
    while (text.charAtOrEnd(end).let { it.isAsciiAlphanumeric() || it == '-' }) end++
    val length = end - start
    val valid = length in 1..MAX_DOMAIN_LABEL_LENGTH && text[start] != '-' && text[end - 1] != '-'
    return if (valid) end else -1
}

private fun Char.isEmailLocalChar(): Boolean = isAsciiAlphanumeric() || this in ".!#$%&'*+/=?^_`{|}~-"
