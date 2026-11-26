package io.github.springmusk026.composdown.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import io.github.springmusk026.composdown.compose.BlockComponent
import io.github.springmusk026.composdown.compose.Composdown
import io.github.springmusk026.composdown.compose.ComposdownBlock
import io.github.springmusk026.composdown.compose.ComposdownComponents
import io.github.springmusk026.composdown.compose.ComposdownDefaults
import io.github.springmusk026.composdown.core.IncrementalMarkdownParser
import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.highlight.HighlightsCodeHighlighter
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.io.File
import kotlin.math.ceil

private const val SCALE = 2f
private const val ANIMATION_SCALE = 1.5f
private const val MEASURE_HEIGHT_DP = 4000
private const val TOKENS_PER_FRAME = 3
private val TOKEN = Regex("""\s+|\w+|[^\w\s]""")
private val Extensions = ComposdownDefaults.Extensions + MarkdownExtension.MATH
private val Highlighter = HighlightsCodeHighlighter()

/**
 * Writes the documentation images to the directory given as the first argument, and the frames of
 * the streaming animation to the directory given as the second.
 */
fun main(args: Array<String>) {
    val images = File(args[0]).apply { mkdirs() }
    val frames = File(args[1]).apply { deleteRecursively() }.apply { mkdirs() }

    renderLogo(File(images, "logo.svg"), File(images, "logo.png"), sizePx = 512)
    render(File(images, "overview.png"), widthDp = 1000) { Overview() }
    render(File(images, "customization.png"), widthDp = 1000) { Customization() }
    render(File(images, "rtl.png"), widthDp = 560) { RightToLeft() }
    renderStreaming(frames)
}

@Composable
private fun Overview() {
    Backdrop {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            for ((title, colors) in listOf("Light" to lightColorScheme(), "Dark" to darkColorScheme())) {
                ImageTheme(colors) {
                    AppWindow(title = "Release notes · $title", width = 444.dp) {
                        Composdown(
                            markdown = Content.overview,
                            extensions = Extensions,
                            theme = imageComposdownTheme(),
                            codeHighlighter = Highlighter,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Customization() {
    Backdrop {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            ImageTheme(lightColorScheme()) {
                Column {
                    Caption("Default rendering")
                    AppWindow(title = "Runbook", width = 444.dp) {
                        Composdown(
                            markdown = Content.customization,
                            theme = imageComposdownTheme(),
                            codeHighlighter = Highlighter,
                        )
                    }
                }
            }
            ImageTheme(BrandColors) {
                Column {
                    Caption("Custom theme and components")
                    AppWindow(title = "Runbook", width = 444.dp) {
                        val base = imageComposdownTheme()
                        Composdown(
                            markdown = Content.customization,
                            theme =
                                ComposdownDefaults.theme(
                                    colors = ComposdownDefaults.colors(link = Palette.Teal),
                                    typography = base.typography,
                                    spacing = ComposdownDefaults.spacing(block = 14.dp),
                                ),
                            components = CustomComponents,
                            codeHighlighter = Highlighter,
                        )
                    }
                }
            }
        }
    }
}

private val BrandColors =
    lightColorScheme(
        primary = Palette.Teal,
        surfaceContainerHighest = Palette.CodeBackground,
        surfaceContainer = Palette.TealSurface,
    )

/** A code block with a header bar, and alerts as tinted cards: examples of custom components. */
private val CustomComponents =
    ComposdownComponents(
        codeBlock =
            BlockComponent { block, modifier ->
                val shape = RoundedCornerShape(12.dp)
                Column(modifier.fillMaxWidth().clip(shape).background(Palette.CodeBackground)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Palette.CodeHeader,
                            ).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            block.language.orEmpty(),
                            color = Palette.CodeLabel,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text("Copy", color = Palette.CodeAction, style = MaterialTheme.typography.labelMedium)
                    }
                    val code =
                        Highlighter.highlight(
                            block.literal.trimEnd(),
                            block.language.orEmpty(),
                            darkTheme = true,
                        )
                    Text(
                        text =
                            code ?: androidx.compose.ui.text
                                .AnnotatedString(block.literal.trimEnd()),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                        color = Palette.CodeText,
                    )
                }
            },
        alert =
            BlockComponent { alert, modifier ->
                Column(
                    modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.WarningBackground)
                        .padding(14.dp),
                ) {
                    Text(
                        text =
                            "⚠  " +
                                alert.type.name
                                    .lowercase()
                                    .replaceFirstChar(Char::uppercaseChar),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Palette.WarningText,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    alert.children.forEach { ComposdownBlock(it) }
                }
            },
    )

@Composable
private fun RightToLeft() {
    Backdrop {
        ImageTheme(lightColorScheme()) {
            AppWindow(title = "Mixed directions", width = 480.dp) {
                Composdown(markdown = Content.rightToLeft, theme = imageComposdownTheme())
            }
        }
    }
}

@Composable
private fun Chat(
    document: MarkdownDocument,
    contentHeight: Dp,
) {
    Backdrop(flat = true) {
        ImageTheme(lightColorScheme()) {
            AppWindow(title = "Assistant", width = 440.dp, contentHeight = contentHeight, elevation = 0.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    UserBubble(Content.CHAT_PROMPT)
                    Composdown(
                        document = document,
                        theme = imageComposdownTheme(),
                        codeHighlighter = Highlighter,
                    )
                }
            }
        }
    }
}

/** Renders the reply token by token, as a language model would stream it, one frame per few tokens. */
private fun renderStreaming(directory: File) {
    val parser = IncrementalMarkdownParser(Extensions)
    val documents = mutableListOf(parser.document)
    TOKEN.findAll(Content.chatReply).map { it.value }.chunked(TOKENS_PER_FRAME).forEach { tokens ->
        tokens.forEach(parser::append)
        documents += parser.document
    }
    documents += parser.finish()

    // Every frame has the size of the last one, so the window does not grow while text arrives.
    val (widthPx, heightPx) =
        measure(widthDp = 520, scale = ANIMATION_SCALE) {
            Chat(documents.last(), Dp.Unspecified)
        }
    val bodyHeight = (heightPx / ANIMATION_SCALE).dp - 80.dp - 40.dp
    documents.forEachIndexed { index, document ->
        val file = File(directory, "frame_%03d.png".format(index))
        renderScene(file, widthPx, heightPx, ANIMATION_SCALE) { Chat(document, bodyHeight) }
    }
    println("Wrote ${documents.size} frames to $directory (window body ${bodyHeight.value} dp)")
}

/** Renders [content] at [widthDp] and its natural height. */
private fun render(
    file: File,
    widthDp: Int,
    scale: Float = SCALE,
    content: @Composable () -> Unit,
) {
    val (widthPx, heightPx) = measure(widthDp, scale, content)
    renderScene(file, widthPx, heightPx, scale, content)
    println("Wrote $file ($widthPx x $heightPx)")
}

/** The size in pixels of [content] laid out at [widthDp]. */
private fun measure(
    widthDp: Int,
    scale: Float,
    content: @Composable () -> Unit,
): Pair<Int, Int> {
    var height = 0
    val widthPx = ceil(widthDp * scale).toInt()
    ImageComposeScene(widthPx, ceil(MEASURE_HEIGHT_DP * scale).toInt(), Density(scale)) {
        Box(Modifier.wrapContentHeight().onSizeChanged { height = it.height }) { content() }
    }.use { it.render() }
    return widthPx to height
}

private fun renderScene(
    file: File,
    widthPx: Int,
    heightPx: Int,
    scale: Float,
    content: @Composable () -> Unit,
) {
    ImageComposeScene(widthPx, heightPx, Density(scale), content = content).use { scene ->
        val image = scene.render()
        file.writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
    }
}

/** Rasterizes the SVG logo, for places that do not accept SVG (such as social previews). */
private fun renderLogo(
    svg: File,
    png: File,
    sizePx: Int,
) {
    val dom = SVGDOM(Data.makeFromBytes(svg.readBytes()))
    val surface = Surface.makeRasterN32Premul(sizePx, sizePx)
    val scale = sizePx / LOGO_SIZE
    surface.canvas.scale(scale, scale)
    dom.setContainerSize(LOGO_SIZE, LOGO_SIZE)
    dom.render(surface.canvas)
    png.writeBytes(checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)).bytes)
}

private const val LOGO_SIZE = 128f
