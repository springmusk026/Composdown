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
        named("desktopTest").dependencies {
            implementation(libs.coil.test)
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
