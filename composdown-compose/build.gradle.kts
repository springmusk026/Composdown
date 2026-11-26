plugins {
    alias(libs.plugins.composdown.compose.library)
    alias(libs.plugins.poko)
    alias(libs.plugins.roborazzi)
}

roborazzi {
    // Golden images are committed next to the tests that produce them.
    outputDir.set(layout.projectDirectory.dir("src/desktopTest/snapshots"))
}

poko {
    // Composdown's own internal annotation, so the library has no runtime dependency on Poko.
    pokoAnnotation.set("io/github/springmusk026/composdown/compose/internal/Poko")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.composdownCore)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        named("desktopTest").dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
        }
    }
}
