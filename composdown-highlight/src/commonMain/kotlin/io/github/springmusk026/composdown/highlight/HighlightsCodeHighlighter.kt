package io.github.springmusk026.composdown.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import io.github.springmusk026.composdown.compose.CodeHighlighter

/** Fully opaque alpha for the RGB colors Highlights reports. */
private const val OPAQUE = 0xFF000000.toInt()

/**
 * A [CodeHighlighter] based on [Highlights](https://github.com/SnipMeDev/Highlights), a pure Kotlin
 * highlighter that runs on every Composdown platform.
 *
 * Supported languages: C, C++, C#, CoffeeScript, Dart, Go, Java, JavaScript, Kotlin, Perl, PHP, Python,
 * Ruby, Rust, shell, Swift and TypeScript, plus common aliases such as `kt`, `js`, `ts`, `py`, `sh` and
 * `bash`. Code in other languages is shown without colors.
 */
public class HighlightsCodeHighlighter : CodeHighlighter {
    override fun highlight(
        code: String,
        language: String,
        darkTheme: Boolean,
    ): AnnotatedString? {
        val syntax = syntaxLanguage(language) ?: return null
        val highlights =
            Highlights
                .Builder()
                .code(code)
                .language(syntax)
                .theme(SyntaxThemes.default(darkTheme))
                .build()
                .getHighlights()
        return buildAnnotatedString {
            append(code)
            for (highlight in highlights) {
                val location = highlight.location
                val start = location.start.coerceIn(0, code.length)
                val end = location.end.coerceIn(start, code.length)
                val style =
                    when (highlight) {
                        is ColorHighlight -> SpanStyle(color = Color(highlight.rgb or OPAQUE))
                        is BoldHighlight -> SpanStyle(fontWeight = FontWeight.Bold)
                    }
                addStyle(style, start, end)
            }
        }
    }
}

/** Names and aliases used in code block info strings, mapped to the languages Highlights supports. */
private val LANGUAGES: Map<String, SyntaxLanguage> =
    buildMap {
        for (language in SyntaxLanguage.entries) {
            if (language != SyntaxLanguage.DEFAULT) put(language.name.lowercase(), language)
        }
        put("kt", SyntaxLanguage.KOTLIN)
        put("kts", SyntaxLanguage.KOTLIN)
        put("js", SyntaxLanguage.JAVASCRIPT)
        put("jsx", SyntaxLanguage.JAVASCRIPT)
        put("ts", SyntaxLanguage.TYPESCRIPT)
        put("tsx", SyntaxLanguage.TYPESCRIPT)
        put("py", SyntaxLanguage.PYTHON)
        put("rb", SyntaxLanguage.RUBY)
        put("rs", SyntaxLanguage.RUST)
        put("cs", SyntaxLanguage.CSHARP)
        put("c#", SyntaxLanguage.CSHARP)
        put("c++", SyntaxLanguage.CPP)
        put("h", SyntaxLanguage.C)
        put("hpp", SyntaxLanguage.CPP)
        put("golang", SyntaxLanguage.GO)
        put("sh", SyntaxLanguage.SHELL)
        put("bash", SyntaxLanguage.SHELL)
        put("zsh", SyntaxLanguage.SHELL)
        put("coffee", SyntaxLanguage.COFFEESCRIPT)
        put("pl", SyntaxLanguage.PERL)
    }

/** The Highlights language for a code block's [language] name, ignoring letter case, or `null`. */
internal fun syntaxLanguage(language: String): SyntaxLanguage? = LANGUAGES[language.lowercase()]
