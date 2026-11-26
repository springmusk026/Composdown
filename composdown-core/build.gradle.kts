plugins {
    alias(libs.plugins.composdown.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.collections.immutable)
        }
    }
}
