package io.github.springmusk026.composdown.core

import io.github.springmusk026.composdown.core.ast.headings
import io.github.springmusk026.composdown.core.ast.plainText
import io.github.springmusk026.composdown.core.html.HtmlOptions
import io.github.springmusk026.composdown.core.html.toHtml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regression tests for defects found by the code audit. */
class AuditRegressionTest {
    private val parser = MarkdownParser()

    @Test
    fun headingIdsWorkForHeadingsInFootnotes() {
        val document = parser.parse("Text[^1]\n\n# Body\n\n[^1]: # Note heading\n")
        val html = document.toHtml(HtmlOptions(headingIds = true))
        assertTrue("<h1 id=\"body\">Body</h1>" in html, html)
        assertTrue("<h1 id=\"note-heading\">Note heading</h1>" in html, html)
        assertEquals(listOf("body", "note-heading"), document.headings().map { it.anchor })
    }

    @Test
    fun imagesInHeadingsDoNotContributeToAnchors() {
        assertEquals(
            "-title",
            parser
                .parse("# ![logo](x.png) Title")
                .headings()
                .single()
                .anchor,
        )
    }

    @Test
    fun anchorsKeepLettersOutsideTheBasicMultilingualPlane() {
        assertEquals(listOf("𠀀-test", "𝐀bc"), parser.parse("# 𠀀 test\n# 𝐀bc").headings().map { it.anchor })
    }

    @Test
    fun anchorsDropEmoji() {
        assertEquals(
            "-party",
            parser
                .parse("# 🎉 party")
                .headings()
                .single()
                .anchor,
        )
    }

    @Test
    fun headingIdsCanBePrefixed() {
        val html = parser.parse("# config\n").toHtml(HtmlOptions(headingIds = true, headingIdPrefix = "user-content-"))
        assertEquals("<h1 id=\"user-content-config\">config</h1>\n", html)
    }

    @Test
    fun plainTextSkipsEmptyListItems() {
        assertEquals("a", parser.parse("-\n-\n- a").plainText())
    }

    @Test
    fun footnoteReferencesInImageDescriptionsGetNoBackReference() {
        val html = parser.parse("![x[^1]](a.png) and[^1]\n\n[^1]: n\n").toHtml(HtmlOptions.CommonMark)
        assertFalse("fnref-1-2" in html, html)
    }
}
