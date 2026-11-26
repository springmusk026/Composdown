package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MathTest {
    private val parser = MarkdownParser(MarkdownExtension.GFM + MarkdownExtension.MATH)

    private fun inlines(markdown: String) = assertIs<Block.Paragraph>(parser.parse(markdown).blocks.single()).content

    private fun math(markdown: String) = inlines(markdown).filterIsInstance<Inline.Math>()

    @Test
    fun dollarMath() {
        assertEquals(listOf("x^2" to false), math("Area is \$x^2\$ here").map { it.literal to it.isDisplay })
        assertEquals(
            listOf("\\sum_i a_i" to true),
            math("Sum: \$\$\\sum_i a_i\$\$ done").map {
                it.literal to
                    it.isDisplay
            },
        )
    }

    @Test
    fun bracketMath() {
        assertEquals(
            listOf("a+b" to false, "c" to true),
            math("Inline \\(a+b\\) and \\[c\\]").map {
                it.literal to
                    it.isDisplay
            },
        )
    }

    @Test
    fun pricesAreNotMath() {
        assertTrue(math("It costs \$5 and \$10 today.").isEmpty())
        assertTrue(math("Between \$ 5 and 6 \$").isEmpty())
        assertTrue(math("\$a\$1").isEmpty())
        assertEquals("It costs \$5 and \$10 today.", inlines("It costs \$5 and \$10 today.").plainText())
    }

    @Test
    fun escapedDollarIsText() {
        assertTrue(math("\\\$a\$").isEmpty())
    }

    @Test
    fun codeSpansWinOverMath() {
        assertTrue(math("`\$x\$`").isEmpty())
    }

    @Test
    fun mathBlocks() {
        val blocks =
            parser
                .parse(
                    "\$\$\nE = mc^2\n\$\$\n\n\\[\na^2 + b^2\n\\]\n\n\$\$x\$\$\n\n```math\n\\pi\n```\n",
                ).blocks
        assertEquals(listOf("E = mc^2", "a^2 + b^2", "x", "\\pi"), blocks.map { assertIs<Block.MathBlock>(it).literal })
    }

    @Test
    fun paragraphStartingWithDisplayMathIsAParagraph() {
        assertIs<Block.Paragraph>(parser.parse("\$\$x\$\$ is display math in text\n").blocks.single())
    }

    @Test
    fun unclosedMathBlockExtendsToTheEnd() {
        assertEquals("a\nb", assertIs<Block.MathBlock>(parser.parse("\$\$\na\nb\n").blocks.single()).literal)
    }

    @Test
    fun mathIsOptIn() {
        val plain = MarkdownParser()
        assertEquals("\$x\$", assertIs<Block.Paragraph>(plain.parse("\$x\$").blocks.single()).content.plainText())
        assertIs<Block.CodeBlock>(plain.parse("```math\nx\n```\n").blocks.single())
    }
}
