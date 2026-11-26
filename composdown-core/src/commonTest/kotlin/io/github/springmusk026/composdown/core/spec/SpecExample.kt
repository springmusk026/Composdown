package io.github.springmusk026.composdown.core.spec

import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.html.HtmlRenderer
import kotlin.test.assertEquals

/** One example from a specification test suite: Markdown input and the expected HTML output. */
internal class SpecExample(
    val number: Int,
    val section: String,
    val markdown: String,
    val html: String,
)

/** Parses the example's Markdown and asserts that it renders to exactly the expected HTML. */
internal fun assertSpecExample(example: SpecExample) {
    val actual = HtmlRenderer.render(MarkdownParser().parse(example.markdown))
    assertEquals(
        expected = example.html,
        actual = actual,
        message = "Example ${example.number} (${example.section})\n--- Markdown ---\n${example.markdown}",
    )
}
