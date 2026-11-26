package io.github.springmusk026.composdown.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A soft brand-colored background behind the windows of an image. */
@Composable
internal fun Backdrop(
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // Animated GIFs have a 256-color palette, in which gradients show bands: they use a flat color.
    val background =
        if (flat) {
            Modifier.background(Palette.Backdrop)
        } else {
            Modifier
                .background(
                    Brush.linearGradient(listOf(Brand.Violet.copy(alpha = 0.16f), Brand.Blue.copy(alpha = 0.16f))),
                ).background(Color.White.copy(alpha = 0.5f))
        }
    Box(
        modifier = modifier.fillMaxWidth().then(background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.padding(40.dp), content = content)
    }
}

/** An application window: a title bar with [title] above [content], on the current surface. */
@Composable
internal fun AppWindow(
    title: String,
    width: Dp,
    modifier: Modifier = Modifier,
    contentHeight: Dp = Dp.Unspecified,
    elevation: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            modifier
                .width(width)
                .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (color in listOf(Palette.WindowClose, Palette.WindowMinimize, Palette.WindowZoom)) {
                Box(Modifier.size(11.dp).clip(CircleShape).background(color))
            }
            Text(
                text = title,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .then(
                    if (contentHeight !=
                        Dp.Unspecified
                    ) {
                        Modifier.height(contentHeight)
                    } else {
                        Modifier
                    },
                ).padding(20.dp),
        ) {
            content()
        }
    }
}

/** A chat message bubble sent by the user. */
@Composable
internal fun UserBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** A small caption above a window, such as "Default" or "Customized". */
@Composable
internal fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Palette.Caption,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}
