package io.github.springmusk026.composdown.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** Package name of the sample application under test. */
internal const val PACKAGE_NAME = "io.github.springmusk026.composdown.sample"

/** How long to wait for an element or for a streamed response to complete. */
private const val TIMEOUT_MS = 15_000L

/** Fraction of the scrollable's height that each scroll gesture leaves as margin. */
private const val GESTURE_MARGIN = 0.2f

/**
 * Interactions with the sample application, shared by the benchmarks and the Baseline Profile
 * generator. The resource ids are the sample's test tags (`SampleTags` in `sample/shared`).
 */
internal object SampleApp {
    /** Opens the Showcase tab and scrolls through the whole document and back. */
    fun MacrobenchmarkScope.scrollShowcase() {
        device.findObject(By.res("showcase_tab")).click()
        val showcase = waitFor("showcase")
        showcase.setGestureMargin((device.displayHeight * GESTURE_MARGIN).toInt())
        while (showcase.scroll(Direction.DOWN, 1f)) {
            device.waitForIdle()
        }
        showcase.fling(Direction.UP)
        device.waitForIdle()
    }

    /** Opens the Chat tab, sends the first prompt and waits until its response has streamed. */
    fun MacrobenchmarkScope.streamResponse() {
        device.findObject(By.res("chat_tab")).click()
        waitFor("prompt_0").click()
        // The last sentence of the first canned response: streaming has finished when it appears.
        check(device.wait(Until.hasObject(By.textContains("That's it!")), TIMEOUT_MS)) {
            "The streamed response did not complete within $TIMEOUT_MS ms"
        }
    }

    private fun MacrobenchmarkScope.waitFor(resourceId: String): UiObject2 =
        checkNotNull(device.wait(Until.findObject(By.res(resourceId)), TIMEOUT_MS)) {
            "No element with resource id $resourceId"
        }
}
