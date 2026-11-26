package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isAsciiAlphanumeric
import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter
import io.github.springmusk026.composdown.core.internal.text.isInlineWhitespace

/**
 * Scans an HTML open tag (`<tag attr="value">`) at the `<` at [start] and returns the index after
 * the closing `>`, or -1 if there is none. Whitespace between attributes may include line feeds.
 */
internal fun scanOpenTag(
    text: CharSequence,
    start: Int,
): Int {
    var i = scanTagName(text, start + 1)
    if (i < 0) return -1
    // Each attribute must be preceded by whitespace.
    while (true) {
        val afterWhitespace = skipHtmlWhitespace(text, i)
        val afterAttribute = if (afterWhitespace > i) scanAttribute(text, afterWhitespace) else -1
        if (afterAttribute < 0) {
            i = afterWhitespace
            break
        }
        i = afterAttribute
    }
    i = skipHtmlWhitespace(text, i)
    if (text.charAtOrEnd(i) == '/') i++
    return if (text.charAtOrEnd(i) == '>') i + 1 else -1
}

/** Scans an HTML closing tag (`</tag>`) at the `<` at [start]; returns the index after `>` or -1. */
internal fun scanClosingTag(
    text: CharSequence,
    start: Int,
): Int {
    if (text.charAtOrEnd(start + 1) != '/') return -1
    var i = scanTagName(text, start + 2)
    if (i < 0) return -1
    i = skipHtmlWhitespace(text, i)
    return if (text.charAtOrEnd(i) == '>') i + 1 else -1
}

/** Scans a tag name (`[A-Za-z][A-Za-z0-9-]*`); returns the index after it or -1. */
private fun scanTagName(
    text: CharSequence,
    start: Int,
): Int {
    if (!text.charAtOrEnd(start).isAsciiLetter()) return -1
    var i = start + 1
    while (text.charAtOrEnd(i).let { it.isAsciiAlphanumeric() || it == '-' }) i++
    return i
}

/** Scans an attribute name with an optional value specification; returns the index after it or -1. */
private fun scanAttribute(
    text: CharSequence,
    start: Int,
): Int {
    val first = text.charAtOrEnd(start)
    if (!first.isAsciiLetter() && first != '_' && first != ':') return -1
    var i = start + 1
    while (text.charAtOrEnd(i).isAttributeNameChar()) i++
    val nameEnd = i
    i = skipHtmlWhitespace(text, i)
    if (text.charAtOrEnd(i) != '=') return nameEnd
    i = skipHtmlWhitespace(text, i + 1)
    return scanAttributeValue(text, i)
}

private fun scanAttributeValue(
    text: CharSequence,
    start: Int,
): Int {
    val quote = text.charAtOrEnd(start)
    if (quote == '"' || quote == '\'') {
        var i = start + 1
        while (i < text.length && text[i] != quote) i++
        return if (i < text.length) i + 1 else -1
    }
    var i = start
    while (i < text.length && text[i].isUnquotedAttributeValueChar()) i++
    return if (i > start) i else -1
}

private fun Char.isAttributeNameChar(): Boolean = isAsciiAlphanumeric() || this in "_:.-"

private fun Char.isUnquotedAttributeValueChar(): Boolean = this > ' ' && this !in "\"'=<>`"

private fun skipHtmlWhitespace(
    text: CharSequence,
    start: Int,
): Int {
    var i = start
    while (text.charAtOrEnd(i).isInlineWhitespace()) i++
    return i
}

/**
 * Scans inline raw HTML at [start] for the forms that are not tags and need a closing delimiter:
 * comments, processing instructions, declarations and CDATA sections.
 *
 * Searches for closing delimiters use [memo] so that a paragraph with many unterminated openers is
 * still scanned in linear time.
 */
internal fun scanHtmlSpecial(
    text: CharSequence,
    start: Int,
    memo: HtmlScanMemo,
): Int {
    if (text.charAtOrEnd(start + 1) == '?') {
        return memo.find(text, start + 2, "?>", HtmlScanMemo.Kind.PROCESSING_INSTRUCTION)
    }
    if (text.charAtOrEnd(start + 1) != '!') return -1
    return when {
        text.startsWith("<!--", start) -> scanComment(text, start, memo)

        text.startsWith(
            "<![CDATA[",
            start,
        ) -> memo.find(text, start + "<![CDATA[".length, "]]>", HtmlScanMemo.Kind.CDATA)

        text
            .charAtOrEnd(
                start + 2,
            ).isAsciiLetter() -> memo.find(text, start + "<!x".length, ">", HtmlScanMemo.Kind.DECLARATION)

        else -> -1
    }
}

private fun scanComment(
    text: CharSequence,
    start: Int,
    memo: HtmlScanMemo,
): Int {
    val contentStart = start + "<!--".length
    if (text.charAtOrEnd(contentStart) == '>') return contentStart + 1
    if (text.startsWith("->", contentStart)) return contentStart + 2
    return memo.find(text, contentStart, "-->", HtmlScanMemo.Kind.COMMENT)
}

/**
 * Remembers, per kind of HTML construct, the position from which its closing delimiter is known to
 * be absent, so repeated failing searches in the same text do not rescan it.
 */
internal class HtmlScanMemo {
    enum class Kind { COMMENT, PROCESSING_INSTRUCTION, DECLARATION, CDATA }

    private val absentFrom = IntArray(Kind.entries.size) { Int.MAX_VALUE }

    fun find(
        text: CharSequence,
        from: Int,
        closer: String,
        kind: Kind,
    ): Int {
        if (from >= absentFrom[kind.ordinal]) return -1
        val index = text.indexOf(closer, from)
        if (index < 0) {
            absentFrom[kind.ordinal] = from
            return -1
        }
        return index + closer.length
    }
}
