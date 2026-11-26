package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import io.github.springmusk026.composdown.core.ast.ListKind
import io.github.springmusk026.composdown.core.ast.TaskState

/** Nesting depth of the list being rendered: 0 outside lists, 1 for top-level list items, ... */
internal val LocalListDepth = compositionLocalOf { 0 }

/** Line height used to center markers when the text style does not specify one. */
private const val DEFAULT_LINE_HEIGHT = 1.4f

/** Bullet diameter relative to the font size. */
private const val BULLET_SIZE = 0.36f

/** Checkbox side relative to the font size. */
private const val CHECKBOX_SIZE = 0.9f

/** Ordered list markers are text: the item number followed by the delimiter from the source. */
internal fun orderedMarker(
    kind: ListKind.Ordered,
    index: Int,
): String = "${kind.startNumber + index}${kind.delimiter}"

/**
 * The marker before a list item, aligned with the item's first line of [style]: a number for ordered
 * lists, a checkbox for task items, otherwise a bullet whose shape alternates with nesting [depth].
 * Bullets and checkboxes are drawn rather than taken from a font, so they look the same everywhere.
 */
@Composable
internal fun ListMarker(
    kind: ListKind,
    index: Int,
    depth: Int,
    taskState: TaskState?,
    style: TextStyle,
    color: Color,
    accent: Color,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val lineHeight =
        with(density) {
            if (style.lineHeight.isSpecified) {
                style.lineHeight.toDp()
            } else {
                style.fontSize.toDp() *
                    DEFAULT_LINE_HEIGHT
            }
        }
    val fontSize = with(density) { style.fontSize.toDp() }
    when {
        taskState != null -> {
            val checked = taskState == TaskState.CHECKED
            Box(
                modifier =
                    modifier.width(width).height(lineHeight).semantics(mergeDescendants = false) {
                        // The platform announces the state in the user's language.
                        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Checkbox(checked, color, accent, Modifier.size(fontSize * CHECKBOX_SIZE))
            }
        }

        kind is ListKind.Ordered -> {
            Text(
                text = orderedMarker(kind, index),
                modifier = modifier.width(width),
                style = style.merge(color = color, textAlign = TextAlign.End),
            )
        }

        else -> {
            Box(
                modifier = modifier.width(width).height(lineHeight).clearAndSetSemantics {},
                contentAlignment = Alignment.CenterEnd,
            ) {
                Bullet(depth, color, Modifier.size(fontSize * BULLET_SIZE))
            }
        }
    }
}

@Composable
private fun Bullet(
    depth: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        when (depth % BULLET_SHAPES) {
            0 -> drawCircle(color)
            1 -> drawCircle(color, style = Stroke(width = size.minDimension / BULLET_STROKE_RATIO))
            else -> drawRect(color)
        }
    }
}

@Composable
private fun Checkbox(
    checked: Boolean,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = size.minDimension / CHECKBOX_STROKE_RATIO
        val radius = CornerRadius(size.minDimension / CHECKBOX_CORNER_RATIO)
        if (checked) {
            drawRoundRect(accent, cornerRadius = radius)
            val check =
                Path().apply {
                    moveTo(size.width * CHECK_START_X, size.height * CHECK_START_Y)
                    lineTo(size.width * CHECK_MIDDLE_X, size.height * CHECK_MIDDLE_Y)
                    lineTo(size.width * CHECK_END_X, size.height * CHECK_END_Y)
                }
            drawPath(check, Color.White, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            val inset = stroke / 2
            drawRoundRect(
                color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = radius,
                style = Stroke(width = stroke),
            )
        }
    }
}

private const val BULLET_SHAPES = 3
private const val BULLET_STROKE_RATIO = 5f
private const val CHECKBOX_STROKE_RATIO = 8f
private const val CHECKBOX_CORNER_RATIO = 5f
private const val CHECK_START_X = 0.24f
private const val CHECK_START_Y = 0.52f
private const val CHECK_MIDDLE_X = 0.42f
private const val CHECK_MIDDLE_Y = 0.7f
private const val CHECK_END_X = 0.76f
private const val CHECK_END_Y = 0.32f
