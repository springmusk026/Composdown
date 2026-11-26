package io.github.springmusk026.composdown.compose.internal

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.springmusk026.composdown.compose.CodeHighlighter
import io.github.springmusk026.composdown.compose.ComposdownComponents
import io.github.springmusk026.composdown.compose.ComposdownTheme
import io.github.springmusk026.composdown.compose.ImageRenderer

/** Everything that rendering depends on besides the document itself. */
@Immutable
@Poko
internal class ComposdownEnvironment(
    val theme: ComposdownTheme,
    val components: ComposdownComponents,
    val imageRenderer: ImageRenderer?,
    val codeHighlighter: CodeHighlighter?,
    /** Called with a link's destination when it is clicked; `null` opens safe links with the platform. */
    val onLinkClick: ((String) -> Unit)?,
)

/** Provided by the `Composdown` entry points; `null` outside of them. */
internal val LocalComposdownEnvironment = staticCompositionLocalOf<ComposdownEnvironment?> { null }
