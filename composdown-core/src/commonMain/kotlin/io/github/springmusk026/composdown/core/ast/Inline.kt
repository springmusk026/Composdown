package io.github.springmusk026.composdown.core.ast

import io.github.springmusk026.composdown.core.internal.Poko
import kotlinx.collections.immutable.ImmutableList

/**
 * An inline element inside a paragraph, heading or other block with inline content.
 *
 * The set of inline types is closed, so renderers can handle every case with an exhaustive `when`.
 * Adjacent text is always merged into a single [Text] element.
 */
public sealed interface Inline {
    /**
     * Plain text, with backslash escapes and character references already resolved.
     *
     * @property literal the text.
     */
    @Poko
    public class Text internal constructor(
        public val literal: String,
    ) : Inline

    /**
     * A code span (`` `code` ``).
     *
     * @property literal the code, with line endings converted to spaces.
     */
    @Poko
    public class Code internal constructor(
        public val literal: String,
    ) : Inline

    /**
     * Emphasis (`*text*` or `_text_`), usually rendered in italics.
     *
     * @property children the emphasized content.
     */
    @Poko
    public class Emphasis internal constructor(
        public val children: ImmutableList<Inline>,
    ) : Inline

    /**
     * Strong emphasis (`**text**` or `__text__`), usually rendered in bold.
     *
     * @property children the emphasized content.
     */
    @Poko
    public class Strong internal constructor(
        public val children: ImmutableList<Inline>,
    ) : Inline

    /**
     * Strikethrough (`~~text~~` or `~text~`, GFM), usually rendered with a line through the text.
     *
     * @property children the struck-through content.
     */
    @Poko
    public class Strikethrough internal constructor(
        public val children: ImmutableList<Inline>,
    ) : Inline

    /**
     * A reference to a footnote (`[^label]`). Only references to defined footnotes become footnote
     * references; others remain text.
     *
     * @property label the label of the referenced footnote's definition.
     * @property number the number of the referenced footnote; see [FootnoteDefinition.number].
     */
    @Poko
    public class FootnoteReference internal constructor(
        public val label: String,
        public val number: Int,
    ) : Inline

    /**
     * A link: inline (`[text](url "title")`), reference (`[text][label]`) or autolink (`<url>`).
     *
     * @property destination the link destination, with escapes and character references resolved.
     *   It is not percent-encoded and not validated; applications decide which URLs to open.
     * @property title the link title, or `null` if there is none.
     * @property children the link text.
     */
    @Poko
    public class Link internal constructor(
        public val destination: String,
        public val title: String?,
        public val children: ImmutableList<Inline>,
    ) : Inline

    /**
     * An image (`![description](url "title")`).
     *
     * @property destination the image source, with escapes and character references resolved.
     * @property title the image title, or `null` if there is none.
     * @property children the image description. Use [plainText] to get it as alternative text.
     */
    @Poko
    public class Image internal constructor(
        public val destination: String,
        public val title: String?,
        public val children: ImmutableList<Inline>,
    ) : Inline

    /**
     * Raw inline HTML, such as `<span>`. Composdown never interprets HTML.
     *
     * @property literal the raw HTML source.
     */
    @Poko
    public class HtmlInline internal constructor(
        public val literal: String,
    ) : Inline

    /** A line break in the source that is not a hard break. Usually rendered as a space. */
    public data object SoftBreak : Inline

    /** A hard line break: a line ending preceded by two or more spaces or a backslash. */
    public data object HardBreak : Inline
}

/**
 * Returns the text content of these inline elements with all formatting removed, for example to use
 * as an image's alternative text or a heading's accessibility label.
 *
 * Soft and hard line breaks become a line feed. Raw HTML is omitted.
 */
public fun Iterable<Inline>.plainText(): String = buildString { appendPlainText(this@plainText) }

private fun StringBuilder.appendPlainText(inlines: Iterable<Inline>) {
    for (inline in inlines) {
        when (inline) {
            is Inline.Text -> append(inline.literal)
            is Inline.Code -> append(inline.literal)
            is Inline.Emphasis -> appendPlainText(inline.children)
            is Inline.Strong -> appendPlainText(inline.children)
            is Inline.Strikethrough -> appendPlainText(inline.children)
            is Inline.FootnoteReference -> append(inline.number)
            is Inline.Link -> appendPlainText(inline.children)
            is Inline.Image -> appendPlainText(inline.children)
            is Inline.HtmlInline -> Unit
            Inline.SoftBreak, Inline.HardBreak -> append('\n')
        }
    }
}
