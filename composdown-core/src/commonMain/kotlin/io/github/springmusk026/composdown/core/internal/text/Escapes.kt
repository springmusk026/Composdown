package io.github.springmusk026.composdown.core.internal.text

/**
 * Resolves backslash escapes of ASCII punctuation and character references in [text], as CommonMark
 * requires for link destinations, link titles and code block info strings.
 */
internal fun unescapeString(text: String): String {
    if (text.indexOf('\\') < 0 && text.indexOf('&') < 0) return text
    val result = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && text.charAtOrEnd(i + 1).isAsciiPunctuation()) {
            result.append(text[i + 1])
            i += 2
        } else if (c == '&') {
            val reference = matchCharacterReference(text, i)
            if (reference != null) {
                result.append(reference.replacement)
                i = reference.end
            } else {
                result.append(c)
                i++
            }
        } else {
            result.append(c)
            i++
        }
    }
    return result.toString()
}

/**
 * Normalizes a link label for matching, as CommonMark requires: surrounding whitespace is removed,
 * inner whitespace runs collapse to one space, and the result is Unicode case-folded.
 *
 * @param label the label without its surrounding brackets.
 */
private const val LABEL_WHITESPACE = " \t\n\r"

internal fun normalizeLinkLabel(label: CharSequence): String {
    val collapsed = StringBuilder(label.length)
    var pendingSpace = false
    for (c in label) {
        if (c in LABEL_WHITESPACE) {
            pendingSpace = collapsed.isNotEmpty()
        } else {
            if (pendingSpace) collapsed.append(' ')
            pendingSpace = false
            collapsed.append(c)
        }
    }
    // Lowercasing and then uppercasing approximates Unicode case folding, including mappings such as
    // "ẞ" -> "ß" -> "SS", so that labels differing only in case match.
    return collapsed.toString().lowercase().uppercase()
}

/** Strips leading and trailing spaces, tabs and line feeds (but not other Unicode whitespace). */
internal fun String.trimSpacesTabsAndNewlines(): String = trim { it == ' ' || it == '\t' || it == '\n' }
