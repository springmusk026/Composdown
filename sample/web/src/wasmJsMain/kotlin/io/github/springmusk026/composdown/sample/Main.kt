package io.github.springmusk026.composdown.sample

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import io.github.springmusk026.composdown.sample.web.resources.Res
import io.github.springmusk026.composdown.sample.web.resources.noto_sans_arabic_regular
import io.github.springmusk026.composdown.sample.web.resources.noto_sans_hebrew_regular
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

/**
 * Runs the sample application in the page.
 *
 * Browsers give Compose no system fonts to fall back on, so the fonts for the sample's Hebrew and
 * Arabic text are bundled and preloaded as fallback fonts before the application is shown.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    ComposeViewport {
        val resolver = LocalFontFamilyResolver.current
        val hebrew by preloadFont(Res.font.noto_sans_hebrew_regular)
        val arabic by preloadFont(Res.font.noto_sans_arabic_regular)
        var fontsReady by remember { mutableStateOf(false) }
        LaunchedEffect(hebrew, arabic) {
            val fonts = listOfNotNull(hebrew, arabic)
            if (fonts.size == 2) {
                resolver.preload(FontFamily(fonts))
                fontsReady = true
            }
        }
        if (fontsReady) SampleApp()
    }
}
