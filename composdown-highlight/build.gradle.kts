plugins {
    alias(libs.plugins.composdown.compose.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.composdownCompose)
            implementation(libs.highlights)
        }
    }
}
