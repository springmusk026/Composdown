package io.github.springmusk026.composdown.core.internal.block

import io.github.springmusk026.composdown.core.internal.inline.scanClosingTag
import io.github.springmusk026.composdown.core.internal.inline.scanOpenTag
import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isAsciiAlphanumeric
import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter
import io.github.springmusk026.composdown.core.internal.text.isSpaceOrTab

/** Raw text elements, whose content may contain blank lines. */
private val RAW_TEXT_TAGS = setOf("pre", "script", "style", "textarea")

/** Block-level tag names from the CommonMark 0.31 specification. */
private val BLOCK_TAGS =
    (
        "address article aside base basefont blockquote body caption center col colgroup dd details " +
            "dialog dir div dl dt fieldset figcaption figure footer form frame frameset h1 h2 h3 h4 h5 h6 " +
            "head header hr html iframe legend li link main menu menuitem nav noframes ol optgroup option " +
            "p param search section summary table tbody td tfoot th thead title tr track ul"
    ).split(' ').toSet()

/**
 * The seven kinds of HTML block in the CommonMark specification, in the order their start conditions
 * are checked. The kind decides how the block ends.
 */
internal enum class HtmlBlockType(
    /** Whether the block ends at a blank line rather than at a closing delimiter. */
    val endsAtBlankLine: Boolean,
) {
    /** `<pre>`, `<script>`, `<style>` or `<textarea>`; ends at the matching closing tag. */
    RAW_TEXT(endsAtBlankLine = false),

    /** `<!--`; ends at `-->`. */
    COMMENT(endsAtBlankLine = false),

    /** `<?`; ends at `?>`. */
    PROCESSING_INSTRUCTION(endsAtBlankLine = false),

    /** `<!` followed by a letter; ends at `>`. */
    DECLARATION(endsAtBlankLine = false),

    /** `<![CDATA[`; ends at `]]>`. */
    CDATA(endsAtBlankLine = false),

    /** A known block-level tag such as `<div>` or `</table>`. */
    BLOCK_TAG(endsAtBlankLine = true),

    /** Any other complete open or closing tag alone on its line. Cannot interrupt a paragraph. */
    OTHER_TAG(endsAtBlankLine = true),
    ;

    /** Whether [line] contains this block's closing delimiter. Always false for types ending at a blank line. */
    fun endsOn(line: String): Boolean =
        when (this) {
            RAW_TEXT -> line.lowercase().let { lower -> RAW_TEXT_TAGS.any { lower.contains("</$it>") } }
            COMMENT -> line.contains("-->")
            PROCESSING_INSTRUCTION -> line.contains("?>")
            DECLARATION -> line.contains('>')
            CDATA -> line.contains("]]>")
            BLOCK_TAG, OTHER_TAG -> false
        }

    companion object {
        /**
         * Returns the type of HTML block that [line] starts at [start] (a `<`), or `null` if none.
         * [OTHER_TAG] is only returned when [canInterruptParagraph] is true.
         */
        fun startingAt(
            line: String,
            start: Int,
            canInterruptParagraph: Boolean,
        ): HtmlBlockType? =
            when {
                isRawTextTag(line, start) -> RAW_TEXT
                line.startsWith("<!--", start) -> COMMENT
                line.startsWith("<?", start) -> PROCESSING_INSTRUCTION
                line.charAtOrEnd(start + 1) == '!' && line.charAtOrEnd(start + 2).isAsciiLetter() -> DECLARATION
                line.startsWith("<![CDATA[", start) -> CDATA
                isBlockTag(line, start) -> BLOCK_TAG
                canInterruptParagraph && isCompleteTagLine(line, start) -> OTHER_TAG
                else -> null
            }
    }
}

private fun isRawTextTag(
    line: String,
    start: Int,
): Boolean {
    val tag = tagNameAt(line, start + 1) ?: return false
    return tag.lowercase() in RAW_TEXT_TAGS && isTagNameEnd(line, start + 1 + tag.length, allowSlash = false)
}

private fun isBlockTag(
    line: String,
    start: Int,
): Boolean {
    val nameStart = if (line.charAtOrEnd(start + 1) == '/') start + 2 else start + 1
    val tag = tagNameAt(line, nameStart) ?: return false
    return tag.lowercase() in BLOCK_TAGS && isTagNameEnd(line, nameStart + tag.length, allowSlash = true)
}

/** A complete open or closing tag (other than a raw text tag), followed only by spaces or tabs. */
private fun isCompleteTagLine(
    line: String,
    start: Int,
): Boolean {
    val closing = line.charAtOrEnd(start + 1) == '/'
    val tag = tagNameAt(line, if (closing) start + 2 else start + 1) ?: return false
    if (tag.lowercase() in RAW_TEXT_TAGS) return false
    val end = if (closing) scanClosingTag(line, start) else scanOpenTag(line, start)
    return end >= 0 && (end until line.length).all { line[it].isSpaceOrTab() }
}

private fun tagNameAt(
    line: String,
    start: Int,
): String? {
    if (!line.charAtOrEnd(start).isAsciiLetter()) return null
    var end = start + 1
    while (line.charAtOrEnd(end).let { it.isAsciiAlphanumeric() || it == '-' }) end++
    return line.substring(start, end)
}

/** Whether a tag name ending at [index] is followed by a space, tab, the end of the line, `>`, or `/>`. */
private fun isTagNameEnd(
    line: String,
    index: Int,
    allowSlash: Boolean,
): Boolean {
    if (index >= line.length) return true
    val c = line[index]
    return c.isSpaceOrTab() || c == '>' || (allowSlash && c == '/' && line.charAtOrEnd(index + 1) == '>')
}
