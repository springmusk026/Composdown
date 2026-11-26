package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.normalizeLinkLabel

/**
 * Footnote definitions found by the block parser, and the numbers assigned to footnotes as they are
 * first referenced. Numbers follow the order of first reference in the document, so numbering is
 * stable as more of a document is parsed.
 *
 * Keys are normalized labels (see [normalizeLinkLabel]). A registry can be layered over a [base]
 * registry: lookups fall through to the base, while new definitions and numbers stay in this layer.
 * The incremental parser uses this for the provisional end of a document.
 */
internal class FootnoteRegistry(
    private val base: FootnoteRegistry? = null,
) {
    private val labels = LinkedHashMap<String, String>()
    private val numbers = HashMap<String, Int>()

    /** Number of distinct footnote labels defined in this layer. */
    val definedCount: Int get() = labels.size

    /** Normalized keys of the footnotes defined in this layer, in order of definition. */
    val definedKeys: Collection<String> get() = labels.keys

    /** Number of footnotes numbered in this layer and its base. */
    private val numberedCount: Int get() = numbers.size + (base?.numberedCount ?: 0)

    /** Whether this layer numbered any footnote. */
    val hasOwnNumbers: Boolean get() = numbers.isNotEmpty()

    /** Records a definition with [label]. The first definition of a label wins. */
    fun define(label: String) {
        val key = normalizeLinkLabel(label)
        if (labelOf(key) == null) labels[key] = label
    }

    private fun labelOf(key: String): String? = labels[key] ?: base?.labelOf(key)

    /** The number of the footnote with normalized [key], or `null` if it has not been referenced. */
    fun numberOfKey(key: String): Int? = numbers[key] ?: base?.numberOfKey(key)

    /**
     * Records a reference to [label] and returns the definition's label and the footnote's number, or
     * `null` if no such footnote is defined.
     */
    fun reference(label: String): Pair<String, Int>? {
        val key = normalizeLinkLabel(label)
        val definitionLabel = labelOf(key) ?: return null
        val number = numberOfKey(key) ?: (numberedCount + 1).also { numbers[key] = it }
        return definitionLabel to number
    }

    /** The number of the footnote defined with [label], or `null` if it has not been referenced. */
    fun numberOf(label: String): Int? = numberOfKey(normalizeLinkLabel(label))

    /** Footnote numbers assigned in this layer (not its base), by normalized key. */
    fun ownNumbers(): Map<String, Int> = numbers

    /** Replaces the numbers of this layer, for renumbering after references were resolved out of order. */
    fun setNumbers(newNumbers: Map<String, Int>) {
        numbers.clear()
        numbers.putAll(newNumbers)
    }
}
