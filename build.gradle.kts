plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.binary.compatibility.validator)
}

apiValidation {
    // Benchmarks and samples are not published.
    ignoredProjects += listOf("benchmarks", "shared", "desktop", "android", "screenshots", "macrobenchmark", "web")

    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

// Formatting for files outside library modules: root scripts and the build-logic included build.
// Library modules get the same rules from the `composdown.quality` convention plugin.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("build-logic/**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts", "build-logic/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
}

// API reference for all library modules in one site: ./gradlew dokkaGenerate (build/dokka/html).
dokka {
    moduleName.set("Composdown")
    dokkaPublications.html {
        includes.from("docs/api-reference.md")
    }
    pluginsConfiguration.html {
        // Replaces Dokka's default logo with Composdown's (the file name is what Dokka's styles expect).
        customAssets.from("config/dokka/logo-icon.svg")
        footerMessage.set("Copyright 2026 Composdown contributors. Licensed under the Apache License 2.0.")
    }
}

dependencies {
    dokka(projects.composdownCore)
    dokka(projects.composdownCompose)
    dokka(projects.composdownCoil)
    dokka(projects.composdownHighlight)
}

// The wasmJs tool distributions come from the repositories declared in settings.gradle.kts.
allprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}
