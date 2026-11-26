package io.github.springmusk026.composdown.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Runs the sample application in a desktop window. */
fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Composdown sample",
            state = rememberWindowState(width = 720.dp, height = 900.dp),
        ) {
            SampleApp()
        }
    }
