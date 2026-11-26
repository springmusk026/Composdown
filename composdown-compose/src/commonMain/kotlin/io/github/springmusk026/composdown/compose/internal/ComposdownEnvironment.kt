package io.github.springmusk026.composdown.compose.internal

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.springmusk026.composdown.compose.CodeHighlighter
import io.github.springmusk026.composdown.compose.ComposdownColors
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

/** This environment with [color] as the color of body text, for example inside block quotes. */
internal fun ComposdownEnvironment.withTextColor(color: Color): ComposdownEnvironment {
    val colors = theme.colors
    if (colors.text == color) return this
    val newColors =
        ComposdownColors(
            color,
            colors.link,
            colors.code,
            colors.codeBlockBackground,
            colors.inlineCodeBackground,
            colors.blockQuoteBar,
            colors.blockQuoteText,
            colors.divider,
            colors.tableHeaderBackground,
            colors.listMarker,
            colors.alerts,
        )
    return ComposdownEnvironment(
        ComposdownTheme(newColors, theme.typography, theme.spacing, theme.strings),
        components,
        imageRenderer,
        codeHighlighter,
        onLinkClick,
    )
}

/** Provided by the `Composdown` entry points; `null` outside of them. */
internal val LocalComposdownEnvironment = staticCompositionLocalOf<ComposdownEnvironment?> { null }
