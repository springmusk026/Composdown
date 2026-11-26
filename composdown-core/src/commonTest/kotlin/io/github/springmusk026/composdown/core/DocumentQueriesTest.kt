package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.allBlocks
import io.github.springmusk026.composdown.core.ast.allInlines
import io.github.springmusk026.composdown.core.ast.headings
import io.github.springmusk026.composdown.core.ast.plainText
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentQueriesTest {
    private val parser = MarkdownParser()

    @Test
    fun allBlocksVisitsNestedBlocksInDocumentOrderThenFootnotes() {
        val document = parser.parse("> quote\n>\n> - item\n\nafter[^1]\n\n[^1]: note\n")
        val kinds =
            document.allBlocks().map { it::class.simpleName to (it as? Block.Paragraph)?.content?.plainText() }.toList()
        assertEquals(
            listOf(
                "BlockQuote" to null,
                "Paragraph" to "quote",
                "ListBlock" to null,
                "Paragraph" to "item",
                "Paragraph" to "after1",
                "Paragraph" to "note",
            ),
            kinds,
        )
    }

    @Test
    fun allInlinesIncludesNestedElementsAndTableCells() {
        val document =
            parser.parse(
                "A [**bold** link](https://a.example)\n\n| [b](https://b.example) |\n| - |\n| ![c](c.png) |\n",
            )
        assertEquals(
            listOf("https://a.example", "https://b.example"),
            document
                .allInlines()
                .filterIsInstance<Inline.Link>()
                .map { it.destination }
                .toList(),
        )
        assertEquals(1, document.allInlines().filterIsInstance<Inline.Strong>().count())
        assertEquals(
            listOf("c.png"),
            document
                .allInlines()
                .filterIsInstance<Inline.Image>()
                .map { it.destination }
                .toList(),
        )
    }

    @Test
    fun headingsHaveGitHubAnchors() {
        val document =
            parser.parse(
                """
                # Getting *started*
                ## Getting started
                ## Getting started-1
                ### C++ & Kotlin: API v2.0!
                ## Ünïcödé Überschrift
                - ## In a list
                ## 🎉 Emoji
                Setext
                ------
                """.trimIndent(),
            )
        assertEquals(
            listOf(
                "getting-started",
                "getting-started-1",
                "getting-started-1-1",
                "c--kotlin-api-v20",
                "ünïcödé-überschrift",
                "in-a-list",
                "-emoji",
                "setext",
            ),
            document.headings().map { it.anchor },
        )
        assertEquals(listOf(1, 2, 2, 3, 2, 2, 2, 2), document.headings().map { it.level })
        assertEquals("Getting started", document.headings().first().text)
    }

    @Test
    fun plainTextSeparatesBlocksAndKeepsStructureReadable() {
        val document =
            parser.parse(
                """
                # Title

                Some *emphasis* and `code`.

                - one
                - two

                | a | b |
                | - | - |
                | 1 | 2 |

                ---

                ```
                code block
                ```

                > quoted[^n]

                [^n]: The note.
                """.trimIndent(),
            )
        assertEquals(
            "Title\n\nSome emphasis and code.\n\none\ntwo\n\na\tb\n1\t2\n\ncode block\n\nquoted1\n\nThe note.",
            document.plainText(),
        )
    }
}
