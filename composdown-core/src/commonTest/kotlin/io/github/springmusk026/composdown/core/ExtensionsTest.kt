package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.AlertType
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.TableAlignment
import io.github.springmusk026.composdown.core.ast.TaskState
import io.github.springmusk026.composdown.core.ast.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionsTest {
    private val gfm = MarkdownParser()
    private val withAlerts = MarkdownParser(MarkdownExtension.GFM + MarkdownExtension.ALERTS)

    @Test
    fun gfmIsTheDefault() {
        assertEquals(MarkdownExtension.GFM, MarkdownParser().extensions)
        assertTrue(MarkdownExtension.ALERTS !in MarkdownExtension.GFM)
    }

    @Test
    fun tableCellsArePaddedAndTruncatedToTheColumnCount() {
        val table = assertIs<Block.Table>(gfm.parse("| a | b |\n|:--|--:|\n| 1 |\n| 1 | 2 | 3 |\n").blocks.single())
        assertEquals(listOf(TableAlignment.LEFT, TableAlignment.RIGHT), table.columns.toList())
        assertEquals(listOf("a", "b"), table.header.cells.map { it.content.plainText() })
        assertEquals(
            listOf(listOf("1", ""), listOf("1", "2")),
            table.rows.map { row ->
                row.cells.map { it.content.plainText() }
            },
        )
    }

    @Test
    fun taskListItemsCarryTheirState() {
        val list =
            assertIs<Block.ListBlock>(
                gfm.parse("- [ ] todo\n- [x] done\n- [X] also done\n- plain\n- [ ]\n").blocks.single(),
            )
        assertEquals(
            listOf(TaskState.UNCHECKED, TaskState.CHECKED, TaskState.CHECKED, null, null),
            list.items.map { it.taskState },
        )
        assertEquals("todo", assertIs<Block.Paragraph>(list.items[0].children.single()).content.plainText())
    }

    @Test
    fun footnotesAreNumberedByFirstReferenceAndUnreferencedOnesOmitted() {
        val document =
            gfm.parse(
                """
                Second[^b], first[^a], second again[^B], missing[^none].

                [^a]: Alpha.
                [^b]: Beta.
                [^unused]: Never referenced.
                """.trimIndent(),
            )
        val references =
            assertIs<Block.Paragraph>(
                document.blocks.single(),
            ).content.filterIsInstance<Inline.FootnoteReference>()
        assertEquals(listOf("b" to 1, "a" to 2, "b" to 1), references.map { it.label to it.number })
        assertEquals(listOf("b", "a"), document.footnotes.map { it.label })
        assertEquals(listOf(1, 2), document.footnotes.map { it.number })
        assertTrue(assertIs<Block.Paragraph>(document.blocks.single()).content.plainText().contains("[^none]"))
    }

    @Test
    fun alertsAreRecognizedCaseInsensitively() {
        val alert = assertIs<Block.Alert>(withAlerts.parse("> [!warning]\n> Be careful.\n").blocks.single())
        assertEquals(AlertType.WARNING, alert.type)
        assertEquals("Be careful.", assertIs<Block.Paragraph>(alert.children.single()).content.plainText())
    }

    @Test
    fun alertWithoutContentYetIsStillAnAlert() {
        val alert = assertIs<Block.Alert>(withAlerts.parse("> [!NOTE]\n").blocks.single())
        assertTrue(alert.children.isEmpty())
    }

    @Test
    fun alertsRequireTheMarkerAloneOnTheFirstLineOfATopLevelQuote() {
        assertIs<Block.BlockQuote>(withAlerts.parse("> [!NOTE] inline text\n").blocks.single())
        assertIs<Block.BlockQuote>(withAlerts.parse("> [!UNKNOWN]\n> text\n").blocks.single())
        val outer = assertIs<Block.BlockQuote>(withAlerts.parse("> > [!NOTE]\n> > nested\n").blocks.single())
        assertIs<Block.BlockQuote>(outer.children.single())
    }

    @Test
    fun alertsAreOptIn() {
        assertIs<Block.BlockQuote>(gfm.parse("> [!NOTE]\n> text\n").blocks.single())
    }

    @Test
    fun eachExtensionCanBeDisabledOnItsOwn() {
        fun parserWithout(extension: MarkdownExtension) = MarkdownParser(MarkdownExtension.GFM - extension)

        assertIs<Block.Paragraph>(parserWithout(MarkdownExtension.TABLES).parse("| a |\n| - |\n").blocks.single())

        val list =
            assertIs<Block.ListBlock>(parserWithout(MarkdownExtension.TASK_LISTS).parse("- [x] a\n").blocks.single())
        assertNull(list.items.single().taskState)

        val strike =
            assertIs<Block.Paragraph>(parserWithout(MarkdownExtension.STRIKETHROUGH).parse("~~a~~").blocks.single())
        assertEquals("~~a~~", strike.content.plainText())
        assertTrue(strike.content.none { it is Inline.Strikethrough })

        val autolinks =
            assertIs<Block.Paragraph>(
                parserWithout(MarkdownExtension.AUTOLINKS).parse("www.example.com a@b.co").blocks.single(),
            )
        assertTrue(autolinks.content.none { it is Inline.Link })

        val footnotes = parserWithout(MarkdownExtension.FOOTNOTES).parse("a[^1]\n\n[^1]: note\n")
        assertTrue(footnotes.footnotes.isEmpty())
        assertTrue(
            footnotes.blocks
                .flatMap {
                    (it as? Block.Paragraph)?.content.orEmpty()
                }.none { it is Inline.FootnoteReference },
        )
    }

    @Test
    fun autolinksDoNotSwallowEmphasisOrSentencePunctuation() {
        val paragraph =
            assertIs<Block.Paragraph>(gfm.parse("See _https://example.com/a_b_, then www.example.com.").blocks.single())
        val emphasis = assertIs<Inline.Emphasis>(paragraph.content[1])
        assertEquals("https://example.com/a_b", assertIs<Inline.Link>(emphasis.children.single()).destination)
        val www = paragraph.content.filterIsInstance<Inline.Link>().single()
        assertEquals("http://www.example.com", www.destination)
        assertEquals(".", assertIs<Inline.Text>(paragraph.content.last()).literal)
    }
}
