package io.github.springmusk026.composdown.core.internal.text

private const val MAX_DECIMAL_DIGITS = 7
private const val MAX_HEX_DIGITS = 6
private const val MAX_ENTITY_NAME_LENGTH = 32
private const val MAX_CODE_POINT = 0x10FFFF
private const val HEX_RADIX = 16
private const val DECIMAL_RADIX = 10
private val SURROGATES = 0xD800..0xDFFF
private const val REPLACEMENT_CHARACTER = 0xFFFD

/** A character reference found in text: its end index (exclusive) and the text it stands for. */
internal class CharacterReference(
    val end: Int,
    val replacement: String,
)

/**
 * Matches an entity (`&amp;`) or numeric character reference (`&#35;`, `&#x23;`) starting at the
 * `&` at [start], following CommonMark's rules. Returns `null` if there is no valid reference there.
 */
internal fun matchCharacterReference(
    text: CharSequence,
    start: Int,
): CharacterReference? {
    if (text.charAtOrEnd(start) != '&') return null
    return if (text.charAtOrEnd(start + 1) == '#') {
        matchNumericReference(text, start)
    } else {
        matchNamedReference(text, start)
    }
}

private fun matchNumericReference(
    text: CharSequence,
    start: Int,
): CharacterReference? {
    val afterHash = start + "&#".length
    val hex = text.charAtOrEnd(afterHash).let { it == 'x' || it == 'X' }
    val digitsStart = if (hex) afterHash + 1 else afterHash
    val end =
        if (hex) {
            scanDigits(text, digitsStart, MAX_HEX_DIGITS) { it.isAsciiHexDigit() }
        } else {
            scanDigits(text, digitsStart, MAX_DECIMAL_DIGITS) { it.isAsciiDigit() }
        }
    if (end == digitsStart || text.charAtOrEnd(end) != ';') return null
    val value = text.subSequence(digitsStart, end).toString().toInt(if (hex) HEX_RADIX else DECIMAL_RADIX)
    val codePoint = if (value == 0 || value > MAX_CODE_POINT || value in SURROGATES) REPLACEMENT_CHARACTER else value
    return CharacterReference(end + 1, StringBuilder(2).appendCodePoint(codePoint).toString())
}

private inline fun scanDigits(
    text: CharSequence,
    start: Int,
    maxDigits: Int,
    isDigit: (Char) -> Boolean,
): Int {
    var end = start
    while (end - start < maxDigits && isDigit(text.charAtOrEnd(end))) end++
    return end
}

private fun matchNamedReference(
    text: CharSequence,
    start: Int,
): CharacterReference? {
    val nameStart = start + 1
    if (!text.charAtOrEnd(nameStart).isAsciiLetter()) return null
    var end = nameStart + 1
    while (end - nameStart < MAX_ENTITY_NAME_LENGTH && text.charAtOrEnd(end).isAsciiAlphanumeric()) end++
    if (end - nameStart < 2 || text.charAtOrEnd(end) != ';') return null
    val replacement = HtmlEntities.lookup(text.subSequence(nameStart, end).toString()) ?: return null
    return CharacterReference(end + 1, replacement)
}

/** HTML named character references, decoded lazily from the generated [HTML_ENTITY_DATA]. */
private object HtmlEntities {
    private val entities: Map<String, String> by lazy { decode(HTML_ENTITY_DATA) }

    fun lookup(name: String): String? = entities[name]

    private fun decode(data: String): Map<String, String> {
        val result = HashMap<String, String>()
        for (entry in data.split(',')) {
            val name = entry.substringBefore(':')
            val value = StringBuilder(2)
            for (codePoint in entry.substringAfter(':').split('+')) {
                value.appendCodePoint(codePoint.toInt(HEX_RADIX))
            }
            result[name] = value.toString()
        }
        return result
    }
}
