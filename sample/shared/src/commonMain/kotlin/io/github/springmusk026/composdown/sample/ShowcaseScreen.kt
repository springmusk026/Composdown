package io.github.springmusk026.composdown.sample

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.springmusk026.composdown.compose.CodeHighlighter
import io.github.springmusk026.composdown.compose.Composdown
import io.github.springmusk026.composdown.compose.ImageRenderer

/** Static content: the whole showcase document in a scrolling column. */
@Composable
internal fun ShowcaseScreen(
    imageRenderer: ImageRenderer,
    codeHighlighter: CodeHighlighter,
    modifier: Modifier = Modifier,
) {
    Composdown(
        markdown = SampleContent.showcase,
        modifier =
            modifier
                .testTag(SampleTags.SHOWCASE)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        extensions = SampleExtensions,
        imageRenderer = imageRenderer,
        codeHighlighter = codeHighlighter,
    )
}
