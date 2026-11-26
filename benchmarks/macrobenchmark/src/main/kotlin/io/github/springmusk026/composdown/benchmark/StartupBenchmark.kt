package io.github.springmusk026.composdown.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold startup of the sample app, whose first screen renders Markdown, without precompilation and
 * with the Baseline Profile. The difference shows what the profile saves on a fresh install.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    /** Runs the measurements. */
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Startup with all code interpreted or JIT-compiled, as on a first launch without profiles. */
    @Test
    fun startupWithoutPrecompilation() = startup(CompilationMode.None())

    /** Startup with the Baseline Profile compiled ahead of time, as after a Play Store install. */
    @Test
    fun startupWithBaselineProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = ITERATIONS,
        ) {
            pressHome()
            startActivityAndWait()
        }

    private companion object {
        const val ITERATIONS = 10
    }
}
