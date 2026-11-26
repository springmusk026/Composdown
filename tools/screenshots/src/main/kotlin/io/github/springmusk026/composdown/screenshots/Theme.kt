package io.github.springmusk026.composdown.screenshots

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import io.github.springmusk026.composdown.compose.ComposdownDefaults
import io.github.springmusk026.composdown.compose.ComposdownTheme

/** Fonts bundled with the project, so that images do not depend on the machine's fonts. */
private val Inter =
    FontFamily(
        Font("fonts/Inter-Regular.ttf"),
        Font("fonts/Inter-Italic.ttf", style = FontStyle.Italic),
        Font("fonts/Inter-SemiBold.ttf", weight = FontWeight.SemiBold),
        Font("fonts/Inter-Bold.ttf", weight = FontWeight.Bold),
        Font("fonts/Inter-BoldItalic.ttf", weight = FontWeight.Bold, style = FontStyle.Italic),
    )

internal val JetBrainsMono = FontFamily(Font("fonts/JetBrainsMono-Regular.ttf"))

/** Brand colors of the generated images (the logo's gradient). */
internal object Brand {
    val Violet = Color(0xFF7F52FF)
    val Blue = Color(0xFF3D7BF5)
}

/** Other colors of the generated images. */
internal object Palette {
    val Backdrop = Color(0xFFF0EEFB)
    val WindowClose = Color(0xFFFF5F57)
    val WindowMinimize = Color(0xFFFEBC2E)
    val WindowZoom = Color(0xFF28C840)
    val Caption = Color(0xFF3B3355)
    val Teal = Color(0xFF00897B)
    val CodeBackground = Color(0xFF1E1F29)
    val TealSurface = Color(0xFFF1F4F4)
    val CodeHeader = Color(0xFF2A2C3A)
    val CodeLabel = Color(0xFFB8BCD0)
    val CodeAction = Color(0xFF7FDBCA)
    val CodeText = Color(0xFFE4E6F0)
    val WarningBackground = Color(0xFFFFF4E0)
    val WarningText = Color(0xFF8A5300)
}

@Composable
internal fun ImageTheme(
    colors: ColorScheme,
    content: @Composable () -> Unit,
) {
    val base = Typography()

    fun TextStyle.inter() = copy(fontFamily = Inter)
    val typography =
        Typography(
            headlineMedium = base.headlineMedium.inter(),
            headlineSmall = base.headlineSmall.inter(),
            titleLarge = base.titleLarge.inter(),
            titleMedium = base.titleMedium.inter(),
            titleSmall = base.titleSmall.inter(),
            bodyLarge = base.bodyLarge.inter(),
            bodyMedium = base.bodyMedium.inter(),
            bodySmall = base.bodySmall.inter(),
            labelLarge = base.labelLarge.inter(),
            labelMedium = base.labelMedium.inter(),
            labelSmall = base.labelSmall.inter(),
        )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

/** The default Composdown theme, with the bundled monospace font for code and math. */
@Composable
internal fun imageComposdownTheme(): ComposdownTheme {
    val defaults = ComposdownDefaults.typography()
    return ComposdownDefaults.theme(
        typography =
            ComposdownDefaults.typography(
                codeBlock = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                inlineCode = defaults.inlineCode.copy(fontFamily = JetBrainsMono),
                math = defaults.math.copy(fontFamily = JetBrainsMono),
                mathBlock = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono),
            ),
    )
}
