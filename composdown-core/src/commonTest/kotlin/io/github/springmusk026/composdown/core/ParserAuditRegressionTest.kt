package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.allInlines
import io.github.springmusk026.composdown.core.ast.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Regression tests for parser defects found by the code audit. */
class ParserAuditRegressionTest {
    private val parser = MarkdownParser()

    private fun timed(
        limit: Duration = TIME_LIMIT,
        block: () -> Unit,
    ) {
        val start = TimeSource.Monotonic.markNow()
        block()
        val elapsed = start.elapsedNow()
        assertTrue(elapsed < limit, "Took $elapsed")
    }

    @Test
    fun definitionFollowedByDashesIsADefinitionAndABreak() {
        val document = parser.parse("[foo]: /url\n---\n\n[foo]\n")
        assertIs<Block.ThematicBreak>(document.blocks[0])
        assertEquals(
            "/url",
            document
                .allInlines()
                .filterIsInstance<Inline.Link>()
                .single()
                .destination,
        )
        parser.parse("[a]: /b\n-")
        parser.parse("> [a]: /b\n> ---\n")
        IncrementalMarkdownParser().apply { append("[foo]: /url\n---\n") }.finish()
    }

    @Test
    fun blankLineInsideAnIndentedCodeBlockMakesTheListLoose() {
        assertFalse(assertIs<Block.ListBlock>(parser.parse("-     code\n\n- b\n").blocks.single()).isTight)
    }

    @Test
    fun setTextWithTheSameTextKeepsTheFinishedDocument() {
        val incremental = IncrementalMarkdownParser()
        incremental.append("**a")
        val finished = incremental.finish()
        assertEquals(finished, incremental.setText("**a"))
        assertTrue(incremental.isFinished)
    }

    @Test
    fun escapedCaretInBracketsIsShownOnce() {
        assertEquals(
            "[^foo]",
            parser
                .parse("[\\^foo]")
                .blocks
                .single()
                .text(),
        )
        assertEquals(
            "[^foo]",
            parser
                .parse("[&#94;foo]")
                .blocks
                .single()
                .text(),
        )
    }

    @Test
    fun emailAutolinkDoesNotIncludeTextBeforeItsProtocol() {
        val link =
            parser
                .parse("x.mailto:a@b.com")
                .allInlines()
                .filterIsInstance<Inline.Link>()
                .single()
        assertEquals("mailto:a@b.com", link.destination)
    }

    @Test
    fun manyLinkReferenceDefinitionsParseInLinearTime() =
        timed(limit = 1.seconds) { parser.parse((0 until N).joinToString("\n") { "[r$it]: /u$it" }) }

    @Test
    fun longRunsOfQuoteMarkersStreamInLinearTime() = timed { IncrementalMarkdownParser().append(">".repeat(N * 2)) }

    @Test
    fun nestedFootnoteLikeBracketsParseInLinearTime() = timed { parser.parse("[^".repeat(N) + "]".repeat(N)) }

    @Test
    fun manyDollarPricesParseInLinearTime() =
        timed { MarkdownParser(MarkdownExtension.GFM + MarkdownExtension.MATH).parse("\$5 ".repeat(N)) }

    @Test
    fun autolinkWithManyClosingParenthesesParsesInLinearTime() = timed { parser.parse("www.a.com" + ")".repeat(N * 3)) }

    @Test
    fun manyUnclosedLinkDestinationsParseInLinearTime() = timed { parser.parse("[a](".repeat(N)) }

    @Test
    fun streamingLinkWithParenthesesInItsDestinationIsShownAsALink() {
        val document = IncrementalMarkdownParser().append("See [wiki](https://en.wikipedia.org/wiki/Foo_(bar)")
        val link = document.allInlines().filterIsInstance<Inline.Link>().singleOrNull()
        assertEquals("wiki", link?.children?.plainText())
    }

    @Test
    fun streamingCostDoesNotGrowWithTheNumberOfBlocks() =
        timed(limit = 2.seconds) {
            val incremental = IncrementalMarkdownParser()
            for (i in 0 until N) incremental.append("Paragraph $i with some words.\n\n")
            incremental.finish()
        }

    @Test
    fun streamedFootnotesAreNumberedLikeAOneShotParse() {
        val text =
            buildString {
                for (i in 0 until 200) append("Paragraph $i cites[^${i % 7}] and[^${(i * 3) % 11}].\n\n")
                for (i in 0 until 11) append("[^$i]: Note $i.\n\n")
                append("After[^3][^new].\n\n[^new]: Late.\n")
            }
        val incremental = IncrementalMarkdownParser()
        for (chunk in text.chunked(7)) incremental.append(chunk)
        assertEquals(parser.parse(text), incremental.finish())
    }

    private fun Block.text(): String = assertIs<Block.Paragraph>(this).content.plainText()

    private companion object {
        const val N = 30_000
        val TIME_LIMIT = 5.seconds
    }
}
