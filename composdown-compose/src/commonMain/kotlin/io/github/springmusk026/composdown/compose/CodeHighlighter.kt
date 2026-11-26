package io.github.springmusk026.composdown.compose

import androidx.compose.ui.text.AnnotatedString

/**
 * Adds syntax colors to the code of code blocks. Composdown has no highlighter of its own, so that the
 * core library has no dependency on one; the `composdown-highlight` module provides an implementation.
 *
 * Highlighting runs during composition whenever a code block's text changes, including while it is
 * streamed, so implementations should be fast.
 */
public fun interface CodeHighlighter {
    /**
     * Returns [code] with syntax colors for [language] (the first word of the code block's info
     * string, such as `kotlin`), or `null` if the language is not supported. [darkTheme] tells whether
     * the code background is dark, so that colors can be chosen for contrast.
     */
    public fun highlight(
        code: String,
        language: String,
        darkTheme: Boolean,
    ): AnnotatedString?
}
