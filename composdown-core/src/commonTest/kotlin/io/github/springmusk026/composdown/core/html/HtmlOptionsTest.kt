package io.github.springmusk026.composdown.core.html

import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlOptionsTest {
    private val parser = MarkdownParser(MarkdownExtension.GFM + MarkdownExtension.ALERTS + MarkdownExtension.MATH)

    private fun html(
        markdown: String,
        options: HtmlOptions = HtmlOptions.Default,
    ) = parser.parse(markdown).toHtml(options)

    @Test
    fun rawHtmlIsEscapedByDefault() {
        assertEquals(
            "<pre><code>&lt;script&gt;alert(1)&lt;/script&gt;</code></pre>\n",
            html("<script>alert(1)</script>\n"),
        )
        assertEquals("<p>a &lt;b onclick=&quot;x()&quot;&gt;b&lt;/b&gt;</p>\n", html("a <b onclick=\"x()\">b</b>"))
    }

    @Test
    fun rawHtmlCanBeOmittedOrAllowed() {
        val omit = HtmlOptions(rawHtml = RawHtml.OMIT)
        assertEquals("<p>a b</p>\n", html("a <b>b</b>", omit))
        assertEquals("", html("<div>x</div>\n", omit))
        assertEquals("<p>a <b>b</b></p>\n", html("a <b>b</b>", HtmlOptions(rawHtml = RawHtml.ALLOW)))
    }

    @Test
    fun unsafeLinksAreRemovedByDefault() {
        assertEquals("<p><a href=\"\">x</a></p>\n", html("[x](javascript:alert(1))"))
        assertEquals("<p><a href=\"\">x</a></p>\n", html("[x]( JAVASCRIPT:alert(1))"))
        assertEquals("<p><a href=\"\">x</a></p>\n", html("[x](file:///etc/passwd)"))
        assertEquals("<p><img src=\"\" alt=\"x\" /></p>\n", html("![x](data:text/html;base64,AAAA)"))
        assertEquals(
            "<p><img src=\"data:image/png;base64,AAAA\" alt=\"x\" /></p>\n",
            html("![x](data:image/png;base64,AAAA)"),
        )
        assertEquals("<p><a href=\"https://example.com\">x</a></p>\n", html("[x](https://example.com)"))
        assertEquals(
            "<p><a href=\"javascript:alert(1)\">x</a></p>\n",
            html("[x](javascript:alert(1))", HtmlOptions.CommonMark),
        )
    }

    @Test
    fun headingIdsMatchDocumentAnchors() {
        val options = HtmlOptions(headingIds = true)
        assertEquals(
            "<h1 id=\"intro\">Intro</h1>\n<h2 id=\"intro-1\">Intro</h2>\n" +
                "<blockquote>\n<h3 id=\"c--kotlin\">C++ &amp; Kotlin</h3>\n</blockquote>\n",
            html("# Intro\n## Intro\n> ### C++ & Kotlin\n", options),
        )
    }

    @Test
    fun softBreaksCanBeLineBreaks() {
        assertEquals("<p>a<br />b</p>\n", html("a\nb", HtmlOptions(softBreak = "<br />")))
    }

    @Test
    fun extensionsUseGitHubMarkup() {
        assertEquals(
            "<div class=\"markdown-alert markdown-alert-note\">\n" +
                "<p class=\"markdown-alert-title\">Note</p>\n<p>Hi</p>\n</div>\n",
            html("> [!NOTE]\n> Hi\n"),
        )
        assertEquals("<p><span class=\"math inline\">x^2</span></p>\n", html("\$x^2\$"))
    }
}
