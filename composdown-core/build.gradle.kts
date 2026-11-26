plugins {
    alias(libs.plugins.composdown.kmp.library)
    alias(libs.plugins.poko)
}

poko {
    // Use Composdown's own internal annotation so the published library has no runtime dependency on
    // Poko's annotations artifact.
    pokoAnnotation.set("io/github/springmusk026/composdown/core/internal/Poko")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.collections.immutable)
            }
        }
    }
}
