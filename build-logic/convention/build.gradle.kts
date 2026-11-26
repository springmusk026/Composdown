plugins {
    `kotlin-dsl`
}

group = "io.github.springmusk026.composdown.buildlogic"

dependencies {
    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.compose.compiler)
    compileOnly(libs.gradlePlugin.compose)
    compileOnly(libs.gradlePlugin.android)
    compileOnly(libs.gradlePlugin.spotless)
    compileOnly(libs.gradlePlugin.detekt)
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "composdown.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("composeLibrary") {
            id = "composdown.compose.library"
            implementationClass = "ComposeLibraryConventionPlugin"
        }
        register("quality") {
            id = "composdown.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
