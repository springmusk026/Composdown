package io.github.springmusk026.composdown.core.html

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.FootnoteDefinition
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.ListKind
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.ast.TableAlignment
import io.github.springmusk026.composdown.core.ast.TableRow
import io.github.springmusk026.composdown.core.ast.TaskState

/**
 * Renders a [MarkdownDocument] to HTML exactly as the CommonMark reference implementations
 * (commonmark.js, and cmark-gfm for GFM extensions) do, so that parser output can be compared with
 * the specification examples.
 */
internal object HtmlRenderer {
    fun render(document: MarkdownDocument): String = Writer(document).render()

    private class Writer(
        private val document: MarkdownDocument,
    ) {
        private val out = StringBuilder()

        /** Total references per footnote label, for the back-references of each definition. */
        private val referenceCounts = HashMap<String, Int>()

        /** References rendered so far per footnote label, for unique reference ids. */
        private val renderedReferences = HashMap<String, Int>()

        fun render(): String {
            countFootnoteReferences()
            blocks(document.blocks, tight = false)
            if (document.footnotes.isNotEmpty()) footnotes()
            return out.toString()
        }

        /** Starts a new line unless the output already ends with one (or is empty). */
        private fun cr() {
            if (out.isNotEmpty() && out.last() != '\n') out.append('\n')
        }

        private fun blocks(
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
                    paragraph(block, tight, suffix = null)
                }

                is Block.Heading -> {
                    cr()
                    out.append("<h").append(block.level).append('>')
                    inlines(block.content)
                    out.append("</h").append(block.level).append('>')
                    cr()
                }

                Block.ThematicBreak -> {
                    line("<hr />")
                }

                is Block.BlockQuote -> {
                    container("<blockquote>", "</blockquote>") { blocks(block.children, tight = false) }
                }

                is Block.Alert -> {
                    val name = block.type.name.lowercase()
                    container("<div class=\"markdown-alert markdown-alert-$name\">", "</div>") {
                        line("<p class=\"markdown-alert-title\">${name.replaceFirstChar(Char::uppercaseChar)}</p>")
                        blocks(block.children, tight = false)
                    }
                }

                is Block.ListBlock -> {
                    list(block)
                }

                is Block.CodeBlock -> {
                    codeBlock(block)
                }

                is Block.HtmlBlock -> {
                    line(block.literal)
                }

                is Block.Table -> {
                    table(block)
                }

                is Block.MathBlock -> {
                    line("<div class=\"math\">${escape(block.literal)}</div>")
                }
            }
        }

        private fun line(html: String) {
            cr()
            out.append(html)
            cr()
        }

        private inline fun container(
            open: String,
            close: String,
            content: () -> Unit,
        ) {
            line(open)
            content()
            line(close)
        }

        /** Renders a paragraph, appending [suffix] (raw HTML) inside it if given. */
        private fun paragraph(
            paragraph: Block.Paragraph,
            tight: Boolean,
            suffix: String?,
        ) {
            if (!tight) {
                cr()
                out.append("<p>")
            }
            inlines(paragraph.content)
            suffix?.let { out.append(' ').append(it) }
            if (!tight) {
                out.append("</p>")
                cr()
            }
        }

        private fun codeBlock(block: Block.CodeBlock) {
            cr()
            out.append("<pre><code")
            block.info?.split(' ', '\t', '\n')?.first()?.takeIf { it.isNotEmpty() }?.let {
                out.append(" class=\"language-").append(escape(it)).append('"')
            }
            out.append('>').append(escape(block.literal)).append("</code></pre>")
            cr()
        }

        private fun list(list: Block.ListBlock) {
            val kind = list.kind
            val tag = if (kind is ListKind.Ordered) "ol" else "ul"
            val start = if (kind is ListKind.Ordered && kind.startNumber != 1) " start=\"${kind.startNumber}\"" else ""
            line("<$tag$start>")
            for (item in list.items) {
                out.append("<li>")
                when (item.taskState) {
                    TaskState.CHECKED -> out.append("<input type=\"checkbox\" checked=\"\" disabled=\"\" /> ")
                    TaskState.UNCHECKED -> out.append("<input type=\"checkbox\" disabled=\"\" /> ")
                    null -> Unit
                }
                blocks(item.children, tight = list.isTight)
                out.append("</li>")
                cr()
            }
            line("</$tag>")
        }

        private fun table(table: Block.Table) {
            line("<table>")
            line("<thead>")
            tableRow(table.header, "th", table.columns)
            line("</thead>")
            if (table.rows.isNotEmpty()) {
                line("<tbody>")
                for (row in table.rows) tableRow(row, "td", table.columns)
                line("</tbody>")
            }
            line("</table>")
        }

        private fun tableRow(
            row: TableRow,
            tag: String,
            columns: List<TableAlignment>,
        ) {
            line("<tr>")
            row.cells.forEachIndexed { index, cell ->
                out.append('<').append(tag)
                when (columns[index]) {
                    TableAlignment.LEFT -> out.append(" align=\"left\"")
                    TableAlignment.CENTER -> out.append(" align=\"center\"")
                    TableAlignment.RIGHT -> out.append(" align=\"right\"")
                    TableAlignment.NONE -> Unit
                }
                out.append('>')
                inlines(cell.content)
                out.append("</").append(tag).append('>')
                cr()
            }
            line("</tr>")
        }

        private fun footnotes() {
            line("<section class=\"footnotes\" data-footnotes>")
            line("<ol>")
            for (footnote in document.footnotes) {
                line("<li id=\"fn-${hrefEscape(footnote.label)}\">")
                val backReferences = backReferences(footnote)
                val last = footnote.children.lastOrNull()
                if (last is Block.Paragraph) {
                    blocks(footnote.children.dropLast(1), tight = false)
                    paragraph(last, tight = false, suffix = backReferences)
                } else {
                    blocks(footnote.children, tight = false)
                    line(backReferences)
                }
                line("</li>")
            }
            line("</ol>")
            line("</section>")
        }

        private fun backReferences(footnote: FootnoteDefinition): String {
            val label = hrefEscape(footnote.label)
            val count = referenceCounts[footnote.label] ?: 1
            return (1..count).joinToString(" ") { occurrence ->
                val suffix = if (occurrence == 1) "" else "-$occurrence"
                val index = "${footnote.number}$suffix"
                val superscript = if (occurrence == 1) "" else "<sup class=\"footnote-ref\">$occurrence</sup>"
                "<a href=\"#fnref-$label$suffix\" class=\"footnote-backref\" data-footnote-backref " +
                    "data-footnote-backref-idx=\"$index\" aria-label=\"Back to reference $index\">↩$superscript</a>"
            }
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
                    wrap("em", inline.children)
                }

                is Inline.Strong -> {
                    wrap("strong", inline.children)
                }

                is Inline.Strikethrough -> {
                    wrap("del", inline.children)
                }

                is Inline.Link -> {
                    link(inline)
                }

                is Inline.Image -> {
                    image(inline)
                }

                is Inline.FootnoteReference -> {
                    footnoteReference(inline)
                }

                is Inline.Math -> {
                    val kind = if (inline.isDisplay) "math display" else "math inline"
                    out
                        .append(
                            "<span class=\"",
                        ).append(kind)
                        .append("\">")
                        .append(escape(inline.literal))
                        .append("</span>")
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

        private fun link(inline: Inline.Link) {
            out.append("<a href=\"").append(escape(normalizeUri(inline.destination))).append('"')
            inline.title?.let { out.append(" title=\"").append(escape(it)).append('"') }
            out.append('>')
            inlines(inline.children)
            out.append("</a>")
        }

        private fun image(inline: Inline.Image) {
            out.append("<img src=\"").append(escape(normalizeUri(inline.destination))).append("\" alt=\"")
            altText(inline.children)
            out.append('"')
            inline.title?.let { out.append(" title=\"").append(escape(it)).append('"') }
            out.append(" />")
        }

        private fun wrap(
            tag: String,
            children: List<Inline>,
        ) {
            out.append('<').append(tag).append('>')
            inlines(children)
            out.append("</").append(tag).append('>')
        }

        private fun footnoteReference(reference: Inline.FootnoteReference) {
            val occurrence = (renderedReferences[reference.label] ?: 0) + 1
            renderedReferences[reference.label] = occurrence
            val label = hrefEscape(reference.label)
            val id = if (occurrence == 1) "fnref-$label" else "fnref-$label-$occurrence"
            out
                .append("<sup class=\"footnote-ref\"><a href=\"#fn-$label\" id=\"$id\" data-footnote-ref>")
                .append(reference.number)
                .append("</a></sup>")
        }

        /** Image descriptions are rendered as text only, without tags. */
        private fun altText(inlines: List<Inline>) {
            for (inline in inlines) {
                when (inline) {
                    is Inline.Text -> out.append(escape(inline.literal))
                    is Inline.Code -> out.append(escape(inline.literal))
                    is Inline.Emphasis -> altText(inline.children)
                    is Inline.Strong -> altText(inline.children)
                    is Inline.Strikethrough -> altText(inline.children)
                    is Inline.Link -> altText(inline.children)
                    is Inline.Image -> altText(inline.children)
                    is Inline.FootnoteReference -> out.append(inline.number)
                    is Inline.Math -> out.append(escape(inline.literal))
                    is Inline.HtmlInline -> out.append(inline.literal)
                    Inline.SoftBreak, Inline.HardBreak -> out.append('\n')
                }
            }
        }

        private fun countFootnoteReferences() {
            val pending = ArrayDeque<Any>()
            pending.addAll(document.blocks)
            document.footnotes.forEach { pending.addAll(it.children) }
            while (pending.isNotEmpty()) {
                when (val node = pending.removeFirst()) {
                    is Inline.FootnoteReference -> referenceCounts[node.label] = (referenceCounts[node.label] ?: 0) + 1
                    is Block -> pending.addAll(node.childrenForCounting())
                    is Inline -> pending.addAll(node.childrenForCounting())
                }
            }
        }

        private fun Block.childrenForCounting(): List<Any> =
            when (this) {
                is Block.Paragraph -> content
                is Block.Heading -> content
                is Block.BlockQuote -> children
                is Block.Alert -> children
                is Block.ListBlock -> items.flatMap { it.children }
                is Block.Table -> (listOf(header) + rows).flatMap { row -> row.cells.flatMap { it.content } }
                Block.ThematicBreak, is Block.CodeBlock, is Block.HtmlBlock, is Block.MathBlock -> emptyList()
            }

        private fun Inline.childrenForCounting(): List<Any> =
            when (this) {
                is Inline.Emphasis -> children
                is Inline.Strong -> children
                is Inline.Strikethrough -> children
                is Inline.Link -> children
                is Inline.Image -> children
                else -> emptyList()
            }
    }

    /** Escapes a footnote label for use in an `href` or `id` attribute, as cmark-gfm does. */
    fun hrefEscape(label: String): String = escape(normalizeUri(label))

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
