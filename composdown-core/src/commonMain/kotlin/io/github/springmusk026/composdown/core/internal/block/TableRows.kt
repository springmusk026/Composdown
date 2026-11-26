package io.github.springmusk026.composdown.core.internal.block

import io.github.springmusk026.composdown.core.ast.TableAlignment
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab

/**
 * Splits a table row into trimmed cell contents, starting at [start]. Cells are separated by pipes
 * that are not backslash-escaped; a leading pipe and a trailing pipe are optional. `\|` becomes `|`
 * in cell content (even inside code spans, as in GFM); other escapes are left for inline parsing.
 */
internal fun splitTableRow(
    line: String,
    start: Int,
): List<String> {
    var i = start
    while (i < line.length && line[i].isSpaceOrTab()) i++
    if (i < line.length && line[i] == '|') i++
    val cells = ArrayList<String>()
    val cell = StringBuilder()
    var endsWithPipe = false
    while (i < line.length) {
        val c = line[i]
        endsWithPipe = false
        when {
            c == '\\' && i + 1 < line.length -> {
                if (line[i + 1] == '|') cell.append('|') else cell.append(c).append(line[i + 1])
                i += 2
            }

            c == '|' -> {
                cells.add(cell.trimCell())
                cell.clear()
                endsWithPipe = true
                i++
            }

            else -> {
                cell.append(c)
                i++
            }
        }
    }
    // Text after the last pipe is a cell unless it is only whitespace (a trailing pipe).
    if (!endsWithPipe && cell.any { !it.isSpaceOrTab() }) cells.add(cell.trimCell())
    return cells
}

private fun CharSequence.trimCell(): String = toString().trim(' ', '\t')

/**
 * Parses the cells of a delimiter row (`| :--- | :-: | --: |`) into column alignments, or returns
 * `null` if any cell is not a run of hyphens with optional colons at either end.
 */
internal fun parseDelimiterRow(cells: List<String>): List<TableAlignment>? {
    if (cells.isEmpty()) return null
    return cells.map { cell ->
        val left = cell.startsWith(':')
        val right = cell.length > 1 && cell.endsWith(':')
        val dashes = cell.substring(if (left) 1 else 0, cell.length - if (right) 1 else 0)
        if (dashes.isEmpty() || dashes.any { it != '-' }) return null
        when {
            left && right -> TableAlignment.CENTER
            left -> TableAlignment.LEFT
            right -> TableAlignment.RIGHT
            else -> TableAlignment.NONE
        }
    }
}
