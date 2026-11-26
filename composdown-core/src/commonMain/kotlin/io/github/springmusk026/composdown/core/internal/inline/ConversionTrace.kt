package io.github.springmusk026.composdown.core.internal.inline

/**
 * What the inline content of a block depended on while it was parsed: labels that could not be
 * resolved yet, and the footnotes it referenced. The incremental parser uses this to update a
 * completed block when a later definition resolves one of its labels, or when footnote numbers change.
 */
internal class ConversionTrace {
    /** Normalized link labels that were looked up without a matching definition. */
    val linkMisses: MutableSet<String> = HashSet()

    /** Normalized footnote labels that were referenced without a matching definition. */
    val footnoteMisses: MutableSet<String> = HashSet()

    /** Normalized labels of resolved footnote references, in document order (with repetitions). */
    val footnoteKeys: MutableList<String> = ArrayList()

    /** The number each entry of [footnoteKeys] was rendered with. */
    val footnoteNumbers: MutableList<Int> = ArrayList()
}
