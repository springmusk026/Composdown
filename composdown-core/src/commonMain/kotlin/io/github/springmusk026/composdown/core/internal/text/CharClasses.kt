package io.github.springmusk026.composdown.core.internal.text

/** Returned by [charAtOrEnd] past the end of the text. Never occurs in parser input (NUL is replaced). */
internal const val END: Char = '\u0000'

private const val ASCII_LIMIT = 0x80
private const val ASCII_DELETE = 0x7F
private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
private const val HIGH_SURROGATE_OFFSET = 0xD800 - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10)
private const val LOW_SURROGATE_BASE = 0xDC00
private const val SURROGATE_BITS = 10
private const val LOW_SURROGATE_MASK = 0x3FF

internal fun CharSequence.charAtOrEnd(index: Int): Char = if (index in indices) this[index] else END

internal fun Char.isSpaceOrTab(): Boolean = this == ' ' || this == '\t'

/** Space, tab or line feed: the whitespace allowed inside inline constructs of a paragraph. */
internal fun Char.isInlineWhitespace(): Boolean = this == ' ' || this == '\t' || this == '\n'

/** ASCII control characters (U+0000-U+001F and U+007F). */
internal fun Char.isAsciiControl(): Boolean = this < ' ' || code == ASCII_DELETE

internal fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

internal fun Char.isAsciiAlphanumeric(): Boolean = isAsciiLetter() || isAsciiDigit()

internal fun Char.isAsciiHexDigit(): Boolean = isAsciiDigit() || this in 'a'..'f' || this in 'A'..'F'

/** ASCII punctuation as defined by CommonMark: the characters that can be backslash-escaped. */
internal fun Char.isAsciiPunctuation(): Boolean =
    this in '!'..'/' || this in ':'..'@' || this in '['..'`' || this in '{'..'~'

/** Unicode whitespace as defined by CommonMark: general category Zs, tab, line feed, form feed, carriage return. */
internal fun isUnicodeWhitespace(codePoint: Int): Boolean =
    codePoint == '\t'.code ||
        codePoint == '\n'.code ||
        codePoint == '\u000C'.code ||
        codePoint == '\r'.code ||
        SPACE_SEPARATOR_RANGES.containsCodePoint(codePoint)

/** Unicode punctuation as defined by CommonMark 0.31: general categories P (punctuation) and S (symbols). */
internal fun isUnicodePunctuation(codePoint: Int): Boolean =
    if (codePoint < ASCII_LIMIT) {
        codePoint.toChar().isAsciiPunctuation()
    } else {
        PUNCTUATION_RANGES.containsCodePoint(codePoint)
    }

/** Binary search in a sorted array of inclusive `[start, end]` pairs. */
private fun IntArray.containsCodePoint(codePoint: Int): Boolean {
    var low = 0
    var high = size / 2 - 1
    while (low <= high) {
        val mid = (low + high) ushr 1
        when {
            codePoint < this[2 * mid] -> high = mid - 1
            codePoint > this[2 * mid + 1] -> low = mid + 1
            else -> return true
        }
    }
    return false
}

/** The code point that ends just before [index], or -1 if [index] is 0. */
internal fun CharSequence.codePointBefore(index: Int): Int {
    if (index <= 0) return -1
    val low = this[index - 1]
    if (low.isLowSurrogate() && index >= 2) {
        val high = this[index - 2]
        if (high.isHighSurrogate()) return toCodePoint(high, low)
    }
    return low.code
}

/** The code point that starts at [index], or -1 if [index] is past the end. */
internal fun CharSequence.codePointAt(index: Int): Int {
    if (index >= length) return -1
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) return toCodePoint(high, low)
    }
    return high.code
}

private fun toCodePoint(
    high: Char,
    low: Char,
): Int = ((high.code - HIGH_SURROGATE_OFFSET) shl SURROGATE_BITS) + (low.code - LOW_SURROGATE_BASE)

internal fun StringBuilder.appendCodePoint(codePoint: Int): StringBuilder {
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
        append(codePoint.toChar())
    } else {
        append(((codePoint ushr SURROGATE_BITS) + HIGH_SURROGATE_OFFSET).toChar())
        append(((codePoint and LOW_SURROGATE_MASK) + LOW_SURROGATE_BASE).toChar())
    }
    return this
}
