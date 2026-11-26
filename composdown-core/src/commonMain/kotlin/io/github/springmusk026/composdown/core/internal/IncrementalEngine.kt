package io.github.springmusk026.composdown.core.internal

import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.FootnoteDefinition
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.internal.block.BlockNode
import io.github.springmusk026.composdown.core.internal.block.BlockParser
import io.github.springmusk026.composdown.core.internal.block.ParagraphNode
import io.github.springmusk026.composdown.core.internal.block.TableNode
import io.github.springmusk026.composdown.core.internal.inline.ConversionTrace
import io.github.springmusk026.composdown.core.internal.inline.FootnoteRegistry
import io.github.springmusk026.composdown.core.internal.inline.InlineParser
import io.github.springmusk026.composdown.core.internal.inline.LinkReferenceMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

/** A converted footnote definition: its label as written and its blocks. */
private typealias ConvertedFootnote = Pair<String, ImmutableList<Block>>

/**
 * Parses a Markdown document that grows over time, re-parsing as little as possible.
 *
 * A persistent [BlockParser] receives only complete lines. Blocks it has closed can never change
 * (CommonMark blocks never reopen), so each closed top-level block is converted once and committed.
 * The rest of the text (the last, still-open top-level block and the line being written) is the
 * tail: it is parsed again, from the start of that block, on every update. The work per update is
 * proportional to the tail, not to the document.
 *
 * Link reference and footnote definitions are the only way later text affects earlier blocks. Each
 * committed block records the labels it could not resolve; when a definition for one is committed,
 * that block is converted again. Footnote numbers follow the order of first reference, so they are
 * recomputed over all committed blocks and the blocks whose numbers changed are converted again.
 * Definitions in the tail are provisional and never affect committed blocks.
 *
 * After [finish], the document is exactly what a one-shot parse of the same text produces.
 */
internal class IncrementalEngine(
    private val extensions: Set<MarkdownExtension>,
    private val optimistic: Boolean,
) {
    private val source = StringBuilder()
    private var live = LiveParse()
    private var finished = false

    /** The tail blocks of the last document, so that equal blocks keep their instances across updates. */
    private var previousTail: List<Block> = emptyList()

    /**
     * Incremented whenever committed blocks change: added, converted again, or replaced by a new
     * live parse. Never repeats, so a cached [CommittedView] can always be checked against it.
     */
    private var committedVersion = 0L

    /** What is derived from committed blocks, rebuilt only when they change. */
    private var committedView = CommittedView(version = -1L, persistentListOf(), emptyMap(), emptyList())

    var document: MarkdownDocument = MarkdownDocument(persistentListOf(), persistentListOf())
        private set

    val text: String get() = source.toString()

    val isFinished: Boolean get() = finished

    fun append(chunk: String): MarkdownDocument {
        if (finished) {
            // A finished parse has closed every block; start over to continue the document.
            finished = false
            restartLiveParse()
        }
        source.append(chunk)
        feedCompleteLines()
        return publish(parseTail())
    }

    fun setText(text: String): MarkdownDocument {
        if (!finished && text.length >= source.length && text.startsWith(source)) {
            return append(text.substring(source.length))
        }
        source.setLength(0)
        source.append(text)
        finished = false
        restartLiveParse()
        feedCompleteLines()
        return publish(parseTail())
    }

    fun finish(): MarkdownDocument {
        if (finished) return document
        if (live.fedEnd < source.length) live.addLine(source.substring(live.fedEnd).trimEnd('\r', '\n'), live.fedEnd)
        live.parser.finish()
        live.commitClosedBlocks()
        finished = true
        return publish(tail = null)
    }

    /** Parses [markdown] as a complete document in one pass. */
    fun parseComplete(markdown: String): MarkdownDocument {
        source.setLength(0)
        source.append(markdown)
        restartLiveParse()
        forEachLine(markdown) { live.parser.addLine(it) }
        live.parser.finish()
        live.commitClosedBlocks()
        finished = true
        return publish(tail = null)
    }

    /** Discards all parse state, so that the text is parsed again from its start. */
    private fun restartLiveParse() {
        live = LiveParse()
        committedVersion++
    }

    /** Feeds lines that are known to be complete to the live parser and commits closed blocks. */
    private fun feedCompleteLines() {
        var lineStart = live.fedEnd
        while (true) {
            val end = lineEnd(source, lineStart)
            // A line is complete once its line break is known: a `\r` at the very end may still be
            // followed by `\n`.
            val complete = end < source.length && (source[end] == '\n' || end + 1 < source.length)
            if (!complete) break
            live.addLine(source.substring(lineStart, end), lineStart)
            lineStart = afterLineBreak(source, end)
            live.fedEnd = lineStart
        }
        live.commitClosedBlocks()
    }

    /** Parses the open part of the document, or returns `null` if there is none. */
    private fun parseTail(): TailParse? {
        val startLine = live.tailStartLine()
        val start = if (startLine != null) live.lineStarts[startLine - 1] else live.fedEnd
        val end = source.length - heldBackLength(start)
        if (end <= start) return null
        val text = source.substring(start, end)
        val tail = TailParse(text)
        // A paragraph line that looks like a table header is held back until the next line shows
        // whether it starts a table, so that the header does not flash as paragraph text.
        if (!tail.endsWithTableHeaderCandidate()) return tail
        val lastLineStart = lastLineStart(text)
        return if (lastLineStart > 0) TailParse(text.substring(0, lastLineStart)) else null
    }

    /**
     * Length of the line being written if it could still turn into a block marker (`` ` ``, `-`,
     * `1.`, `|`, ...), and so should not be shown yet. Only in optimistic mode.
     */
    private fun heldBackLength(tailStart: Int): Int {
        if (!optimistic) return 0
        var lineStart = source.length
        while (lineStart > tailStart && source[lineStart - 1] != '\n' && source[lineStart - 1] != '\r') lineStart--
        val partial = source.subSequence(lineStart, source.length)
        return if (mayBecomeBlockMarker(partial)) partial.length else 0
    }

    /** Start of the last line of [text] that has content, ignoring a final line break. */
    private fun lastLineStart(text: String): Int {
        var end = text.length
        while (end > 0 && (text[end - 1] == '\n' || text[end - 1] == '\r')) end--
        var start = end
        while (start > 0 && text[start - 1] != '\n' && text[start - 1] != '\r') start--
        return start
    }

    /**
     * Publishes committed blocks followed by the tail's. The work is proportional to the tail: what
     * depends on committed blocks only is cached in [committedView] until they change.
     */
    private fun publish(tail: TailParse?): MarkdownDocument {
        val view = committedView()
        val tailBlocks =
            tail?.blocks.orEmpty().mapIndexed { index, block ->
                previousTail.getOrNull(index)?.takeIf { it == block } ?: block
            }
        previousTail = tailBlocks
        val footnotes = if (tail != null && tail.involvesFootnotes) mergeFootnotes(view, tail) else view.footnotes
        document = MarkdownDocument(view.blocks.addingAll(tailBlocks), footnotes.toImmutableList())
        return document
    }

    private fun committedView(): CommittedView {
        val previous = committedView
        if (previous.version == committedVersion) return previous
        // A block that was in the tail of the last document keeps that instance once committed.
        val published = document.blocks
        val blocks =
            live.committed.mapNotNull { it.block }.mapIndexed { index, block ->
                published.getOrNull(index)?.takeIf { it === block || it == block } ?: block
            }
        val definitions = LinkedHashMap<String, ConvertedFootnote>()
        for (committed in live.committed) {
            for ((key, definition) in committed.footnotes) {
                if (key !in definitions) definitions[key] = definition
            }
        }
        val footnotes =
            numberedFootnotes(definitions) { key -> live.footnotes?.numberOfKey(key) }.mapIndexed { index, footnote ->
                previous.footnotes.getOrNull(index)?.takeIf { it == footnote } ?: footnote
            }
        return CommittedView(committedVersion, blocks.toPersistentList(), definitions, footnotes).also {
            committedView =
                it
        }
    }

    /** Footnotes when the tail defines or references any: committed and tail definitions together. */
    private fun mergeFootnotes(
        view: CommittedView,
        tail: TailParse,
    ): List<FootnoteDefinition> {
        val definitions = LinkedHashMap(view.definitions)
        for ((key, definition) in tail.footnotes) {
            if (key !in definitions) definitions[key] = definition
        }
        return numberedFootnotes(definitions, tail::numberOfKey)
    }

    /** The referenced footnotes among [definitions], in number order. */
    private fun numberedFootnotes(
        definitions: Map<String, ConvertedFootnote>,
        numberOf: (String) -> Int?,
    ): List<FootnoteDefinition> =
        definitions
            .mapNotNull { (key, definition) ->
                numberOf(key)?.let { FootnoteDefinition(definition.first, it, definition.second) }
            }.sortedBy { it.number }

    /** Converts one top-level block with the given definitions, recording what it depended on. */
    private fun convert(
        node: BlockNode,
        references: LinkReferenceMap,
        footnotes: FootnoteRegistry?,
    ): Converted {
        val trace = ConversionTrace()
        val builder = astBuilder(references, footnotes, streamingLeaf = null, trace)
        return Converted(builder.topLevelBlock(node), HashMap(builder.collectedFootnotes), trace)
    }

    private fun astBuilder(
        references: LinkReferenceMap,
        footnotes: FootnoteRegistry?,
        streamingLeaf: BlockNode?,
        trace: ConversionTrace?,
    ): AstBuilder {
        val inlineParser = InlineParser(references, extensions, footnotes).also { it.trace = trace }
        return AstBuilder(InlineConverter(inlineParser), extensions, footnotes, streamingLeaf)
    }

    private class Converted(
        val block: Block?,
        val footnotes: Map<String, ConvertedFootnote>,
        val trace: ConversionTrace,
    )

    /** A committed top-level block: final in structure, converted again only if a definition resolves. */
    private class CommittedBlock(
        val node: BlockNode,
        var block: Block?,
        var footnotes: Map<String, ConvertedFootnote>,
        var trace: ConversionTrace,
    )

    /** Committed blocks and footnotes, valid while [committedVersion] equals [version]. */
    private class CommittedView(
        val version: Long,
        val blocks: PersistentList<Block>,
        val definitions: Map<String, ConvertedFootnote>,
        val footnotes: List<FootnoteDefinition>,
    )

    /** The persistent parse of the complete lines of the document. */
    private inner class LiveParse {
        val references = LinkReferenceMap()
        val footnotes: FootnoteRegistry? = if (MarkdownExtension.FOOTNOTES in extensions) FootnoteRegistry() else null
        val parser = BlockParser(references, extensions, footnotes)

        /** Source offset of each line given to [parser]; index 0 is line 1. */
        val lineStarts = ArrayList<Int>()

        /** Source offset after the last complete line given to [parser]. */
        var fedEnd = 0

        val committed = ArrayList<CommittedBlock>()
        private var seenLinkDefinitions = 0
        private var seenFootnoteDefinitions = 0

        fun addLine(
            line: String,
            start: Int,
        ) {
            lineStarts.add(start)
            parser.addLine(line)
        }

        /**
         * The first line of the tail: the start of the open top-level block, or `null` if every
         * top-level block is closed. Blocks starting at or after it are not committed.
         */
        fun tailStartLine(): Int? {
            val open =
                parser.document.children
                    .lastOrNull()
                    ?.takeIf { it.isOpen } ?: return null
            return if (open is TableNode && open.splitFromLine > 0) open.splitFromLine else open.startLine
        }

        fun commitClosedBlocks() {
            val children = parser.document.children
            val firstNew = committed.size
            val tailStart = tailStartLine() ?: Int.MAX_VALUE
            while (committed.size < children.size &&
                children[committed.size].let { !it.isOpen && it.startLine < tailStart }
            ) {
                val node = children[committed.size]
                val converted = convert(node, references, footnotes)
                committed.add(CommittedBlock(node, converted.block, converted.footnotes, converted.trace))
                committedVersion++
            }
            // A paragraph made only of definitions is removed when it closes, so new definitions can
            // appear without any new block.
            val newDefinitions =
                references.size > seenLinkDefinitions || (footnotes?.definedCount ?: 0) > seenFootnoteDefinitions
            if (committed.size > firstNew || newDefinitions) resolveRetroactively()
        }

        /** Converts committed blocks again whose unresolved labels are now defined, and renumbers footnotes. */
        private fun resolveRetroactively() {
            val newLinks = (seenLinkDefinitions until references.size).mapTo(HashSet(), references::labelAt)
            seenLinkDefinitions = references.size
            val newFootnotes =
                footnotes
                    ?.definedKeys
                    ?.drop(seenFootnoteDefinitions)
                    ?.toHashSet()
                    .orEmpty()
            seenFootnoteDefinitions = footnotes?.definedCount ?: 0
            if (newLinks.isNotEmpty() || newFootnotes.isNotEmpty()) {
                for (block in committed) {
                    val missed =
                        block.trace.linkMisses.any { it in newLinks } ||
                            block.trace.footnoteMisses.any { it in newFootnotes }
                    if (missed) reconvert(block)
                }
            }
            renumberFootnotes()
        }

        /** Footnotes are numbered in order of first reference across all committed blocks. */
        private fun renumberFootnotes() {
            val registry = footnotes ?: return
            val numbers = LinkedHashMap<String, Int>()
            for (block in committed) {
                for (key in block.trace.footnoteKeys) numbers.getOrPut(key) { numbers.size + 1 }
            }
            if (numbers == registry.ownNumbers()) return
            registry.setNumbers(numbers)
            for (block in committed) {
                if (block.trace.footnoteKeys.map { numbers[it] } != block.trace.footnoteNumbers) reconvert(block)
            }
        }

        private fun reconvert(block: CommittedBlock) {
            val converted = convert(block.node, references, footnotes)
            block.block = converted.block
            block.footnotes = converted.footnotes
            block.trace = converted.trace
            committedVersion++
        }
    }

    /**
     * A throwaway parse of the open end of the document, layered over the committed definitions so
     * that its own definitions stay provisional.
     */
    private inner class TailParse(
        text: String,
    ) {
        private val references = LinkReferenceMap(base = live.references)
        private val footnoteRegistry = live.footnotes?.let { FootnoteRegistry(base = it) }
        private val parser = BlockParser(references, extensions, footnoteRegistry)
        private val streamingLeaf: BlockNode?

        val blocks: List<Block>
        val footnotes: Map<String, ConvertedFootnote>

        init {
            forEachLine(text) { parser.addLine(it) }
            streamingLeaf = if (optimistic) parser.openTextBlock else null
            parser.finish()
            val builder = astBuilder(references, footnoteRegistry, streamingLeaf, trace = null)
            blocks = parser.document.children.mapNotNull(builder::topLevelBlock)
            footnotes = builder.collectedFootnotes
        }

        /** Whether the tail defines or newly numbers footnotes, so the footnote list must be recomputed. */
        val involvesFootnotes: Boolean get() = footnotes.isNotEmpty() || footnoteRegistry?.hasOwnNumbers == true

        fun numberOfKey(key: String): Int? = footnoteRegistry?.numberOfKey(key)

        /** Whether the text ends inside a paragraph whose last line looks like a table row. */
        fun endsWithTableHeaderCandidate(): Boolean {
            val paragraph = streamingLeaf as? ParagraphNode ?: return false
            if (MarkdownExtension.TABLES !in extensions) return false
            val content = paragraph.content
            val lastLineStart = content.lastIndexOf('\n', content.length - 2) + 1
            return content.substring(lastLineStart).trimStart().startsWith('|')
        }
    }
}
