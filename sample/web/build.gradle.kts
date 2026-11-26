// The sample application in the browser (Kotlin/Wasm). Run it with
// `./gradlew :sample:web:wasmJsBrowserDevelopmentRun`, or build the static site with
// `./gradlew :sample:web:wasmJsBrowserDistribution` (build/dist/wasmJs/productionExecutable).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.composdown.quality)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composdown-sample")
        browser {
            commonWebpackConfig {
                outputFileName = "composdown-sample.js"
            }
            // The sample has no tests.
            testTask { enabled = false }
        }
        binaries.executable()
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.sample.shared)
            implementation(libs.compose.ui)
        }
    }
}
