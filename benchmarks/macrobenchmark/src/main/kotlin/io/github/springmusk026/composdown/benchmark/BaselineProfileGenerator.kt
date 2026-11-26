package io.github.springmusk026.composdown.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.springmusk026.composdown.benchmark.SampleApp.scrollShowcase
import io.github.springmusk026.composdown.benchmark.SampleApp.streamResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the sample app's Baseline Profile by exercising every rendering path: startup, a long
 * document with every element, and a streamed response.
 *
 * Run with `./gradlew :sample:android:generateBaselineProfile` and a connected device (API 28+).
 * The rules for Composdown's own classes are also the source of the library profile shipped in
 * `composdown-compose`; see `docs/performance.md`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    /** Collects the profile. */
    @get:Rule
    val rule = BaselineProfileRule()

    /** Runs the user journeys whose code the profile should contain. */
    @Test
    fun generate() =
        rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            scrollShowcase()
            streamResponse()
        }
}
