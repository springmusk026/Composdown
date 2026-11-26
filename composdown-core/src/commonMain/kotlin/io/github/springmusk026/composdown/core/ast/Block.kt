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
     * A table (GFM). Every row has exactly as many cells as there are [columns]: missing cells are
     * empty and extra cells are dropped.
     *
     * @property columns the alignment of each column, from the delimiter row.
     * @property header the header row.
     * @property rows the body rows, possibly none.
     */
    @Poko
    public class Table internal constructor(
        public val columns: ImmutableList<TableAlignment>,
        public val header: TableRow,
        public val rows: ImmutableList<TableRow>,
    ) : Block

    /**
     * A GitHub alert: a block quote that starts with `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`,
     * `[!WARNING]` or `[!CAUTION]` on its own line.
     *
     * @property type the kind of alert.
     * @property children the blocks after the marker line, possibly none while content is still arriving.
     */
    @Poko
    public class Alert internal constructor(
        public val type: AlertType,
        public val children: ImmutableList<Block>,
    ) : Block

    /**
     * A block of TeX math (the `MATH` extension).
     *
     * @property literal the TeX source, without delimiters.
     */
    @Poko
    public class MathBlock internal constructor(
        public val literal: String,
    ) : Block

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
 * @property taskState for a task list item (`- [ ]` or `- [x]`), whether it is checked; `null` for an
 *   ordinary item.
 */
@Poko
public class ListItem internal constructor(
    public val children: ImmutableList<Block>,
    public val taskState: TaskState?,
)

/** Whether a task list item is checked. */
public enum class TaskState {
    /** `[ ]` */
    UNCHECKED,

    /** `[x]` or `[X]` */
    CHECKED,
}

/** The horizontal alignment of a [Block.Table] column, set by colons in the delimiter row. */
public enum class TableAlignment {
    /** `---`: no alignment specified; renderers usually align to the start. */
    NONE,

    /** `:---` */
    LEFT,

    /** `:---:` */
    CENTER,

    /** `---:` */
    RIGHT,
}

/**
 * A row of a [Block.Table].
 *
 * @property cells the cells of the row, one per column.
 */
@Poko
public class TableRow internal constructor(
    public val cells: ImmutableList<TableCell>,
)

/**
 * A cell of a [TableRow]. Cells hold inline content only.
 *
 * @property content the inline elements of the cell.
 */
@Poko
public class TableCell internal constructor(
    public val content: ImmutableList<Inline>,
)

/** The kind of a [Block.Alert]. */
public enum class AlertType {
    /** `[!NOTE]`: useful information. */
    NOTE,

    /** `[!TIP]`: helpful advice. */
    TIP,

    /** `[!IMPORTANT]`: key information. */
    IMPORTANT,

    /** `[!WARNING]`: urgent information that needs attention. */
    WARNING,

    /** `[!CAUTION]`: advice about risks or negative outcomes. */
    CAUTION,
}

/**
 * The definition of a footnote, collected in [MarkdownDocument.footnotes].
 *
 * @property label the label used in the source, such as `note` for `[^note]: ...`.
 * @property number the footnote's number: its position in the order in which footnotes are first
 *   referenced, starting at 1.
 * @property children the blocks of the footnote.
 */
@Poko
public class FootnoteDefinition internal constructor(
    public val label: String,
    public val number: Int,
    public val children: ImmutableList<Block>,
)
