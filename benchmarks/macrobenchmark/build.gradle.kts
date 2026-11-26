// Android performance tests of the sample app: Macrobenchmarks (startup, scrolling, streaming) and
// the Baseline Profile generator. They run on a connected device or emulator (API 28 or newer):
//
//   ./gradlew :benchmarks:macrobenchmark:connectedBenchmarkReleaseAndroidTest
//   ./gradlew :sample:android:generateBaselineProfile
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.composdown.quality)
}

android {
    namespace = "io.github.springmusk026.composdown.benchmark"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        // Baseline Profile generation needs API 28; Macrobenchmark's metrics are reliable from there.
        minSdk = 28
        targetSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":sample:android"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.test.ext.junit)
}
