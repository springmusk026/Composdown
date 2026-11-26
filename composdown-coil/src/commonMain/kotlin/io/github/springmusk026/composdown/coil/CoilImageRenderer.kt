package io.github.springmusk026.composdown.coil

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import io.github.springmusk026.composdown.compose.ImageRenderer
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.plainText

/**
 * An [ImageRenderer] that loads images with [Coil](https://coil-kt.github.io/coil/).
 *
 * While an image loads, it reserves a box of [placeholderAspectRatio] across the available width.
 * Once loaded, an image is drawn at its own aspect ratio, no wider than its natural size, and that
 * size is remembered by URL, so the same image does not change size again when it is shown later
 * (for example, after scrolling back to it in a lazy list). Images that fail to load show their
 * description.
 *
 * Markdown from untrusted sources can reference any URL, and loading a remote image reveals the
 * reader's address to its server. Only URLs accepted by [canLoad] are loaded; by default, HTTPS URLs.
 *
 * Use one instance for many documents so they share the size cache.
 *
 * @param imageLoader the Coil image loader, or `null` to use Coil's singleton loader. Remote images
 *   need a Coil network module (such as `coil-network-ktor3`) in the application.
 * @param placeholderAspectRatio width divided by height of the space reserved while loading.
 * @param canLoad whether an image URL may be loaded. Other images show their description.
 */
public class CoilImageRenderer(
    private val imageLoader: ImageLoader? = null,
    private val placeholderAspectRatio: Float = DEFAULT_PLACEHOLDER_ASPECT_RATIO,
    private val canLoad: (String) -> Boolean = { it.startsWith("https://", ignoreCase = true) },
) : ImageRenderer {
    init {
        require(placeholderAspectRatio > 0f) { "placeholderAspectRatio must be positive, was $placeholderAspectRatio" }
    }

    /** Natural sizes, in pixels, of images that loaded, by URL. */
    private val sizes = mutableStateMapOf<String, Size>()

    @Composable
    override fun Image(
        image: Inline.Image,
        modifier: Modifier,
    ) {
        val description = image.children.plainText()
        if (!canLoad(image.destination)) {
            Description(description, modifier)
            return
        }
        val loader = imageLoader ?: SingletonImageLoader.get(LocalPlatformContext.current)
        val painter = rememberAsyncImagePainter(model = image.destination, imageLoader = loader)
        val state by painter.state.collectAsState()
        val size = sizes[image.destination]
        // The width limit comes before fillMaxWidth, so that an image is never wider than its natural
        // size; fillMaxWidth then takes the available width up to that limit.
        val sizeModifier =
            if (size != null) {
                Modifier
                    .widthIn(max = with(LocalDensity.current) { size.width.toDp() })
                    .fillMaxWidth()
                    .aspectRatio(size.width / size.height)
            } else {
                Modifier.fillMaxWidth().aspectRatio(placeholderAspectRatio)
            }
        Box(
            modifier = modifier.then(sizeModifier).semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state) {
                is AsyncImagePainter.State.Success -> {
                    val loaded = current.painter.intrinsicSize
                    if (loaded.isSpecified && loaded.width > 0f && loaded.height > 0f) sizes[image.destination] = loaded
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }

                is AsyncImagePainter.State.Error -> {
                    // The box already carries the description for accessibility services.
                    Description(description, Modifier.clearAndSetSemantics {})
                }

                is AsyncImagePainter.State.Loading, AsyncImagePainter.State.Empty -> {
                    // The reserved box stays empty until the image is available.
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_PLACEHOLDER_ASPECT_RATIO = 16f / 9f
    }
}

@Composable
private fun Description(
    description: String,
    modifier: Modifier = Modifier,
) {
    Text(text = description, modifier = modifier, style = MaterialTheme.typography.bodyMedium)
}
