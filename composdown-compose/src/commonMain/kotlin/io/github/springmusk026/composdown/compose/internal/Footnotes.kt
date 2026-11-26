package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.springmusk026.composdown.compose.ComposdownBlock
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.FootnoteDefinition

/** The footnotes of a document, numbered, below a divider. */
@Composable
internal fun Footnotes(
    footnotes: List<FootnoteDefinition>,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(theme.spacing.tightListItem)) {
        HorizontalDivider(color = theme.colors.divider)
        for (footnote in footnotes) {
            Row {
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
            }
        }
    }
}
