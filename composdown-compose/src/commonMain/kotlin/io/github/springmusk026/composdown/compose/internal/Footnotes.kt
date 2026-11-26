package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import io.github.springmusk026.composdown.compose.ComposdownBlock
import io.github.springmusk026.composdown.compose.ComposdownTheme
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.FootnoteDefinition

/** The footnotes of a document, numbered, below a divider. */
@Composable
internal fun Footnotes(
    footnotes: List<FootnoteDefinition>,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    val targets = LocalNavigationTargets.current
    val scope = rememberCoroutineScope()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(theme.spacing.tightListItem)) {
        HorizontalDivider(color = theme.colors.divider)
        for (footnote in footnotes) {
            Row(modifier = Modifier.then(rememberFootnoteTarget(footnote.label))) {
                Text(
                    text = "${footnote.number}.",
                    modifier = Modifier.width(theme.spacing.listMarkerWidth),
                    style = theme.typography.footnote.merge(color = theme.colors.listMarker, textAlign = TextAlign.End),
                )
                Spacer(modifier = Modifier.width(theme.spacing.tightListItem * 2))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(theme.spacing.tightListItem),
                ) {
                    for (block in footnote.children) {
                        if (block is Block.Paragraph) {
                            InlineContent(block.content, theme.typography.footnote)
                        } else {
                            ComposdownBlock(block)
                        }
                    }
                }
                if (targets != null) {
                    BackToReference(footnote.number, theme) { targets.scrollToReference(footnote.label, scope) }
                }
            }
        }
    }
}

/** A link from a footnote back to the text that references it. */
@Composable
private fun BackToReference(
    number: Int,
    theme: ComposdownTheme,
    onClick: () -> Unit,
) {
    val text =
        remember(theme, onClick) {
            buildAnnotatedString {
                val link =
                    LinkAnnotation.Clickable(
                        tag = "footnote-back",
                        styles = TextLinkStyles(style = SpanStyle(color = theme.colors.link)),
                        linkInteractionListener = { onClick() },
                    )
                withLink(link) { append(BACK_ARROW) }
            }
        }
    Text(
        text = text,
        modifier =
            Modifier.padding(start = theme.spacing.tightListItem).semantics {
                contentDescription =
                    "Back to reference $number"
            },
        style = theme.typography.footnote,
    )
}

/** The symbol GitHub uses for a footnote's link back to its reference. */
private const val BACK_ARROW = "\u21A9"
