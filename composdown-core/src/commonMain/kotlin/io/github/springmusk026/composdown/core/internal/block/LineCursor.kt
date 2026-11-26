package io.github.springmusk026.composdown.core.internal.block

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab

/** Columns of indentation that make a line indented code. */
internal const val CODE_INDENT = 4

/** Width of a tab stop. */
internal const val TAB_STOP = 4

/**
 * Position within the line being parsed, tracking both the character [offset] and the visual
 * [column] (tabs expand to the next multiple of [TAB_STOP]), which CommonMark uses for indentation.
 */
internal class LineCursor {
    var line: String = ""
        private set

    /** Index of the next character of [line] to consume. */
    var offset: Int = 0
        private set

    /** Column of [offset], with tabs expanded. */
    var column: Int = 0
        private set

    /** Index of the next character that is not a space or tab, from [offset]. */
    var nextNonspace: Int = 0
        private set
    private var nextNonspaceColumn = 0

    /** Columns of indentation between [offset] and [nextNonspace]. */
    var indent: Int = 0
        private set

    /** Whether [indent] is enough for indented code. */
    val indented: Boolean get() = indent >= CODE_INDENT

    /** Whether the line contains only spaces and tabs from [offset]. */
    var blank: Boolean = false
        private set

    /** Whether part of the tab at [offset] has been consumed as columns of indentation. */
    var partiallyConsumedTab: Boolean = false
        private set

    fun reset(line: String) {
        this.line = line
        offset = 0
        column = 0
        blank = false
        partiallyConsumedTab = false
    }

    fun charAt(index: Int): Char = line.charAtOrEnd(index)

    fun findNextNonspace() {
        var i = offset
        var cols = column
        while (i < line.length) {
            when (line[i]) {
                ' ' -> cols++
                '\t' -> cols += TAB_STOP - cols % TAB_STOP
                else -> break
            }
            i++
        }
        blank = i >= line.length
        nextNonspace = i
        nextNonspaceColumn = cols
        indent = nextNonspaceColumn - column
    }

    fun advanceNextNonspace() {
        offset = nextNonspace
        column = nextNonspaceColumn
        partiallyConsumedTab = false
    }

    /** Advances by [count] characters. Tabs count as one character. */
    fun advanceChars(count: Int) {
        var remaining = count
        while (remaining > 0 && offset < line.length) {
            column += if (line[offset] == '\t') TAB_STOP - column % TAB_STOP else 1
            offset++
            remaining--
        }
        partiallyConsumedTab = false
    }

    /** Advances by [count] columns. A tab wider than the remaining count is only partially consumed. */
    fun advanceColumns(count: Int) {
        var remaining = count
        while (remaining > 0 && offset < line.length) {
            if (line[offset] == '\t') {
                val charsToTab = TAB_STOP - column % TAB_STOP
                partiallyConsumedTab = charsToTab > remaining
                val advance = minOf(charsToTab, remaining)
                column += advance
                if (!partiallyConsumedTab) offset++
                remaining -= advance
            } else {
                partiallyConsumedTab = false
                offset++
                column++
                remaining--
            }
        }
    }

    /** Consumes the rest of the line. */
    fun advanceToEnd() {
        advanceChars(line.length - offset)
    }

    /**
     * Consumes the rest of a partially consumed tab, returning the number of columns it still
     * covers (to be added as spaces), or 0 if no tab is partially consumed.
     */
    fun consumePartialTab(): Int {
        if (!partiallyConsumedTab) return 0
        offset++
        partiallyConsumedTab = false
        return TAB_STOP - column % TAB_STOP
    }

    /** Moves back to a previously observed [offset] and [column]. */
    fun restore(
        offset: Int,
        column: Int,
    ) {
        this.offset = offset
        this.column = column
        partiallyConsumedTab = false
    }

    /** Whether the line contains only spaces and tabs from [index] to its end. */
    fun isBlankFrom(index: Int): Boolean {
        for (i in index until line.length) {
            if (!line[i].isSpaceOrTab()) return false
        }
        return true
    }
}
