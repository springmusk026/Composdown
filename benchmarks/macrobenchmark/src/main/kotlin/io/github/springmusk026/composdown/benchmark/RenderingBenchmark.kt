package io.github.springmusk026.composdown.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.springmusk026.composdown.benchmark.SampleApp.scrollShowcase
import io.github.springmusk026.composdown.benchmark.SampleApp.streamResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing while Markdown is on screen: scrolling a long static document, and a chat response
 * streamed at a language model's pace (where every token updates the document).
 */
@RunWith(AndroidJUnit4::class)
class RenderingBenchmark {
    /** Runs the measurements. */
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Frame timing while scrolling through the whole showcase document. */
    @Test
    fun scrollShowcase() =
        measure {
            scrollShowcase()
        }

    /** Frame timing while a chat response streams in and the list follows it. */
    @Test
    fun streamChatResponse() =
        measure {
            streamResponse()
        }

    private fun measure(block: MacrobenchmarkScope.() -> Unit) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = ITERATIONS,
            setupBlock = {
                pressHome()
                startActivityAndWait()
            },
            measureBlock = block,
        )

    private companion object {
        const val ITERATIONS = 5
    }
}
