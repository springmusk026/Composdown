package io.github.springmusk026.composdown.core.ast

import io.github.springmusk026.composdown.core.internal.Poko
import kotlinx.collections.immutable.ImmutableList

/**
 * The result of parsing a Markdown document: an immutable tree of [Block]s, plus the footnotes
 * referenced from it.
 *
 * Instances are produced by [io.github.springmusk026.composdown.core.MarkdownParser]. All model
 * types are immutable and implement structural equality, so unchanged parts of a document compare
 * equal between parses.
 *
 * @property blocks the top-level blocks of the document, in source order. Footnote definitions are
 *   not included here.
 * @property footnotes the definitions of the footnotes that are referenced in the document, in the
 *   order of their [numbers][FootnoteDefinition.number]. Unreferenced definitions are omitted.
 */
@Poko
public class MarkdownDocument internal constructor(
    public val blocks: ImmutableList<Block>,
    public val footnotes: ImmutableList<FootnoteDefinition>,
)
