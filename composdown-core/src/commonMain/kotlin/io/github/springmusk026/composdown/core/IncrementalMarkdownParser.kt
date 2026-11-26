package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.internal.IncrementalEngine

/**
 * Parses a Markdown document that arrives in pieces, such as a response streamed from a language
 * model, re-parsing as little as possible on each update.
 *
 * Completed top-level blocks are parsed once and then reused: they keep the same instances in every
 * later [document], so UI frameworks can skip them cheaply. Only the last, still-open block and the
 * line being written are parsed again on each update. Later text can change a completed block in
 * one way only: a link reference or footnote definition that resolves a label the block uses.
 *
 * ```
 * val parser = IncrementalMarkdownParser()
 * tokens.collect { token -> render(parser.append(token)) }
 * render(parser.finish())
 * ```
 *
 * Instances are not thread-safe: call them from one thread at a time.
 *
 * @property extensions the syntax extensions to recognize. Defaults to GitHub Flavored Markdown.
 * @property streamingPolicy how incomplete syntax at the end of the text is presented before [finish].
 */
public class IncrementalMarkdownParser(
    public val extensions: Set<MarkdownExtension> = MarkdownExtension.GFM,
    public val streamingPolicy: StreamingPolicy = StreamingPolicy.OPTIMISTIC,
) {
    private val engine = IncrementalEngine(extensions, optimistic = streamingPolicy == StreamingPolicy.OPTIMISTIC)

    /** All text received so far. */
    public val text: String get() = engine.text

    /** The document for the text received so far, as returned by the last update. */
    public val document: MarkdownDocument get() = engine.document

    /** Whether [finish] was called and no text was added since. */
    public val isFinished: Boolean get() = engine.isFinished

    /**
     * Appends [chunk] to the text and returns the updated document. Chunks may split the text
     * anywhere, including inside a line ending or a multi-character construct.
     *
     * Appending after [finish] continues the document; the parser then re-parses it once.
     */
    public fun append(chunk: String): MarkdownDocument = engine.append(chunk)

    /**
     * Replaces the whole text and returns the updated document. If [text] starts with the current
     * text, this is as cheap as [append]; otherwise the document is parsed again, and blocks equal to
     * blocks of the previous document keep their instances.
     */
    public fun setText(text: String): MarkdownDocument = engine.setText(text)

    /**
     * Declares that the text is complete and returns the final document, which is exactly what
     * [MarkdownParser.parse] produces for [text].
     */
    public fun finish(): MarkdownDocument = engine.finish()
}
