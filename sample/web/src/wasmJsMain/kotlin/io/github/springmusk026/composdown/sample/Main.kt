package io.github.springmusk026.composdown.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

/** Runs the sample application in the page. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport { SampleApp() }
}
