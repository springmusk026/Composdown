package io.github.springmusk026.composdown.coil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import io.github.springmusk026.composdown.compose.Composdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalTestApi::class, ExperimentalCoilApi::class)
class CoilImageRendererTest {
    private val imageLoader =
        ImageLoader
            .Builder(PlatformContext.INSTANCE)
            .components {
                add(
                    FakeImageLoaderEngine
                        .Builder()
                        .intercept(SMALL_IMAGE, ColorImage(Color.Red.toArgb(), width = 40, height = 20))
                        .build(),
                )
            }.build()

    @Test
    fun loadedImagesAreNoWiderThanTheirNaturalSize() =
        runComposeUiTest {
            setContent {
                Box(Modifier.width(400.dp)) {
                    Composdown("![badge]($SMALL_IMAGE)", imageRenderer = CoilImageRenderer(imageLoader))
                }
            }
            // The fake engine answers immediately; let the image load and the size be remembered.
            waitUntil(timeoutMillis = 5_000) {
                val bounds = onNodeWithContentDescription("badge").getUnclippedBoundsInRoot()
                bounds.bottom - bounds.top < 100.dp
            }
            val bounds = onNodeWithContentDescription("badge").getUnclippedBoundsInRoot()
            assertEquals((40 / density.density).dp, bounds.right - bounds.left)
            assertEquals((20 / density.density).dp, bounds.bottom - bounds.top)
        }

    @Test
    fun placeholderAspectRatioMustBePositive() {
        assertFailsWith<IllegalArgumentException> { CoilImageRenderer(placeholderAspectRatio = 0f) }
    }

    private companion object {
        const val SMALL_IMAGE = "https://example.com/badge.png"
    }
}
