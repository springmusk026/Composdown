package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab
import io.github.springmusk026.composdown.core.internal.text.normalizeLinkLabel

/** The target of a link reference definition (`[label]: destination "title"`). */
internal class LinkReference(
    val destination: String,
    val title: String?,
)

/**
 * Link reference definitions, keyed by normalized label. The first definition of a label wins, as
 * CommonMark requires.
 */
internal class LinkReferenceMap {
    private val references = HashMap<String, LinkReference>()

    operator fun get(normalizedLabel: String): LinkReference? = references[normalizedLabel]

    /** Adds a definition unless one already exists for [normalizedLabel]. */
    fun putIfAbsent(
        normalizedLabel: String,
        reference: LinkReference,
    ) {
        if (normalizedLabel !in references) references[normalizedLabel] = reference
    }
}

/**
 * Parses one link reference definition at the start of [text] (paragraph content) and adds it to
 * [references]. Returns the number of characters consumed, or 0 if [text] does not start with a
 * valid definition.
 */
internal fun parseLinkReferenceDefinition(
    text: String,
    references: LinkReferenceMap,
): Int {
    val labelEnd = scanLinkLabel(text, 0)
    if (labelEnd < 0 || text.charAtOrEnd(labelEnd) != ':') return 0
    val label = normalizeLinkLabel(text.subSequence(1, labelEnd - 1))
    if (label.isEmpty()) return 0

    val destination = scanLinkDestination(text, skipSpacesTabsAndOneNewline(text, labelEnd + 1)) ?: return 0
    val beforeTitle = destination.end
    val titleStart = skipSpacesTabsAndOneNewline(text, beforeTitle)
    val title = if (titleStart != beforeTitle) scanLinkTitle(text, titleStart) else null

    // The definition must end at a line ending. If a title was found but is followed by other text,
    // the definition may still be valid without the title, with the title text on the next line.
    val endWithTitle = title?.let { endOfLine(text, it.end) } ?: -1
    val end: Int
    val titleValue: String?
    if (endWithTitle >= 0) {
        end = endWithTitle
        titleValue = title?.value
    } else {
        end = endOfLine(text, beforeTitle)
        titleValue = null
    }
    if (end < 0) return 0

    references.putIfAbsent(label, LinkReference(destination.value, titleValue))
    return end
}

/** If only spaces and tabs remain before a line ending (or the end), returns the index after it; else -1. */
private fun endOfLine(
    text: CharSequence,
    start: Int,
): Int {
    var i = start
    while (text.charAtOrEnd(i).isSpaceOrTab()) i++
    return when {
        i == text.length -> i
        text[i] == '\n' -> i + 1
        else -> -1
    }
}
