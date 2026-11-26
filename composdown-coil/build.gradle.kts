plugins {
    alias(libs.plugins.composdown.compose.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.composdownCompose)
            api(libs.coil.compose)
            implementation(libs.compose.material3)
        }
    }
}
