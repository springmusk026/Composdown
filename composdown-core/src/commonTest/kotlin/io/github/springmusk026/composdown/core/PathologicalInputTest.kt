package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Inputs designed to trigger quadratic (or worse) behavior or unbounded nesting, modeled on the
 * pathological tests of the CommonMark reference implementation. Each must parse quickly and without
 * crashing. The sizes are chosen so that a quadratic algorithm would take far longer than the limit.
 */
class PathologicalInputTest {
    private val parser = MarkdownParser()

    private fun assertParsesQuickly(
        markdown: String,
        check: (MarkdownDocument) -> Unit = {},
    ) {
        val start = TimeSource.Monotonic.markNow()
        val document = parser.parse(markdown)
        val elapsed = start.elapsedNow()
        assertTrue(elapsed < TIME_LIMIT, "Parsing ${markdown.length} characters took $elapsed")
        check(document)
    }

    @Test
    fun nestedBrackets() = assertParsesQuickly("[".repeat(N) + "a" + "]".repeat(N))

    @Test
    fun unclosedLinkOpeners() = assertParsesQuickly("[a".repeat(N))

    @Test
    fun unopenedLinkClosers() = assertParsesQuickly("a]".repeat(N))

    @Test
    fun unclosedImageOpeners() = assertParsesQuickly("![a".repeat(N))

    @Test
    fun emphasisOpenersWithoutClosers() = assertParsesQuickly("_a ".repeat(N) + "*a ".repeat(N))

    @Test
    fun emphasisClosersWithoutOpeners() = assertParsesQuickly("a_ ".repeat(N) + "a* ".repeat(N))

    @Test
    fun mismatchedEmphasisDelimiters() = assertParsesQuickly("*a_ ".repeat(N))

    @Test
    fun emphasisRunsSubjectToRuleOfThree() = assertParsesQuickly("a**b" + "c* ".repeat(N))

    @Test
    fun linkOpenersWithEmphasisClosers() = assertParsesQuickly("[ a_".repeat(N))

    @Test
    fun deeplyNestedEmphasis() =
        assertParsesQuickly("*a **a ".repeat(N) + "b" + " a** a*".repeat(N)) { document ->
            assertTrue(maxInlineDepth(document) <= MAX_DEPTH + 1)
        }

    @Test
    fun backtickRunsOfManyLengths() =
        assertParsesQuickly(buildString { for (length in 1..BACKTICK_RUNS) append("`".repeat(length)).append('a') })

    @Test
    fun unclosedHtmlComments() = assertParsesQuickly("a <!--".repeat(N))

    @Test
    fun unclosedProcessingInstructions() = assertParsesQuickly("a <?".repeat(N))

    @Test
    fun unclosedDeclarations() = assertParsesQuickly("a <!A".repeat(N))

    @Test
    fun unclosedCdataSections() = assertParsesQuickly("a <![CDATA[".repeat(N))

    @Test
    fun unclosedTagsWithAttributes() = assertParsesQuickly("<a b=\"c\" ".repeat(N))

    @Test
    fun manyInvalidEntities() = assertParsesQuickly("&a".repeat(N) + "&#".repeat(N))

    @Test
    fun deeplyNestedBlockQuotes() =
        assertParsesQuickly("> ".repeat(N) + "a\n") { document ->
            assertTrue(maxBlockDepth(document.blocks) <= MAX_DEPTH + 1)
        }

    @Test
    fun deeplyNestedLists() =
        // Lines past the nesting cap are indented as deeply as the cap allows, so each one is matched
        // against the full chain of open lists.
        assertParsesQuickly(
            (0 until LIST_LINES).joinToString("\n") { " ".repeat(2 * minOf(it, MAX_DEPTH)) + "- a" },
        ) { document ->
            assertTrue(maxBlockDepth(document.blocks) <= 2 * MAX_DEPTH + 2)
        }

    @Test
    fun manyLinkReferenceDefinitionsAndReferences() =
        assertParsesQuickly(
            (0 until N / 10).joinToString("\n") { "[ref$it]: /url$it" } + "\n\n" +
                (0 until N / 10).joinToString(" ") { "[ref$it]" },
        )

    private fun maxBlockDepth(blocks: List<Block>): Int {
        var deepest = 0
        var level = blocks.map { it to 1 }
        while (level.isNotEmpty()) {
            deepest = maxOf(deepest, level.maxOf { it.second })
            level =
                level.flatMap { (block, depth) ->
                    when (block) {
                        is Block.BlockQuote -> block.children.map { it to depth + 1 }
                        is Block.ListBlock -> block.items.flatMap { item -> item.children.map { it to depth + 2 } }
                        else -> emptyList()
                    }
                }
        }
        return deepest
    }

    private fun maxInlineDepth(document: MarkdownDocument): Int {
        var deepest = 0
        var level =
            document.blocks
                .filterIsInstance<Block.Paragraph>()
                .flatMap { it.content }
                .map { it to 1 }
        while (level.isNotEmpty()) {
            deepest = maxOf(deepest, level.maxOf { it.second })
            level =
                level.flatMap { (inline, depth) ->
                    when (inline) {
                        is Inline.Emphasis -> inline.children.map { it to depth + 1 }
                        is Inline.Strong -> inline.children.map { it to depth + 1 }
                        is Inline.Link -> inline.children.map { it to depth + 1 }
                        is Inline.Image -> inline.children.map { it to depth + 1 }
                        else -> emptyList()
                    }
                }
        }
        return deepest
    }

    private companion object {
        const val N = 30_000
        const val BACKTICK_RUNS = 1_000
        const val LIST_LINES = 3_000
        const val MAX_DEPTH = 100
        val TIME_LIMIT = 10.seconds
    }
}
