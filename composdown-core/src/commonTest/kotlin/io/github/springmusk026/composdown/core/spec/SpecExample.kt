package io.github.springmusk026.composdown.core.spec

import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.html.HtmlOptions
import io.github.springmusk026.composdown.core.html.toHtml
import kotlin.test.assertEquals

/** One example from a specification test suite: Markdown input and the expected HTML output. */
internal class SpecExample(
    val number: Int,
    val section: String,
    val markdown: String,
    val html: String,
)

/** Expected output of examples that only check that the input is handled without errors. */
private const val IGNORE_OUTPUT = "<IGNORE>\n"

private val commonMarkParser = MarkdownParser(extensions = emptySet())
private val gfmParser = MarkdownParser(extensions = MarkdownExtension.GFM)

/** Asserts that a CommonMark example renders to exactly the expected HTML with no extensions enabled. */
internal fun assertCommonMarkExample(example: SpecExample) = assertExample(commonMarkParser, example)

/** Asserts that a GitHub Flavored Markdown example renders to exactly the expected HTML. */
internal fun assertGfmExample(example: SpecExample) = assertExample(gfmParser, example)

private fun assertExample(
    parser: MarkdownParser,
    example: SpecExample,
) {
    val actual = parser.parse(example.markdown).toHtml(HtmlOptions.CommonMark)
    if (example.html == IGNORE_OUTPUT) return
    assertEquals(
        expected = example.html,
        actual = actual,
        message = "Example ${example.number} (${example.section})\n--- Markdown ---\n${example.markdown}",
    )
}
