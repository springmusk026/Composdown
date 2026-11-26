package io.github.springmusk026.composdown.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.springmusk026.composdown.coil.CoilImageRenderer
import io.github.springmusk026.composdown.highlight.HighlightsCodeHighlighter

/** The sample application: a simulated chat and a showcase of every element. */
@Composable
fun SampleApp(modifier: Modifier = Modifier) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val imageRenderer = remember { CoilImageRenderer() }
    val codeHighlighter = remember { HighlightsCodeHighlighter() }
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = modifier.fillMaxSize()) {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            Column {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Chat") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Showcase") })
                }
                when (tab) {
                    0 -> ChatScreen(imageRenderer, codeHighlighter)
                    else -> ShowcaseScreen(imageRenderer, codeHighlighter)
                }
            }
        }
    }
}
