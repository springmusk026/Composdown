package io.github.springmusk026.composdown.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import io.github.springmusk026.composdown.compose.ComposdownDefaults
import io.github.springmusk026.composdown.compose.ComposdownTheme
import io.github.springmusk026.composdown.sample.resources.Res
import io.github.springmusk026.composdown.sample.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

/**
 * The default theme with JetBrains Mono, bundled as a resource, for code and math. Browsers have no
 * system monospace font that Compose can use, so code would otherwise not be monospaced on the web;
 * bundling the font also makes code look the same on every platform.
 */
@Composable
internal fun sampleTheme(): ComposdownTheme {
    val monospace = FontFamily(Font(Res.font.jetbrains_mono_regular))
    val defaults = ComposdownDefaults.typography()
    return ComposdownDefaults.theme(
        typography =
            ComposdownDefaults.typography(
                codeBlock = MaterialTheme.typography.bodyMedium.copy(fontFamily = monospace),
                inlineCode = defaults.inlineCode.copy(fontFamily = monospace),
                math = defaults.math.copy(fontFamily = monospace),
                mathBlock = MaterialTheme.typography.bodyLarge.copy(fontFamily = monospace),
            ),
    )
}
