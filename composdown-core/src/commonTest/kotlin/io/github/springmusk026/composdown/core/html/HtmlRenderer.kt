package io.github.springmusk026.composdown.core.html

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.ListKind
import io.github.springmusk026.composdown.core.ast.MarkdownDocument

/**
 * Renders a [MarkdownDocument] to HTML exactly as the CommonMark reference implementations do, so
 * that parser output can be compared with the specification examples.
 */
internal object HtmlRenderer {
    fun render(document: MarkdownDocument): String =
        Writer().apply { blocks(document.blocks, tight = false) }.toString()

    private class Writer {
        private val out = StringBuilder()

        override fun toString(): String = out.toString()

        /** Starts a new line unless the output already ends with one (or is empty). */
        private fun cr() {
            if (out.isNotEmpty() && out.last() != '\n') out.append('\n')
        }

        fun blocks(
            blocks: List<Block>,
            tight: Boolean,
        ) {
            for (block in blocks) block(block, tight)
        }

        private fun block(
            block: Block,
            tight: Boolean,
        ) {
            when (block) {
                is Block.Paragraph -> {
                    if (tight) {
                        inlines(block.content)
                    } else {
                        cr()
                        out.append("<p>")
                        inlines(block.content)
                        out.append("</p>")
                        cr()
                    }
                }

                is Block.Heading -> {
                    cr()
                    out.append("<h").append(block.level).append('>')
                    inlines(block.content)
                    out.append("</h").append(block.level).append('>')
                    cr()
                }

                Block.ThematicBreak -> {
                    cr()
                    out.append("<hr />")
                    cr()
                }

                is Block.BlockQuote -> {
                    cr()
                    out.append("<blockquote>")
                    cr()
                    blocks(block.children, tight = false)
                    cr()
                    out.append("</blockquote>")
                    cr()
                }

                is Block.ListBlock -> {
                    list(block)
                }

                is Block.CodeBlock -> {
                    cr()
                    out.append("<pre><code")
                    block.info?.split(' ', '\t', '\n')?.first()?.takeIf { it.isNotEmpty() }?.let {
                        out.append(" class=\"language-").append(escape(it)).append('"')
                    }
                    out.append('>').append(escape(block.literal)).append("</code></pre>")
                    cr()
                }

                is Block.HtmlBlock -> {
                    cr()
                    out.append(block.literal)
                    cr()
                }
            }
        }

        private fun list(list: Block.ListBlock) {
            val tag = if (list.kind is ListKind.Ordered) "ol" else "ul"
            cr()
            out.append('<').append(tag)
            val kind = list.kind
            if (kind is ListKind.Ordered &&
                kind.startNumber != 1
            ) {
                out.append(" start=\"").append(kind.startNumber).append('"')
            }
            out.append('>')
            cr()
            for (item in list.items) {
                out.append("<li>")
                blocks(item.children, tight = list.isTight)
                out.append("</li>")
                cr()
            }
            cr()
            out.append("</").append(tag).append('>')
            cr()
        }

        private fun inlines(inlines: List<Inline>) {
            for (inline in inlines) inline(inline)
        }

        private fun inline(inline: Inline) {
            when (inline) {
                is Inline.Text -> {
                    out.append(escape(inline.literal))
                }

                is Inline.Code -> {
                    out.append("<code>").append(escape(inline.literal)).append("</code>")
                }

                is Inline.Emphasis -> {
                    out.append("<em>")
                    inlines(inline.children)
                    out.append("</em>")
                }

                is Inline.Strong -> {
                    out.append("<strong>")
                    inlines(inline.children)
                    out.append("</strong>")
                }

                is Inline.Link -> {
                    out.append("<a href=\"").append(escape(normalizeUri(inline.destination))).append('"')
                    inline.title?.let { out.append(" title=\"").append(escape(it)).append('"') }
                    out.append('>')
                    inlines(inline.children)
                    out.append("</a>")
                }

                is Inline.Image -> {
                    out.append("<img src=\"").append(escape(normalizeUri(inline.destination))).append("\" alt=\"")
                    altText(inline.children)
                    out.append('"')
                    inline.title?.let { out.append(" title=\"").append(escape(it)).append('"') }
                    out.append(" />")
                }

                is Inline.HtmlInline -> {
                    out.append(inline.literal)
                }

                Inline.SoftBreak -> {
                    out.append('\n')
                }

                Inline.HardBreak -> {
                    out.append("<br />\n")
                }
            }
        }

        /** Image descriptions are rendered as text only, without tags. */
        private fun altText(inlines: List<Inline>) {
            for (inline in inlines) {
                when (inline) {
                    is Inline.Text -> out.append(escape(inline.literal))
                    is Inline.Code -> out.append(escape(inline.literal))
                    is Inline.Emphasis -> altText(inline.children)
                    is Inline.Strong -> altText(inline.children)
                    is Inline.Link -> altText(inline.children)
                    is Inline.Image -> altText(inline.children)
                    is Inline.HtmlInline -> out.append(inline.literal)
                    Inline.SoftBreak, Inline.HardBreak -> out.append('\n')
                }
            }
        }
    }

    fun escape(text: String): String {
        if (text.none { it in "&<>\"" }) return text
        return buildString(text.length + 16) {
            for (c in text) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    else -> append(c)
                }
            }
        }
    }

    /**
     * Percent-encodes a URL the way the reference implementations do: characters other than ASCII
     * letters, digits and `;/?:@&=+$,-_.!~*'()#` are UTF-8 encoded, and existing `%XX` escapes are kept.
     */
    fun normalizeUri(uri: String): String =
        buildString(uri.length) {
            var i = 0
            while (i < uri.length) i += appendNormalized(uri, i)
        }

    /** Appends the normalized form of the character(s) at [index] and returns how many were consumed. */
    private fun StringBuilder.appendNormalized(
        uri: String,
        index: Int,
    ): Int {
        val c = uri[index]
        val hasLowSurrogateAfter = index + 1 < uri.length && uri[index + 1].isLowSurrogate()
        return when {
            isPercentEscape(uri, index) -> 3.also { append(uri, index, index + 3) }
            c.code < 0x80 && (c.isLetterOrDigit() || c in SAFE_URI_CHARACTERS) -> 1.also { append(c) }
            c.isHighSurrogate() && hasLowSurrogateAfter -> 2.also { percentEncode(uri.substring(index, index + 2)) }
            c.isSurrogate() -> 1.also { append("%EF%BF%BD") }
            else -> 1.also { percentEncode(c.toString()) }
        }
    }

    private fun isPercentEscape(
        uri: String,
        index: Int,
    ): Boolean =
        uri[index] == '%' && index + 2 < uri.length && uri[index + 1].isHexDigit() && uri[index + 2].isHexDigit()

    private fun StringBuilder.percentEncode(text: String) {
        for (byte in text.encodeToByteArray()) {
            append('%').append(HEX_DIGITS[(byte.toInt() shr 4) and 0xF]).append(HEX_DIGITS[byte.toInt() and 0xF])
        }
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private const val SAFE_URI_CHARACTERS = ";/?:@&=+$,-_.!~*'()#"
    private const val HEX_DIGITS = "0123456789ABCDEF"
}
