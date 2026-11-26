package io.github.springmusk026.composdown.core.internal

/** Maximum digits of an ordered list marker. */
private const val MAX_LIST_NUMBER_DIGITS = 9

/** Maximum level of an ATX heading. */
private const val MAX_HEADING_LEVEL = 6

/**
 * Whether [line], a line that is still being written, could turn out to be a block marker that
 * changes how it is parsed: the start of a code fence (`` ` ``, `~`), a thematic break, setext
 * underline or list item (`-`, `=`, `*`, `_`, `+`), an ATX heading (`#`), an ordered list item
 * (`1.`), a table delimiter row (`| --- |`), or any of these inside block quotes (`>`).
 *
 * Such a line is better hidden until it is complete, so that it does not briefly appear as text.
 */
private fun Char.isQuoteMarkerOrSpace(): Boolean = this == '>' || this == ' ' || this == '\t'

internal fun mayBecomeBlockMarker(line: CharSequence): Boolean {
    // Skip block quote markers by index rather than by copying the rest of the line for each one.
    var start = 0
    while (start < line.length && line[start].isQuoteMarkerOrSpace()) start++
    val content = line.subSequence(start, line.length)
    // A line of only block quote markers may still be followed by any of the markers below.
    if (content.isEmpty()) return line.any { it == '>' }
    val first = content[0]
    return when (first) {
        '`', '~' -> content.all { it == first }
        '-', '=', '*', '_', '+' -> content.all { it == first || it == ' ' || it == '\t' }
        '#' -> content.length <= MAX_HEADING_LEVEL && content.all { it == '#' }
        '|', ':' -> content.all { it == '|' || it == '-' || it == ':' || it == ' ' || it == '\t' }
        else -> isOrderedListMarkerPrefix(content)
    }
}

/** `1`, `12.`, `3)`: digits optionally followed by the list delimiter. */
private fun isOrderedListMarkerPrefix(content: CharSequence): Boolean {
    val digits = content.takeWhile { it in '0'..'9' }
    val rest = content.substring(digits.length)
    return digits.length in 1..MAX_LIST_NUMBER_DIGITS && (rest.isEmpty() || rest == "." || rest == ")")
}
