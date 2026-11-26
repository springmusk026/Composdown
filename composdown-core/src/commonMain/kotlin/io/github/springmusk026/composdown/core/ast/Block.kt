package io.github.springmusk026.composdown.core.ast

import io.github.springmusk026.composdown.core.internal.Poko
import kotlinx.collections.immutable.ImmutableList

/**
 * A block-level element of a Markdown document, such as a paragraph, heading or list.
 *
 * The set of block types is closed, so renderers can handle every case with an exhaustive `when`.
 */
public sealed interface Block {
    /**
     * A paragraph of inline content.
     *
     * @property content the inline elements of the paragraph.
     */
    @Poko
    public class Paragraph internal constructor(
        public val content: ImmutableList<Inline>,
    ) : Block

    /**
     * An ATX (`# Heading`) or setext (`Heading` underlined with `=` or `-`) heading.
     *
     * @property level the heading level, from 1 to 6.
     * @property content the inline elements of the heading.
     */
    @Poko
    public class Heading internal constructor(
        public val level: Int,
        public val content: ImmutableList<Inline>,
    ) : Block

    /** A thematic break (`---`, `***` or `___`), usually rendered as a horizontal rule. */
    public data object ThematicBreak : Block

    /**
     * A block quote (`> quoted text`).
     *
     * @property children the blocks inside the quote.
     */
    @Poko
    public class BlockQuote internal constructor(
        public val children: ImmutableList<Block>,
    ) : Block

    /**
     * A bullet or ordered list.
     *
     * @property kind whether the list is a bullet or an ordered list, with its marker details.
     * @property isTight `true` if no list item is separated from the next by a blank line and no
     *   item contains blocks separated by a blank line. Tight lists are usually rendered with less
     *   vertical spacing.
     * @property items the list items, in order.
     */
    @Poko
    public class ListBlock internal constructor(
        public val kind: ListKind,
        public val isTight: Boolean,
        public val items: ImmutableList<ListItem>,
    ) : Block

    /**
     * A fenced (```` ``` ````) or indented code block.
     *
     * @property literal the code, ending with a line feed unless the block is empty.
     * @property info the info string after the opening fence (for example `kotlin title="a.kt"`),
     *   or `null` for indented code blocks and fences without one.
     * @property isFenced `true` for fenced code blocks, `false` for indented code blocks.
     */
    @Poko
    public class CodeBlock internal constructor(
        public val literal: String,
        public val info: String?,
        public val isFenced: Boolean,
    ) : Block {
        /** The first word of [info], conventionally the language of the code, or `null` if there is none. */
        public val language: String?
            get() = info?.substringBefore(' ')?.substringBefore('\t')?.takeIf { it.isNotEmpty() }
    }

    /**
     * A block of raw HTML. Composdown never interprets HTML; renderers decide whether to show the
     * source text or hide it.
     *
     * @property literal the raw HTML source.
     */
    @Poko
    public class HtmlBlock internal constructor(
        public val literal: String,
    ) : Block
}

/**
 * The type of a [Block.ListBlock].
 */
public sealed interface ListKind {
    /**
     * A bullet list.
     *
     * @property marker the bullet character used in the source: `-`, `+` or `*`.
     */
    @Poko
    public class Bullet internal constructor(
        public val marker: Char,
    ) : ListKind

    /**
     * An ordered list.
     *
     * @property startNumber the number of the first item.
     * @property delimiter the character after the number in the source: `.` or `)`.
     */
    @Poko
    public class Ordered internal constructor(
        public val startNumber: Int,
        public val delimiter: Char,
    ) : ListKind
}

/**
 * An item of a [Block.ListBlock].
 *
 * @property children the blocks inside the item.
 */
@Poko
public class ListItem internal constructor(
    public val children: ImmutableList<Block>,
)
