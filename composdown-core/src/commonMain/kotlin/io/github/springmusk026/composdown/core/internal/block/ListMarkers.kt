package io.github.springmusk026.composdown.core.internal.block

import io.github.springmusk026.composdown.core.internal.text.isAsciiDigit
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab

/** Maximum digits in an ordered list marker. */
private const val MAX_LIST_NUMBER_DIGITS = 9

/** Columns after a list marker from which the content is indented code instead. */
private const val MAX_SPACES_AFTER_MARKER = 5

/**
 * Parses a list marker at the cursor's next non-space character. On success, advances the cursor
 * past the marker and the spaces that belong to it, and returns the marker's details.
 *
 * @param interruptsParagraph whether the line would otherwise continue a paragraph, in which case
 *   the item cannot be empty and an ordered list must start with 1.
 */
internal fun parseListMarker(
    cursor: LineCursor,
    interruptsParagraph: Boolean,
): ListData? {
    if (cursor.indented) return null
    val markerStart = cursor.nextNonspace
    val data = scanListMarker(cursor, markerStart) ?: return null
    val markerEnd = markerStart + data.markerWidth
    if (markerEnd < cursor.line.length && !cursor.charAt(markerEnd).isSpaceOrTab()) return null
    if (interruptsParagraph && !canInterruptParagraph(cursor, data, markerEnd)) return null

    cursor.advanceNextNonspace()
    cursor.advanceColumns(data.markerWidth)
    val spacesStartColumn = cursor.column
    val spacesStartOffset = cursor.offset
    do {
        cursor.advanceColumns(1)
    } while (cursor.column - spacesStartColumn < MAX_SPACES_AFTER_MARKER && cursor.charAt(cursor.offset).isSpaceOrTab())

    val spacesAfterMarker = cursor.column - spacesStartColumn
    val blankItem = cursor.offset >= cursor.line.length
    if (spacesAfterMarker >= MAX_SPACES_AFTER_MARKER || spacesAfterMarker < 1 || blankItem) {
        // Content that starts 5 or more columns after the marker is indented code, so only one space
        // belongs to the marker.
        data.padding = data.markerWidth + 1
        cursor.restore(spacesStartOffset, spacesStartColumn)
        if (cursor.charAt(cursor.offset).isSpaceOrTab()) cursor.advanceColumns(1)
    } else {
        data.padding = data.markerWidth + spacesAfterMarker
    }
    return data
}

/** A list item can interrupt a paragraph only if it is not empty and, if ordered, starts at 1. */
private fun canInterruptParagraph(
    cursor: LineCursor,
    data: ListData,
    markerEnd: Int,
): Boolean = !cursor.isBlankFrom(markerEnd) && (!data.isOrdered || data.start == 1)

/** Recognizes `-`, `+`, `*` or 1-9 digits followed by `.` or `)` at [start]. */
private fun scanListMarker(
    cursor: LineCursor,
    start: Int,
): ListData? {
    val first = cursor.charAt(start)
    if (first == '-' || first == '+' || first == '*') {
        return ListData(
            isOrdered = false,
            bulletChar = first,
            start = 0,
            delimiter = ' ',
            markerOffset = cursor.indent,
            markerWidth = 1,
        )
    }
    var end = start
    while (end - start < MAX_LIST_NUMBER_DIGITS && cursor.charAt(end).isAsciiDigit()) end++
    val delimiter = cursor.charAt(end)
    if (end == start || (delimiter != '.' && delimiter != ')')) return null
    return ListData(
        isOrdered = true,
        bulletChar = ' ',
        start = cursor.line.substring(start, end).toInt(),
        delimiter = delimiter,
        markerOffset = cursor.indent,
        markerWidth = end - start + 1,
    )
}
