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
 *
 * A map can be layered over a [base] map: lookups fall through to the base, and new definitions are
 * kept in this layer only. The incremental parser uses this for the provisional end of a document,
 * whose definitions must not leak into the definitions of completed blocks.
 */
internal class LinkReferenceMap(
    private val base: LinkReferenceMap? = null,
) {
    private val references = HashMap<String, LinkReference>()
    private val labels = ArrayList<String>()

    /** Number of definitions added to this layer. */
    val size: Int get() = labels.size

    /** The normalized label of the [index]th definition added to this layer, in order of definition. */
    fun labelAt(index: Int): String = labels[index]

    operator fun get(normalizedLabel: String): LinkReference? =
        references[normalizedLabel] ?: base?.get(normalizedLabel)

    /** Adds a definition unless one already exists for [normalizedLabel] in this layer or the base. */
    fun putIfAbsent(
        normalizedLabel: String,
        reference: LinkReference,
    ) {
        if (get(normalizedLabel) == null) {
            references[normalizedLabel] = reference
            labels.add(normalizedLabel)
        }
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
    start: Int = 0,
): Int {
    val labelEnd = scanLinkLabel(text, start)
    if (labelEnd < 0 || text.charAtOrEnd(labelEnd) != ':') return 0
    val label = normalizeLinkLabel(text.subSequence(start + 1, labelEnd - 1))
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
    return end - start
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
