package io.github.springmusk026.composdown.core.streaming

import io.github.springmusk026.composdown.core.IncrementalMarkdownParser
import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.StreamingPolicy
import io.github.springmusk026.composdown.core.spec.CommonMarkSpecExamples
import io.github.springmusk026.composdown.core.spec.GfmSpecExamples
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The defining guarantee of incremental parsing: however the text is split into chunks, the
 * finished document equals a one-shot parse of the same text.
 */
class StreamingEquivalenceTest {
    private val gfm = MarkdownExtension.GFM + MarkdownExtension.ALERTS + MarkdownExtension.MATH
    private val inputs: List<Pair<String, Set<MarkdownExtension>>> =
        CommonMarkSpecExamples.all.map { it.markdown to emptySet<MarkdownExtension>() } +
            CommonMarkSpecExamples.all.map { it.markdown to gfm } +
            GfmSpecExamples.all.map { it.markdown to gfm } +
            StreamingCorpus.documents.map { it to gfm }

    @Test
    fun characterByCharacterStreamingFinishesLikeAOneShotParse() {
        for (policy in StreamingPolicy.entries) {
            for ((markdown, extensions) in inputs) {
                val parser = IncrementalMarkdownParser(extensions, policy)
                for (c in markdown) parser.append(c.toString())
                assertEquals(MarkdownParser(extensions).parse(markdown), parser.finish(), "$policy, input:\n$markdown")
            }
        }
    }

    @Test
    fun randomChunkingFinishesLikeAOneShotParse() {
        val random = Random(SEED)
        for ((markdown, extensions) in inputs) {
            repeat(CHUNKINGS_PER_INPUT) {
                val parser = IncrementalMarkdownParser(extensions)
                var position = 0
                while (position < markdown.length) {
                    val end = minOf(markdown.length, position + 1 + random.nextInt(MAX_CHUNK))
                    parser.append(markdown.substring(position, end))
                    position = end
                }
                assertEquals(MarkdownParser(extensions).parse(markdown), parser.finish(), "input:\n$markdown")
            }
        }
    }

    @Test
    fun strictIntermediateDocumentsEqualOneShotParsesOfThePrefix() {
        // Definitions (`[label]: ...`) are the one case where intermediate results intentionally
        // differ: definitions in the unfinished part do not apply to completed blocks yet.
        for ((markdown, extensions) in inputs.filter { "]:" !in it.first }) {
            val parser = IncrementalMarkdownParser(extensions, StreamingPolicy.STRICT)
            val oneShot = MarkdownParser(extensions)
            for (end in 1..markdown.length) {
                val document = parser.append(markdown.substring(end - 1, end))
                // A `\r` at the end may still become `\r\n`, so it is only compared once settled.
                if (!markdown.substring(0, end).endsWith('\r')) {
                    assertEquals(
                        oneShot.parse(markdown.substring(0, end)),
                        document,
                        "prefix:\n${markdown.substring(0, end)}",
                    )
                }
            }
        }
    }

    @Test
    fun arbitraryEditsWithSetTextFinishLikeAOneShotParse() {
        val random = Random(SEED)
        for ((markdown, extensions) in StreamingCorpus.documents.map { it to gfm } +
            GfmSpecExamples.all.map { it.markdown to gfm }) {
            val parser = IncrementalMarkdownParser(extensions)
            var text = ""
            repeat(EDITS) {
                text = randomEdit(text, markdown, random)
                parser.setText(text)
            }
            assertEquals(MarkdownParser(extensions).parse(text), parser.finish(), "text:\n$text")
        }
    }

    @Test
    fun fuzzedMarkdownLikeTextStreamsLikeAOneShotParse() {
        val random = Random(SEED)
        repeat(FUZZ_DOCUMENTS) {
            val markdown =
                buildString { repeat(random.nextInt(FUZZ_MAX_LENGTH)) { append(FUZZ_PIECES.random(random)) } }
            for (policy in StreamingPolicy.entries) {
                val parser = IncrementalMarkdownParser(gfm, policy)
                val oneShot = MarkdownParser(gfm)
                var position = 0
                while (position < markdown.length) {
                    val end = minOf(markdown.length, position + 1 + random.nextInt(MAX_CHUNK))
                    val document = parser.append(markdown.substring(position, end))
                    position = end
                    val prefix = markdown.substring(0, end)
                    if (policy == StreamingPolicy.STRICT && "]:" !in markdown && !prefix.endsWith('\r')) {
                        assertEquals(oneShot.parse(prefix), document, "prefix:\n$prefix")
                    }
                }
                assertEquals(oneShot.parse(markdown), parser.finish(), "$policy, input:\n$markdown")
            }
        }
    }

    @Test
    fun appendingAfterFinishContinuesTheDocument() {
        val parser = IncrementalMarkdownParser()
        parser.append("# Title\n\nFirst *paragraph")
        parser.finish()
        parser.append("* continues.\n\n- item\n")
        val expected = MarkdownParser().parse("# Title\n\nFirst *paragraph* continues.\n\n- item\n")
        assertEquals(expected, parser.finish())
    }

    /** Appends, inserts or deletes a piece of text, drawing new text from [source]. */
    private fun randomEdit(
        text: String,
        source: String,
        random: Random,
    ): String {
        val position = if (text.isEmpty()) 0 else random.nextInt(text.length + 1)
        val pieceStart = random.nextInt(source.length)
        val piece = source.substring(pieceStart, minOf(source.length, pieceStart + 1 + random.nextInt(MAX_CHUNK)))
        return when (random.nextInt(3)) {
            0 -> text + piece
            1 -> text.substring(0, position) + piece + text.substring(position)
            else -> text.removeRange(position, minOf(text.length, position + random.nextInt(MAX_CHUNK)))
        }
    }

    private companion object {
        const val SEED = 20_261_126
        const val CHUNKINGS_PER_INPUT = 3
        const val MAX_CHUNK = 12
        const val EDITS = 40
        const val FUZZ_DOCUMENTS = 400
        const val FUZZ_MAX_LENGTH = 60

        /** Fragments dense in Markdown syntax, so random documents exercise many constructs. */
        val FUZZ_PIECES =
            listOf(
                "a",
                "b ",
                " ",
                "  ",
                "\n",
                "\n\n",
                "\r\n",
                "\t",
                "*",
                "**",
                "_",
                "~~",
                "`",
                "```",
                "~~~",
                "#",
                "# ",
                "> ",
                "- ",
                "+ ",
                "1. ",
                "2) ",
                "[",
                "]",
                "(",
                ")",
                "[^1]",
                "[a]",
                "]: /u",
                "<",
                ">",
                "<div>",
                "&amp;",
                "\\",
                "|",
                " | ",
                "---",
                "|---|",
                "===",
                "    ",
                "- [ ] ",
                "www.x.co",
                "$",
                "$$",
                "\\(",
                "\\)",
                "\\[",
                "\\]",
                "http://x.co",
                "a@b.co",
                "!",
                "\"t\"",
            )
    }
}
