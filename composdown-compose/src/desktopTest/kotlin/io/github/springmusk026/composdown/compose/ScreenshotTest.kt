package io.github.springmusk026.composdown.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/**
 * Visual regression tests: every kind of element, in light and dark themes. Fonts are bundled with
 * the tests so that the images do not depend on the fonts installed on the machine.
 *
 * Record new golden images with `./gradlew :composdown-compose:recordRoborazziDesktop` and check
 * them with `verifyRoborazziDesktop`.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
class ScreenshotTest {
    @Test
    fun showcaseLight() = capture("showcase_light", lightColorScheme())

    @Test
    fun showcaseDark() = capture("showcase_dark", darkColorScheme())

    private fun capture(
        name: String,
        colors: ColorScheme,
    ) = runDesktopComposeUiTest(width = WIDTH_PX, height = HEIGHT_PX) {
        setContent {
            TestTheme(colors) {
                Box(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp).width(480.dp)) {
                    Composdown(
                        Showcase.MARKDOWN,
                        extensions = ComposdownDefaults.Extensions + MarkdownExtension.MATH,
                        theme = testComposdownTheme(),
                    )
                }
            }
        }
        onRoot().captureRoboImage("src/desktopTest/snapshots/$name.png", roborazziOptions = OPTIONS)
    }

    private companion object {
        /** Large enough for the whole showcase, which is 480 dp wide plus padding. */
        const val WIDTH_PX = 512
        const val HEIGHT_PX = 1240

        /** Tolerates tiny anti-aliasing differences between machines. */
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f))
    }
}

private val Inter =
    FontFamily(
        Font("fonts/Inter-Regular.ttf"),
        Font("fonts/Inter-Italic.ttf", style = FontStyle.Italic),
        Font("fonts/Inter-SemiBold.ttf", weight = FontWeight.SemiBold),
        Font("fonts/Inter-Bold.ttf", weight = FontWeight.Bold),
        Font("fonts/Inter-BoldItalic.ttf", weight = FontWeight.Bold, style = FontStyle.Italic),
    )

private val JetBrainsMono = FontFamily(Font("fonts/JetBrainsMono-Regular.ttf"))

@Composable
private fun TestTheme(
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
        )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

@Composable
private fun testComposdownTheme(): ComposdownTheme =
    ComposdownDefaults.theme(
        typography =
            ComposdownDefaults.typography(
                codeBlock = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                inlineCode = ComposdownDefaults.typography().inlineCode.copy(fontFamily = JetBrainsMono),
                math = ComposdownDefaults.typography().math.copy(fontFamily = JetBrainsMono),
                mathBlock = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono),
            ),
    )
