package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.normalizeLinkLabel

/**
 * Footnote definitions found by the block parser, and the numbers assigned to footnotes as they are
 * first referenced. Numbers follow the order of first reference in the document, so numbering is
 * stable as more of a document is parsed.
 */
internal class FootnoteRegistry {
    private val labels = HashMap<String, String>()
    private val numbers = HashMap<String, Int>()

    /** Records a definition with [label]. The first definition of a label wins. */
    fun define(label: String) {
        val key = normalizeLinkLabel(label)
        if (key !in labels) labels[key] = label
    }

    /** Whether a definition exists for [label] (in any letter case or spacing). */
    fun isDefined(label: String): Boolean = normalizeLinkLabel(label) in labels

    /**
     * Records a reference to [label] and returns the definition's label and the footnote's number, or
     * `null` if no such footnote is defined.
     */
    fun reference(label: String): Pair<String, Int>? {
        val key = normalizeLinkLabel(label)
        val definitionLabel = labels[key] ?: return null
        val number = numbers.getOrPut(key) { numbers.size + 1 }
        return definitionLabel to number
    }

    /** The number of the footnote defined with [label], or `null` if it has not been referenced. */
    fun numberOf(label: String): Int? = numbers[normalizeLinkLabel(label)]
}
