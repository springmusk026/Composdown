package io.github.springmusk026.composdown.core.internal

/**
 * Calls [action] with each line of [text] from [start], without line endings. Lines end with `\n`,
 * `\r\n` or `\r`; a final line without a line ending is included, and a line ending at the very end
 * does not produce an extra empty line.
 */
internal inline fun forEachLine(
    text: CharSequence,
    start: Int = 0,
    action: (String) -> Unit,
) {
    var lineStart = start
    while (lineStart < text.length) {
        val end = lineEnd(text, lineStart)
        action(text.substring(lineStart, end))
        lineStart = afterLineBreak(text, end)
    }
}

/** Index of the line break (or end of text) that ends the line starting at [start]. */
internal fun lineEnd(
    text: CharSequence,
    start: Int,
): Int {
    var end = start
    while (end < text.length && text[end] != '\n' && text[end] != '\r') end++
    return end
}

/** Index after the line break at [end] (`\r\n` counts as one), or [end] if it is the end of the text. */
internal fun afterLineBreak(
    text: CharSequence,
    end: Int,
): Int =
    when {
        end >= text.length -> end
        text[end] == '\r' && end + 1 < text.length && text[end + 1] == '\n' -> end + 2
        else -> end + 1
    }
