package io.github.springmusk026.composdown.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/** Hosts the sample application. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Test tags become resource ids, for the macrobenchmarks and the Baseline Profile generator.
            SampleApp(Modifier.safeDrawingPadding().semantics { testTagsAsResourceId = true })
        }
    }
}
