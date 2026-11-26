plugins {
    alias(libs.plugins.composdown.compose.library)
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
    }
}
