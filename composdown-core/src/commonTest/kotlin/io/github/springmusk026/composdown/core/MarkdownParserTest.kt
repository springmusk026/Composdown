package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.ListKind
import io.github.springmusk026.composdown.core.ast.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownParserTest {
    private val parser = MarkdownParser()

    @Test
    fun emptyInputProducesEmptyDocument() {
        assertTrue(parser.parse("").blocks.isEmpty())
        assertTrue(parser.parse("\n\n   \n").blocks.isEmpty())
    }

    @Test
    fun headingKeepsLevelAndContent() {
        val heading = assertIs<Block.Heading>(parser.parse("### Title *here*").blocks.single())
        assertEquals(3, heading.level)
        assertEquals("Title here", heading.content.plainText())
    }

    @Test
    fun adjacentTextIsMergedIntoOneElement() {
        // "*" without a closer and the backslash escape both produce separate text internally.
        val paragraph = assertIs<Block.Paragraph>(parser.parse("a * b \\* c [d").blocks.single())
        assertEquals(listOf<Inline>(textOf("a * b * c [d")), paragraph.content.toList())
    }

    @Test
    fun listKindTightnessAndItems() {
        val bullet = assertIs<Block.ListBlock>(parser.parse("- a\n- b\n").blocks.single())
        assertEquals('-', assertIs<ListKind.Bullet>(bullet.kind).marker)
        assertTrue(bullet.isTight)
        assertEquals(2, bullet.items.size)

        val ordered = assertIs<Block.ListBlock>(parser.parse("7) a\n\n8) b\n").blocks.single())
        val kind = assertIs<ListKind.Ordered>(ordered.kind)
        assertEquals(7, kind.startNumber)
        assertEquals(')', kind.delimiter)
        assertTrue(!ordered.isTight)
    }

    @Test
    fun codeBlockInfoAndLanguage() {
        val code = assertIs<Block.CodeBlock>(parser.parse("```kotlin title=\"a.kt\"\nval x = 1\n```").blocks.single())
        assertEquals("kotlin title=\"a.kt\"", code.info)
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1\n", code.literal)
        assertTrue(code.isFenced)

        val indented = assertIs<Block.CodeBlock>(parser.parse("    code\n").blocks.single())
        assertNull(indented.info)
        assertNull(indented.language)
        assertTrue(!indented.isFenced)
    }

    @Test
    fun linkWithoutTitleHasNullTitle() {
        val paragraph = assertIs<Block.Paragraph>(parser.parse("[a](/b) [c](/d \"T\")").blocks.single())
        val links = paragraph.content.filterIsInstance<Inline.Link>()
        assertNull(links[0].title)
        assertEquals("/b", links[0].destination)
        assertEquals("T", links[1].title)
    }

    @Test
    fun linkDestinationIsUnescapedButNotPercentEncoded() {
        val paragraph = assertIs<Block.Paragraph>(parser.parse("[a](/f&ouml;\\*o ä)").blocks.single())
        // A space ends a bare destination, so this is not a link; check the bracketed form instead.
        assertTrue(paragraph.content.none { it is Inline.Link })
        val link =
            parser
                .parse("[a](</f&ouml;\\*o ä>)")
                .blocks
                .single()
                .let { assertIs<Block.Paragraph>(it) }
        assertEquals("/fö*o ä", assertIs<Inline.Link>(link.content.single()).destination)
    }

    @Test
    fun imagePlainTextIsItsDescription() {
        val paragraph = assertIs<Block.Paragraph>(parser.parse("![a *b* `c`](/i.png)").blocks.single())
        assertEquals("a b c", assertIs<Inline.Image>(paragraph.content.single()).children.plainText())
    }

    @Test
    fun allLineEndingStylesAreEquivalent() {
        val unix = parser.parse("# a\nb\nc\n\n- d\n")
        assertEquals(unix, parser.parse("# a\r\nb\r\nc\r\n\r\n- d\r\n"))
        assertEquals(unix, parser.parse("# a\rb\rc\r\r- d\r"))
    }

    @Test
    fun nulCharactersAreReplaced() {
        val paragraph = assertIs<Block.Paragraph>(parser.parse("a\u0000b").blocks.single())
        assertEquals("a�b", paragraph.content.plainText())
    }

    @Test
    fun equalInputsProduceEqualDocuments() {
        val markdown = "# Title\n\n> quote with [link](/x)\n\n1. one\n2. two\n\n```\ncode\n```\n"
        val first = parser.parse(markdown)
        val second = parser.parse(markdown)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun linkReferenceDefinitionsAreNotBlocks() {
        val document = parser.parse("[a]: /url\n\n[a]\n")
        val paragraph = assertIs<Block.Paragraph>(document.blocks.single())
        assertEquals("/url", assertIs<Inline.Link>(paragraph.content.single()).destination)
    }

    private fun textOf(literal: String): Inline =
        assertIs<Block.Paragraph>(
            parser.parse(literal.replace("*", "\\*").replace("[", "\\[")).blocks.single(),
        ).content.single()
}
