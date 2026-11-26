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

/**
 * Growable text, used for the source of a document that is appended to while it is read. Reading a
 * character or a range costs the same however long the text is, on every platform: Kotlin/Wasm's
 * `StringBuilder` re-flattens its content when read after an append, which made each update of a
 * streamed document cost as much as the whole document there.
 */
internal class TextBuffer : CharSequence {
    private var chars = CharArray(INITIAL_CAPACITY)

    override var length: Int = 0
        private set

    override fun get(index: Int): Char {
        if (index !in 0 until length) throw IndexOutOfBoundsException("index $index, length $length")
        return chars[index]
    }

    override fun subSequence(
        startIndex: Int,
        endIndex: Int,
    ): CharSequence = copyRange(startIndex, endIndex)

    fun append(text: CharSequence) {
        if (length + text.length > chars.size) chars = chars.copyOf(maxOf(chars.size * 2, length + text.length))
        for (i in text.indices) chars[length + i] = text[i]
        length += text.length
    }

    fun clear() {
        length = 0
    }

    /** The characters from [start] to [end] as a string. */
    fun copyRange(
        start: Int,
        end: Int = length,
    ): String {
        if (start < 0 || end > length ||
            start > end
        ) {
            throw IndexOutOfBoundsException("range $start..$end, length $length")
        }
        return chars.concatToString(start, end)
    }

    override fun toString(): String = chars.concatToString(0, length)

    private companion object {
        const val INITIAL_CAPACITY = 256
    }
}
