package io.github.springmusk026.composdown.highlight

import dev.snipme.highlights.model.SyntaxLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HighlightsCodeHighlighterTest {
    private val highlighter = HighlightsCodeHighlighter()

    @Test
    fun highlightsKnownLanguagesWithoutChangingTheText() {
        val code = "fun main() {\n    val x = \"text\" // comment\n}"
        val highlighted = assertNotNull(highlighter.highlight(code, "kotlin", darkTheme = false))
        assertEquals(code, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun aliasesAndLetterCaseResolve() {
        assertEquals(SyntaxLanguage.KOTLIN, syntaxLanguage("kt"))
        assertEquals(SyntaxLanguage.PYTHON, syntaxLanguage("Python"))
        assertEquals(SyntaxLanguage.SHELL, syntaxLanguage("bash"))
    }

    @Test
    fun unknownLanguagesAreNotHighlighted() {
        assertNull(highlighter.highlight("SELECT 1", "sql", darkTheme = true))
    }

    @Test
    fun partialCodeWhileStreamingIsHighlightedSafely() {
        for (end in 0.."fun f() = \"unterminated".length) {
            val code = "fun f() = \"unterminated".substring(0, end)
            assertEquals(code, highlighter.highlight(code, "kotlin", darkTheme = true)?.text ?: code)
        }
    }
}
