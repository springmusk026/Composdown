// Renders the images used in the README and the documentation (docs/images) with Compose's
// offscreen renderer, so they always show the current rendering. Run with
// `./gradlew :tools:screenshots:run`, then `python3 scripts/assemble_gif.py` for the animation.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.composdown.quality)
    application
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(projects.composdownCompose)
    implementation(projects.composdownHighlight)
    implementation(libs.compose.material3)
    implementation(compose.desktop.currentOs)
}

// The fonts bundled for the screenshot tests make the images independent of the machine's fonts.
sourceSets.main {
    resources.srcDir(rootDir.resolve("composdown-compose/src/desktopTest/resources"))
}

application {
    mainClass = "io.github.springmusk026.composdown.screenshots.MainKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    args =
        listOf(
            rootDir.resolve("docs/images").path,
            layout.buildDirectory
                .dir("frames")
                .get()
                .asFile.path,
        )
}
