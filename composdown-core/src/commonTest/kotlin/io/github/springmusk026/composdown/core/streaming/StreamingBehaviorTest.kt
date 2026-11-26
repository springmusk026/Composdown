package io.github.springmusk026.composdown.core.streaming

import io.github.springmusk026.composdown.core.IncrementalMarkdownParser
import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.StreamingPolicy
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.ast.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StreamingBehaviorTest {
    private fun optimistic(text: String): MarkdownDocument = IncrementalMarkdownParser().append(text)

    private fun strict(text: String): MarkdownDocument =
        IncrementalMarkdownParser(streamingPolicy = StreamingPolicy.STRICT).append(text)

    private fun MarkdownDocument.singleParagraph(): List<Inline> = assertIs<Block.Paragraph>(blocks.single()).content

    @Test
    fun unclosedStrongEmphasisIsShownClosed() {
        val content = optimistic("Some **bold te").singleParagraph()
        assertEquals("bold te", assertIs<Inline.Strong>(content.last()).children.plainText())
        assertEquals("Some **bold te", strict("Some **bold te").singleParagraph().plainText())
    }

    @Test
    fun nestedUnclosedEmphasisIsClosedInnermostFirst() {
        val strong = assertIs<Inline.Strong>(optimistic("**bold *both").singleParagraph().single())
        assertIs<Inline.Emphasis>(strong.children.last())
    }

    @Test
    fun unclosedCodeSpanAndStrikethroughAreShownClosed() {
        assertEquals("val x", assertIs<Inline.Code>(optimistic("Use `val x").singleParagraph().last()).literal)
        assertEquals(
            "gone",
            assertIs<Inline.Strikethrough>(optimistic("~~gone").singleParagraph().single()).children.plainText(),
        )
    }

    @Test
    fun linkWithPartialDestinationIsShownAsLink() {
        val link = assertIs<Inline.Link>(optimistic("See [the docs](https://exa").singleParagraph().last())
        assertEquals("the docs", link.children.plainText())
    }

    @Test
    fun trailingDelimiterRunIsHeldBack() {
        assertEquals("Price is 5", optimistic("Price is 5 *").singleParagraph().plainText())
        assertEquals("Price is 5 *", strict("Price is 5 *").singleParagraph().plainText())
    }

    @Test
    fun partialLineThatMayBecomeABlockMarkerIsHeldBack() {
        assertEquals(1, optimistic("Intro.\n\n``").blocks.size)
        assertEquals(1, optimistic("Intro.\n\n1.").blocks.size)
        assertEquals(1, optimistic("Intro.\n\n- ").blocks.size)
        assertEquals(1, optimistic("Intro.\n\n> > ##").blocks.size)
        assertEquals(1, optimistic("Intro.\n\n| :--").blocks.size)
        assertEquals(2, optimistic("Intro.\n\n- a").blocks.size)
        assertEquals(2, optimistic("Intro.\n\n2024 was").blocks.size)
    }

    @Test
    fun tableHeaderIsHeldBackUntilTheDelimiterRowDecides() {
        val parser = IncrementalMarkdownParser()
        assertTrue(parser.append("| a | b |\n").blocks.isEmpty())
        assertTrue(parser.append("| -").blocks.isEmpty())
        assertIs<Block.Table>(parser.append("-- | --- |\n").blocks.single())
        val table = assertIs<Block.Table>(parser.append("| 1 | 2").blocks.single())
        assertEquals(
            listOf("1", "2"),
            table.rows
                .single()
                .cells
                .map { it.content.plainText() },
        )
    }

    @Test
    fun unclosedCodeFenceIsAnOpenCodeBlock() {
        val code = assertIs<Block.CodeBlock>(optimistic("```kotlin\nval x = 1\nval").blocks.single())
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1\nval\n", code.literal)
    }

    @Test
    fun completedBlocksKeepTheirInstances() {
        val parser = IncrementalMarkdownParser()
        val first = parser.append("# Title\n\nFirst paragraph.\n\nSecond").blocks
        val second = parser.append(" paragraph.\n\n- item one\n- item").blocks
        assertSame(first[0], second[0])
        assertSame(first[1], second[1])
        val third = parser.append(" two\n\nDone.").blocks
        for (index in 0..2) assertSame(second[index], third[index])
        val finished = parser.finish().blocks
        for (index in 0..3) assertSame(third[index], finished[index])
    }

    @Test
    fun aLaterDefinitionUpdatesOnlyTheBlocksThatUseIt() {
        val parser = IncrementalMarkdownParser()
        val before = parser.append("Read [the guide].\n\nUnrelated text.\n\n").blocks
        val after = parser.append("[the guide]: /guide\n\nMore.").blocks
        assertEquals("/guide", assertIs<Inline.Link>(assertIs<Block.Paragraph>(after[0]).content[1]).destination)
        assertNotSame(before[0], after[0])
        assertSame(before[1], after[1])
    }

    @Test
    fun definitionsInTheUnfinishedTailDoNotAffectCompletedBlocks() {
        val parser = IncrementalMarkdownParser()
        val document = parser.append("Read [the guide].\n\n[the guide]: /gu")
        assertEquals("Read [the guide].", assertIs<Block.Paragraph>(document.blocks.single()).content.plainText())
        assertEquals(MarkdownParser().parse(parser.text + "ide\n"), parser.append("ide\n").let { parser.finish() })
    }

    @Test
    fun tableSplitOffAParagraphIsReparsedWithThatParagraph() {
        // "2) b" continues the paragraph (an ordered list starting at 2 cannot interrupt one), so it
        // becomes the table header. Read on its own, it would start a list instead.
        val text = "a\n2) b\n|---|\n"
        val parser = IncrementalMarkdownParser(streamingPolicy = StreamingPolicy.STRICT)
        val document = text.map { parser.append(it.toString()) }.last()
        assertEquals(MarkdownParser().parse(text), document)
        assertIs<Block.Table>(document.blocks.last())
    }

    @Test
    fun crlfSplitAcrossChunksIsOneLineBreak() {
        val parser = IncrementalMarkdownParser(streamingPolicy = StreamingPolicy.STRICT)
        parser.append("a\r")
        parser.append("\nb")
        assertEquals(MarkdownParser().parse("a\r\nb"), parser.finish())
    }
}
