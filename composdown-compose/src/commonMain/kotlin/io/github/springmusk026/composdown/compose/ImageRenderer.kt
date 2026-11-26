package io.github.springmusk026.composdown.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.springmusk026.composdown.core.ast.Inline

/**
 * Loads and draws Markdown images. Composdown has no image loading of its own, so that the core
 * library has no dependency on an image loader; the `composdown-coil` module provides an
 * implementation based on Coil.
 *
 * Images that stand alone in a paragraph (`![alt](url)`) are drawn as blocks with this renderer.
 * Images inside text are shown as their description.
 */
public fun interface ImageRenderer {
    /**
     * Draws [image]. Implementations should set the image's content description to
     * `image.children.plainText()` for accessibility, and should reserve a stable size while loading
     * so that content below does not move.
     */
    @Composable
    public fun Image(
        image: Inline.Image,
        modifier: Modifier,
    )
}
